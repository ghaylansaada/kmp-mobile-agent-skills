# Setup: Session Management Dependencies

> **Note:** Version numbers are intentionally omitted. Always use the latest stable release from the official documentation.

## Prerequisites

Before implementing session management, ensure the following skills are completed:

- **kmp-networking** -- Ktor base client is configured with Auth plugin
- **kmp-database** -- Room AppDatabase with `clearAllTables()` extension
- **kmp-dependency-injection** -- Koin DI graph is operational

## Artifacts Used by Session Management

```toml
[versions]
# Always use latest stable versions — check official release pages
androidxSecurity = "..."

[libraries]
# Ktor Auth (from kmp-networking)
ktor-client-auth = { group = "io.ktor", name = "ktor-client-auth" }
ktor-client-content-negotiation = { group = "io.ktor", name = "ktor-client-content-negotiation" }
ktor-client-logging = { group = "io.ktor", name = "ktor-client-logging" }
ktor-serialization-kotlinx-json = { group = "io.ktor", name = "ktor-serialization-kotlinx-json" }

# Android secure storage
androidx-security-crypto = { group = "androidx.security", name = "security-crypto", version.ref = "androidxSecurity" }
```

## build.gradle.kts (composeApp)

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            // Ktor Auth + networking (kmp-networking)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
        androidMain.dependencies {
            // EncryptedSharedPreferences for secure token storage
            implementation(libs.androidx.security.crypto)
        }
        // iOS uses Keychain via Security framework (no extra dependency)
    }
}
```

## Directory Structure

```
composeApp/src/
    commonMain/kotlin/{your.package}/
        core/
            session/
                SessionState.kt
                SessionManager.kt
                SecureTokenStorage.kt          # expect declaration
            network/
                HttpClientFactory.kt
            config/
                AppConfig.kt
        data/
            remote/
                dto/
                    refreshauth/
                        RefreshAuthRequestDto.kt
                        RefreshAuthResponseDto.kt
        di/
            modules/
                SessionModule.kt
                KtorfitModule.kt
    androidMain/kotlin/{your.package}/
        core/
            session/
                SecureTokenStorage.android.kt   # actual (EncryptedSharedPreferences)
    iosMain/kotlin/{your.package}/
        core/
            session/
                SecureTokenStorage.ios.kt       # actual (Keychain)
```

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| `Unresolved reference: BearerTokens` | Missing `ktor-client-auth` | Add `ktor-client-auth` to `commonMain.dependencies` |
| `Unresolved reference: bearer` | Missing `ktor-client-auth` | Same as above; `bearer {}` is an extension from ktor-client-auth |
| `Unresolved reference: Mutex` | Missing coroutines | Ensure `kotlinx-coroutines-core` is on classpath (transitive via Ktor) |
| `Unresolved reference: EncryptedSharedPreferences` | Missing security-crypto | Add `androidx-security-crypto` to `androidMain.dependencies` |
| `Unresolved reference: SecItemAdd` | Missing cinterop for Security | Ensure `platform.Security` imports work in iosMain (built-in K/N interop) |
| `No actual declaration for expect class SecureTokenStorage` | Missing platform source set | Create `SecureTokenStorage.android.kt` and `SecureTokenStorage.ios.kt` |
