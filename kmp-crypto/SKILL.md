---
name: kmp-crypto
description: >
  Implement platform-native cryptographic operations in KMP — SHA-256 hashing,
  HMAC-SHA256 signing, AES-256-GCM encryption, PBKDF2 key derivation, and
  secure random generation using only system frameworks. Activate when hashing
  sensitive data, signing API requests, encrypting fields before storage,
  deriving keys from passwords, or generating secure tokens.
compatibility: >
  KMP with platform cryptography APIs. Android KeyStore/javax.crypto, iOS Security/CommonCrypto.
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---

# KMP Crypto

## When to use

- Hashing sensitive data (emails, identifiers) before storage or transmission
- HMAC-signing API requests for server-side verification
- Encrypting sensitive fields before writing to Room or DataStore
- Deriving encryption keys from user-provided passwords
- Generating cryptographically secure tokens, nonces, or session IDs
- Hashing data for integrity verification (SHA-256)
- Encrypting/decrypting local data with AES-GCM
- Verifying data authenticity with HMAC before processing

## Depends on

- kmp-architecture -- expect/actual pattern for all crypto providers
- kmp-dependency-injection -- Koin module wiring

## Workflow

1. **Verify source sets and link iOS frameworks** --> [setup.md](references/setup.md)
2. **Create shared types, expect declarations, and CryptoProvider facade** --> [shared-types.md](references/shared-types.md)
3. **Implement Android actuals** (MessageDigest, javax.crypto, SecureRandom) --> [android-implementation.md](references/android-implementation.md)
4. **Implement iOS actuals** (CommonCrypto, CryptoKit, Security) --> [ios-implementation.md](references/ios-implementation.md)
5. **Wire DI and integrate with app layers** --> [integration.md](references/integration.md)

## Gotchas

1. **AES-GCM nonce reuse breaks everything.** Reusing a 12-byte IV with the same key leaks XOR of plaintexts and allows forgery. Always generate a fresh IV per encryption call via SecureRandomProvider.
2. **iOS CommonCrypto has NO native GCM mode.** AES-GCM on iOS requires CryptoKit (iOS 13+) via Swift interop. See ios-implementation.md for the required Swift bridge.
3. **`kotlin.random.Random.Default` is NOT cryptographically secure.** It uses a deterministic PRNG. Always use SecureRandomProvider for keys, nonces, tokens.
4. **Android Keystore keys are device-bound.** Data encrypted with Keystore-derived keys cannot be decrypted after factory reset or on another device. PBKDF2-derived keys from user passwords survive device changes but Keystore-backed keys do not.
5. **PBKDF2 with 600k iterations takes ~200ms on modern devices.** Can take seconds on budget hardware. Always run on `Dispatchers.Default` and show a progress indicator.
6. **All crypto ops are CPU-bound.** Running on the main thread causes visible UI jank. Wrap in `withContext(Dispatchers.Default)`.
7. **`toHex()` must use consistent case.** When comparing hashes from external sources, normalize with `.lowercase()` before comparing. A mixed-case comparison silently returns false.
8. **Derived keys must be cleared from memory after use.** ByteArrays holding key material should be zeroed with `fill(0)` when no longer needed. On Android, call `PBEKeySpec.clearPassword()` after key derivation.
9. **CryptoKit `AES.GCM.SealedBox(combined:)` expects nonce+ciphertext+tag.** The combined format is exactly 12 bytes nonce, then ciphertext, then 16 bytes tag. Reordering causes `CryptoKitError.authenticationFailure` with no useful message.

## Assets

| Path | Load when... |
|------|-------------|
| [HmacRequestSigning.kt](assets/snippets/HmacRequestSigning.kt) | Adding HMAC-SHA256 request signing to a Ktor client |

## Validation

### A. Kotlin and KMP correctness
- [ ] `expect` declarations in commonMain have matching `actual` in both androidMain and iosMain
- [ ] AES-GCM used (not AES-CBC) for authenticated encryption
- [ ] IV generated from SecureRandomProvider (not `kotlin.random.Random`)
- [ ] IV is 12 bytes and auth tag is 16 bytes for GCM
- [ ] `sealed interface` or proper result types for crypto operations
- [ ] SHA-256 of `"hello"` matches NIST vector `2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824`
- [ ] HMAC-SHA256 of `"what do ya want for nothing?"` with key `"Jefe"` matches RFC 4231 vector `5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843`
- [ ] AES-GCM encrypt then decrypt round-trips on both platforms
- [ ] Different encryption calls produce different ciphertext (IV uniqueness)
- [ ] Wrong-key decryption fails with authentication error, not silent corruption
- [ ] Invalid key sizes rejected with `IllegalArgumentException`

### B. Security
- [ ] No hardcoded keys, IVs, or secrets in any reference file
- [ ] PBKDF2 iteration count >= 600,000 (OWASP 2023 recommendation)
- [ ] Salt is at least 16 bytes and generated from SecureRandomProvider
- [ ] Key material cleared after use (`ByteArray.fill(0)`, `PBEKeySpec.clearPassword()`)

### C. Performance
- [ ] PBKDF2 called on `Dispatchers.Default`, not main thread
- [ ] All crypto operations wrapped in `withContext(Dispatchers.Default)` in production code

### D. Integration
- [ ] Koin module registers CryptoProvider with named constructor parameters
- [ ] HmacRequestSigning asset compiles with current Ktor plugin API
- [ ] Downstream skills (kmp-security, kmp-networking) reference consistent type names
