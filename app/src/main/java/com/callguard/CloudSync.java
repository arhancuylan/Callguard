package com.callguard;
import android.content.*;import androidx.work.*;import java.util.concurrent.TimeUnit;
final class CloudSync {
    static void dirty(Context c){if(!CloudClient.configured()||SessionVault.userId(c).isEmpty())return;SharedPreferences prefs=c.getSharedPreferences("cloud",0);prefs.edit().putBoolean("dirty",true).apply();if(prefs.getBoolean("autoBackup",false))enqueue(c);}
    static void enqueue(Context c){if(!CloudClient.configured()||SessionVault.userId(c).isEmpty())return;Constraints network=new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();WorkManager.getInstance(c).enqueueUniqueWork("callguard-sync",ExistingWorkPolicy.KEEP,new OneTimeWorkRequest.Builder(CloudWorker.class).setConstraints(network).build());}
    static void periodic(Context c){if(!CloudClient.configured()||SessionVault.userId(c).isEmpty())return;WorkManager.getInstance(c).enqueueUniquePeriodicWork("callguard-account",ExistingPeriodicWorkPolicy.KEEP,new PeriodicWorkRequest.Builder(CloudWorker.class,6,TimeUnit.HOURS).setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build());}
    static void cancel(Context c){WorkManager.getInstance(c).cancelUniqueWork("callguard-sync");WorkManager.getInstance(c).cancelUniqueWork("callguard-account");}
}
