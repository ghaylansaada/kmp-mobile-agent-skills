# Secure Token Storage: expect/actual

Tokens must be stored in the platform keystore, not in plain DataStore Preferences
or SharedPreferences. DataStore writes protobuf/XML to disk in plaintext, readable
by any process with root access or a device backup extraction.

## Common Interface

**File:** `commonMain/kotlin/{your.package}/core/session/SecureTokenStorage.kt`

```kotlin
package {your.package}.core.session

@JvmInline
value class AccessToken(val value: String)

@JvmInline
value class RefreshToken(val value: String)

expect class SecureTokenStorage {
    fun getAccessToken(): AccessToken?
    fun getRefreshToken(): RefreshToken?
    fun setTokens(accessToken: AccessToken, refreshToken: RefreshToken)
    fun clear()
}
```

The `value class` wrappers prevent accidentally swapping access and refresh tokens
at call sites. The compiler erases them at runtime so there is no allocation overhead.

## Android Implementation

**File:** `androidMain/kotlin/{your.package}/core/session/SecureTokenStorage.android.kt`

Uses `EncryptedSharedPreferences` from AndroidX Security, backed by Android Keystore.

```kotlin
package {your.package}.core.session

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

actual class SecureTokenStorage(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    actual fun getAccessToken(): AccessToken? =
        prefs.getString(KEY_ACCESS_TOKEN, null)?.let(::AccessToken)

    actual fun getRefreshToken(): RefreshToken? =
        prefs.getString(KEY_REFRESH_TOKEN, null)?.let(::RefreshToken)

    actual fun setTokens(accessToken: AccessToken, refreshToken: RefreshToken) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken.value)
            .putString(KEY_REFRESH_TOKEN, refreshToken.value)
            .apply()
    }

    actual fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val FILE_NAME = "secure_session_prefs"
        const val KEY_ACCESS_TOKEN = "session.accessToken"
        const val KEY_REFRESH_TOKEN = "session.refreshToken"
    }
}
```

## iOS Implementation

**File:** `iosMain/kotlin/{your.package}/core/session/SecureTokenStorage.ios.kt`

Uses iOS Keychain via `Security` framework interop.

```kotlin
package {your.package}.core.session

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryRef
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

actual class SecureTokenStorage {

    actual fun getAccessToken(): AccessToken? =
        readKeychain(KEY_ACCESS_TOKEN)?.let(::AccessToken)

    actual fun getRefreshToken(): RefreshToken? =
        readKeychain(KEY_REFRESH_TOKEN)?.let(::RefreshToken)

    actual fun setTokens(
        accessToken: AccessToken,
        refreshToken: RefreshToken,
    ) {
        writeKeychain(KEY_ACCESS_TOKEN, accessToken.value)
        writeKeychain(KEY_REFRESH_TOKEN, refreshToken.value)
    }

    actual fun clear() {
        deleteKeychain(KEY_ACCESS_TOKEN)
        deleteKeychain(KEY_REFRESH_TOKEN)
    }

    private companion object {
        const val SERVICE_NAME = "com.app.session"
        const val KEY_ACCESS_TOKEN = "session.accessToken"
        const val KEY_REFRESH_TOKEN = "session.refreshToken"
    }
}
```

The `readKeychain`, `writeKeychain`, and `deleteKeychain` helper functions use
the Security framework's `SecItemCopyMatching`, `SecItemAdd`/`SecItemUpdate`,
and `SecItemDelete` respectively. Full Keychain helper implementation varies
by project -- see Apple's Keychain Services documentation.

## Gradle Dependencies

```toml
[libraries]
# Android only -- for EncryptedSharedPreferences
androidx-security-crypto = { group = "androidx.security", name = "security-crypto", version.ref = "androidxSecurity" }

[versions]
# Always use latest stable version — check official release page
androidxSecurity = "..."
```

```kotlin
kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.security.crypto)
        }
    }
}
```

## Testing

For unit tests, use `FakeSecureTokenStorage` (see **kmp-testing** skill)
that stores tokens in a plain `MutableMap`. This avoids platform keystore
dependencies in commonTest.
