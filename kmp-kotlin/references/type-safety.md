# Type Safety

## Sealed Interface over Sealed Class

Use `sealed interface` for state, result, and error hierarchies. Sealed interfaces allow subtypes to extend other classes and implement multiple sealed interfaces. Use `sealed class` only when subtypes need shared mutable state.

```kotlin
// Right -- sealed interface with generic variance
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String, val cause: Throwable? = null) : UiState<Nothing>
}
```

### Nested sealed hierarchies

```kotlin
sealed interface PaymentResult {
    data class Completed(val txId: String) : PaymentResult
    sealed interface Failed : PaymentResult {
        data class Declined(val reason: String) : Failed
        data class NetworkError(val cause: Throwable) : Failed
        data object Timeout : Failed
    }
    data object Cancelled : PaymentResult
}

fun handle(r: PaymentResult): String = when (r) {
    is PaymentResult.Completed -> "Paid: ${r.txId}"
    is PaymentResult.Failed.Declined -> "Declined: ${r.reason}"
    is PaymentResult.Failed.NetworkError -> "Network error"
    is PaymentResult.Failed.Timeout -> "Timed out"
    is PaymentResult.Cancelled -> "Cancelled"
}
```

## Value Class for Type-Safe Wrappers

Use `@JvmInline value class` to wrap primitives representing domain concepts. Zero runtime cost in non-generic contexts.

```kotlin
@JvmInline value class UserId(val value: String)
@JvmInline value class OrderId(val value: Long)
@JvmInline value class Amount(val cents: Long) {
    init { require(cents >= 0) { "Negative: $cents" } }
    fun toDollars(): Double = cents / 100.0
}

fun fetchUser(userId: UserId): User      // compiler prevents mixing UserId/OrderId
fun charge(userId: UserId, amount: Amount): PaymentResult
```

When NOT to use: generic arguments in hot paths (`List<UserId>` boxes), multiple properties needed, inheritance needed.

## Exhaustive `when` Expressions

Never add `else` to sealed/enum `when` -- it defeats compile-time exhaustiveness when new subtypes are added.

```kotlin
// Right -- compiler catches new subtypes   // Wrong -- else hides missing branches
fun render(s: UiState<Data>) = when (s) {   fun render(s: UiState<Data>) = when (s) {
    is UiState.Loading -> showLoading()          is UiState.Loading -> showLoading()
    is UiState.Success -> showData(s.data)       else -> showError("Unknown")
    is UiState.Error -> showError(s.message) }
}
```

## Enum vs Sealed Interface

| Use enum when... | Use sealed interface when... |
|-----------------|----------------------------|
| Fixed set of simple constants | Subtypes carry different data |
| All entries share same properties | Subtypes extend other classes |
| Need `values()`, `valueOf()`, `entries` | Need hierarchical nesting |

## Type Aliases

Use for complex generic signatures. Never as a substitute for value classes.

```kotlin
typealias EventHandler = (Event) -> Unit        // Right -- simplifies signature
typealias UserCache = Map<UserId, UserProfile>
typealias UserId = String                        // Wrong -- no type safety, use value class
```

## Generic Constraints

```kotlin
fun <T> sort(items: List<T>): List<T> where T : Comparable<T>, T : Serializable = items.sorted()
fun <T : Comparable<T>> max(a: T, b: T): T = if (a >= b) a else b  // single bound, no where
```

## Variance: `in`, `out`, Star Projection

`out` (covariant) for producers. `in` (contravariant) for consumers. `*` when type argument is irrelevant.

```kotlin
interface Source<out E : Event> { fun next(): E }       // produces E
interface Sink<in E : Event> { fun send(event: E) }     // consumes E
fun logAll(sources: List<Source<*>>) { sources.forEach { println(it.next()) } }
```

## Reified Type Parameters

Use `reified` with `inline fun` to access actual type at runtime. Avoids passing `KClass`.

```kotlin
inline fun <reified T> parse(json: String): T = Json.decodeFromString<T>(json)
inline fun <reified T : Any> createLogger(): Logger = LoggerFactory.getLogger(T::class.java)
inline fun <reified T> isInstance(value: Any): Boolean = value is T
```

Limitations: only with `inline` functions, not on class-level type params, cannot call `T()`.

## Nothing Type

Return type for functions that never return normally. Enables compiler unreachability analysis.

```kotlin
fun fail(msg: String): Nothing = throw IllegalStateException(msg)

fun processOrFail(data: Data?): Processed {
    val d = data ?: fail("Data required")  // compiler knows fail() never returns
    return d.process()
}
```

## Data Class vs Regular Class

`data class` for value semantics (auto `equals`/`hashCode`/`copy`/`toString`). Regular class for identity semantics or complex behavior.

```kotlin
data class Coordinates(val lat: Double, val lng: Double)                    // value
class UserSession(val token: AccessToken, private val expiresAt: Instant) { // identity
    fun isExpired(): Boolean = Clock.System.now() > expiresAt
}
```
