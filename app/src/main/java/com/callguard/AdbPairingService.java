package com.callguard;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.os.*;
import java.util.concurrent.*;

/** User-started, time-limited setup service. Keeps Settings' pairing window in the foreground. */
@android.annotation.TargetApi(30)
public final class AdbPairingService extends Service {
    private static final String CHANNEL="local-adb-pairing",REPLY="pairing_reply",START="start",SEND="reply",STOP="stop";
    private static final int ID=9021,RESULT=9022;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private LocalAdbDiscovery discovery;private int port;private boolean busy,connecting,closed,active;
    static void start(Context context){context.startForegroundService(new Intent(context,AdbPairingService.class).setAction(START));}
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public void onCreate(){
        super.onCreate();getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel(CHANNEL,"CallGuard ADB eşleştirme",NotificationManager.IMPORTANCE_HIGH));
    }
    @Override public int onStartCommand(Intent intent,int flags,int id){
        String action=intent==null?null:intent.getAction();
        if(STOP.equals(action)){stopSelf();return START_NOT_STICKY;}
        if(!START.equals(action)&&!active){stopSelf();return START_NOT_STICKY;}
        if(START.equals(action)){
            active=true;
            if(discovery==null&&!busy){
                Notification notification=notification("Eşleştirme kodu bekleniyor","Ayarlar → Kablosuz hata ayıklama → Eşleştirme koduyla cihaz eşleştir. Bu ekran açık kalsın.",true);
                if(Build.VERSION.SDK_INT>=34)startForeground(ID,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);else startForeground(ID,notification);
                main.postDelayed(this::expired,6*60*1000L);discoverPairing();
            }
        }else if(SEND.equals(action)&&!busy&&!connecting){
            Bundle results=RemoteInput.getResultsFromIntent(intent);CharSequence raw=results==null?null:results.getCharSequence(REPLY);PairingReply reply;
            try{reply=PairingReply.parse(raw,port);}catch(IllegalArgumentException ex){update("Kod veya port eksik","Port bulunduysa 6 haneli kodu yaz. Bulunmadıysa Android’deki port ve kodu araya boşluk koyarak yaz: port kod",true);return START_NOT_STICKY;}
            intent.replaceExtras((Bundle)null);busy=true;update("Eşleştiriliyor…","Android’in kod penceresini kapatma.",false);
            worker.execute(()->{
                boolean success=false;try{success=LocalAdb.get(this).pairLocal(reply.port,reply.code);}catch(Exception ex){CallDiagnostics.record(this,"ADB_BILDIRIM_ESLESTIRME: "+ex.getClass().getSimpleName());}
                final boolean paired=success;main.post(()->{if(closed)return;busy=false;if(paired){connecting=true;port=0;closeDiscovery();update("Eşleşti · bağlantı aranıyor","Bağlantı otomatik açılacak. Ardından CallGuard’a dönebilirsin.",false);discoverConnection();}else update("Eşleştirme tamamlanamadı","Kod penceresi açık kalsın. Gerekirse yeni kod üret ve bildirimin Kod gir düğmesiyle tekrar dene.",true);});
            });
        }else if(action==null){stopSelf();}
        return START_NOT_STICKY;
    }
    private void discoverPairing(){
        discovery=new LocalAdbDiscovery(this,true,value->{if(closed||connecting)return;port=value;if(!busy)update(value>0?"Eşleştirme bulundu · şimdi kodu gir":"Eşleştirme kodu bekleniyor",value>0?"Android’in kod penceresi açıkken aşağıdaki Kod gir düğmesine dokun ve 6 haneli kodu gönder.":"Kod penceresini aç. Otomatik bulunmazsa Kod gir ile port ve kodu birlikte yaz: port kod",true);});
    }
    private void discoverConnection(){
        discovery=new LocalAdbDiscovery(this,false,value->{if(closed||!connecting||busy||value<=0)return;busy=true;
            worker.execute(()->{boolean success=false;try{LocalAdb.get(this).connectLocal(value);success=true;}catch(Exception ex){CallDiagnostics.record(this,"ADB_BILDIRIM_BAGLANTI: "+ex.getClass().getSimpleName());}
                final boolean connected=success;main.post(()->{if(closed)return;busy=false;finish(connected?"CallGuard ADB bağlantısı hazır":"Eşleşti · bağlantıyı uygulamadan aç",connected?"CallGuard’a dönüp aracını seçebilirsin.":"CallGuard → Kurulum ve durum → Yerel ADB bağlantısını aç.");});});
        });
        main.postDelayed(()->{if(!closed&&connecting&&!busy)finish("Eşleştirme başarılı","Otomatik bağlantı bulunamadı. CallGuard → Kurulum ve durum → Yerel ADB bağlantısını aç.");},20000);
    }
    private PendingIntent command(String action,int request,boolean mutable){return PendingIntent.getService(this,request,new Intent(this,AdbPairingService.class).setAction(action),PendingIntent.FLAG_UPDATE_CURRENT|(mutable?PendingIntent.FLAG_MUTABLE:PendingIntent.FLAG_IMMUTABLE));}
    private PendingIntent open(){return PendingIntent.getActivity(this,9023,new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    private Notification notification(String title,String text,boolean reply){
        Notification.Builder builder=new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_pairing).setContentTitle(title).setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text)).setOngoing(true).setOnlyAlertOnce(true).setVisibility(Notification.VISIBILITY_SECRET);
        if(reply){Notification.Action.Builder action=new Notification.Action.Builder(android.graphics.drawable.Icon.createWithResource(this,R.drawable.ic_pairing),"Kod gir",command(SEND,9024,true)).addRemoteInput(new RemoteInput.Builder(REPLY).setLabel(port>0?"6 haneli kod":"Port ve kod (port kod)").build()).setSemanticAction(Notification.Action.SEMANTIC_ACTION_REPLY);
            if(Build.VERSION.SDK_INT>=31)action.setAuthenticationRequired(true);builder.addAction(action.build());}
        builder.addAction(new Notification.Action.Builder(android.graphics.drawable.Icon.createWithResource(this,R.drawable.ic_pairing),"Vazgeç",command(STOP,9025,false)).build());return builder.build();
    }
    private void update(String title,String text,boolean reply){if(!closed)getSystemService(NotificationManager.class).notify(ID,notification(title,text,reply));}
    private void finish(String title,String text){
        if(closed)return;closed=true;closeDiscovery();getSystemService(NotificationManager.class).notify(RESULT,new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_pairing).setContentTitle(title).setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text)).setContentIntent(open()).setAutoCancel(true).build());stopSelf();
    }
    private void expired(){if(!closed)finish("Eşleştirme dinlemesi sona erdi","İstersen CallGuard içinden bildirimle eşleştirmeyi yeniden başlat.");}
    private void closeDiscovery(){if(discovery!=null){discovery.close();discovery=null;}}
    @Override public void onDestroy(){closed=true;main.removeCallbacksAndMessages(null);closeDiscovery();worker.shutdown();stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();}
}
