# Integration

## Koin Module Order

`loggingModule` must be first so the logger is available to all other modules:

```kotlin
fun commonModules(context: PlatformContext, isDebug: Boolean) = listOf(
    loggingModule(isDebug),
    coreModule(context),
    // ... other modules
    repositoryModule(),
    viewModelModule(),
)
```

## Platform Entry Points

### Android

```kotlin
class MobileApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(
            context = this,
            config = PlatformConfig(Platform.OsType.ANDROID),
            isDebug = BuildConfig.DEBUG,
        )
    }
}
```

### iOS

```swift
@main
struct iOSApp: App {
    init() {
        #if DEBUG
        let isDebug = true
        #else
        let isDebug = false
        #endif
        KoinInitializerKt.doInitKoin(
            context: IosPlatformContext(),
            config: PlatformConfig(osType: .ios),
            isDebug: isDebug
        )
    }
}
```

### Updated KoinInitializer

```kotlin
import kotlin.concurrent.AtomicBoolean

private val koinStarted = AtomicBoolean(false)

fun initKoin(
    context: PlatformContext,
    config: PlatformConfig,
    isDebug: Boolean = false,
) {
    if (!koinStarted.compareAndSet(expectedValue = false, newValue = true)) return
    startKoin {
        modules(commonModules(context, isDebug) + platformModule(context, config))
    }
}
```

## Ktor HttpClient Wiring

```kotlin
fun createHttpClient(/* existing params */): HttpClient {
    return HttpClient {
        // ... existing configuration ...
        installKermitLogging()
    }
}
```

## Log Filtering

**Android Logcat:**
```
tag:HttpClient            -- Ktor request/response
tag:AccountRepository     -- repository API calls
tag:Transfer:DownloadTask -- download state transitions
```

**iOS Xcode Console:**
```
subsystem:{your.bundle.id} category:HttpClient
```

## Connected Skills

- **kmp-analytics-crashlytics** -- uses Kermit for analytics event logging and crash breadcrumbs
- **kmp-performance** -- tagged loggers for startup timing and profiling
