# Navigation Patterns

## Bottom Navigation with Tabs

Nav3 bottom navigation requires managing per-tab back stacks. Each tab maintains its own stack so state is preserved across tab switches:

```kotlin
package {your.package}.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.NavDisplay
import androidx.navigation3.rememberNavBackStack
import androidx.navigation3.entryProvider
import androidx.savedstate.rememberSavedStateRegistryNavEntryDecorator
import {your.package}.navigation.routes.*
import org.jetbrains.compose.resources.stringResource
import {your.package}.resources.Res
import {your.package}.resources.*

enum class BottomNavTab(
    val labelRes: StringResource,
    val icon: ImageVector,
    val startKey: AppNavKey,
) {
    Home(labelRes = Res.string.tab_home, icon = Icons.Default.Home, startKey = HomeKey),
    Search(labelRes = Res.string.tab_search, icon = Icons.Default.Search, startKey = SearchKey),
    Profile(labelRes = Res.string.tab_profile, icon = Icons.Default.Person, startKey = ProfileKey),
}

@Composable
fun MainScaffold(onLogout: () -> Unit, modifier: Modifier = Modifier) {
    var currentTab by rememberSaveable { mutableStateOf(BottomNavTab.Home) }
    val tabBackStacks = mapOf(
        BottomNavTab.Home to rememberNavBackStack(HomeKey),
        BottomNavTab.Search to rememberNavBackStack(SearchKey),
        BottomNavTab.Profile to rememberNavBackStack(ProfileKey),
    )

    Scaffold(
        modifier = modifier,
        bottomBar = {
            AppBottomNavBar(
                currentTab = currentTab,
                onTabSelected = { tab -> currentTab = tab },
            )
        },
    ) { innerPadding ->
        val backStack = tabBackStacks.getValue(currentTab)
        TabNavDisplay(
            backStack = backStack,
            onLogout = onLogout,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun TabNavDisplay(
    backStack: SnapshotStateList<AppNavKey>,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        entryDecorators = listOf(
            rememberSavedStateRegistryNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
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
                    onLogout = onLogout,
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

@Composable
fun AppBottomNavBar(
    currentTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        BottomNavTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = currentTab == tab,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = stringResource(tab.labelRes),
                    )
                },
                label = { Text(text = stringResource(tab.labelRes)) },
            )
        }
    }
}
```

## Nested Navigation

For features that have their own internal navigation flow (e.g., a settings section with sub-screens), use a nested NavDisplay within an entry:

```kotlin
import androidx.navigation3.NavDisplay
import androidx.navigation3.rememberNavBackStack
import androidx.navigation3.entryProvider

entry<SettingsKey> { key ->
    val settingsBackStack = rememberNavBackStack(SettingsHomeKey)

    NavDisplay(
        backStack = settingsBackStack,
        entryDecorators = listOf(
            rememberSavedStateRegistryNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<SettingsHomeKey> { _ ->
                SettingsHomeScreen(
                    onNavigateToNotifications = {
                        settingsBackStack.add(NotificationSettingsKey)
                    },
                    onNavigateToPrivacy = {
                        settingsBackStack.add(PrivacySettingsKey)
                    },
                    onNavigateBack = { parentBackStack.removeLastOrNull() },
                )
            }
            entry<NotificationSettingsKey> { _ ->
                NotificationSettingsScreen(
                    onNavigateBack = { settingsBackStack.removeLastOrNull() },
                )
            }
            entry<PrivacySettingsKey> { _ ->
                PrivacySettingsScreen(
                    onNavigateBack = { settingsBackStack.removeLastOrNull() },
                )
            }
        },
    )
}
```

## Argument Passing via NavKey Properties

Nav3 arguments are simply properties on your NavKey data classes. No bundles, no type maps, no string-based argument parsing:

```kotlin
@Serializable
data class DetailKey(val id: String) : AppNavKey

@Serializable
data class SearchResultsKey(
    val query: String,
    val category: String? = null,
    val page: Int = 1,
) : AppNavKey

// Navigate with arguments -- just construct the key
backStack.add(DetailKey(id = "abc-123"))
backStack.add(SearchResultsKey(query = "kotlin", category = "tutorials", page = 2))

// Access in entry factory -- key properties are directly available
entry<DetailKey> { key ->
    DetailScreen(itemId = key.id)
}

entry<SearchResultsKey> { key ->
    SearchResultsScreen(
        query = key.query,
        category = key.category,
        page = key.page,
    )
}
```

## Result Passing Between Destinations

Nav3 does not have a built-in SavedStateHandle result mechanism like Nav2. Use a shared state holder or callback pattern:

**Option A: Callback via shared state holder**

```kotlin
class NavigationResultHolder {
    private val results = mutableMapOf<String, Any?>()

    fun setResult(key: String, value: Any?) {
        results[key] = value
    }

    fun <T> consumeResult(key: String): T? {
        @Suppress("UNCHECKED_CAST")
        return results.remove(key) as? T
    }
}

// Provide via Koin or CompositionLocal
// In the destination that returns a result:
resultHolder.setResult("selectedItem", selectedItem)
backStack.removeLastOrNull()

// In the calling destination:
val result = resultHolder.consumeResult<String>("selectedItem")
```

**Option B: Shared ViewModel scoped to parent**

```kotlin
// Parent and child entries share a ViewModel scoped to the parent entry.
// The child writes a result to the shared ViewModel, then pops.
// The parent reads from the shared ViewModel on recomposition.
```

## Adaptive Multi-Destination Layouts

For tablet/desktop layouts that show multiple destinations simultaneously (e.g., list-detail), use scene strategies from the Material3 Adaptive Navigation 3 artifact:

```kotlin
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy

NavDisplay(
    backStack = backStack,
    entryDecorators = listOf(
        rememberSavedStateRegistryNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
    ),
    sceneStrategy = ListDetailSceneStrategy,
    entryProvider = entryProvider {
        entry<ListKey>(
            metadata = ListDetailSceneStrategy.listPane,
        ) { key ->
            ListScreen(onItemSelected = { id -> backStack.add(DetailKey(id = id)) })
        }

        entry<DetailKey>(
            metadata = ListDetailSceneStrategy.detailPane,
        ) { key ->
            DetailScreen(itemId = key.id)
        }
    },
)
```

On large screens, both list and detail show side-by-side. On small screens, they stack normally.

Additional navigation patterns and back stack utilities are in [navigate-with-args.kt](../assets/snippets/navigate-with-args.kt).
