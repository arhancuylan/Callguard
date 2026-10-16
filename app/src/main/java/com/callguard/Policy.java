package com.callguard;
import java.util.Locale;
public final class Policy {
    private Policy() {}
    public static String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " "); }
    public static String phone(String value) { return value == null ? "" : value.replaceAll("[^0-9+]", ""); }
    public static boolean voiceLabel(String s) { return s.equals("gelen sesli arama") || s.equals("sesli arama") || s.equals("incoming voice call") || s.equals("voice call") || s.equals("whatsapp sesli arama") || s.equals("whatsapp voice call"); }
    public static boolean rejectLabel(String s) { return s.equals("reddet") || s.equals("decline") || s.equals("reject"); }
    public static boolean answerLabel(String s) { return s.equals("yanıtla") || s.equals("cevapla") || s.equals("answer") || s.equals("accept"); }
}
