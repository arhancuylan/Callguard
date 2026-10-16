package com.callguard;

/** Shared presentation and pause policy; no alarms or Bluetooth changes required. */
public final class ProtectionState {
    public static boolean paused(long until,long now){return until>now;}
    public static String title(boolean enabled,boolean access,boolean licensed,boolean paused,boolean schedule,boolean recordOnly,boolean connected){
        if(!access)return "Bildirim erişimi gerekli";
        if(!licensed)return "Kullanım hakkı gerekli";
        if(!enabled)return "Koruma kapalı";
        if(paused)return "Koruma duraklatıldı";
        if(!schedule)return "Zamanlama bekleniyor";
        if(!connected)return "Bildirim bağlantısı bekleniyor";
        if(recordOnly)return "Yalnızca kayıt açık";
        return "Koruma açık";
    }
    public static String reason(Rules.Mode mode,boolean listed){
        if(mode==Rules.Mode.ALL)return "Tüm gelen aramaları reddet kuralı";
        if(mode==Rules.Mode.WHITELIST)return listed?"Beyaz listede":"Beyaz listede bulunamadı";
        return listed?"Kara listede":"Kara listede bulunamadı";
    }
    public static String result(String status,String reason){return status+"\nNeden: "+reason;}
    public static boolean permitted(String result){return result.startsWith("İzin verildi")||result.startsWith("Seçilen arama türünün dışında");}
    private ProtectionState(){}
}
