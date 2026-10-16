# CallGuard — Turhost cPanel / PHP + MySQL

Bu paket mevcut **Turhost cPanel, limitsiz hosting** bilgisine göre hazırlanmıştır. Node.js, PostgreSQL, Docker, SSH, Composer veya sürekli çalışan süreç sunucuda gerekli değildir. Composer bağımlılıkları yayımlanan ZIP'e dahil edilir. Bu dizin CallGuard'ın bu hosting için tercih edilen sunucu uygulamasıdır; eski `backend/functions` / Docker paketi bu kurulumda kullanılmaz.

**Henüz Turhost'a kurulmadı.** Hosting hesabının PHP sürümü/modülleri, dış bağlantıları, yeni veritabanı/alan adı kotası, SSL ve SMTP bilgileri uzaktan doğrulanmadı. “Limitsiz” ifadesi CPU, eşzamanlı istek veya e-posta kotasının olmadığı anlamına gelmez.

## Plan ve ayrım

Öncelikli API: `https://mktech.net.tr/callguard`.
Alternatif: `https://callguard.mktech.net.tr` — belge kökü yine `public_html/callguard` olabilir.

```
/home/CPANELUSER/
  callguard-private/               # Web kökünün DIŞINDA
    config.php                    # CallGuard'a özel DB, SMTP, bağımsız anahtarlar
    config.example.php
    play-service-account.json     # CallGuard'a özel, sunucuda tutulur
    schema.sql
    src/                          # PHP API
    vendor/                       # ZIP içinde hazır bağımlılıklar
  public_html/
    ... mevcut site ...           # Değiştirilmez
    mktech/                       # Değiştirilmez
    callguard/                    # Sadece yeni CallGuard dosyaları
      index.php
      .htaccess                   # Yalnızca bu dizine uygulanır
```

- Yeni, ayrı MySQL veritabanı: ör. `CPANELUSER_callguard`.
- Yeni, ayrı veritabanı kullanıcısı: ör. `CPANELUSER_cguser`. Yalnızca yeni DB üzerinde yetki verilir; MK Tech DB'sine yetki verilmez.
- Yalnızca `cg_` tablolardan oluşan ayrı şema kullanılır. Şema dosyası DROP veya CREATE DATABASE içermez; çalışma anında DDL çalıştırılmaz.
- MK Tech yapılandırması, oturumları, lisans tablosu, e-posta parolası ve erişim anahtarları kopyalanmaz/paylaşılmaz.
- `DATA_KEY` ve `ACCOUNT_KEY` yerine PHP yapılandırmasındaki `data_key` / `account_key` kullanılır. İki anahtar bağımsız, 32 rastgele bayt olmalı; MK Tech anahtarları kullanılmaz.
- Dosyaların tek cPanel hesabında olması işletim sistemi düzeyinde ayrı kiracı izolasyonu değildir. Kod/DB yetkileri ayrıdır; daha güçlü izolasyon gerekirse ayrı cPanel hesabı veya sunucu gerekir.

## Önce doğrulanacak hosting özellikleri

Turhost'ta cPanel olması bunların bu hesapta etkin olduğunu tek başına kanıtlamaz:

