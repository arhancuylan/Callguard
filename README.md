# CallGuard · by AC

Native Android app for WhatsApp and WhatsApp Business voice/video calls. Turkish UI; Android 8+ for notification protection, Android 11+ for optional built-in wireless ADB setup.

## Protection
All incoming calls, whitelist and blacklist rules; voice/video/both; decline or record-only; profiles and schedules. Only WhatsApp’s validated decline PendingIntent is sent. A sent decline request does not independently prove call termination. Notification cleanup targets only the same rejected call generation. Physical voice/video rejection and backup/restore have been reported successful by the user; this does not establish compatibility with every device/WhatsApp release.

Contacts are searchable and support selecting individual numbers and multiple people. Numbers are preferred; exact normalized displayed names are a fallback and are not authenticated identifiers. History entries can be moved to the opposite list. Google sign-in uses Drive appDataFolder backup; email accounts can use the separately deployed PHP/MySQL service. Preview builds have commercial gating disabled.

## Optional Bluetooth call silence, without an extra installed app
Settings → Bluetooth görüşme sessizliği → Cihazlarımı yönet. Add previously paired cars, headsets or other devices supporting the HFP call profile. Each remembered device has independent on/off/restore controls. Turning off silence retains the device configuration; Forget is available only when no recovery is pending and never deletes the Android Bluetooth pairing.

Configurations and prior policies are stored locally by normalized Bluetooth address, excluded from cloud/Drive backups. The previous single-car recovery record is migrated atomically. An uncertain operation retains that device’s original policy, and failure on one device does not erase or overwrite another. Preferences survive app restarts/updates; they are not claims that current hardware state was verified. Actual policy reads are displayed separately. The HFP policy is stored by Android for the device; CallGuard does not run a background loop forcing preferences after external changes.

ADB setup uses Android 11+ Wireless debugging and an in-app notification reply while Android’s pairing-code dialog stays open. Local DNS-SD discovery supplies ports; manual port/code entry remains available. The setup checklist automatically probes an existing CallGuard identity and marks verified steps green. Developer settings are inferred only from positive ADB evidence or explicit user confirmation. The private, user-started pairing service expires after six minutes. No root or additional installed app is required.

The ADB client connects only to 127.0.0.1; RSA identity is encrypted at rest with Android Keystore. Fixed bounded shell commands load the installed APK in a short-lived app_process, validate shell UID, and read/write only paired-device HFP policy. Music/A2DP is unchanged. All calls to that device are affected, including permitted contacts and cellular calls. Media-only speakers may not support HFP, and watch companion-app call notifications may continue. Device compatibility requires real hardware testing.

Restore each active device before uninstalling/clearing app data. When ADB is unavailable, manually enable Calls in Android Bluetooth settings for that device. Disconnecting ADB does not restore profile policies. Normal WhatsApp decline protection remains independent; Whatsmeow is a separate planned experiment.

## Build and verification
Java 17, Gradle 8.11.1, AGP 8.9.1, Android SDK 36. `gradle :adb-transport:testDebugUnitTest :app:testDebugUnitTest :app:lintRelease :app:assembleRelease :app:bundleRelease`. CI runs short rule, recovery, command validation and upstream pairing-crypto tests, lint, native inventory and APK signature continuity checks. Emulator tests are opt-in; real WhatsApp/car testing is performed by the user. The preview signing identity is preserved for updates; production Play signing must be managed separately.

Open-source ADB transport is vendored from libadb-android-bc 3.2.0 under its Apache-2.0 alternative and applicable BSD/MIT notices. See `adb-transport/NOTICE.md` and `adb-transport/LICENSES`; notices are also packaged in APK assets. Bouncy Castle dependencies retain their licenses. No proprietary reference APK is distributed. WhatsApp and Meta are not affiliated with CallGuard.
