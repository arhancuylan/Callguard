package com.callguard;

import android.content.*;
import android.net.Uri;
import android.os.*;

/** Launched by the user's authorized USB ADB host, never by the normal app. No network socket. */
public final class CallGuardShell {
    private static IBinder uidWatcher;
    static Context shellContext()throws Exception{
        Class<?> type=Class.forName("android.app.ActivityThread");
        Object thread=type.getMethod("systemMain").invoke(null);
        Context system=(Context)type.getMethod("getSystemContext").invoke(thread);
        Context shell=system.createPackageContext("com.android.shell",Context.CONTEXT_IGNORE_SECURITY);
        return new ContextWrapper(shell){
            @Override public Context getApplicationContext(){return this;}
            @Override public String getOpPackageName(){return "com.android.shell";}
            @android.annotation.TargetApi(31)
            @Override public android.content.AttributionSource getAttributionSource(){return new android.content.AttributionSource.Builder(2000).setPackageName("com.android.shell").build();}
        };
    }
    public static void main(String[] args){
        if(args.length!=0||android.os.Process.myUid()!=2000){System.err.println("Authorized USB shell UID 2000 required");System.exit(1);return;}
        if(Build.VERSION.SDK_INT<30){System.err.println("CallGuard USB HFP helper requires Android 11 or newer");System.exit(1);return;}
        try{
            Looper.prepareMainLooper();Context context=shellContext();
            ShellAppIdentity identity=ShellAppIdentity.load(context);
            identity.check(context,identity.uid);
            if(!identity.apk.equals(System.getenv("CLASSPATH")))throw new SecurityException("Launch only the installed CallGuard APK");
            CarProfileService profile=new CarProfileService();
            HfpShellService.Backend backend=new HfpShellService.Backend(){
                public int read(String address)throws Exception{return profile.readPolicy(address);}
                public boolean write(String address,int policy)throws Exception{return profile.writePolicy(address,policy);}
                public int state(String address)throws Exception{return profile.connectionState(address);}
            };
            run(context,identity,backend);
        }catch(Exception ex){System.err.println("CallGuard helper could not start: "+ex.getClass().getSimpleName());System.exit(1);}
    }
    /** Package-private for debug-only emulator transport tests with a synthetic HFP backend. */
    static void run(Context context,ShellAppIdentity identity,HfpShellService.Backend backend)throws Exception{
        Runnable stop=()->android.os.Process.killProcess(android.os.Process.myPid());
        HfpShellService service=new HfpShellService(context,identity,backend,stop);
        Runnable attach=()->{
            try{
                identity.check(context,identity.uid);
                Bundle reply=ShellProviderRendezvous.attach(service);
                if(reply==null||!reply.getBoolean("accepted"))throw new SecurityException("Helper consent missing");
            }catch(Exception ex){android.util.Log.e("CallGuardShell","Binder delivery rejected",ex);stop.run();}
        };
        uidWatcher=ShellUidWatcher.register(identity.uid,attach);
        attach.run();System.out.println("CallGuard HFP helper started; no Wi-Fi transport required. Reboot requires USB startup again.");
        Looper.loop();
    }
    private CallGuardShell(){}
}
