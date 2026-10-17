package com.callguard;

import android.content.*;
import android.os.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import org.json.*;

/** Built-in local ADB client. Its local recovery journal is deliberately excluded from cloud/Drive. */
final class CarSilence implements AutoCloseable {
    private final Context context;
    private final SharedPreferences prefs;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Runnable changed;
    private final Consumer<String> message;
    private boolean busy,closed;
    private String verified="";
    private final CarSilencePolicy.Journal journal=new CarSilencePolicy.Journal(){
        public CarSilencePolicy.Saved load()throws Exception{
            String raw=prefs.getString("recovery",null);if(raw==null)return null;JSONObject value=new JSONObject(raw);
            return new CarSilencePolicy.Saved(value.getString("address"),value.getString("name"),value.getInt("previous"));
        }
        public void save(CarSilencePolicy.Saved value)throws Exception{
            String raw=new JSONObject().put("address",value.address).put("name",value.name).put("previous",value.previous).toString();
            if(!prefs.edit().putString("recovery",raw).commit())throw new IllegalStateException("Geri yükleme kaydı yazılamadı. Araç ayarı değiştirilmedi.");
        }
        public void clear(){if(!prefs.edit().remove("recovery").commit())throw new IllegalStateException("Araç ayarı geri yüklendi, kayıt temizlenemedi. Yeniden kontrol et.");}
    };
    CarSilence(Context context,Runnable changed,Consumer<String> message){
        this.context=context.getApplicationContext();this.changed=changed;this.message=message;
        prefs=this.context.getSharedPreferences("car-silence",Context.MODE_PRIVATE);

    }
    private void tell(String value){if(!closed)message.accept(value);}
    private void update(){if(!closed)changed.run();}
    boolean saved(){return prefs.contains("recovery");}
    String status(){
        if(busy)return "Araç ayarı kontrol ediliyor…";
        if(!verified.isEmpty())return verified;
        return saved()?"Geri yükleme kaydı var · araç durumu henüz doğrulanmadı":"Kapalı · araç ayarı değiştirilmedi";
    }
    static boolean ready(){return LocalAdb.ready();}
    private interface Job {void run(LocalAdb service)throws Exception;}
    private void run(Job job){
        if(closed)return;if(busy){tell("Önce devam eden işlemin bitmesini bekle.");return;}
        if(!ready()){tell("Kurulum ve durum bölümünden yerel ADB bağlantısını aç. Varsa geri yükleme kaydı korundu.");return;}
        execute(()->job.run(LocalAdb.get(context)),null);
    }
    private interface Task {void run()throws Exception;}
    private void execute(Task task,Runnable success){
        if(closed||busy)return;busy=true;verified="";update();
        worker.execute(()->{
            String error=null;try{task.run();}catch(Exception ex){error=CarErrors.describe(ex);CallDiagnostics.record(context,"YEREL_ADB_HATASI: "+ex.getClass().getSimpleName());}
            final String failure=error;main.post(()->{busy=false;if(!closed){if(failure!=null)tell(failure);else if(success!=null)success.run();update();}});
        });
    }
    void pair(int port,String code,Runnable success){execute(()->{if(!LocalAdb.get(context).pairLocal(port,code))throw new IllegalStateException();},success);}
    void connect(int port,Runnable success){execute(()->LocalAdb.get(context).connectLocal(port),success);}
    void disconnect(){execute(()->LocalAdb.get(context).disconnectLocal(),()->tell("Yerel ADB bağlantısı kapatıldı. Araç ayarı değiştirilmedi."));}
    private CarSilencePolicy.Backend backend(LocalAdb service){return new CarSilencePolicy.Backend(){public int read(String address)throws Exception{return service.command("read",address);}public boolean write(String address,int policy)throws Exception{return service.command("write",address,Integer.toString(policy))==1;}};}
    void list(Consumer<JSONArray> result){
        if(closed)return;if(busy){tell("Önce devam eden işlemin bitmesini bekle.");return;}
        busy=true;update();worker.execute(()->{
            JSONArray devices=null;String error=null;
            try{devices=CarDevices.load(context);}catch(Exception ex){error=CarErrors.describe(ex);CallDiagnostics.record(context,"ARAC_LISTE_HATASI: "+ex.getClass().getSimpleName());}
            final JSONArray found=devices;final String failure=error;
            main.post(()->{busy=false;update();if(!closed){if(failure!=null)tell(failure);else if(found!=null)result.accept(found);}});
        });
    }
    void enable(String address,String name){run(service->{CarSilencePolicy.enable(backend(service),journal,address,name);main.post(()->{verified="Görüşme profili kapalı · "+name;tell("Araç görüşme profili kapatıldı. Müzik profili değiştirilmedi.");});});}
    void restore(){run(service->{CarSilencePolicy.restore(backend(service),journal);main.post(()->{verified="Kapalı · önceki görüşme ayarı geri yüklendi";tell("Önceki görüşme ayarı geri yüklendi. Gerekirse araçtan yeniden bağlan.");});});}
    void inspect(){run(service->{CarSilencePolicy.Saved value=journal.load();String status="Kapalı · geri yükleme kaydı yok";if(value!=null){int policy=service.command("read",value.address);int state=service.command("state",value.address);status=(policy==0?"Görüşme profili kapalı":policy==100?"Görüşme profili açık":"Görüşme ayarı belirsiz")+" · "+value.name+"\nHFP bağlantısı: "+(state==0?"kesik":state==1?"bağlanıyor":state==2?"bağlı":"kesiliyor")+" · geri yükleme kaydı saklanıyor";}final String found=status;main.post(()->verified=found);});}
    @Override public void close(){closed=true;worker.shutdown();}
}
