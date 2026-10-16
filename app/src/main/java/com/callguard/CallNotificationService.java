package com.callguard;

import android.app.*;
import android.content.ComponentName;
import android.os.*;
import android.service.notification.*;
import java.util.*;

/** Invokes only the verified, WhatsApp-owned Decline action. No Telecom or accessibility route. */
public final class CallNotificationService extends NotificationListenerService {
    private Store store;
    static volatile boolean connected;
    private void trace(String value){if(BuildConfig.DEBUG)android.util.Log.d("CallGuardTest",value);}
    @Override public void onListenerConnected(){connected=true;trace("listener connected");}
    private final CallSessions processed=new CallSessions();
    @Override public void onCreate(){super.onCreate();store=new Store(this);}
    @Override public void onDestroy(){connected=false;store.close();super.onDestroy();}
    @Override public void onListenerDisconnected(){connected=false;trace("listener disconnected");requestRebind(new ComponentName(this,CallNotificationService.class));}
    @Override public void onNotificationPosted(StatusBarNotification sbn){
        if(sbn==null)return;
        trace("posted owner="+sbn.getPackageName()+" enabled="+store.enabled());
        if(!store.enabled())return;
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
        trace("category="+n.category+" type="+e.getInt("android.callType",0)+" video="+e.get("android.callIsVideo")+" actions="+actions+" incoming="+(incoming!=null));
        if(incoming==null)return;
        String caller=String.valueOf(e.getCharSequence(Notification.EXTRA_TITLE,""));String uri="";
        if(Build.VERSION.SDK_INT>=28){Person person=e.getParcelable("android.callPerson");if(person!=null){uri=person.getUri();if(person.getName()!=null)caller=person.getName().toString();}}
        Rules.Mode mode=store.mode();
        Rules.Media media=store.media();
        boolean recordOnly=store.recordOnly();
        // ALL never depends on a contact lookup or either list.
        boolean listed=mode!=Rules.Mode.ALL&&Rules.listed(caller,uri,store.contacts(),mode==Rules.Mode.WHITELIST);
        boolean blocked=Rules.block(mode,listed);
        String number=Rules.number(uri);if(number.isEmpty())number=Rules.number(caller);
        Notification.Action action=n.actions[incoming.declineIndex];
        long startedAt=n.when>0?n.when:sbn.getPostTime();
        String policy=mode.name()+":"+media.name()+":"+recordOnly+":"+blocked;
        if(!processed.claim(sbn.getKey(),startedAt,caller,incoming.media,action.actionIntent,policy))return;
        boolean video=incoming.media==Rules.Media.VIDEO;
        if(!Rules.selected(media,incoming.media)){store.history(caller,number,video,"Seçilen arama türünün dışında");return;}
        if(!blocked){store.history(caller,number,video,"İzin verildi");return;}
        if(recordOnly){store.history(caller,number,video,"Yalnızca kaydedildi");return;}
        if(action.actionIntent==null||!owner.equals(action.actionIntent.getCreatorPackage())){store.history(caller,number,video,"Reddetme eylemi doğrulanamadı");return;}
        try{action.actionIntent.send();store.history(caller,number,video,"Reddetme isteği gönderildi");}
        catch(PendingIntent.CanceledException|SecurityException ex){store.history(caller,number,video,"Reddetme isteği gönderilemedi");}
    }
    @Override public void onNotificationRemoved(StatusBarNotification sbn){if(sbn!=null)processed.remove(sbn.getKey());}
}
