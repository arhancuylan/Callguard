package com.callguard;

/** Never show raw framework exceptions (which can include device addresses or internal strings). */
final class CarErrors {
    static String describe(Exception ex){
        if(ex instanceof SecurityException)return "Bluetooth iznini ve yerel ADB bağlantısını kontrol et. Varsa geri yükleme kaydı korundu.";
        String message=ex.getMessage();
        if(java.util.Arrays.asList(
            "Bluetooth kapalı veya kullanılamıyor.","Bluetooth cihaz listesi alınamadı.",
            "Önce kayıtlı aracın ayarını geri yükle.","Geçersiz araç adresi.",
            "Bu cihazın görüşme profili zaten kapalı. Ayarı değiştirmedim.",
            "Önceki görüşme ayarı okunamadı; değişiklik yapılmadı.",
            "Kapatma doğrulanamadı. Geri yükleme kaydı korundu.",
            "Geri yükleme kaydı geçersiz. Bluetooth ayarından Aramalar’ı kontrol et.",
            "Araç ayarı okunamadı. Geri yükleme kaydı korundu.",
            "Geri yükleme doğrulanamadı. Kayıt korundu.",
            "Geri yükleme kaydı yazılamadı. Araç ayarı değiştirilmedi.",
            "Araç ayarı geri yüklendi, kayıt temizlenemedi. Yeniden kontrol et."
        ).contains(message))return message;
        return "Araç işlemi tamamlanamadı. Bluetooth açıkken yeniden dene; Kurulum ve durum bölümünden eşleştirme kodunu ve bağlantı portunu kontrol et. Varsa geri yükleme kaydı korundu.";
    }
    private CarErrors(){}
}
