# Wiring Analytics into the Application

## Koin Module (commonMain)

```kotlin
package {{your_package}}.di.modules

import {{your_package}}.core.analytics.AnalyticsTracker
import {{your_package}}.core.analytics.CrashReporter
import {{your_package}}.core.analytics.createAnalyticsTracker
import {{your_package}}.core.analytics.createCrashReporter
import org.koin.dsl.module

fun analyticsModule() = module {
    single<AnalyticsTracker> { createAnalyticsTracker() }
    single<CrashReporter> { createCrashReporter() }
}
```

Register after `loggingModule` so the logger is available:

```kotlin
fun commonModules(context: PlatformContext, isDebug: Boolean) = buildList {
    add(loggingModule(isDebug))
    add(analyticsModule())
    add(coreModule(context))
    // other modules can now inject AnalyticsTracker and CrashReporter
}
```

## Repository Integration

Inject `CrashReporter` and report API failures in `onFailure`:

```kotlin
class AccountRepositoryImpl(
    private val authService: AuthService,
    private val database: AppDatabase,
    private val crashReporter: CrashReporter,
) : AccountRepository {

    override suspend fun callApi(): ApiResult<List<AccountEntity>> {
        return object : ApiCall<List<AccountEntity>>() {
            override suspend fun onFailure(error: ApiResult.Error) {
                crashReporter.recordApiError(error, "AccountRepository.callApi")
            }
        }.await()
    }
}
```

### API Error Reporting Extension

```kotlin
fun CrashReporter.recordApiError(error: ApiResult.Error, context: String = "") {
    val throwable = error.exception ?: RuntimeException(error.message ?: "Unknown API error")
    val msg = buildString {
        append("API Error")
        if (context.isNotEmpty()) append(" [$context]")
        if (error is ApiResult.Error.HttpError) append(" HTTP ${error.status.value}")
        error.message?.let { append(": $it") }
    }
    logBreadcrumb(msg)
    recordException(throwable, msg)
}
```

Do not call in a tight loop for batch failures — Crashlytics queues exceptions for next-launch upload and drops excess entries. Log one exception with a count instead.

## ViewModel Screen Tracking

```kotlin
class AccountViewModel(
    private val accountRepository: AccountRepository,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {
    init {
        analyticsTracker.trackScreenView("AccountScreen", "AccountViewModel")
    }
}
```

## Compose Screen Tracking

```kotlin
@Composable
fun TrackScreenView(
    screenName: String,
    screenClass: String? = null,
    analyticsTracker: AnalyticsTracker = koinInject(),
) {
    DisposableEffect(screenName) {
        analyticsTracker.trackScreenView(screenName, screenClass)
        onDispose { }
    }
}
```

Use `DisposableEffect`, not `LaunchedEffect` — `LaunchedEffect` fires only on initial composition and cannot detect screen exit for accurate screen-time tracking.

## Session Identity

```kotlin
class SessionObserver(
    private val analyticsTracker: AnalyticsTracker,
    private val crashReporter: CrashReporter,
) {
    fun onLogin(userId: String) {
        analyticsTracker.setUserId(userId)
        crashReporter.setUserId(userId)
        crashReporter.logBreadcrumb("User logged in: $userId")
    }

    fun onLogout() {
        crashReporter.logBreadcrumb("User logged out")
        analyticsTracker.setUserId(null)
        crashReporter.setUserId(null)
    }
}
```

## GDPR Consent

```kotlin
fun CrashReporter.initializeWithConsent(hasConsent: Boolean, userId: String? = null) {
    setCrashlyticsCollectionEnabled(hasConsent)
    if (hasConsent) {
        userId?.let { setUserId(it) }
        logBreadcrumb("Crash reporting enabled with consent")
    }
}
```

## Crash Context Utilities

```kotlin
fun CrashReporter.logNavigation(from: String, to: String) =
    logBreadcrumb("Navigate: $from -> $to")

inline fun <T> CrashReporter.withCrashContext(operationName: String, block: () -> T): T {
    logBreadcrumb("Start: $operationName")
    return try {
        block().also { logBreadcrumb("Success: $operationName") }
    } catch (e: Exception) {
        logBreadcrumb("Failed: $operationName - ${e.message}")
        recordException(e, "Failed during: $operationName")
        throw e
    }
}

fun CrashReporter.setContext(vararg pairs: Pair<String, String>) {
    pairs.forEach { (key, value) -> setCustomKey(key, value) }
}
```
