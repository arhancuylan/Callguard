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
    public boolean enabled() {return prefs().getBoolean("enabled",false);}
    public boolean recordOnly() {return prefs().getBoolean("recordOnly",false);}
    public void setting(String key,String value) {prefs().edit().putString(key,value).apply(); changed();}
    public void setting(String key,boolean value) {prefs().edit().putBoolean(key,value).apply(); changed();}
    public void add(String name,String number,boolean white) {
        String normalized=Rules.number(number);
        if(normalized.isEmpty())throw new IllegalArgumentException("Geçerli bir numara yaz: +905xxxxxxxxx");
        ContentValues v=new ContentValues();v.put("name",name.trim().isEmpty()?normalized:name.trim());v.put("number",normalized);v.put("white",white?1:0);
        getWritableDatabase().insertWithOnConflict("contacts",null,v,SQLiteDatabase.CONFLICT_REPLACE); changed();
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
    public int today() {
        Calendar cal=Calendar.getInstance();cal.set(Calendar.HOUR_OF_DAY,0);cal.set(Calendar.MINUTE,0);cal.set(Calendar.SECOND,0);cal.set(Calendar.MILLISECOND,0);
        try(Cursor c=getReadableDatabase().rawQuery("SELECT count(*) FROM history WHERE at>=? AND result=?",new String[]{String.valueOf(cal.getTimeInMillis()),"Reddetme isteği gönderildi"})){c.moveToFirst();return c.getInt(0);}
    }
    public void clearHistory(){getWritableDatabase().delete("history",null,null);changed();}
    private void changed(){context.sendBroadcast(new Intent(context.getPackageName()+".CHANGED").setPackage(context.getPackageName()));}
}
