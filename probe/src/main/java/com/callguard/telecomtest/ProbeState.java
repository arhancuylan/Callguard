package com.callguard.telecomtest;
import android.content.*;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.*;
final class ProbeState {
    static SharedPreferences prefs(Context c){return c.getSharedPreferences("telecom_probe",Context.MODE_PRIVATE);}
    static Set<String> allowed(Context c){return new HashSet<>(prefs(c).getStringSet("allowed",Collections.emptySet()));}
    static void log(Context c,String message){
        SharedPreferences p=prefs(c);
        synchronized(ProbeState.class){
            try{
                JSONArray old=new JSONArray(p.getString("log","[]"));
                JSONArray next=new JSONArray();
                next.put(new SimpleDateFormat("HH:mm:ss.SSS",new Locale("tr","TR")).format(new Date())+" · "+message);
                for(int i=0;i<old.length()&&i<299;i++)next.put(old.getString(i));
                p.edit().putString("log",next.toString()).apply();
            }catch(JSONException e){p.edit().putString("log","[]").apply();}
        }
    }
    static String logs(Context c){try{JSONArray a=new JSONArray(prefs(c).getString("log","[]"));StringBuilder s=new StringBuilder();for(int i=0;i<a.length();i++)s.append(a.getString(i)).append("\n\n");return s.length()==0?"Henüz olay yok.":s.toString();}catch(JSONException e){return "Kayıt okunamadı.";}}
}
