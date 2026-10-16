package com.callguard.telecomtest;
import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.companion.*;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.view.*;
import android.widget.*;
import java.util.*;

public final class ProbeActivity extends Activity {
    private LinearLayout body;
    private TextView logs;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable refresh=new Runnable(){public void run(){if(logs!=null)logs.setText(ProbeState.logs(ProbeActivity.this));handler.postDelayed(this,1500);}};
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
    private TextView label(String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(0xff182b26);t.setPadding(0,dp(8),0,dp(8));return t;}
    private void button(String title,Runnable action){Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setOnClickListener(v->action.run());body.addView(b);}
    @Override public void onCreate(Bundle b){super.onCreate(b);}
    @Override public void onResume(){super.onResume();render();handler.removeCallbacks(refresh);handler.post(refresh);}
    @Override public void onPause(){handler.removeCallbacks(refresh);super.onPause();}
    private void render(){
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(0xfff4f7f5);
        scroll.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i;});
        body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(22),dp(16),dp(22),dp(24));scroll.addView(body);setContentView(scroll);
        body.addView(label("CallGuard · Bildirim Test 0.2.5",25));
        body.addView(label("DENEYSEL · WhatsApp aramaları için cihaz testi\nBu uygulama eski CallGuard'dan ayrı kurulur. Yeni bildirim yöntemi, WhatsApp çağrı bildirimindeki reddet eylemini çalıştırmayı dener. Bildirim erişimi gerekir; saat gerekmez. Yeni sürüm ilk açılışta yalnızca gözlem moduna geçer.",15));
        if(!ProbeState.prefs(this).getBoolean("notification_migration",false)){
            ProbeState.prefs(this).edit().putString("command","notification").putBoolean("armed",false).putBoolean("notification_migration",true).apply();
        }
        boolean notificationAccess=getSystemService(android.app.NotificationManager.class).isNotificationListenerAccessGranted(new ComponentName(this,CallNotificationService.class));
        body.addView(label("Bildirim erişimi: "+(notificationAccess?"verildi":"verilmedi"),18));
        button("Bildirim erişimini aç / kontrol et",this::notificationDisclosure);
        CompanionDeviceManager manager=getSystemService(CompanionDeviceManager.class);
        int count=manager==null?0:manager.getMyAssociations().size();
        boolean access=ProbeAccess.canManageCalls(this);
        body.addView(label("Bu uygulamaya eşlenen cihaz: "+count+"\nAndroid çağrı yönetimi izni: "+(access?"verildi":"verilmedi"),16));
        button("1 · Watch 6 Classic ile ilişkilendir",()->new AlertDialog.Builder(this).setTitle("Saat ile ilişkilendirme").setMessage("Android'in kendi cihaz seçicisinde gerçek Watch 6 Classic saatini seç. CallGuard, bu yardımcı cihaz ilişkisi üzerinden Telecom çağrı yönetimine erişmeyi deneyecek. Saatin Galaxy Wearable bağlantısı CallGuard'a otomatik yetki vermez. Sistem izin vermezse deney çalışmaz. Eşleştirmeyi kaldırmak deneyi durdurur; Galaxy Wearable eşleştirmesini silme.").setNegativeButton("Vazgeç",null).setPositiveButton("Cihaz seç",(d,w)->pairWithPermission()).show());
        body.addView(label("Denenecek çağrı komutu",18));
        RadioGroup commands=new RadioGroup(this);
        RadioButton rejectCommand=new RadioButton(this);rejectCommand.setId(View.generateViewId());rejectCommand.setText("Reject · önceki yöntem");
        RadioButton disconnectCommand=new RadioButton(this);disconnectCommand.setId(View.generateViewId());disconnectCommand.setText("Disconnect · alternatif deneme");
        RadioButton notificationCommand=new RadioButton(this);notificationCommand.setId(View.generateViewId());notificationCommand.setText("WhatsApp bildirimindeki Reddet · yeni yöntem");
        commands.addView(notificationCommand);commands.addView(rejectCommand);commands.addView(disconnectCommand);
        String selectedCommand=ProbeState.prefs(this).getString("command","notification");
        commands.check(selectedCommand.equals("notification")?notificationCommand.getId():selectedCommand.equals("disconnect")?disconnectCommand.getId():rejectCommand.getId());
        commands.setOnCheckedChangeListener((g,id)->{
            String command=id==notificationCommand.getId()?"notification":id==disconnectCommand.getId()?"disconnect":"reject";
            ProbeState.prefs(this).edit().putString("command",command).putBoolean("armed",false).apply();
            ProbeState.log(this,"Komut seçildi: "+command+" · otomatik işlem kapatıldı; yeniden açmak gerekir");
            render();
        });
        body.addView(commands);
        body.addView(label("Disconnect, yalnızca hâlâ çalan ve izin listesi dışında kalan gelen WhatsApp sesli çağrısında denenir. Çağrı yanıtlanmaz. Etkinliği doğrulanmadı. Yöntem değişince otomatik işlem kapanır.",14));
        Switch armed=new Switch(this);armed.setText("2 · Otomatik reddetmeyi aç");armed.setPadding(0,dp(16),0,dp(16));armed.setChecked(ProbeState.prefs(this).getBoolean("armed",false));
        armed.setOnCheckedChangeListener((v,on)->{
            if(on){armed.setChecked(false);new AlertDialog.Builder(this).setTitle("Reddetme denemesini aç?").setMessage("Açıkken izin listesi dışındaki algılanan gelen WhatsApp sesli aramalarına seçtiğin yöntem uygulanır. Bildirim yöntemi, WhatsApp bildiriminin Reddet eylemini çalıştırır; bunun için saat veya Telecom izni gerekmez. Boş izin listesi tüm algılanan gelen sesli aramaları hedefler. Önce gözlem modunda çağrı olayının geldiğini doğrula. SIM aramalarına işlem yapılmaz.").setNegativeButton("Vazgeç",null).setPositiveButton("Denemeyi aç",(d,w)->{ProbeState.prefs(this).edit().putBoolean("armed",true).apply();ProbeState.log(this,"Otomatik reddetme kullanıcı tarafından açıldı");render();}).show();}
            else{ProbeState.prefs(this).edit().putBoolean("armed",false).apply();}
        });body.addView(armed);
        body.addView(label("İzin listesi · tam uluslararası numara",18));
        body.addView(label("Her satıra bir numara yaz: +905xxxxxxxxx. Eski CallGuard listesinden aktarılmaz. Ad eşleştirmesi yapılmaz. Numara gizliyse ve liste doluysa arama reddedilmez.",14));
        EditText numbers=new EditText(this);numbers.setHint("+905xxxxxxxxx");numbers.setMinLines(2);numbers.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        List<String> entries=new ArrayList<>(ProbeState.allowed(this));Collections.sort(entries);numbers.setText(String.join("\n",entries));body.addView(numbers);
        button("İzin listesini kaydet",()->{
            Set<String> allowed=new HashSet<>();for(String line:numbers.getText().toString().split("\\r?\\n")){if(line.trim().isEmpty())continue;String n=CallRule.number(line.trim());if(n.isEmpty()||!n.startsWith("+")){Toast.makeText(this,"Her numarayı + ve ülke koduyla yaz.",Toast.LENGTH_LONG).show();return;}allowed.add(n);}
            ProbeState.prefs(this).edit().putStringSet("allowed",allowed).apply();Toast.makeText(this,"İzin listesi kaydedildi",Toast.LENGTH_SHORT).show();
        });
        button("Durumu yenile",this::render);
        body.addView(label("3 · Tanılama olayları",20));
        button("Test başlangıcı: saat bağlı",()->ProbeState.log(this,"KULLANICI İŞARETİ: saat bağlı test BAŞLADI"));
        button("Test başlangıcı: saat bağlantısı kesik",()->ProbeState.log(this,"KULLANICI İŞARETİ: saat bağlantısı kesik test BAŞLADI"));
        button("Arayan kişi kapattı: işaretle",()->ProbeState.log(this,"KULLANICI İŞARETİ: arayan kişi çağrıyı sonlandırdı"));
        body.addView(label("Önce test başlangıcını işaretle, sonra başka telefondan ara. Her WhatsApp çağrısına ayrı kimlik verilir. API dönüşü veya kapanma tek başına başarı değildir. Kapanma gecikirse arayan kişi kapattıktan sonra ilgili işarete bas. Tam numara ve Bluetooth adresi kayda yazılmaz.",14));
        logs=label(ProbeState.logs(this),14);logs.setTextIsSelectable(true);body.addView(logs);
        button("Kayıtları kopyala",()->{android.content.ClipboardManager cm=getSystemService(android.content.ClipboardManager.class);cm.setPrimaryClip(ClipData.newPlainText("CallGuard Telecom",ProbeState.logs(this)));Toast.makeText(this,"Kayıtlar kopyalandı",Toast.LENGTH_SHORT).show();});
        body.addView(label("Bildirim yönteminde yukarıdaki saat ilişkisi ve Telecom izni gerekmez. İzin listesi doluyken bildirim arayanı yalnızca adla gösteriyorsa, bu deneme eşleşmeyi doğrulayamadığı için çağrıya dokunmaz.",14));
        button("Bu uygulamanın saat ilişkisini kaldır",()->new AlertDialog.Builder(this).setMessage("Yalnızca CallGuard Telecom Test ilişkileri kaldırılacak ve otomatik reddetme kapanacak.").setNegativeButton("Vazgeç",null).setPositiveButton("Kaldır",(d,w)->{
            ProbeState.prefs(this).edit().putBoolean("armed",false).apply();if(manager!=null)for(AssociationInfo a:manager.getMyAssociations())manager.disassociate(a.getId());render();}).show());
        body.addView(label("Sonuç henüz doğrulanmadı. Bildirimler kapalıyken, ekran kilitliyken ve saat bağlantısı kesilince ayrı test gerekir. Sesli/görüntülü ayrımı Telecom bilgisinin doğruluğuna bağlıdır. Eski CallGuard erişilebilirlik servisini bu test sırasında kapat.",14));
    }
    private void notificationDisclosure(){
        new AlertDialog.Builder(this).setTitle("Bildirim erişimi")
            .setMessage("Android bu erişimle uygulamaların bildirimlerini okuma ve bildirim eylemlerini çalıştırma yetkisi verir. CallGuard yalnızca WhatsApp/WhatsApp Business çağrı bildirimlerini işler; mesaj içeriklerini ve kişi başlıklarını kaydetmez. Reddet eylemi doğrulanırsa ve otomatik işlem açıksa bu eylemi çalıştırır. Veriler cihazda kalır. Erişimi Android ayarlarından istediğin zaman kapatabilirsin.")
            .setNegativeButton("Vazgeç",null).setPositiveButton("Android ayarlarını aç",(d,w)->{
                Intent intent=new Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS);
                intent.putExtra(android.provider.Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,new ComponentName(this,CallNotificationService.class).flattenToString());
                try{startActivity(intent);}catch(ActivityNotFoundException e){startActivity(new Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));}
            }).show();
    }
    private void pairWithPermission(){if(checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},20);return;}chooseBondedWatch();}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){super.onRequestPermissionsResult(request,permissions,grants);if(request==20&&grants.length>0&&grants[0]==PackageManager.PERMISSION_GRANTED)chooseBondedWatch();}
    private void chooseBondedWatch(){
        if(checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)return;
        BluetoothManager bluetooth=getSystemService(BluetoothManager.class);
        if(bluetooth==null||bluetooth.getAdapter()==null){Toast.makeText(this,"Bluetooth bu cihazda kullanılamıyor",Toast.LENGTH_LONG).show();return;}
        try{
            if(!bluetooth.getAdapter().isEnabled()){Toast.makeText(this,"Önce telefonun Bluetooth bağlantısını aç",Toast.LENGTH_LONG).show();return;}
            List<BluetoothDevice> devices=new ArrayList<>(bluetooth.getAdapter().getBondedDevices());
            if(devices.isEmpty()){ProbeState.log(this,"Eşleşmiş Bluetooth cihazı bulunamadı");render();return;}
            String[] names=new String[devices.size()];
            for(int i=0;i<devices.size();i++){
                BluetoothDevice device=devices.get(i);
                String name=device.getName();String address=device.getAddress();
                names[i]=(name==null?"Adsız eşleşmiş cihaz":name)+" · …"+address.substring(Math.max(0,address.length()-5));
            }
            new AlertDialog.Builder(this).setTitle("Eşleşmiş cihazlardan saatini seç")
                .setItems(names,(dialog,index)->pair(devices.get(index).getAddress()))
                .setNegativeButton("Vazgeç",null).show();
        }catch(SecurityException e){ProbeState.log(this,"Eşleşmiş cihazları okuma izni kaldırılmış");render();}
    }
    private void pair(String address){
        CompanionDeviceManager manager=getSystemService(CompanionDeviceManager.class);
        if(manager==null){ProbeState.log(this,"CompanionDeviceManager bu cihazda yok");render();return;}
        AssociationRequest request=new AssociationRequest.Builder().addDeviceFilter(new BluetoothDeviceFilter.Builder().setAddress(address).build()).setSingleDevice(true).setDeviceProfile(AssociationRequest.DEVICE_PROFILE_WATCH).build();
        try{manager.associate(request,getMainExecutor(),new CompanionDeviceManager.Callback(){
            @Override public void onAssociationPending(IntentSender sender){openChooser(sender);}
            @Override public void onDeviceFound(IntentSender sender){openChooser(sender);}
            @Override public void onAssociationCreated(AssociationInfo association){ProbeState.log(ProbeActivity.this,"Saat ilişkisi oluşturuldu; çağrı yönetim iznini ve yeni çağrıyı kontrol et");render();}
            @Override public void onFailure(CharSequence reason){ProbeState.log(ProbeActivity.this,"Saat ilişkilendirmesi tamamlanamadı");Toast.makeText(ProbeActivity.this,reason==null?"Eşleştirme başarısız":reason,Toast.LENGTH_LONG).show();render();}
        });}catch(SecurityException|IllegalStateException e){ProbeState.log(this,"Saat ilişkilendirme hatası: "+e.getClass().getSimpleName());render();}
    }
    private void openChooser(IntentSender sender){try{startIntentSenderForResult(sender,30,null,0,0,0);}catch(IntentSender.SendIntentException e){ProbeState.log(this,"Android cihaz seçicisi açılamadı");}}
}
