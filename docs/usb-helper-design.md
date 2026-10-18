# CallGuard USB HFP helper — review candidate, not hardware accepted

The normal Android app cannot acquire shell privileges itself. The owner first enables the USB helper in CallGuard, authorizes their computer in Android's USB debugging prompt, and runs the supplied script using USB ADB. No root, Shizuku installation, network control socket, `adb tcpip`, persistent permission grant, or security-setting modification is part of this design.

## Start and stop

Use a review build only in an isolated test environment until the acceptance matrix below is complete. The script deliberately uses no automatic wireless debugging setup.

1. Open **ADB kurulum ve durum → USB ile Wi-Fi'siz kullan**, read the disclosure, and allow the helper.
2. Connect your own USB host and manually authorize it in Android. The app cannot perform this step.
3. On the host, with `start-usb-helper.sh` downloaded from this exact reviewed revision:

   ```text
   adb -d push start-usb-helper.sh /data/local/tmp/callguard-start-usb.sh
   adb -d shell sh /data/local/tmp/callguard-start-usb.sh
   ```

4. Wait for **USB yardımcısı doğrulandı**. A launch-request message from the host is not proof of success. Wi-Fi and the USB cable are not used by the Binder connection after successful startup.
5. To stop, preferably restore each device first from **Cihazlarım**, then choose **USB yardımcısının yetkisini iptal et**. Revocation persists before attempting to stop the process. Late Binder delivery is rejected. Stopping deliberately does not modify Bluetooth policies or erase recovery records.

Reboot, OEM process termination, APK replacement, permission revocation, or loss of the helper requires fresh USB startup. This is not a permanent grant. App process restart can reconnect to the surviving helper when Android reports the pinned CallGuard UID becoming active; the helper delivers its Binder only through CallGuard's shell-only provider. It does not repeatedly restart the app in the background. Android force-stop is not a guarantee that a separately started shell process has stopped: use explicit revoke or reboot.

The current USB HFP implementation requires Android 11/API 30 or newer and supports the primary Android user only. Earlier systems lack the connection-policy API used here (and Android 8.0 also lacks the required shell UID-observer permission); they are explicitly rejected, rather than presented as supported. Work profiles, secondary users, shared app UIDs and multiple APK signers fail closed. Android 35 is the runtime tested by this candidate; other eligible Android/OEM versions still need acceptance. An OEM may deny shell access to Bluetooth hidden APIs or terminate detached processes; no security-setting workaround is automatically applied.

## Trust boundaries

