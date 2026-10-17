CallGuard 1.1.5-preview · Call Guard by AC

- Google Drive, CallGuard bulutundan bağımsız ek yedekleme seçeneğidir. Google veya e-posta ile girişten bağımsız kullanılabilir.
- Drive için ayrıca hesap seçimi ve yalnızca drive.appdata izni istenir. Diğer Drive dosyalarına erişilmez.
- Drive'a şimdi yedekle: kişi listeleri, ayarlar ve en fazla 500 arama kaydı için tarihli ayrı kopyalar oluşturulur; eski kopyalar üzerine yazılmaz.
- Drive yedeklerini tarih seçerek geri yükle veya tek kopyayı onayla sil. Gizli uygulama dosyaları normal Drive listesinde görünmez.
- Drive geri yüklemesi korumayı ve otomatik CallGuard bulut yedeklemesini kapatır; mevcut sunucu yedeğinin otomatik ezilmesini önler. İkisini kullanıcı yeniden açabilir.
- CallGuard sunucusundaki otomatik yedekleme korunur. Drive işlemleri bu sürümde elle başlatılır. Hesap silme, ayrı Drive kopyalarını silmez; ilgili menüden ayrıca silinebilir.
- Google erişim belirteçleri sunucuya gönderilmez, uygulama tarafından dosyaya kaydedilmez.

Google Cloud projesinde Google Drive API etkin olmalı; OAuth Data Access bölümünde https://www.googleapis.com/auth/drive.appdata kapsamı tanımlanmalıdır. Mevcut Android OAuth istemcisi ve aynı imza kullanılır. Google'ın gerçek hesap/izin ekranı ve Drive işlemleri telefonda test edilecektir. Kısa birim testleri, lint ve derleme çalıştırılır; uzun emülatör testleri çalıştırılmaz. Ticari kilit/ödeme bu önizlemede kapalıdır.
