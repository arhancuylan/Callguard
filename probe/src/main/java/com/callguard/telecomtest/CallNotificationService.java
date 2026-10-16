package com.callguard.telecomtest;
import android.app.*;
import android.os.Bundle;
import android.os.SystemClock;
import android.service.notification.*;
import java.util.*;

public final class CallNotificationService extends NotificationListenerService {
    private final Map<String,Long> sent=new HashMap<>();
    private final Map<String,String> last=new HashMap<>();
    @Override public void onListenerConnected(){super.onListenerConnected();ProbeState.log(this,"BİLDİRİM DİNLEYİCİSİ BAĞLANDI · sonraki yeni çağrıyı bekliyor");}
    @Override public void onListenerDisconnected(){ProbeState.log(this,"BİLDİRİM DİNLEYİCİSİ BAĞLANTISI KESİLDİ");super.onListenerDisconnected();}
    private void once(String key,String message){if(!message.equals(last.get(key))){last.put(key,message);ProbeState.log(this,message);}}
    @Override public void onNotificationPosted(StatusBarNotification sbn){
        if(sbn==null||!"notification".equals(ProbeState.prefs(this).getString("command","reject")))return;
        String pkg=sbn.getPackageName();
        if(!"com.whatsapp".equals(pkg)&&!"com.whatsapp.w4b".equals(pkg))return;
        Notification n=sbn.getNotification();
        // Never inspect or store WhatsApp message notification content.
        if(n==null||!Notification.CATEGORY_CALL.equals(n.category))return;
        String key=sbn.getKey();
        if(last.size()>100)last.clear();
        Bundle extras=n.extras==null?Bundle.EMPTY:n.extras;
        String text=String.valueOf(extras.getCharSequence(Notification.EXTRA_TEXT,""));
        String caller=String.valueOf(extras.getCharSequence(Notification.EXTRA_TITLE,""));
        int type=extras.getInt(Notification.EXTRA_CALL_TYPE,0);
        Boolean isVideo=extras.containsKey(Notification.EXTRA_CALL_IS_VIDEO)?extras.getBoolean(Notification.EXTRA_CALL_IS_VIDEO):null;
        List<String> labels=new ArrayList<>();
        if(n.actions!=null)for(Notification.Action a:n.actions)labels.add(a==null?"":String.valueOf(a.title));
        boolean summary=(n.flags&Notification.FLAG_GROUP_SUMMARY)!=0;
        NotificationRule.Result result=NotificationRule.analyze(pkg,n.category,summary,text,type,isVideo,labels);
        int index=result.index;
        List<String> normalized=new ArrayList<>();for(String actionLabel:labels)normalized.add(NotificationRule.label(actionLabel));
        // Only coarse metadata and action titles enter logs; no caller/message text.
        once(key,"ÇAĞRI BİLDİRİMİ: id="+sbn.getId()+", tür="+type+", video="+isVideo+", eylemler="+labels+", normalize="+normalized+", özet="+summary+", karar="+result.reason+", doğrulanan reddet="+index);
        if(index<0)return;
        if(!ProbeState.prefs(this).getBoolean("armed",false)){once(key,"BİLDİRİM: gelen sesli çağrı + reddet eylemi doğrulandı · yalnızca gözlem");return;}
        Set<String> allowed=ProbeState.allowed(this);
        String number=CallRule.number(caller);
        if(!number.isEmpty()&&allowed.contains(number)){once(key,"BİLDİRİM: izinli numara · işlem yok");return;}
        if(number.isEmpty()&&!allowed.isEmpty()){once(key,"BİLDİRİM: arayan başlığında numara yok; izinli kişiyi engellememek için işlem yapılmadı");return;}
        Notification.Action action=n.actions[index];
        if(action.actionIntent==null){once(key,"BİLDİRİM: reddet eyleminin PendingIntent nesnesi yok");return;}
        if(!pkg.equals(action.actionIntent.getCreatorPackage())){once(key,"BİLDİRİM: eylemin sahibi WhatsApp değil; işlem yapılmadı");return;}
        long now=SystemClock.elapsedRealtime();
        if(sent.containsKey(key)&&now-sent.get(key)<15000)return;
        if(sent.size()>100)sent.clear();
        sent.put(key,now);
        ProbeState.log(this,"BİLDİRİM: WhatsApp Reddet eylemi çalıştırılıyor; eylem sırası="+index);
        try{action.actionIntent.send();ProbeState.log(this,"BİLDİRİM: PendingIntent.send döndü · gerçek kapanmayı iki telefonda kontrol et");}
        catch(PendingIntent.CanceledException|SecurityException e){ProbeState.log(this,"BİLDİRİM: eylem çalıştırılamadı: "+e.getClass().getSimpleName());}
    }
    @Override public void onNotificationRemoved(StatusBarNotification sbn){
        if(sbn!=null&&last.remove(sbn.getKey())!=null){sent.remove(sbn.getKey());ProbeState.log(this,"BİLDİRİM: izlenen çağrı bildirimi kaldırıldı · tek başına başarı kanıtı değildir");}
    }
}
