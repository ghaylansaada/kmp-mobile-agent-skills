# Security -- Troubleshooting

## EncryptedSharedPreferences crashes on first access

**Symptom:** `KeyStoreException: Failed to load key` or `InvalidKeyException`

**Cause:** Android Keystore master key invalidated (user changed lock screen, device
restored from backup).

**Fix:** Wrap init in try/catch. Delete corrupted prefs file and recreate. User must
re-authenticate.

## iOS Keychain returns errSecItemNotFound (-25300)

**Symptom:** `getToken()` always returns null after `saveToken()` succeeds.

**Cause:** Keychain access group mismatch, or Keychain cleared by iOS (app reinstall
without iCloud Keychain sync).

**Fix:**
- Verify `kSecAttrService` matches between save and retrieve
- Check entitlements include correct Keychain access group
- Simulator: reset via Keychain Access or Simulator > Device > Erase All Content

## SQLCipher "file is not a database" error

**Symptom:** `SQLiteException: file is not a database`

**Cause:** Opening unencrypted DB with SQLCipher, or wrong passphrase.

**Fix:**
- If migrating from unencrypted, run `DatabaseEncryptionMigration.migrateToEncrypted()` first
- If passphrase lost (Keystore wiped), delete DB and let Room recreate

## Certificate pinning blocks all requests

**Symptom:** `SSLPeerUnverifiedException` (Android) or connection errors (iOS).

**Cause:** Pin hashes don't match server's certificate chain (cert rotation).

**Fix:**
- Regenerate pins with the openssl command from CertificatePinningConfig
- Always include a backup pin (different CA or next certificate)
- Test with `CertificatePinningConfig.pins = emptyList()` to confirm pinning is the issue

## ProGuard strips Ktor/Room classes in release build

**Symptom:** `ClassNotFoundException` or `NoSuchMethodError` in release only.

**Cause:** Missing keep rules for reflection-heavy libraries.

**Fix:**
- Apply all rules from `assets/snippets/proguard-security-rules.pro`
- Verify with a release build (see kmp-build skill)
- Enable `printusage.txt` in ProGuard config to see stripped classes

## Root detection false positive on emulator

**Symptom:** `isDeviceCompromised()` returns true on Android emulators.

**Cause:** Emulators have `ro.debuggable=1` and test-keys.

**Fix:** In debug builds, skip or log integrity warnings instead of blocking.
