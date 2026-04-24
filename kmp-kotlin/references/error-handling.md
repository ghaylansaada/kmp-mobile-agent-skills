# Error Handling

> **Note:** For coroutine-specific error handling (`CancellationException`, `suspendRunCatching`, `CoroutineExceptionHandler`), see the **kmp-kotlin-coroutines** skill. This file covers Kotlin language-level error handling only.

## `Result` Type -- Chaining and Handling

```kotlin
fun parseConfig(json: String): Result<AppConfig> = runCatching { Json.decodeFromString<AppConfig>(json) }

val userName = fetchUser(userId)
    .map { it.displayName }
    .getOrDefault("Unknown User")

val processed = fetchData()
    .mapCatching { transform(it) }
    .recover { fallbackData() }
    .getOrThrow()

fetchUser(userId)
    .onSuccess { logger.info("Fetched: ${it.name}") }
    .onFailure { logger.error("Fetch failed", it) }
    .getOrElse { User.anonymous() }
```

### When NOT to use `Result`

- Different error cases carry different data -- use sealed interface
- Caller must handle each error type differently -- use sealed interface
- Error types are part of the public API contract

## `require` / `check` / `error` Preconditions

| Function | Validates | Throws | Use when... |
|----------|----------|--------|------------|
| `require(condition)` | Arguments | `IllegalArgumentException` | Caller passed bad input |
| `requireNotNull(value)` | Arg nullability | `IllegalArgumentException` | Null input is caller bug |
| `check(condition)` | State | `IllegalStateException` | Wrong lifecycle state |
| `checkNotNull(value)` | State nullability | `IllegalStateException` | Required state not init |
| `error(message)` | Unreachable | `IllegalStateException` | Code path should never run |

```kotlin
fun withdraw(account: Account, amount: Money) {
    require(amount.cents > 0) { "Amount must be positive: $amount" }
    check(account.balance >= amount) { "Insufficient: ${account.balance} < $amount" }
    account.balance -= amount
}
```

## Exception Hierarchy for Domain Errors

Keep the hierarchy shallow. Use sealed class for exhaustive catching.

```kotlin
sealed class DomainException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NotFound(val resourceType: String, val id: String) : DomainException("$resourceType not found: $id")
    class Unauthorized(message: String = "Auth required") : DomainException(message)
    class ValidationFailed(val errors: List<FieldError>) : DomainException("${errors.size} error(s)")
}

try {
    orderService.placeOrder(order)
} catch (e: DomainException.NotFound) {
    showError("Item unavailable: ${e.id}")
} catch (e: DomainException.ValidationFailed) {
    showFieldErrors(e.errors)
}
```

## Sealed Interface for Typed Results

When different error cases carry different data, prefer a sealed interface over exceptions.

```kotlin
sealed interface Outcome<out T> {
    data class Success<T>(val data: T) : Outcome<T>
    data class Failure(val error: AppError) : Outcome<Nothing>
}

sealed interface AppError {
    data class Network(val cause: Throwable) : AppError
    data class Validation(val fields: List<FieldError>) : AppError
    data class NotFound(val resourceId: String) : AppError
    data object Unauthorized : AppError
}

fun <T> handle(outcome: Outcome<T>) = when (outcome) {
    is Outcome.Success -> showData(outcome.data)
    is Outcome.Failure -> when (outcome.error) {
        is AppError.Network -> showRetry()
        is AppError.Validation -> showFieldErrors(outcome.error.fields)
        is AppError.NotFound -> showNotFound(outcome.error.resourceId)
        is AppError.Unauthorized -> navigateToLogin()
    }
}
```

## Never Catch Broadly

Be specific. Catching `Exception`/`Throwable` swallows bugs, OOM, and cancellation.

```kotlin
// Wrong
try { process() } catch (e: Exception) { logger.error("Failed", e) }

// Right -- specific types
try { process() }
catch (e: IOException) { logger.error("I/O", e) }
catch (e: JsonException) { logger.error("Parse", e) }

// If you must catch broadly (top-level boundary), rethrow critical exceptions
try { process() }
catch (e: CancellationException) { throw e }
catch (e: Throwable) { logger.error("Unexpected", e); reportCrash(e) }
```

## `use {}` for Closeable Resources

`use` guarantees `close()` on success or failure. Always prefer over manual try-finally.

```kotlin
fun readConfig(path: Path): String = path.bufferedReader().use { it.readText() }

// Nested resources
fun copy(src: Path, dst: Path) {
    src.inputStream().use { input -> dst.outputStream().use { output -> input.copyTo(output) } }
}
```

## try-catch-finally

Use `finally` for cleanup that must run regardless. Prefer `use {}` for `Closeable`.

```kotlin
val conn = openConnection()
try { conn.execute(query) }
catch (e: SqlException) { throw RepositoryException("Query failed", e) }
finally { conn.close() }
```
