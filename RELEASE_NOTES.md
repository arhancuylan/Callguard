CallGuard 1.1.24-preview · Android 9 WhatsApp sesli çağrı uyumluluğu

- Note 8 teknik kaydındaki çağrı biçimi düzeltildi: WhatsApp sesli çağrıyı grup özeti olarak işaretlediğinde ve yeni CallStyle alanları bulunmadığında, sesli çağrı metni ile açık Reddet/Yanıtla eylemleri birlikte kabul edilir.
- Önceki kontrol aynı eski bildirim biçiminde yalnızca görüntülü çağrılara izin veriyordu; sesli çağrılar kural değerlendirmesine ve geçmişe ulaşamıyordu.
- Çağrı kategorisi, WhatsApp/Business kaynağı, tek ve açık reddet eylemi ve yanıtla kanıtı zorunluluğu korunur. Devam eden çağrılar, mesajlar ve reddet eylemi bulunmayan sonraki bildirimler işlem görmez.
- Ortak grup özetleri ayrıca silinmez; yalnızca doğrulanmış reddet eylemi çalıştırılır. Normal engelleme için ADB/Shizuku gerekmez.
- Note 8 kaydının biçimi, Business/İngilizce eylem sırası ve yanlış eşleşmeleri önleyen durumlar için kısa testler eklendi. Gerçek Android 9 telefon testi kullanıcı cihazında yapılacaktır.
