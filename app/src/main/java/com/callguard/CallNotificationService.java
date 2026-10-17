package com.callguard;

import android.app.*;
import android.os.*;
import android.service.notification.*;
import java.util.*;

/** Invokes only the verified, WhatsApp-owned Decline action. No Telecom or accessibility route. */
public final class CallNotificationService extends NotificationListenerService {
    private Store store;
    private final Set<String> cleanupRequested=new HashSet<>();
    static volatile boolean connected;
    private void trace(String value){if(BuildConfig.DEBUG)android.util.Log.d("CallGuardTest",value);}
    @Override public void onListenerConnected(){super.onListenerConnected();connected=true;CallDiagnostics.record(this,"SERVIS_BAGLANDI");trace("listener connected");}
    private final CallSessions processed=new CallSessions();
    @Override public void onCreate(){super.onCreate();store=new Store(this);}
    @Override public void onDestroy(){connected=false;CallDiagnostics.record(this,"SERVIS_SONLANDI");if(store!=null)store.close();super.onDestroy();}
    @Override public void onListenerDisconnected(){super.onListenerDisconnected();connected=false;CallDiagnostics.record(this,"SERVIS_BAGLANTISI_KESILDI");trace("listener disconnected");BackgroundProtection.reconnect(this);}
    @Override public void onNotificationPosted(StatusBarNotification sbn){
        if(sbn==null)return;
        trace("posted owner="+sbn.getPackageName()+" enabled="+store.enabled());
        if(!"com.whatsapp".equals(sbn.getPackageName())&&!"com.whatsapp.w4b".equals(sbn.getPackageName()))return;
        if(store.enabled()&&BuildConfig.COMMERCIAL_ENABLED&&!LicenseManager.allowed(this)){store.setting("enabled",false);CloudWorker.notifyUser(this,"Deneme sona erdi","Otomatik koruma durduruldu. Listelerin korunuyor.",2);return;}
        if(!store.enabled())return;
        String owner=sbn.getPackageName();
        if(!"com.whatsapp".equals(owner)&&!"com.whatsapp.w4b".equals(owner))return;
        Notification n=sbn.getNotification();
        if(n==null)return;
        Bundle e=n.extras==null?Bundle.EMPTY:n.extras;
        if(!Notification.CATEGORY_CALL.equals(n.category)){
            CallDiagnostics.record(this,"WHATSAPP_BILDIRIMI: CAGRI_KATEGORISI_YOK");
            return;
        }
        int callType=e.getInt("android.callType",0);
        Boolean isVideo=e.get("android.callIsVideo") instanceof Boolean?(Boolean)e.get("android.callIsVideo"):null;
        String text=callText(e);
        List<Notification.Action> candidates=new ArrayList<>();
        List<String> actions=new ArrayList<>();
        PendingIntent nativeDecline=pending(e,"android.declineIntent"),nativeAnswer=pending(e,"android.answerIntent");
        boolean verifiedDecline=callType==1&&owned(nativeDecline,owner);
        boolean verifiedAnswer=callType==1&&owned(nativeAnswer,owner);
        if(n.actions!=null)for(Notification.Action action:n.actions){
            candidates.add(action);
            String title=action==null?"":String.valueOf(action.title);
            if(action!=null&&verifiedDecline&&nativeDecline.equals(action.actionIntent))title="Reddet";
            else if(action!=null&&verifiedAnswer&&nativeAnswer.equals(action.actionIntent))title="Yanıtla";
            actions.add(title);
        }
        if(verifiedDecline)for(int i=0;i<candidates.size();i++){
            Notification.Action action=candidates.get(i);
            if((action==null||!nativeDecline.equals(action.actionIntent))&&Rules.decline(actions.get(i)))actions.set(i,"");
        }
        if(verifiedDecline&&!containsIntent(candidates,nativeDecline)){
            candidates.add(new Notification.Action.Builder(0,"Reddet",nativeDecline).build());actions.add("Reddet");
        }
        if(verifiedAnswer&&!containsIntent(candidates,nativeAnswer)){
            candidates.add(new Notification.Action.Builder(0,"Yanıtla",nativeAnswer).build());actions.add("Yanıtla");
        }
        Rules.Analysis analysis=Rules.analyze(owner,n.category,(n.flags&Notification.FLAG_GROUP_SUMMARY)!=0,text,callType,isVideo,actions);
        int declineLabels=0,answerLabels=0;for(String title:actions){if(Rules.decline(title))declineLabels++;if(Rules.answer(title))answerLabels++;}
        CallDiagnostics.record(this,"BILDIRIM: grup="+((n.flags&Notification.FLAG_GROUP_SUMMARY)!=0)+", tip="+callType+", video="+isVideo+
            ", metinVideo="+Rules.videoText(text)+", metinSes="+Rules.voiceText(text)+", eylem="+actions.size()+
            ", reddet="+declineLabels+", yanitla="+answerLabels+", yerelReddet="+verifiedDecline+", karar="+analysis.reason);
        Rules.Incoming incoming=analysis.incoming;
        if(incoming==null)return;
        String caller=String.valueOf(e.getCharSequence(Notification.EXTRA_TITLE,""));String uri="";
        if(Build.VERSION.SDK_INT>=28){Person person=e.getParcelable("android.callPerson");if(person!=null){uri=person.getUri();if(person.getName()!=null)caller=person.getName().toString();}}
        Rules.Mode mode=store.effectiveMode();
        Rules.Media media=store.effectiveMedia();
        boolean recordOnly=store.recordOnly();
        // ALL never depends on a contact lookup or either list.
        boolean listed=mode!=Rules.Mode.ALL&&Rules.listed(caller,uri,store.contacts(),mode==Rules.Mode.WHITELIST);
        boolean blocked=Rules.block(mode,listed);
        String number=Rules.number(uri);if(number.isEmpty())number=Rules.number(caller);
        Notification.Action action=candidates.get(incoming.declineIndex);
        long startedAt=n.when>0?n.when:sbn.getPostTime();
        boolean paused=store.paused();boolean schedule=store.scheduleActive();
        String reason=ProtectionState.reason(mode,listed);
        String policy=mode.name()+":"+media.name()+":"+recordOnly+":"+blocked+":"+paused+":"+schedule;
        if(!processed.claim(sbn.getKey(),startedAt,caller,incoming.media,action.actionIntent,policy))return;
        CallDiagnostics.record(this,"KURAL: tur="+incoming.media+", secim="+media+", mod="+mode+", engelle="+blocked+", duraklat="+paused+", zaman="+schedule+", yalnizKayit="+recordOnly);
        boolean video=incoming.media==Rules.Media.VIDEO;
        if(paused){store.history(caller,number,video,ProtectionState.result("İzin verildi","Koruma geçici olarak duraklatıldı"));return;}
        if(!schedule){store.history(caller,number,video,ProtectionState.result("İzin verildi","Seçili profilin gün ve saatleri dışında"));return;}
        if(!Rules.selected(media,incoming.media)){store.history(caller,number,video,ProtectionState.result("Seçilen arama türünün dışında",video?"Görüntülü aramalar engelleme kapsamında değil":"Sesli aramalar engelleme kapsamında değil"));return;}
        if(!blocked){store.history(caller,number,video,ProtectionState.result("İzin verildi",reason));return;}
        if(recordOnly){store.history(caller,number,video,ProtectionState.result("Yalnızca kaydedildi","Yalnızca kaydet modu · "+reason));return;}
        if(action.actionIntent==null||!owner.equals(action.actionIntent.getCreatorPackage())){store.history(caller,number,video,ProtectionState.result("Reddetme eylemi doğrulanamadı",reason));return;}
        try{action.actionIntent.send();CallDiagnostics.record(this,"REDDET_ISTEGI_GONDERILDI");store.history(caller,number,video,ProtectionState.result("Reddetme isteği gönderildi",reason));cleanupRejected(sbn,action.actionIntent);}
        catch(PendingIntent.CanceledException|SecurityException ex){CallDiagnostics.record(this,"REDDET_ISTEGI_GONDERILEMEDI");store.history(caller,number,video,ProtectionState.result("Reddetme isteği gönderilemedi",reason));}
    }
    private RejectedNotification snapshot(StatusBarNotification sbn,PendingIntent expected){
        Notification n=sbn.getNotification();if(n==null)return null;
        Bundle extras=n.extras==null?Bundle.EMPTY:n.extras;
        PendingIntent matching=null;
        if(expected.equals(pending(extras,"android.declineIntent")))matching=expected;
        if(n.actions!=null)for(Notification.Action a:n.actions)if(a!=null&&expected.equals(a.actionIntent))matching=expected;
        return new RejectedNotification(sbn.getKey(),sbn.getPackageName(),n.category,sbn.getPostTime(),n.when,
            extras.getInt("android.callType",0),(n.flags&Notification.FLAG_GROUP_SUMMARY)!=0,matching);
    }
    private void cleanupRejected(StatusBarNotification original,PendingIntent decline){
        // Reaching here means our rejection action returned, NOT that call termination was confirmed.
        // No delayed task: a reused notification ID must never erase a later call or message.
        if(!connected)return;
        try{
            RejectedNotification expected=snapshot(original,decline);
            if(expected==null||!expected.matches(expected)){CallDiagnostics.record(this,"BILDIRIM_TEMIZLIGI_ATLANDI: ortak_ozet_veya_belirsiz");return;}
            StatusBarNotification[] active=getActiveNotifications(new String[]{original.getKey()});
            if(active==null)return;
            for(StatusBarNotification current:active)if(current!=null&&expected.matches(snapshot(current,decline))){
                if(cleanupRequested.size()>=200)cleanupRequested.clear();
                cleanupRequested.add(original.getKey());
                cancelNotification(original.getKey());
                CallDiagnostics.record(this,"ENGELLENEN_BILDIRIMI_SILME_ISTEGI: kapanma_onayi_degildir");
                return;
            }
        }catch(SecurityException|IllegalStateException|IllegalArgumentException ex){
            cleanupRequested.remove(original.getKey());
            CallDiagnostics.record(this,"ENGELLENEN_BILDIRIMI_SILINEMEDI");
        }
    }
    private static boolean owned(PendingIntent intent,String owner){return intent!=null&&owner.equals(intent.getCreatorPackage());}
    private static PendingIntent pending(Bundle extras,String key){Object value=extras.get(key);return value instanceof PendingIntent?(PendingIntent)value:null;}
    private static boolean containsIntent(List<Notification.Action> actions,PendingIntent intent){for(Notification.Action a:actions)if(a!=null&&intent.equals(a.actionIntent))return true;return false;}
    static String callText(Bundle extras){
        StringBuilder text=new StringBuilder();
        for(String key:new String[]{Notification.EXTRA_TEXT,Notification.EXTRA_BIG_TEXT,Notification.EXTRA_SUB_TEXT}){
            Object value=extras.get(key);if(value instanceof CharSequence)text.append(value).append(' ');
        }
        Object lines=extras.get(Notification.EXTRA_TEXT_LINES);if(lines instanceof CharSequence[])for(CharSequence line:(CharSequence[])lines)if(line!=null)text.append(line).append(' ');
        return text.toString();
    }
    @Override public void onNotificationRemoved(StatusBarNotification sbn){if(sbn!=null){processed.remove(sbn.getKey());cleanupRequested.remove(sbn.getKey());}}
    @Override public void onNotificationRemoved(StatusBarNotification sbn,RankingMap ranking,int reason){
        if(sbn!=null&&cleanupRequested.remove(sbn.getKey()))CallDiagnostics.record(this,"TEMIZLIK_SONRASI_BILDIRIM_KALDIRILDI: neden="+reason+"; cagri_kapanma_onayi_degildir");
        super.onNotificationRemoved(sbn,ranking,reason);
    }
}
