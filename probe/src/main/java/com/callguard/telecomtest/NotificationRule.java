package com.callguard.telecomtest;
import java.util.*;
/** Independently implemented notification action policy; no third-party source included. */
public final class NotificationRule {
    private NotificationRule() {}
    public static String label(String raw){return raw==null?"":raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+"," ");}
    public static boolean decline(String raw){return Set.of("reddet","aramayı reddet","aramayi reddet","çağrıyı reddet","cagriyi reddet","decline","decline call","reject","reject call").contains(label(raw));}
    public static boolean answer(String raw){return Set.of("yanıtla","yanitla","cevapla","aramayı yanıtla","aramayi yanitla","answer","answer call","accept","accept call").contains(label(raw));}
    public static int declineIndex(String pkg,String category,boolean summary,String text,int callType,
            Boolean callIsVideo,List<String> actions){
        if(!("com.whatsapp".equals(pkg)||"com.whatsapp.w4b".equals(pkg))||!"call".equals(category)||summary)return -1;
        String body=label(text);
        if(Boolean.TRUE.equals(callIsVideo)||body.contains("video")||body.contains("görüntülü")||body.contains("goruntulu")||body.contains("📹")||body.contains("🎥"))return -1;
        if(callType==2||callType==3)return -1; // ongoing / screening CallStyle
        boolean voice=body.contains("sesli arama")||body.contains("voice call")||body.contains("audio call")||(callType==1&&Boolean.FALSE.equals(callIsVideo));
        if(!voice)return -1;
        int reject=-1;boolean accept=false;
        for(int i=0;i<actions.size();i++){if(decline(actions.get(i))){if(reject!=-1)return -1;reject=i;}accept|=answer(actions.get(i));}
        return reject>=0&&(accept||callType==1)?reject:-1;
    }
}
