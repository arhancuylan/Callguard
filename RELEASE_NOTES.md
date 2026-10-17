CallGuard 1.1.20-preview · Otomatik kurulum kontrolü ve anlaşılır adımlar

- Araç seç düğmesi artık ADB kurulumu açmak yerine doğrudan eşleşmiş Bluetooth cihazlarını gösterir. Kurulum eksikse seçilen araç korunur ve kurulum sonrası tekrar onayla sessizliği açılır.
- Kurulum ekranında beş adım, tamamlananlarda yeşil ✓ ve eksik/doğrulanamayanlarda kırmızı ✕ ile gösterilir. Üstte yalnızca sıradaki adımın eylemi bulunur; tamamlanan adımlar otomatik atlanır.
- Ekran açıkken yerel kablosuz ADB bulunur, mevcut CallGuard kimliğiyle bağlantı denenir ve shell yetkisi canlı olarak doğrulanır. Kayıtlı eşleştirme çalışıyorsa tekrar kod istenmez.
- Canlı yetkili bağlantı ilk dört adımı tamamlar. Araç sessizliği yalnızca kayıtlı aracın gerçek görüşme ayarı okunarak doğrulanır; geri yükleme kaydı tek başına yeşil gösterilmez.
- Android geliştirici ayarını doğrudan göstermediğinde ziyaret edilmiş ekran tamamlandı sayılmaz; kablosuz ADB üzerinden doğrulanır veya kullanıcı açık olduğunu ayrıca onaylayabilir. Onay kaynağı ekranda belirtilir.
- Otomatik kontrol yalnızca kurulum ekranı ön plandayken çalışır. Önceki bildirimden kod girme ve elle bağlantı portu seçenekleri korunur.
- Kısa birim testleri, lint, APK/AAB derlemesi ve güncelleme imzası kontrol edilir. Gerçek Samsung ayar menüleri ve araç davranışı kullanıcı cihazında sınanmalıdır.
