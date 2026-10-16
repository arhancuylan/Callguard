# CallGuard

Android 8.0+ için Java ile hazırlanmış yerel Android uygulama iskeleti. Arayüz Türkçedir, harici UI bağımlılığı ve ağ erişimi yoktur.

## GitHub üzerinden APK indir

`main` dalına gönderilen her değişiklik GitHub Actions üzerinde JDK 17 / Gradle 8.9 ile derlenir ve lint kontrolünden geçer. Başarılı derleme, Releases bölümüne `CallGuard-debug.apk` ekler. Doğrudan bağlantı: https://github.com/arhancuylan/Callguard/releases/latest/download/CallGuard-debug.apk

Bu debug imzalı test sürümüdür. Actions çalışması başarısızsa yeni APK yayımlanmaz. İlk derleme tamamlanmadan bağlantı çalışmaz.

## Aç ve derle

1. Android Studio'da bu klasörü proje olarak aç. JDK 17, Android SDK Platform 35 ve Gradle 8.9 kullan.
2. Gradle kurulumu olan bir bilgisayarda proje kökünde `gradle wrapper --gradle-version 8.9` çalıştır. Wrapper bu ortamda ağ erişimi çalışmadığı için indirilemedi; depoya eksik/temsili wrapper konmadı.
3. `./gradlew assembleDebug` ile APK üret. Çıktı: `app/build/outputs/apk/debug/app-debug.apk`.
4. APK'yı cihaza kur. Koruma ekranındaki açıklamayı okuyup erişilebilirlik ayarlarından CallGuard korumasını aç. Android bazı dışarıdan kurulan uygulamalarda önce uygulama bilgi ekranından “Kısıtlanmış ayarlara izin ver” seçeneğini gerektirebilir.
5. İzin listesinde rehber erişimi verip kişileri ekle. Servis ayrıcalığını Android'de kullanıcı açar; uygulama bunu otomatik veremez.

## Davranış

- Varsayılan: koruma açık, otomatik reddet, boş izin listesi. Yalnızca izin listesiyle eşleşen arayanlara dokunulmaz; tanınamayan arayanlar engellenir.
- İzinler, mod ve ana anahtar uygulama yeniden açıldığında korunur. SQLite geçmişi en son 500 işlemi saklar; geçmiş silinebilir.
- Otomatik reddet: yalnızca gelen sesli arama etiketi, Yanıtla ve Reddet kontrolleri aynı WhatsApp penceresinde bulunduğunda reddet düğmesine erişilebilirlik tıklaması gönderilir. Başarılı `ACTION_CLICK`, çağrının kapandığının doğrulaması değildir; geçmiş “Reddetme komutu gönderildi” yazar.
- Sessizce yok say: çağrıya dokunmadan kayıt tutar. **WhatsApp zil sesi, titreşim veya bildirimini susturmaz.** Bunun için WhatsApp/Android bildirim ayarları kullanılmalıdır. Bu iskelet gerçek sessize alma uygulamaz.
- Gelen görüntülü çağrılar, devam eden ve giden çağrılar hedeflenmez. Servis koordinatlara basmaz.
- READ_CONTACTS yalnızca rehberden ekleme istendiğinde istenir. İzin reddi, boş rehber ve okuma hataları arayüzde ele alınır. İnternet, çağrı günlüğü, telefon araması veya mikrofon izni istenmez.

## Uyumluluk sınırı

WhatsApp üçüncü taraf çağrı reddetme API'si sunmaz. Bu iskelet ekrandaki erişilebilirlik ağacına bağlıdır ve gerçek WhatsApp cihazında doğrulanmamıştır. Türkçe/İngilizce başlangıç etiketleri `Policy.java`, arayan kaynak kimlikleri `CallGuardService.java` içindedir. WhatsApp sürümüne göre bunlar güncellenmelidir. Bildirimden gelen arama, kilit ekranı ve farklı üretici cihazlarında ekran ağacı görünmeyebilir; böyle durumlarda işlem yapılmaz. Bildirim dinleyicisi eklenmemiştir.

Arayan yalnızca ayrılmış arama ekranı kaynak kimliklerinden okunur. Kaynak kimliği bulunamazsa bilinmeyen sayılır; bu durumda izinli kişi bile engellenebilir. Kimlik eşleştirme tam ad veya numaraya dayanır; aynı adlı kişiler ayrıştırılamaz, WhatsApp görünen adı değişirse eşleşme başarısız olabilir. Bu, doğrulanmış telefon kimliği sağlayan bir güvenlik mekanizması değildir. Üretime geçmeden desteklenen WhatsApp sürümlerinde arayan kimliğini ve izin listesi davranışını doğrula.

İşlem tekrarı aynı görünen arayan için 15 saniyelik olay aralığıyla bastırılır; hızlı ardışık aynı kişi çağrıları ayrıca test edilmelidir. Reddet düğmesi erişilemiyorsa en çok üç erişilebilirlik olayında deneme yapılır; sonuca göre hata kaydı yazılır.

## Yapı

- `MainActivity.java`: koruma, aranabilir rehber seçici, izin listesi, geçmiş, ayarlar ve erişilebilirlik açıklaması.
- `CallGuardService.java`: yalnızca WhatsApp / WhatsApp Business pencere olayları; temkinli sesli çağrı algılama ve reddetme.
- `Policy.java`: Türkçe/İngilizce arama etiketleri ve eşleştirme normalizasyonu.
- `Store.java`: yerel izin listesi ve sınırlı geçmiş. Yedekleme kapalıdır.
- `app/src/main/res/xml/accessibility_service.xml`: erişilebilirlik kapsamı.

## Gerçek cihaz kabul testleri

İki telefonla, WhatsApp ve gerekiyorsa WhatsApp Business üzerinde:

1. Boş izin listesinde gelen sesli çağrı: Reddet komutu ve tek geçmiş kaydı.
2. İzinli kişi: çağrı açık kalır, engelleme geçmişine yazılmaz. Aynı adı taşıyan kişiler ve farklı numara biçimlerini ayrıca dene.
3. İzinli olmayan kişi, gizli/bilinmeyen arayan: reddetme denemesi.
4. Yok say modu: çağrı kapanmaz, kayıt oluşur; sesin otomatik susturulmadığını doğrula.
5. Giden, devam eden ve görüntülü arama: reddetme olmamalı.
6. Koruma kapalı veya servis kapalı: işlem olmamalı; ana ekran durumu güncellenmeli.
7. Rehber izni reddedilmiş/kalıcı reddedilmiş, boş rehber, izin sonradan kaldırılmış: çökmemeli.
8. Ekran kilitli, uygulama arka planda, Türkçe/İngilizce, farklı WhatsApp sürümleri ve art arda çağrılar: destek durumunu kaydet.
9. Uygulama yeniden açıldığında izinler/mod/geçmiş korunmalı, geçmiş silme çalışmalı.

Mağaza dağıtımında erişilebilirlik API kullanım beyanı ve ilgili mağaza politikaları ayrıca değerlendirilmelidir.

## Bu ortamda doğrulama

XML dosyaları ve manifest/Java sınıf bağlantıları statik olarak kontrol edildi. Ortamda Android SDK, Gradle ve Java derleyicisi yok; ağ proxy bağlantısı da başarısız. APK derlemesi ve gerçek cihaz testi yapılmadı.
