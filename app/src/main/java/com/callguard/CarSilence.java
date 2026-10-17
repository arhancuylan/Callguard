package com.callguard;

import android.content.*;
import android.os.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import org.json.*;

/** Multi-device local ADB controller; each configuration owns its own recovery journal. */
final class CarSilence implements AutoCloseable {
    private final Context context;private final SharedPreferences prefs;
    private final Handler main=new Handler(Looper.getMainLooper());private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Runnable changed;private final Consumer<String> message;
    private BluetoothSilenceStore configurations;private boolean busy,closed;private String storageError="";
    private final Map<String,Integer> verified=new HashMap<>();private final Set<String> unavailable=new HashSet<>();
    CarSilence(Context context,Runnable changed,Consumer<String> message){
        this.context=context.getApplicationContext();this.changed=changed;this.message=message;prefs=this.context.getSharedPreferences("car-silence",Context.MODE_PRIVATE);
        try{configurations=new BluetoothSilenceStore(new BluetoothSilenceStore.Storage(){
            public String current(){return prefs.getString("devices-v1",null);}public String legacy(){return prefs.getString("recovery",null);}
            public void write(String value,boolean removeLegacy)throws Exception{SharedPreferences.Editor editor=prefs.edit().putString("devices-v1",value);if(removeLegacy)editor.remove("recovery");if(!editor.commit())throw new java.io.IOException("Cihaz ayarları kaydedilemedi; mevcut kayıtlar korunuyor.");}
        });}catch(Exception ex){storageError="Cihaz yapılandırması okunamadı. Önceki kayıtlar korundu; Bluetooth ayarlarını kontrol et.";CallDiagnostics.record(this.context,"BT_KAYIT_OKUNAMADI: "+ex.getClass().getSimpleName());}
    }
    private void tell(String value){if(!closed)message.accept(value);}private void update(){if(!closed)changed.run();}
    boolean isBusy(){return busy;}
    List<BluetoothSilenceStore.Entry> devices(){return configurations==null?Collections.emptyList():configurations.entries();}
    boolean saved(){for(BluetoothSilenceStore.Entry entry:devices())if(entry.recovery())return true;return false;}
    boolean hasRecovery(String address){BluetoothSilenceStore.Entry entry=configurations==null?null:configurations.get(address);return entry!=null&&entry.recovery();}
    boolean silencedVerified(){for(BluetoothSilenceStore.Entry entry:devices())if(entry.recovery()&&Integer.valueOf(0).equals(verified.get(entry.address)))return true;return false;}
    boolean verifiedSilent(String address){return hasRecovery(address)&&Integer.valueOf(0).equals(verified.get(address));}
    void invalidateSetupVerification(){verified.clear();unavailable.clear();}
    String status(){
        if(!storageError.isEmpty())return storageError;if(busy)return "Cihaz ayarları kontrol ediliyor…";
        int active=0,checked=0;for(BluetoothSilenceStore.Entry entry:devices())if(entry.recovery()){active++;if(Integer.valueOf(0).equals(verified.get(entry.address)))checked++;}
        if(active>0)return active+" cihaz için sessizlik kaydı · "+checked+" doğrulandı";
        return devices().isEmpty()?"Henüz cihaz eklenmedi":devices().size()+" cihaz hatırlanıyor · CallGuard sessizliği kapalı";
    }
    String deviceStatus(BluetoothSilenceStore.Entry entry){
        if(unavailable.contains(entry.address))return "Durum doğrulanamadı · eşleşme ve görüşme desteğini kontrol et";
        Integer value=verified.get(entry.address);
        if(entry.recovery())return value==null?"Sessizlik tercihi kayıtlı · henüz doğrulanmadı":value==0?"Sessiz · görüşme profili kapalı, doğrulandı":value==100?"Görüşmeler açık · geri yükleme kaydı korunuyor":"Görüşme ayarı belirsiz · kayıt korundu";
        return value!=null&&value==0?"CallGuard sessizliği kapalı · görüşme profili zaten kapalı olabilir":"CallGuard sessizliği kapalı · cihaz yapılandırması kayıtlı";
    }
    boolean remember(String address,String name){
        if(configurations==null){tell(storageError);return false;}
        try{configurations.remember(address,name);update();return true;}catch(Exception ex){tell("Cihaz yapılandırması kaydedilemedi; mevcut kayıtlar korundu.");return false;}
    }
    void forget(String address){
        if(busy){tell("Önce devam eden işlemin bitmesini bekle.");return;}if(configurations==null){tell(storageError);return;}
        try{configurations.forget(address);verified.remove(address);unavailable.remove(address);update();tell("Bu cihazın CallGuard yapılandırması unutuldu. Bluetooth eşleştirmesi silinmedi.");}catch(Exception ex){tell(hasRecovery(address)?"Önce bu cihazın önceki görüşme ayarını geri yükle.":"Yapılandırma silinemedi; kayıtlar korundu.");}
    }
    static boolean ready(){return LocalAdb.ready();}
    private interface Task {void run()throws Exception;}
    private void execute(Task task,Runnable success){
        if(closed)return;if(busy){tell("Önce devam eden işlemin bitmesini bekle.");return;}busy=true;update();
        worker.execute(()->{String error=null;try{task.run();}catch(Exception ex){error=CarErrors.describe(ex);CallDiagnostics.record(context,"BLUETOOTH_ISLEM_HATASI: "+ex.getClass().getSimpleName());}
            final String failure=error;main.post(()->{busy=false;if(!closed){if(failure!=null)tell(failure);else if(success!=null)success.run();update();}});
        });
    }
    private void run(Task task,Runnable success){if(configurations==null){tell(storageError);return;}if(!ready()){tell("ADB kurulum ve durum bölümünden bağlantıyı aç. Cihaz yapılandırmaları korundu.");return;}execute(task,success);}
    void pair(int port,String code,Runnable success){execute(()->{if(!LocalAdb.get(context).pairLocal(port,code))throw new IllegalStateException();},success);}
    void connect(int port,Runnable success){execute(()->LocalAdb.get(context).connectLocal(port),success);}
    void disconnect(){execute(()->LocalAdb.get(context).disconnectLocal(),()->tell("ADB bağlantısı kesildi. Cihaz ayarları değiştirilmedi."));}
    private CarSilencePolicy.Backend backend(LocalAdb service){return new CarSilencePolicy.Backend(){public int read(String address)throws Exception{return service.command("read",address);}public boolean write(String address,int policy)throws Exception{return service.command("write",address,Integer.toString(policy))==1;}};}
    void list(Consumer<JSONArray> result){
        final JSONArray[] values=new JSONArray[1];execute(()->values[0]=CarDevices.load(context),()->result.accept(values[0]));
    }
    void enable(String address,String name){
        final String key=BluetoothSilenceStore.address(address);verified.remove(key);unavailable.remove(key);
        run(()->CarSilencePolicy.enable(backend(LocalAdb.get(context)),configurations.journal(key),key,name),()->{verified.put(key,0);tell(name+": görüşme sessizliği açıldı. Diğer cihazlar değiştirilmedi.");});
    }
    void restore(String address){
        final String key=BluetoothSilenceStore.address(address);verified.remove(key);unavailable.remove(key);
        run(()->{BluetoothSilenceStore.Entry entry=configurations.get(key);if(entry==null||!entry.recovery())throw new IllegalStateException();CarSilencePolicy.restore(backend(LocalAdb.get(context)),configurations.journal(key));},()->{verified.put(key,100);tell("Bu cihazın önceki görüşme ayarı geri yüklendi. Yapılandırması hatırlanıyor.");});
    }
    void verifyForSetup(Runnable finished){verify(true,finished);}
    void inspectAll(){verify(false,()->{});}
    private void verify(boolean recoveryOnly,Runnable finished){
        if(closed||busy||configurations==null||!ready()){finished.run();return;}
        List<BluetoothSilenceStore.Entry> entries=devices();busy=true;update();
        worker.execute(()->{Map<String,Integer> results=new HashMap<>();Set<String> failed=new HashSet<>();
            for(BluetoothSilenceStore.Entry entry:entries){if(recoveryOnly&&!entry.recovery())continue;try{results.put(entry.address,LocalAdb.get(context).command("read",entry.address));}catch(Exception ex){failed.add(entry.address);CallDiagnostics.record(context,"BT_CIHAZ_KONTROL: "+ex.getClass().getSimpleName());}}
            main.post(()->{busy=false;if(!closed){for(String address:failed){verified.remove(address);unavailable.add(address);}for(Map.Entry<String,Integer> entry:results.entrySet()){verified.put(entry.getKey(),entry.getValue());unavailable.remove(entry.getKey());}update();finished.run();}});
        });
    }
    @Override public void close(){closed=true;worker.shutdown();}
}
