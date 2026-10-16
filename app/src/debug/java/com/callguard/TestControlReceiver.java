package com.callguard;
import android.content.*;
import android.service.notification.NotificationListenerService;
/** Shell-only CI controller. This source set is excluded from the release APK. */
public final class TestControlReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){
        if("com.callguard.debug.REBIND".equals(i.getAction())){NotificationListenerService.requestRebind(new ComponentName(c,CallNotificationService.class));return;}
        try(Store s=new Store(c)){
            s.setting("enabled",false);s.getWritableDatabase().delete("contacts",null,null);s.clearHistory();
            s.setting("rule",i.getStringExtra("rule")==null?"WHITELIST":i.getStringExtra("rule"));
            s.setting("media",i.getStringExtra("media")==null?"BOTH":i.getStringExtra("media"));
            s.setting("recordOnly",i.getBooleanExtra("recordOnly",false));
            if(i.hasExtra("whiteNumber"))s.add("Test Caller",i.getStringExtra("whiteNumber"),true);
            if(i.hasExtra("blackNumber"))s.add("Test Caller",i.getStringExtra("blackNumber"),false);
            s.setting("enabled",i.getBooleanExtra("enabled",true));
        }
    }
}
