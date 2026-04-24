# Compose Performance

## Stability and Recomposition Skipping

### @Immutable and @Stable Annotations

The Compose compiler skips recomposition of a composable when all its parameters are unchanged. For custom types, the compiler must know whether they are stable:

```kotlin
// WRONG -- compiler cannot prove stability, recomposes every time
data class UserProfile(
    val name: String,
    val avatarUrl: String,
    val tags: List<String>,  // List is not stable (could be MutableList)
)

// RIGHT -- @Immutable guarantees no property changes after construction
@Immutable
data class UserProfile(
    val name: String,
    val avatarUrl: String,
    val tags: List<String>,  // safe under @Immutable contract
)
```

Use `@Immutable` when the class is truly deeply immutable after construction. Use `@Stable` when the class is mutable but notifies Compose of changes (e.g., `MutableState` properties):

```kotlin
@Stable
class CounterState {
    var count by mutableStateOf(0)
        private set

    fun increment() { count++ }
}
```

### Unstable Parameters to Avoid

These common patterns break recomposition skipping:

```kotlin
// WRONG -- new list instance every recomposition
@Composable
fun TagList(tags: List<String>) { /* ... */ }
// called as: TagList(tags = listOf("a", "b"))  // new List on every call

// RIGHT -- hoist or remember the list
val tags = remember { listOf("a", "b") }
TagList(tags = tags)
```

### ImmutableList for Stable Collection Parameters

`List<T>` is unstable because the Compose compiler cannot guarantee the underlying implementation is not `MutableList`. This causes composables with `List<T>` parameters to recompose on every frame, even when the content has not changed.

```kotlin
// WRONG: List<T> is unstable — Compose cannot guarantee immutability
@Composable
fun UserList(users: List<User>) { // Recomposes EVERY time, even if content unchanged
    // ...
}

// RIGHT: ImmutableList<T> is stable — Compose can skip recomposition
@Composable
fun UserList(users: ImmutableList<User>) { // Skips recomposition when content unchanged
    // ...
}
```

Dependency (version catalog, no hardcoded version):

```
// Version catalog
kotlinx-collections-immutable = { module = "org.jetbrains.kotlinx:kotlinx-collections-immutable", version.ref = "..." }
```

Conversion patterns in ViewModels:

```kotlin
// In ViewModel: convert before exposing to UI
val users: StateFlow<ImmutableList<User>> = repository.getUsers()
    .map { it.toImmutableList() }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())

// In composable: pass ImmutableList directly
UserList(users = state.users) // state.users is ImmutableList<User>
```

### Stability Strategy Comparison

```
| Strategy                | Compose can skip? | Best for                                           |
|-------------------------|-------------------|----------------------------------------------------|
| `@Immutable` data class | Always            | Pure data holders (ViewState, UiModel)             |
| `@Stable` class         | Between changes   | Classes with observable mutable properties         |
| `ImmutableList<T>`      | Yes               | Collection parameters                              |
| Raw `List<T>`           | Never (unstable)  | Avoid as @Composable parameter                     |
| `MutableState<T>`       | Depends on T      | Local UI state                                     |
| `derivedStateOf`        | Yes (deferred)    | Computed values from state                         |
```

## remember and derivedStateOf

### remember for Expensive Calculations

```kotlin
// WRONG -- regex compiled on every recomposition
@Composable
fun EmailField(value: String) {
    val isValid = Regex("^[\\w.]+@[\\w.]+$").matches(value)
    // ...
}

// RIGHT -- regex compiled once
@Composable
fun EmailField(value: String) {
    val emailPattern = remember { Regex("^[\\w.]+@[\\w.]+$") }
    val isValid = emailPattern.matches(value)
    // ...
}
```

### derivedStateOf for Computed State

Use `derivedStateOf` when a value is derived from one or more `State` objects and should only trigger recomposition when the derived value actually changes:

```kotlin
// WRONG -- recomposes every time scrollState changes (even when button visibility is unchanged)
@Composable
fun ScrollableContent(scrollState: LazyListState) {
    val showButton = scrollState.firstVisibleItemIndex > 0
    // ...
}

// RIGHT -- only recomposes when showButton changes from true to false or vice versa
@Composable
fun ScrollableContent(scrollState: LazyListState) {
    val showButton by remember {
        derivedStateOf { scrollState.firstVisibleItemIndex > 0 }
    }
    // ...
}
```

## Lambda Stability

New lambda instances on every recomposition break recomposition skipping for child composables:

```kotlin
// WRONG -- new lambda created on every recomposition of the parent
@Composable
fun ItemList(items: List<Item>, viewModel: ItemViewModel) {
    LazyColumn {
        items(items, key = { it.id }) { item ->
            ItemRow(
                item = item,
                onDelete = { viewModel.delete(item.id) },  // new lambda per recomposition
            )
        }
    }
}

// RIGHT -- remember the callback to stabilize lambda identity
@Composable
fun ItemList(items: List<Item>, viewModel: ItemViewModel) {
    LazyColumn {
        items(items, key = { it.id }) { item ->
            val onDelete = remember(item.id) { { viewModel.delete(item.id) } }
            ItemRow(
                item = item,
                onDelete = onDelete,
            )
        }
    }
}
```

