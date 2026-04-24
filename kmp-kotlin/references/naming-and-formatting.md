# Naming and Formatting Conventions

## KDoc

Every public class, function, and property must have KDoc. Internal/private members should have KDoc when the purpose is non-obvious.

### Format

```kotlin
/**
 * Fetches the user profile from the remote API and caches it locally.
 *
 * The profile is cached for [CacheDuration.DEFAULT] before requiring a refresh.
 * Returns null if the user account has been deactivated.
 *
 * @param userId The unique identifier for the user.
 * @param forceRefresh When true, bypasses the cache and fetches directly from the API.
 * @return The user profile, or null if the user does not exist or is deactivated.
 * @throws NetworkException If the API call fails due to connectivity issues.
 * @throws AuthenticationException If the access token has expired.
 * @see UserProfile
 * @see UserRepository.invalidateCache
 * @sample com.example.samples.fetchUserProfileSample
 */
suspend fun fetchUserProfile(userId: UserId, forceRefresh: Boolean = false): UserProfile?
```

### Tag rules

- First sentence is a summary -- concise, present tense, no "This method..." or "This function..."
- Blank line between summary paragraph and tag block
- `@param` for every parameter (skip for self-evident single-param functions like `toString`)
- `@return` when the return type is non-Unit and the meaning is not obvious from the function name
- `@throws` for every documented exception, including unchecked exceptions callers should know about
- `@see` to link related classes, functions, or external documentation
- `@sample` to reference a runnable example function in test sources
- Tags appear in order: `@param`, `@return`, `@throws`, `@see`, `@sample`

### Wrong

```kotlin
// Line comment instead of KDoc -- invisible to documentation tools
// Gets the user
fun getUser(id: String): User

/** Gets the user. */  // Missing @param, @return, @throws
fun getUser(id: String): User
```

## Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Class, Interface, Object | PascalCase | `UserRepository`, `ApiResult` |
| Function, Property | camelCase | `fetchProfile`, `userName` |
| Constant (`const val`, top-level `val`) | SCREAMING_CASE | `MAX_RETRY_COUNT`, `DEFAULT_TIMEOUT_MS` |
| Enum entry | SCREAMING_CASE | `NETWORK_ERROR`, `SUCCESS` |
| Type parameter | Single letter or PascalCase | `T`, `K`, `ResponseType` |
| Package | lowercase, dot-separated, no underscores | `com.example.app.data.repository` |
| Backing property | Prefix with underscore | `_mutableState`, `_users` |
| Test function | camelCase, descriptive (no backticks for KMP) | `fetchProfileReturnsNullWhenNotFound` |
| Boolean property/function | `is`/`has`/`should`/`can` prefix | `isActive`, `hasPermission`, `shouldRetry` |
| Factory function | PascalCase matching the return type | `fun UserProfile(name: String): UserProfile` |

### No Hungarian notation

```kotlin
// Wrong
private var mUserName: String = ""
private val sInstance: Singleton = Singleton()
private val strName: String = ""

// Right
private var userName: String = ""
private val instance: Singleton = Singleton()
private val name: String = ""
```

### Package naming

```kotlin
// Wrong -- underscores, camelCase segments
package com.example.my_app.dataLayer
package com.example.myApp.DataLayer

// Right -- all lowercase, no underscores
package com.example.myapp.data
package com.example.myapp.data.repository
```

## File Naming

- **One top-level class**: File name matches the class name in PascalCase (`UserRepository.kt`)
- **Utility/extension files without a dominant class**: lowercase with hyphens or descriptive camelCase (`StringExtensions.kt`, `DateUtils.kt`)
- **One file per top-level class**: Do not put multiple unrelated public classes in one file
- **Related extensions**: Group extensions for the same receiver type in one file (`StringExtensions.kt`)

## Formatting

### Indentation

4-space indentation everywhere. Never use tabs.

### Imports

Explicit imports only. Never use wildcard imports.

```kotlin
// Wrong
import kotlinx.coroutines.flow.*

// Right
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
```

Import ordering: stdlib, then third-party, then project -- separated by blank lines. Let the IDE manage ordering.

### Line length

120-character limit. Break after assignment operators, before dots on chained calls, and wrap parameters when the signature exceeds the limit.

```kotlin
val info = userRepository.fetchProfile(userId).toDisplayInfo(locale, timeZone)

val result = userRepository
    .fetchProfile(userId)
    .map { it.toDisplayModel(locale) }
    .getOrElse { DisplayModel.empty() }
```

### Trailing commas

Always use trailing commas on multi-line parameter lists, argument lists, enum entries, collection literals, and destructuring declarations.

```kotlin
data class UserProfile(
    val id: UserId,
    val name: String,
    val email: String,
)

enum class Priority {
    LOW,
    DEFAULT,
    HIGH,
}

val config = mapOf(
    "timeout" to 30_000,
    "retries" to 3,
)
```

### Blank lines

- One blank line between top-level declarations (functions, classes, properties)
- One blank line between the class header and the first member
- No blank line after an opening brace or before a closing brace
- No consecutive blank lines anywhere
- No blank line before the first statement in a function body

### Expression body functions

Use `=` for single-expression functions. Block body for multiple statements or complex expressions.

```kotlin
fun fullName(): String = "$firstName $lastName"
fun isAdult(age: Int): Boolean = age >= 18
fun List<User>.activeCount(): Int = count { it.isActive }
```

### Parameter ordering

1. Required parameters first, optional (defaulted) parameters last
2. Context/scope parameters before data parameters (e.g., `CoroutineScope` before `userId`)
3. Callback/lambda parameters last (enables trailing lambda syntax)

```kotlin
// Right -- required, then defaulted, then lambda last
fun fetchUsers(
    query: String,
    limit: Int = 50,
    includeInactive: Boolean = false,
    onResult: (List<User>) -> Unit,
)
```

### String templates

Use string templates instead of concatenation.

```kotlin
// Wrong
val message = "Hello, " + user.name + "! You have " + count + " items."

// Right
val message = "Hello, ${user.name}! You have $count items."
```
