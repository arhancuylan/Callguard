package com.callguard;

import android.content.Context;
import android.os.*;
import android.provider.Settings;
import java.util.concurrent.*;

/** Foreground-only discovery and authenticated, read-only shell checks. No Bluetooth mutation. */
final class AdbSetupMonitor implements AutoCloseable {
    private final Context context;private final Runnable changed,verified;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private LocalAdbDiscovery discovery;
    private boolean active,closed,busy,discovered;private int generation,port,tried;
    boolean wireless,connected,scanning,usb;String detail="";
    AdbSetupMonitor(Context context,Runnable changed,Runnable verified){this.context=context.getApplicationContext();this.changed=changed;this.verified=verified;}
    void resume(){
        if(closed||active)return;active=true;generation++;connected=false;discovered=false;port=0;tried=0;scanning=true;detail="Mevcut eşleştirme ve bağlantı kontrol ediliyor…";
        wireless=wifiFlag();
        usb=ShellBridge.ready();if(usb){attempt();changed.run();return;}
        discovery=new LocalAdbDiscovery(context,false,value->{if(!active)return;port=value;discovered=value>0;if(value>0){wireless=true;attempt();}else{connected=LocalAdb.ready()&&connected;changed.run();}});
        if(LocalAdb.ready())attempt();
        else{int previous=context.getSharedPreferences("local-adb",0).getInt("port",0);if(previous>0){port=previous;attempt();}}
        final int token=generation;
        main.postDelayed(()->{if(active&&token==generation&&!busy){scanning=false;if(!connected)detail=wireless?"Kablosuz hata ayıklama açık; eşleştirme/bağlantı bekleniyor.":"Kablosuz hata ayıklama henüz doğrulanamadı.";changed.run();}},5000);
        changed.run();
    }
    private boolean wifiFlag(){try{return Settings.Global.getInt(context.getContentResolver(),"adb_wifi_enabled",0)==1;}catch(SecurityException ex){return false;}}
    void poll(){if(!active)return;wireless=wifiFlag()||discovered||(connected&&!usb);if(connected&&(usb?!ShellBridge.ready():!LocalAdb.ready())){connected=false;detail="Yetkili hizmet bağlantısı kesildi. Yeniden kontrol et.";}if(ShellBridge.ready()&&!connected&&!busy)attempt();changed.run();}
    private void attempt(){
        if(!active||busy||(!HfpConnection.ready()&&(port<=0||port==tried)))return;
        int target=port,token=generation;tried=target;busy=true;scanning=true;detail="Kayıtlı CallGuard eşleştirmesi doğrulanıyor…";changed.run();
        worker.execute(()->{
            boolean success=false,usingUsb=ShellBridge.ready();try{if(usingUsb){if(ShellBridge.command(ShellWire.PROBE,null,0)!=2000)throw new SecurityException();}else{LocalAdb adb=LocalAdb.get(context);if(LocalAdb.ready())adb.verifyLocal();else adb.connectLocal(target);}success=true;}catch(Exception ex){CallDiagnostics.record(context,"ADB_KURULUM_KONTROL: "+ex.getClass().getSimpleName());}
            final boolean ok=success,viaUsb=usingUsb;main.post(()->{
                busy=false;if(closed||!active)return;
                if(token!=generation){attempt();return;}
                connected=ok;usb=ok&&viaUsb;scanning=false;if(ok){if(!usb)wireless=true;detail=usb?"USB yardımcısı doğrulandı. Wi-Fi ve USB kablosu çalışma sırasında gerekmez; yeniden başlatınca USB ile tekrar başlat.":"Yetkili ADB bağlantısı doğrulandı.";verified.run();}else detail="Bağlantı doğrulanamadı. USB yardımcısını başlat veya kablosuz ADB bağlantısını kontrol et.";
                changed.run();if(!ok&&port>0&&port!=tried)attempt();
            });
        });
    }
    void pause(){active=false;generation++;if(discovery!=null){discovery.close();discovery=null;}main.removeCallbacksAndMessages(null);}
    void rescan(){pause();resume();}
    void disconnectRequested(){pause();connected=false;scanning=false;detail="ADB bağlantısı senin isteğinle kesiliyor. Araç ayarı değiştirilmez.";changed.run();}
    public void close(){closed=true;pause();worker.shutdown();}
}
