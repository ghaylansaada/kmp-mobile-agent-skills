# Compose Recomposition Profiling

## Stability Annotations

```kotlin
package {your.package}.presentation.user

import androidx.compose.runtime.Immutable
import {your.package}.data.local.entity.AccountEntity

/**
 * @Immutable: all properties are val and their types are also immutable.
 * @Stable: guarantees equals() consistency for Compose.
 *
 * WARNING: @Stable is a contract. Annotating a class with mutable fields
 * as @Stable causes subtle bugs where the UI does not update.
 */
@Immutable
data class AccountUiState(
    val account: AccountEntity? = null,
    val isLoading: Boolean = false,
    val lastStatusCode: Int? = null,
    val lastMessage: String? = null,
)
```

## Recomposition Tracker (Debug Only)

```kotlin
package {your.package}.core.performance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import co.touchlab.kermit.Logger
import {your.package}.core.logging.AppLogger

private val logger: Logger = AppLogger.withTag("Recomposition")

@Composable
fun LogRecompositions(name: String) {
    val counter = remember { RecompositionCounter(name) }
    SideEffect {
        counter.increment()
        logger.d { "Recompose: $name count=${counter.count}" }
    }
}

private class RecompositionCounter(val name: String) {
    var count = 0
        private set
    fun increment() { count++ }
}
```

High counts (>10 per user action) indicate unstable parameters. Check Compose compiler metrics to find which classes are unstable and which composables are not skippable.

## Compose Compiler Metrics

Add to `composeApp/build.gradle.kts`:

```kotlin
composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_metrics")
    metricsDestination = layout.buildDirectory.dir("compose_metrics")
}
```

### Reading the Output

```
# composables.txt -- restartability:
restartable skippable scheme("[...]") fun AccountScreen(...)
restartable scheme("[...]") fun AccountContent(...)  # NOT skippable = issue

# classes.txt -- stability:
stable class AccountUiState {
  stable val account: AccountEntity?
  stable val isLoading: Boolean
}
```

If a class shows as `unstable`, all composables reading it recompose on every state change.

### Fixing Unstable Classes

```kotlin
// Before (unstable):
data class State(val items: List<Item>)

// After (stable):
@Immutable
data class State(val items: kotlinx.collections.immutable.ImmutableList<Item>)
```

## derivedStateOf vs remember

- `derivedStateOf`: caches result, only recomputes when read State objects change
- `remember { computed }`: computed once, never updates (usually a bug)
- `remember(keys) { computed }`: recomputes when keys change, but may recompute even if the result would be the same

Use `derivedStateOf` when the result depends on other Compose state and you want to avoid redundant work.
