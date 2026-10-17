package com.callguard;
import org.junit.Test;
import org.json.*;
import java.io.IOException;
import java.util.*;
import static org.junit.Assert.*;

public class PairedCarCatalogTest {
    @Test public void missingListIsAnExplicitFailureRatherThanNullJsonParsing(){assertThrows(IOException.class,()->PairedCarCatalog.encode(null));}
    @Test public void genuineEmptyListRemainsAnEmptyList()throws Exception{assertEquals(0,PairedCarCatalog.encode(Collections.emptyList()).length());}
    @Test public void deviceWithoutNameStillCanBeChosen()throws Exception{JSONArray values=PairedCarCatalog.encode(Arrays.asList(new PairedCarCatalog.Device("AA:BB:CC:DD:EE:FF",null)));assertEquals("Bluetooth cihazı",values.getJSONObject(0).getString("name"));assertEquals("AA:BB:CC:DD:EE:FF",values.getJSONObject(0).getString("address"));}
    @Test public void invalidRecordsAndDuplicateAddressesNeverCreateBrokenOptions()throws Exception{JSONArray values=PairedCarCatalog.encode(Arrays.asList(null,new PairedCarCatalog.Device(null,"Bad"),new PairedCarCatalog.Device("invalid","Bad"),new PairedCarCatalog.Device("aa:bb:cc:dd:ee:ff","Fiat"),new PairedCarCatalog.Device("AA:BB:CC:DD:EE:FF","Duplicate")));assertEquals(1,values.length());assertEquals("Fiat",values.getJSONObject(0).getString("name"));}
    @Test public void internalErrorsNeverLeakIntoTheToast(){String error=CarErrors.describe(new NullPointerException("Attempt to invoke String.length() on null device AA:BB:CC:DD:EE:FF"));assertFalse(error.contains("length"));assertFalse(error.contains("AA:BB"));assertFalse(error.contains("null"));assertNotNull(CarErrors.describe(new RuntimeException()));}
    @Test public void permissionsAndKnownRecoverableErrorsRemainActionable(){assertTrue(CarErrors.describe(new SecurityException("private details")).contains("iznini"));assertEquals("Bluetooth kapalı veya kullanılamıyor.",CarErrors.describe(new IllegalStateException("Bluetooth kapalı veya kullanılamıyor.")));}
}
