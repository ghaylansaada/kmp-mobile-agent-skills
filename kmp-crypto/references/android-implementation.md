# Android Actual Implementations

## SecureRandomProvider

```kotlin
package {your.package}.crypto

import java.security.SecureRandom

actual class SecureRandomProvider actual constructor() {
    private val secureRandom = SecureRandom()

    actual fun nextBytes(size: Int): ByteArray =
        ByteArray(size).also { secureRandom.nextBytes(it) }

    actual fun nextInt(bound: Int): Int = secureRandom.nextInt(bound)
}
```

## HashProvider (SHA-256)

```kotlin
package {your.package}.crypto

import java.security.MessageDigest

actual class HashProvider actual constructor() {
    actual fun sha256(input: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(input)

    actual fun sha256(input: String): ByteArray =
        sha256(input.encodeToByteArray())
}
```

## HmacProvider (HMAC-SHA256)

```kotlin
package {your.package}.crypto

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

actual class HmacProvider actual constructor() {
    actual fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    actual fun hmacSha256(key: String, data: String): ByteArray =
        hmacSha256(key.encodeToByteArray(), data.encodeToByteArray())
}
```

## AesGcmProvider (AES-256-GCM)

```kotlin
package {your.package}.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

actual class AesGcmProvider actual constructor() {
    private val secureRandom = SecureRandom()

    actual fun encrypt(
        key: ByteArray,
        plaintext: ByteArray,
        associatedData: ByteArray?,
    ): EncryptionResult {
        require(key.size == 32) { "AES-256 requires a 32-byte key, got ${key.size}" }
        val iv = ByteArray(12).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        associatedData?.let { cipher.updateAAD(it) }
        val output = cipher.doFinal(plaintext)
        // Android GCM appends 16-byte auth tag to ciphertext
        return EncryptionResult(
            ciphertext = output.copyOfRange(0, output.size - 16),
            iv = iv,
            authTag = output.copyOfRange(output.size - 16, output.size),
        )
    }

    actual fun decrypt(
        key: ByteArray,
        ciphertext: ByteArray,
        iv: ByteArray,
        authTag: ByteArray,
        associatedData: ByteArray?,
    ): ByteArray {
        require(key.size == 32) { "AES-256 requires a 32-byte key, got ${key.size}" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(128, iv),
        )
        associatedData?.let { cipher.updateAAD(it) }
        // Android GCM expects ciphertext + tag concatenated
        return cipher.doFinal(ciphertext + authTag)
    }
}
```

## KeyDerivation (PBKDF2)

```kotlin
package {your.package}.crypto

import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

actual class KeyDerivation actual constructor() {
    actual fun deriveKey(
        password: String,
        salt: ByteArray,
        iterations: Int,
        keyLengthBytes: Int,
    ): ByteArray {
        val keySpec = PBEKeySpec(
            password.toCharArray(),
            salt,
            iterations,
            keyLengthBytes * 8,
        )
        try {
            return SecretKeyFactory
                .getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(keySpec)
                .encoded
        } finally {
            keySpec.clearPassword()
        }
    }
}
```
