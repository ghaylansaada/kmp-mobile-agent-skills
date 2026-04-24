# Migration: Navigation 2 to Navigation 3

## Key Conceptual Changes

Navigation 3 fundamentally changes who owns the back stack. In Nav2, `NavController` owned and managed the back stack internally. In Nav3, you own the back stack as a `SnapshotStateList<NavKey>` and manipulate it directly.

## API Mapping Table

| Navigation 2 | Navigation 3 | Notes |
|--------------|--------------|-------|
| `NavHost` | `NavDisplay` | NavDisplay takes a back stack + entry provider |
| `NavController` | `SnapshotStateList<NavKey>` | User-owned back stack, no controller object |
| `rememberNavController()` | `rememberNavBackStack(startKey)` | Returns a SnapshotStateList |
| `composable<T> { }` | `entry<T> { key -> }` | Entry factory within `entryProvider { }` |
| `navController.navigate(route)` | `backStack.add(key)` | Direct list manipulation |
| `navController.popBackStack()` | `backStack.removeLastOrNull()` | Direct list manipulation |
| `navController.navigate(route) { popUpTo(...) }` | `backStack.clear(); backStack.add(key)` | Manual stack management |
| `NavGraphBuilder.navigation<T>(startDestination)` | Nested `NavDisplay` within an entry | No graph builder concept |
| `backStackEntry.toRoute<T>()` | `key` parameter in `entry<T> { key -> }` | Key is directly available |
| `NavDeepLink` annotations | Manual URI parsing + `backStack.add()` | No built-in deep link matching |
| `SavedStateHandle` result API | Shared state holder or shared ViewModel | No built-in result mechanism |
| `saveState`/`restoreState` | Per-tab back stacks | Manage separate stacks per tab |
| `NavBackStackEntry` | `NavEntry` | Different type, accessed via entry factory |
| `currentBackStackEntryAsState()` | `backStack.lastOrNull()` | Read directly from the list |
| `NavController.graph` | No equivalent | No graph concept -- just a list of keys |

## Step-by-Step Migration

### 1. Update Dependencies

Remove Nav2 artifact and add Nav3 artifacts in `libs.versions.toml`:

```toml
# Remove:
# navigation-compose = { module = "org.jetbrains.androidx.navigation:navigation-compose", ... }

# Add:
navigation3-ui = { module = "org.jetbrains.androidx.navigation3:navigation3-ui", version.ref = "navigation3" }
lifecycle-viewmodel-navigation3 = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-navigation3", version.ref = "lifecycle" }
```

Update `build.gradle.kts`:

```kotlin
commonMain.dependencies {
    // Remove: implementation(libs.navigation.compose)
    implementation(libs.navigation3.ui)
    implementation(libs.lifecycle.viewmodel.navigation3)
}
```

### 2. Convert Route Objects to NavKey

**Before (Nav2):**
```kotlin
@Serializable data object HomeRoute
@Serializable data class DetailRoute(val id: String)
```

**After (Nav3):**
```kotlin
import androidx.navigation3.NavKey

sealed interface AppNavKey : NavKey

@Serializable data object HomeKey : AppNavKey
@Serializable data class DetailKey(val id: String) : AppNavKey
```

### 3. Replace NavHost with NavDisplay

**Before (Nav2):**
```kotlin
val navController = rememberNavController()
NavHost(navController = navController, startDestination = HomeRoute) {
    composable<HomeRoute> {
        HomeScreen(onNavigateToDetail = { id ->
            navController.navigate(DetailRoute(id = id))
        })
    }
    composable<DetailRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<DetailRoute>()
        DetailScreen(itemId = route.id, onBack = { navController.popBackStack() })
    }
}
```

**After (Nav3):**
```kotlin
val backStack = rememberNavBackStack(HomeKey)
NavDisplay(
    backStack = backStack,
    entryDecorators = listOf(
        rememberSavedStateRegistryNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
    ),
    entryProvider = entryProvider {
        entry<HomeKey> { key ->
            HomeScreen(onNavigateToDetail = { id ->
                backStack.add(DetailKey(id = id))
            })
        }
        entry<DetailKey> { key ->
            DetailScreen(itemId = key.id, onBack = { backStack.removeLastOrNull() })
        }
    },
)
```

### 4. Convert Navigation Graphs to Nested NavDisplay

**Before (Nav2):**
```kotlin
fun NavGraphBuilder.settingsNavGraph(navController: NavController) {
    navigation<SettingsGraph>(startDestination = SettingsHomeRoute) {
        composable<SettingsHomeRoute> { ... }
        composable<NotificationSettingsRoute> { ... }
    }
}
```

**After (Nav3):**
```kotlin
entry<SettingsKey> { key ->
    val settingsBackStack = rememberNavBackStack(SettingsHomeKey)
    NavDisplay(
        backStack = settingsBackStack,
        entryProvider = entryProvider {
            entry<SettingsHomeKey> { ... }
            entry<NotificationSettingsKey> { ... }
        },
    )
}
```

### 5. Convert Bottom Navigation

**Before (Nav2):**
```kotlin
navController.navigate(tab.route) {
    popUpTo(navController.graph.startDestinationId) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
```

**After (Nav3):**
```kotlin
// Maintain per-tab back stacks
val tabBackStacks = mapOf(
    Tab.Home to rememberNavBackStack(HomeKey),
    Tab.Search to rememberNavBackStack(SearchKey),
    Tab.Profile to rememberNavBackStack(ProfileKey),
)
var currentTab by rememberSaveable { mutableStateOf(Tab.Home) }

// Switch tabs by changing currentTab -- each stack is preserved
NavDisplay(
    backStack = tabBackStacks.getValue(currentTab),
    // ...
)
```

### 6. Convert Auth Gating

**Before (Nav2):**
```kotlin
key(startDestination) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDestination) {
        authNavGraph(navController)
        mainNavGraph(navController)
    }
}
```

**After (Nav3):**
```kotlin
val backStack = rememberNavBackStack(LoginKey)
LaunchedEffect(sessionState) {
    when (sessionState) {
        SessionState.Authenticated -> {
            backStack.clear()
            backStack.add(HomeKey)
        }
        else -> {
            backStack.clear()
            backStack.add(LoginKey)
        }
    }
}
NavDisplay(backStack = backStack, ...)
```

### 7. Add Polymorphic Serialization (KMP)

This step is new for Nav3. Non-JVM targets require explicit polymorphic registration. See [setup.md](setup.md) for the full `SerializersModule` configuration.

## Common Migration Pitfalls

| Pitfall | Symptom | Fix |
|---------|---------|-----|
| Forgetting entry decorators | ViewModels not scoped, state not saved | Add both `rememberSavedStateRegistryNavEntryDecorator()` and `rememberViewModelStoreNavEntryDecorator()` |
| Using Nav2 imports | Compile errors on `NavHost`, `composable<T>` | Replace all `androidx.navigation` imports with `androidx.navigation3` |
| Not clearing back stack on auth transition | Stale entries remain, user can back-navigate to wrong flow | Call `backStack.clear()` before adding the new start key |
| Missing polymorphic serialization on iOS | Crash on state save/restore | Register all NavKey subtypes in `SerializersModule` |
| Calling `removeLastOrNull()` on single-entry stack | Back stack becomes empty, NavDisplay has nothing to show | Guard with `if (backStack.size > 1)` for root destinations |
