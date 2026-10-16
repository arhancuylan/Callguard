package com.callguard.telecomtest;
import android.telecom.*;
import android.os.*;
import java.util.*;

public final class TelecomProbeService extends InCallService {
    private final Map<Call,Call.Callback> tracked=new IdentityHashMap<>();
    private final Set<Call> attempted=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<Call,String> last=new IdentityHashMap<>();
    @Override public void onCallAdded(Call call){
        super.onCallAdded(call);
        Call.Callback cb=new Call.Callback(){
            @Override public void onStateChanged(Call c,int state){inspect(c);}
            @Override public void onDetailsChanged(Call c,Call.Details d){inspect(c);}
        };
        tracked.put(call,cb);call.registerCallback(cb,new Handler(Looper.getMainLooper()));
        inspect(call);
    }
    private void once(Call c,String message){if(!message.equals(last.get(c))){last.put(c,message);ProbeState.log(this,message);}}
    private boolean whatsapp(Call.Details d){
        if(d==null||d.getAccountHandle()==null||d.getAccountHandle().getComponentName()==null)return false;
        String p=d.getAccountHandle().getComponentName().getPackageName();
        return "com.whatsapp".equals(p)||"com.whatsapp.w4b".equals(p);
    }
    private void inspect(Call c){
        Call.Details d=c.getDetails();
        // Ordinary SIM calls, unknown owners and all other apps are never logged or touched.
        if(!whatsapp(d))return;
        int state=d.getState();
        if(state==Call.STATE_DISCONNECTED){
            int cause=d.getDisconnectCause()==null?DisconnectCause.UNKNOWN:d.getDisconnectCause().getCode();
            once(c,"WhatsApp çağrısı kapandı. Neden kodu: "+cause+"; reddetme isteği: "+attempted.contains(c));return;
        }
        String pkg=d.getAccountHandle().getComponentName().getPackageName();
        String phone=d.getHandle()!=null&&"tel".equals(d.getHandle().getScheme())?d.getHandle().getSchemeSpecificPart():"";
        boolean incoming=d.getCallDirection()==Call.Details.DIRECTION_INCOMING;
        boolean video=VideoProfile.isVideo(d.getVideoState());
        boolean armed=ProbeState.prefs(this).getBoolean("armed",false);
        CallRule.Decision decision=CallRule.decide(pkg,incoming,state==Call.STATE_RINGING,video,armed,phone,ProbeState.allowed(this));
        if(decision==CallRule.Decision.OUT_OF_SCOPE){once(c,"WhatsApp olayı: durum="+state+", gelen="+incoming+", video="+video+" · işlem yok");return;}
        if(attempted.contains(c))return;
        if(decision==CallRule.Decision.OBSERVE){once(c,"Gelen WhatsApp sesli çağrısı Telecom'dan alındı · yalnızca gözlem");return;}
        if(decision==CallRule.Decision.ALLOW){once(c,"İzin listesindeki WhatsApp çağrısına dokunulmadı");return;}
        if(decision==CallRule.Decision.UNKNOWN_WITH_ALLOWLIST){once(c,"Numara okunamadı; izinli kişiyi engellememek için işlem yapılmadı");return;}
        // Require the real watch association and OS permission immediately before acting.
        android.companion.CompanionDeviceManager manager=getSystemService(android.companion.CompanionDeviceManager.class);
        if(manager==null||manager.getMyAssociations().isEmpty()||checkSelfPermission(android.Manifest.permission.MANAGE_ONGOING_CALLS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){once(c,"Çağrı görüldü; eşleştirme veya çağrı yönetim izni eksik. Reddedilmedi.");return;}
        attempted.add(c);
        try{c.reject(false,null);ProbeState.log(this,"Reddetme isteği gönderildi; kapanma olayı bekleniyor");}
        catch(SecurityException|IllegalStateException e){ProbeState.log(this,"Reddetme isteği başarısız: "+e.getClass().getSimpleName());}
    }
    @Override public void onCallRemoved(Call c){
        if(whatsapp(c.getDetails()))ProbeState.log(this,"WhatsApp çağrısı Telecom listesinden kaldırıldı · tek başına reddetme kanıtı değildir");
        Call.Callback cb=tracked.remove(c);if(cb!=null)c.unregisterCallback(cb);attempted.remove(c);last.remove(c);super.onCallRemoved(c);
    }
    @Override public void onDestroy(){for(Map.Entry<Call,Call.Callback> e:tracked.entrySet())e.getKey().unregisterCallback(e.getValue());tracked.clear();attempted.clear();last.clear();super.onDestroy();}
}
