# List-Detail and Supporting Pane Layouts

Canonical layouts are pre-built adaptive patterns from Material 3 that handle single-pane vs. multi-pane transitions automatically based on window size class. The two primary patterns are list-detail (master-detail) and supporting pane.

## Dependencies

These are the same adaptive dependencies from [window-size-classes.md](window-size-classes.md):

```kotlin
commonMain.dependencies {
    implementation(libs.material3.adaptive)
    implementation(libs.material3.adaptive.layout)
    implementation(libs.material3.adaptive.navigation)
}
```

## List-Detail Layout

### NavigableListDetailPaneScaffold

`NavigableListDetailPaneScaffold` manages a list pane and a detail pane with built-in navigation and back handling:

```kotlin
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator

@Composable
fun ItemListDetailScreen(
    modifier: Modifier = Modifier,
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<String>()

    NavigableListDetailPaneScaffold(
        modifier = modifier,
        navigator = navigator,
        listPane = {
            ItemListPane(
                onItemClick = { itemId ->
                    navigator.navigateTo(
                        pane = ListDetailPaneScaffoldRole.Detail,
                        contentKey = itemId,
                    )
                },
            )
        },
        detailPane = {
            val selectedItemId = navigator.currentDestination?.contentKey
            if (selectedItemId != null) {
                ItemDetailPane(itemId = selectedItemId)
            } else {
                EmptyDetailPlaceholder()
            }
        },
    )
}
```

### Pane Composables

```kotlin
@Composable
fun ItemListPane(
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        items(
            items = items,
            key = { it.id },
        ) { item ->
            ItemRow(
                item = item,
                onClick = { onItemClick(item.id) },
            )
        }
    }
}

@Composable
fun ItemDetailPane(
    itemId: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppTheme.spacing.lg),
    ) {
        Text(
            text = stringResource(Res.string.item_detail_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(AppTheme.spacing.lg))
        // Detail content
    }
}

@Composable
fun EmptyDetailPlaceholder(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.select_item_prompt),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
```

### Layout Behavior

| Window Width Class | List Pane | Detail Pane |
|---|---|---|
| Compact | Full screen; detail pushes list off-screen | Full screen; back returns to list |
| Medium | Side-by-side, ~40/60 split | Side-by-side |
| Expanded | Side-by-side, ~33/67 split | Side-by-side |

The scaffold handles these transitions automatically. You do not write `when (widthSizeClass)` for pane visibility.

### Navigation Roles

`ListDetailPaneScaffoldRole` defines three roles:

| Role | Purpose |
|---|---|
| `List` | The primary list pane |
| `Detail` | The secondary detail pane |
| `Extra` | An optional third pane (rarely used) |

Navigate to a role with a content key:

```kotlin
navigator.navigateTo(
    pane = ListDetailPaneScaffoldRole.Detail,
    contentKey = itemId,
)
```

Navigate to the extra pane (e.g., item settings):

```kotlin
navigator.navigateTo(
    pane = ListDetailPaneScaffoldRole.Extra,
    contentKey = settingsKey,
)
```

### Back Navigation Strategies

`NavigableListDetailPaneScaffold` supports two back navigation behaviors via the `defaultBackBehavior` parameter:

**PopUntilScaffoldValueChange** (default): Pops the back stack until the visible pane combination changes. Use this when back should close the detail pane and return to the list.

```kotlin
NavigableListDetailPaneScaffold(
    navigator = navigator,
    defaultBackBehavior = BackNavigationBehavior.PopUntilScaffoldValueChange,
    listPane = { /* ... */ },
    detailPane = { /* ... */ },
)
```

**PopUntilContentChange**: Pops the back stack until the content key changes. Use this when back should navigate through previously viewed detail items.

```kotlin
NavigableListDetailPaneScaffold(
    navigator = navigator,
    defaultBackBehavior = BackNavigationBehavior.PopUntilContentChange,
    listPane = { /* ... */ },
    detailPane = { /* ... */ },
)
```

