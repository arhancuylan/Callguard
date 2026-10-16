package com.callguard;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class RulesTest {
    private final List<String> actions=Arrays.asList("Reddet","Yanıtla");
    private Rules.Incoming call(boolean video){return Rules.incoming("com.whatsapp","call",true,"",1,video,actions);}
    @Test public void capturedTurkishSummaryVoiceIsSupported(){assertEquals(0,call(false).declineIndex);assertEquals(Rules.Media.VOICE,call(false).media);}
    @Test public void incomingVideoSummaryIsSupported(){assertEquals(Rules.Media.VIDEO,call(true).media);}
    @Test public void businessIsSupported(){assertNotNull(Rules.incoming("com.whatsapp.w4b","call",false,"Sesli arama",0,null,actions));}
    @Test public void ongoingAndScreeningAreNeverRejected(){for(int type:new int[]{2,3})assertNull(Rules.incoming("com.whatsapp","call",false,"Sesli arama",type,false,actions));}
    @Test public void nonCallAndOtherOwnersAreIgnored(){assertNull(Rules.incoming("other","call",false,"Sesli arama",1,false,actions));assertNull(Rules.incoming("com.whatsapp","msg",false,"Sesli arama",1,false,actions));}
    @Test public void untypedSummaryIsIgnored(){assertNull(Rules.incoming("com.whatsapp","call",true,"Sesli arama",0,null,actions));}
    @Test public void positiveMediaEvidenceRequired(){assertNull(Rules.incoming("com.whatsapp","call",false,"",0,null,actions));}
    @Test public void declineMustBeExplicitAndUnique(){assertNull(Rules.incoming("com.whatsapp","call",false,"",1,false,Arrays.asList("Yanıtla")));assertNull(Rules.incoming("com.whatsapp","call",false,"",1,false,Arrays.asList("Reddet","Reject","Yanıtla")));}
    @Test public void answerEvidenceRequiredForLegacy(){assertNull(Rules.incoming("com.whatsapp","call",false,"Sesli arama",0,null,Arrays.asList("Reddet")));}
    @Test public void actionOrderNotAssumed(){assertEquals(1,Rules.incoming("com.whatsapp","call",false,"Voice call",0,null,Arrays.asList("Answer","Decline")).declineIndex);}
    @Test public void unicodeActionsNormalize(){assertTrue(Rules.decline("\u200e REDDET\u00a0"));assertTrue(Rules.answer("YANITLA"));}
    @Test public void numberFormats(){assertEquals("+905551112233",Rules.number("0555 111 22 33"));assertEquals("+905551112233",Rules.number("5551112233"));assertEquals("+442079460123",Rules.number("0044 20 7946 0123"));assertEquals("+442079460123",Rules.number("tel:+442079460123"));}
    @Test public void invalidAndCallerTextNotParsedAsNumber(){for(String input:new String[]{"Ayşe 05551112233","123","++905551112233","private",""})assertEquals("",Rules.number(input));}
    @Test public void allThreeRules(){assertTrue(Rules.block(Rules.Mode.ALL,true));assertTrue(Rules.block(Rules.Mode.ALL,false));assertFalse(Rules.block(Rules.Mode.WHITELIST,true));assertTrue(Rules.block(Rules.Mode.WHITELIST,false));assertTrue(Rules.block(Rules.Mode.BLACKLIST,true));assertFalse(Rules.block(Rules.Mode.BLACKLIST,false));}
    @Test public void allThreeMediaFilters(){assertTrue(Rules.selected(Rules.Media.BOTH,Rules.Media.VOICE));assertTrue(Rules.selected(Rules.Media.BOTH,Rules.Media.VIDEO));assertFalse(Rules.selected(Rules.Media.VOICE,Rules.Media.VIDEO));assertFalse(Rules.selected(Rules.Media.VIDEO,Rules.Media.VOICE));}
    private List<Rules.Contact> people(){return Arrays.asList(new Rules.Contact(1,"Ayşe Yılmaz","+905551112233",true),new Rules.Contact(2,"Mehmet Kaya","+905559998877",false));}
    @Test public void contactMatchesNumberOrExactDisplayedName(){assertTrue(Rules.listed("Ayşe Yılmaz","",people(),true));assertTrue(Rules.listed("whatever","tel:+905551112233",people(),true));assertTrue(Rules.listed("0555 111 22 33","",people(),true));}
    @Test public void numberIdentityOverridesMatchingName(){assertFalse(Rules.listed("Ayşe Yılmaz","tel:+905559999999",people(),true));}
    @Test public void turkishUppercaseContactNamesMatch(){assertTrue(Rules.listed("AYŞE YILMAZ","",people(),true));}
    @Test public void partialNamesAndWrongListDoNotMatch(){assertFalse(Rules.listed("Ayşe","",people(),true));assertFalse(Rules.listed("Mehmet Kaya","",people(),true));assertTrue(Rules.listed("Mehmet Kaya","",people(),false));}
}
