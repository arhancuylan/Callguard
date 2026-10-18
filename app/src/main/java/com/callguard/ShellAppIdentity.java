package com.callguard;

import android.content.Context;
import android.content.pm.*;
import android.os.Build;
import java.security.MessageDigest;

/** Captures the installed primary-user app, never a caller-supplied package or certificate. */
final class ShellAppIdentity {
    final int uid;final String signer,apk;final boolean exclusive;
    private ShellAppIdentity(int uid,String signer,String apk,boolean exclusive){this.uid=uid;this.signer=signer;this.apk=apk;this.exclusive=exclusive;}
    @SuppressWarnings("deprecation")
    static ShellAppIdentity load(Context context)throws Exception{
        PackageManager pm=context.getPackageManager();
        PackageInfo info=pm.getPackageInfo(ShellWire.PACKAGE,Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES);
        Signature[] signatures=Build.VERSION.SDK_INT>=28?info.signingInfo.getApkContentsSigners():info.signatures;
        if(signatures==null||signatures.length!=1||info.applicationInfo==null)throw new SecurityException("Single app signer required");
        byte[] hash=MessageDigest.getInstance("SHA-256").digest(signatures[0].toByteArray());
        StringBuilder signer=new StringBuilder();for(byte b:hash)signer.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
        String[] packages=pm.getPackagesForUid(info.applicationInfo.uid);
        return new ShellAppIdentity(info.applicationInfo.uid,signer.toString(),info.applicationInfo.sourceDir,
            packages!=null&&packages.length==1&&ShellWire.PACKAGE.equals(packages[0]));
    }
    void check(Context context,int callerUid)throws Exception{
        ShellAppIdentity current=load(context);
        ShellAccessPolicy.caller(callerUid,uid,current.uid,signer,current.signer,apk,current.apk,current.exclusive);
    }
}
