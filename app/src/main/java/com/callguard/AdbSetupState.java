package com.callguard;

/** Completed rows require positive evidence; remembered UI visits are never proof of access. */
final class AdbSetupState {
    final boolean[] done;
    AdbSetupState(boolean developer,boolean wireless,boolean paired,boolean connected,boolean silenced){
        done=new boolean[]{developer||wireless||connected,wireless||connected,paired||connected,connected,silenced};
    }
    int next(){for(int i=0;i<done.length;i++)if(!done[i])return i;return done.length;}
    int completed(){int value=0;for(boolean item:done)if(item)value++;return value;}
    /** Settings and device selection can be opened independently; pairing/connection need prerequisites. */
    int actionFor(int requested){
        if(requested<0||requested>=done.length)throw new IllegalArgumentException();
        if(requested==2||requested==3)for(int i=0;i<requested;i++)if(!done[i])return i;
        return requested;
    }
}
