---
name: kmp-security
description: >
  Implement application security for Kotlin Multiplatform -- secure token storage
  via platform keystores, TLS certificate pinning, encrypted databases, root and
  jailbreak detection, and ProGuard/R8 hardening. Use this skill when storing
  sensitive credentials, pinning certificates to API endpoints, encrypting local
  databases, detecting compromised devices, or hardening release builds -- even if
  the user just says "make it secure" or "protect the tokens."
compatibility: >
  KMP with Compose Multiplatform. Android: AndroidKeyStore, EncryptedSharedPreferences,
  OkHttp CertificatePinner, SQLCipher. iOS: Keychain Services, URLSession delegate
  pinning. Requires Ktor with OkHttp/Darwin engines.
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---

# KMP Security

## When to use

- Storing auth tokens, refresh tokens, or API keys securely at rest
- Pinning TLS certificates for API communication
- Encrypting a Room database with SQLCipher for sensitive data
- Detecting rooted Android or jailbroken iOS devices
- Configuring ProGuard/R8 rules for a KMP release build
- Obfuscating sensitive API endpoint strings in the binary
- Wrapping DataStore with platform-native encryption
- Migrating plain-text tokens or unencrypted databases to secure storage

## Depends on

- **kmp-architecture** -- Gradle and source set structure
- **kmp-architecture** -- Platform-specific implementations
- **kmp-dependency-injection** -- Koin module wiring

## Workflow

1. Add dependencies and configure ProGuard --> [references/setup.md](references/setup.md)
2. Implement secure token storage (expect/actual) --> [references/token-storage.md](references/token-storage.md)
3. Add device integrity checking --> [references/device-integrity.md](references/device-integrity.md)
4. Configure certificate pinning --> [references/cert-pinning.md](references/cert-pinning.md)
5. Add data encryption (endpoints, DataStore, SQLCipher) --> [references/data-encryption.md](references/data-encryption.md)
6. Wire into DI, Ktor client, and app startup --> [references/integration.md](references/integration.md)
7. Migrate existing plain storage if needed --> [references/migrations.md](references/migrations.md)
8. Troubleshoot runtime issues --> [references/troubleshooting.md](references/troubleshooting.md)

## Gotchas

1. **Certificate pin hashes MUST use the `sha256/` prefix when passed to OkHttp CertificatePinner.** Omitting the prefix causes a silent no-op -- requests succeed without any pinning, giving a false sense of security.

2. **iOS App Transport Security (ATS) enforces baseline TLS but does NOT do certificate pinning.** Custom pinning via URLSession delegate is required. ATS and custom pinning are complementary, not alternatives.

3. **The iOS URLSession pinning delegate must actually compare SPKI hashes from the server certificate chain against the expected pins.** Accepting the credential after `SecTrustEvaluateWithError` alone only validates the system trust store, not your pins.

4. **Android `network_security_config.xml` pinning does NOT apply to WebView.** If your app loads URLs in WebView, you must implement `WebViewClient.onReceivedSslError()` separately.

5. **Always include at least one backup pin from a different CA.** When the primary certificate rotates, the app bricks all network calls if no backup pin matches. Ship pins for both the current AND next certificate.

6. **`EncryptedSharedPreferences` throws `KeyStoreException` if the device lock screen changes or the device is restored from backup.** This corrupts the master key. Wrap access in try/catch with a fallback that clears and recreates the preference file.

7. **iOS Keychain items with `kSecAttrAccessibleWhenUnlockedThisDeviceOnly` are NOT included in device backups.** They are lost on device migration/restore. This is intentional for security, but users lose their tokens.

8. **SQLCipher adds 3-4 MB to APK size.** If the passphrase stored in Keystore is lost (factory reset, Keystore corruption), the encrypted database becomes permanently unreadable.

9. **ProGuard rules must be tested with actual release builds.** Debug builds do not run R8, so reflection-based crashes only surface in release -- typically during app review or in production. See **kmp-release** skill for release build configuration.

10. **iOS `fork()` is killed by the OS on modern iOS versions (post-iOS 9).** Using it for jailbreak detection will crash the app on non-jailbroken devices unless guarded. Prefer file-path and URL-scheme checks instead.

## Assets

| Path | Load when... |
|------|-------------|
| [assets/templates/SecurityModule.kt](assets/templates/SecurityModule.kt) | Wiring security components into Koin DI |
| [assets/snippets/proguard-security-rules.pro](assets/snippets/proguard-security-rules.pro) | Adding security-specific R8 rules (SQLCipher, obfuscation) |

## Validation

### A. Security correctness
- [ ] No hardcoded secrets, tokens, or API keys in any reference or asset file
- [ ] Certificate pinning rejects connections when pins are wrong (Android)
- [ ] Certificate pinning rejects connections when pins are wrong (iOS)
- [ ] Certificate pinning allows connections with correct pins on both platforms
- [ ] Backup pin from a different CA allows connections after primary cert rotation
- [ ] iOS URLSession delegate compares SPKI hashes -- does not just accept after trust evaluation
- [ ] Android uses AndroidKeyStore-backed `MasterKey` for EncryptedSharedPreferences
- [ ] iOS uses Keychain with `kSecAttrAccessibleWhenUnlockedThisDeviceOnly`
- [ ] `EncryptedSharedPreferences` fallback works after simulated Keystore corruption
- [ ] Root/jailbreak detection returns correct results on clean device and emulator
- [ ] ProGuard/R8 rules keep all security classes from being stripped or obfuscated
- [ ] Token refresh/rotation: old tokens cleared from secure storage after rotation
- [ ] Database passphrase stored in platform keystore, not hardcoded
- [ ] XOR encryption key generated per-install and stored in platform keystore

### B. Functional
- [ ] `SecureTokenStorage` round-trips tokens on both Android and iOS
- [ ] `DeviceIntegrityChecker` returns empty reasons on clean device
- [ ] `EndpointObfuscator` encode/decode round-trips all test strings including empty and special chars
- [ ] `SecureDataStoreWrapper` encrypts values (raw prefs file does not contain plaintext)
- [ ] SQLCipher DB opens with correct passphrase, rejects wrong passphrase
- [ ] Token migration moves tokens from plain DataStore to secure storage
- [ ] Token migration is idempotent -- second run is a no-op

### C. Integration
- [ ] Koin modules resolve all security dependencies without crashes
- [ ] Ktor HttpClient with certificate pinning makes successful API calls
- [ ] Device integrity check runs at app startup without blocking the main thread
- [ ] Obfuscated endpoints decode to correct URLs at runtime
