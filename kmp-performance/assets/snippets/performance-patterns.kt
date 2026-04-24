// =============================================================================
// Common Performance Patterns
// Skill: kmp-performance
//
// Patterns for lazy initialization, Compose stability wrappers,
// StateFlow selectors, batch updates, and Flow throttling.
// =============================================================================

package {your.package}.core.performance

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile

// ---------------------------------------------------------------------------
// Pattern 1: Suspend-safe lazy singleton
// ---------------------------------------------------------------------------

/**
 * Thread-safe lazy initialization for expensive singletons.
 * Unlike Kotlin's built-in lazy {}, this is suspend-safe.
 *
 * On Kotlin/Native there is no escape analysis -- every object
 * goes to heap. Avoid creating SuspendLazy instances in tight loops.
 */
class SuspendLazy<T>(private val initializer: suspend () -> T) {

    private val mutex = Mutex()

    @Volatile
    private var value: T? = null

    @Volatile
    private var initialized = false

    suspend fun get(): T {
        if (initialized) return value as T
        return mutex.withLock {
            if (initialized) return value as T
            val result = initializer()
            value = result
            initialized = true
            result
        }
    }
}

// Usage:
// private val expensiveResource = SuspendLazy { createExpensiveResource() }
// val resource = expensiveResource.get()

// ---------------------------------------------------------------------------
// Pattern 2: Timed initialization for Koin modules
// ---------------------------------------------------------------------------

/**
 * Wraps a Koin singleton to log initialization time.
 * Uses println because this may run before Kermit is configured.
 */
inline fun <T> timedInit(name: String, block: () -> T): T {
    val start = currentTimeMs()
    val result = block()
    val elapsed = currentTimeMs() - start
    println("[PerfInit] $name initialized in ${elapsed}ms")
    return result
}

// Usage in Koin module:
// single<HttpClient> { timedInit("HttpClient") { createHttpClient() } }

// ---------------------------------------------------------------------------
// Pattern 3: Compose stability wrapper
// ---------------------------------------------------------------------------

/**
 * Wraps an unstable type to make it Compose-stable.
 * Use when you cannot convert List/Map to ImmutableList/ImmutableMap.
 */
@androidx.compose.runtime.Stable
class StableWrapper<T>(val value: T) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StableWrapper<*>) return false
        return value == other.value
    }
    override fun hashCode(): Int = value?.hashCode() ?: 0
}

// ---------------------------------------------------------------------------
// Pattern 4: StateFlow selector (derivedStateOf for ViewModels)
// ---------------------------------------------------------------------------

/**
 * Creates a derived StateFlow that only emits when the selected value changes.
 * Prevents downstream recompositions when unrelated state fields change.
 */
fun <T, R> StateFlow<T>.selectDistinct(
    scope: CoroutineScope,
    selector: (T) -> R,
): StateFlow<R> {
    val initial = selector(value)
    val derived = MutableStateFlow(initial)
    scope.launch {
        map(selector).distinctUntilChanged().collect { derived.value = it }
    }
    return derived.asStateFlow()
}

// Usage: val isLoading = _uiState.selectDistinct(viewModelScope) { it.isLoading }

// ---------------------------------------------------------------------------
// Pattern 5: Batch state updates to reduce recompositions
// ---------------------------------------------------------------------------

/**
 * Batches multiple state changes into a single emission.
 * 3 separate .value= calls = 3 recompositions.
 * 1 batchUpdate call = 1 recomposition.
 */
inline fun <T> MutableStateFlow<T>.batchUpdate(transform: T.() -> T) {
    value = value.transform()
}

// ---------------------------------------------------------------------------
// Pattern 6: Flow throttling for progress updates
// ---------------------------------------------------------------------------

/**
 * Throttles a Flow to emit at most once per [intervalMs].
 * Always emits the final value on upstream completion so the UI
 * never shows stale progress (e.g., 97% instead of 100%).
 */
fun <T> Flow<T>.throttleLatest(intervalMs: Long): Flow<T> =
    flow {
        var lastEmitTime = 0L
        var pending: Any? = UNSET
        collect { value ->
            val now = currentTimeMs()
            if (now - lastEmitTime >= intervalMs) {
                emit(value)
                lastEmitTime = now
                pending = UNSET
            } else {
                pending = value
            }
        }
        // Flush the last value if it was throttled.
        if (pending !== UNSET) {
            @Suppress("UNCHECKED_CAST")
            emit(pending as T)
        }
    }

private val UNSET = Any()

// Usage:
// transferTask.stateFlow
//     .throttleLatest(100)  // Max 10 UI updates per second
//     .collect { snapshot -> updateUI(snapshot) }
