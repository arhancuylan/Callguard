package com.callguard;

import android.content.Context;
import android.os.*;
import java.util.concurrent.*;

/** Fixed HFP Binder operations. Never forwards transactions or runs caller-provided code. */
final class HfpShellService extends Binder {
    interface Backend extends CarSilencePolicy.Backend {int state(String address)throws Exception;}
    private final Context context;private final ShellAppIdentity identity;private final Backend backend;private final Runnable stop;
    private boolean stopped;
    private final ScheduledExecutorService deadline=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"hfp-deadline");t.setDaemon(true);return t;});
    HfpShellService(Context context,ShellAppIdentity identity,Backend backend,Runnable stop){this.context=context;this.identity=identity;this.backend=backend;this.stop=stop;attachInterface(null,ShellWire.DESCRIPTOR);}
    @Override protected synchronized boolean onTransact(int code,Parcel data,Parcel reply,int flags)throws RemoteException{
        if((flags&IBinder.FLAG_ONEWAY)!=0||reply==null)return false;
        if(code==INTERFACE_TRANSACTION){reply.writeString(ShellWire.DESCRIPTOR);return true;}
        try{
            identity.check(context,Binder.getCallingUid());
            if(stopped)throw new IllegalStateException("Helper stopped");
            data.enforceInterface(ShellWire.DESCRIPTOR);
            String address=data.readString();int policy=data.readInt();
            if(data.dataAvail()!=0)throw new IllegalArgumentException("Unexpected request data");
            ShellAccessPolicy.operation(code,address,policy);
            if(code==ShellWire.STOP){stopped=true;reply.writeNoException();reply.writeInt(1);new Handler(Looper.getMainLooper()).postDelayed(stop,100);return true;}
            // Bluetooth Binder calls must carry the shell identity, not the ordinary app's UID.
            long token=Binder.clearCallingIdentity();
            ScheduledFuture<?> watchdog=deadline.schedule(stop,20,TimeUnit.SECONDS);
            int result;
            try{
                switch(code){
                    case ShellWire.PROBE:result=android.os.Process.myUid();break;
                    case ShellWire.READ:result=backend.read(address);break;
                    case ShellWire.STATE:result=backend.state(address);break;
                    case ShellWire.WRITE:result=backend.write(address,policy)?1:0;break;
                    default:throw new IllegalArgumentException();
                }
            }finally{watchdog.cancel(false);Binder.restoreCallingIdentity(token);}
            reply.writeNoException();reply.writeInt(result);return true;
        }catch(SecurityException|IllegalArgumentException|IllegalStateException ex){reply.writeException(ex);return true;}
        catch(Exception ex){reply.writeException(new IllegalStateException("HFP operation failed; recovery record must be retained"));return true;}
    }
}
