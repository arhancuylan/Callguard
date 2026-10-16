package com.callguard.telecomtest;
import android.telecom.*;
import android.os.*;
import android.content.*;
import java.util.*;

public final class TelecomProbeService extends InCallService {
    private static final String SESSION=UUID.randomUUID().toString().substring(0,6);
    private final Map<Call,Trace> tracked=new IdentityHashMap<>();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private int sequence=0;
    private final class Trace {
        final String id=SESSION+"-"+(++sequence);
        final long added=SystemClock.elapsedRealtime();
        boolean whatsapp=false,attempted=false,returned=false,disconnected=false;
        long requested=0;
        String lastDecision="";
        Call.Callback callback;
    }
    private void write(Trace t,String message){ProbeState.log(this,"["+t.id+" +"+(SystemClock.elapsedRealtime()-t.added)+"ms] "+message);}
    private String state(int state){
        switch(state){
            case Call.STATE_NEW:return "NEW";
            case Call.STATE_RINGING:return "RINGING";
            case Call.STATE_DIALING:return "DIALING";
            case Call.STATE_ACTIVE:return "ACTIVE";
            case Call.STATE_HOLDING:return "HOLDING";
            case Call.STATE_DISCONNECTING:return "DISCONNECTING";
            case Call.STATE_DISCONNECTED:return "DISCONNECTED";
            case Call.STATE_CONNECTING:return "CONNECTING";
            default:return "STATE_"+state;
        }
    }
    private String packageName(Call.Details d){
        return d==null||d.getAccountHandle()==null||d.getAccountHandle().getComponentName()==null?"":d.getAccountHandle().getComponentName().getPackageName();
    }
    private boolean whatsapp(Call.Details d){String p=packageName(d);return "com.whatsapp".equals(p)||"com.whatsapp.w4b".equals(p);}
    private void discover(Call c,Trace t){
        if(t.whatsapp||!whatsapp(c.getDetails()))return;
        t.whatsapp=true;
        Call.Details d=c.getDetails();
        long creation=d.getCreationTimeMillis();
        long age=creation>0?System.currentTimeMillis()-creation:-1;
        write(t,"İLK WHATSAPP OLAYI: mevcut="+state(c.getState())+", details="+state(d.getState())+", Telecom oluşumundan beri="+age+"ms (duvar saati ölçümü); servis erişimi="+ProbeAccess.canManageCalls(this));
    }
    @Override public void onCreate(){super.onCreate();ProbeState.log(this,"SERVİS OLUŞTU ["+SESSION+"] · Telecom erişimi="+ProbeAccess.canManageCalls(this));}
    @Override public IBinder onBind(Intent intent){ProbeState.log(this,"SERVİS BAĞLANDI ["+SESSION+"]");return super.onBind(intent);}
    @Override public boolean onUnbind(Intent intent){ProbeState.log(this,"SERVİS BAĞLANTISI KESİLDİ ["+SESSION+"]");return super.onUnbind(intent);}
    @Override public void onCallAdded(Call call){
        super.onCallAdded(call);
        Trace t=new Trace();tracked.put(call,t);
        t.callback=new Call.Callback(){
            @Override public void onStateChanged(Call c,int callbackState){
                discover(c,t);
                if(t.whatsapp)write(t,"DURUM CALLBACK: bildirilen="+state(callbackState)+", mevcut="+state(c.getState())+", details="+state(c.getDetails().getState()));
                inspect(c,t);
            }
            @Override public void onDetailsChanged(Call c,Call.Details d){
                discover(c,t);
                if(t.whatsapp)write(t,"DETAY CALLBACK: mevcut="+state(c.getState())+", details="+state(d.getState())+", yön="+d.getCallDirection()+", video="+d.getVideoState());
                inspect(c,t);
            }
        };
        call.registerCallback(t.callback,handler);
        discover(call,t);
        if(t.whatsapp)write(t,"onCallAdded alındı");
        inspect(call,t);
    }
    private void once(Trace t,String message){if(!message.equals(t.lastDecision)){t.lastDecision=message;write(t,message);}}
    private void inspect(Call c,Trace t){
        discover(c,t);
        if(!t.whatsapp)return;
        Call.Details d=c.getDetails();
        // Require a currently identifiable WhatsApp owner before taking any action.
        if(!whatsapp(d)){once(t,"Çağrı sahibi artık doğrulanamıyor; işlem yok");return;}
        int current=c.getState();
        if(current==Call.STATE_DISCONNECTED){
            if(!t.disconnected){
                t.disconnected=true;
                int cause=d.getDisconnectCause()==null?DisconnectCause.UNKNOWN:d.getDisconnectCause().getCode();
                write(t,"KAPANMA GÖZLENDİ: neden="+cause+", istek="+t.attempted+", API döndü="+t.returned+", istekten sonra="+(t.attempted?SystemClock.elapsedRealtime()-t.requested:-1)+"ms · kimin kapattığını tek başına kanıtlamaz");
            }
            return;
        }
        String phone=d.getHandle()!=null&&"tel".equals(d.getHandle().getScheme())?d.getHandle().getSchemeSpecificPart():"";
        boolean incoming=d.getCallDirection()==Call.Details.DIRECTION_INCOMING;
        boolean video=VideoProfile.isVideo(d.getVideoState());
        boolean armed=ProbeState.prefs(this).getBoolean("armed",false);
        CallRule.Decision decision=CallRule.decide(packageName(d),incoming,current==Call.STATE_RINGING,video,armed,phone,ProbeState.allowed(this));
        if(t.attempted)return;
        if(decision!=CallRule.Decision.REJECT){once(t,"KARAR="+decision+", mevcut="+state(current)+", details="+state(d.getState())+", gelen="+incoming+", video="+video);return;}
        android.companion.CompanionDeviceManager manager=getSystemService(android.companion.CompanionDeviceManager.class);
        if(manager==null||manager.getMyAssociations().isEmpty()||!ProbeAccess.canManageCalls(this)){once(t,"Çağrı görüldü; eşleştirme veya Telecom erişimi eksik. Komut gönderilmedi.");return;}
        t.attempted=true;t.requested=SystemClock.elapsedRealtime();
        write(t,"REJECT ÇAĞRILMADAN ÖNCE: mevcut="+state(c.getState())+", details="+state(d.getState())+", Telecom erişimi=true");
        try{
            c.reject(false,null);
            t.returned=true;
            write(t,"REJECT API DÖNDÜ: mevcut="+state(c.getState())+" · komutun kabul veya uygulanma onayı değildir");
            for(long delay:new long[]{1000,3000,10000}){
                handler.postDelayed(()->{
                    if(tracked.get(c)!=t)return;
                    write(t,"KOMUT SONRASI KONTROL: "+delay+"ms hedefi, gerçek="+(SystemClock.elapsedRealtime()-t.requested)+"ms, mevcut="+state(c.getState())+", details="+state(c.getDetails().getState())+" · devam ediyorsa reddetme doğrulanmadı");
                },delay);
            }
        }catch(SecurityException|IllegalStateException e){write(t,"REJECT HATASI: "+e.getClass().getSimpleName());}
    }
    @Override public void onCallRemoved(Call c){
        Trace t=tracked.get(c);
        if(t!=null){
            discover(c,t);
            if(t.whatsapp){inspect(c,t);write(t,"onCallRemoved: mevcut="+state(c.getState())+", istek="+t.attempted+", API döndü="+t.returned+" · otomatik reddetme başarısı değildir");}
            c.unregisterCallback(t.callback);tracked.remove(c);
        }
        super.onCallRemoved(c);
    }
    @Override public void onDestroy(){handler.removeCallbacksAndMessages(null);for(Map.Entry<Call,Trace> e:tracked.entrySet())e.getKey().unregisterCallback(e.getValue().callback);tracked.clear();ProbeState.log(this,"SERVİS SONLANDI ["+SESSION+"]");super.onDestroy();}
}
