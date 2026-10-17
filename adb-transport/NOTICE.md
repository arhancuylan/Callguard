CallGuard includes libadb-android-bc 3.2.0 sources from
https://github.com/osservatorionessuno/libadb-android-bc/tree/3.2.0
Pinned upstream tag: 3.2.0 (source file hashes retained in Git history)

Copyright notices and SPDX identifiers are retained in each source. CallGuard selects the Apache-2.0 alternative for dual-licensed files; BSD-3-Clause and MIT notices remain applicable to their respective portions. Full upstream licenses are in LICENSES/. Bouncy Castle dependencies retain their own licenses.

Local modifications: Android module targets SDK 36/min 26; TCP connect and pairing read deadlines added; peer identity logging removed. No JNI/native binaries are included. CallGuard uses only local pairing/TLS shell streams; discovery and general shell features of the library are not exposed in the app.
