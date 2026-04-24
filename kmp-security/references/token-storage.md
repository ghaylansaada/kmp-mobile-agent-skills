# Secure Token Storage

Platform-specific secure storage: Android Keystore + EncryptedSharedPreferences / iOS Keychain.

## commonMain -- SecureTokenStorage.kt

```kotlin
package {your.package}.security

expect class SecureTokenStorage {
    suspend fun saveToken(key: String, value: String)
    suspend fun getToken(key: String): String?
    suspend fun deleteToken(key: String)
    suspend fun clearAll()
}
```

## androidMain -- SecureTokenStorage.android.kt

EncryptedSharedPreferences can throw `KeyStoreException` if the device lock screen
changes or the device is restored from backup. Wrap init in try/catch with a fallback
that clears and recreates.

```kotlin
package {your.package}.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual class SecureTokenStorage(private val context: Context) {
    private val masterKey: MasterKey by lazy {
        val spec = KeyGenParameterSpec.Builder(
            MasterKey.DEFAULT_MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        MasterKey.Builder(context)
            .setKeyGenParameterSpec(spec)
            .build()
    }

    private val prefs: SharedPreferences by lazy {
        try {
            createEncryptedPrefs()
        } catch (_: Exception) {
            // Master key corrupted -- clear and recreate
            File(context.filesDir.parent, "shared_prefs/secure_token_prefs.xml")
                .delete()
            createEncryptedPrefs()
        }
    }

    private fun createEncryptedPrefs(): SharedPreferences =
        EncryptedSharedPreferences.create(
            context,
            "secure_token_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

    actual suspend fun saveToken(key: String, value: String) =
        withContext(Dispatchers.IO) {
            prefs.edit().putString(key, value).apply()
        }

    actual suspend fun getToken(key: String): String? =
        withContext(Dispatchers.IO) {
            prefs.getString(key, null)
        }

    actual suspend fun deleteToken(key: String) =
        withContext(Dispatchers.IO) {
            prefs.edit().remove(key).apply()
        }

    actual suspend fun clearAll() =
        withContext(Dispatchers.IO) {
            prefs.edit().clear().apply()
        }
}
```

## iosMain -- SecureTokenStorage.ios.kt

`kSecAttrAccessibleWhenUnlockedThisDeviceOnly` means tokens are NOT included in
backups and are lost on device migration. This is intentional for security.

```kotlin
package {your.package}.security

import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.CoreFoundation.CFDictionaryRef
import platform.Foundation.*
import platform.Security.*
import platform.darwin.OSStatus

@OptIn(ExperimentalForeignApi::class)
actual class SecureTokenStorage {
    private val serviceName = "{your.package}.tokens"

    actual suspend fun saveToken(key: String, value: String) =
        withContext(Dispatchers.IO) {
            deleteToken(key)
            val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding)
                ?: return@withContext
            val query = mapOf<Any?, Any?>(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to serviceName,
                kSecAttrAccount to key,
                kSecValueData to data,
                kSecAttrAccessible to kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
            )
            @Suppress("UNCHECKED_CAST")
            SecItemAdd(query as CFDictionaryRef, null)
        }

    actual suspend fun getToken(key: String): String? =
        withContext(Dispatchers.IO) {
            val query = mapOf<Any?, Any?>(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to serviceName,
                kSecAttrAccount to key,
                kSecReturnData to true,
                kSecMatchLimit to kSecMatchLimitOne,
            )
            memScoped {
                val result = alloc<ObjCObjectVar<Any?>>()
                @Suppress("UNCHECKED_CAST")
                val status: OSStatus =
                    SecItemCopyMatching(query as CFDictionaryRef, result.ptr)
                if (status == errSecSuccess) {
                    val data = result.value as? NSData
                        ?: return@withContext null
                    NSString.create(data = data, encoding = NSUTF8StringEncoding)
                        as? String
                } else {
                    null
                }
            }
        }

    actual suspend fun deleteToken(key: String) =
        withContext(Dispatchers.IO) {
            val query = mapOf<Any?, Any?>(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to serviceName,
                kSecAttrAccount to key,
            )
            @Suppress("UNCHECKED_CAST")
            SecItemDelete(query as CFDictionaryRef)
        }

    actual suspend fun clearAll() =
        withContext(Dispatchers.IO) {
            val query = mapOf<Any?, Any?>(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to serviceName,
            )
            @Suppress("UNCHECKED_CAST")
            SecItemDelete(query as CFDictionaryRef)
        }
}
```