1. CallGuard dizini/alt alan adı için **PHP 8.2+**, `pdo_mysql`, `curl`, `openssl`, `mbstring`, JSON.
2. MySQL 5.7+ veya MariaDB 10.4+; InnoDB, utf8mb4 ve yeni DB/kullanıcı oluşturma hakkı. MySQL 8 / güncel MariaDB tercih edilir. Şema içe aktarılırken indeks/karakter kümesi desteği kontrol edilir.
3. Yeni dizinde `.htaccess`, rewrite ve `Authorization` başlığının PHP'ye aktarılması; GET/POST/PUT/DELETE isteklerinin ModSecurity tarafından engellenmemesi.
4. PHP'den Google OAuth sertifika adreslerine, `oauth2.googleapis.com` ve `androidpublisher.googleapis.com` adreslerine doğrulanan TLS ile dış HTTPS erişimi.
5. Ayrı SMTP göndericisi, doğrulanmış sertifika ve izin verilen 465/587 portu. Host/port bilgisi tahmin edilmez; cPanel e-posta istemci ayarlarından alınır. SMTP kotası kontrol edilir.
6. En az 4 MB JSON istek/yanıtını karşılayan PHP/WAF limitleri. Upload API'si JSON kullanır; istekler için sunucunun `post_max_size`/WAF sınırları da kontrol edilir.
7. Ana alan adının mevcut yönlendirme kuralları `/callguard` isteklerini başka uygulamaya götürmemeli. Götürüyorsa ana `.htaccess` dosyasına dokunmak yerine ayrı belge köklü alt alan adı tercih edilir.
8. SSL seçilen API adresinde geçerli olmalı. `/callguard` mevcut ana alan adı sertifikasından yararlanır. Alt alan adı seçilirse sertifikada o ad ayrıca bulunmalı; wildcard/AutoSSL olduğu varsayılmaz.

**Mevcut sitenin genel PHP sürümü değiştirilmez.** CallGuard için gereken sürüm dizin/alan adı bazında ayrı seçilemiyorsa önce Turhost desteğinden uyumluluk netleştirilir; MK Tech'i riske atarak genel ayar değiştirilmez.

İsteğe bağlı cPanel Terminal erişimi varsa özel dizinde `php check.php` yalnızca sürüm/modül/kurulum durumunu gösterir; anahtarları yazdırmaz. Web üzerinden çalışmaz. Terminal yoksa cPanel PHP modül ekranı ve `/health` kontrolü kullanılır; herkese açık `phpinfo()` dosyası yüklenmez.

## Kurulum — canlıya yükleme yapılmadan hazırlanmış adımlar

1. cPanel'de **yeni ve boş** bir CallGuard DB'si ve yalnızca ona erişen kullanıcı oluştur. MK Tech DB'sini seçme. phpMyAdmin'de yalnızca yeni DB'ye `callguard-private/schema.sql` dosyasını içe aktar.
2. ZIP'i bilgisayarda aç. Windows'ta `powershell -ExecutionPolicy Bypass -File .\tools\Prepare-CallGuard.ps1` komutu özel `config.php` dosyasını iki yeni anahtarla oluşturur. Mevcut dosyanın üzerine yazmaz ve sırları ekrana basmaz. Sonra DB/SMTP/Google değerlerini bu özel dosyada tamamla. Gerçek config veya parolaları GitHub'a/sohbete gönderme.
3. cPanel Dosya Yöneticisi'nde **hesabın ana dizinine** `callguard-private` dizinini koy. Bu dizin `public_html`, `www` veya herhangi bir alt alan adının web kökü altında olmamalı. `public_html/callguard` dosyalarını yalnızca boş CallGuard dizinine yükle. ZIP'i yanlışlıkla bütünüyle `public_html` altında açma.
4. API'nin varsayılan özel dizin yolu `/home/CPANELUSER/callguard-private` şeklindedir: `public_html/callguard/index.php` üzerinden hesap ana dizini otomatik bulunur. Farklı belge kökü varsa yalnızca CallGuard için `CALLGUARD_PRIVATE_DIR` tanımlanmalıdır. Web kökü içindeki özel dizin reddedilir. Var olan dosyaların üzerine yükleme veya ana dizin `.htaccess` değişikliği yapılmaz.
5. `base_url` için dizin modelinde `https://mktech.net.tr/callguard`, alt alan adında `https://callguard.mktech.net.tr` yaz. PHP e-posta linkleri ve Android API kökü aynı adresi kullanır. Alt alan adı seçilirse cPanel alan adını/SSL'ini ayrıca tanımla.
6. Google girişinde Web OAuth istemci kimliğini config'e ve Android'e ver. `com.callguard.mobile` Android OAuth istemcisinde doğru Play App Signing/test imzasını tanımla. İstemci gizli anahtarı Android'e konmaz. Facebook bu pakette henüz eklenmemiştir.
7. Play Console'da `callguard_pro_lifetime` tüketilmeyen tek seferlik **satın alma** ürününü hazırla. CallGuard'a özel en az yetkili Android Publisher hizmet hesabının JSON dosyasını özel dizine yükle; Android'e koyma. Gerçek satın alma yalnızca sunucuda doğrulanır; test/uydurma token Pro açmaz.
8. Özel dizin/dosyaların izinlerini PHP kullanıcısının okuyabildiği en dar değere ayarla; config ve hizmet hesabı için tipik değer 600'dür. 777 kullanılmaz. PHPMailer's/Google istemcisinin TLS doğrulaması kapatılmaz.
9. `https://mktech.net.tr/callguard/health` 200 yanıtını kontrol et. Bu yalnızca API/DB erişimi göstergesidir; SMTP, OAuth ve Play yetkisini ispatlamaz. Bir test hesabıyla e-posta doğrulama, giriş, yedekleme/geri yükleme, hesap silme; Play test kanalında gerçek test satın alması/geri yükleme ayrıca doğrulanır.
10. Android derleme değişkenlerini canlı testlerden sonra ayarla:
    - `CALLGUARD_API_BASE=https://mktech.net.tr/callguard`
    - `CALLGUARD_GOOGLE_CLIENT_ID=<Web OAuth istemci kimliği>`
    - `CALLGUARD_COMMERCIAL=true` **yalnızca servis ve Play testleri tamamlandıktan sonra**.

