CallGuard 1.1.18-preview · Uygulama içi ADB araç sessizliği

- Shizuku uygulaması, SDK’sı, sağlayıcısı ve izin isteği kaldırıldı. Ayrı uygulama kurulmaz.
- Android 11+ kablosuz ADB eşleştirmesi CallGuard içinden yapılır. Geliştirici seçenekleri ve Android’in eşleştirme onayı gereklidir; uygulama bunları kendi kendine açmaz.
- Yalnızca aynı telefonun 127.0.0.1 adresine bağlanılır. Eşleştirme portu/kodu ve bağlantı portu için uygulama içinde yönlendirme vardır.
- Seçilen aracın görüşme profili, kısa ömürlü ADB shell yardımcısıyla okunur/değiştirilir/doğrulanır. Diğer Bluetooth cihazları ve müzik profili değiştirilmez. Araç sessizliği bütün Bluetooth görüşmelerini etkiler, kişi istisnası yoktur.
- Önceki sürümün yerel geri yükleme kaydı korunur. İşlem başarısız veya belirsizse kayıt silinmez.
- ADB kimliği Android Keystore ile şifreli saklanır; bulut/Drive yedeğine katılmaz. Eşleştirme kodu saklanmaz. İstenildiğinde ADB bağlantısı kesilebilir; bu işlem araç ayarını geri açmaz.
- Yeniden başlatma/ağ değişimi sonrası ADB bağlantısını yeniden açmak gerekebilir. Normal WhatsApp koruması ADB’den bağımsızdır.
- APK güncelleme imzası, birim testleri, lint ve yerel kütüphane envanteri derleme aşamasında kontrol edilir. Gerçek Samsung/Fiat kablosuz eşleştirme ve HFP davranışı bu platformda doğrulanamaz.
- Bu sürüm Whatsmeow içermez; o yöntem sonraki ayrı deney sürümüdür.
