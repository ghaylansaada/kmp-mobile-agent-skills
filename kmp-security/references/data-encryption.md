# Data Encryption

Endpoint obfuscation, encrypted DataStore wrapper, and encrypted Room database.

## commonMain -- EndpointObfuscator.kt

XOR + Base64 obfuscation of API endpoint strings. NOT cryptographic -- just raises the
bar for casual reverse engineering via static analysis.

```kotlin
package {your.package}.security

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object EndpointObfuscator {
    private val xorKey = byteArrayOf(
        0x4A, 0x7E, 0x2B, 0x59, 0x6D, 0x1F, 0x3C, 0x68,
    )

    @OptIn(ExperimentalEncodingApi::class)
    fun encode(plaintext: String): String {
        val bytes = plaintext.encodeToByteArray()
        return Base64.encode(
            ByteArray(bytes.size) { i ->
                (bytes[i].toInt() xor xorKey[i % xorKey.size].toInt()).toByte()
            },
        )
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun decode(encoded: String): String {
        val xored = Base64.decode(encoded)
        return ByteArray(xored.size) { i ->
            (xored[i].toInt() xor xorKey[i % xorKey.size].toInt()).toByte()
        }.decodeToString()
    }
}
```

## commonMain -- SecureDataStoreWrapper.kt

Wraps DataStore with XOR encryption keyed from the platform keystore.
XOR is symmetric obfuscation, not AES-level encryption. For stronger protection,
integrate with a proper AES-GCM implementation via `expect`/`actual`.

The encryption key is generated once per install using `kotlin.random.Random`
(not cryptographically secure) and stored in the platform keystore. For
production apps requiring stronger guarantees, replace the key generation
with platform-specific `SecureRandom` (Android) or `SecRandomCopyBytes` (iOS).

```kotlin
package {your.package}.security

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SecureDataStoreWrapper(
    private val dataStore: DataStore<Preferences>,
    private val secureTokenStorage: SecureTokenStorage,
) {
    private val encryptionKeyAlias = "datastore_encryption_key"

    suspend fun putSecureString(key: String, value: String) {
        val encKey = getOrCreateEncryptionKey()
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey(key)] = xorEncrypt(value, encKey)
        }
    }

    fun getSecureString(key: String): Flow<String?> {
        val prefKey = stringPreferencesKey(key)
        return dataStore.data.map { prefs ->
            prefs[prefKey]?.let { encrypted ->
                xorDecrypt(encrypted, getOrCreateEncryptionKey())
            }
        }
    }

    suspend fun removeSecureString(key: String) {
        dataStore.edit { prefs ->
            prefs.remove(stringPreferencesKey(key))
        }
    }

    private suspend fun getOrCreateEncryptionKey(): String {
        secureTokenStorage.getToken(encryptionKeyAlias)?.let { return it }
        val chars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        val key = (1..32).map { chars.random() }.joinToString("")
        secureTokenStorage.saveToken(encryptionKeyAlias, key)
        return key
    }

    private fun xorEncrypt(plaintext: String, key: String): String {
        val keyBytes = key.encodeToByteArray()
        val inputBytes = plaintext.encodeToByteArray()
        return ByteArray(inputBytes.size) { i ->
            (inputBytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
        }.joinToString("") { "%02x".format(it) }
    }

    private fun xorDecrypt(hex: String, key: String): String {
        val keyBytes = key.encodeToByteArray()
        val inputBytes = hex.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
        return ByteArray(inputBytes.size) { i ->
            (inputBytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
        }.decodeToString()
    }
}
```

## androidMain -- EncryptedDatabaseFactory.kt

If the passphrase stored in Keystore is lost (factory reset, Keystore corruption), the
encrypted database is permanently unreadable. Design for clean re-sync.

```kotlin
package {your.package}.security

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

object EncryptedDatabaseFactory {
    private const val DB_PASSPHRASE_KEY = "room_db_passphrase"

    suspend fun <T : RoomDatabase> createEncryptedDatabase(
        context: Context,
        klass: Class<T>,
        databaseName: String,
        secureTokenStorage: SecureTokenStorage,
    ): T {
        val passphrase = getOrCreatePassphrase(secureTokenStorage)
        return Room.databaseBuilder(context, klass, databaseName)
            .openHelperFactory(SupportOpenHelperFactory(passphrase.toByteArray()))
            .build()
    }

    private suspend fun getOrCreatePassphrase(
        storage: SecureTokenStorage,
    ): String {
        storage.getToken(DB_PASSPHRASE_KEY)?.let { return it }
        val chars = ('A'..'Z') + ('a'..'z') + ('0'..'9') + "!@#$%^&*".toList()
        val passphrase = (1..64).map { chars.random() }.joinToString("")
        storage.saveToken(DB_PASSPHRASE_KEY, passphrase)
        return passphrase
    }
}
```
