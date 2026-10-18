package com.callguard;

import android.content.*;
import android.os.*;
import android.net.Uri;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import static org.junit.Assert.*;

/** Emulator only. Real shell/Binder transport, synthetic HFP devices, no physical Bluetooth writes. */
@RunWith(AndroidJUnit4.class)
public class UsbShellTransportTest {
    private static final String A="AA:BB:CC:DD:EE:01",B="AA:BB:CC:DD:EE:02";
    private final android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
    private final Context context=instrumentation.getTargetContext();
    private void emulatorOnly(){assertTrue("Never run on a physical phone",Build.HARDWARE.contains("ranchu")||Build.HARDWARE.contains("goldfish"));}
    private String shell(String command)throws Exception{
        emulatorOnly();try(ParcelFileDescriptor p=instrumentation.getUiAutomation().executeShellCommand(command);InputStream in=new FileInputStream(p.getFileDescriptor())){
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[1024];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toString("UTF-8");
        }
    }
    private void waitReady(boolean expected)throws Exception{
        long end=System.currentTimeMillis()+12000;while(ShellBridge.ready()!=expected&&System.currentTimeMillis()<end)Thread.sleep(100);
        assertEquals("Helper log: "+shell("cat /data/local/tmp/callguard-shell-test.log"),expected,ShellBridge.ready());
    }
    private void startFake()throws Exception{
        startServer("com.callguard.ShellTestServer");
    }
    private void startServer(String entry)throws Exception{
        ShellBridge.allow(context);
        String apk=context.getApplicationInfo().sourceDir;
        assertTrue(apk.matches("[a-zA-Z0-9_./=+~-]+"));
        // UiAutomation does not promise shell quoting/pipelines. Execute a fixed script file.
        File script=new File(context.getExternalFilesDir(null),"start-shell-fixture.sh");
        try(FileOutputStream out=new FileOutputStream(script)){
            out.write(("CLASSPATH='"+apk+"' nohup /system/bin/app_process -Xhidden-api-policy:disabled /system/bin "+entry+" </dev/null >/data/local/tmp/callguard-shell-test.log 2>&1 &\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        shell("sh "+script.getAbsolutePath());
        waitReady(true);assertEquals(2000,ShellBridge.command(ShellWire.PROBE,null,0));
    }
    private BluetoothSilenceStore store()throws Exception{
        SharedPreferences prefs=context.getSharedPreferences("usb-test-journals",0);
        return new BluetoothSilenceStore(new BluetoothSilenceStore.Storage(){
            public String current(){return prefs.getString("devices",null);}public String legacy(){return null;}
            public void write(String value,boolean remove)throws Exception{if(!prefs.edit().putString("devices",value).commit())throw new IOException();}
        });
    }
    private void deniedRaw(int operation,String token,boolean trailing)throws Exception{
        Parcel data=Parcel.obtain(),reply=Parcel.obtain();
        try{
            data.writeInterfaceToken(token);data.writeString(A);data.writeInt(0);if(trailing)data.writeInt(42);
            ((IBinder)ShellBridge.token()).transact(operation,data,reply,0);
            assertThrows(RuntimeException.class,reply::readException);
        }finally{data.recycle();reply.recycle();}
    }
    @Test public void wifiOffDetachedShellAndIndependentDeviceRollback()throws Exception{
        emulatorOnly();ShellBridge.revoke(context);context.getSharedPreferences("usb-test-journals",0).edit().clear().commit();
        shell("svc wifi disable");startFake();
        assertThrows(SecurityException.class,()->context.getContentResolver().call(Uri.parse("content://"+ShellWire.AUTHORITY),ShellWire.ATTACH,null,new Bundle()));
        deniedRaw(42,ShellWire.DESCRIPTOR,false);deniedRaw(ShellWire.READ,"wrong.descriptor",false);deniedRaw(ShellWire.READ,ShellWire.DESCRIPTOR,true);
        BluetoothSilenceStore store=store();CarSilencePolicy.Backend backend=HfpConnection.backend(context);
        CarSilencePolicy.enable(backend,store.journal(A),A,"Synthetic car");CarSilencePolicy.enable(backend,store.journal(B),B,"Synthetic watch");
        assertEquals(0,backend.read(A));assertEquals(0,backend.read(B));
        CarSilencePolicy.restore(backend,store.journal(A));assertEquals(100,backend.read(A));assertEquals(0,backend.read(B));
        assertFalse(store.get(A).recovery());assertTrue(store.get(B).recovery());
        System.out.println("PASS: WIFI_OFF, DETACHED_ADB_SHELL, REAL_BINDER_AUTH, TWO_SYNTHETIC_DEVICES, INDEPENDENT_RESTORE");
        // Leave the helper and B's journal alive for the host-controlled app-process restart.
    }
    @Test public void appRestartReconnectsAndHelperDeathPreservesRecovery()throws Exception{
        emulatorOnly();assertTrue(ShellBridge.enabled(context));waitReady(true);
        assertEquals(2000,ShellBridge.command(ShellWire.PROBE,null,0));
        BluetoothSilenceStore store=store();CarSilencePolicy.Backend captured=HfpConnection.backend(context);
        assertTrue(store.get(B).recovery());assertEquals(0,captured.read(B));
        CarSilencePolicy.restore(captured,store.journal(B));assertEquals(100,captured.read(B));assertFalse(store.get(B).recovery());
        CarSilencePolicy.enable(captured,store.journal(B),B,"Synthetic watch");
        ShellBridge.revoke(context);waitReady(false);assertFalse(ShellBridge.enabled(context));
        assertThrows(Exception.class,()->CarSilencePolicy.restore(captured,store.journal(B)));assertTrue(store.get(B).recovery());
        System.out.println("PASS: APP_RESTART_REATTACH, JOURNAL_SURVIVES, EXPLICIT_REVOKE, DEAD_BINDER_FAIL_CLOSED");
    }
    @Test public void prepareReboot()throws Exception{emulatorOnly();startFake();System.out.println("PASS: HELPER_ALIVE_BEFORE_REBOOT");}
    @Test public void rebootDoesNotGrantOrStartShell()throws Exception{
        emulatorOnly();assertTrue(ShellBridge.enabled(context));Thread.sleep(500);
        assertFalse(ShellBridge.ready());assertTrue(store().get(B).recovery());ShellBridge.revoke(context);
        System.out.println("PASS: REBOOT_REQUIRES_USB_STARTUP, RECOVERY_PRESERVED");
    }
    @Test public void productionStartupWithoutWifiRejectsAnUnbondedTarget()throws Exception{
        emulatorOnly();ShellBridge.revoke(context);shell("svc wifi disable");
        startServer("com.callguard.CallGuardShell");
        assertEquals(2000,ShellBridge.command(ShellWire.PROBE,null,0));
        assertThrows(Exception.class,()->ShellBridge.command(ShellWire.READ,A,0));
        assertThrows(Exception.class,()->ShellBridge.command(ShellWire.WRITE,A,0));
        ShellBridge.revoke(context);waitReady(false);
        System.out.println("PASS: PRODUCTION_ENTRYPOINT_WIFI_OFF, UNBONDED_TARGET_REJECTED, NO_REAL_HFP_CHANGE");
    }
}
