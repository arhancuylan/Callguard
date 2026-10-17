package com.callguard;
import org.junit.Test;
import static org.junit.Assert.*;
public class AdbSetupStateTest {
    @Test public void aLiveAuthenticatedConnectionSkipsAllFourPrerequisites(){AdbSetupState state=new AdbSetupState(false,false,false,true,false);assertEquals(4,state.completed());assertEquals(4,state.next());}
    @Test public void wirelessDiscoveryCompletesDeveloperAndWirelessOnly(){AdbSetupState state=new AdbSetupState(false,true,false,false,false);assertTrue(state.done[0]);assertTrue(state.done[1]);assertFalse(state.done[2]);assertFalse(state.done[3]);assertEquals(2,state.next());}
    @Test public void oldPairingAloneDoesNotClaimTheCurrentConnectionIsOpen(){AdbSetupState state=new AdbSetupState(true,true,true,false,false);assertEquals(3,state.next());assertFalse(state.done[3]);}
    @Test public void aRecoveryRecordIsNotProofThatCarSilenceIsActive(){assertFalse(new AdbSetupState(true,true,true,true,false).done[4]);assertEquals(5,new AdbSetupState(true,true,true,true,true).completed());}
    @Test public void connectionLossReopensTheConnectionStep(){assertEquals(3,new AdbSetupState(true,true,true,false,true).next());}
}
