# Coroutine Performance Patterns

## Dispatcher Recommendations

```
Dispatchers.Default:
  - CPU-bound work (JSON parsing, sorting, filtering)
  - Ktor network calls (Ktor manages IO internally -- do NOT wrap in Dispatchers.IO)

Dispatchers.Main:
  - UI state updates (viewModelScope already uses Main)
  - Navigation events

Room suspend functions:
  - Room's suspend DAO methods dispatch to a background thread internally.
  - Call them from viewModelScope without an explicit withContext.
  - For Room .openHelper.writableDatabase (synchronous), use withContext(Dispatchers.Default).

Platform IO Dispatcher (not in commonMain):
  - File I/O in transfer tasks (use expect/actual)

AVOID:
  - Dispatchers.Unconfined (unpredictable thread)
  - newSingleThreadContext (wasteful)
  - GlobalScope (no structured concurrency, leaks)
  - withContext(Dispatchers.IO) { ktorCall() } -- redundant, adds overhead
```

## Transfer Task Progress Throttling

Without throttling, small chunks generate hundreds of StateFlow updates per second, causing unnecessary UI recompositions.

```kotlin
package {your.package}.core.transfer.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import {your.package}.core.performance.currentTimeMs

class ThrottledProgressUpdater(
    private val stateFlow: MutableStateFlow<TransferSnapshot>,
    private val minIntervalMs: Long = 100L,
) {
    private var lastUpdateMs: Long = 0L

    fun update(bytesTransferred: Long, speedBytesPerSec: Long) {
        val now = currentTimeMs()
        if (now - lastUpdateMs >= minIntervalMs) {
            stateFlow.update {
                it.copy(
                    bytesTransferred = bytesTransferred,
                    speedBytesPerSec = speedBytesPerSec,
                )
            }
            lastUpdateMs = now
        }
    }

    /**
     * Force-emit the final state regardless of throttle interval.
     * Without this, the UI can show stale progress (e.g., 97% instead of 100%).
     */
    fun flush(bytesTransferred: Long, speedBytesPerSec: Long) {
        stateFlow.update {
            it.copy(
                bytesTransferred = bytesTransferred,
                speedBytesPerSec = speedBytesPerSec,
            )
        }
        lastUpdateMs = currentTimeMs()
    }
}
```

## Database Migration on Startup

Room migrations run synchronously on the calling thread. If a migration runs during `onCreate`, it blocks the main thread.

```kotlin
import {your.package}.core.performance.StartupTracer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

withContext(Dispatchers.Default) {
    database.openHelper.writableDatabase
}
StartupTracer.markPhase("db_migration_done")
```
