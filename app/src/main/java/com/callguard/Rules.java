package com.callguard;

import java.text.Normalizer;
import java.util.*;

/** Pure policy shared by the listener, list editor and tests. */
public final class Rules {
    public enum Mode { ALL, WHITELIST, BLACKLIST }
    public enum Media { VOICE, VIDEO, BOTH }
    public static String label(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFKD).toLowerCase(Locale.ROOT)
            .replaceAll("[\\p{M}\\p{Cf}]", "").replace('ı', 'i').replaceAll("[\\p{Z}\\s]+", " ").trim();
    }
    public static String number(String raw) {
        if (raw == null) return "";
        String value = raw.trim().replaceFirst("(?i)^tel:", "");
        if (!value.matches("[+0-9() .-]+")) return "";
        value = value.replaceAll("[^+0-9]", "");
        if (value.startsWith("00")) value = "+" + value.substring(2);
        // Turkish local representation; explicit foreign international numbers remain intact.
        if (value.matches("0[0-9]{10}")) value = "+90" + value.substring(1);
        if (value.matches("[0-9]{10}")) value = "+90" + value;
        if (!value.matches("\\+[1-9][0-9]{6,14}")) return "";
        return value;
    }
    public static boolean decline(String value) {
        return Arrays.asList("reddet", "aramayı reddet", "aramayi reddet", "cagrıyı reddet", "cagriyi reddet", "decline", "decline call", "reject", "reject call", "goruntulu aramayi reddet", "decline video call", "reject video call").contains(label(value));
    }
    public static boolean answer(String value) {
        return Arrays.asList("yanıtla", "yanitla", "cevapla", "aramayı yanıtla", "aramayi yanitla", "answer", "answer call", "accept", "accept call", "goruntulu yanitla", "goruntulu aramayi yanitla", "goruntulu aramayi cevapla", "answer video call", "accept video call", "video answer", "goruntulu cevapla", "goruntulu olarak yanitla", "videolu yanitla", "videolu cevapla").contains(label(value));
    }
    public static final class Incoming {
        public final int declineIndex;
        public final Media media;
        Incoming(int index, Media media) { this.declineIndex = index; this.media = media; }
    }
    public static final class Analysis {
        public final Incoming incoming;
        public final String reason;
        Analysis(Incoming incoming,String reason){this.incoming=incoming;this.reason=reason;}
    }
    private static Analysis no(String reason){return new Analysis(null,reason);}
    public static boolean videoText(String text){String value=label(text);return value.contains("goruntulu")||value.contains("video")||value.contains("📹")||value.contains("🎥");}
    public static boolean voiceText(String text){String value=label(text);return value.contains("sesli arama")||value.contains("voice call")||value.contains("audio call");}
    public static Incoming incoming(String owner,String category,boolean summary,String text,int callType,Boolean isVideo,List<String> actions){
        return analyze(owner,category,summary,text,callType,isVideo,actions).incoming;
    }
    public static Analysis analyze(String owner,String category,boolean summary,String text,int callType,Boolean isVideo,List<String> actions){
        if(!("com.whatsapp".equals(owner)||"com.whatsapp.w4b".equals(owner)))return no("DESTEKLENMEYEN_UYGULAMA");
        if(!"call".equals(category))return no("CAGRI_KATEGORISI_YOK");
        if(callType==2||callType==3)return no("GELEN_CAGRI_DEGIL");
        boolean video=Boolean.TRUE.equals(isVideo)||videoText(text);
        boolean voice=Boolean.FALSE.equals(isVideo)&&callType==1||voiceText(text);
        for(String action:actions)if(answer(action)&&videoText(action))video=true;
        if(!video&&!voice)return no("SES_VIDEO_BILGISI_YOK");
        int decline=-1;boolean answer=false;
        for(int i=0;i<actions.size();i++){
            if(decline(actions.get(i))){if(decline!=-1)return no("BIRDEN_FAZLA_REDDET_EYLEMI");decline=i;}
            answer|=answer(actions.get(i));
        }
        if(decline<0)return no("REDDET_EYLEMI_TANINMADI");
        if(!answer&&callType!=1)return no("YANITLA_KANITI_YOK");
        boolean typed=callType==1&&isVideo!=null;
        // Android 9 WhatsApp marks live voice calls as group summaries and has no
        // CallStyle extras. Both explicit incoming actions plus positive media
        // evidence identify these calls; the summary flag alone is not a veto.
        if(summary&&!typed&&!((video||voice)&&answer))return no("GRUP_BILDIRIMI_KANITI_YETERSIZ");
        return new Analysis(new Incoming(decline,video?Media.VIDEO:Media.VOICE),"GELEN_CAGRI_TANINDI");
    }
    public static boolean selected(Media selection, Media call) { return selection == Media.BOTH || selection == call; }
    public static boolean block(Mode mode, boolean listed) {
        return mode == Mode.ALL || (mode == Mode.WHITELIST ? !listed : listed);
    }
    public static final class Contact {
        public final long id; public final String name, number; public final boolean white;
        public Contact(long id, String name, String number, boolean white) { this.id=id; this.name=name; this.number=number; this.white=white; }
    }
    public static boolean listed(String caller, String uri, List<Contact> contacts, boolean white) {
        String number = number(uri);
        if (number.isEmpty()) number = number(caller);
        for (Contact c : contacts) if (c.white == white) {
            // A real numeric identity wins; a matching display name cannot override a different number.
            if (!number.isEmpty()) { if (number.equals(c.number)) return true; }
            else if (!label(c.name).isEmpty() && label(c.name).equals(label(caller))) return true;
        }
        return false;
    }
    private Rules() {}
}
