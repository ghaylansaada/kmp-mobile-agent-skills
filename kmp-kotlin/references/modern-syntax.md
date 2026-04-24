# Modern Syntax and Language Features

## Guard Conditions in `when` (Stable Since Kotlin 2.2)

Guard conditions add an `if` clause to `when` branches for filtering without nesting.

```kotlin
fun classify(value: Any) = when (value) {
    is String if value.length > 10 -> "long string"
    is String -> "short string"
    is Int if value > 0 -> "positive int"
    is Int -> "non-positive int"
    else -> "other"
}
```

Guard conditions use `if`, not `&&`. The `if` keyword is required syntax.

## Non-Local Break and Continue (Stable Since Kotlin 2.2)

`break` and `continue` now work inside inline lambda blocks (`forEach`, `map`, etc.).

```kotlin
for (batch in items.chunked(10)) {
    batch.forEach { item ->
        if (item.isPoison) break      // breaks the outer for loop
        if (item.isSkippable) continue // continues the outer for loop
        process(item)
    }
}
```

Only works with inline lambdas. Non-inline lambdas (e.g., `Sequence.forEach`) still reject `break`/`continue`.

## Multi-Dollar String Interpolation (Stable Since Kotlin 2.2)

Use `$$` prefix for strings containing literal `$` characters. Variables use `$$var`.

```kotlin
val jsonTemplate = $$"""{"name": "$${user.name}", "price": "$19.99"}"""
// Without multi-dollar, literal $ requires: "${'$'}19.99"
```

The dollar count in the prefix must match variable references: `$$"..$$var.."`.

## Context Parameters (Stable in Kotlin 2.4)

Inject dependencies through the call chain without explicit passing.

```kotlin
context(logger: Logger)
fun processOrder(order: Order) {
    logger.info("Processing ${order.id}")
    validateOrder(order)  // logger is implicitly forwarded
}

context(logger: Logger)
fun validateOrder(order: Order) {
    logger.debug("Validating ${order.items.size} items")
}
```

Two context parameters of the same type cause ambiguity. Use value classes to differentiate.

## Name-Based Destructuring (Preview in Kotlin 2.3.20)

Destructure by property name instead of position, preventing reorder bugs.

```kotlin
data class User(val name: String, val email: String, val age: Int)
val (val name = name, val mail = email) = user  // order-independent
```

Cannot mix positional and name-based destructuring in the same declaration.

## Explicit Backing Fields (Since Kotlin 2.3.0)

Declare a backing field with a different type than the public property.

```kotlin
class TaskRepository {
    val tasks: List<Task>
        field = mutableListOf()
    fun addTask(task: Task) { field.add(task) }
}
// Replaces the _tasks / tasks pattern
```

## Scope Functions

| Function | Ref | Returns | Use when... |
|----------|-----|---------|------------|
| `let` | `it` | Lambda result | Null-safe transform |
| `run` | `this` | Lambda result | Configure + compute |
| `apply` | `this` | Object | Builder configuration |
| `also` | `it` | Object | Side effects (logging) |
| `with` | `this` | Lambda result | Grouping calls |

```kotlin
val displayName = user?.let { "${it.firstName} ${it.lastName}" }
val request = HttpRequest().apply { url = endpoint; method = GET; timeout = 30.seconds }
fun createUser(name: String) = User(name).also { logger.info("Created: ${it.id}") }
```

Never nest scope functions beyond one level. Use named variables instead.

## Expression Body Functions

Use `=` for single-expression functions. Block body for multiple statements.

```kotlin
fun fullName(): String = "$firstName $lastName"
fun isActive(): Boolean = status == Status.ACTIVE && !isExpired()
```

## Extension Functions

**Member** when core to the type's API or accesses private state. **Extension** when adding utility to types you don't own or context-specific helpers.

```kotlin
fun String.toSlug(): String = lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
val String.wordCount: Int get() = split(Regex("\\s+")).size
fun User.Companion.fromCsv(line: String): User { val (n, e) = line.split(","); return User(n.trim(), e.trim()) }
fun <T : Comparable<T>> List<T>.isSorted(): Boolean = zipWithNext().all { (a, b) -> a <= b }
```

Extensions are resolved statically -- no polymorphic dispatch. Use member functions for polymorphism.

## Inline Functions

Use `inline` for functions with lambda parameters to eliminate allocation. `noinline` for stored lambdas. `crossinline` for lambdas called from a different execution context. Do not inline functions without lambda parameters.

```kotlin
inline fun <T> measureTime(block: () -> T): Pair<T, Duration> {
    val start = TimeSource.Monotonic.markNow(); return block() to start.elapsedNow()
}
```

## Reified Type Parameters

Use `reified` with `inline fun` to retain type information at runtime.

```kotlin
inline fun <reified T> parse(json: String): T = Json.decodeFromString<T>(json)
inline fun <reified T : Any> createLogger(): Logger = LoggerFactory.getLogger(T::class.java)
inline fun <reified T> isInstance(value: Any): Boolean = value is T
```

Only works with `inline` functions. Cannot be used on class-level type parameters.

## Collections

Immutable by default in public APIs. Use `buildList`/`buildMap`/`buildSet` for conditional construction. Use `Sequence` for multi-step chains on large collections. Use destructuring for maps: `for ((key, value) in map)`.

```kotlin
val features = buildList { add("core"); add("auth"); if (isPremium) add("premium") }
val result = largeList.asSequence().filter { it.isActive }.map { it.name }.take(10).toList()
```

## Delegation Patterns

```kotlin
val config: Config by lazy { loadConfig() }                           // thread-safe lazy
val uiConfig by lazy(LazyThreadSafetyMode.NONE) { buildUiConfig() }  // skip sync on UI thread
var status by Delegates.observable(Status.IDLE) { _, old, new -> log("$old -> $new") }
var score by Delegates.vetoable(0) { _, _, new -> new in 0..100 }     // reject invalid
```

Interface delegation: `class LoggingList<T>(private val inner: MutableList<T>) : MutableList<T> by inner`. Custom delegates implement `ReadWriteProperty<Any?, T>` or `ReadOnlyProperty<Any?, T>`.

## DSL Builders

Use `@DslMarker` to prevent scope leaking in nested DSL blocks. Combine with receiver lambdas for type-safe builders.

```kotlin
@DslMarker annotation class HtmlDsl
@HtmlDsl class HtmlBuilder { /* ... */ }
fun html(block: HtmlBuilder.() -> Unit): String = HtmlBuilder().apply(block).build()
```

## Operator Overloading

Only overload when semantics are obvious for the domain.

```kotlin
data class Vector(val x: Double, val y: Double) {
    operator fun plus(other: Vector) = Vector(x + other.x, y + other.y)
    operator fun times(scalar: Double) = Vector(x * scalar, y * scalar)
}
```

## `@Deprecated` with `ReplaceWith`

```kotlin
@Deprecated("Use fetchUserProfile(UserId)", ReplaceWith("fetchUserProfile(UserId(userId))"), WARNING)
suspend fun getUser(userId: String): User = fetchUserProfile(UserId(userId))
```
