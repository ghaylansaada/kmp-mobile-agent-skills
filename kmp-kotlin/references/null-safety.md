# Null Safety

## Smart Casts

The compiler tracks null checks and type checks automatically.

```kotlin
fun greet(user: User?) {
    if (user != null) {
        println("Hello, ${user.name}")  // smart-cast to User
        sendWelcomeEmail(user.email)
    }
}
```

### When smart casts fail

Smart casts do NOT work on: mutable properties (`var`), properties with custom getters, open properties, and delegated properties. The value can change between the check and usage. Assign to a local `val` first.

```kotlin
class Container {
    var item: Item? = null

    fun process() {
        // if (item != null) { item.use() }  // Compiler error -- var can change

        val current = item  // capture in local val
        if (current != null) { current.use() }
    }
}
```

## `?.let {}` vs `if (x != null)`

Use `?.let` for single-expression transforms. Use `if` for multi-statement blocks or when you need `else`.

```kotlin
val displayName = user?.let { "${it.firstName} ${it.lastName}" }  // single expression

if (user != null) {         // multi-statement with else
    updateProfile(user)
    refreshUI(user.name)
} else {
    showLoginScreen()
}
```

Avoid `?.let {} ?: run {}` -- the `run` block fires both when the receiver is null AND when `let` returns null. Use `if/else` instead.

## Safe Call Chains

```kotlin
val city = user?.address?.city?.uppercase()  // clean chain
```

## Elvis Operator for Defaults and Early Returns

```kotlin
val displayName = user?.name ?: "Guest"               // default value
fun process(user: User?) { val u = user ?: return }   // early return
fun getConfig(key: String): Config =                   // throw on null
    configMap[key] ?: throw ConfigNotFoundException(key)
```

## Avoid `!!` (Non-Null Assertion)

Every `!!` is a potential NPE. Acceptable only in tests or with a documented justification comment.

```kotlin
// Wrong                                    // Right
val name = user!!.name                      val name = user?.name ?: "Unknown"
                                            val name = requireNotNull(user) { "Required after auth" }.name
                                            val name = user?.name ?: return
```

## `requireNotNull` vs `checkNotNull`

| Function | Validates | Throws | Use when... |
|----------|----------|--------|------------|
| `requireNotNull` | Input/arguments | `IllegalArgumentException` | Null is a caller bug |
| `checkNotNull` | Internal state | `IllegalStateException` | Null means wrong lifecycle state |

```kotlin
fun process(orderId: String?) {
    val id = requireNotNull(orderId) { "orderId must not be null" }
}

class Processor {
    private var conn: Connection? = null
    fun execute() { val c = checkNotNull(conn) { "Call connect() first" }; c.run(query) }
}
```

## Platform Types from Java Interop

Java types without nullability annotations become platform types (`Type!`). The compiler does NOT enforce null checks.

```kotlin
// Java: public String getName() -- no @Nullable/@NonNull
// Kotlin sees: String! (platform type)

val name: String = javaObject.name   // Dangerous -- implicit !! if Java returns null
val name: String? = javaObject.name  // Safe -- treat as nullable at boundary
val safeName: String = javaObject.name ?: "default"
```

Rules: treat platform types as nullable unless Java has `@NonNull`. Add explicit `: Type?` annotations when storing platform values.

## Contracts

Tell the compiler about relationships between calls and effects. Use for custom null/type check functions.

```kotlin
@OptIn(ExperimentalContracts::class)
fun requireNotBlank(value: String?): String {
    contract { returns() implies (value != null) }
    require(!value.isNullOrBlank()) { "Value must not be blank" }
    return value
}
```

Contracts are `@ExperimentalContracts` as of Kotlin 2.3.20. Use in internal code where experimental APIs are acceptable.

## Null-Safe Collection Operations

Prefer specialized functions over manual null filtering.

```kotlin
val names: List<String?> = listOf("Alice", null, "Bob", null)

val nonNull: List<String> = names.filterNotNull()
val lengths: List<Int> = names.mapNotNull { it?.length }
val first: String = names.firstNotNullOf { it?.takeIf { n -> n.startsWith("A") } }

// Wrong -- manual filtering + !!
val nonNull = names.filter { it != null }.map { it!! }
```

### `firstNotNullOfOrNull` for safe search

```kotlin
val config = listOf(envConfig, fileConfig, defaultConfig)
    .firstNotNullOfOrNull { it.getValueOrNull(key) }
    ?: throw ConfigMissingException(key)
```
