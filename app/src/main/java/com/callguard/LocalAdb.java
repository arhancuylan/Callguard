package com.callguard;

import android.content.Context;
import android.os.Build;
import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.util.concurrent.*;
import io.github.muntashirakon.adb.AbsAdbConnectionManager;
import io.github.muntashirakon.adb.AdbStream;

/** One local device only. Identity is encrypted with Android Keystore and excluded from backups. */
final class LocalAdb extends AbsAdbConnectionManager {
    private static volatile LocalAdb instance;
    private volatile boolean authorized;
    private final PrivateKey key;
    private final Certificate certificate;
    private final Context context;
    private static final ScheduledExecutorService CLOCK=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"adb-deadline");t.setDaemon(true);return t;});
    private LocalAdb(Context context)throws Exception{
        this.context=context.getApplicationContext();setApi(Build.VERSION.SDK_INT);setTimeout(12000,TimeUnit.MILLISECONDS);setThrowOnUnauthorised(true);
        AdbIdentity identity=AdbIdentity.load(this.context);key=identity.key;certificate=identity.certificate;
    }
    static synchronized LocalAdb get(Context context)throws Exception {if(instance==null)instance=new LocalAdb(context);return instance;}
    static boolean ready(){LocalAdb current=instance;return current!=null&&current.authorized;}
    @Override protected PrivateKey getPrivateKey(){return key;}
    @Override protected Certificate getCertificate(){return certificate;}
    @Override protected String getDeviceName(){return "CallGuard";}
    synchronized boolean pairLocal(int port,String code)throws Exception{
        if(port<1||port>65535)throw new IllegalArgumentException();
        if(Build.VERSION.SDK_INT<30)throw new IllegalStateException();
        if(code==null||!code.matches("[0-9]{6}"))throw new IllegalArgumentException();
        return pair("127.0.0.1",port,code);
    }
    synchronized void connectLocal(int port)throws Exception{
        if(port<1||port>65535)throw new IllegalArgumentException();
        authorized=false;disconnect();
        try {
            if(!connect("127.0.0.1",port))throw new IOException("ADB connect failed");
            if(command("probe")!=2000)throw new IOException("Unexpected ADB identity");
        }catch(Exception ex){disconnect();throw ex;}
        authorized=true;
        context.getSharedPreferences("local-adb",0).edit().putInt("port",port).apply();
    }
    synchronized int command(String...args)throws Exception{
        String command=CarCommandProtocol.command(context.getApplicationInfo().sourceDir,args);
        // Closing the underlying transport unblocks an unresponsive shell or TLS connection.
        final io.github.muntashirakon.adb.AdbConnection connection=getAdbConnection();
        if(connection==null)throw new IOException("ADB disconnected");
        ScheduledFuture<?> deadline=CLOCK.schedule(()->{try{connection.close();}catch(IOException ignored){}},25000,TimeUnit.MILLISECONDS);
        try(AdbStream stream=openStream("shell:"+command);InputStream input=stream.openInputStream()){
            ByteArrayOutputStream result=new ByteArrayOutputStream();byte[] buffer=new byte[512];int count;
            while((count=input.read(buffer))!=-1){if(result.size()+count>8192)throw new IOException("Helper response too large");result.write(buffer,0,count);}
            return CarCommandProtocol.parse(result.toString("UTF-8"));
        }catch(Exception ex){authorized=false;throw ex;}finally{deadline.cancel(false);}
    }
    // Do not call AbsAdbConnectionManager.close(): this device identity must survive reconnection.
    synchronized void disconnectLocal()throws IOException{authorized=false;disconnect();}
}
