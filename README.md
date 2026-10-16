# CallGuard · by AC
Native Android app for managing incoming WhatsApp and WhatsApp Business voice/video calls. Turkish UI, Android 8+.

## Features
- Protection toggle, all incoming / whitelist / blacklist rules (default whitelist).
- Voice, video, or both; automatic decline or record only.
- Contacts permission requested only when selecting a contact; searchable contact picker, manual entry, delete and search.
- Local history: decline requests, permitted calls and recorded calls. Tap an entry to add the caller to the opposite list. Missing numbers can be completed in the editor.
- About: **Call Guard by AC**. No Bluetooth, companion device, Accessibility or Telecom permissions/services in the installed final APK.

Enable CallGuard notification access in Android Settings and then enable protection. Notifications do not have to be visually shown if WhatsApp still supplies its internal call notification. If the OS withholds that notification entirely, CallGuard cannot intercept the call. It uses only WhatsApp's own validated Decline PendingIntent, never the Answer action. The record 'Reddetme isteği gönderildi' means the action was sent; it does not independently prove remote termination. A short ring can reach a connected watch before the decline completes. Record-only does not silence calls.

Contacts are matched by international number where available, otherwise by exact normalized displayed name. Names alone are not authenticated; identical or changed names are an inherent limitation. Turkish local numbers are canonicalized to +90, foreign numbers require the country code. Blocklist only rejects known listed callers; whitelist rejects all unmatched callers. 'İzin verildi' means permitted by the rules, not that the user answered. Calls outside the chosen media type are untouched. Adding a number to one list removes it from the opposite list; only the currently selected rule applies. The all-incoming rule ignores both lists.

No Internet permission, analytics, remote backend, message notification processing, or backup. Selected contacts and last 500 history records are local; clearing history deletes those records. Only the selected caller's name/number, media, timestamp and action status are retained. Disabling protection stops new processing.

## Verification
GitHub Actions runs JUnit policy regressions, Android lint, builds, and Android 35 emulator instrumentation. A separately installed `fixture` module produces controlled incoming-call notifications (including the real GROUP_SUMMARY regression), validates a foreign PendingIntent reaching its receiver, and confirms Answer is never invoked. UI tests cover rule persistence, contacts provider selection, manual entry/search/remove, history shortcuts/clear, settings and screenshots. The signed release APK is separately installed and launched. The fixture is CI-only, never shipped or used on a real phone with WhatsApp installed. This validates OS integration, not the real WhatsApp network protocol or physical watch behavior. Real WhatsApp voice decline was confirmed by the user before this release; real video and device/OS-specific compatibility remain to be confirmed on the phone.

## Build
Java 17, Gradle 8.9, Android SDK 35/build tools 35.0.0. `gradle :app:testDebugUnitTest :app:lintRelease :app:assembleRelease`. Distribution APK uses a dedicated distribution test signing key initialized once for the new application ID and restored by CI, without silently regenerating it. Future store distribution needs a securely managed production signing key. No private signing key or proprietary reference app is committed or distributed. Application ID `com.callguard.mobile` keeps the final app separate from old experiments; disable/uninstall old blockers before enabling it.

Independent implementation. WhatsApp and Meta are not affiliated with this project. No third-party app source, assets, or binaries are included.
