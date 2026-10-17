# CallGuard · by AC

Native Android app for WhatsApp and WhatsApp Business voice/video calls. Turkish UI; Android 8+ for notification protection, Android 11+ for optional built-in wireless ADB setup.

## Protection
All incoming calls, whitelist and blacklist rules; voice/video/both; decline or record-only; profiles and schedules. Only WhatsApp’s validated decline PendingIntent is sent. A sent decline request does not independently prove call termination. Notification cleanup targets only the same rejected call generation. Physical voice/video rejection and backup/restore have been reported successful by the user; this does not establish compatibility with every device/WhatsApp release.

Contacts are searchable and support selecting individual numbers and multiple people. Numbers are preferred; exact normalized displayed names are a fallback and are not authenticated identifiers. History entries can be moved to the opposite list. Google sign-in uses Drive appDataFolder backup; email accounts can use the separately deployed PHP/MySQL service. Preview builds have commercial gating disabled.

## Optional car silence, without Shizuku
Settings → Araç sessizliği → Kurulum ve durum. Enable Developer options and Wireless debugging, pair with a code **inside CallGuard**, then enter the connection port from the main Wireless debugging screen. Pairing and connection ports differ. Split screen keeps the Android pairing dialog alive. No additional installed app or root is needed for this Android 11+ flow. Pairing does not silently grant permission: Android’s developer/ADB confirmation remains mandatory.

The ADB client connects only to 127.0.0.1. RSA identity is encrypted with a device-only Android Keystore AES key; identity, pairing codes and recovery state are excluded from Drive/cloud backups. The app exposes no terminal or exported privileged server. Fixed commands load the installed APK in a short-lived shell app_process; the helper validates the shell UID and accepts only probe, paired-device HFP read/write/state operations. Restoring requires an available ADB connection; reboot/network changes may require reconnecting with the current port.

Car silence disables **all Bluetooth calls** for the chosen paired car, including permitted contacts and cellular calls. The phone may still ring; A2DP music is not changed. Previous policy is committed before mutation and read back after writes. An uncertain operation retains the recovery journal. Restore before uninstalling/clearing app data; manual fallback is Android Bluetooth → car → Calls. Disconnecting ADB does not restore Bluetooth policy. This is separate from WhatsApp rejection, not a promise to intercept WhatsApp signaling before Android creates a call.

The local ADB pairing and actual Samsung/Fiat HFP behavior need phone testing; CI cannot establish those hardware results. Whatsmeow is not included in this version and remains a separate planned experiment. Existing notification protection stays independent.

## Build and verification
Java 17, Gradle 8.11.1, AGP 8.9.1, Android SDK 36. `gradle :adb-transport:testDebugUnitTest :app:testDebugUnitTest :app:lintRelease :app:assembleRelease :app:bundleRelease`. CI runs short rule, recovery, command validation and upstream pairing-crypto tests, lint, native inventory and APK signature continuity checks. Emulator tests are opt-in; real WhatsApp/car testing is performed by the user. The preview signing identity is preserved for updates; production Play signing must be managed separately.

Open-source ADB transport is vendored from libadb-android-bc 3.2.0 under its Apache-2.0 alternative and applicable BSD/MIT notices. See `adb-transport/NOTICE.md` and `adb-transport/LICENSES`; notices are also packaged in APK assets. Bouncy Castle dependencies retain their licenses. No proprietary reference APK is distributed. WhatsApp and Meta are not affiliated with CallGuard.
