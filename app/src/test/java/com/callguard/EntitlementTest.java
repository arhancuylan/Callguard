package com.callguard;
import org.junit.Test;
import static org.junit.Assert.*;
public class EntitlementTest {
    @Test public void expirationIsExclusiveAndPaidAccessRemains(){assertTrue(Entitlement.access(false,100,99));assertFalse(Entitlement.access(false,100,100));assertTrue(Entitlement.access(true,100,1000));assertFalse(Entitlement.access(false,0,0));}
    @Test public void daysRoundUp(){assertEquals(7,Entitlement.daysLeft(Entitlement.TRIAL_MS,0));assertEquals(1,Entitlement.daysLeft(100,99));assertEquals(0,Entitlement.daysLeft(100,100));}
    @Test public void clockRollbackDoesNotExtendSession(){assertEquals(1500,Entitlement.estimatedNow(1000,500,1000,1,1200));assertEquals(1600,Entitlement.estimatedNow(1000,500,2,1,1600));}
}