Kurulum için sunucu erişimi ve gerçek yapılandırma halen gereklidir. Depodaki örnek değerler veya yayımlanan ZIP canlı hesap/ödeme servisi değildir. Mevcut önizleme APK'ları bunları etkin kabul etmez.

## Uygulama veri akışı

- Android mevcut `/v1/...` yollarını API köküne ekler. Dizinde `/callguard/v1/...`, alt alan adında `/v1/...` oluşur; API iki seçeneği destekler. `/mktech/` yolunu işlemez.
- E-posta parolaları prehash + `password_hash` ile saklanır; oturumların sadece token özeti DB'ye yazılır. Google ID tokenı resmi Google PHP istemcisiyle doğrulanır; nonce tek kullanımlıktır.
- 7 gün deneme sunucu saatiyle hesap başına başlar. Yeniden kurulumda aynı hesap süreyi sıfırlamaz. Farklı hesap açmayı bütünüyle engellediği iddia edilmez.
- Yedekler ve Play tokenları AES-256-GCM ile şifreli saklanır. Kişi listeleri, ayarlar ve en fazla 500 geçmiş kaydı alınır; telefon rehberinin tamamı alınmaz. Android izinleri/oturum/Pro hakları yedekten yüklenmez.
- Bulut yedeği revizyonu denetlenir; eski cihaz başka cihazın yeni yedeğini sessizce ezemez.
- Hesap silme uygulama içinde ve dışarıdan `BASE_URL/account-deletion` adresinde e-posta onayıyla kullanılabilir. Silme sonrası asgari deneme/satın alma doğrulama kaydı korunur; saklama amacı/süresi gerçek gizlilik politikasında açıklanmalı.
- Sürekli işçi süreci yoktur. Otomatik yedeklemeyi Android WorkManager istekleri başlatır. E-posta istek sırasında SMTP ile gönderilir. Günlük süre aşmış oturum/challenge temizliği için cPanel Cron isteğe bağlı eklenebilir; zorunlu değildir. MySQL yedeği ve şifreleme anahtarı yedeği ayrı korunmalıdır; sunucu yedeği bu API'nin yerine geçmez.

## Kontrollerin kapsamı

CI'da PHP sözdizimi/politika ve geçici MySQL üzerinde oturum, hesap izolasyonu, deneme süresinin korunması, şifreli yedek, revizyon çakışması, sahte ödeme reddi ve silme testleri yapılır. Gerçek Turhost erişimi, gerçek SMTP/OAuth/Play satın alması ve dış güvenlik incelemesi bunlarla doğrulanmış sayılmaz. Uzun Android emülatör testleri çalıştırılmaz.
