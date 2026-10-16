package com.callguard;

import org.junit.Test;
import static org.junit.Assert.*;

public class CallSessionsTest {
    private final CallSessions sessions=new CallSessions();
    private boolean claim(long time,String caller,String policy){return sessions.claim("whatsapp:23",time,caller,Rules.Media.VOICE,"decline",policy);}
    @Test public void updatesOfSameCallAreNotSentTwice(){assertTrue(claim(1000,"Caller","ALL"));assertFalse(claim(1000,"Caller","ALL"));}
    @Test public void nextCallWithReusedIdIsNotLostWithinFifteenSeconds(){assertTrue(claim(1000,"Caller","ALL"));assertTrue(claim(2000,"Caller","ALL"));}
    @Test public void changingFromAllowedToAllReevaluatesSameCall(){assertTrue(claim(1000,"Caller","WHITELIST:allowed"));assertTrue(claim(1000,"Caller","ALL:blocked"));assertFalse(claim(1000,"Caller","ALL:blocked"));}
    @Test public void newCallerAndNewActionAreProcessed(){assertTrue(claim(1000,"First","ALL"));assertTrue(claim(1000,"Second","ALL"));assertTrue(sessions.claim("whatsapp:23",1000,"Second",Rules.Media.VOICE,"new decline","ALL"));}
    @Test public void removalEndsCallSession(){assertTrue(claim(1000,"Caller","ALL"));sessions.remove("whatsapp:23");assertTrue(claim(1000,"Caller","ALL"));}
}
