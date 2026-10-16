package com.callguard;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.*;
public final class Store extends SQLiteOpenHelper {
    public Store(Context c) { super(c, "callguard.db", null, 1); }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE allowed (id INTEGER PRIMARY KEY, name TEXT NOT NULL, phone TEXT NOT NULL, UNIQUE(name,phone))");
        db.execSQL("CREATE TABLE history (id INTEGER PRIMARY KEY, caller TEXT NOT NULL, result TEXT NOT NULL, time INTEGER NOT NULL)");
    }
    public void onUpgrade(SQLiteDatabase db,int oldV,int newV) {}
    public void add(String name,String phone) { ContentValues v=new ContentValues(); v.put("name",name);v.put("phone",phone);getWritableDatabase().insertWithOnConflict("allowed",null,v,SQLiteDatabase.CONFLICT_IGNORE); }
    public List<String[]> allowed() { List<String[]> r=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT id,name,phone FROM allowed ORDER BY name",null)){while(c.moveToNext())r.add(new String[]{c.getString(0),c.getString(1),c.getString(2)});}return r; }
    public void remove(String id) { getWritableDatabase().delete("allowed","id=?",new String[]{id}); }
    public boolean permits(String caller) {
        if(caller==null || caller.trim().isEmpty())return false;
        String name=Policy.normalize(caller), number=Policy.phone(caller);
        for(String[] a:allowed()) if(name.equals(Policy.normalize(a[1])) || (!number.isEmpty() && number.equals(Policy.phone(a[2]))))return true;
        return false;
    }
    public void log(String caller,String result) { ContentValues v=new ContentValues();v.put("caller",caller==null?"Bilinmeyen kişi":caller);v.put("result",result);v.put("time",System.currentTimeMillis());getWritableDatabase().insert("history",null,v);getWritableDatabase().execSQL("DELETE FROM history WHERE id NOT IN (SELECT id FROM history ORDER BY id DESC LIMIT 500)"); }
    public List<String[]> history() {List<String[]> r=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT caller,result,time FROM history ORDER BY id DESC LIMIT 500",null)){while(c.moveToNext())r.add(new String[]{c.getString(0),c.getString(1),c.getString(2)});}return r;}
    public void clear() {getWritableDatabase().delete("history",null,null);}
}
