# KoinInitializer and Module Registration

## KoinInitializer

**File:** `commonMain/kotlin/{your/package}/di/KoinInitializer.kt`

```kotlin
package {your.package}.di

import {your.package}.platform.PlatformConfig
import {your.package}.platform.PlatformContext
import kotlin.concurrent.AtomicBoolean
import org.koin.core.context.startKoin

private val koinStarted = AtomicBoolean(false)

fun initKoin(context: PlatformContext, config: PlatformConfig) {
    if (!koinStarted.compareAndSet(expectedValue = false, newValue = true)) return
    startKoin {
        modules(commonModules(context) + platformModule(context, config))
    }
}
```

Uses `AtomicBoolean` for thread-safe double-init guard. On Android, `initKoin()`
is called from `onCreate()` which runs on the main thread, but on iOS the
`ComposeUIViewController` configure block timing can vary.

## CommonModules

**File:** `commonMain/kotlin/{your/package}/di/CommonModules.kt`

```kotlin
package {your.package}.di

import {your.package}.platform.PlatformContext
import org.koin.core.module.Module

fun commonModules(context: PlatformContext): List<Module> = listOf(
    coreModule(context),
    localStorageModule,
    sessionModule,
    ktorfitModule,
    externalStorageModule,
    imageLoaderModule,
    repositoryModule,
    viewModelModule,
)
```
