# Coroutine Fundamentals

> **Note:** Version numbers are intentionally omitted. Always use the latest stable release from the official documentation.

## CoroutineContext

An indexed set of `Element` instances keyed by unique `Key` -- an immutable map where each key type appears at most once.

| Key | Element | Purpose |
|-----|---------|---------|
| `Job` | `Job`, `SupervisorJob` | Lifecycle and parent-child relationship |
| `ContinuationInterceptor` | `CoroutineDispatcher` | Thread/queue scheduling |
| `CoroutineName` | `CoroutineName` | Debug label (appears in thread names) |
| `CoroutineExceptionHandler` | `CoroutineExceptionHandler` | Uncaught exception callback |

### Composition with `+`

Right-side elements override left-side for the same key:

```kotlin
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

val context = SupervisorJob() + Dispatchers.Main + CoroutineName("ui-scope")
// Result: SupervisorJob, Main dispatcher, name = "ui-scope"

val overridden = context + Dispatchers.Default
// Result: SupervisorJob, Default dispatcher, name = "ui-scope"
```

### Context Inheritance

Effective child context = `parent context + child arguments`. The child inherits all parent elements, overridden by anything passed to the builder. The child always gets a **new `Job`** whose parent is the parent's `Job`.

## Dispatchers

| Dispatcher | Thread Pool | Use Case | KMP Notes |
|------------|------------|----------|-----------|
| `Dispatchers.Main` | Platform UI thread | UI updates, StateFlow emission | Android main thread / iOS `dispatch_get_main_queue` |
| `Dispatchers.Default` | Shared CPU pool | CPU-intensive computation, sorting, parsing | Kotlin/Native may schedule on main thread via GCD |
| `Dispatchers.IO` | Elastic I/O pool | Network, disk, database | Requires `import kotlinx.coroutines.IO` on non-JVM |
| `Dispatchers.Unconfined` | Caller's thread (initially) | Almost never -- resumes in whichever thread completes the suspension | Breaks assumptions about thread confinement |

