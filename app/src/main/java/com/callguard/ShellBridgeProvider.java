package com.callguard;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;

/** Only the authenticated local shell may deliver a Binder; this provider exposes no file/data API. */
public final class ShellBridgeProvider extends ContentProvider {
    @Override public boolean onCreate(){return true;}
    @Override public Bundle call(String method,String arg,Bundle extras){
        if(Binder.getCallingUid()!=2000||!"com.android.shell".equals(getCallingPackage()))throw new SecurityException("Shell only");
        if(!ShellWire.ATTACH.equals(method)||arg!=null||extras==null)throw new IllegalArgumentException();
        if(!ShellBridge.enabled(getContext()))throw new SecurityException("USB helper consent is disabled");
        IBinder binder=extras.getBinder(ShellWire.BINDER);
        if(binder==null)throw new IllegalArgumentException();
        long token=Binder.clearCallingIdentity();
        try{ShellBridge.attach(getContext(),binder);}finally{Binder.restoreCallingIdentity(token);}
        Bundle result=new Bundle();result.putBoolean("accepted",true);return result;
    }
    @Override public Cursor query(Uri u,String[] p,String s,String[] a,String o){throw new SecurityException();}
    @Override public String getType(Uri u){throw new SecurityException();}
    @Override public Uri insert(Uri u,ContentValues v){throw new SecurityException();}
    @Override public int delete(Uri u,String s,String[] a){throw new SecurityException();}
    @Override public int update(Uri u,ContentValues v,String s,String[] a){throw new SecurityException();}
}
