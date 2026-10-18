package com.callguard;

/** Pure access checks, also exercised without Android or real Bluetooth devices. */
final class ShellAccessPolicy {
    static void caller(int callerUid,int pinnedUid,int currentUid,String pinnedSigner,String currentSigner,
                       String pinnedApk,String currentApk,boolean exclusiveUid){
        if(callerUid!=pinnedUid||currentUid!=pinnedUid||pinnedUid<10000||pinnedUid>=100000||!exclusiveUid
            ||pinnedSigner==null||pinnedSigner.isEmpty()||!pinnedSigner.equals(currentSigner)
            ||pinnedApk==null||!pinnedApk.equals(currentApk))throw new SecurityException("CallGuard identity changed");
    }
    static void operation(int operation,String address,int policy){
        if(operation==ShellWire.PROBE||operation==ShellWire.STOP){
            if(address!=null||policy!=0)throw new IllegalArgumentException();return;
        }
        if(address==null||!address.matches("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}"))throw new IllegalArgumentException();
        if((operation==ShellWire.READ||operation==ShellWire.STATE)&&policy==0)return;
        if(operation==ShellWire.WRITE&&(policy==0||policy==100))return;
        throw new IllegalArgumentException("Unsupported operation");
    }
    private ShellAccessPolicy(){}
}
