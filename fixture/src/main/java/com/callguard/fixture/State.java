package com.callguard.fixture;
import android.content.*;
import android.database.*;
import android.net.Uri;

public final class State extends ContentProvider {
    public boolean onCreate(){return true;}
    public Cursor query(Uri uri,String[] p,String s,String[] a,String sort){MatrixCursor c=new MatrixCursor(new String[]{"declined","answered"});c.addRow(new Object[]{getContext().getSharedPreferences("state",0).getBoolean("declined",false)?1:0,getContext().getSharedPreferences("state",0).getBoolean("answered",false)?1:0});return c;}
    public String getType(Uri uri){return "vnd.android.cursor.item/fixture";}
    public Uri insert(Uri uri,ContentValues v){throw new UnsupportedOperationException();}
    public int update(Uri uri,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
    public int delete(Uri uri,String s,String[] a){throw new UnsupportedOperationException();}
}
