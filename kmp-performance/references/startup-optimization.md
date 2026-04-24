# Startup Optimization

## StartupTracer Utility (commonMain)

Tracks and logs app startup phases. Call `markPhase` at each milestone, `finish` when fully interactive.

```kotlin
package {your.package}.core.performance

import co.touchlab.kermit.Logger
import {your.package}.core.logging.AppLogger

object StartupTracer {

    private val logger: Logger = AppLogger.withTag("Startup")
    private var startTimeMs: Long = 0L
    private val phases = mutableListOf<Pair<String, Long>>()

    fun markPhase(name: String) {
        val now = currentTimeMs()
        if (startTimeMs == 0L) startTimeMs = now
        val elapsed = now - startTimeMs
        phases.add(name to elapsed)
        logger.i { "Startup phase: $name at +${elapsed}ms" }
    }

    fun finish() {
        val total = currentTimeMs() - startTimeMs
        logger.i { "Startup complete in ${total}ms" }
        phases.forEach { (name, elapsed) ->
            logger.d { "  $name: +${elapsed}ms" }
        }
    }

    fun reset() {
        startTimeMs = 0L
        phases.clear()
    }
}

internal expect fun currentTimeMs(): Long
```

### Platform Implementations

**androidMain:**

```kotlin
package {your.package}.core.performance

internal actual fun currentTimeMs(): Long = System.currentTimeMillis()
```

**iosMain:**

```kotlin
package {your.package}.core.performance

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

internal actual fun currentTimeMs(): Long =
    (NSDate().timeIntervalSince1970 * 1000).toLong()
```

Use `timeIntervalSince1970` (Unix epoch). Do NOT use `timeIntervalSinceReferenceDate` -- that uses Apple epoch (2001-01-01), shifting timestamps by 31 years.

## Lazy Initialization for Koin

Koin `single {}` IS already lazy -- it instantiates on first `get()`, not at module load. The optimization is ensuring nothing triggers `get()` for non-critical singletons during startup.

### Eager vs Lazy Candidates

```
KEEP EAGER (needed at startup):
  - PlatformContext, Logger, DataStore<Preferences>, SessionManager

MAKE LAZY (not needed until first use):
  - ImageLoader, Room Database, FileReaderFactory/FileWriterFactory
  - Individual repositories (resolved by ViewModels on-demand)
```

### Optimized Module Loading Order

```kotlin
package {your.package}.di

import {your.package}.core.platform.PlatformContext

fun commonModules(context: PlatformContext, isDebug: Boolean) = listOf(
    // Priority 1: Logging (zero-cost, needed by everything)
    loggingModule(isDebug),
    // Priority 2: Core + Session (needed for auth check)
    coreModule(context),
    localStorageModule(),
    sessionModule(),
    // Priority 3: Network (needed for first API call)
    ktorfitModule(),
    // Priority 4: Deferred (resolved on first use)
    externalStorageModule(),
    imageLoaderModule(),
    repositoryModule(),
    viewModelModule()
)
```

Do not call `koin.get<ImageLoader>()` in `Application.onCreate()`.
