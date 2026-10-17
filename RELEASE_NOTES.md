CallGuard 1.1.22-preview · Birden fazla Bluetooth cihazını bağımsız yönetme

- Menü Bluetooth görüşme sessizliği olarak genişletildi. Cihazlarım ekranında birden fazla araç/kulaklık/görüşme destekli cihaz eklenip ayrı yönetilebilir.
- Her cihazın adı, adresi, sessizlik tercihi ve önceki görüşme ayarı bağımsız saklanır. Bir cihazı açmak/kapatmak diğerlerinin ayarlarını değiştirmez.
- Sessizliği kapatınca cihaz yapılandırması silinmez. İstenirse yalnızca geri yükleme gerektirmeyen yapılandırmalar Unut ile kaldırılır; Android Bluetooth eşleştirmesi silinmez.
- Önceki sürümün tek araç geri yükleme kaydı yeni biçime atomik olarak taşınır. Başarısız/geçersiz aktarım eski kaydı silmez ve Bluetooth ayarı değiştirilmez.
- Tercihler bu telefonda yerel saklanır; uygulama güncellemesi/yeniden açılmasıyla korunur. Başka telefona yanlış donanım ayarı taşımamak için bulut/Drive yedeğine dahil değildir.
- Kayıtlı tercih ile canlı doğrulanmış durum ayrılır. İşlemi doğrulanamayan cihazın geri yükleme kaydı korunur; o cihazın kaydı unutulamaz.
- Özellik HFP görüşme profiline yöneliktir; yalnızca müzik destekleyen hoparlör veya saatin kendi uygulamasından gelen ekran bildirimleri için engelleme sözü vermez. Müzik profili değiştirilmez.
- Kısa testler iki cihazın bağımsız açılması/geri yüklenmesi, yeniden başlatma sonrası kayıtların korunması, eski kaydın taşınması, disk/RPC hataları ve güvenli unutma davranışını kapsar. Gerçek donanım testi kullanıcı cihazında yapılmalıdır.
