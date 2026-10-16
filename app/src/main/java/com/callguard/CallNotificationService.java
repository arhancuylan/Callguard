package com.callguard;

import android.app.*;
import android.content.ComponentName;
import android.os.*;
import android.service.notification.*;
import java.util.*;

/** Invokes only the verified, WhatsApp-owned Decline action. No Telecom or accessibility route. */
public final class CallNotificationService extends NotificationListenerService {
    private Store store;
    private final Map<String,Long> processed=new HashMap<>();
    @Override public void onCreate(){super.onCreate();store=new Store(this);}
    @Override public void onDestroy(){store.close();super.onDestroy();}
    @Override public void onListenerDisconnected(){requestRebind(new ComponentName(this,CallNotificationService.class));}
    @Override public void onNotificationPosted(StatusBarNotification sbn){
        if(sbn==null||!store.enabled())return;
        String owner=sbn.getPackageName();
        if(!"com.whatsapp".equals(owner)&&!"com.whatsapp.w4b".equals(owner))return;
        Notification n=sbn.getNotification();
        if(n==null||!Notification.CATEGORY_CALL.equals(n.category))return;
        Bundle e=n.extras==null?Bundle.EMPTY:n.extras;
        List<String> actions=new ArrayList<>();
        if(n.actions!=null)for(Notification.Action a:n.actions)actions.add(a==null?"":String.valueOf(a.title));
        Rules.Incoming incoming=Rules.incoming(owner,n.category,(n.flags&Notification.FLAG_GROUP_SUMMARY)!=0,
            String.valueOf(e.getCharSequence(Notification.EXTRA_TEXT,"")),e.getInt("android.callType",0),
            e.containsKey("android.callIsVideo")?e.getBoolean("android.callIsVideo"):null,actions);
        if(incoming==null||!Rules.selected(store.media(),incoming.media))return;
        String caller=String.valueOf(e.getCharSequence(Notification.EXTRA_TITLE,""));String uri="";
        if(Build.VERSION.SDK_INT>=28){Person person=e.getParcelable("android.callPerson");if(person!=null){uri=person.getUri();if(person.getName()!=null)caller=person.getName().toString();}}
        boolean listed=Rules.listed(caller,uri,store.contacts(),store.mode()==Rules.Mode.WHITELIST);
        boolean blocked=Rules.block(store.mode(),listed);
        String number=Rules.number(uri);if(number.isEmpty())number=Rules.number(caller);
        long now=SystemClock.elapsedRealtime();String key=sbn.getKey();
        Long previous=processed.get(key);if(previous!=null&&now-previous<15000)return;
        if(processed.size()>200)processed.clear();processed.put(key,now);
        boolean video=incoming.media==Rules.Media.VIDEO;
        if(!blocked){store.history(caller,number,video,"İzin verildi");return;}
        if(store.recordOnly()){store.history(caller,number,video,"Yalnızca kaydedildi");return;}
        Notification.Action action=n.actions[incoming.declineIndex];
        if(action.actionIntent==null||!owner.equals(action.actionIntent.getCreatorPackage())){store.history(caller,number,video,"Reddetme eylemi doğrulanamadı");return;}
        try{action.actionIntent.send();store.history(caller,number,video,"Reddetme isteği gönderildi");}
        catch(PendingIntent.CanceledException|SecurityException ex){store.history(caller,number,video,"Reddetme isteği gönderilemedi");}
    }
    @Override public void onNotificationRemoved(StatusBarNotification sbn){if(sbn!=null)processed.remove(sbn.getKey());}
}
