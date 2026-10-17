package com.callguard;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.view.accessibility.AccessibilityEvent;
import android.widget.*;
import java.util.function.BooleanSupplier;
import rikka.shizuku.Shizuku;

/** In-app step highlighting, safe Settings navigation and truthful manual/observed progress. */
final class ShizukuGuide {
    private static final int GREEN=Color.rgb(8,99,78),INK=Color.rgb(13,22,38),MUTED=Color.rgb(99,110,125);
    private final Activity activity;
    private final BooleanSupplier installed;
    private final Runnable install,launch,authorize,choose,inspect,bluetooth;
    private final SharedPreferences confirmations;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private AlertDialog dialog;
    private LinearLayout body;
    private ScrollView scroll;
    private int stage=-1,selected=-1;
    private String signature="";
    private boolean resumed;
    private final Runnable tick=new Runnable(){public void run(){if(resumed&&showing()){refresh();handler.postDelayed(this,1200);}}};
    ShizukuGuide(Activity activity,BooleanSupplier installed,Runnable install,Runnable launch,Runnable authorize,Runnable choose,Runnable inspect,Runnable bluetooth){
        this.activity=activity;this.installed=installed;this.install=install;this.launch=launch;this.authorize=authorize;this.choose=choose;this.inspect=inspect;this.bluetooth=bluetooth;
        confirmations=activity.getSharedPreferences("shizuku-setup",Context.MODE_PRIVATE);
        // Do not carry a manual wireless-debugging confirmation across a reboot.
        int boot=-1;try{boot=Settings.Global.getInt(activity.getContentResolver(),Settings.Global.BOOT_COUNT);}catch(Settings.SettingNotFoundException|SecurityException ignored){}
        if(boot>=0&&confirmations.getInt("boot",-1)!=boot)confirmations.edit().putInt("boot",boot).remove("wireless").apply();
    }
    private int dp(int value){return Math.round(value*activity.getResources().getDisplayMetrics().density);}
    private LinearLayout column(){LinearLayout view=new LinearLayout(activity);view.setOrientation(LinearLayout.VERTICAL);return view;}
    private void gap(LinearLayout view,int height){view.addView(new View(activity),new LinearLayout.LayoutParams(1,dp(height)));}
    private TextView text(String value,int size,int color,boolean bold){TextView view=new TextView(activity);view.setText(value);view.setTextSize(size);view.setTextColor(color);if(bold)view.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));return view;}
    private GradientDrawable rounded(int color,boolean outlined){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(14));if(outlined)d.setStroke(dp(2),GREEN);return d;}
    private TextView button(LinearLayout parent,String title,boolean primary,Runnable action){
        TextView view=text(title,15,primary?Color.WHITE:GREEN,true);view.setGravity(Gravity.CENTER);view.setMinHeight(dp(48));view.setPadding(dp(12),dp(10),dp(12),dp(10));view.setBackground(rounded(primary?GREEN:Color.rgb(232,243,237),false));view.setFocusable(true);view.setClickable(true);view.setContentDescription(title);view.setOnClickListener(v->action.run());parent.addView(view,new LinearLayout.LayoutParams(-1,-2));gap(parent,8);return view;
    }
    boolean showing(){return dialog!=null&&dialog.isShowing();}
    void show(){
        if(showing()){refresh();return;}
        scroll=new ScrollView(activity);scroll.setFillViewport(true);body=column();body.setPadding(dp(18),dp(8),dp(18),dp(12));scroll.addView(body);
        dialog=new AlertDialog.Builder(activity).setTitle("Shizuku kurulum yardımcısı").setView(scroll).setNegativeButton("Daha sonra",null).create();
        dialog.setOnDismissListener(d->{handler.removeCallbacks(tick);});dialog.show();signature="";stage=-1;selected=-1;resume();
    }
    void resume(){resumed=true;if(showing()){refresh();handler.removeCallbacks(tick);handler.postDelayed(tick,1200);}}
    void pause(){resumed=false;handler.removeCallbacks(tick);}
    void close(){pause();if(dialog!=null)dialog.dismiss();}
    private boolean running(){try{return Shizuku.pingBinder()&&!Shizuku.isPreV11();}catch(RuntimeException ex){return false;}}
    void refresh(){
        if(!showing()||activity.isFinishing()||activity.isDestroyed())return;
        boolean present=installed.getAsBoolean(),started=running(),allowed=CarSilence.ready();
        boolean dev=confirmations.getBoolean("developer",false),wifi=confirmations.getBoolean("wireless",false),modern=Build.VERSION.SDK_INT>=30;
        String nextSignature=present+":"+started+":"+allowed+":"+dev+":"+wifi;
        if(nextSignature.equals(signature))return;signature=nextSignature;
        int next=ShizukuSetupState.next(present,started,allowed,dev,wifi,modern);boolean advance=stage!=next;
        stage=next;if(advance||selected<0)selected=stage;
        draw(present,started,allowed,dev,wifi,modern,advance);
    }
    private void draw(boolean present,boolean started,boolean allowed,boolean dev,boolean wifi,boolean modern,boolean focus){
        int previousY=scroll.getScrollY();body.removeAllViews();
        body.addView(text(stage==5?"Hazır · aracı seçebilirsin":"Sıradaki adım: "+(stage+1),18,GREEN,true));gap(body,8);
        body.addView(text("Yeşil çerçeve sıradaki adımı gösterir. CallGuard’a döndüğünde durum otomatik kontrol edilir. Eşleştirme kodunu yalnızca Shizuku’ya gir; CallGuard bu kodu istemez.",12,MUTED,false));gap(body,14);
        String[] titles={"Shizuku’yu kur","Geliştirici seçeneklerini etkinleştir","Kablosuz hata ayıklamayı aç","Shizuku’yu eşleştir ve başlat","CallGuard’a Shizuku izni ver","Aracını seç"};
        boolean[] done={present||started,started||dev,started||wifi,started,allowed,false};
        View focusTarget=null;
        for(int i=0;i<titles.length;i++){
            final int index=i;boolean active=i==stage;LinearLayout card=column();card.setPadding(dp(14),dp(12),dp(14),dp(8));card.setBackground(rounded(active?Color.rgb(230,246,236):Color.rgb(247,248,247),active));
            LinearLayout.LayoutParams margins=new LinearLayout.LayoutParams(-1,-2);margins.bottomMargin=dp(10);body.addView(card,margins);
            TextView title=text((done[i]?"✓ ":(i+1)+" · ")+titles[i]+(active?"  ←":""),15,INK,true);title.setFocusable(true);title.setContentDescription(titles[i]+(done[i]?", tamamlandı":active?", sıradaki adım":""));title.setOnClickListener(v->{selected=index;draw(present,started,allowed,dev,wifi,modern,true);});card.addView(title);gap(card,6);
            String status=done[i]?(i==1||i==2?started?"Shizuku çalışıyor; bu adım atlanabilir":"Sen tamamladığını belirttin":"Otomatik doğrulandı"):active?"Bu adımla devam et":"Gerektiğinde açıp inceleyebilirsin";
            card.addView(text(status,11,MUTED,false));
            if(i==selected){gap(card,12);details(i,card,modern);if(focus)focusTarget=card;}
        }
        LinearLayout help=column();body.addView(help);button(help,"Durumu yeniden kontrol et",false,()->{signature="";refresh();});
        button(help,"Resmî resimli kurulum rehberi",false,()->open(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://shizuku.rikka.app/guide/setup/"))));
        button(help,"Araç bağlantısını doğrula",false,inspect);button(help,"Bluetooth ayarları / elle geri alma",false,bluetooth);
        help.addView(text("Telefon yeniden başlarsa Shizuku’yu tekrar başlatman gerekebilir. Araç sessizliğini kapatmadan uygulamayı kaldırma veya verilerini temizleme.",12,MUTED,false));
        View target=focusTarget;scroll.post(()->{if(!showing())return;if(target!=null){scroll.smoothScrollTo(0,target.getTop());View next=target.findFocus();if(next==null&&target instanceof LinearLayout){LinearLayout group=(LinearLayout)target;for(int j=0;j<group.getChildCount();j++)if(group.getChildAt(j).isClickable()&&group.getChildAt(j).getMinimumHeight()>=dp(48)){next=group.getChildAt(j);break;}}if(next!=null){next.requestFocus();next.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);}}else scroll.scrollTo(0,previousY);});
    }
    private void manual(String key){confirmations.edit().putBoolean(key,true).apply();signature="";refresh();}
    private void details(int index,LinearLayout card,boolean modern){
        if(index==0){card.addView(text("Resmî mağazadan Yükle’ye dokun, kurulum bitince CallGuard’a dön. Kurulduğunu otomatik algılayacağız.",13,INK,false));gap(card,10);button(card,"Shizuku’yu kur",true,install);}
        else if(index==1){
            card.addView(text("Samsung: Ayarlar → Telefon hakkında → Yazılım bilgileri → Yapım numarası. Yapım numarası’na 7 kez dokun, istenirse telefon kilidini doğrula. Sonra Geliştirici seçeneklerini aç. Android bu ayarın gerçek durumunu üçüncü taraf uygulamalara gizleyebilir; tamamladığını sen belirteceksin.",13,INK,false));gap(card,10);
            button(card,"Telefon hakkında ekranını aç",true,()->open(new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)));
            button(card,"Geliştirici seçeneklerini aç",false,()->development(false));button(card,"Etkinleştirdim, devam et",false,()->manual("developer"));
        }else if(index==2){
            card.addView(text(modern?"Wi-Fi ağına bağlan. Geliştirici seçeneklerinde Kablosuz hata ayıklama satırına dokun ve anahtarı aç; Android sorarsa bu ağ için izin ver. Destekleyen telefonlarda ilgili satır vurgulanır. Samsung vurguyu göstermiyorsa ayarlar içindeki aramaya Kablosuz hata ayıklama yaz.":"Bu Android sürümünde kablosuz başlatma yok. Shizuku’nun bilgisayarla başlatma rehberini kullan.",13,INK,false));gap(card,10);
            if(modern){button(card,"Kablosuz hata ayıklama satırını bul",true,()->development(true));button(card,"Kablosuz hata ayıklamayı açtım",false,()->manual("wireless"));}
            else button(card,"Bilgisayarla başlatma rehberi",true,()->open(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://shizuku.rikka.app/guide/setup/#start-by-connecting-to-a-computer"))));
        }else if(index==3){
            card.addView(text(modern?"1. Shizuku’yu aç. Kablosuz hata ayıklama ile başlat bölümünde Eşleştirme’ye dokun.\n2. Android Kablosuz hata ayıklama ekranında Eşleştirme koduyla cihaz eşle’ye dokun.\n3. Görünen kodu Shizuku’nun eşleştirme bildirimine gir.\n4. Shizuku’ya dönüp Başlat’a dokun. Zaten eşleşmişse yeniden eşleştirmeden Başlat’ı deneyebilirsin.\n5. CallGuard’a dön; servis çalışıyorsa sonraki adım otomatik açılacak.":"Android 10 ve altında rootsuz başlatma için bilgisayar gerekir. Resmî rehberde bilgisayarla başlatma adımlarını uygula, ardından buraya dön.",13,INK,false));gap(card,10);
            button(card,"Shizuku’yu aç: Eşleştirme / Başlat",true,launch);if(modern)button(card,"Android eşleştirme ayarlarına git",false,()->development(true));
        }else if(index==4){card.addView(text("Aşağıdaki düğmeye dokun. Shizuku’nun açtığı izin penceresinde CallGuard için İzin ver’i seç. İzin durumunu otomatik doğrulayacağız.",13,INK,false));gap(card,10);button(card,"CallGuard’a Shizuku izni ver",true,authorize);}
        else {card.addView(text("Araç sessizliği için kurulum hazır olduğunda eşleşmiş aracını seçebilirsin. Bu mod beyaz listedekiler dahil aracın bütün görüşmelerini etkiler; telefonun kendisinde çağrı devam edebilir.",13,INK,false));gap(card,10);button(card,"Kurulumu bitir ve araç seç",true,()->{if(!CarSilence.ready()){Toast.makeText(activity,"Önce Shizuku’yu başlatıp CallGuard’a izin ver.",Toast.LENGTH_LONG).show();signature="";refresh();return;}dialog.dismiss();choose.run();});}
    }
    private void development(boolean wireless){
        Intent intent=new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
        if(wireless){Bundle args=new Bundle();args.putString(":settings:fragment_args_key","toggle_adb_wireless");intent.putExtra(":settings:fragment_args_key","toggle_adb_wireless").putExtra(":settings:show_fragment_args",args);}
        open(intent);
    }
    private void open(Intent intent){try{activity.startActivity(intent);}catch(ActivityNotFoundException|SecurityException ex){Toast.makeText(activity,"Bu ekran doğrudan açılamadı. Telefonun Ayarlar uygulamasından belirtilen yolu izle.",Toast.LENGTH_LONG).show();}}
}
