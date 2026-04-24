# Platform Modules and Initialization

## Common Declaration

**File:** `commonMain/kotlin/{your/package}/di/PlatformModule.kt`

```kotlin
package {your.package}.di

import {your.package}.platform.PlatformConfig
import {your.package}.platform.PlatformContext
import org.koin.core.module.Module

expect fun platformModule(context: PlatformContext, config: PlatformConfig): Module
```

## Android Actual

**File:** `androidMain/kotlin/{your/package}/di/PlatformModule.android.kt`

```kotlin
package {your.package}.di

import {your.package}.platform.PlatformConfig
import {your.package}.platform.PlatformContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(context: PlatformContext, config: PlatformConfig): Module = module {
    single<Context> { context.androidContext }
    single<DataStoreFactory> { DataStoreFactory(context = get()) }
    single<HttpClientEngineFactory<*>> { OkHttp }
    single<DatabaseFactory> {
        DatabaseFactory(driver = BundledSQLiteDriver(), context = get(), dispatcher = Dispatchers.IO)
    }
    single<FileReaderFactory> { FileReaderFactory(context = get()) }
    single<FileWriterFactory> { FileWriterFactory(context = get()) }
}
```

## iOS Actual

**File:** `iosMain/kotlin/{your/package}/di/PlatformModule.ios.kt`

```kotlin
package {your.package}.di

import {your.package}.platform.PlatformConfig
import {your.package}.platform.PlatformContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(context: PlatformContext, config: PlatformConfig): Module = module {
    single<DataStoreFactory> { DataStoreFactory() }
    single<HttpClientEngineFactory<*>> { Darwin }
    single<DatabaseFactory> { DatabaseFactory(dispatcher = Dispatchers.Default) }
    single<FileReaderFactory> { FileReaderFactory() }
    single<FileWriterFactory> { FileWriterFactory() }
}
```

## Android Entry Point

**File:** `androidMain/kotlin/{your/package}/MainActivity.kt`

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initKoin(
            context = PlatformContext(this),
            config = PlatformConfig(platform = PlatformConfig.Platform.ANDROID),
        )
        setContent { App() }
    }
}
```

## iOS Entry Point

**File:** `iosMain/kotlin/{your/package}/MainViewController.kt`

```kotlin
fun MainViewController() = ComposeUIViewController(
    configure = {
        initKoin(
            context = PlatformContext(),
            config = PlatformConfig(platform = PlatformConfig.Platform.IOS),
        )
    }
) {
    App()
}
```
