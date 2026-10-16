package com.callguard.telecomtest;
import java.util.*;
import java.text.Normalizer;
/** Independently implemented notification action policy; no third-party source included. */
public final class NotificationRule {
    public enum Reason { READY, UNSUPPORTED_OWNER, NOT_CALL, SUMMARY_WITHOUT_CALLSTYLE,
        VIDEO, NOT_INCOMING, UNCONFIRMED_VOICE, NO_DECLINE_LABEL, DUPLICATE_DECLINE_LABEL, NO_ANSWER_EVIDENCE }
    public static final class Result {
        public final int index;
        public final Reason reason;
        Result(int index,Reason reason){this.index=index;this.reason=reason;}
    }
    private NotificationRule() {}
    public static String label(String raw){
        if(raw==null)return "";
        return Normalizer.normalize(raw,Normalizer.Form.NFKD).toLowerCase(Locale.ROOT)
            .replaceAll("[\\p{M}\\p{Cf}]", "").replaceAll("[\\p{Z}\\s]+"," ").trim();
    }
    public static boolean decline(String raw){return Set.of("reddet","aramayı reddet","aramayi reddet","cagrıyı reddet","cagriyi reddet","decline","decline call","reject","reject call").contains(label(raw));}
    public static boolean answer(String raw){return Set.of("yanıtla","yanitla","cevapla","aramayı yanıtla","aramayi yanitla","answer","answer call","accept","accept call").contains(label(raw));}
    private static Result no(Reason reason){return new Result(-1,reason);}
    public static Result analyze(String pkg,String category,boolean summary,String text,int callType,
            Boolean callIsVideo,List<String> actions){
        if(!("com.whatsapp".equals(pkg)||"com.whatsapp.w4b".equals(pkg)))return no(Reason.UNSUPPORTED_OWNER);
        if(!"call".equals(category))return no(Reason.NOT_CALL);
        boolean typedAudio=callType==1&&Boolean.FALSE.equals(callIsVideo);
        // A summary flag alone does not invalidate an explicitly typed incoming audio call.
        if(summary&&!typedAudio)return no(Reason.SUMMARY_WITHOUT_CALLSTYLE);
        String body=label(text);
        if(Boolean.TRUE.equals(callIsVideo)||body.contains("video")||body.contains("goruntulu")||body.contains("📹")||body.contains("🎥"))return no(Reason.VIDEO);
        if(callType==2||callType==3)return no(Reason.NOT_INCOMING);
        boolean voice=body.contains("sesli arama")||body.contains("voice call")||body.contains("audio call")||typedAudio;
        if(!voice)return no(Reason.UNCONFIRMED_VOICE);
        int reject=-1;boolean accept=false;
        for(int i=0;i<actions.size();i++){
            if(decline(actions.get(i))){if(reject!=-1)return no(Reason.DUPLICATE_DECLINE_LABEL);reject=i;}
            accept|=answer(actions.get(i));
        }
        if(reject<0)return no(Reason.NO_DECLINE_LABEL);
        if(!accept&&callType!=1)return no(Reason.NO_ANSWER_EVIDENCE);
        return new Result(reject,Reason.READY);
    }
    public static int declineIndex(String pkg,String category,boolean summary,String text,int callType,Boolean callIsVideo,List<String> actions){
        return analyze(pkg,category,summary,text,callType,callIsVideo,actions).index;
    }
}
