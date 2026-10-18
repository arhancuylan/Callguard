package com.callguard;

import android.content.AttributionSource;
import android.os.*;
import java.lang.reflect.*;

/**
 * Shell processes are not AMS application processes: use the same external-provider API as
 * Android's `content` shell tool. Fixed authority and method only, never caller-provided IPC.
 * The external-provider rendezvous pattern was compared with Shizuku (Apache-2.0);
 * see assets/notices/callguard-shell-notices.txt. This compatibility shim is independently written.
 */
final class ShellProviderRendezvous {
    static Bundle attach(IBinder helper)throws Exception{
        if(android.os.Process.myUid()!=2000)throw new SecurityException();
        Class<?> activityManager=Class.forName("android.app.ActivityManager");
        Object manager=activityManager.getMethod("getService").invoke(null);
        Class<?> managerApi=Class.forName("android.app.IActivityManager");
        Object holder;
        if(Build.VERSION.SDK_INT>=29)holder=managerApi.getMethod("getContentProviderExternal",String.class,int.class,IBinder.class,String.class)
            .invoke(manager,ShellWire.AUTHORITY,0,null,ShellWire.PACKAGE);
        else holder=managerApi.getMethod("getContentProviderExternal",String.class,int.class,IBinder.class)
            .invoke(manager,ShellWire.AUTHORITY,0,null);
        if(holder==null)throw new IllegalStateException("CallGuard provider unavailable");
        try{
            Object provider=holder.getClass().getField("provider").get(holder);
            if(provider==null)throw new IllegalStateException("CallGuard provider unavailable");
            Class<?> api=Class.forName("android.content.IContentProvider");
            Bundle extras=new Bundle();extras.putBinder(ShellWire.BINDER,helper);
            if(Build.VERSION.SDK_INT>=31){
                AttributionSource source=new AttributionSource.Builder(2000).setPackageName("com.android.shell").build();
                return (Bundle)api.getMethod("call",AttributionSource.class,String.class,String.class,String.class,Bundle.class)
                    .invoke(provider,source,ShellWire.AUTHORITY,ShellWire.ATTACH,null,extras);
            }
            if(Build.VERSION.SDK_INT>=30)return (Bundle)api.getMethod("call",String.class,String.class,String.class,String.class,String.class,Bundle.class)
                .invoke(provider,"com.android.shell",null,ShellWire.AUTHORITY,ShellWire.ATTACH,null,extras);
            if(Build.VERSION.SDK_INT>=29)return (Bundle)api.getMethod("call",String.class,String.class,String.class,String.class,Bundle.class)
                .invoke(provider,"com.android.shell",ShellWire.AUTHORITY,ShellWire.ATTACH,null,extras);
            return (Bundle)api.getMethod("call",String.class,String.class,String.class,Bundle.class)
                .invoke(provider,"com.android.shell",ShellWire.ATTACH,null,extras);
        }finally{
            managerApi.getMethod("removeContentProviderExternal",String.class,IBinder.class).invoke(manager,ShellWire.AUTHORITY,null);
        }
    }
    private ShellProviderRendezvous(){}
}
