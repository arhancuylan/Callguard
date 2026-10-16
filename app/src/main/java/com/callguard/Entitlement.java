package com.callguard;

/** Account-scoped server timestamps. Client-supplied start dates never create a paid entitlement. */
public final class Entitlement {
    public static final long TRIAL_MS=7L*24*60*60*1000;
    public static boolean access(boolean pro,long trialEnds,long now){return pro||(trialEnds>0&&now<trialEnds);}
    public static long daysLeft(long trialEnds,long now){return trialEnds<=now?0:1+(trialEnds-now-1)/(24L*60*60*1000);}
    public static long estimatedNow(long serverTime,long observedElapsed,long elapsed,long wall,long highestSeen){
        long monotonic=elapsed>=observedElapsed?serverTime+(elapsed-observedElapsed):serverTime;
        return Math.max(Math.max(monotonic,wall),highestSeen);
    }
    private Entitlement(){}
}
