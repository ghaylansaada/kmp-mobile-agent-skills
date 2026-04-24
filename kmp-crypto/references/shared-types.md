# Shared Types and Expect Declarations (commonMain)

## CryptoResult Sealed Interface

```kotlin
package {your.package}.crypto

sealed interface CryptoResult {
    data class Success(val data: ByteArray) : CryptoResult {
        override fun equals(other: Any?): Boolean =
            this === other || (other is Success && data.contentEquals(other.data))
        override fun hashCode(): Int = data.contentHashCode()
    }
    data class Failure(val message: String, val cause: Throwable? = null) : CryptoResult
}
```

## EncryptionResult and Hex Utilities

```kotlin
package {your.package}.crypto

data class EncryptionResult(
    val ciphertext: ByteArray,
    val iv: ByteArray,       // 12 bytes for GCM
    val authTag: ByteArray,  // 16 bytes for GCM
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptionResult) return false
        return ciphertext.contentEquals(other.ciphertext) &&
            iv.contentEquals(other.iv) &&
            authTag.contentEquals(other.authTag)
    }

    override fun hashCode(): Int {
        var result = ciphertext.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + authTag.contentHashCode()
        return result
    }
}

fun ByteArray.toHex(): String = buildString(size * 2) {
    for (byte in this@toHex) {
        val value = byte.toInt() and 0xFF
        append(HEX_CHARS[value shr 4])
        append(HEX_CHARS[value and 0x0F])
    }
}

fun String.hexToByteArray(): ByteArray {
    require(length % 2 == 0) { "Hex string must have even length" }
    return ByteArray(length / 2) { i ->
        val high = Character.digit(this[i * 2], 16)
        val low = Character.digit(this[i * 2 + 1], 16)
        require(high != -1 && low != -1) { "Invalid hex character at index ${i * 2}" }
        ((high shl 4) or low).toByte()
    }
}

private val HEX_CHARS = "0123456789abcdef".toCharArray()
```

## Expect Declarations

```kotlin
package {your.package}.crypto

expect class SecureRandomProvider() {
    fun nextBytes(size: Int): ByteArray
    fun nextInt(bound: Int): Int
}

expect class HashProvider() {
    fun sha256(input: ByteArray): ByteArray
    fun sha256(input: String): ByteArray
}

expect class HmacProvider() {
    fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray
    fun hmacSha256(key: String, data: String): ByteArray
}

expect class AesGcmProvider() {
    fun encrypt(
        key: ByteArray,
        plaintext: ByteArray,
        associatedData: ByteArray? = null,
    ): EncryptionResult

    fun decrypt(
        key: ByteArray,
        ciphertext: ByteArray,
        iv: ByteArray,
        authTag: ByteArray,
        associatedData: ByteArray? = null,
    ): ByteArray
}

expect class KeyDerivation() {
    fun deriveKey(
        password: String,
        salt: ByteArray,
        iterations: Int = 600_000,
        keyLengthBytes: Int = 32,
    ): ByteArray
}
```

## CryptoProvider Facade

```kotlin
package {your.package}.crypto

class CryptoProvider(
    val hash: HashProvider,
    val hmac: HmacProvider,
    val aesGcm: AesGcmProvider,
    val keyDerivation: KeyDerivation,
    val secureRandom: SecureRandomProvider,
) {
    fun sha256Hex(input: String): String =
        hash.sha256(input).toHex()

    fun hmacSha256Hex(key: String, data: String): String =
        hmac.hmacSha256(key, data).toHex()

    fun encryptWithPassword(plaintext: String, password: String): String {
        val salt = secureRandom.nextBytes(16)
        val key = keyDerivation.deriveKey(password, salt)
        try {
            val result = aesGcm.encrypt(key, plaintext.encodeToByteArray())
            return salt.toHex() +
                result.iv.toHex() +
                result.authTag.toHex() +
                result.ciphertext.toHex()
        } finally {
            key.fill(0) // Clear derived key from memory
        }
    }

    // Parse fixed-length hex fields: salt(32) + iv(24) + tag(32) + ciphertext(rest)
    fun decryptWithPassword(payload: String, password: String): String {
        val salt = payload.substring(0, 32).hexToByteArray()
        val iv = payload.substring(32, 56).hexToByteArray()
        val authTag = payload.substring(56, 88).hexToByteArray()
        val ciphertext = payload.substring(88).hexToByteArray()
        val key = keyDerivation.deriveKey(password, salt)
        try {
            return aesGcm.decrypt(key, ciphertext, iv, authTag).decodeToString()
        } finally {
            key.fill(0)
        }
    }

    fun randomHex(byteLength: Int): String =
        secureRandom.nextBytes(byteLength).toHex()
}
```
