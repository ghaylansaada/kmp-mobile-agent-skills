# Integration: Crypto

## Koin Module

```kotlin
package {your.package}.di

import {your.package}.crypto.*
import org.koin.dsl.module

val cryptoModule = module {
    single { SecureRandomProvider() }
    single { HashProvider() }
    single { HmacProvider() }
    single { AesGcmProvider() }
    single { KeyDerivation() }
    single {
        CryptoProvider(
            hash = get(),
            hmac = get(),
            aesGcm = get(),
            keyDerivation = get(),
            secureRandom = get(),
        )
    }
}
```

Add `cryptoModule` to your `startKoin { modules(...) }` block.

## Usage: Hashing Sensitive Data

```kotlin
class UserRepository(private val crypto: CryptoProvider) {
    fun hashEmail(email: String): String =
        crypto.sha256Hex(email.trim().lowercase())
}
```

## Usage: Encrypting Local Data

```kotlin
class EncryptedNoteRepository(private val crypto: CryptoProvider) {
    // Format: iv_hex:tag_hex:ciphertext_hex
    fun encryptNote(note: String, key: ByteArray): String {
        val result = crypto.aesGcm.encrypt(key, note.encodeToByteArray())
        return "${result.iv.toHex()}:${result.authTag.toHex()}:${result.ciphertext.toHex()}"
    }

    fun decryptNote(encrypted: String, key: ByteArray): String {
        val (ivHex, tagHex, ctHex) = encrypted.split(":")
        return crypto.aesGcm.decrypt(
            key,
            ctHex.hexToByteArray(),
            ivHex.hexToByteArray(),
            tagHex.hexToByteArray(),
        ).decodeToString()
    }
}
```

## Usage: HMAC Request Signing

See [HmacRequestSigning.kt](../assets/snippets/HmacRequestSigning.kt) for a complete
Ktor client plugin. Install it per platform:

```kotlin
// Android
val httpClient = HttpClient(OkHttp) {
    install(HmacSigningPlugin) {
        apiKey = BuildConfig.API_KEY       // Never hardcode
        secretKey = BuildConfig.HMAC_SECRET // Never hardcode
        cryptoProvider = get<CryptoProvider>()
    }
}

// iOS
val httpClient = HttpClient(Darwin) {
    install(HmacSigningPlugin) {
        apiKey = PlatformConfig.apiKey
        secretKey = PlatformConfig.hmacSecret
        cryptoProvider = get<CryptoProvider>()
    }
}
```

## Usage: Password-Based Encryption

```kotlin
val encrypted = crypto.encryptWithPassword("sensitive data", "user-password")
val decrypted = crypto.decryptWithPassword(encrypted, "user-password")
```

## Upgrading kmp-security SecureDataStoreWrapper

If both kmp-security and kmp-crypto are installed, upgrade SecureDataStoreWrapper
to use AES-GCM instead of XOR by injecting `AesGcmProvider` and
`SecureRandomProvider`, generating a key stored in Keystore/Keychain via
`SecureTokenStorage`, and using `aesGcm.encrypt()`/`decrypt()` for DataStore values.

## Dependencies

| Direction | Skill | Relation |
|-----------|-------|----------|
| Upstream | kmp-architecture | expect/actual for all crypto providers |
| Upstream | kmp-dependency-injection | Koin module wiring |
| Downstream | kmp-security | AES-GCM upgrade for SecureDataStoreWrapper |
| Downstream | kmp-networking | HMAC request signing via Ktor plugin |
