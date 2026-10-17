CallGuard 1.1.9-preview · Arka plan bağlantısı ve pil ayarları

- Android tarafından yönetilen bildirim dinleyicisinin otomatik bağlanması açıkça belirtilir.
- Servis bağlantısı koptuğunda veya uygulamaya dönüldüğünde, izin açıksa güvenli yeniden bağlanma istenir. İstekler 15 saniye arayla sınırlandırılır; sürekli sorgulama yapılmaz.
- Ayarlara Arka planda çalışma bölümü eklendi: gerçek servis bağlantısı, sistemin bildirdiği arka plan kısıtlaması, pil optimizasyonu ve tasarruf durumu görünür.
- Pil ayarlarına ve Samsung’un Asla uyumayan uygulamalar listesine kısayollar eklendi. Sistem ayarları kullanıcı tarafından değiştirilir; Samsung listesi uygulamadan doğrulanamaz.
- Son kullanılanlardan kapatma ile Zorla durdur arasındaki fark uygulama içinde açıklanır. Zorla durdur sonrası uygulamayı yeniden açmak gerekir.
- Ek izin, sürekli ön plan servisi, wake lock veya periyodik koruma işi eklenmedi. Mevcut sesli/görüntülü çağrı reddetme ve yedekleme yöntemleri korunur.
- Önceki kırmızı kapalı koruma kartı, uyarı simgesi, öncelikli arama ayarları ve ayrı yedekleme ekranı dahildir.

Ödeme kapalıdır. Kısa birim testleri/lint/derleme/imza doğrulaması yapılır; uzun emülatör testleri çalıştırılmaz. Üreticinin uyku yönetimi ve ekran kapalı gerçek çağrı testi kullanıcı cihazında doğrulanmalıdır.