Decision guide:
- Email app (back closes message, returns to inbox): `PopUntilScaffoldValueChange`
- Browser history (back goes to previous page): `PopUntilContentChange`

### Predictive Back Animation

`NavigableListDetailPaneScaffold` has built-in support for predictive back gestures. The detail pane animates back toward the list pane when the user starts a back swipe.

Requirements:
- Android manifest must include `android:enableOnBackInvokedCallback="true"` on the `<application>` or `<activity>` tag
- No additional code needed -- the scaffold handles the animation

```xml
<application
    android:enableOnBackInvokedCallback="true"
    ...>
```

## Supporting Pane Layout

`NavigableSupportingPaneScaffold` provides a main content area with a supporting pane (e.g., a properties panel, help sidebar, or context panel):

```kotlin
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableSupportingPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator

@Composable
fun EditorWithPropertiesScreen(
    modifier: Modifier = Modifier,
) {
    val navigator = rememberSupportingPaneScaffoldNavigator()

    NavigableSupportingPaneScaffold(
        modifier = modifier,
        navigator = navigator,
        mainPane = {
            EditorMainContent(
                onShowProperties = {
                    navigator.navigateTo(SupportingPaneScaffoldRole.Supporting)
                },
            )
        },
        supportingPane = {
            PropertiesPanel()
        },
    )
}

@Composable
fun EditorMainContent(
    onShowProperties: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppTheme.spacing.lg),
    ) {
        Text(
            text = stringResource(Res.string.editor_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(AppTheme.spacing.lg))
        // Main content area
        Spacer(Modifier.weight(1f))
        FilledTonalButton(
            onClick = onShowProperties,
            modifier = Modifier.heightIn(min = AppTheme.sizing.minTouchTarget),
        ) {
            Text(stringResource(Res.string.show_properties))
        }
    }
}

@Composable
fun PropertiesPanel(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppTheme.spacing.lg),
    ) {
        Text(
            text = stringResource(Res.string.properties_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(AppTheme.spacing.md))
        // Properties content
    }
}
```

### Supporting Pane Behavior

| Window Width Class | Main Pane | Supporting Pane |
|---|---|---|
| Compact | Full screen | Pushed on top; back dismisses |
| Medium | Side-by-side, ~60/40 split | Side-by-side |
| Expanded | Side-by-side, ~67/33 split | Side-by-side |

## ThreePaneScaffoldNavigator API

Both `rememberListDetailPaneScaffoldNavigator` and `rememberSupportingPaneScaffoldNavigator` return a `ThreePaneScaffoldNavigator`:

```kotlin
interface ThreePaneScaffoldNavigator<T> {
    val currentDestination: ThreePaneScaffoldDestinationItem<T>?
    val scaffoldValue: ThreePaneScaffoldValue
    suspend fun navigateTo(pane: ThreePaneScaffoldRole, contentKey: T? = null)
    suspend fun navigateBack(): Boolean
}
```

Key properties:
- `currentDestination` -- the currently focused pane and its content key
- `scaffoldValue` -- the current visibility state of all three panes
- `navigateTo()` -- navigates to a pane, optionally with a content key
- `navigateBack()` -- pops the internal back stack; returns false if already at root

## Rules

1. **Use `NavigableListDetailPaneScaffold` for all list-detail patterns.** Do not build manual two-pane layouts with `Row` and `if (isExpanded)`.
2. **Choose the correct back behavior.** Defaulting to `PopUntilScaffoldValueChange` is correct for most cases. Only use `PopUntilContentChange` when back should navigate through content history.
3. **Show a placeholder in the detail pane when no item is selected.** On expanded windows the detail pane is visible from the start. A blank pane is a poor UX.
4. **All text must use `stringResource()`.** Labels, titles, placeholders, and empty states must never be hardcoded.
5. **All spacing must use `AppTheme.spacing.*`.** Content padding, arrangement spacing, and margins must use design tokens.
6. **Enable predictive back in the manifest.** Without `android:enableOnBackInvokedCallback="true"`, the built-in predictive back animation does not work.

## Imports

```kotlin
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.NavigableSupportingPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
```
