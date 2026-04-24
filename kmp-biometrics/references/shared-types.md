# Shared Biometric Types (commonMain)

## BiometricResult

**File:** `core/biometrics/BiometricResult.kt`

```kotlin
package {your.package}.core.biometrics

sealed interface BiometricResult {
    data object Success : BiometricResult
    data class Failure(val message: String) : BiometricResult
    data object Cancelled : BiometricResult
    data object NotAvailable : BiometricResult
    data object NotEnrolled : BiometricResult
}
```

## BiometricAuthenticator -- expect Declaration

**File:** `core/biometrics/BiometricAuthenticator.kt`

```kotlin
package {your.package}.core.biometrics

import {your.package}.core.platform.PlatformContext

expect class BiometricAuthenticator(context: PlatformContext) {
    fun canAuthenticate(): Boolean
    fun canAuthenticateWithDeviceCredential(): Boolean
    suspend fun authenticate(
        title: String,
        subtitle: String,
        allowDeviceCredential: Boolean = true,
    ): BiometricResult
}
```

## SecureCredentialStore -- expect Declaration

**File:** `core/biometrics/SecureCredentialStore.kt`

```kotlin
package {your.package}.core.biometrics

import {your.package}.core.platform.PlatformContext

expect class SecureCredentialStore(context: PlatformContext) {
    fun store(key: String, value: String)
    fun retrieve(key: String): String?
    fun delete(key: String)
    fun clear()
}
```
