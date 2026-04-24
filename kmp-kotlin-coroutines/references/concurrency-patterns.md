# Concurrency Patterns

## Mutex for Thread-Safe Shared State

```kotlin
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val mutex = Mutex()
private var sharedCounter = 0

suspend fun safeIncrement() = mutex.withLock { sharedCounter++ }
```

`Mutex.withLock` suspends (does not block the thread) while waiting. Never use
`synchronized` or `ReentrantLock` inside coroutines. `Mutex` is non-reentrant --
avoid nested locking.

For the real-world Mutex double-check pattern used in token refresh, see
`kmp-networking/references/http-client.md` (`HttpClientFactory.refreshTokens`).

## Channel for One-Shot UI Events

Use `Channel` (not `SharedFlow(replay=0)`) for events that must be delivered
exactly once, even if the collector briefly detaches during configuration changes.

```kotlin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

sealed interface UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent
    data object NavigateBack : UiEvent
    data class NavigateTo(val route: String) : UiEvent
}

private val _events = Channel<UiEvent>(Channel.BUFFERED)
val events: Flow<UiEvent> = _events.receiveAsFlow()

suspend fun sendEvent(event: UiEvent) { _events.send(event) }
```

`Channel.BUFFERED` provides a default buffer so `send` does not suspend under
normal conditions. `receiveAsFlow()` produces a cold Flow that consumes each
element exactly once.

## Retry with Exponential Backoff

```kotlin
import kotlinx.coroutines.delay
import kotlin.coroutines.cancellation.CancellationException

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
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            delay(delayMs)
            delayMs = (delayMs * factor).toLong()
        }
    }
    return block()
}
```

Key points:
- `CancellationException` is always rethrown -- catching it would break structured concurrency.
- Only `Exception` is caught for transient failures (not `Throwable`, which includes `OutOfMemoryError`).
- The final attempt runs outside the loop so its exception propagates directly.

## Parallel Processing with Semaphore

```kotlin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

suspend fun processChunks(
    chunks: List<Chunk>,
    semaphore: Semaphore,
    progressMutex: Mutex,
) {
    var bytesTransferred = 0L
    coroutineScope {
        chunks.forEach { chunk ->
            launch {
                semaphore.withPermit {
                    ensureActive()
                    retryWithBackoff {
                        val data = downloadRange(chunk.start, chunk.end)
                        writer.writeAt(chunk.start, data)
                        progressMutex.withLock {
                            bytesTransferred += data.size
                        }
                    }
                }
            }
        }
    }
}
```

- `coroutineScope {}` waits for all children; if any throws, all siblings cancel.
- `semaphore.withPermit {}` bounds concurrent operations.
- `ensureActive()` checks cancellation before expensive work.

For the full transfer task implementation using SupervisorJob + Semaphore + pause/cancel, see kmp-transfer.

## SupervisorJob Scope Management

```kotlin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LongLivedComponent(parentScope: CoroutineScope) {
    private val supervisorJob = SupervisorJob(parentScope.coroutineContext[Job])
    private val scope = CoroutineScope(Dispatchers.IO + supervisorJob)

    fun doWork() {
        scope.launch { /* child failure won't cancel siblings */ }
    }

    fun close() { scope.cancel() }
}
```

Cancelling `supervisorJob` cancels ALL children. Individual child failure does NOT
propagate up to siblings -- this is the key difference from regular `Job`.

## Structured Concurrency: Never Launch Inside Launch

### The WRONG Pattern

Launching new coroutines inside an existing coroutine scope breaks structured
concurrency guarantees:

```kotlin
// WRONG: nested launches break structured concurrency
viewModelScope.launch {
    callApi1()
    callApi2()
}

private fun callApi1() {
    viewModelScope.launch { // new uncontrolled coroutine
        // parent does NOT wait for this
        // cancelling parent does NOT cancel this
        // exceptions here do NOT propagate to parent
    }
}
```

### Why It Is Wrong

- Parent coroutine doesn't wait for children -- breaks sequential assumptions
- Cancelling the parent does NOT cancel the inner launches -- leaked work
- Exceptions in children don't propagate -- silent failures
- Multiple uncontrolled loading states -- flickering UI
- Execution order becomes unpredictable -- race conditions

### The RIGHT Pattern

Called functions should be `suspend` functions -- they do not launch new coroutines:

```kotlin
// RIGHT: suspend functions respect structured concurrency
viewModelScope.launch {
    showLoading(true)
    try {
        // Sequential
        val data1 = callApi1()
        val data2 = callApi2(data1.id)
        updateState(data1, data2)

        // OR parallel with async
        val result1 = async { callApi1() }
        val result2 = async { callApi2() }
        updateState(result1.await(), result2.await())
    } finally {
        showLoading(false)
    }
}

// Functions are suspend — they don't launch new coroutines
private suspend fun callApi1(): Data { /* ... */ }
private suspend fun callApi2(): Data { /* ... */ }
```

### The Rule

If you are already inside a coroutine scope, called functions should `suspend` -- not `launch`.

## StateFlow vs SharedFlow: Choosing the Right Type

### Comparison

| Aspect | StateFlow | SharedFlow |
|--------|-----------|------------|
| Purpose | Holds current state (source of truth) | Emits one-time events |
| Initial value | Required | Not required |
| Replay | Always replays latest (replay=1) | Configurable (default replay=0) |
| Equality | Drops consecutive equal values | Emits all values |
| Best for | UI state, data, loading flags | Navigation events, toasts, snackbars |

### The KMP Golden Rule

State = StateFlow. Events = Channel.

### KMP-Specific Pitfalls

- On iOS, when a SwiftUI view re-appears, SKIE creates a new AsyncSequence collection. If events are in a StateFlow, the latest event replays and triggers duplicate navigation/alerts.
- SharedFlow(replay=0) drops events when no collector is active (between screen transitions). Use Channel(Channel.BUFFERED) with receiveAsFlow() for guaranteed delivery.
- StateFlow uses equality-based dedup -- emitting the same value twice is a no-op. This catches developers who expect re-emission.

### Decision Rule

```
Does the UI need the CURRENT value on subscribe? → StateFlow
Does the UI need to react to something that HAPPENED? → Channel + receiveAsFlow()
Do multiple collectors need the same event? → SharedFlow(replay=1) with caution
```
