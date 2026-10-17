package com.callguard;
import android.content.Context;
import java.text.SimpleDateFormat;
import java.util.*;
/** Local bounded structural diagnostics. No caller, number, message or credential values. */
final class CallDiagnostics {
    static synchronized void record(Context context,String detail){
        android.content.SharedPreferences prefs=context.getSharedPreferences("callguard-diagnostics",0);
        List<String> rows=new ArrayList<>(Arrays.asList(prefs.getString("events","").split("\\n")));
        rows.removeIf(String::isEmpty);
        rows.add(new SimpleDateFormat("HH:mm:ss.SSS",Locale.ROOT).format(new Date())+" "+detail);
        while(rows.size()>120)rows.remove(0);
        prefs.edit().putString("events",String.join("\n",rows)).apply();
    }
    static synchronized String report(Context context){return "CallGuard "+BuildConfig.VERSION_NAME+" · yerel teknik kayıt\nArayan adı/numarası ve mesaj içeriği içermez.\n"+context.getSharedPreferences("callguard-diagnostics",0).getString("events","Henüz olay yok.");}
    static synchronized void clear(Context context){context.getSharedPreferences("callguard-diagnostics",0).edit().clear().apply();}
    private CallDiagnostics(){}
}
