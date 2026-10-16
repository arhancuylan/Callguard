package com.callguard.telecomtest;
import java.util.Set;
/** Pure policy: only explicitly identified incoming WhatsApp audio calls. */
public final class CallRule {
    public enum Decision { OUT_OF_SCOPE, OBSERVE, ALLOW, UNKNOWN_WITH_ALLOWLIST, REJECT }
    private CallRule() {}
    public static String number(String raw) {
        if(raw==null || !raw.matches("[+0-9() .-]+"))return "";
        String n=raw.replaceAll("[^+0-9]", "");
        if(n.startsWith("00"))n="+"+n.substring(2);
        if(!n.matches("\\+?[0-9]{5,15}"))return "";
        return n;
    }
    public static Decision decide(String pkg, boolean incoming, boolean ringing,
            boolean video, boolean armed, String caller, Set<String> allowed) {
        if(!("com.whatsapp".equals(pkg)||"com.whatsapp.w4b".equals(pkg))||!incoming||!ringing||video)
            return Decision.OUT_OF_SCOPE;
        if(!armed)return Decision.OBSERVE;
        String n=number(caller);
        if(!n.isEmpty() && allowed.contains(n))return Decision.ALLOW;
        // Never reject an unidentifiable call when a whitelist exception might apply.
        if(n.isEmpty() && !allowed.isEmpty())return Decision.UNKNOWN_WITH_ALLOWLIST;
        return Decision.REJECT;
    }
}
