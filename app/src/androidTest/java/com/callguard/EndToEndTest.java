package com.callguard;

import android.Manifest;
import android.app.Instrumentation;
import android.content.*;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.ContactsContract;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.*;
import java.util.*;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/** OS notification delivery -> listener -> foreign PendingIntent -> foreign receiver, plus native UI workflows. */
@RunWith(AndroidJUnit4.class)
public class EndToEndTest {
    private final Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
    private final Context context=instrumentation.getTargetContext();
    private Store store;
    private void shell(String command) throws Exception {try(ParcelFileDescriptor p=instrumentation.getUiAutomation().executeShellCommand(command);FileInputStream in=new FileInputStream(p.getFileDescriptor())){byte[] b=new byte[1024];while(in.read(b)!=-1){}}}
    @Before public void before() throws Exception {
        store=new Store(context);store.setting("enabled",false);store.setting("rule","WHITELIST");store.setting("media","BOTH");store.setting("recordOnly",false);store.getWritableDatabase().delete("contacts",null,null);store.clearHistory();
        shell("am broadcast -a fixture.CLEAR -n com.whatsapp/com.callguard.fixture.Calls");
        shell("cmd notification allow_listener com.callguard.mobile/com.callguard.CallNotificationService");
    }
    @After public void after() throws Exception {store.setting("enabled",false);shell("am broadcast -a fixture.CLEAR -n com.whatsapp/com.callguard.fixture.Calls");store.close();}
    private void post(boolean video,String number) throws Exception {shell("am broadcast -a fixture.POST -n com.whatsapp/com.callguard.fixture.Calls --ez video "+video+" --es number "+number);}
    private boolean declined(){try(Cursor c=context.getContentResolver().query(Uri.parse("content://com.whatsapp.callguardfixture/state"),null,null,null,null)){assertNotNull(c);assertTrue(c.moveToFirst());assertEquals("Answer must never be invoked",0,c.getInt(1));return c.getInt(0)==1;}}
    private void awaitRecords(int count) throws Exception {long end=System.currentTimeMillis()+10000;while(System.currentTimeMillis()<end){try(Cursor c=store.history()){if(c.getCount()>=count)return;}Thread.sleep(50);}fail("Listener did not produce "+count+" records");}
    private String result(){try(Cursor c=store.history()){assertTrue(c.moveToFirst());return c.getString(3);}}
    @Test public void voiceDeclineTraversesRealAndroidNotificationPipeline() throws Exception {store.setting("enabled",true);post(false,"+905550001111");awaitRecords(1);assertTrue(declined());assertEquals("Reddetme isteği gönderildi",result());}
    @Test public void videoDeclineTraversesRealAndroidNotificationPipeline() throws Exception {store.setting("enabled",true);post(true,"+905550001111");awaitRecords(1);assertTrue(declined());try(Cursor c=store.history()){c.moveToFirst();assertEquals(1,c.getInt(2));}}
    @Test public void whitelistPermitsMatchingCallerAndRecordsIt() throws Exception {store.add("Test Caller","+905550001111",true);store.setting("enabled",true);post(false,"+905550001111");awaitRecords(1);assertFalse(declined());assertEquals("İzin verildi",result());}
    @Test public void blacklistRejectsListedCaller() throws Exception {store.add("Test Caller","+905550001111",false);store.setting("rule","BLACKLIST");store.setting("enabled",true);post(false,"+905550001111");awaitRecords(1);assertTrue(declined());}
    @Test public void blacklistPermitsUnlistedCaller() throws Exception {store.setting("rule","BLACKLIST");store.setting("enabled",true);post(false,"+905550001111");awaitRecords(1);assertFalse(declined());assertEquals("İzin verildi",result());}
    @Test public void allIncomingRuleOverridesWhitelist() throws Exception {store.add("Test Caller","+905550001111",true);store.setting("rule","ALL");store.setting("enabled",true);post(false,"+905550001111");awaitRecords(1);assertTrue(declined());}
    @Test public void recordOnlyDoesNotInvokeDecline() throws Exception {store.setting("recordOnly",true);store.setting("enabled",true);post(false,"+905550001111");awaitRecords(1);assertFalse(declined());assertEquals("Yalnızca kaydedildi",result());assertEquals(0,store.today());}
    @Test public void mediaSelectionExcludesOtherType() throws Exception {store.setting("media","VOICE");store.setting("enabled",true);post(true,"+905550001111");Thread.sleep(1500);try(Cursor c=store.history()){assertEquals(0,c.getCount());}assertFalse(declined());shell("am broadcast -a fixture.CLEAR -n com.whatsapp/com.callguard.fixture.Calls");post(false,"+905550001111");awaitRecords(1);assertTrue(declined());}
    @Test public void disabledProtectionDoesNotAct() throws Exception {post(false,"+905550001111");Thread.sleep(1500);assertFalse(declined());try(Cursor c=store.history()){assertEquals(0,c.getCount());}}
    @Test public void duplicateContactsAndLocalPersistence(){store.add("First","05550001111",true);store.add("Updated","+905550001111",true);store.add("Black","+905550001111",false);assertEquals(1,store.contacts().size());assertFalse(store.contacts().get(0).white);try(Store reopened=new Store(context)){assertEquals(1,reopened.contacts().size());}}
    private void nav(String name){onView(withContentDescription(name+" sekmesi")).perform(click());}
    @Test public void allSettingsMenusPersistAndProtectionToggleWorks() throws Exception {try(ActivityScenario<MainActivity> ignored=ActivityScenario.launch(MainActivity.class)){onView(withContentDescription("Tüm gelen aramaları reddet")).perform(scrollTo(),click());assertEquals(Rules.Mode.ALL,store.mode());nav("Ayarlar");onView(withText("Görüntülü")).perform(scrollTo(),click());assertEquals(Rules.Media.VIDEO,store.media());onView(withContentDescription("Yalnızca kaydet")).perform(scrollTo(),click());assertTrue(store.recordOnly());onView(withText("Call Guard by AC")).perform(scrollTo()).check(matches(isDisplayed()));nav("Koruma");onView(withContentDescription("Koruma anahtarı")).perform(scrollTo(),click());assertTrue(store.enabled());}}
    @Test public void manualContactSearchAndRemoveViaUi() throws Exception {try(ActivityScenario<MainActivity> ignored=ActivityScenario.launch(MainActivity.class)){nav("Kişiler");onView(withText("＋  Kişi ekle")).perform(scrollTo(),click());onView(withText("Elle numara ekle")).perform(click());onView(withHint("WhatsApp'ta görünen ad")).perform(typeText("Test Name"));onView(withHint("Telefon numarası (+90…)" )).perform(typeText("05550001111"),closeSoftKeyboard());onView(withText("Kaydet")).perform(click());onView(withText("Test Name")).check(matches(isDisplayed()));onView(withHint("Ad veya numara ara")).perform(typeText("Nobody"),closeSoftKeyboard());onView(withText("Aramana uygun kişi bulunamadı.")).check(matches(isDisplayed()));onView(withHint("Ad veya numara ara")).perform(replaceText(""),closeSoftKeyboard());onView(withContentDescription("Test Name kişisini kaldır")).perform(click());onView(withId(android.R.id.button1)).perform(click());assertTrue(store.contacts().isEmpty());}}
    @Test public void historyShortcutsAddToBothListsAndClear() throws Exception {store.history("Blocked Caller","+905550001111",false,"Reddetme isteği gönderildi");store.history("Allowed Caller","+905550002222",true,"İzin verildi");try(ActivityScenario<MainActivity> ignored=ActivityScenario.launch(MainActivity.class)){nav("Geçmiş");onView(withContentDescription("Blocked Caller arama kaydı")).perform(scrollTo(),click());onView(withText("Beyaz listeye ekle")).perform(click());onView(withText("Ekle")).perform(click());assertTrue(Rules.listed("","+905550001111",store.contacts(),true));onView(withContentDescription("Allowed Caller arama kaydı")).perform(scrollTo(),click());onView(withText("Kara listeye ekle")).perform(click());onView(withText("Ekle")).perform(click());assertTrue(Rules.listed("","+905550002222",store.contacts(),false));onView(withText("Geçmişi temizle")).perform(scrollTo(),click());onView(withText("Temizle")).perform(click());onView(withText("Henüz arama kaydı yok.")).check(matches(isDisplayed()));}}
    @Test public void realContactsProviderCanBeReadAndSelected() throws Exception {
        instrumentation.getUiAutomation().adoptShellPermissionIdentity(Manifest.permission.WRITE_CONTACTS,Manifest.permission.READ_CONTACTS);
        long rawId=0;
        try {
            ArrayList<ContentProviderOperation> ops=new ArrayList<>();
            ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI).withValue(ContactsContract.RawContacts.ACCOUNT_TYPE,null).withValue(ContactsContract.RawContacts.ACCOUNT_NAME,null).build());
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID,0).withValue(ContactsContract.Data.MIMETYPE,ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE).withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,"Rehber Test").build());
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID,0).withValue(ContactsContract.Data.MIMETYPE,ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE).withValue(ContactsContract.CommonDataKinds.Phone.NUMBER,"+905550003333").build());
            ContentProviderResult[] results=context.getContentResolver().applyBatch(ContactsContract.AUTHORITY,ops);rawId=ContentUris.parseId(results[0].uri);
        }finally{instrumentation.getUiAutomation().dropShellPermissionIdentity();}
        shell("pm grant com.callguard.mobile android.permission.READ_CONTACTS");
        try(ActivityScenario<MainActivity> ignored=ActivityScenario.launch(MainActivity.class)){
            nav("Kişiler");onView(withText("＋  Kişi ekle")).perform(scrollTo(),click());onView(withText("Rehberden seç")).perform(click());
            // Async contacts loading is bounded and then the real picker row is clicked.
            long end=System.currentTimeMillis()+8000;while(true){try{onView(withHint("Rehberde ara")).check(matches(isDisplayed()));break;}catch(androidx.test.espresso.NoMatchingViewException e){if(System.currentTimeMillis()>end)throw e;Thread.sleep(100);}}
            onView(withText("Rehber Test\n+905550003333")).perform(click());assertTrue(Rules.listed("Rehber Test","",store.contacts(),true));
        } finally {
            instrumentation.getUiAutomation().adoptShellPermissionIdentity(Manifest.permission.WRITE_CONTACTS);
            context.getContentResolver().delete(ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI,rawId),null,null);instrumentation.getUiAutomation().dropShellPermissionIdentity();
        }
    }
    private void screenshot(String name) throws Exception {instrumentation.waitForIdleSync();File dir=new File(context.getExternalFilesDir(null),"screenshots");assertTrue(dir.isDirectory()||dir.mkdirs());try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){instrumentation.getUiAutomation().takeScreenshot().compress(Bitmap.CompressFormat.PNG,100,out);}}
    @Test public void captureFourScreensAndMenusInTurkish() throws Exception {store.add("Ayşe Yılmaz","+905551112233",true);store.add("Mehmet Kaya","+905554445566",true);store.add("Elif Demir","+905557778899",true);store.history("Ahmet Yıldız","+905550005555",true,"Reddetme isteği gönderildi");store.history("Bilinmeyen numara","",false,"Reddetme isteği gönderildi");store.setting("enabled",true);try(ActivityScenario<MainActivity> ignored=ActivityScenario.launch(MainActivity.class)){screenshot("01-koruma");nav("Kişiler");screenshot("02-kisiler");nav("Geçmiş");screenshot("03-gecmis");nav("Ayarlar");screenshot("04-ayarlar");onView(withText("Call Guard by AC")).perform(scrollTo());screenshot("05-hakkinda");}}
}
