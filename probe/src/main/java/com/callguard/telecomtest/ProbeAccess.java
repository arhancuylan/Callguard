package com.callguard.telecomtest;
import android.content.Context;
import android.telecom.TelecomManager;
/** Telecom's companion-call access can be granted through an AppOp, not a normal runtime grant. */
final class ProbeAccess {
    private ProbeAccess() {}
    static boolean canManageCalls(Context context) {
        TelecomManager telecom=context.getSystemService(TelecomManager.class);
        if(telecom==null)return false;
        try{return telecom.hasManageOngoingCallsPermission();}
        catch(SecurityException|IllegalStateException e){return false;}
    }
}
