package com.callguard;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;

/** System-bound listener recovery. No polling, wake locks or perpetual foreground service. */
final class BackgroundProtection {
    private static long lastRequest=-1;
    private BackgroundProtection(){}
    static boolean hasAccess(Context context){
        String value=Settings.Secure.getString(context.getContentResolver(),"enabled_notification_listeners");
        if(value==null)return false;
        ComponentName expected=new ComponentName(context,CallNotificationService.class);
        for(String item:value.split(":"))if(expected.equals(ComponentName.unflattenFromString(item)))return true;
        return false;
    }
    static synchronized boolean reconnect(Context context){
        if(CallNotificationService.connected||!hasAccess(context))return false;
        long now=SystemClock.elapsedRealtime();
        // A disconnect callback and an Activity resume may arrive together. Do not hammer the system listener manager.
        if(lastRequest>=0&&now-lastRequest<15000)return false;
        lastRequest=now;
        try{
            NotificationListenerService.requestRebind(new ComponentName(context,CallNotificationService.class));
            CallDiagnostics.record(context,"SERVIS_YENIDEN_BAGLANMA_ISTEGI");
            return true;
        }catch(SecurityException|IllegalArgumentException ex){
            CallDiagnostics.record(context,"SERVIS_YENIDEN_BAGLANMA_ISTEGI_REDDEDILDI");
            return false;
        }
    }
    static boolean batteryExempt(Context context){
        PowerManager power=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
        return power!=null&&power.isIgnoringBatteryOptimizations(context.getPackageName());
    }
    static String status(Context context){
        ActivityManager activity=(ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        PowerManager power=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
        boolean restricted=Build.VERSION.SDK_INT>=28&&activity!=null&&activity.isBackgroundRestricted();
        return "Bildirim erişimi: "+(hasAccess(context)?"açık":"kapalı")
            +"\nServis: "+(CallNotificationService.connected?"bağlı":"bağlı değil")
            +"\nAndroid arka plan kısıtlaması: "+(restricted?"var; pil ayarlarını kontrol et":"bildirilmiyor")
            +"\nPil optimizasyonu: "+(batteryExempt(context)?"muaf":"standart (Samsung uyku durumu değildir)")
            +"\nPil tasarrufu: "+(power!=null&&power.isPowerSaveMode()?"açık":"kapalı");
    }
    static boolean samsung(){return "samsung".equalsIgnoreCase(Build.MANUFACTURER);}
}
