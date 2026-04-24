# Anti-Patterns Checklist

Review code for these Kotlin anti-patterns. Each shows the wrong pattern and the correct alternative.

## 1. `var` Where `val` Works

```kotlin
// Wrong                                    // Right
var name = "${user.first} ${user.last}"     val name = "${user.first} ${user.last}"
```

## 2. Mutable Collections Exposed Publicly

```kotlin
// Wrong -- callers can mutate internal state
class Cache { val users: MutableList<User> = mutableListOf() }

// Right -- immutable public API
class Cache {
    private val _users = mutableListOf<User>()
    val users: List<User> get() = _users.toList()
}
```

## 3. `Any` as a Parameter Type

```kotlin
// Wrong -- ClassCastException at runtime
fun process(data: Any) { val user = data as User }

// Right -- specific type or bounded generic
fun process(data: User) { /* ... */ }
fun <T : Processable> process(data: T) { /* ... */ }
```

## 4. `lateinit` Where Nullable or Lazy Works Better

`lateinit` bypasses null safety and crashes with `UninitializedPropertyAccessException`.

```kotlin
// Wrong
class Screen { lateinit var viewModel: VM }

// Right -- constructor injection, lazy, or nullable
class Screen(private val viewModel: VM)
class Screen { private val viewModel by lazy { createVM() } }
class Screen { private var viewModel: VM? = null }
```

## 5. Raw String Comparisons Instead of Enums

```kotlin
// Wrong -- typo-prone, not refactor-safe
if (theme == "dark") { /* "Dark" silently falls through */ }

// Right
enum class Theme { DARK, LIGHT, SYSTEM }
fun apply(theme: Theme) = when (theme) { Theme.DARK -> dark(); Theme.LIGHT -> light(); Theme.SYSTEM -> system() }
```

## 6. Wildcard Imports

```kotlin
// Wrong -- hides symbol origins, causes silent conflicts
import kotlinx.coroutines.flow.*

// Right
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
```

## 7. `else` on Sealed/Enum `when`

Adding `else` hides missing branches when new subtypes are added.

```kotlin
// Wrong                                          // Right -- exhaustive
fun handle(s: UiState) = when (s) {              fun handle(s: UiState) = when (s) {
    is UiState.Loading -> showLoading()               is UiState.Loading -> showLoading()
    else -> showDefault()                              is UiState.Success -> showData(s.data)
}                                                      is UiState.Error -> showError(s.message)
                                                  }
```

## 8. Catching `Exception` or `Throwable` Broadly

Swallows bugs, OOM, and `CancellationException`.

```kotlin
// Wrong                                    // Right -- catch specific types
try { process() }                           try { process() }
catch (e: Exception) { log(e) }            catch (e: IOException) { log("I/O", e) }
                                            catch (e: JsonException) { log("Parse", e) }
```

## 9. Hardcoded Strings and Magic Numbers

```kotlin
// Wrong                                    // Right
if (retryCount > 3) { delay(5000) }        const val MAX_RETRIES = 3
                                            val RETRY_DELAY = 5.seconds
                                            if (retryCount > MAX_RETRIES) { delay(RETRY_DELAY) }
```

## 10. `!!` Without Justification

```kotlin
// Wrong                                    // Right
val name = user!!.name                      val name = user?.name ?: "Unknown"
                                            val name = requireNotNull(user) { "Required after auth" }.name
```

## 11. Mutable Default Parameters

Default parameters referencing mutable objects share state across callers.

```kotlin
// Wrong -- shared mutable list across calls
fun addItem(item: Item, items: MutableList<Item> = mutableListOf()) { items.add(item) }

// Right -- immutable default, return new list
fun addItem(item: Item, items: List<Item> = emptyList()): List<Item> = items + item
```

## 12. `object` Singletons With Mutable State

Global mutable state is hard to test and thread-unsafe.

```kotlin
// Wrong                                    // Right -- injectable state holder
object AppState {                           class AppState(
    var currentUser: User? = null               private val _user: MutableStateFlow<User?> = MutableStateFlow(null),
}                                           ) {
                                                val currentUser: StateFlow<User?> = _user.asStateFlow()
                                            }
```

## 13. String Concatenation in Loops

`+` in loops creates O(n^2) allocations.

```kotlin
// Wrong                                    // Right
var r = ""                                  val r = items.joinToString(", ") { it.name }
for (i in items) { r += "${i.name}, " }
```

## Quick Scan Checklist

- [ ] `var` that could be `val`
- [ ] `MutableList`/`MutableMap`/`MutableStateFlow` in public API
- [ ] Parameter typed as `Any`
- [ ] `lateinit` property
- [ ] String comparison instead of enum/sealed type
- [ ] Wildcard import (`import x.y.*`)
- [ ] `else` on sealed/enum `when`
- [ ] `!!` without justification comment
- [ ] Discarded `Result` or fallible return value
- [ ] String concatenation with `+` instead of templates
- [ ] Broad `catch (e: Exception)` or `catch (e: Throwable)`
- [ ] Magic numbers or hardcoded strings
- [ ] `object` singleton with mutable `var`
- [ ] String `+` concatenation in loops

> For coroutine anti-patterns (`GlobalScope`, nested launch, catching `CancellationException`, `SharedFlow(replay=0)` for events), see the **kmp-kotlin-coroutines** skill.
