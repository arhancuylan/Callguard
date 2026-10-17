package com.callguard;

/** Only installed/running/permission are observed. Android setup confirmations are explicitly manual. */
final class ShizukuSetupState {
    static int next(boolean installed,boolean running,boolean authorized,boolean developerConfirmed,boolean wirelessConfirmed,boolean wirelessSupported){
        if(running)return authorized?5:4;
        if(!installed)return 0;
        if(!wirelessSupported)return 3;
        if(!developerConfirmed)return 1;
        if(!wirelessConfirmed)return 2;
        return 3;
    }
    private ShizukuSetupState(){}
}
