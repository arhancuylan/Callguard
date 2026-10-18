package com.callguard;
import org.junit.Test;
import static org.junit.Assert.*;
public class AdbSetupStateTest {
    @Test public void settingsAndDeviceSelectionRemainDirectShortcuts(){
        AdbSetupState state=new AdbSetupState(false,false,false,false,false);
        assertEquals(0,state.actionFor(0));assertEquals(1,state.actionFor(1));assertEquals(4,state.actionFor(4));
    }
    @Test public void laterStepsExplainAndRouteToFirstMissingPrerequisite(){
        assertEquals(0,new AdbSetupState(false,false,false,false,false).actionFor(2));
        assertEquals(1,new AdbSetupState(true,false,false,false,false).actionFor(2));
        assertEquals(2,new AdbSetupState(true,true,false,false,false).actionFor(3));
        assertEquals(3,new AdbSetupState(true,true,true,false,false).actionFor(3));
    }
    @Test public void shortcutsNeverAlterEvidenceAcrossAllInputStates(){
        for(int mask=0;mask<32;mask++){
            AdbSetupState state=new AdbSetupState((mask&1)!=0,(mask&2)!=0,(mask&4)!=0,(mask&8)!=0,(mask&16)!=0);
            boolean[] before=state.done.clone();int next=state.next();
            for(int step=0;step<5;step++){
                int action=state.actionFor(step);assertTrue(action>=0&&action<=step);
                if(step==2||step==3)for(int i=0;i<action;i++)assertTrue(state.done[i]);
                assertArrayEquals(before,state.done);assertEquals(next,state.next());
            }
        }
    }
    @Test public void aLiveAuthenticatedConnectionSkipsAllFourPrerequisites(){AdbSetupState state=new AdbSetupState(false,false,false,true,false);assertEquals(4,state.completed());assertEquals(4,state.next());}
    @Test public void wirelessDiscoveryCompletesDeveloperAndWirelessOnly(){AdbSetupState state=new AdbSetupState(false,true,false,false,false);assertTrue(state.done[0]);assertTrue(state.done[1]);assertFalse(state.done[2]);assertFalse(state.done[3]);assertEquals(2,state.next());}
    @Test public void oldPairingAloneDoesNotClaimTheCurrentConnectionIsOpen(){AdbSetupState state=new AdbSetupState(true,true,true,false,false);assertEquals(3,state.next());assertFalse(state.done[3]);}
    @Test public void aRecoveryRecordIsNotProofThatCarSilenceIsActive(){assertFalse(new AdbSetupState(true,true,true,true,false).done[4]);assertEquals(5,new AdbSetupState(true,true,true,true,true).completed());}
    @Test public void connectionLossReopensTheConnectionStep(){assertEquals(3,new AdbSetupState(true,true,true,false,true).next());}
}
