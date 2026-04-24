# iOS Implementation: LocalAuthentication + Keychain

## BiometricAuthenticator -- actual

**File:** `iosMain/.../core/biometrics/BiometricAuthenticator.ios.kt`

```kotlin
package {your.package}.core.biometrics

import {your.package}.core.platform.PlatformContext
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAErrorBiometryNotAvailable
import platform.LocalAuthentication.LAErrorBiometryNotEnrolled
import platform.LocalAuthentication.LAErrorUserCancel
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import kotlin.coroutines.resume

actual class BiometricAuthenticator actual constructor(
    @Suppress("UNUSED_PARAMETER") context: PlatformContext,
) {
    actual fun canAuthenticate(): Boolean =
        LAContext().canEvaluatePolicy(
            LAPolicyDeviceOwnerAuthenticationWithBiometrics,
            error = null,
        )

    actual fun canAuthenticateWithDeviceCredential(): Boolean =
        LAContext().canEvaluatePolicy(
            LAPolicyDeviceOwnerAuthentication,
            error = null,
        )

    actual suspend fun authenticate(
        title: String,
        subtitle: String,
        allowDeviceCredential: Boolean,
    ): BiometricResult = suspendCancellableCoroutine { continuation ->
        // LAContext is single-use -- create a fresh instance per call
        val laContext = LAContext()
        val policy = if (allowDeviceCredential) {
            LAPolicyDeviceOwnerAuthentication
        } else {
            LAPolicyDeviceOwnerAuthenticationWithBiometrics
        }

        if (!laContext.canEvaluatePolicy(policy, error = null)) {
            continuation.resume(BiometricResult.NotAvailable)
            return@suspendCancellableCoroutine
        }

        laContext.evaluatePolicy(
            policy,
            localizedReason = title,
        ) { success, error ->
            if (continuation.isActive) {
                continuation.resume(
                    when {
                        success -> BiometricResult.Success
                        error != null -> mapLAError(error)
                        else -> BiometricResult.Failure("Unknown error")
                    },
                )
            }
        }
    }

    private fun mapLAError(error: NSError): BiometricResult =
        when (error.code) {
            LAErrorUserCancel -> BiometricResult.Cancelled
            LAErrorBiometryNotEnrolled -> BiometricResult.NotEnrolled
            LAErrorBiometryNotAvailable -> BiometricResult.NotAvailable
            else -> BiometricResult.Failure(
                error.localizedDescription ?: "Authentication failed",
            )
        }
}
```

## SecureCredentialStore -- actual (Keychain-backed)

**File:** `iosMain/.../core/biometrics/SecureCredentialStore.ios.kt`

```kotlin
package {your.package}.core.biometrics

import {your.package}.core.platform.PlatformContext
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import platform.CoreFoundation.CFDictionaryRef
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

@OptIn(ExperimentalForeignApi::class)
actual class SecureCredentialStore actual constructor(
    @Suppress("UNUSED_PARAMETER") context: PlatformContext,
) {
    private val serviceName = "{your.package}.credentials"

    actual fun store(key: String, value: String) {
        delete(key) // Remove existing entry to avoid errSecDuplicateItem
        val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding)
            ?: return
        val query = mapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to serviceName,
            kSecAttrAccount to key,
            kSecValueData to data,
        )
        @Suppress("UNCHECKED_CAST")
        SecItemAdd(query as CFDictionaryRef, null)
    }

    actual fun retrieve(key: String): String? {
        val query = mapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to serviceName,
            kSecAttrAccount to key,
            kSecReturnData to true,
            kSecMatchLimit to kSecMatchLimitOne,
        )
        @Suppress("UNCHECKED_CAST")
        memScoped {
            val result = alloc<kotlinx.cinterop.ObjCObjectVar<Any?>>()
            val status = SecItemCopyMatching(query as CFDictionaryRef, result.ptr)
            if (status == errSecSuccess) {
                val data = result.value as? NSData ?: return null
                return NSString.create(data = data, encoding = NSUTF8StringEncoding)
                    as? String
            }
        }
        return null
    }

    actual fun delete(key: String) {
        val query = mapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to serviceName,
            kSecAttrAccount to key,
        )
        @Suppress("UNCHECKED_CAST")
        SecItemDelete(query as CFDictionaryRef)
    }

    actual fun clear() {
        val query = mapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to serviceName,
        )
        @Suppress("UNCHECKED_CAST")
        SecItemDelete(query as CFDictionaryRef)
    }
}
```
