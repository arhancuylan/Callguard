package com.callguard;

import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import org.json.*;
import rikka.shizuku.Shizuku;

/** Optional Shizuku client. Its local recovery journal is deliberately excluded from cloud/Drive. */
final class CarSilence implements AutoCloseable {
    private final Context context;
    private final SharedPreferences prefs;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Runnable changed;
    private final Consumer<String> message;
    private final Shizuku.UserServiceArgs args;
    private ICarProfile remote;
    private boolean busy,closed,bound;
    private String verified="";
    private Runnable waiting;
    private final Runnable timeout=()->{if(waiting!=null){waiting=null;busy=false;verified="";tell("Shizuku servisi bağlanamadı. Tekrar dene; geri yükleme kaydı korunuyor.");update();}};
    private final Shizuku.OnRequestPermissionResultListener permission=(code,result)->{
        if(code==892){tell(result==PackageManager.PERMISSION_GRANTED?"Shizuku izni verildi. Araç sessizliği bölümünden devam edebilirsin.":"Shizuku izni verilmedi; araç ayarı değiştirilmedi.");update();}
    };
    private final Shizuku.OnBinderDeadListener dead=()->main.post(()->{remote=null;verified="";if(waiting!=null){waiting=null;busy=false;}tell("Shizuku bağlantısı kesildi. Araç ayarı kendiliğinden geri dönmeyebilir; geri yükleme kaydı korundu.");update();});
    private final ServiceConnection connection=new ServiceConnection(){
        @Override public void onServiceConnected(ComponentName name,IBinder binder){main.post(()->{
            remote=ICarProfile.Stub.asInterface(binder);main.removeCallbacks(timeout);
            Runnable task=waiting;waiting=null;if(task!=null&&!closed)task.run();else if(closed)disconnect();
        });}
        @Override public void onServiceDisconnected(ComponentName name){main.post(()->{remote=null;verified="";update();});}
    };
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
        args=new Shizuku.UserServiceArgs(new ComponentName(this.context,CarProfileService.class)).tag("callguard-car-hfp").version(BuildConfig.VERSION_CODE).daemon(false).debuggable(false).processNameSuffix("car_profile");
        Shizuku.addRequestPermissionResultListener(permission);Shizuku.addBinderDeadListener(dead);
    }
    private void tell(String value){if(!closed)message.accept(value);}
    private void update(){if(!closed)changed.run();}
    boolean saved(){return prefs.contains("recovery");}
    String status(){
        if(busy)return "Araç ayarı kontrol ediliyor…";
        if(!verified.isEmpty())return verified;
        return saved()?"Geri yükleme kaydı var · araç durumu henüz doğrulanmadı":"Kapalı · araç ayarı değiştirilmedi";
    }
    static boolean ready(){try{return Shizuku.pingBinder()&&!Shizuku.isPreV11()&&Shizuku.checkSelfPermission()==PackageManager.PERMISSION_GRANTED;}catch(RuntimeException ex){return false;}}
    void authorize(){
        try{
            if(!Shizuku.pingBinder()){tell("Önce Shizuku’yu kurup başlat. Bu özellik isteğe bağlıdır.");return;}
            if(Shizuku.isPreV11()){tell("Shizuku sürümünü güncelle.");return;}
            if(Shizuku.checkSelfPermission()==PackageManager.PERMISSION_GRANTED){tell("Shizuku izni zaten açık.");return;}
            if(Shizuku.shouldShowRequestPermissionRationale()){tell("Shizuku uygulamasındaki Yetkilendirilmiş uygulamalar bölümünden CallGuard’a izin ver.");return;}
            Shizuku.requestPermission(892);
        }catch(RuntimeException ex){tell("Shizuku’ya erişilemedi. Shizuku’nun çalıştığını kontrol et.");}
    }
    private interface Job {void run(ICarProfile service)throws Exception;}
    private void run(Job job){
        if(closed)return;if(busy){tell("Önce devam eden işlemin bitmesini bekle.");return;}
        if(!ready()){tell("Shizuku’yu başlat ve CallGuard için izin ver. Araç ayarı değişmedi; varsa geri yükleme kaydı korundu.");return;}
        busy=true;verified="";update();
        Runnable execute=()->{final ICarProfile service=remote;worker.execute(()->{
            String error=null;try{if(service==null)throw new IllegalStateException("Shizuku servisi bağlı değil.");job.run(service);}catch(Exception ex){error=CarErrors.describe(ex);CallDiagnostics.record(context,"ARAC_KONTROL_HATASI: "+ex.getClass().getSimpleName());}
            final String failure=error;main.post(()->{busy=false;if(failure!=null)tell(failure);update();if(closed)disconnect();});
        });};
        if(remote!=null&&remote.asBinder().isBinderAlive()){execute.run();return;}
        waiting=execute;
        try{bound=true;Shizuku.bindUserService(args,connection);main.postDelayed(timeout,12000);}
        catch(RuntimeException ex){waiting=null;busy=false;tell("Shizuku servisi başlatılamadı.");update();}
    }
    private CarSilencePolicy.Backend backend(ICarProfile service){return new CarSilencePolicy.Backend(){public int read(String address)throws Exception{return service.readPolicy(address);}public boolean write(String address,int policy)throws Exception{return service.writePolicy(address,policy);}};}
    void list(Consumer<JSONArray> result){
        if(closed)return;if(busy){tell("Önce devam eden işlemin bitmesini bekle.");return;}
        busy=true;update();worker.execute(()->{
            JSONArray devices=null;String error=null;
            try{devices=CarDevices.load(context);}catch(Exception ex){error=CarErrors.describe(ex);CallDiagnostics.record(context,"ARAC_LISTE_HATASI: "+ex.getClass().getSimpleName());}
            final JSONArray found=devices;final String failure=error;
            main.post(()->{busy=false;update();if(!closed){if(failure!=null)tell(failure);else if(found!=null)result.accept(found);}if(closed)disconnect();});
        });
    }
    void enable(String address,String name){run(service->{CarSilencePolicy.enable(backend(service),journal,address,name);main.post(()->{verified="Görüşme profili kapalı · "+name;tell("Araç görüşme profili kapatıldı. Müzik profili değiştirilmedi.");});});}
    void restore(){run(service->{CarSilencePolicy.restore(backend(service),journal);main.post(()->{verified="Kapalı · önceki görüşme ayarı geri yüklendi";tell("Önceki görüşme ayarı geri yüklendi. Gerekirse araçtan yeniden bağlan.");});});}
    void inspect(){run(service->{CarSilencePolicy.Saved value=journal.load();String status="Kapalı · geri yükleme kaydı yok";if(value!=null){int policy=service.readPolicy(value.address);int state=service.connectionState(value.address);status=(policy==0?"Görüşme profili kapalı":policy==100?"Görüşme profili açık":"Görüşme ayarı belirsiz")+" · "+value.name+"\nHFP bağlantısı: "+(state==0?"kesik":state==1?"bağlanıyor":state==2?"bağlı":"kesiliyor")+" · geri yükleme kaydı saklanıyor";}final String found=status;main.post(()->verified=found);});}
    private void disconnect(){if(bound)try{Shizuku.unbindUserService(args,connection,false);}catch(RuntimeException ignored){}bound=false;remote=null;}
    @Override public void close(){closed=true;main.removeCallbacks(timeout);waiting=null;Shizuku.removeRequestPermissionResultListener(permission);Shizuku.removeBinderDeadListener(dead);worker.shutdown();if(!busy)disconnect();}
}
