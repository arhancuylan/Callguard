package com.callguard;

import android.app.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.*;
import java.util.function.BiConsumer;

/** Remembered device configurations, with independent explicit controls and truthful live status. */
final class BluetoothDevicesDialog {
    private final Activity activity;private final CarSilence controller;private final Runnable add,setup;private final BiConsumer<String,String> enable;
    private AlertDialog dialog;private LinearLayout body;private ScrollView scroll;private String signature="";
    BluetoothDevicesDialog(Activity activity,CarSilence controller,Runnable add,Runnable setup,BiConsumer<String,String> enable){this.activity=activity;this.controller=controller;this.add=add;this.setup=setup;this.enable=enable;}
    boolean showing(){return dialog!=null&&dialog.isShowing();}
    void show(){
        if(showing()){refresh();return;}body=column();scroll=new ScrollView(activity);scroll.addView(body);dialog=new AlertDialog.Builder(activity).setTitle("Bluetooth cihazlarım").setView(scroll).setNegativeButton("Kapat",null).create();dialog.show();signature="";refresh();
        resume();
    }
    void resume(){if(showing()){controller.invalidateSetupVerification();refresh();if(CarSilence.ready())controller.verifyForSetup(this::refresh);}}
    void refresh(){
        if(!showing()||activity.isFinishing()||activity.isDestroyed())return;
        StringBuilder key=new StringBuilder(controller.status()).append(controller.isBusy()).append(CarSilence.ready());
        for(BluetoothSilenceStore.Entry entry:controller.devices())key.append(entry.address).append(entry.name).append(entry.recovery()).append(controller.deviceStatus(entry));
        if(key.toString().equals(signature))return;signature=key.toString();int y=scroll.getScrollY();body.removeAllViews();
        label(body,controller.status(),15,0xff08634e,true);
        label(body,"Araç, kulaklık ve görüşme destekli diğer cihazları ayrı yönet. Bu ayar seçilen cihazdaki tüm görüşmeleri etkiler; müzik profilini değiştirmez. Saat uygulamasının ilettiği ekran bildirimlerini engellemeyebilir.",12,0xff65736d,false);
        button(body,"+ Eşleşmiş cihaz ekle",true,add);
        button(body,CarSilence.ready()?"Cihaz durumlarını yenile":"ADB kurulum ve durum",false,()->{if(CarSilence.ready())controller.inspectAll();else setup.run();});
        if(controller.devices().isEmpty())label(body,"Henüz cihaz eklenmedi. Yukarıdan telefonla eşleşmiş cihazlarından birini seç.",14,0xff65736d,false);
        for(BluetoothSilenceStore.Entry entry:controller.devices()){
            LinearLayout card=column();card.setPadding(dp(12),dp(10),dp(12),dp(12));GradientDrawable background=new GradientDrawable();background.setColor(controller.verifiedSilent(entry.address)?0xffe7f3eb:0xfff3f5f3);background.setCornerRadius(dp(14));card.setBackground(background);
            LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(-1,-2);layout.topMargin=dp(12);body.addView(card,layout);
            label(card,entry.name,17,0xff14221d,true);label(card,entry.address,11,0xff65736d,false);
            label(card,controller.deviceStatus(entry),13,controller.verifiedSilent(entry.address)?0xff08634e:0xff65736d,false);
            if(entry.recovery())button(card,"Sessizliği kapat · önceki ayarı geri yükle",true,()->controller.restore(entry.address));
            else{
                button(card,"Görüşme sessizliğini aç",true,()->enable.accept(entry.address,entry.name));
                button(card,"Bu yapılandırmayı unut",false,()->new AlertDialog.Builder(activity).setTitle("Bu cihaz unutulsun mu?").setMessage(entry.name+" için CallGuard yapılandırması kaldırılır. Telefonun Bluetooth eşleştirmesi silinmez.").setNegativeButton("Vazgeç",null).setPositiveButton("Unut",(d,w)->controller.forget(entry.address)).show());
            }
        }
        label(body,"Yapılandırmalar bu telefonda saklanır. Bağlı olmayan veya görüşme profili desteklemeyen cihazlarda işlem doğrulanamayabilir. Uygulamayı silmeden önce sessiz cihazların önceki ayarlarını geri yükle.",12,0xff65736d,false);
        scroll.post(()->{if(showing())scroll.scrollTo(0,y);});
    }
    private int dp(int n){return Math.round(n*activity.getResources().getDisplayMetrics().density);}
    private LinearLayout column(){LinearLayout view=new LinearLayout(activity);view.setOrientation(LinearLayout.VERTICAL);view.setPadding(dp(16),dp(8),dp(16),dp(12));return view;}
    private void label(LinearLayout parent,String text,int size,int color,boolean bold){TextView view=new TextView(activity);view.setText(text);view.setTextSize(size);view.setTextColor(color);view.setPadding(0,dp(4),0,dp(5));if(bold)view.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));parent.addView(view);}
    private void button(LinearLayout parent,String text,boolean primary,Runnable action){TextView view=new TextView(activity);view.setText(text);view.setTextSize(14);view.setTextColor(primary?Color.WHITE:0xff08634e);view.setGravity(Gravity.CENTER);view.setMinHeight(dp(46));view.setPadding(dp(10),dp(10),dp(10),dp(10));GradientDrawable background=new GradientDrawable();background.setColor(primary?0xff08634e:0xffe6eee8);background.setCornerRadius(dp(12));view.setBackground(background);view.setFocusable(true);view.setClickable(true);view.setEnabled(!controller.isBusy());view.setAlpha(controller.isBusy()?0.5f:1f);view.setOnClickListener(v->action.run());LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(-1,-2);layout.topMargin=dp(7);parent.addView(view,layout);}
    void close(){if(dialog!=null)dialog.dismiss();}
}
