# Build Configuration Integration

## Migration from Hardcoded Config

Before:

```kotlin
object AppConfig {
    const val BASE_URL = "https://api.example.com/"
}
```

After:

```kotlin
import {your.package}.BuildKonfig

object AppConfig {
    val BASE_URL: String = BuildKonfig.BASE_URL
}
```

No call-site changes needed if you keep the `AppConfig` wrapper. Note: `const val` becomes `val` -- code using these in annotation arguments will fail with `Const val required`.

## Feature Flag Usage

```kotlin
import {your.package}.config.FeatureFlags

fun logDebug(tag: String, message: String) {
    if (FeatureFlags.loggingEnabled) println("[$tag] $message")
}

fun initAnalytics() {
    if (!FeatureFlags.analyticsEnabled) return
    // analytics SDK initialization
}

class UserRepository(private val api: UserApi) {
    suspend fun getUsers(): List<User> {
        if (FeatureFlags.mockDataEnabled) {
            return listOf(User(id = 1, name = "Mock User"))
        }
        return api.fetchUsers()
    }
}
```

## Ktor Client Configuration

```kotlin
import {your.package}.config.AppConfig
import {your.package}.config.FeatureFlags
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun createHttpClient(): HttpClient = HttpClient {
    defaultRequest { url(AppConfig.BASE_URL) }
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    if (FeatureFlags.loggingEnabled) {
        install(Logging) { level = LogLevel.ALL }
    }
    install(HttpTimeout) {
        requestTimeoutMillis = AppConfig.API_TIMEOUT_SECONDS * 1000L
    }
}
```

## Koin Module

```kotlin
import org.koin.dsl.module
import {your.package}.config.FeatureFlags

val networkModule = module {
    single { createHttpClient() }
    single<UserApi> {
        if (FeatureFlags.mockDataEnabled) MockUserApi() else RealUserApi(get())
    }
}
```

## Adding a New Feature Flag

1. Add the field to **every** flavor in `buildkonfig {}`:

```kotlin
buildConfigField(BOOLEAN, "ENABLE_NEW_FEATURE", "false") // in each flavor
```

2. Add accessor in `FeatureFlags.kt`:

```kotlin
val newFeatureEnabled: Boolean = BuildKonfig.ENABLE_NEW_FEATURE
```

3. Use: `if (FeatureFlags.newFeatureEnabled) { ... }`

## Adding a New Environment String

1. Add to every flavor:

```kotlin
buildConfigField(STRING, "CDN_URL", "https://dev-cdn.example.com/")
```

2. Expose in `AppConfig.kt`:

```kotlin
val CDN_URL: String = BuildKonfig.CDN_URL
```
