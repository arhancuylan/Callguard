package com.callguard;

/** Completed rows require positive evidence; remembered UI visits are never proof of access. */
final class AdbSetupState {
    final boolean[] done;
    AdbSetupState(boolean developer,boolean wireless,boolean paired,boolean connected,boolean silenced){
        done=new boolean[]{developer||wireless||connected,wireless||connected,paired||connected,connected,silenced};
    }
    int next(){for(int i=0;i<done.length;i++)if(!done[i])return i;return done.length;}
    int completed(){int value=0;for(boolean item:done)if(item)value++;return value;}
}
