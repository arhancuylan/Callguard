#!/usr/bin/env bash
set -euo pipefail
mkdir -p evidence
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb install -r fixture/build/outputs/apk/debug/fixture-debug.apk
adb shell pm grant com.whatsapp android.permission.POST_NOTIFICATIONS
adb shell cmd notification allow_listener com.callguard.mobile/com.callguard.CallNotificationService
adb shell settings put system system_locales tr-TR
adb shell wm size 1080x2400
adb shell wm density 420
adb logcat -c
timeout 300s adb shell am instrument -w -r -e timeout_msec 30000 com.callguard.mobile.test/androidx.test.runner.AndroidJUnitRunner | tee evidence/instrumentation.txt
adb logcat -d -s AndroidRuntime:E > evidence/crashes.txt
adb pull /sdcard/Android/data/com.callguard.mobile/files/screenshots evidence/ || true
if ! grep -q 'OK (' evidence/instrumentation.txt; then exit 1; fi
if grep -qE 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' evidence/instrumentation.txt; then exit 1; fi
# Release installation and launch smoke check after the fully exercised debug variant.
adb shell am force-stop com.callguard.mobile
adb uninstall com.callguard.mobile
adb install app/build/outputs/apk/release/app-release.apk
adb shell am start -W -n com.callguard.mobile/com.callguard.MainActivity | tee evidence/release-launch.txt
adb shell uiautomator dump /sdcard/final-ui.xml
adb pull /sdcard/final-ui.xml evidence/release-ui.xml
if ! grep -q 'Huzurla devam et.' evidence/release-ui.xml; then exit 1; fi
if grep -q 'FATAL EXCEPTION' evidence/crashes.txt; then exit 1; fi
