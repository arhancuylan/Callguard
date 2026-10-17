package com.callguard;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.*;

public final class Store extends SQLiteOpenHelper {
    private final Context context;
    public Store(Context c) { super(c.getApplicationContext(), "callguard-final.db", null, 1); context=c.getApplicationContext(); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE contacts (_id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,number TEXT NOT NULL,white INTEGER NOT NULL,UNIQUE(number,white))");
        db.execSQL("CREATE TABLE history (_id INTEGER PRIMARY KEY AUTOINCREMENT,caller TEXT NOT NULL,video INTEGER NOT NULL,result TEXT NOT NULL,at INTEGER NOT NULL,number TEXT NOT NULL DEFAULT '')");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) { }
    public SharedPreferences prefs() { return context.getSharedPreferences("callguard-final",Context.MODE_PRIVATE); }
    public Rules.Mode mode() { try { return Rules.Mode.valueOf(prefs().getString("rule","WHITELIST")); } catch (IllegalArgumentException e) {return Rules.Mode.WHITELIST;} }
    public Rules.Media media() { try {return Rules.Media.valueOf(prefs().getString("media","BOTH"));}catch(IllegalArgumentException e){return Rules.Media.BOTH;} }
    public String profile(){return prefs().getString("profile","custom");}
    private String profileKey(String key){return "profile_"+profile()+"_"+key;}
    public boolean scheduled(){return prefs().getBoolean(profileKey("scheduled"),false);}
    public int scheduleDays(){return prefs().getInt(profileKey("days"),127);}
    public int scheduleStart(){return prefs().getInt(profileKey("start"),540);}
    public int scheduleEnd(){return prefs().getInt(profileKey("end"),1080);}
    public boolean scheduleActive(){return ProtectionSchedule.active(scheduled(),scheduleDays(),scheduleStart(),scheduleEnd(),java.time.ZonedDateTime.now());}
    public Rules.Mode effectiveMode(){try{return Rules.Mode.valueOf(prefs().getString(profileKey("rule"),mode().name()));}catch(IllegalArgumentException e){return mode();}}
    public Rules.Media effectiveMedia(){try{return Rules.Media.valueOf(prefs().getString(profileKey("media"),media().name()));}catch(IllegalArgumentException e){return media();}}
    public void selectProfile(String profile){
        if(!Arrays.asList("custom","work","sleep","meeting").contains(profile))throw new IllegalArgumentException("Profil bulunamadı");
        android.content.SharedPreferences.Editor edit=prefs().edit();String prefix="profile_"+profile+"_";
        if(!prefs().contains(prefix+"initialized")){edit.putBoolean(prefix+"initialized",true).putString(prefix+"rule",mode().name()).putString(prefix+"media",media().name()).putBoolean(prefix+"scheduled",!profile.equals("custom"));edit.putInt(prefix+"days",profile.equals("work")?31:127).putInt(prefix+"start",profile.equals("sleep")?1320:profile.equals("meeting")?600:540).putInt(prefix+"end",profile.equals("sleep")?420:profile.equals("meeting")?660:1080);}
        edit.putString("profile",profile).apply();changed();
    }
    public void profileSetting(String key,int value){if(!Arrays.asList("days","start","end").contains(key))throw new IllegalArgumentException("Geçersiz ayar");prefs().edit().putInt(profileKey(key),value).apply();changed();}
    public void scheduleEnabled(boolean value){prefs().edit().putBoolean(profileKey("scheduled"),value).apply();changed();}
    public long pauseUntil(){return prefs().getLong("pauseUntil",0);}
    public boolean paused(){return ProtectionState.paused(pauseUntil(),System.currentTimeMillis());}
    public void pauseUntil(long until){prefs().edit().putLong("pauseUntil",Math.max(0,until)).apply();context.sendBroadcast(new Intent(context.getPackageName()+".CHANGED").setPackage(context.getPackageName()));}
    public boolean enabled() {return prefs().getBoolean("enabled",false);}
    public boolean recordOnly() {return prefs().getBoolean("recordOnly",false);}
    public void setting(String key,String value) {android.content.SharedPreferences.Editor edit=prefs().edit().putString(key,value);if(key.equals("rule")||key.equals("media"))edit.putString(profileKey(key),value);edit.apply(); changed();}
    public void setting(String key,boolean value) {android.content.SharedPreferences.Editor edit=prefs().edit().putBoolean(key,value);if(key.equals("enabled"))edit.remove("pauseUntil");edit.apply(); changed();}
    public void add(String name,String number,boolean white) {addAll(Collections.singletonList(new Rules.Contact(0,name,number,white)),white);}
    public void addAll(List<Rules.Contact> contacts,boolean white) {
        List<ContentValues> rows=new ArrayList<>();
        for(Rules.Contact contact:contacts){String normalized=Rules.number(contact.number);if(normalized.isEmpty())throw new IllegalArgumentException("Geçerli bir numara yaz: +905xxxxxxxxx");ContentValues value=new ContentValues();value.put("name",contact.name.trim().isEmpty()?normalized:contact.name.trim());value.put("number",normalized);value.put("white",white?1:0);rows.add(value);}
        if(rows.isEmpty())return;
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try{for(ContentValues value:rows){db.delete("contacts","number=? AND white<>?",new String[]{value.getAsString("number"),white?"1":"0"});if(db.insertWithOnConflict("contacts",null,value,SQLiteDatabase.CONFLICT_REPLACE)<0)throw new SQLiteException("Kişi kaydedilemedi");}db.setTransactionSuccessful();}finally{db.endTransaction();}changed();
    }
    public List<Rules.Contact> contacts() {
        List<Rules.Contact> values=new ArrayList<>();
        try(Cursor c=getReadableDatabase().query("contacts",null,null,null,null,null,"name COLLATE NOCASE ASC")) {
            while(c.moveToNext())values.add(new Rules.Contact(c.getLong(0),c.getString(1),c.getString(2),c.getInt(3)==1));
        } return values;
    }
    public void remove(long id) {getWritableDatabase().delete("contacts","_id=?",new String[]{String.valueOf(id)});changed();}
    public void history(String caller,boolean video,String result) { history(caller,"",video,result); }
    public void history(String caller,String number,boolean video,String result) {
        ContentValues v=new ContentValues();v.put("caller",caller==null||caller.trim().isEmpty()?"Bilinmeyen numara":caller);v.put("video",video?1:0);v.put("result",result);v.put("at",System.currentTimeMillis());v.put("number",Rules.number(number));
        getWritableDatabase().insertOrThrow("history",null,v);
        getWritableDatabase().execSQL("DELETE FROM history WHERE _id NOT IN (SELECT _id FROM history ORDER BY _id DESC LIMIT 500)");changed();
    }
    public Cursor history() {return getReadableDatabase().query("history",null,null,null,null,null,"at DESC, _id DESC");}
    private String[] todayRejectedArgs(){
        Calendar start=Calendar.getInstance();start.set(Calendar.HOUR_OF_DAY,0);start.set(Calendar.MINUTE,0);start.set(Calendar.SECOND,0);start.set(Calendar.MILLISECOND,0);
        Calendar end=(Calendar)start.clone();end.add(Calendar.DATE,1);
        return new String[]{String.valueOf(start.getTimeInMillis()),String.valueOf(end.getTimeInMillis()),"Reddetme isteği gönderildi%"};
    }
    public Cursor todayRejected(){return getReadableDatabase().query("history",null,"at>=? AND at<? AND result LIKE ?",todayRejectedArgs(),null,null,"at DESC, _id DESC");}
    public int today(){try(Cursor c=getReadableDatabase().rawQuery("SELECT count(*) FROM history WHERE at>=? AND at<? AND result LIKE ?",todayRejectedArgs())){c.moveToFirst();return c.getInt(0);}}
    public void clearHistory(){getWritableDatabase().delete("history",null,null);changed();}
    private void changed(){CloudSync.dirty(context);context.sendBroadcast(new Intent(context.getPackageName()+".CHANGED").setPackage(context.getPackageName()));}
}
