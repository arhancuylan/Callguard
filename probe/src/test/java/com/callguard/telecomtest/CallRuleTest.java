package com.callguard.telecomtest;
import org.junit.Test;
import java.util.Set;
import static org.junit.Assert.*;
public class CallRuleTest {
    private CallRule.Decision decide(String pkg,boolean incoming,boolean ringing,boolean video,boolean armed,String phone,Set<String> allow){return CallRule.decide(pkg,incoming,ringing,video,armed,phone,allow);}
    @Test public void defaultModeOnlyObserves(){assertEquals(CallRule.Decision.OBSERVE,decide("com.whatsapp",true,true,false,false,"+905551112233",Set.of()));}
    @Test public void simCallsNeverTargeted(){assertEquals(CallRule.Decision.OUT_OF_SCOPE,decide("com.android.phone",true,true,false,true,"+905551112233",Set.of()));}
    @Test public void unknownOwnerNeverTargeted(){assertEquals(CallRule.Decision.OUT_OF_SCOPE,decide(null,true,true,false,true,"",Set.of()));}
    @Test public void outgoingAndActiveAndVideoNeverRejected(){assertEquals(CallRule.Decision.OUT_OF_SCOPE,decide("com.whatsapp",false,true,false,true,"",Set.of()));assertEquals(CallRule.Decision.OUT_OF_SCOPE,decide("com.whatsapp",true,false,false,true,"",Set.of()));assertEquals(CallRule.Decision.OUT_OF_SCOPE,decide("com.whatsapp",true,true,true,true,"",Set.of()));}
    @Test public void whitelistProtectsAllowedNumber(){assertEquals(CallRule.Decision.ALLOW,decide("com.whatsapp",true,true,false,true,"0090 (555) 111-2233",Set.of("+905551112233")));}
    @Test public void unknownNumberDoesNotOverrideWhitelist(){assertEquals(CallRule.Decision.UNKNOWN_WITH_ALLOWLIST,decide("com.whatsapp",true,true,false,true,"private",Set.of("+905551112233")));}
    @Test public void nonAllowedIncomingRejectedWhenArmed(){assertEquals(CallRule.Decision.REJECT,decide("com.whatsapp.w4b",true,true,false,true,"+905554445566",Set.of("+905551112233")));}
    @Test public void emptyWhitelistBlocksUnknownWhenArmed(){assertEquals(CallRule.Decision.REJECT,decide("com.whatsapp",true,true,false,true,"",Set.of()));}
    @Test public void numbersAreNotGuessedFromNamesOrSuffixes(){assertEquals("",CallRule.number("Ahmet 123456"));assertNotEquals(CallRule.number("05551112233"),CallRule.number("+905551112233"));assertEquals("",CallRule.number("+90555+1112233"));}
}
