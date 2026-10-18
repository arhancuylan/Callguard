package com.callguard;

import org.junit.Test;
import static org.junit.Assert.*;

public class ShellAccessPolicyTest {
    private void caller(int actual,int installed,String signer,String apk,boolean exclusive){
        ShellAccessPolicy.caller(actual,10123,installed,"retained-signer",signer,"/data/app/callguard/base.apk",apk,exclusive);
    }
    @Test public void onlyPinnedUidSignerAndInstalledApkAreAccepted(){caller(10123,10123,"retained-signer","/data/app/callguard/base.apk",true);}
    @Test public void otherAppShellAndRootCannotUseTheHfpBinder(){for(int uid:new int[]{0,2000,10124,110123})assertThrows(SecurityException.class,()->caller(uid,10123,"retained-signer","/data/app/callguard/base.apk",true));}
    @Test public void reinstallUpdateSignatureChangeAndSharedUidFailClosed(){
        assertThrows(SecurityException.class,()->caller(10123,10124,"retained-signer","/data/app/callguard/base.apk",true));
        assertThrows(SecurityException.class,()->caller(10123,10123,"other-signer","/data/app/callguard/base.apk",true));
        assertThrows(SecurityException.class,()->caller(10123,10123,"retained-signer","/data/app/updated/base.apk",true));
        assertThrows(SecurityException.class,()->caller(10123,10123,"retained-signer","/data/app/callguard/base.apk",false));
    }
    @Test public void onlyFixedHfpRequestsAreAccepted(){
        ShellAccessPolicy.operation(ShellWire.PROBE,null,0);ShellAccessPolicy.operation(ShellWire.STOP,null,0);
        for(int operation:new int[]{ShellWire.READ,ShellWire.STATE,ShellWire.WRITE})ShellAccessPolicy.operation(operation,"01:23:45:67:89:AB",0);
        ShellAccessPolicy.operation(ShellWire.WRITE,"01:23:45:67:89:AB",100);
    }
    @Test public void arbitraryCommandsPathsPoliciesAndUnknownOpcodesAreRejected(){
        for(String input:new String[]{null,"","127.0.0.1:5555","/data/local/tmp/x","AA:BB:CC:DD:EE:FF;reboot"})
            assertThrows(IllegalArgumentException.class,()->ShellAccessPolicy.operation(ShellWire.WRITE,input,0));
        for(int policy:new int[]{-1,1,99,101})assertThrows(IllegalArgumentException.class,()->ShellAccessPolicy.operation(ShellWire.WRITE,"01:23:45:67:89:AB",policy));
        assertThrows(IllegalArgumentException.class,()->ShellAccessPolicy.operation(42,"01:23:45:67:89:AB",0));
        assertThrows(IllegalArgumentException.class,()->ShellAccessPolicy.operation(ShellWire.STOP,"01:23:45:67:89:AB",0));
    }
}