- `CallGuardShell` accepts shell UID 2000 only and loads its code from the currently installed CallGuard base APK. The ordinary app never launches the elevated process.
- The exported rendezvous provider allows only caller UID 2000 attributed to `com.android.shell`, a single `attach-v1` method, a Binder object and an enabled local consent flag. Query/insert/update/delete/type/file access is unavailable; no URI grants exist.
- Every HFP Binder request checks the exact pinned application UID, exclusive package ownership of that UID, current APK path and SHA-256 signer digest against the values captured at bootstrap. Another app with the same signing certificate still fails the UID/package check.
- Only probe, HFP policy read, HFP connection state, policy write (0 or 100) and stop exist. Addresses must be Bluetooth MAC addresses and the existing backend verifies that the device is bonded. Unknown opcodes, trailing Parcel data and asynchronous transactions are rejected. There is no arbitrary shell command, file path, forwarded Binder transaction, exported command executor, TCP/UDP listener or remote control endpoint.
- Caller identity is checked before clearing Binder identity for the actual Bluetooth call. The Bluetooth API call executes as shell. A 20-second watchdog ends a hung helper; callers retain recovery records on failures or uncertain replies.
- App-side Binder death handling removes readiness. A changed transport identity invalidates cached verification. A remembered device, desired preference, durable recovery record and live policy verification are separate facts.
- Shell reattachment uses the framework UID observer's native `onUidActive` transaction for the pinned UID only; callbacks from any UID except system_server (1000) are rejected. The mapping follows [AOSP IUidObserver's native ABI](https://android.googlesource.com/platform/frameworks/base/+/android-15.0.0_r1/core/java/android/app/IUidObserver.aidl), without reflecting blocked private transaction fields. Other Android/OEM implementations remain an explicit compatibility test requirement.
- An operation selects one backend once. It never retries an uncertain write through the other backend. Wireless local ADB remains an optional fallback for a later explicit operation.

## Rollback and persistence

The existing per-device `BluetoothSilenceStore` and `CarSilencePolicy` remain authoritative. Original policy is durably recorded before writing policy 0. A failed RPC, helper death or failed readback retains that record. Restore writes the original policy, verifies it, then clears only that device's recovery marker. Other devices and remembered preferences are preserved. No new helper identity or consent is included in cloud/Drive backup.

The Bluetooth policy is stored by Android's Bluetooth service, not maintained by the lifetime of the ADB socket. Therefore loss of Wi-Fi or the helper is not an undo operation. Actual policy persistence across Bluetooth restart/reboot is OEM dependent and must be measured on the target hardware. Manual recovery remains Android Bluetooth → device → Calls; companion-app watch notifications are separate from HFP.

## Acceptance matrix

| Case | Isolated test method | Hardware acceptance still required |
| --- | --- | --- |
| Wi-Fi off | Emulator disables Wi-Fi, probes actual shell Binder | Yes, target OEM |
| USB cable removed | Detached helper; app talks only Binder, no TCP/ADB stream | Yes, physically unplug cable |
| Helper alive/dead | Probe, explicit stop, death recipient and denied late attachment | OEM termination behavior |
| App restart | Kill app process, reopen, pinned UID activation reattaches surviving helper | Yes |
| Reboot | Reboot emulator, ensure no helper and no automatic privileged startup | Yes |
| Bluetooth off/on, device reconnect | Synthetic backend/domain state transitions only on emulator | Yes, real bonded HFP device |
| Silence/restore | Journaled synthetic HFP backend through actual Binder | Yes, audible HFP/normal-call behavior |
| Prior preference and multiple devices | Independent journals, migration and restart fixtures | Yes, at least two real devices |

Passing synthetic tests is not physical Wi-Fi-free HFP acceptance. This candidate must not be released until the target-phone matrix is recorded with the exact commit, phone Android/OEM version, Bluetooth device models and observed results. No real user-phone command or setting change is authorized by automated CI.

The PR workflow only builds/tests and uploads evidence; it never publishes APKs or creates credentials. It restores the existing retained signing key for disposable emulator APKs and fails closed if that key is unavailable. The release workflow additionally requires a manual dispatch with `publish_after_hardware_acceptance=true` and the non-secret repository variable `CALLGUARD_WIFI_FREE_ACCEPTED_SHA` matching the exact run SHA. This task does not set that variable, approve a release or merge the PR.

## Sources and licensing

This helper's source is original CallGuard code using Android Binder/ContentProvider APIs and the existing CallGuard HFP backend. No Shizuku implementation or Shizuku-API dependency was copied or bundled. The architecture was compared with [Shizuku's startup documentation](https://shizuku.rikka.app/guide/setup/), [Android ADB](https://developer.android.com/tools/adb), [Binder caller identity](https://developer.android.com/reference/android/os/Binder#getCallingUid()) and [AOSP BluetoothHeadset](https://android.googlesource.com/platform/frameworks/base/+/android-12.0.0_r1/core/java/android/bluetooth/BluetoothHeadset.java).

License inspection on 2026-10-18: Shizuku's [server repository license](https://github.com/RikkaApps/Shizuku/blob/master/LICENSE) is Apache-2.0; its [API repository license](https://github.com/RikkaApps/Shizuku-API/blob/master/LICENSE) is MIT. If future changes copy their implementation, retain applicable copyright/license notices, Apache notices and modification attribution as appropriate; do not treat these references as a license to remove notices. Existing libadb licensing and notices are unchanged in `adb-transport/NOTICE.md` and `LICENSES/`.
