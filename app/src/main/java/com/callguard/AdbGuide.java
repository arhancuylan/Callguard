package com.callguard;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.Build;
import android.provider.Settings;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.function.Consumer;

/** Pair inside CallGuard; Settings remains the authority for enabling developer/ADB access. */
final class AdbGuide {
    private final Activity activity;
    private final CarSilence car;
    private final Runnable choose;
    private final Consumer<String> message;
    private AlertDialog dialog;
    private TextView status;
    AdbGuide(Activity activity,CarSilence car,Runnable choose,Consumer<String> message){this.activity=activity;this.car=car;this.choose=choose;this.message=message;}
    boolean showing(){return dialog!=null&&dialog.isShowing();}
    void show(){
        if(showing()){refresh();return;}
        LinearLayout panel=column();status=label(panel,"",15);refresh();
        label(panel,"Ek uygulama ve root gerekmez. Android 11 ve üzerinde Wi-Fi bağlantısıyla kurulur. Yeniden başlatmadan sonra tekrar bağlantı gerekebilir. Eşleştirme izni Android tarafından verilir.",13);
        button(panel,"1 · Geliştirici seçeneklerini aç",()->open(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)));
        label(panel,"Menü açılmazsa Telefon hakkında → Yazılım bilgileri → Yapım numarası üzerine 7 kez dokun.",12);
        button(panel,"2 · Kablosuz hata ayıklamayı aç",this::wireless);
        label(panel,"Ayarlar ve CallGuard’ı bölünmüş ekranda aç. Kablosuz hata ayıklama → Eşleştirme koduyla cihaz eşleştir ekranını açık tut. Bu pencereyi kapatmak kodu geçersiz kılabilir.",12);
        button(panel,"3 · Kodla CallGuard’ı eşleştir",this::pair);
        button(panel,"4 · Yerel ADB bağlantısını aç",this::connect);
        label(panel,"Eşleştirme portu ile ana ekrandaki IP adresi ve bağlantı noktası farklıdır. Bağlantı açarken ana ekrandaki portu kullan. Yalnızca bu telefona bağlanılır; IP adresi girmen gerekmez.",12);
        button(panel,"5 · Araç seç ve sessizliği aç",()->{if(!CarSilence.ready()){message.accept("Önce yerel ADB bağlantısını aç.");return;}dialog.dismiss();choose.run();});
        button(panel,"Araç ayarını kontrol et",car::inspect);
        button(panel,"ADB bağlantısını kes",car::disconnect);
        label(panel,"Bağlantıyı kesmek araç ayarını geri açmaz. Önceki ayarı Araç sessizliği → Kapat ve önceki ayarı geri yükle ile açabilirsin. ADB kullanılamıyorsa Bluetooth → araç → Aramalar seçeneğini elle aç. CallGuard’ı silmeden önce geri yükle.",12);
        if(Build.VERSION.SDK_INT<30)label(panel,"Bu telefonda uygulama içinden kablosuz eşleştirme desteklenmiyor. Normal WhatsApp koruması çalışmaya devam eder.",13);
        ScrollView scroll=new ScrollView(activity);scroll.addView(panel);
        dialog=new AlertDialog.Builder(activity).setTitle("CallGuard ADB kurulumu").setView(scroll).setNegativeButton("Kapat",null).create();dialog.show();
    }
    private LinearLayout column(){LinearLayout panel=new LinearLayout(activity);panel.setOrientation(LinearLayout.VERTICAL);int pad=dp(18);panel.setPadding(pad,dp(8),pad,dp(12));return panel;}
    private int dp(int n){return Math.round(n*activity.getResources().getDisplayMetrics().density);}
    private TextView label(LinearLayout panel,String value,int size){TextView text=new TextView(activity);text.setText(value);text.setTextSize(size);text.setTextColor(Color.rgb(30,60,50));text.setPadding(0,dp(6),0,dp(10));panel.addView(text);return text;}
    private void button(LinearLayout panel,String value,Runnable action){Button button=new Button(activity);button.setText(value);button.setAllCaps(false);button.setOnClickListener(v->action.run());panel.addView(button,new LinearLayout.LayoutParams(-1,-2));}
    void refresh(){if(status!=null)status.setText(CarSilence.ready()?"● CallGuard yerel ADB bağlantısı açık":"○ Yerel ADB bağlantısı kapalı");}
    void resume(){refresh();}
    void pause(){}
    void close(){if(dialog!=null)dialog.dismiss();}
    private void wireless(){
        Intent intent=new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
        intent.putExtra(":settings:fragment_args_key","toggle_adb_wireless");android.os.Bundle args=new android.os.Bundle();args.putString(":settings:fragment_args_key","toggle_adb_wireless");intent.putExtra(":settings:show_fragment_args",args);open(intent);
    }
    private void open(Intent intent){try{activity.startActivity(intent);}catch(ActivityNotFoundException|SecurityException ex){try{activity.startActivity(new Intent(Settings.ACTION_SETTINGS));}catch(ActivityNotFoundException|SecurityException ignored){message.accept("Telefonun Ayarlar uygulamasını aç.");}}}
    private EditText input(LinearLayout panel,String hint,boolean code){EditText view=new EditText(activity);view.setHint(hint);view.setSingleLine(true);view.setInputType(InputType.TYPE_CLASS_NUMBER|(code?InputType.TYPE_NUMBER_VARIATION_PASSWORD:0));view.setSaveEnabled(false);view.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);panel.addView(view,new LinearLayout.LayoutParams(-1,-2));return view;}
    private void pair(){
        if(Build.VERSION.SDK_INT<30){message.accept("Kablosuz eşleştirme için Android 11 veya üzeri gerekir.");return;}
        LinearLayout panel=column();label(panel,"Android’in eşleştirme penceresindeki portu ve 6 haneli kodu gir. Eşleştirme ekranı açık kalsın. Kod kaydedilmez.",13);
        EditText port=input(panel,"Eşleştirme portu",false),code=input(panel,"6 haneli eşleştirme kodu",true);
        AlertDialog entry=new AlertDialog.Builder(activity).setTitle("CallGuard’ı bu telefonla eşleştir").setView(panel).setNegativeButton("Vazgeç",null).setPositiveButton("Eşleştir",null).create();entry.show();entry.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        entry.getButton(-1).setOnClickListener(v->{try{
            int number=CarCommandProtocol.port(port.getText().toString());String pin=code.getText().toString().trim();if(!pin.matches("[0-9]{6}"))throw new IllegalArgumentException();code.setText("");
            car.pair(number,pin,()->{if(!activity.isDestroyed()){entry.dismiss();message.accept("CallGuard eşleşti. Şimdi ana kablosuz hata ayıklama ekranındaki bağlantı portuyla bağlantıyı aç.");refresh();}});
        }catch(IllegalArgumentException ex){message.accept("Geçerli port ve 6 haneli kod gir.");}});
    }
    private void connect(){
        LinearLayout panel=column();label(panel,"Kablosuz hata ayıklamanın ana ekranındaki IP adresi ve bağlantı noktası satırında, iki noktadan sonraki sayıyı gir. Eşleştirme portunu kullanma.",13);EditText port=input(panel,"Bağlantı portu",false);
        int previous=activity.getSharedPreferences("local-adb",0).getInt("port",0);if(previous>0)port.setText(Integer.toString(previous));
        AlertDialog entry=new AlertDialog.Builder(activity).setTitle("Bu telefona bağlan").setView(panel).setNegativeButton("Vazgeç",null).setPositiveButton("Bağlan",null).create();entry.show();
        entry.getButton(-1).setOnClickListener(v->{try{int number=CarCommandProtocol.port(port.getText().toString());car.connect(number,()->{if(!activity.isDestroyed()){entry.dismiss();refresh();message.accept("CallGuard ADB bağlantısı doğrulandı. Aracını seçebilirsin.");}});}catch(IllegalArgumentException ex){message.accept("Geçerli bağlantı portunu gir.");}});
    }
}
