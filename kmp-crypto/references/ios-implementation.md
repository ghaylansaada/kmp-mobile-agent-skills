# iOS Actual Implementations

All iOS implementations require `@OptIn(ExperimentalForeignApi::class)`.

## SecureRandomProvider
```kotlin
package {your.package}.crypto

import kotlinx.cinterop.*
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault
import platform.Security.errSecSuccess

@OptIn(ExperimentalForeignApi::class)
actual class SecureRandomProvider actual constructor() {
    actual fun nextBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        bytes.usePinned { pinned ->
            val status = SecRandomCopyBytes(kSecRandomDefault, size.convert(), pinned.addressOf(0))
            require(status == errSecSuccess) { "SecRandomCopyBytes failed: $status" }
        }
        return bytes
    }

    actual fun nextInt(bound: Int): Int {
        require(bound > 0) { "Bound must be positive, got $bound" }
        val bytes = nextBytes(4)
        val raw = ((bytes[0].toInt() and 0xFF) shl 24) or
            ((bytes[1].toInt() and 0xFF) shl 16) or
            ((bytes[2].toInt() and 0xFF) shl 8) or (bytes[3].toInt() and 0xFF)
        return (raw and Int.MAX_VALUE) % bound
    }
}
```

## HashProvider (SHA-256 via CommonCrypto)
```kotlin
package {your.package}.crypto

import kotlinx.cinterop.*
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH

@OptIn(ExperimentalForeignApi::class)
actual class HashProvider actual constructor() {
    actual fun sha256(input: ByteArray): ByteArray {
        val digest = ByteArray(CC_SHA256_DIGEST_LENGTH)
        input.usePinned { inp ->
            digest.usePinned { dig ->
                CC_SHA256(inp.addressOf(0), input.size.convert(), dig.addressOf(0).reinterpret())
            }
        }
        return digest
    }
    actual fun sha256(input: String): ByteArray = sha256(input.encodeToByteArray())
}
```

## HmacProvider (HMAC-SHA256 via CommonCrypto)
```kotlin
package {your.package}.crypto

import kotlinx.cinterop.*
import platform.CoreCrypto.CCHmac
import platform.CoreCrypto.kCCHmacAlgSHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH

@OptIn(ExperimentalForeignApi::class)
actual class HmacProvider actual constructor() {
    actual fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val macOut = ByteArray(CC_SHA256_DIGEST_LENGTH)
        key.usePinned { k ->
            data.usePinned { d ->
                macOut.usePinned { m ->
                    CCHmac(kCCHmacAlgSHA256, k.addressOf(0), key.size.convert(),
                        d.addressOf(0), data.size.convert(), m.addressOf(0))
                }
            }
        }
        return macOut
    }
    actual fun hmacSha256(key: String, data: String): ByteArray =
        hmacSha256(key.encodeToByteArray(), data.encodeToByteArray())
}
```

## AesGcmProvider (CryptoKit via Swift interop)
CommonCrypto has NO native GCM mode. Create this Swift helper in `iosApp/`:
```swift
// iosApp/iosApp/AesGcmHelper.swift
import CryptoKit
import Foundation

@objc public class AesGcmHelper: NSObject {
    @objc public static func seal(
        key: Data, nonce: Data, plaintext: Data, aad: Data?
    ) -> Data? {
        guard key.count == 32, nonce.count == 12 else { return nil }
        guard let n = try? AES.GCM.Nonce(data: nonce),
              let box = try? AES.GCM.seal(
                  plaintext, using: SymmetricKey(data: key),
                  nonce: n, authenticating: aad ?? Data()
              ) else { return nil }
        return box.combined  // nonce(12) + ciphertext + tag(16)
    }

    @objc public static func open(
        key: Data, combined: Data, aad: Data?
    ) -> Data? {
        guard key.count == 32,
              let box = try? AES.GCM.SealedBox(combined: combined) else { return nil }
        return try? AES.GCM.open(
            box, using: SymmetricKey(data: key), authenticating: aad ?? Data())
    }
}
```

Kotlin actual calls the Swift helper via ObjC interop:
```kotlin
package {your.package}.crypto

import kotlinx.cinterop.*
import platform.Foundation.NSData
import platform.Foundation.create
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual class AesGcmProvider actual constructor() {
    private val secureRandom = SecureRandomProvider()

    actual fun encrypt(
        key: ByteArray, plaintext: ByteArray, associatedData: ByteArray?,
    ): EncryptionResult {
        require(key.size == 32) { "AES-256 requires a 32-byte key, got ${key.size}" }
        val iv = secureRandom.nextBytes(12)
        val combined = AesGcmHelper.seal(
            key = key.toNSData(), nonce = iv.toNSData(),
            plaintext = plaintext.toNSData(), aad = associatedData?.toNSData(),
        ) ?: error("AES-GCM seal failed")
        val bytes = combined.toByteArray()
        return EncryptionResult(
            ciphertext = bytes.copyOfRange(12, bytes.size - 16),
            iv = iv,
            authTag = bytes.copyOfRange(bytes.size - 16, bytes.size),
        )
    }

    actual fun decrypt(
        key: ByteArray, ciphertext: ByteArray, iv: ByteArray,
        authTag: ByteArray, associatedData: ByteArray?,
    ): ByteArray {
        require(key.size == 32) { "AES-256 requires a 32-byte key, got ${key.size}" }
        val combined = iv + ciphertext + authTag
        return AesGcmHelper.open(
            key = key.toNSData(), combined = combined.toNSData(),
            aad = associatedData?.toNSData(),
        )?.toByteArray() ?: error("AES-GCM open failed — wrong key, tampered data, or AAD mismatch")
    }
}

private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = size.convert())
}

private fun NSData.toByteArray(): ByteArray = ByteArray(length.toInt()).also { bytes ->
    if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), this.bytes, length) }
}
```

## KeyDerivation (PBKDF2 via CommonCrypto)
```kotlin
package {your.package}.crypto

import kotlinx.cinterop.*
import platform.CoreCrypto.*

@OptIn(ExperimentalForeignApi::class)
actual class KeyDerivation actual constructor() {
    actual fun deriveKey(
        password: String, salt: ByteArray, iterations: Int, keyLengthBytes: Int,
    ): ByteArray {
        val pw = password.encodeToByteArray()
        val derivedKey = ByteArray(keyLengthBytes)
        pw.usePinned { pwPin ->
            salt.usePinned { saltPin ->
                derivedKey.usePinned { keyPin ->
                    val status = CCKeyDerivationPBKDF(
                        kCCPBKDF2, pwPin.addressOf(0).reinterpret(), pw.size.convert(),
                        saltPin.addressOf(0).reinterpret(), salt.size.convert(),
                        kCCPRFHmacAlgSHA256, iterations.convert(),
                        keyPin.addressOf(0).reinterpret(), keyLengthBytes.convert(),
                    )
                    require(status == kCCSuccess) { "CCKeyDerivationPBKDF failed: $status" }
                }
            }
        }
        return derivedKey
    }
}
```
