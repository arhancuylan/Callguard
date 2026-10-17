package com.callguard;
import org.junit.Test;
import static org.junit.Assert.*;
public class ShizukuSetupStateTest {
    @Test public void installIsFirstWhenMissing(){assertEquals(0,ShizukuSetupState.next(false,false,false,true,true,true));}
    @Test public void manualAndroidStepsNeverClaimServiceReady(){assertEquals(1,ShizukuSetupState.next(true,false,false,false,false,true));assertEquals(2,ShizukuSetupState.next(true,false,false,true,false,true));assertEquals(3,ShizukuSetupState.next(true,false,false,true,true,true));}
    @Test public void actualServiceSkipsManualSteps(){assertEquals(4,ShizukuSetupState.next(true,true,false,false,false,true));assertEquals(5,ShizukuSetupState.next(true,true,true,false,false,true));}
    @Test public void revokedPermissionAndStoppedServerReturnToSetup(){assertEquals(4,ShizukuSetupState.next(true,true,false,true,true,true));assertEquals(3,ShizukuSetupState.next(true,false,true,true,true,true));}
    @Test public void oldAndroidGoesToComputerInstructions(){assertEquals(3,ShizukuSetupState.next(true,false,false,false,false,false));}
    @Test public void liveBinderCanBeUsedEvenWhenPackageVisibilityFails(){assertEquals(5,ShizukuSetupState.next(false,true,true,false,false,true));}
}
