package com.callguard;
import org.junit.Test;
import static org.junit.Assert.*;

public class RejectedNotificationTest {
    private RejectedNotification n(String key,String owner,String category,long posted,long when,int type,boolean summary,Object action){return new RejectedNotification(key,owner,category,posted,when,type,summary,action);}
    private RejectedNotification original(){return n("wa:23","com.whatsapp","call",100,90,1,false,"decline");}
    @Test public void exactIncomingNotificationMayBeCleaned(){assertTrue(original().matches(original()));}
    @Test public void businessAndLegacyIncomingUseSameStrictChecks(){for(int type:new int[]{0,1}){RejectedNotification b=n("business:23","com.whatsapp.w4b","call",100,90,type,false,"decline");assertTrue(b.matches(b));assertFalse(b.matches(original()));}}
    @Test public void reusedIdNewCallOrUpdatedNotificationIsPreserved(){assertFalse(original().matches(n("wa:23","com.whatsapp","call",101,90,1,false,"decline")));assertFalse(original().matches(n("wa:23","com.whatsapp","call",100,91,1,false,"decline")));assertFalse(original().matches(n("wa:23","com.whatsapp","call",100,90,1,false,"new action")));}
    @Test public void messagesSummariesAndConnectedCallsArePreserved(){assertFalse(original().matches(n("wa:23","com.whatsapp","msg",100,90,1,false,"decline")));assertFalse(original().matches(n("wa:23","com.whatsapp","call",100,90,1,true,"decline")));assertFalse(original().matches(n("wa:23","com.whatsapp","call",100,90,2,false,"decline")));assertFalse(original().matches(n("wa:23","com.whatsapp","call",100,90,3,false,"decline")));}
    @Test public void noBroadOrUnverifiedCancellation(){assertFalse(original().matches(null));assertFalse(original().matches(n("other key","com.whatsapp","call",100,90,1,false,"decline")));assertFalse(original().matches(n("wa:23","other","call",100,90,1,false,"decline")));assertFalse(original().matches(n("wa:23","com.whatsapp","call",100,90,1,false,null)));RejectedNotification summary=n("wa:23","com.whatsapp","call",100,90,1,true,"decline");assertFalse(summary.matches(summary));RejectedNotification ongoing=n("wa:23","com.whatsapp","call",100,90,2,false,"decline");assertFalse(ongoing.matches(ongoing));}
}
