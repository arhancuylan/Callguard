CallGuard 1.1.11-preview · Yalnızca engellenen çağrı bildirimini temizleme

- WhatsApp'ın doğrulanmış Reddet isteği döndükten sonra aynı çağrının tek bildirimi için silme isteği gönderilir. İstek dönüşü gerçek çağrı kapanışını kanıtlamaz.
- Bildirim anahtarı, paket, oluşturulma/gönderim zamanları ve Reddet eylemi yeniden karşılaştırılır. Mesaj, yeni çağrı, izin verilen çağrı, yalnızca kayıt modu ve ortak bildirim özetleri temizlenmez.
- Sonradan ayrı oluşan cevapsız çağrı bildirimi veya değişmiş bildirim güvenli eşleştirme olmadığı için silinmez. Android silme isteğini reddedebilir.
- Yerel teknik kayıtta temizlik isteği ve gözlenen kaldırma ayrı gösterilir; arayan adı/numarası eklenmez.
- Bluetooth araştırması diagnostics/bluetooth-cleanup-review.txt dosyasındadır. AOSP, Gadgetbridge, SpamBlocker-Extended, Bluetooth Bouncer ve Clear Mic Router incelendi. Normal uygulama yetkisiyle yalnızca engellenen WhatsApp çağrısının araç HFP olayını önceden durduran doğrulanmış yöntem bulunmadı. Cihazın Bluetooth profilleri değiştirilmez.

Ödeme kapalıdır. Temizliğin başka çağrıya/mesaja taşmasını önleyen birim testleri, lint, derleme ve imza kontrolleri çalıştırılır. Gerçek araç davranışı telefonda doğrulanmalıdır.
