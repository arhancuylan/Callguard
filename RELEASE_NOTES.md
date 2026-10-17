CallGuard 1.1.12-preview · Araç sessizliği (Shizuku, deneysel)

- Ayarlara WhatsApp korumasından bağımsız Araç sessizliği bölümü eklendi.
- Resmi Shizuku kurulumu, açık yetkilendirme, eşleşmiş araç seçimi ve tüm görüşmelere etkisi için uygulama içi açıklama/onay eklendi.
- Yalnızca seçilen cihazın HEADSET/HFP görüşme profili kapatılır; A2DP müzik ve diğer cihazların profilleri değiştirilmez. Beyaz liste istisnası yoktur; telefonun kendisinde çağrı devam edebilir. LE Audio desteklenmez.
- Eski politika işlemden önce yerelde kalıcı kaydedilir. Kapatırken aynı cihazın önceki ayarı geri yüklenir. Belirsizlik/izin kaybı varsa geri alma kaydı korunur ve doğrulanmış aktif durum gösterilmez.
- Shizuku yokken Bluetooth araç ayarlarındaki Aramalar seçeneğiyle elle geri alma yolu sunulur. Uygulamayı kaldırmadan/verilerini temizlemeden önce modu kapatmak gerekir.
- Kurulum/sınırlar: diagnostics/car-silence-setup.txt. Ödeme kapalıdır.

Kısa politika birim testleri, lint, APK/AAB derlemesi ve imza kontrolleri çalıştırılır. Bu ortamda gerçek Shizuku + Samsung + araç uçtan uca testi yapılamaz. Deneysel araç modu telefonda doğrulanmalıdır. Mevcut WhatsApp reddetme, bildirim temizliği ve yedekleme korunur.
