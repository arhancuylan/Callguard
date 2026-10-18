#!/usr/bin/env sh
# Runs on the Android device under the user's already-authorized USB ADB shell.
set -eu
test "$(id -u)" = 2000 || { echo 'USB ADB shell UID 2000 required'; exit 1; }
apk=$(pm path --user 0 com.callguard.mobile | sed -n 's/^package://p' | head -n 1)
case "$apk" in /data/app/*/base.apk) ;; *) echo 'Install CallGuard in the primary user first'; exit 1;; esac
case "$apk" in *[!a-zA-Z0-9_./=+~-]*) echo 'Unexpected APK path'; exit 1;; esac
test -r "$apk"
export CLASSPATH="$apk"
# Detach the process and all standard streams from adbd; there is no listening TCP/UDP port.
nohup app_process -Xhidden-api-policy:disabled /system/bin com.callguard.CallGuardShell </dev/null >/dev/null 2>&1 &
echo 'Startup requested. Verify USB helper status in CallGuard before unplugging USB.'
