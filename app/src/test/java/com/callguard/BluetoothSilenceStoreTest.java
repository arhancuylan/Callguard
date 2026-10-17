package com.callguard;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class BluetoothSilenceStoreTest {
    private static final String A="AA:BB:CC:DD:EE:01",B="AA:BB:CC:DD:EE:02";
    private static final class Disk implements BluetoothSilenceStore.Storage {
        String current,legacy;boolean fail;
        public String current(){return current;}public String legacy(){return legacy;}
        public void write(String value,boolean remove)throws Exception{if(fail)throw new java.io.IOException();current=value;if(remove)legacy=null;}
    }
    private static final class Hardware implements CarSilencePolicy.Backend {
        final Map<String,Integer> policy=new HashMap<>();final BluetoothSilenceStore store;boolean failWrite;
        Hardware(BluetoothSilenceStore store){this.store=store;policy.put(A,100);policy.put(B,100);}
        public int read(String address){return policy.get(address);}
        public boolean write(String address,int value){assertTrue(store.get(address).recovery());if(failWrite)return false;policy.put(address,value);return true;}
    }
    @Test public void twoDevicesCanBeSilencedAndRestoredIndependently()throws Exception{
        Disk disk=new Disk();BluetoothSilenceStore store=new BluetoothSilenceStore(disk);Hardware hardware=new Hardware(store);
        CarSilencePolicy.enable(hardware,store.journal(A),A,"Car");CarSilencePolicy.enable(hardware,store.journal(B),B,"Headset");
        CarSilencePolicy.restore(hardware,store.journal(A));assertEquals(Integer.valueOf(100),hardware.policy.get(A));assertEquals(Integer.valueOf(0),hardware.policy.get(B));assertFalse(store.get(A).recovery());assertTrue(store.get(B).recovery());
    }
    @Test public void preferencesAndRecoverySurviveRestart()throws Exception{
        Disk disk=new Disk();BluetoothSilenceStore first=new BluetoothSilenceStore(disk);first.remember(A,"Car");first.remember(B,"Headset");first.journal(B).save(new CarSilencePolicy.Saved(B,"Headset",100));
        BluetoothSilenceStore restarted=new BluetoothSilenceStore(disk);assertEquals(2,restarted.entries().size());assertEquals("Car",restarted.get(A).name);assertFalse(restarted.get(A).recovery());assertTrue(restarted.get(B).recovery());
    }
    @Test public void restoringKeepsTheDeviceConfiguration()throws Exception{
        Disk disk=new Disk();BluetoothSilenceStore store=new BluetoothSilenceStore(disk);Hardware hardware=new Hardware(store);CarSilencePolicy.enable(hardware,store.journal(A),A,"Car");CarSilencePolicy.restore(hardware,store.journal(A));assertEquals(1,new BluetoothSilenceStore(disk).entries().size());assertEquals("Car",store.get(A).name);
    }
    @Test public void legacyRecoveryMigratesWithoutLosingTheOriginalPolicy()throws Exception{
        Disk disk=new Disk();disk.legacy="{\"address\":\""+A.toLowerCase(Locale.ROOT)+"\",\"name\":\"Fiat\",\"previous\":100}";BluetoothSilenceStore store=new BluetoothSilenceStore(disk);assertNull(disk.legacy);assertEquals(Integer.valueOf(100),store.get(A).previous);assertEquals(A,store.journal(A).load().address);
    }
    @Test public void failedMigrationRetainsTheLegacyRecord(){Disk disk=new Disk();disk.legacy="{\"address\":\""+A+"\",\"name\":\"Car\",\"previous\":100}";disk.fail=true;assertThrows(Exception.class,()->new BluetoothSilenceStore(disk));assertNotNull(disk.legacy);assertNull(disk.current);}
    @Test public void failedWritesKeepOnlyThatDevicesRecovery()throws Exception{
        Disk disk=new Disk();BluetoothSilenceStore store=new BluetoothSilenceStore(disk);Hardware hardware=new Hardware(store);hardware.failWrite=true;assertThrows(Exception.class,()->CarSilencePolicy.enable(hardware,store.journal(A),A,"Car"));assertTrue(store.get(A).recovery());assertNull(store.get(B));
    }
    @Test public void cannotForgetAnUnrestoredDevice()throws Exception{Disk disk=new Disk();BluetoothSilenceStore store=new BluetoothSilenceStore(disk);store.journal(A).save(new CarSilencePolicy.Saved(A,"Car",100));assertThrows(Exception.class,()->store.forget(A));assertTrue(store.get(A).recovery());}
    @Test public void forgettingOneInactiveDevicePreservesAllOtherRecords()throws Exception{Disk disk=new Disk();BluetoothSilenceStore store=new BluetoothSilenceStore(disk);store.remember(A,"Car");store.journal(B).save(new CarSilencePolicy.Saved(B,"Headset",100));store.forget(A);assertNull(store.get(A));assertTrue(new BluetoothSilenceStore(disk).get(B).recovery());}
    @Test public void failedJournalCommitNeverChangesHardware()throws Exception{Disk disk=new Disk();BluetoothSilenceStore store=new BluetoothSilenceStore(disk);Hardware hardware=new Hardware(store);disk.fail=true;assertThrows(Exception.class,()->CarSilencePolicy.enable(hardware,store.journal(A),A,"Car"));assertEquals(Integer.valueOf(100),hardware.policy.get(A));assertNull(store.get(A));}
    @Test public void corruptOrNewerSchemasAreNotSilentlyOverwritten(){Disk disk=new Disk();disk.current="{\"version\":2,\"devices\":[]}";assertThrows(Exception.class,()->new BluetoothSilenceStore(disk));assertEquals("{\"version\":2,\"devices\":[]}",disk.current);}
    @Test public void renamingOrReaddingNeverOverwritesAnActiveRecovery()throws Exception{Disk disk=new Disk();BluetoothSilenceStore store=new BluetoothSilenceStore(disk);store.journal(A).save(new CarSilencePolicy.Saved(A,"Old name",100));store.remember(A.toLowerCase(Locale.ROOT),"New name");assertEquals(1,store.entries().size());assertEquals("New name",store.get(A).name);assertEquals(Integer.valueOf(100),store.get(A).previous);}
    @Test public void failedRestoreOfOneDeviceDoesNotAffectAnother()throws Exception{Disk disk=new Disk();BluetoothSilenceStore store=new BluetoothSilenceStore(disk);Hardware hardware=new Hardware(store);CarSilencePolicy.enable(hardware,store.journal(A),A,"Car");CarSilencePolicy.enable(hardware,store.journal(B),B,"Headset");hardware.failWrite=true;assertThrows(Exception.class,()->CarSilencePolicy.restore(hardware,store.journal(A)));assertTrue(store.get(A).recovery());assertTrue(store.get(B).recovery());assertEquals(Integer.valueOf(0),hardware.policy.get(B));}
    @Test public void failedClearAfterHardwareRestoreCanBeRetriedWithoutLosingRecovery()throws Exception{Disk disk=new Disk();BluetoothSilenceStore store=new BluetoothSilenceStore(disk);Hardware hardware=new Hardware(store);CarSilencePolicy.enable(hardware,store.journal(A),A,"Car");disk.fail=true;assertThrows(Exception.class,()->CarSilencePolicy.restore(hardware,store.journal(A)));assertEquals(Integer.valueOf(100),hardware.policy.get(A));assertTrue(store.get(A).recovery());disk.fail=false;CarSilencePolicy.restore(hardware,store.journal(A));assertFalse(store.get(A).recovery());}
}
