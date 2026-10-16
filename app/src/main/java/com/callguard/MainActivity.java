package com.callguard;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.view.accessibility.AccessibilityManager;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public final class MainActivity extends Activity {
    private final int green=Color.rgb(22,107,83), ink=Color.rgb(24,43,38), muted=Color.rgb(100,118,109);
    private LinearLayout page;
    private Store store;
    private int tab=0;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private SharedPreferences settings(){return getSharedPreferences("settings",MODE_PRIVATE);}
    @Override public void onCreate(Bundle b){super.onCreate(b);store=new Store(this);if(b!=null)tab=b.getInt("tab",0);}
    @Override protected void onResume(){super.onResume();render();}
    @Override protected void onSaveInstanceState(Bundle b){b.putInt("tab",tab);super.onSaveInstanceState(b);}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density);}
    private TextView text(String s,int size,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setPadding(0,dp(5),0,dp(5));return v;}
    private void title(String s){TextView t=text(s,28,ink);t.setTypeface(null,Typeface.BOLD);page.addView(t);}
    private void note(String s){page.addView(text(s,14,muted));}
    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(1);c.setPadding(dp(18),dp(15),dp(18),dp(15));GradientDrawable bg=new GradientDrawable();bg.setColor(Color.WHITE);bg.setCornerRadius(dp(20));c.setBackground(bg);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(12),0,dp(4));page.addView(c,p);return c;}
    private Button button(String s,Runnable action){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(green);b.setOnClickListener(v->action.run());return b;}
    private boolean serviceEnabled(){AccessibilityManager manager=(AccessibilityManager)getSystemService(ACCESSIBILITY_SERVICE);for(AccessibilityServiceInfo i:manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK))if(i.getResolveInfo().serviceInfo.packageName.equals(getPackageName()) && i.getResolveInfo().serviceInfo.name.equals(CallGuardService.class.getName()))return true;return false;}
    private void render(){
        LinearLayout shell=new LinearLayout(this);shell.setOrientation(1);shell.setBackgroundColor(Color.rgb(244,247,245));
        shell.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
        ScrollView scroll=new ScrollView(this);page=new LinearLayout(this);page.setOrientation(1);page.setPadding(dp(22),dp(20),dp(22),dp(24));scroll.addView(page);shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=new LinearLayout(this);String[] labels={"Koruma","İzin listesi","Geçmiş","Ayarlar"};for(int i=0;i<4;i++){final int index=i;Button b=button(labels[i],()->{tab=index;render();});b.setTextSize(12);if(tab==i)b.setTypeface(null,Typeface.BOLD);nav.addView(b,new LinearLayout.LayoutParams(0,dp(60),1));}shell.addView(nav);setContentView(shell);
        TextView brand=text("◈  CallGuard",17,green);brand.setTypeface(null,Typeface.BOLD);page.addView(brand);
        if(tab==0)home();else if(tab==1)allowed();else if(tab==2)history();else options();
    }
    private void home(){title("Huzurla devam et.");note("WhatsApp sesli aramaları senin kontrolünde.");
        boolean ready=serviceEnabled()&&settings().getBoolean("enabled",true);
        LinearLayout c=card();c.addView(text(ready?"●  Koruma açık":"○  Koruma etkin değil",22,green));c.addView(text("Herkesi engelle, sadece izin verilenler geçsin.",16,ink));
        c.addView(text(store.allowed().size()+" izin verilen kişi",14,muted));
        Switch enabled=new Switch(this);enabled.setText("Arama koruması");enabled.setChecked(settings().getBoolean("enabled",true));enabled.setPadding(0,dp(12),0,dp(12));enabled.setOnCheckedChangeListener((b,on)->{settings().edit().putBoolean("enabled",on).apply();render();});c.addView(enabled);
        if(!serviceEnabled())c.addView(button("Erişilebilirlik servisini etkinleştir",this::disclosure));
        LinearLayout info=card();info.addView(text("Nasıl çalışır?",18,ink));info.addView(text("1. Erişilebilirlik servisini aç.\n2. Aramasını almak istediğin kişileri ekle.\n3. Engelleme modunu seç.",16,muted));
        note("Tanımlanamayan arayanlar da engellenir. İzin listesi boşsa algılanan tüm gelen sesli aramalara seçilen mod uygulanır.");
    }
    private void disclosure(){new AlertDialog.Builder(this).setTitle("Erişilebilirlik erişimi").setMessage("CallGuard, yalnızca WhatsApp ve WhatsApp Business pencerelerindeki arama bilgilerini ve düğmeleri inceleyerek gelen sesli çağrıları tespit eder, izin listesi dışındakiler için reddet düğmesine basar. Android bu erişimle ekran içeriğini okumaya ve işlem yapmaya izin verir. Mesaj içeriği saklanmaz; arayan bilgisi ve işlem geçmişi yalnızca cihazda tutulur. İstediğin zaman servisi kapatabilirsin. Devam ederek bu kullanımı kabul edersin.").setNegativeButton("Vazgeç",null).setPositiveButton("Ayarları aç",(d,w)->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))).show();}
    private void allowed(){title("Sana ulaşabilenler");note("Yalnızca bu listedeki kişiler için çağrıya dokunulmaz.");page.addView(button("+ Rehberden kişi ekle",this::requestContacts));
        List<String[]> list=store.allowed();if(list.isEmpty())note("Henüz kimse eklenmedi. Tüm algılanan gelen sesli aramalar engellenir.");
        for(String[] a:list){LinearLayout c=card();c.addView(text(a[1],19,ink));c.addView(text(a[2],14,muted));c.addView(button("İzni kaldır",()->{store.remove(a[0]);render();}));}
        note("Eşleştirme, WhatsApp'ın gösterdiği tam ad veya tam numarayla yapılır. Aynı adlı kişiler ayırt edilemeyebilir; WhatsApp farklı bir ad gösterirse izin eşleşmeyebilir.");
    }
    private void requestContacts(){if(checkSelfPermission(Manifest.permission.READ_CONTACTS)==PackageManager.PERMISSION_GRANTED){loadContacts();return;}new AlertDialog.Builder(this).setTitle("Rehberden kişi seç").setMessage("İzin listene ekleyebilmen için rehberdeki ad ve telefon numaraları okunur. Yalnızca seçtiğin kişiler cihazda saklanır.").setNegativeButton("Vazgeç",null).setPositiveButton("Devam",(d,w)->requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},10)).show();}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==10){if(g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)loadContacts();else new AlertDialog.Builder(this).setMessage("Rehber izni verilmedi. İzin kalıcı olarak kapatıldıysa uygulama ayarlarından açabilirsin.").setPositiveButton("Uygulama ayarları",(d,w)->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getPackageName())))).setNegativeButton("Kapat",null).show();}}
    private void loadContacts(){Toast.makeText(this,"Rehber yükleniyor…",Toast.LENGTH_SHORT).show();worker.execute(()->{
        List<String[]> contacts=new ArrayList<>();String error=null;
        try(Cursor c=getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER},null,null,ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" ASC")){
            if(c!=null)while(c.moveToNext()){String phone=c.getString(1);if(phone!=null)contacts.add(new String[]{c.getString(0)==null?phone:c.getString(0),phone});}
        }catch(SecurityException e){error="Rehber izni kaldırılmış. Tekrar izin ver.";}catch(RuntimeException e){error="Rehber okunamadı. Lütfen tekrar dene.";}
        final String failure=error;
        runOnUiThread(()->{if(isFinishing()||isDestroyed())return;if(failure!=null){Toast.makeText(this,failure,Toast.LENGTH_LONG).show();return;}if(contacts.isEmpty()){Toast.makeText(this,"Rehberde telefon numarası yok.",Toast.LENGTH_LONG).show();return;}
            LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(16),0,dp(16),0);EditText search=new EditText(this);search.setHint("Ad veya numara ara");box.addView(search);ListView list=new ListView(this);box.addView(list,new LinearLayout.LayoutParams(-1,dp(340)));
            List<String[]> filtered=new ArrayList<>(contacts);ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1);for(String[] a:filtered)adapter.add(a[0]+"\n"+a[1]);list.setAdapter(adapter);
            AlertDialog dialog=new AlertDialog.Builder(this).setTitle("İzin verilecek kişi").setView(box).setNegativeButton("Kapat",null).create();
            search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){filtered.clear();adapter.clear();String q=Policy.normalize(s.toString());for(String[] row:contacts)if(Policy.normalize(row[0]+" "+row[1]).contains(q)){filtered.add(row);adapter.add(row[0]+"\n"+row[1]);}}public void afterTextChanged(android.text.Editable e){}});
            list.setOnItemClickListener((p,v,pos,id)->{String[] a=filtered.get(pos);store.add(a[0],a[1]);dialog.dismiss();render();});dialog.show();
        });
    });}
    private void history(){title("Arama geçmişi");note("En son 500 işlem, yalnızca bu cihazda.");List<String[]> entries=store.history();if(entries.isEmpty())note("Henüz bir işlem yok. Algılanan engelleme işlemleri burada görünecek.");else page.addView(button("Geçmişi temizle",()->new AlertDialog.Builder(this).setMessage("Tüm işlem geçmişi silinsin mi?").setNegativeButton("Vazgeç",null).setPositiveButton("Sil",(d,w)->{store.clear();render();}).show()));SimpleDateFormat fmt=new SimpleDateFormat("dd MMM yyyy · HH:mm",new Locale("tr","TR"));for(String[] h:entries){LinearLayout c=card();c.addView(text(h[0],18,ink));c.addView(text(h[1],14,green));c.addView(text(fmt.format(new Date(Long.parseLong(h[2]))),12,muted));}}
    private void options(){title("Senin kuralların");LinearLayout c=card();c.addView(text("Engelleme modu",19,ink));RadioGroup group=new RadioGroup(this);RadioButton reject=new RadioButton(this);reject.setId(101);reject.setText("Otomatik reddet");RadioButton ignore=new RadioButton(this);ignore.setId(102);ignore.setText("Sessizce yok say");group.addView(reject);group.addView(ignore);group.check(settings().getString("mode","reject").equals("reject")?101:102);group.setOnCheckedChangeListener((g,id)->settings().edit().putString("mode",id==101?"reject":"ignore").apply());c.addView(group);c.addView(text("Otomatik reddet: erişilebilir Reddet düğmesine basılır.\n\nSessizce yok say: çağrıya dokunulmaz, yalnızca geçmişe kaydedilir. WhatsApp zil sesi veya bildirimi susturulmaz. Gerçek sessizlik için WhatsApp bildirim ayarlarını kullan.",14,muted));
        page.addView(button("Erişilebilirlik ayarları",this::disclosure));note("Uyumluluk: Türkçe ve İngilizce arama ekranları için başlangıç kuralları içerir. WhatsApp sürümü, ekran kilidi ve cihaz üreticisi nedeniyle algılama/reddetme başarısız olabilir. Görüntülü çağrılar kapsam dışıdır. Gerçek cihazda dene.");
    }
    @Override public void onDestroy(){worker.shutdownNow();store.close();super.onDestroy();}
}
