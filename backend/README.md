# CallGuard kendi sunucu kurulumu

Firebase kullanılmaz. API Node.js 22, veritabanı PostgreSQL 17, TLS Caddy üzerindedir. Google yalnızca kimlik doğrulama ve Play satın alma doğrulama sağlayıcısıdır. E-posta ile kayıt, doğrulama ve şifre yenileme SMTP ile çalışır.

## Henüz canlıya alınmadı

Bu depoda kurulum paketi ve Android entegrasyonu vardır; alan adı, sunucu erişimi, Google OAuth ve Play Console yapılandırmaları bu oturumda sağlanmadı. `callguard.mktech.net.tr` önerilen adrestir, oluşturulduğu iddia edilmez. Önizleme APK'sında API adresi boş bırakılır; hiçbir yedek yüklenmez ve ücret alınmaz. Ticari derleme, HTTPS API olmadan oluşturulamaz.

## Hosting seçimi

- VPS/VDS/Docker: bu Compose paketi kullanılabilir.
- Paylaşımlı cPanel/Plesk: uzun süre çalışan Node.js, PostgreSQL ve dışarıya HTTPS istekleri desteklenmeli. Desteklenmiyorsa mevcut teklif uygulamasına dokunmadan ayrı VPS gerekir; alan adı aynı kalabilir.
- Mevcut 80/443 portları kullanılıyorsa ikinci Caddy başlatılmamalı. Mevcut Nginx/Plesk proxy'sine yalnızca `callguard.mktech.net.tr` sanal sunucusu eklenmeli, API özel ağda yayınlanmalı. Mevcut teklif uygulamasının yapılandırması değiştirilmemeli.
- Yeni alt alan adı için A/AAAA kaydı sunucuya yönlenmeli. Sertifika bu alt alan adını kapsamalı. Standalone Caddy bu sertifikayı otomatik alır.

## Kurulum adımları

1. `backend/.env.example` dosyasını sunucuda `.env` olarak kopyala. Gerçek sırları GitHub'a koyma.
2. Bağımsız rastgele `POSTGRES_PASSWORD`, `ACCOUNT_ID_KEY`, `DATA_KEY` üret. DATA_KEY tam 32 baytın Base64 gösterimi olmalı. Örnek: `openssl rand -base64 32`. ACCOUNT_ID_KEY farklı bir değer olmalı ve değişmemeli.
3. DOMAIN ve PUBLIC_ORIGIN değerlerini onaylanan alt alan adıyla ayarla. SMTP_URL ve MAIL_FROM için doğrulanmış bir gönderici kullan.
4. Google Cloud Console'da Web OAuth istemcisi ve `com.callguard.mobile` için Android istemcisi oluştur. Android istemcisinde Play App Signing SHA-1/SHA-256 bilgilerini kullan. Web istemci kimliğini GOOGLE_WEB_CLIENT_ID alanına yaz. Web istemci gizli anahtarı Android'e konmaz.
5. Google Play Console'da `callguard_pro_lifetime` adlı tek seferlik **satın alma** ürünü oluştur; kiralama/abonelik/tüketilebilir ürün olarak ayarlama. Fiyatı Console'da belirle. Uygulama fiyatı Google Play'den alır; 249 TL sabit kodlanmamıştır.
6. Android Publisher API'yi etkinleştir; doğrulama hizmet hesabına bu uygulama için gereken en az Play Console sipariş/satın alma yetkisini ver. JSON anahtarı yalnızca sunucuda `secrets/play-publisher.json` olarak, API konteynerindeki node kullanıcısının okuyabileceği biçimde tut. Anahtarı bu sohbete gönderme.
7. `docker compose --env-file .env up -d --build` çalıştır. Sağlık kontrolü: `https://callguard.mktech.net.tr/health`.
8. Yeni bir test hesabıyla kayıt/doğrulama, giriş, yedek yükleme/geri yükleme, hesap silme ve Play test kanalında satın alma/geri yükleme kontrolleri yap. Önizleme APK'sı üzerinden gerçek satın alma test edilemez.
9. Google Play'in veri güvenliği, gizlilik politikası, hesap silme web adresi ve satın alma metinlerini gerçek işletme bilgileriyle tamamla. Bu belge bir hukuki politika değildir.

## Android / GitHub Actions değişkenleri

Depo Actions variables:
- `CALLGUARD_API_BASE`: HTTPS kök API, örnek `https://callguard.mktech.net.tr`
- `CALLGUARD_GOOGLE_CLIENT_ID`: Web OAuth istemci kimliği
- `CALLGUARD_COMMERCIAL`: yalnızca sunucu/Play yapılandırması doğrulandığında `true`

Play dağıtımından önce AAB üretimi ve Play App Signing yapılandırması tamamlanmalıdır. Mevcut APK imza anahtarı pilot dağıtım içindir; mağaza yükleme anahtarının kontrollü şekilde yönetilmesi gerekir. Mevcut cihaz test sürümü aynı uygulama kimliğini kullandığından imza değişikliği geçişi ayrıca planlanmalıdır.

## Veri ve erişim

- Oturumlar rastgele tokenla, sunucuda yalnızca token özetiyle saklanır. Android tokenı Android Keystore ile şifreler.
- Her API işlemi hesabı oturumdan belirler; istemcinin gönderdiği kullanıcı ID'sine göre başka hesaba erişim sağlanmaz.
- Yedekler ve satın alma tokenları AES-256-GCM ile şifrelenir. Veritabanı internete açılmaz.
- Yedek yalnızca CallGuard listeleri, profil/ayarları ve en fazla 500 geçmiş kaydıdır; telefon rehberinin tamamı yüklenmez. Android izinleri, oturum ve Pro/deneme hakları yedekten alınmaz.
- Eski revizyonlu cihaz başka cihazın yedeğini sessizce ezemez; çatışma bildirilir. Silme/üzerine yazma kullanıcı onayı ister.
- Yedi günlük deneme sunucu saatinde, hesap başına bir kez başlar. Uygulamayı kaldırıp kurmak aynı hesap için süreyi sıfırlamaz. Farklı hesap açmayı bütünüyle engellediği iddia edilmez.
- Satın alma durumu sunucuda Google Play API üzerinden doğrulanır, hesapla bağlanır ve acknowledge edilir. PENDING ödeme Pro açmaz. Yeniden doğrulamada iptal/iade edilmiş ürün Pro hakkı vermez.
- Hesap silinince profil, oturumlar, e-posta belirteçleri ve bulut yedeği silinir. Tekrar deneme/satın alma suistimalini önleyen asgari hesap özeti ve satın alma doğrulama kaydı ayrıca tutulur; saklama süresi gerçek gizlilik politikasında açıklanmalıdır.
- API'ye ek olarak günlük şifreli sunucu yedeği, veritabanı/anahtar kurtarma, SMTP teslim takibi ve bakım/sürüm güncellemeleri işletilmelidir. DATA_KEY kaybolursa yedekler çözülemez.
- Mevcut şablon prodüksiyon güvenlik denetimi veya tüm ödeme senaryolarının test edildiği anlamına gelmez. Uzun emülatör testleri kullanıcı isteğiyle varsayılan kapalıdır; burada hızlı mantık/derleme kontrolleri uygulanır.
