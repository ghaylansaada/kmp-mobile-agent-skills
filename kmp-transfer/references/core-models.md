# Core Models & Utilities

## TaskState Enum

**File:** `core/transfer/core/TaskState.kt`

```kotlin
package {your.package}.core.transfer.core

enum class TaskState { IDLE, RUNNING, PAUSED, CANCELED, FAILED, SUCCESS }
```

## TransferSnapshot

**File:** `core/transfer/core/TransferSnapshot.kt`

```kotlin
package {your.package}.core.transfer.core

data class TransferSnapshot(
    val bytesTransferred: Long,
    val totalBytes: Long,
    val state: TaskState,
    val speedBytesPerSec: Long = 0,
    val error: Throwable? = null,
)
```

## TaskResult

**File:** `core/transfer/core/TaskResult.kt`

```kotlin
package {your.package}.core.transfer.core

data class TaskResult(
    val state: TaskState,
    val error: Throwable? = null,
)
```

## FileChunk

**File:** `core/transfer/core/FileChunk.kt`

```kotlin
package {your.package}.core.transfer.core

data class FileChunk(
    val start: Long,
    val end: Long,
    val size: Long,
)
```

## TransferException

**File:** `core/transfer/core/TransferException.kt`

```kotlin
package {your.package}.core.transfer.core

import io.ktor.http.HttpStatusCode

class TransferException(
    val status: HttpStatusCode,
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause)
```

## retryWithBackoff

**File:** `core/transfer/core/RetryPolicy.kt`

```kotlin
package {your.package}.core.transfer.core

import kotlinx.coroutines.delay

suspend fun <T> retryWithBackoff(
    retries: Int = 5,
    initialDelayMs: Long = 500,
    factor: Double = 2.0,
    block: suspend () -> T,
): T {
    var delayMs = initialDelayMs
    repeat(retries - 1) {
        try {
            return block()
        } catch (_: Exception) {
            delay(delayMs)
            delayMs = (delayMs * factor).toLong()
        }
    }
    return block() // final attempt -- exception propagates
}
```

Backoff sequence with defaults: 500 ms, 1 s, 2 s, 4 s, then final attempt. Catches `Exception` (not `Throwable`) so that `CancellationException` propagates immediately through `delay()` and `block()`.

## SpeedEstimator

**File:** `core/transfer/util/SpeedEstimator.kt`

```kotlin
package {your.package}.core.transfer.util

import kotlin.time.Clock

class SpeedEstimator {
    private var lastBytes = 0L
    private var lastTime = 0L
    private var smoothed = 0.0

    fun update(bytes: Long): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        if (lastTime == 0L) {
            lastTime = now
            lastBytes = bytes
            return 0
        }
        val deltaB = bytes - lastBytes
        val deltaT = (now - lastTime).coerceAtLeast(1)
        val instant = deltaB * 1000 / deltaT
        smoothed = if (smoothed == 0.0) {
            instant.toDouble()
        } else {
            smoothed * 0.8 + instant * 0.2
        }
        lastBytes = bytes
        lastTime = now
        return smoothed.toLong()
    }
}
```

Smoothing: `smoothed = smoothed * 0.8 + instant * 0.2` -- favors historical speed to prevent UI jitter.
