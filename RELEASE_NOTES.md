CallGuard 1.1.19-preview · Bildirimden kablosuz ADB eşleştirme

- Android’in geçici eşleştirme kodu penceresini kapatmadan, CallGuard bildirimindeki Kod gir düğmesi ile kod gönderilir. Bölünmüş ekran veya başka uygulama gerekmez.
- Bildirimle eşleştirmeyi başlat düğmesi gerekli bildirim iznini ister, kısa süreli dinlemeyi başlatır ve geliştirici ayarlarını açar.
- Yalnızca bu telefona ait ADB pairing servisi DNS-SD üzerinden bulunur. Port bulunursa yalnızca 6 haneli kod girilir. Bulunmazsa bildirime “port kod” yazılabilir.
- Başarılı eşleştirmeden sonra yerel ADB bağlantısı otomatik aranıp doğrulanır; bulunamazsa mevcut bağlantı portu ekranı kullanılabilir.
- Dinleme en fazla altı dakika sürer; Vazgeç düğmesi veya tamamlanma ile biter. Yeniden başlatılmaz, kod kaydedilmez ve dış uygulamaların çağırabileceği servis sunulmaz.
- Mevcut araç ayarı ve geri yükleme kaydı korunur; normal WhatsApp koruması değişmez.
- Kısa birim testleri, lint, APK/AAB derlemesi ve güncelleme imzası kontrolü uygulanır. Samsung bildirim perdesi/gerçek ADB eşleştirmesi kullanıcı cihazında sınanmalıdır.
