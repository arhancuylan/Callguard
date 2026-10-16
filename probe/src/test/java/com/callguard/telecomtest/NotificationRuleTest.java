package com.callguard.telecomtest;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;
public class NotificationRuleTest {
    private int rule(String pkg,String category,boolean summary,String text,int type,Boolean video,String... actions){return NotificationRule.declineIndex(pkg,category,summary,text,type,video,List.of(actions));}
    @Test public void turkishVoiceCallFindsDecline(){assertEquals(0,rule("com.whatsapp","call",false,"Gelen sesli arama",0,null,"Reddet","Yanıtla"));}
    @Test public void neverAssumesFirstActionIsDecline(){assertEquals(1,rule("com.whatsapp","call",false,"Incoming voice call",0,null,"Answer","Decline"));}
    @Test public void callStyleIdentifiesVoiceWithoutText(){assertEquals(0,rule("com.whatsapp.w4b","call",false,"Incoming call",1,false,"Decline","Answer"));}
    @Test public void videoCallsNotTargeted(){assertEquals(-1,rule("com.whatsapp","call",false,"Görüntülü arama",1,false,"Reddet","Yanıtla"));assertEquals(-1,rule("com.whatsapp","call",false,"Voice call",1,true,"Decline","Answer"));assertEquals(-1,rule("com.whatsapp","call",false,"📹 Incoming call",1,null,"Decline","Answer"));}
    @Test public void ongoingAndOutgoingNotTargeted(){assertEquals(-1,rule("com.whatsapp","call",false,"Voice call",2,false,"Decline","Answer"));assertEquals(-1,rule("com.whatsapp","call",false,"Voice call",0,null,"End call"));}
    @Test public void otherAppsAndMessagesNotTargeted(){assertEquals(-1,rule("com.android.phone","call",false,"Voice call",1,false,"Decline","Answer"));assertEquals(-1,rule("com.whatsapp","msg",false,"Voice call",1,false,"Decline","Answer"));}
    @Test public void ambiguousMetadataDoesNotTriggerAction(){assertEquals(-1,rule("com.whatsapp","call",false,"Incoming call",0,null,"Decline","Answer"));assertEquals(-1,rule("com.whatsapp","call",false,"Incoming voice call",0,null,"Decline"));}
    @Test public void noGuessingUnknownActionOrGroupSummary(){assertEquals(-1,rule("com.whatsapp","call",true,"Voice call",0,null,"Decline","Answer"));assertEquals(-1,rule("com.whatsapp","call",false,"Voice call",1,false,"Button 1","Button 2"));}
    @Test public void multipleDeclineActionsAreAmbiguous(){assertEquals(-1,rule("com.whatsapp","call",false,"Voice call",1,false,"Decline","Reject","Answer"));}
    @Test public void invisibleBidiAndUnicodeSpacesDoNotHideDecline(){
        assertEquals(0,rule("com.whatsapp","call",false,"Gelen sesli arama",1,false,"\u2068Reddet\u2069","\u200eYanıtla\u200f"));
        assertEquals(0,rule("com.whatsapp","call",false,"Incoming voice call",1,false,"\u00a0Decline\u00a0call\u00a0","Answer"));
    }
    @Test public void typedIncomingAudioMayBeAnActionableSummary(){assertEquals(0,rule("com.whatsapp","call",true,"Gelen sesli arama",1,false,"Reddet","Yanıtla"));}
    @Test public void reasonIdentifiesRejectedCondition(){
        assertEquals(NotificationRule.Reason.NO_DECLINE_LABEL,NotificationRule.analyze("com.whatsapp","call",false,"",1,false,List.of("Button 1","Answer")).reason);
        assertEquals(NotificationRule.Reason.SUMMARY_WITHOUT_CALLSTYLE,NotificationRule.analyze("com.whatsapp","call",true,"Voice call",0,null,List.of("Decline","Answer")).reason);
    }
}

