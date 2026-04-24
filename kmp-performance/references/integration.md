# Integration: Upstream and Downstream Dependencies

## Integration with KoinInitializer

```kotlin
package {your.package}.di

import {your.package}.core.config.PlatformConfig
import {your.package}.core.performance.StartupTracer
import {your.package}.core.platform.PlatformContext
import org.koin.core.context.startKoin

fun initKoin(context: PlatformContext, config: PlatformConfig, isDebug: Boolean = false) {
    if (koinStarted) return
    StartupTracer.markPhase("koin_module_loading")
    startKoin {
        modules(commonModules(context, isDebug) + platformModule(context, config))
    }
    koinStarted = true
    StartupTracer.markPhase("koin_ready")
}

private var koinStarted: Boolean = false
```

## Integration with Compose App (First Frame Tracking)

```kotlin
package {your.package}

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import {your.package}.core.performance.StartupTracer

@Composable
fun App() {
    LaunchedEffect(Unit) {
        StartupTracer.markPhase("first_composition")
        StartupTracer.finish()
    }
    // ... existing App content ...
}
```

## Integration with AccountScreen (Recomposition Profiling)

```kotlin
package {your.package}.presentation.user

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import {your.package}.core.performance.LogRecompositions
import {your.package}.data.local.entity.AccountEntity
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AccountScreen(viewModel: AccountViewModel = koinViewModel()) {
    LogRecompositions("AccountScreen")
    val state by viewModel.accountState.collectAsStateWithLifecycle()
    // Extract to a separate composable with stable parameters
    // so it can be skipped when unrelated state changes.
    AccountContent(account = state.account, isLoading = state.isLoading)
}

@Composable
private fun AccountContent(
    account: AccountEntity?,
    isLoading: Boolean,
) {
    LogRecompositions("AccountContent")
    // ... render account data ...
}
```

## Integration with Transfer Tasks

```kotlin
package {your.package}.core.transfer

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import {your.package}.core.logging.AppLogger
import {your.package}.core.performance.currentTimeMs

class DownloadTask(
    parentScope: CoroutineScope,
    parallelism: Int = 4,
    chunkSize: Int = 256 * 1024,
) : BaseTransferTask(parentScope, parallelism, chunkSize) {

    private val logger: Logger = AppLogger.withTag("Transfer:Download")

    override suspend fun execute() {
        val startTime = currentTimeMs()
        // ... existing download logic ...
        val elapsed = currentTimeMs() - startTime
        logger.i { "Download completed in ${elapsed}ms" }
    }
}
```

## Integration with Paging

```kotlin
package {your.package}.core.performance

import co.touchlab.kermit.Logger
import {your.package}.core.logging.AppLogger

object PagingPerformanceMonitor {

    private val logger: Logger = AppLogger.withTag("PagingPerf")

    suspend fun <T> timedLoad(page: Int, block: suspend () -> T): T {
        val start = currentTimeMs()
        val result = block()
        val elapsed = currentTimeMs() - start
        logger.d { "Page $page loaded in ${elapsed}ms" }
        if (elapsed > 2000L) {
            logger.w { "Slow page load: page=$page took ${elapsed}ms" }
        }
        return result
    }
}
```

## Slow Startup Reporting (with kmp-analytics-crashlytics)

```kotlin
package {your.package}.core.performance

import {your.package}.core.analytics.AnalyticsTracker
import {your.package}.core.crash.CrashReporter

fun reportSlowStartup(
    analyticsTracker: AnalyticsTracker,
    crashReporter: CrashReporter,
    startupTimeMs: Long,
    threshold: Long = 3000L,
) {
    if (startupTimeMs > threshold) {
        analyticsTracker.trackEvent(
            "slow_startup",
            mapOf(
                "duration_ms" to startupTimeMs,
                "threshold_ms" to threshold,
            ),
        )
        crashReporter.logBreadcrumb(
            "Slow startup: ${startupTimeMs}ms (threshold: ${threshold}ms)",
        )
    }
}
```

## Performance Thresholds

| Metric | Good | Warning | Critical |
|--------|------|---------|----------|
| Cold start (Android) | <500ms | 500-1500ms | >1500ms |
| Cold start (iOS) | <400ms | 400-1200ms | >1200ms |
| Koin init time | <100ms | 100-300ms | >300ms |
| First composition | <200ms | 200-500ms | >500ms |
| Page load (API + DB) | <500ms | 500-2000ms | >2000ms |
| Image load (cached) | <50ms | 50-200ms | >200ms |
| Recompositions/frame | 1-3 | 4-10 | >10 |
