package com.callguard;
import android.content.*;
import android.database.*;
import android.net.Uri;
/** DUMP-protected local test state, excluded from the distributed release APK. */
public final class TestStateProvider extends ContentProvider {
    public boolean onCreate(){return true;}
    public Cursor query(Uri uri,String[] p,String sel,String[] args,String order){
        MatrixCursor out=new MatrixCursor(new String[]{"connected","count","result","video","today"});
        try(Store s=new Store(getContext());Cursor h=s.history()){
            String result="EMPTY";int video=-1;int count=h.getCount();if(h.moveToFirst()){result=h.getString(3);video=h.getInt(2);}
            out.addRow(new Object[]{CallNotificationService.connected?1:0,count,result,video,s.today()});
        }return out;
    }
    public String getType(Uri u){return "vnd.android.cursor.item/callguard-test";}
    public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
    public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
    public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
}
