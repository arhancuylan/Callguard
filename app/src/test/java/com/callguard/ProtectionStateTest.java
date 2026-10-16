package com.callguard;
import org.junit.Test;
import static org.junit.Assert.*;
public class ProtectionStateTest {
    @Test public void pauseExpiresAtDeadline(){assertTrue(ProtectionState.paused(200,199));assertFalse(ProtectionState.paused(200,200));assertFalse(ProtectionState.paused(0,200));}
    @Test public void failuresAndPauseNeverShowActive(){assertEquals("Bildirim erişimi gerekli",ProtectionState.title(true,false,true,false,true,false,true));assertEquals("Kullanım hakkı gerekli",ProtectionState.title(true,true,false,false,true,false,true));assertEquals("Koruma duraklatıldı",ProtectionState.title(true,true,true,true,true,false,true));assertEquals("Zamanlama bekleniyor",ProtectionState.title(true,true,true,false,false,false,true));assertEquals("Yalnızca kayıt açık",ProtectionState.title(true,true,true,false,true,true,true));assertEquals("Bildirim bağlantısı bekleniyor",ProtectionState.title(true,true,true,false,true,false,false));assertEquals("Koruma açık",ProtectionState.title(true,true,true,false,true,false,true));}
    @Test public void historyKeepsDecisionAndReason(){assertEquals("Beyaz listede bulunamadı",ProtectionState.reason(Rules.Mode.WHITELIST,false));assertEquals("Kara listede",ProtectionState.reason(Rules.Mode.BLACKLIST,true));assertTrue(ProtectionState.permitted(ProtectionState.result("İzin verildi","Beyaz listede")));assertFalse(ProtectionState.permitted(ProtectionState.result("Reddetme isteği gönderildi","Kara listede")));}
}