Use `withContext` to switch dispatchers -- do NOT launch a new coroutine just to switch. See the [withContext](#withcontext) section.

## Job and SupervisorJob

### Job Lifecycle

```
New --> Active --> Completing --> Completed
                      |
                  Cancelling --> Cancelled
```

| Property | New | Active | Completing | Cancelling | Completed | Cancelled |
|----------|-----|--------|------------|------------|-----------|-----------|
| `isActive` | false | **true** | true | false | false | false |
| `isCancelled` | false | false | false | **true** | false | **true** |
| `isCompleted` | false | false | false | false | **true** | **true** |

### Parent-Child Relationship (Structured Concurrency Tree)

- Cancelling a parent cancels **all** children recursively.
- A child failure cancels the parent (then all siblings) -- unless the parent is a `SupervisorJob`.
- A parent waits for all children to complete before completing itself.

Key operations: `job.cancel()`, `job.join()` (suspend until terminal), `job.invokeOnCompletion { cause -> }` (callback on terminal state).

### SupervisorJob

Child failure does **not** propagate up. Parent cancellation propagates **down**:

```kotlin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

// File: commonMain/kotlin/{your.package}/di/AppModule.kt

val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

fun startIndependentTasks() {
    appScope.launch { syncContacts() }   // failure here does NOT cancel downloads
    appScope.launch { syncDownloads() }  // failure here does NOT cancel contacts
}

fun shutdown() {
    appScope.cancel()  // cancels ALL children
}
```

Use `SupervisorJob` for long-lived scopes (app scope, ViewModel scope) where independent work must not cancel each other.

## CoroutineScope

A holder of `CoroutineContext` (always contains a `Job`) that defines the lifecycle boundary for coroutines.

### Built-in Scopes

| Scope | Lifecycle | Provided by |
|-------|-----------|-------------|
| `viewModelScope` | ViewModel cleared | AndroidX Lifecycle / KMP ViewModel |
| `lifecycleScope` | Activity/Fragment destroyed | AndroidX Lifecycle |
| `rememberCoroutineScope()` | Composition leaves | Compose |

### Custom Scopes

```kotlin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class SessionManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun close() { scope.cancel() }
}
```

### Why GlobalScope Is Never Acceptable

No parent `Job`, no lifecycle, no cancellation propagation. Coroutines leak on configuration changes, ViewModel clearing, and scope teardown. Use a properly scoped `CoroutineScope` with `SupervisorJob` instead.

### `coroutineScope {}` Function

The `coroutineScope` **function** (not the interface) creates a child scope, waits for all children to complete, and propagates any child failure:

```kotlin
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

suspend fun fetchDashboard(): Dashboard = coroutineScope {
    val user = async { userRepo.getUser() }
    val stats = async { statsRepo.getStats() }
    Dashboard(user.await(), stats.await())
    // If either fails, both are cancelled and the exception propagates
}
```

## withContext

Switches dispatcher (or other context elements) **without** launching a new coroutine. Returns the result of the block.

```kotlin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

// File: commonMain/kotlin/{your.package}/data/repository/UserRepository.kt

suspend fun readFile(path: String): ByteArray = withContext(Dispatchers.IO) { fileSystem.read(path) }
suspend fun parseJson(raw: String): Config = withContext(Dispatchers.Default) { json.decodeFromString(raw) }
suspend fun safeCleanup() = withContext(NonCancellable) { database.close() }  // completes even if cancelled
```

Anti-pattern -- `async(Dispatchers.IO) { loadData() }.await()`. Use `withContext(Dispatchers.IO) { loadData() }` instead.

## coroutineScope vs supervisorScope

| Behavior | `coroutineScope` | `supervisorScope` |
|----------|------------------|-------------------|
| Child failure | Cancels **all** siblings (all-or-nothing) | **Independent** -- other children continue |
| Waits for children | Yes | Yes |
| Exception propagation | First child failure propagates to caller | Each child's failure must be handled individually |
| Use case | Parallel decomposition where partial results are useless | Independent tasks where some may fail |

### coroutineScope Example

```kotlin
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

// If either call fails, both cancel and the exception propagates
suspend fun transferFunds(from: Account, to: Account, amount: Long) = coroutineScope {
    val debit = async { accountService.debit(from, amount) }
    val credit = async { accountService.credit(to, amount) }
    debit.await()
    credit.await()
}
```

### supervisorScope Example

```kotlin
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

// Each sync is independent -- one failing does not cancel others
suspend fun syncAll() = supervisorScope {
    launch { syncContacts() }  // may fail independently
    launch { syncCalendar() }  // continues even if contacts fails
    launch { syncPhotos() }    // continues even if calendar fails
}
```

## CoroutineExceptionHandler

Receives uncaught exceptions from **root** coroutines launched with `launch`. Does NOT catch `async` exceptions (those surface at `.await()`).

```kotlin
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

val handler = CoroutineExceptionHandler { _, throwable ->
    logger.error("Uncaught coroutine exception", throwable)
}

val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main + handler)
```

Key rules:
- Install on the **root scope**, not on child coroutines (ignored on children).
- Does NOT prevent cancellation -- runs **after** the coroutine has already failed.
- Pair with `SupervisorJob` so one failure does not tear down the entire scope.

## Cooperative Cancellation

Cancellation is **cooperative** -- a coroutine must check for it; it is not forcefully stopped.

```kotlin
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.yield

suspend fun processItems(items: List<Item>) = coroutineScope {
    for (item in items) {
        ensureActive()  // throws CancellationException if cancelled
        heavyComputation(item)
    }
}
```

| Method | Throws | Yields to others | Use when |
|--------|--------|-------------------|----------|
| `isActive` | No | No | You need custom cleanup before exiting |
| `ensureActive()` | Yes (`CancellationException`) | No | Default choice in loops |
| `yield()` | Yes (`CancellationException`) | Yes | CPU-bound tight loops (fairness) |

### CancellationException Rules

1. **Never catch and swallow it.** `CancellationException` signals structured cancellation. Swallowing it breaks the entire parent-child contract.
2. **`runCatching` catches it silently.** Use a `suspendRunCatching` wrapper:

```kotlin
import kotlin.coroutines.cancellation.CancellationException

inline fun <T> suspendRunCatching(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

3. **`delay()` and all standard suspend functions are cancellation points.** They throw `CancellationException` when the coroutine is cancelled.

### NonCancellable Cleanup

Suspend calls inside `finally` of a cancelled coroutine are immediately cancelled. Wrap must-complete cleanup with `withContext(NonCancellable) { ... }` -- see the [withContext](#withcontext) section.

## async and Deferred

`async` launches a coroutine that returns a `Deferred<T>` (a `Job` with a result). Use it for parallel decomposition.

### Parallel Decomposition

```kotlin
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

suspend fun loadScreen(): ScreenData = coroutineScope {
    val user = async { userRepo.get() }
    val feed = async { feedRepo.get() }
    val notifications = async { notificationRepo.get() }

    val (u, f, n) = awaitAll(user, feed, notifications)
    ScreenData(u, f, n)
}
```

### Error Handling

| Builder | Exception behavior |
|---------|-------------------|
| `launch` | Thrown immediately, propagates to parent or `CoroutineExceptionHandler` |
| `async` | Stored in `Deferred`, thrown when `.await()` is called |

Inside `coroutineScope`, a failing `async` still cancels siblings because `coroutineScope` enforces all-or-nothing.

Always use `async` inside `coroutineScope` (structured). Avoid `scope.async { ... }` from outside -- it creates unstructured work the caller cannot wait for or cancel.

## Suspend Function Conventions

| Return type | Semantics | Naming |
|-------------|-----------|--------|
| `suspend fun getUser(): User` | One-shot, may throw | Action verb, no "suspend" prefix |
| `fun observeUsers(): Flow<List<User>>` | Stream of values | "observe", "watch", or noun form |
| `fun getUserAsync(): Deferred<User>` | Concurrent result handle | "Async" suffix on Deferred-returning functions |

### Convention Rules

- Name describes the **action**, not the mechanism. Do not prefix with "async" or "suspend".
- `suspend fun` = one-shot operation. `Flow<T>` = reactive stream. `Channel` = hot communication.
- Suspend functions throw exceptions. Flow-based APIs prefer sealed result types for expected errors.

### callbackFlow for Callback APIs

```kotlin
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

// File: commonMain/kotlin/{your.package}/data/source/LocationSource.kt

fun observeLocation(): Flow<Location> = callbackFlow {
    val listener = LocationListener { location ->
        trySend(location)
    }
    locationProvider.register(listener)
    awaitClose { locationProvider.unregister(listener) }
}
```

`awaitClose` is **mandatory** -- it keeps the Flow alive until the collector cancels, then runs the cleanup block.
