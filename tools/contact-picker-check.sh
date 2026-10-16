#!/usr/bin/env bash
set -euo pipefail
mkdir -p evidence
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell settings put system system_locales tr-TR
adb shell wm size 1080x2400
adb shell wm density 420
adb shell svc power stayon true
adb shell settings put system screen_off_timeout 1800000
adb shell locksettings set-disabled true
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb shell input keyevent KEYCODE_MENU
adb shell am start -W -n com.callguard.mobile/com.callguard.MainActivity
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb shell input keyevent KEYCODE_MENU
adb shell dumpsys window > evidence/windows-before.txt
timeout -k 5s 90s adb shell am instrument -w -r -e class com.callguard.EndToEndTest#realContactsProviderCanBeReadAndSelected com.callguard.mobile.test/androidx.test.runner.AndroidJUnitRunner | tee evidence/contact-picker-test.txt
adb shell screencap -p /sdcard/contact-screen.png
adb pull /sdcard/contact-screen.png evidence/ || true
adb shell dumpsys window > evidence/windows-after.txt
adb pull /sdcard/Android/data/com.callguard.mobile/files/screenshots evidence/ || true
grep -q 'OK (1 test)' evidence/contact-picker-test.txt
