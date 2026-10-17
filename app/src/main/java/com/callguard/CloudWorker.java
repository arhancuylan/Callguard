package com.callguard;
import android.Manifest;import android.app.*;import android.content.*;import android.content.pm.PackageManager;import android.os.Build;import androidx.work.*;import org.json.JSONObject;
public final class CloudWorker extends Worker {
    public CloudWorker(Context c,WorkerParameters p){super(c,p);}
    @Override public Result doWork(){Context c=getApplicationContext();if(SessionVault.userId(c).isEmpty()||!CloudClient.configured())return Result.success();boolean failure=false;
        try(Store store=new Store(c)){try{synchronized(CloudSync.BACKUP_LOCK){CloudClient.refresh(c);SharedPreferences prefs=c.getSharedPreferences("cloud",0);if(prefs.getBoolean("autoBackup",false)&&prefs.getBoolean("dirty",false)&&prefs.getBoolean("backupReady",false)){
                    String before=BackupData.export(store).toString();JSONObject result=CloudClient.request(c,"PUT","/v1/backup",new JSONObject().put("revision",prefs.getInt("revision",0)).put("backup",new JSONObject(before)),true);
                    boolean changed=!before.equals(BackupData.export(store).toString());prefs.edit().putInt("revision",result.getInt("revision")).putBoolean("dirty",changed).putLong("lastBackup",System.currentTimeMillis()).remove("lastError").apply();
                }
            }}catch(Exception ex){failure=true;c.getSharedPreferences("cloud",0).edit().putString("lastError","Bulut eşitlemesi tamamlanamadı; önceki yedek korunuyor.").apply();}
            if(BuildConfig.COMMERCIAL_ENABLED&&!LicenseManager.allowed(c)&&store.enabled()){store.setting("enabled",false);notifyUser(c,"Deneme sona erdi","Otomatik koruma durduruldu. Listelerin ve ayarların korunuyor.",2);}
            else if(BuildConfig.COMMERCIAL_ENABLED){long days=Entitlement.daysLeft(c.getSharedPreferences("license",0).getLong("trialEnds",0),LicenseManager.now(c));if(days>0&&days<=2&&!c.getSharedPreferences("license",0).getBoolean("pro",false))notifyUser(c,"Ücretsiz denemen sona yaklaşıyor",days+" gün kaldı. Otomatik ödeme yapılmaz; Pro tek seferlik satın alınır.",1);}
        }
        c.sendBroadcast(new Intent(c.getPackageName()+".CHANGED").setPackage(c.getPackageName()));return failure&&getRunAttemptCount()<2?Result.retry():Result.success();
    }
    static void notifyUser(Context c,String title,String body,int id){if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;NotificationManager manager=c.getSystemService(NotificationManager.class);manager.createNotificationChannel(new NotificationChannel("account","Hesap ve deneme süresi",NotificationManager.IMPORTANCE_DEFAULT));Notification n=new Notification.Builder(c,"account").setSmallIcon(com.callguard.R.drawable.ic_launcher).setContentTitle(title).setContentText(body).setStyle(new Notification.BigTextStyle().bigText(body)).setOnlyAlertOnce(true).setAutoCancel(true).setContentIntent(PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE)).build();manager.notify(id,n);}
}
