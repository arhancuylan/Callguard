#!/usr/bin/env bash
set -euo pipefail
# The selected serial must be an emulator; never fall through to a USB phone.
serial="${ANDROID_SERIAL:-emulator-5554}"
case "$serial" in emulator-*) ;; *) echo 'Refusing a physical device'; exit 1;; esac
export ANDROID_SERIAL="$serial"
hardware=$(adb shell getprop ro.hardware | tr -d '\r')
case "$hardware" in ranchu|goldfish) ;; *) echo 'Emulator hardware required'; exit 1;; esac
mkdir -p evidence/usb-helper
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
run_test() {
  local method="$1"
  adb shell am instrument -w -r -e class "com.callguard.UsbShellTransportTest#$method" com.callguard.mobile.test/androidx.test.runner.AndroidJUnitRunner | tee "evidence/usb-helper/$method.txt"
  grep -q 'OK (1 test)' "evidence/usb-helper/$method.txt"
}
run_test wifiOffDetachedShellAndIndependentDeviceRollback
adb shell am force-stop com.callguard.mobile
adb shell am start -W -n com.callguard.mobile/com.callguard.MainActivity
run_test appRestartReconnectsAndHelperDeathPreservesRecovery
run_test prepareReboot
adb reboot
adb wait-for-device
for _ in $(seq 1 120); do test "$(adb shell getprop sys.boot_completed | tr -d '\r')" = 1 && break; sleep 1; done
test "$(adb shell getprop sys.boot_completed | tr -d '\r')" = 1
run_test rebootDoesNotGrantOrStartShell
run_test productionStartupWithoutWifiRejectsAnUnbondedTarget
adb logcat -d -s AndroidRuntime:E > evidence/usb-helper/crashes.txt
printf '%s\n' 'PHYSICAL_USB_UNPLUG_AND_REAL_HFP_HARDWARE_PENDING; emulator uses synthetic devices, not audible HFP.' | tee evidence/usb-helper/hardware-pending.txt
