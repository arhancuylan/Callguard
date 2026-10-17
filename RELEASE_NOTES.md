CallGuard 1.1.16-preview · Araç seçme listesindeki null hatasını giderme

- Eşleşmiş cihaz listesi artık Shizuku’dan nullable JSON metni olarak alınmaz. Android Bluetooth API’si normal uygulama sürecinde kullanılır; Shizuku yalnızca görüşme profilini kontrol eder.
- Android 12+ için Yakındaki cihazlar / BLUETOOTH_CONNECT izni, araç listesi açılırken açıklamayla istenir. Eski Android için BLUETOOTH izni API 30 ile sınırlıdır. Yeni cihaz taraması veya konum izni eklenmez.
- Adsız, eksik veya yinelenmiş cihaz kayıtları güvenli işlenir. Boş liste ile liste okuma hatası birbirinden ayrılır.
- String.length gibi teknik hata mesajları doğrudan kullanıcıya gösterilmez. Teknik kayıtta yalnızca işlem aşaması ve hata sınıfı tutulur; cihaz adı/adresi yazılmaz. Geri yükleme kaydı hata halinde korunur.
- Mevcut araç profili seçimi/değişikliği, önceki ayarı geri alma ve Shizuku kurulum yardımcısı korunur.

Regresyon birim testleri, lint, APK/AAB derleme ve imza kontrolleri çalıştırılır. Ekran görüntüsündeki hatanın tam yığını elde olmadığı için uzaktaki cihazda kesin kaynak doğrulanamadı; hataya açık liste taşıma yolu kaldırıldı. Gerçek Samsung Bluetooth/araç işlemi telefonda tekrar denenmelidir. Ödeme kapalıdır.
