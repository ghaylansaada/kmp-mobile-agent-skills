# NavigationSuiteScaffold

`NavigationSuiteScaffold` is a Material 3 adaptive component that automatically switches between a bottom navigation bar, navigation rail, and navigation drawer based on the current window size class. It eliminates manual `when (widthSizeClass)` branching for navigation chrome.

## Dependency

### Version Catalog (libs.versions.toml)

```toml
[libraries]
material3-adaptive-navigation-suite = { module = "org.jetbrains.compose.material3:material3-adaptive-navigation-suite", version.ref = "composeAdaptiveNavigationSuite" }
```

Use the JetBrains artifact (`org.jetbrains.compose.material3`), NOT the AndroidX-only artifact (`androidx.compose.material3`).

### Shared Module build.gradle.kts

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.material3.adaptive.navigation.suite)
        }
    }
}
```

## Basic Setup

```kotlin
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import org.jetbrains.compose.resources.stringResource

enum class TopLevelDestination(
    val icon: ImageVector,
    val labelRes: StringResource,
    val contentDescriptionRes: StringResource,
) {
    Home(
        icon = Icons.Default.Home,
        labelRes = Res.string.nav_home,
        contentDescriptionRes = Res.string.cd_nav_home,
    ),
    Search(
        icon = Icons.Default.Search,
        labelRes = Res.string.nav_search,
        contentDescriptionRes = Res.string.cd_nav_search,
    ),
    Settings(
        icon = Icons.Default.Settings,
        labelRes = Res.string.nav_settings,
        contentDescriptionRes = Res.string.cd_nav_settings,
    ),
}

@Composable
fun AppScaffold(
    currentDestination: TopLevelDestination,
    onDestinationSelected: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    NavigationSuiteScaffold(
        modifier = modifier,
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                item(
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = stringResource(destination.contentDescriptionRes),
                        )
                    },
                    label = { Text(stringResource(destination.labelRes)) },
                    selected = currentDestination == destination,
                    onClick = { onDestinationSelected(destination) },
                )
            }
        },
    ) {
        content()
    }
}
```

## Default Behavior

`NavigationSuiteScaffold` selects the navigation component automatically based on `currentWindowAdaptiveInfo()`:

| Window Width Class | Navigation Component |
|---|---|
| Compact (< 600dp) | Bottom navigation bar |
| Medium (600dp -- 839dp) | Navigation rail |
| Expanded (>= 840dp) | Navigation rail |

## Custom Layout Policy Override

Override the default mapping to show a navigation drawer on expanded windows:

```kotlin
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.window.core.layout.WindowWidthSizeClass

@Composable
fun AppScaffold(
    currentDestination: TopLevelDestination,
    onDestinationSelected: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val adaptiveInfo = currentWindowAdaptiveInfo()

    val navigationSuiteType = with(adaptiveInfo) {
        if (windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED) {
            NavigationSuiteType.NavigationDrawer
        } else {
            NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)
        }
    }

    NavigationSuiteScaffold(
        modifier = modifier,
        layoutType = navigationSuiteType,
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                item(
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = stringResource(destination.contentDescriptionRes),
                        )
                    },
                    label = { Text(stringResource(destination.labelRes)) },
                    selected = currentDestination == destination,
                    onClick = { onDestinationSelected(destination) },
                )
            }
        },
    ) {
        content()
    }
}
```

## Integration with Navigation Back Stack

Wire `NavigationSuiteScaffold` with a navigation back stack so that selecting a tab navigates to its route:

```kotlin
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun MainScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val currentDestination = TopLevelDestination.entries.firstOrNull { destination ->
        currentRoute?.contains(destination.name, ignoreCase = true) == true
    } ?: TopLevelDestination.Home

    NavigationSuiteScaffold(
        modifier = modifier,
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                item(
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = stringResource(destination.contentDescriptionRes),
                        )
                    },
                    label = { Text(stringResource(destination.labelRes)) },
                    selected = currentDestination == destination,
                    onClick = {
                        navController.navigate(destination.name) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) {
        // NavHost content goes here
        AppNavHost(navController = navController)
    }
}
```

Key points:
- `saveState = true` + `restoreState = true` must both be set to preserve tab state (scroll position, sub-screens)
- `launchSingleTop = true` prevents duplicate destinations on double-tap
- `popUpTo(startDestinationId)` prevents back-stack bloat from repeated tab switches

## Navigation Suite with NavigationSuiteScaffoldDefaults

`NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo()` computes the appropriate `NavigationSuiteType` from the current `WindowAdaptiveInfo`. Use it as a baseline and override only for specific cases:

```kotlin
val defaultType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(
    currentWindowAdaptiveInfo()
)
```

This returns:
- `NavigationSuiteType.NavigationBar` for compact width
- `NavigationSuiteType.NavigationRail` for medium and expanded width

## Styling

### Navigation Rail with Header

```kotlin
NavigationSuiteScaffold(
    navigationSuiteItems = { /* ... */ },
    navigationSuiteColors = NavigationSuiteDefaults.colors(
        navigationBarContainerColor = MaterialTheme.colorScheme.surface,
        navigationRailContainerColor = MaterialTheme.colorScheme.surface,
    ),
) {
    content()
}
```

All colors must come from `MaterialTheme.colorScheme`. Do not hardcode color values.

## Rules

1. **Always use `stringResource()` for navigation item labels and content descriptions.** Never hardcode text in `item(label = { Text("Home") })`.
2. **Always use `MaterialTheme.colorScheme.*` for any color overrides.** Never hardcode hex or Color() values.
3. **Use `saveState = true` and `restoreState = true` together.** Omitting either causes tab state loss on switch.
4. **Hoist navigation state in ViewModel or `rememberSaveable`.** The scaffold rebuilds when window size class changes (rotation, fold/unfold). Ephemeral state in the scaffold composable is lost.
5. **Do not nest `Scaffold` inside `NavigationSuiteScaffold` for the top bar.** `NavigationSuiteScaffold` is a scaffold. Nesting causes double inset handling. Place your `TopAppBar` inside the content lambda instead.

## Imports

```kotlin
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.window.core.layout.WindowWidthSizeClass
```
