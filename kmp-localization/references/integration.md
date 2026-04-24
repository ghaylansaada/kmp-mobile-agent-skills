# Integration

## Upstream Dependencies

| Skill | Provides |
|-------|----------|
| kmp-resources-management | composeResources directory, Res class generation |
| kmp-compose-ui | MaterialTheme, CompositionLocalProvider, LocalLayoutDirection |
| kmp-dependency-injection | Koin module registration, DataStore injection |

## App.kt Wiring

```kotlin
package {your.package}

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import org.koin.compose.koinInject
import {your.package}.core.localization.LocaleManager
import {your.package}.ui.theme.AppTheme

@Composable
fun App() {
    val localeManager: LocaleManager = koinInject()

    LaunchedEffect(Unit) {
        localeManager.initialize()
    }

    AppTheme(localeManager = localeManager) {
        // Navigation host / main content
    }
}
```

## Module Loading Order

```kotlin
import org.koin.core.context.startKoin

startKoin {
    modules(
        platformModule(context, config),
        localStorageModule(),          // provides DataStore
        localizationModule(),          // provides LocaleManager (needs DataStore)
        networkModule(),               // sends Accept-Language header
        // ... feature modules
    )
}
```

## Accept-Language Header

Send current locale to the server:

```kotlin
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpHeaders
import {your.package}.core.localization.LocaleManager

fun HttpClientConfig<*>.installLocaleHeader(localeManager: LocaleManager) {
    defaultRequest {
        headers.append(HttpHeaders.AcceptLanguage, localeManager.currentLocale.value)
    }
}
```

## Android Launcher App Name

To localize the Android launcher name, add locale-specific Android resource directories:

```
composeApp/src/androidMain/res/values/strings.xml        (English)
composeApp/src/androidMain/res/values-ar/strings.xml     (Arabic)
```

These are separate from Compose Multiplatform resources and only affect Android system surfaces.

## Connected Skills

- **kmp-networking** -- ApiResult.Error types consumed by ErrorMessageResolver
- **kmp-networking** -- Accept-Language header injection
