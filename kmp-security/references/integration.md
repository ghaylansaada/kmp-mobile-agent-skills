# Security -- Integration

## 1. Koin Module Setup

Register security components in your Koin module. See
`assets/templates/SecurityModule.kt` for the full template.

```kotlin
// Android
val securityModule = module {
    single { SecureTokenStorage(context = get()) }
    single { DeviceIntegrityChecker(context = get()) }
    single { SecureDataStoreWrapper(dataStore = get(), secureTokenStorage = get()) }
}

// iOS (no Context parameter)
val securityModule = module {
    single { SecureTokenStorage() }
    single { DeviceIntegrityChecker() }
    single { SecureDataStoreWrapper(dataStore = get(), secureTokenStorage = get()) }
}
```

Add to Koin init: `startKoin { modules(securityModule) }`

## 2. Ktor Client with Certificate Pinning

```kotlin
// Android
val httpClient = HttpClient(OkHttp) {
    installCertificatePinning()
    install(ContentNegotiation) { json() }
}

// iOS
val httpClient = HttpClient(Darwin) {
    installCertificatePinning()
    install(ContentNegotiation) { json() }
}
```

## 3. Device Integrity Check at App Startup

```kotlin
import {your.package}.security.DeviceIntegrityChecker
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AppStartupChecks : KoinComponent {
    private val integrityChecker: DeviceIntegrityChecker by inject()

    fun performSecurityChecks(): SecurityCheckResult = SecurityCheckResult(
        isDeviceTrusted = !integrityChecker.isDeviceCompromised(),
        warnings = integrityChecker.getCompromiseReasons(),
    )
}

data class SecurityCheckResult(
    val isDeviceTrusted: Boolean,
    val warnings: List<String>,
)
```

## 4. Using Obfuscated Endpoints

```kotlin
import {your.package}.security.EndpointObfuscator

object ApiEndpoints {
    // Pre-encoded at build time (use EndpointObfuscator.encode() to generate)
    private val AUTH_TOKEN_ENCODED = "ZhQGOBxiFVIVGQRhEBxd"
    private val USER_PROFILE_ENCODED = "ZhQGOBxiTEIEHQ=="

    val authToken: String get() = EndpointObfuscator.decode(AUTH_TOKEN_ENCODED)
    val userProfile: String get() = EndpointObfuscator.decode(USER_PROFILE_ENCODED)
}
```

## 5. Using SecureDataStoreWrapper

```kotlin
import {your.package}.security.SecureDataStoreWrapper
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class UserPreferencesRepository : KoinComponent {
    private val secureDataStore: SecureDataStoreWrapper by inject()

    suspend fun saveUserEmail(email: String) {
        secureDataStore.putSecureString("user_email", email)
    }

    suspend fun getUserEmail(): String? =
        secureDataStore.getSecureString("user_email").first()

    suspend fun clearUserEmail() {
        secureDataStore.removeSecureString("user_email")
    }
}
```
