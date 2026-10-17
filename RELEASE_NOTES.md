CallGuard 1.1.7-preview · Görüntülü çağrı algılama uyumluluğu

- Referans Call Blocker APK'sının talimat dökümü yeniden incelendi: sesli ve görüntülü reddetme aynı bildirim PendingIntent yolunu kullanır. Ayrı Telecom/Bluetooth/video protokolü bulunmadı.
- Native CallStyle alanları eksik grup görüntülü çağrı bildirimleri, pozitif video kanıtı ve hem Reddet hem Yanıtla eylemi varsa desteklenir.
- Büyük metin, alt metin ve metin satırları da ses/video algılamasında değerlendirilir.
- Görüntülü yanıtlama düğmesi etiketleri desteklenir. Native gelen CallStyle'da WhatsApp'a ait decline/answer PendingIntent alanları, düğme etiketi yerine güvenli kanıt olarak kullanılabilir.
- Devam eden/aramayı tarama bildirimleri reddedilmez. Rastgele ilk eyleme basılmaz.
- Ayarlar > Çağrı tanılaması: yalnızca cihazda tutulan son 120 teknik olay; isim, numara, hesap ve mesaj içeriği yok. Kayıt kullanıcı tarafından kopyalanabilir veya temizlenebilir.
- Google/e-posta/Drive yedekleme korunur. Ticari kilit ve ödeme kapalıdır.

Bu bir uyumluluk düzeltmesidir; kullanıcının gerçek görüntülü çağrı bildiriminin içeriği henüz yakalanmadığından cihazda kesin çözüm doğrulanmış değildir. Yeni sürümde sesli ve görüntülü çağrı ayrı test edilecek; hata sürerse yerel teknik kayıt ile kesin neden belirlenecektir. Uzun emülatör testleri çalıştırılmaz.
