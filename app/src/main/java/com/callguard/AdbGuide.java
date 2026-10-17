package com.callguard;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.Arrays;
import java.util.function.Consumer;

/** Evidence-based setup checklist with a single next action and foreground-only checks. */
final class AdbGuide {
    private static final int GREEN=0xff08634e,RED=0xffb3261e,INK=0xff14221d,MUTED=0xff65736d;
    private final Activity activity;private final CarSilence car;private final Runnable choose,devices;private final Consumer<String> message;
    private final AdbSetupMonitor monitor;private final Handler main=new Handler(Looper.getMainLooper());
    private AlertDialog dialog;private LinearLayout panel;private ScrollView scroll;
    private boolean resumed,developerConfirmed;private String signature="";
    private final Runnable tick=new Runnable(){public void run(){if(resumed&&showing()){monitor.poll();refresh();main.postDelayed(this,1200);}}};
    AdbGuide(Activity activity,CarSilence car,Runnable choose,Runnable devices,Consumer<String> message){
        this.activity=activity;this.car=car;this.choose=choose;this.devices=devices;this.message=message;
        monitor=new AdbSetupMonitor(activity,this::refresh,()->car.verifyForSetup(this::refresh));
    }
    boolean showing(){return dialog!=null&&dialog.isShowing();}
    void show(){
        if(showing()){refresh();return;}
        panel=column();scroll=new ScrollView(activity);scroll.addView(panel);
        dialog=new AlertDialog.Builder(activity).setTitle("Bluetooth sessizliği kurulumu").setView(scroll).setNegativeButton("Kapat",null).create();
        dialog.setOnDismissListener(d->{pause();});dialog.show();signature="";resume();
    }
    void refresh(){
        if(!showing()||activity.isFinishing()||activity.isDestroyed())return;
        boolean connected=monitor.connected&&LocalAdb.ready();
        AdbSetupState state=new AdbSetupState(developerConfirmed,monitor.wireless,LocalAdb.pairKnown(),connected,car.silencedVerified());
        String key=Arrays.toString(state.done)+monitor.scanning+monitor.detail+car.saved()+car.status();if(key.equals(signature))return;signature=key;
        int y=scroll.getScrollY();panel.removeAllViews();
        TextView progress=label(panel,state.completed()+" / 5 adım tamamlandı",19,GREEN,true);progress.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        label(panel,monitor.detail.isEmpty()?"Mevcut ayarların otomatik kontrol edilir.":monitor.detail,13,MUTED,false);
        int next=state.next();String[] actions={"1 · Geliştirici seçeneklerini hazırla","2 · Kablosuz hata ayıklamayı aç","3 · Bildirimden kodla eşleştir","4 · Bağlantıyı otomatik kontrol et","5 · Bluetooth cihazlarını yönet","Tamam · kurulumu kapat"};
        button(panel,actions[next],true,()->advance(next));
        String[] titles={"Geliştirici seçenekleri","Kablosuz hata ayıklama","CallGuard eşleştirmesi","Yetkili ADB bağlantısı","Bluetooth görüşme sessizliği"};
        String[] notes={
            state.done[0]?(developerConfirmed&&!monitor.wireless&&!connected?"Açık olduğunu sen onayladın.":"Kablosuz ADB üzerinden doğrulandı."):"Henüz doğrulanamadı. Telefon hakkında → Yazılım bilgileri → Yapım numarası: 7 kez dokun.",
            state.done[1]?"Bu telefonda kablosuz ADB açık.":"Geliştirici seçeneklerinde Kablosuz hata ayıklamayı aç.",
            state.done[2]?"CallGuard kimliğiyle eşleştirme doğrulandı.":"Android’in kod penceresi açıkken bildirimde Kod gir düğmesini kullan.",
            state.done[3]?"Bağlantı ve ADB yetkisi canlı olarak doğrulandı.":"Kayıtlı eşleştirme otomatik denenir. Gerekirse bağlantı portunu elle gir.",
            state.done[4]?"En az bir cihazın sessiz olduğu doğrulandı. Cihazlarını ayrı yönetebilirsin.":car.saved()?car.status()+". Cihazlarım ekranından her birini ayrı kontrol edebilirsin.":"Cihaz ekle. Her cihazın sessizliğini ayrı açıp kapatabilirsin."
        };
        for(int i=0;i<titles.length;i++)step(panel,titles[i],notes[i],state.done[i],next==i);
        if(next==0){
            label(panel,"Android geliştirici menüsünün durumunu her telefonda doğrudan bildirmez. Kablosuz ADB bulunduğunda bu adım otomatik tamamlanır.",12,MUTED,false);
            button(panel,"Geliştirici seçenekleri zaten açık",false,()->{developerConfirmed=true;signature="";refresh();});
        }
        if(!state.done[3]){
            button(panel,"Durumu yeniden kontrol et",false,monitor::rescan);
            button(panel,"Bağlantı portunu elle gir",false,this::connect);
        }
        button(panel,"Bluetooth cihazlarımı yönet",false,()->{dialog.dismiss();devices.run();});
        if(state.done[3])button(panel,"ADB bağlantısını kes",false,()->{monitor.disconnectRequested();car.disconnect();});
        label(panel,"Sessizlik seçtiğin cihazın bütün Bluetooth görüşmelerini etkiler; müzik profili değişmez. Uygulamayı silmeden önce cihazların önceki ayarlarını geri yükle.",12,MUTED,false);
        scroll.post(()->{if(showing())scroll.scrollTo(0,y);});
    }
    private void advance(int next){
        switch(next){
            case 0:open(new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS));break;
            case 1:wireless();break;
            case 2:if(Build.VERSION.SDK_INT<30)message.accept("Kablosuz eşleştirme için Android 11 veya üzeri gerekir.");else ((MainActivity)activity).startNotificationPairing();break;
            case 3:monitor.rescan();break;
            case 4:dialog.dismiss();choose.run();break;
            default:dialog.dismiss();
        }
    }
    private void step(LinearLayout parent,String title,String note,boolean done,boolean current){
        LinearLayout row=new LinearLayout(activity);row.setOrientation(LinearLayout.HORIZONTAL);row.setPadding(dp(12),dp(10),dp(12),dp(10));row.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable background=rounded(done?0xffe7f3eb:0xffffeeee);background.setStroke(dp(current?2:1),done?0xffb7d9c4:current?RED:0xfff1d2d0);row.setBackground(background);
        TextView mark=new TextView(activity);mark.setText(done?"✓":"✕");mark.setTextColor(done?GREEN:RED);mark.setTextSize(25);mark.setGravity(Gravity.CENTER);row.addView(mark,new LinearLayout.LayoutParams(dp(35),-2));
        LinearLayout text=new LinearLayout(activity);text.setOrientation(LinearLayout.VERTICAL);label(text,title,15,done?GREEN:RED,true);label(text,note,12,INK,false);row.addView(text,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(-1,-2);layout.topMargin=dp(8);parent.addView(row,layout);row.setContentDescription(title+": "+(done?"tamamlandı":"bekliyor")+". "+note);
    }
    private int dp(int n){return Math.round(n*activity.getResources().getDisplayMetrics().density);}
    private LinearLayout column(){LinearLayout view=new LinearLayout(activity);view.setOrientation(LinearLayout.VERTICAL);view.setPadding(dp(16),dp(8),dp(16),dp(12));return view;}
    private GradientDrawable rounded(int color){GradientDrawable background=new GradientDrawable();background.setColor(color);background.setCornerRadius(dp(14));return background;}
    private TextView label(LinearLayout parent,String value,int size,int color,boolean bold){TextView view=new TextView(activity);view.setText(value);view.setTextSize(size);view.setTextColor(color);view.setPadding(0,dp(3),0,dp(4));if(bold)view.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));parent.addView(view);return view;}
    private void button(LinearLayout parent,String value,boolean primary,Runnable action){TextView view=new TextView(activity);view.setText(value);view.setTextSize(15);view.setTextColor(primary?Color.WHITE:GREEN);view.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));view.setGravity(Gravity.CENTER);view.setPadding(dp(12),dp(12),dp(12),dp(12));view.setMinHeight(dp(48));view.setBackground(rounded(primary?GREEN:0xffeaf1ed));view.setFocusable(true);view.setClickable(true);view.setOnClickListener(v->action.run());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(8);parent.addView(view,p);}
    void resume(){boolean previouslyResumed=resumed;resumed=true;if(showing()){if(!previouslyResumed)car.invalidateSetupVerification();monitor.resume();refresh();main.removeCallbacks(tick);main.postDelayed(tick,1200);}}
    void pause(){resumed=false;main.removeCallbacks(tick);monitor.pause();}
    void close(){pause();monitor.close();if(dialog!=null)dialog.dismiss();}
    private void wireless(){Intent intent=new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).putExtra(":settings:fragment_args_key","toggle_adb_wireless");Bundle args=new Bundle();args.putString(":settings:fragment_args_key","toggle_adb_wireless");intent.putExtra(":settings:show_fragment_args",args);open(intent);}
    private void open(Intent intent){try{activity.startActivity(intent);}catch(ActivityNotFoundException|SecurityException ex){try{activity.startActivity(new Intent(Settings.ACTION_SETTINGS));}catch(ActivityNotFoundException|SecurityException ignored){message.accept("Telefonun Ayarlar uygulamasını aç.");}}}
    private void connect(){
        LinearLayout entry=column();label(entry,"Kablosuz hata ayıklamanın ana ekranındaki bağlantı portunu gir. Eşleştirme kodunun yanındaki portu kullanma.",13,INK,false);EditText port=new EditText(activity);port.setHint("Bağlantı portu");port.setSingleLine(true);port.setInputType(InputType.TYPE_CLASS_NUMBER);entry.addView(port);
        int previous=activity.getSharedPreferences("local-adb",0).getInt("port",0);if(previous>0)port.setText(Integer.toString(previous));
        AlertDialog input=new AlertDialog.Builder(activity).setTitle("Yerel bağlantı portu").setView(entry).setNegativeButton("Vazgeç",null).setPositiveButton("Bağlan",null).create();input.show();
        input.getButton(-1).setOnClickListener(v->{try{int value=CarCommandProtocol.port(port.getText().toString());car.connect(value,()->{if(!activity.isDestroyed()){input.dismiss();monitor.rescan();}});}catch(IllegalArgumentException ex){message.accept("Geçerli bağlantı portunu gir.");}});
    }
}
