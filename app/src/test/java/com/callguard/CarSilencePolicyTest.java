package com.callguard;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class CarSilencePolicyTest {
    @Test public void losingShellDoesNotUndoPolicyAndRestoreNeedsShellAgain()throws Exception{
        Memory journal=new Memory();Device device=new Device(journal);
        CarSilencePolicy.enable(device,journal,CAR,"Synthetic headset");
        device.failReadAfterWrite=true;
        assertThrows(Exception.class,()->CarSilencePolicy.restore(device,journal));
        assertEquals(0,device.policy);assertNotNull(journal.saved);assertEquals(1,device.writes);
        device.failReadAfterWrite=false;
        CarSilencePolicy.restore(device,journal);
        assertEquals(100,device.policy);assertNull(journal.saved);
    }
    private static final String CAR="01:23:45:67:89:AB";
    private static final class Memory implements CarSilencePolicy.Journal {
        CarSilencePolicy.Saved saved;boolean failSave,failClear;
        public CarSilencePolicy.Saved load(){return saved;}
        public void save(CarSilencePolicy.Saved value){if(failSave)throw new IllegalStateException();saved=value;}
        public void clear(){if(failClear)throw new IllegalStateException();saved=null;}
    }
    private static final class Device implements CarSilencePolicy.Backend {
        int policy=100;boolean failWrite,failReadAfterWrite;final Memory journal;final List<String> addresses=new ArrayList<>();int writes;
        Device(Memory journal){this.journal=journal;}
        public int read(String address){if(failReadAfterWrite&&writes>0)throw new IllegalStateException();return policy;}
        public boolean write(String address,int next){assertNotNull("Recovery must be durable before changing Bluetooth",journal.saved);writes++;addresses.add(address);if(failWrite)return false;policy=next;return true;}
    }
    @Test public void enableAndRestoreTouchOnlySelectedCarAndPreserveOriginal()throws Exception{
        Memory journal=new Memory();Device device=new Device(journal);
        CarSilencePolicy.enable(device,journal,CAR,"Car");assertEquals(0,device.policy);assertEquals(100,journal.saved.previous);assertEquals(CAR,journal.saved.address);
        CarSilencePolicy.restore(device,journal);assertEquals(100,device.policy);assertNull(journal.saved);assertEquals(Arrays.asList(CAR,CAR),device.addresses);
    }
    @Test public void noBluetoothMutationIfJournalCannotBeSaved(){Memory journal=new Memory();journal.failSave=true;Device device=new Device(journal);assertThrows(Exception.class,()->CarSilencePolicy.enable(device,journal,CAR,"Car"));assertEquals(0,device.writes);assertEquals(100,device.policy);}
    @Test public void failedOrUncertainEnableRetainsRecovery(){for(boolean uncertain:new boolean[]{false,true}){Memory journal=new Memory();Device device=new Device(journal);device.failWrite=!uncertain;device.failReadAfterWrite=uncertain;assertThrows(Exception.class,()->CarSilencePolicy.enable(device,journal,CAR,"Car"));assertNotNull(journal.saved);assertEquals(100,journal.saved.previous);}}
    @Test public void repeatedEnableCannotOverwriteOriginalOrChangeAnotherCar()throws Exception{Memory journal=new Memory();Device device=new Device(journal);CarSilencePolicy.enable(device,journal,CAR,"Car");assertThrows(Exception.class,()->CarSilencePolicy.enable(device,journal,"AA:BB:CC:DD:EE:FF","Other"));assertEquals(1,device.writes);assertEquals(CAR,journal.saved.address);}
    @Test public void unknownOrAlreadyForbiddenPolicyIsNotChanged(){for(int old:new int[]{-1,0,42}){Memory journal=new Memory();Device device=new Device(journal);device.policy=old;assertThrows(Exception.class,()->CarSilencePolicy.enable(device,journal,CAR,"Car"));assertNull(journal.saved);assertEquals(0,device.writes);}}
    @Test public void failedRestoreKeepsJournalAndCanBeRetried()throws Exception{Memory journal=new Memory();Device device=new Device(journal);CarSilencePolicy.enable(device,journal,CAR,"Car");device.failWrite=true;assertThrows(Exception.class,()->CarSilencePolicy.restore(device,journal));assertNotNull(journal.saved);device.failWrite=false;CarSilencePolicy.restore(device,journal);assertNull(journal.saved);assertEquals(100,device.policy);}
    @Test public void externallyRestoredPolicyDoesNotNeedAnotherWrite()throws Exception{Memory journal=new Memory();Device device=new Device(journal);CarSilencePolicy.enable(device,journal,CAR,"Car");device.policy=100;CarSilencePolicy.restore(device,journal);assertEquals(1,device.writes);assertNull(journal.saved);}
    @Test public void invalidTargetNeverChangesAnything(){Memory journal=new Memory();Device device=new Device(journal);assertThrows(Exception.class,()->CarSilencePolicy.enable(device,journal,"not a device","Car"));assertEquals(0,device.writes);assertNull(journal.saved);}
}
