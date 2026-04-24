package {your.package}.ui.scaffold

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.window.core.layout.WindowWidthSizeClass
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import {your.package}.ui.theme.AppTheme

// --- Navigation destination definition ---

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

// --- Adaptive scaffold with automatic nav mode switching ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveAppScaffold(
    modifier: Modifier = Modifier,
) {
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val widthSizeClass = adaptiveInfo.windowSizeClass.windowWidthSizeClass

    var currentDestination by rememberSaveable { mutableStateOf(TopLevelDestination.Home) }

    // Custom layout policy: drawer for expanded, default for others
    val navigationSuiteType = if (widthSizeClass == WindowWidthSizeClass.EXPANDED) {
        NavigationSuiteType.NavigationDrawer
    } else {
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)
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
                    onClick = { currentDestination = destination },
                )
            }
        },
    ) {
        // Content area — adapt based on window size class
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(currentDestination.labelRes),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )

            // Responsive content area
            AdaptiveContentArea(
                widthSizeClass = widthSizeClass,
                currentDestination = currentDestination,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// --- Responsive content area ---

@Composable
fun AdaptiveContentArea(
    widthSizeClass: WindowWidthSizeClass,
    currentDestination: TopLevelDestination,
    modifier: Modifier = Modifier,
) {
    val horizontalPadding = when (widthSizeClass) {
        WindowWidthSizeClass.COMPACT -> AppTheme.spacing.lg
        WindowWidthSizeClass.MEDIUM -> AppTheme.spacing.xl
        else -> AppTheme.spacing.xxl
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding)
            .padding(vertical = AppTheme.spacing.lg),
    ) {
        when (currentDestination) {
            TopLevelDestination.Home -> HomePlaceholder()
            TopLevelDestination.Search -> SearchPlaceholder()
            TopLevelDestination.Settings -> SettingsPlaceholder()
        }
    }
}

// --- Placeholder content composables (replace with actual implementations) ---

@Composable
private fun HomePlaceholder(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.nav_home),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(AppTheme.spacing.md))
        Text(
            text = stringResource(Res.string.home_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SearchPlaceholder(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.nav_search),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(AppTheme.spacing.md))
        Text(
            text = stringResource(Res.string.search_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsPlaceholder(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.nav_settings),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(AppTheme.spacing.md))
        Text(
            text = stringResource(Res.string.settings_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