## List Performance

### LazyColumn / LazyRow for Lists

Never use `Column` with `forEach` for lists of more than a handful of items:

```kotlin
// WRONG -- composes ALL items, even those off-screen
Column {
    items.forEach { item ->
        ItemRow(item)
    }
}

// RIGHT -- only composes visible items
LazyColumn {
    items(items, key = { it.id }) { item ->
        ItemRow(item)
    }
}
```

### key() for Stable Item Identity

Always provide a `key` to `items()` so Compose can reuse compositions when items move:

```kotlin
LazyColumn {
    items(
        items = accounts,
        key = { it.id },  // stable unique identifier
    ) { account ->
        AccountRow(account = account)
    }
}
```

### Avoid Allocations in Item Composables

```kotlin
// WRONG -- new Modifier and list created per item per recomposition
LazyColumn {
    items(accounts, key = { it.id }) { account ->
        val roles = listOf(account.primaryRole, account.secondaryRole)
        Row(modifier = Modifier.padding(AppTheme.spacing.lg)) {
            // ...
        }
    }
}

// RIGHT -- remember derived data
LazyColumn {
    items(accounts, key = { it.id }) { account ->
        val roles = remember(account.primaryRole, account.secondaryRole) {
            listOf(account.primaryRole, account.secondaryRole)
        }
        Row(modifier = Modifier.padding(AppTheme.spacing.lg)) {
            // ...
        }
    }
}
```

## GPU-Accelerated Transforms

Use `Modifier.graphicsLayer` for transforms (alpha, scale, rotation, translation) that should bypass recomposition and run on the GPU:

```kotlin
// WRONG -- recomposes on every alpha change
@Composable
fun FadingCard(alpha: Float) {
    Card(modifier = Modifier.alpha(alpha)) {
        // entire Card recomposes when alpha changes
    }
}

// RIGHT -- GPU-only operation, no recomposition
@Composable
fun FadingCard(alpha: () -> Float) {
    Card(
        modifier = Modifier.graphicsLayer { this.alpha = alpha() },
    ) {
        // Card does not recompose when alpha changes
    }
}
```

Note the lambda parameter `alpha: () -> Float` -- this defers the read to the draw phase, avoiding recomposition entirely.

## Avoid Allocations During Composition

Do not create objects in the composable body without `remember`:

```kotlin
// WRONG -- new Shape allocated on every recomposition
@Composable
fun StyledCard(content: @Composable () -> Unit) {
    Card(shape = RoundedCornerShape(AppTheme.corners.lg)) {
        content()
    }
}

// RIGHT -- shape remembered
@Composable
fun StyledCard(content: @Composable () -> Unit) {
    val shape = remember { RoundedCornerShape(AppTheme.corners.lg) }
    Card(shape = shape) {
        content()
    }
}
```

This matters most in hot paths (inside `LazyColumn` items, animated composables). For top-level screen composables that recompose rarely, the impact is negligible.

## Compose Compiler Reports

To diagnose stability issues, enable Compose compiler reports in `build.gradle.kts`:

```kotlin
composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    metricsDestination = layout.buildDirectory.dir("compose_compiler")
}
```

After building, check `build/compose_compiler/` for:
- `*-composables.txt` -- lists restartable/skippable status of every composable
- `*-classes.txt` -- lists stability of every class used as a composable parameter

A composable marked `restartable but not skippable` means at least one parameter is unstable. Fix by adding `@Immutable`/`@Stable` or wrapping in a stable type.

## Summary Checklist

| Rule | Why |
|---|---|
| Use `@Immutable` on all data classes passed to composables | Enables recomposition skipping |
| Use `remember` for expensive calculations | Avoids repeated work on recomposition |
| Use `derivedStateOf` for state-derived booleans/values | Reduces unnecessary recompositions |
| Use `key` in `LazyColumn`/`LazyRow` items | Preserves item state across reorders |
| Stabilize lambdas in list items with `remember` | Prevents child recomposition |
| Use `LazyColumn`/`LazyRow` for lists (not `Column` + `forEach`) | Only composes visible items |
| Use `Modifier.graphicsLayer` for animated transforms | GPU-only, no recomposition |
| Avoid allocations (`listOf`, `RoundedCornerShape`) in composable body without `remember` | Reduces GC pressure in hot paths |
| Never pass `MutableList` or `MutableMap` as composable parameters | Breaks structural equality |
| Use `ImmutableList<T>` instead of `List<T>` for composable parameters | Enables recomposition skipping for collections |
