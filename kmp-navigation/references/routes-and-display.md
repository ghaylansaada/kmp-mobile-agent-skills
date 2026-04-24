# Routes and NavDisplay

## NavKey Sealed Interface

All navigation destinations implement a sealed interface that extends `NavKey`. This gives you exhaustive when-expressions and compile-time safety:

```kotlin
package {your.package}.navigation.routes

import androidx.navigation3.NavKey
import kotlinx.serialization.Serializable

sealed interface AppNavKey : NavKey

// Auth destinations
@Serializable data object LoginKey : AppNavKey
@Serializable data object RegisterKey : AppNavKey
@Serializable data object ForgotPasswordKey : AppNavKey

// Main destinations
@Serializable data object HomeKey : AppNavKey
@Serializable data class DetailKey(val id: String) : AppNavKey
@Serializable data object SearchKey : AppNavKey
@Serializable data object ProfileKey : AppNavKey
@Serializable data object SettingsKey : AppNavKey
```

## User-Owned Back Stack

Nav3 gives you direct ownership of the back stack as a `SnapshotStateList<NavKey>`:

```kotlin
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.NavKey
import androidx.navigation3.rememberNavBackStack

@Composable
fun AppNavDisplay(modifier: Modifier = Modifier) {
    val backStack: SnapshotStateList<AppNavKey> = rememberNavBackStack(LoginKey)

    // Navigate forward: add to back stack
    // backStack.add(DetailKey(id = "123"))

    // Navigate back: remove last entry
    // backStack.removeLastOrNull()

    // Clear and navigate: reset stack
    // backStack.clear()
    // backStack.add(HomeKey)
}
```

## NavDisplay Composable

NavDisplay renders the current destination(s) based on the back stack. You provide an entry factory that maps each NavKey to its composable content:

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.NavDisplay
import androidx.navigation3.rememberNavBackStack
import androidx.navigation3.entryProvider
import androidx.savedstate.rememberSavedStateRegistryNavEntryDecorator
import {your.package}.navigation.routes.*

@Composable
fun AppNavDisplay(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(LoginKey)

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        entryDecorators = listOf(
            rememberSavedStateRegistryNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<LoginKey> { key ->
                LoginScreen(
                    onLoginSuccess = {
                        backStack.clear()
                        backStack.add(HomeKey)
                    },
                    onNavigateToRegister = { backStack.add(RegisterKey) },
                    onNavigateToForgotPassword = { backStack.add(ForgotPasswordKey) },
                )
            }

            entry<RegisterKey> { key ->
                RegisterScreen(
                    onRegistrationSuccess = {
                        backStack.clear()
                        backStack.add(HomeKey)
                    },
                    onNavigateBack = { backStack.removeLastOrNull() },
                )
            }

            entry<ForgotPasswordKey> { key ->
                ForgotPasswordScreen(
                    onNavigateBack = { backStack.removeLastOrNull() },
                )
            }

            entry<HomeKey> { key ->
                HomeScreen(
                    onNavigateToDetail = { id -> backStack.add(DetailKey(id = id)) },
                )
            }

            entry<DetailKey> { key ->
                DetailScreen(
                    itemId = key.id,
                    onNavigateBack = { backStack.removeLastOrNull() },
                )
            }

            entry<SearchKey> { key ->
                SearchScreen()
            }

            entry<ProfileKey> { key ->
                ProfileScreen(
                    onNavigateToSettings = { backStack.add(SettingsKey) },
                )
            }

            entry<SettingsKey> { key ->
                SettingsScreen(
                    onNavigateBack = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}
```

## Entry Decorators

Entry decorators wrap each entry with cross-cutting concerns. Always include both:

- `rememberSavedStateRegistryNavEntryDecorator()` -- saves and restores entry state across configuration changes and process death
- `rememberViewModelStoreNavEntryDecorator()` -- scopes ViewModels to individual navigation entries so they are created/destroyed with their destination

## Updated App.kt

Replace direct screen call with `AppNavDisplay()`:

```kotlin
package {your.package}

import androidx.compose.runtime.Composable
import {your.package}.navigation.AppNavDisplay
import {your.package}.ui.theme.AppTheme

@Composable
fun App() {
    AppTheme {
        AppNavDisplay()
    }
}
```

## Back Stack Management Patterns

```kotlin
// Navigate forward
backStack.add(DetailKey(id = "123"))

// Navigate back
backStack.removeLastOrNull()

// Pop to specific destination (keep it, remove everything above)
val index = backStack.indexOfLast { it is HomeKey }
if (index >= 0) {
    while (backStack.size > index + 1) {
        backStack.removeLastOrNull()
    }
}

// Clear and navigate (e.g., after auth transition)
backStack.clear()
backStack.add(HomeKey)

// Replace current destination
backStack.removeLastOrNull()
backStack.add(ProfileKey)
```
