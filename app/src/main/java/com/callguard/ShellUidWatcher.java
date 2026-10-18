package com.callguard;

import android.os.*;
import java.lang.reflect.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Observe only CallGuard activation; never start the app by polling or monitor other app data. */
final class ShellUidWatcher {
    static IBinder register(int appUid,Runnable reattach)throws Exception{
        Class<?> observerApi=Class.forName("android.app.IUidObserver");
        Class<?> stub=Class.forName("android.app.IUidObserver$Stub");
        // AOSP IUidObserver's native Binder ABI: onUidGone first, onUidActive second.
        // Do not reflect the blocked private TRANSACTION_* implementation fields.
        final int activeCode=IBinder.FIRST_CALL_TRANSACTION+1;
        Handler main=new Handler(Looper.getMainLooper());AtomicBoolean pending=new AtomicBoolean();
        Binder receiver=new Binder(){
            @Override protected boolean onTransact(int code,Parcel data,Parcel reply,int flags){
                if(code==INTERFACE_TRANSACTION){if(reply!=null)reply.writeString("android.app.IUidObserver");return true;}
                if(Binder.getCallingUid()!=1000)return false;
                if(code!=activeCode)return false;
                data.enforceInterface("android.app.IUidObserver");int uid=data.readInt();
                if(data.dataAvail()!=0)return false;
                if(uid==appUid&&pending.compareAndSet(false,true))main.post(()->{pending.set(false);reattach.run();});
                return true;
            }
        };
        Object observer=stub.getMethod("asInterface",IBinder.class).invoke(null,receiver);
        Class<?> activityManager=Class.forName("android.app.ActivityManager");
        Object manager=activityManager.getMethod("getService").invoke(null);
        int activeFlag=activityManager.getField("UID_OBSERVER_ACTIVE").getInt(null);
        // Read the named framework flag; no package history or other UID callback is consumed.
        Class.forName("android.app.IActivityManager").getMethod("registerUidObserver",observerApi,int.class,int.class,String.class)
            .invoke(manager,observer,activeFlag,-1,"com.android.shell");
        return receiver;
    }
    private ShellUidWatcher(){}
}
