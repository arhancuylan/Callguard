package com.callguard.fixture;
import android.app.*;
import android.content.*;
import android.graphics.drawable.Icon;

public final class Calls extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent intent){
        NotificationManager manager=c.getSystemService(NotificationManager.class);
        if("fixture.POST".equals(intent.getAction())){
            c.getSharedPreferences("state",0).edit().putBoolean("declined",false).putBoolean("answered",false).commit();
            manager.createNotificationChannel(new NotificationChannel("calls","CI calls",NotificationManager.IMPORTANCE_HIGH));
            PendingIntent decline=PendingIntent.getBroadcast(c,1,new Intent(c,Calls.class).setAction("fixture.DECLINE"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            PendingIntent answer=PendingIntent.getBroadcast(c,2,new Intent(c,Calls.class).setAction("fixture.ANSWER"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            Person person=new Person.Builder().setName(intent.getStringExtra("name")==null?"Test Caller":intent.getStringExtra("name")).setUri("tel:"+(intent.getStringExtra("number")==null?"+905550001111":intent.getStringExtra("number"))).setImportant(true).build();
            boolean video=intent.getBooleanExtra("video",false);
            Notification notification=new Notification.Builder(c,"calls").setSmallIcon(android.R.drawable.sym_call_incoming).setCategory(Notification.CATEGORY_CALL)
                .setContentTitle(person.getName()).setContentText(video?"Görüntülü arama":"Sesli arama")
                .setStyle(Notification.CallStyle.forIncomingCall(person,decline,answer).setIsVideo(video)).build();
            // Actual captured WhatsApp regression: CallStyle plus GROUP_SUMMARY.
            notification.flags|=Notification.FLAG_GROUP_SUMMARY;
            manager.notify(23,notification);
        }else if("fixture.DECLINE".equals(intent.getAction())){
            c.getSharedPreferences("state",0).edit().putBoolean("declined",true).commit();manager.cancel(23);
        }else if("fixture.ANSWER".equals(intent.getAction())){
            c.getSharedPreferences("state",0).edit().putBoolean("answered",true).commit();manager.cancel(23);
        }else if("fixture.CLEAR".equals(intent.getAction()))manager.cancelAll();
    }
}
