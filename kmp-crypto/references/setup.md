# Setup: Crypto

> **Note:** Version numbers are intentionally omitted. Always use the latest stable release from the official documentation.

## No Additional Gradle Dependencies

All cryptographic operations use platform system frameworks:

- **Android:** `java.security.MessageDigest`, `javax.crypto.Mac`, `javax.crypto.Cipher`, `javax.crypto.SecretKeyFactory`, `java.security.SecureRandom`
- **iOS:** `CommonCrypto` (CC_SHA256, CCHmac, CCKeyDerivationPBKDF), `CryptoKit` (AES.GCM), `Security` (SecRandomCopyBytes)

## iOS Framework Linking

CommonCrypto and Security are available by default. If you encounter linker errors:

```kotlin
iosTarget.binaries.framework {
    linkerOpts("-framework", "Security")
}
```

For CryptoKit (AES-GCM), ensure iOS deployment target is 13.0+.

## Directory Structure

```
composeApp/src/
  commonMain/kotlin/{your/package}/crypto/
    CryptoProvider.kt          -- Unified facade
    CryptoResult.kt            -- EncryptionResult, hex utilities
    HashProvider.kt / HmacProvider.kt / AesGcmProvider.kt
    KeyDerivation.kt / SecureRandomProvider.kt
  androidMain/kotlin/{your/package}/crypto/
    HashProvider.android.kt / HmacProvider.android.kt / AesGcmProvider.android.kt
    KeyDerivation.android.kt / SecureRandomProvider.android.kt
  iosMain/kotlin/{your/package}/crypto/
    HashProvider.ios.kt / HmacProvider.ios.kt / AesGcmProvider.ios.kt
    KeyDerivation.ios.kt / SecureRandomProvider.ios.kt
iosApp/iosApp/
  AesGcmHelper.swift           -- CryptoKit bridge for AES-GCM
```

## Encoding Conventions

All byte-to-string conversions use lowercase hex by default:

```
Input:  [0xDE, 0xAD, 0xBE, 0xEF]
Hex:    "deadbeef"
```

Base64 via `kotlin.io.encoding.Base64` is available for compact/URL-safe encoding.
