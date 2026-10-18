package com.callguard;

import android.content.*;
import android.os.*;
import java.io.IOException;

/** App-side, death-aware client. A remembered consent flag never means a live helper. */
final class ShellBridge {
    private static volatile IBinder remote;private static IBinder.DeathRecipient death;
    static boolean enabled(Context c){return c.getSharedPreferences("shell-helper",0).getBoolean("enabled",false);}
    static void allow(Context c){
        if(!c.getSharedPreferences("shell-helper",0).edit().putBoolean("enabled",true).commit())throw new IllegalStateException("Tercih kaydedilemedi");
    }
    static boolean ready(){IBinder b=remote;return b!=null&&b.isBinderAlive();}
    static Object token(){return ready()?remote:null;}
    static synchronized void attach(Context context,IBinder binder){
        if(!enabled(context))throw new SecurityException();
        if(remote!=null&&remote.isBinderAlive()&&remote!=binder)throw new SecurityException("Helper already attached; stop it first");
        if(remote==binder)return;
        clear();
        IBinder.DeathRecipient recipient=()->{synchronized(ShellBridge.class){if(remote==binder){remote=null;death=null;}}changed(context);};
        try{binder.linkToDeath(recipient,0);}catch(RemoteException ex){throw new IllegalStateException("Helper died",ex);}
        remote=binder;death=recipient;changed(context);
    }
    private static void changed(Context c){c.sendBroadcast(new Intent(c.getPackageName()+".CHANGED").setPackage(c.getPackageName()));}
    private static synchronized void clear(){IBinder old=remote;if(old!=null&&death!=null)try{old.unlinkToDeath(death,0);}catch(RuntimeException ignored){}remote=null;death=null;}
    static int command(int operation,String address,int policy)throws Exception{
        return call(remote,operation,address,policy);
    }
    static CarSilencePolicy.Backend backend(){
        final IBinder snapshot=remote;
        return new CarSilencePolicy.Backend(){
            public int read(String address)throws Exception{return call(snapshot,ShellWire.READ,address,0);}
            public boolean write(String address,int policy)throws Exception{return call(snapshot,ShellWire.WRITE,address,policy)==1;}
        };
    }
    private static int call(IBinder binder,int operation,String address,int policy)throws Exception{
        ShellAccessPolicy.operation(operation,address,policy);
        if(binder==null)throw new IOException("USB helper unavailable");
        Parcel data=Parcel.obtain(),reply=Parcel.obtain();
        try{
            data.writeInterfaceToken(ShellWire.DESCRIPTOR);data.writeString(address);data.writeInt(policy);
            if(!binder.transact(operation,data,reply,0))throw new IOException("Unsupported helper protocol");
            reply.readException();return reply.readInt();
        }catch(RemoteException ex){synchronized(ShellBridge.class){if(remote==binder)clear();}throw ex;}
        finally{data.recycle();reply.recycle();}
    }
    static void revoke(Context context)throws Exception{
        // Persist the denial first, so a late attach or app restart cannot resurrect consent.
        if(!context.getSharedPreferences("shell-helper",0).edit().putBoolean("enabled",false).commit())throw new IOException("Tercih kaydedilemedi");
        try{if(ready())command(ShellWire.STOP,null,0);}finally{clear();changed(context);}
    }
    private ShellBridge(){}
}
