# Compose Semantics API

## Overview

The Compose Semantics API is the bridge between your UI and platform accessibility services (TalkBack on Android, VoiceOver on iOS). Every semantic property you set on a composable becomes metadata that accessibility services read and announce to users. In Compose Multiplatform, the same semantics API maps to `AccessibilityNodeInfo` on Android and `UIAccessibility` protocols on iOS.

## Modifier.semantics {}

The primary modifier for annotating composables with accessibility metadata.

```kotlin
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import {your.package}.ui.theme.AppTheme
```

### contentDescription

Provides a text label that screen readers announce instead of (or in addition to) the visible text.

```kotlin
Icon(
    imageVector = Icons.Default.Search,
    contentDescription = null, // will be set via semantics
    modifier = Modifier
        .size(AppTheme.sizing.iconMd)
        .semantics {
            contentDescription = stringResource(Res.string.cd_search)
        },
)
```

### role

Tells the screen reader what kind of element this is. The platform maps roles to native traits.

```kotlin
// Custom composable that behaves as a button
Box(
    modifier = Modifier
        .semantics { role = Role.Button }
        .clickable(
            onClickLabel = stringResource(Res.string.action_open_details),
            onClick = onItemClick,
        )
        .padding(AppTheme.spacing.lg),
) {
    Text(stringResource(Res.string.view_details))
}
```

Available roles and their platform mappings:

| Compose Role | Android AccessibilityNodeInfo | iOS UIAccessibilityTrait |
|---|---|---|
| `Role.Button` | className = "android.widget.Button" | `.button` |
| `Role.Checkbox` | className = "android.widget.CheckBox" | `.button` + checked state |
| `Role.Switch` | className = "android.widget.Switch" | `.button` + toggle state |
| `Role.RadioButton` | className = "android.widget.RadioButton" | `.button` + selected state |
| `Role.Tab` | ACTION_SELECT | `.button` + selected state |
| `Role.Image` | className = "android.widget.ImageView" | `.image` |
| `Role.DropdownList` | className = "android.widget.Spinner" | No direct mapping |

### stateDescription

Provides a human-readable description of the current state for stateful components.

```kotlin
@Composable
fun AccessibleToggle(
    label: String,
    isEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                stateDescription = if (isEnabled) {
                    stringResource(Res.string.cd_state_enabled)
                } else {
                    stringResource(Res.string.cd_state_disabled)
                }
            }
            .clickable(
                onClickLabel = stringResource(Res.string.cd_toggle),
                onClick = onToggle,
            )
            .padding(AppTheme.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
        Switch(
            checked = isEnabled,
            onCheckedChange = null, // handled by Row clickable
        )
    }
}
```

### heading()

Marks a composable as a heading. Screen reader users can navigate between headings using rotor (iOS) or heading navigation (Android).

```kotlin
Text(
    text = stringResource(Res.string.section_profile),
    style = MaterialTheme.typography.headlineMedium,
    modifier = Modifier.semantics { heading() },
)
```

### liveRegion

Causes the screen reader to announce changes to this element without the user needing to navigate to it. Use for status messages, counters, and toast-like updates.

```kotlin
Text(
    text = stringResource(Res.string.items_in_cart, cartCount),
    style = MaterialTheme.typography.bodyMedium,
    modifier = Modifier.semantics {
        liveRegion = LiveRegionMode.Polite
    },
)
```

- `LiveRegionMode.Polite` -- Announcement queued after current speech finishes. Use for non-urgent updates (cart count, form status).
- `LiveRegionMode.Assertive` -- Announcement interrupts current speech immediately. Use only for critical alerts (error messages, urgent warnings). Overuse is disruptive.

### error()

Marks a composable as containing an error. Screen readers announce "Error: [message]" when the user focuses the element.

```kotlin
OutlinedTextField(
    value = email,
    onValueChange = onEmailChange,
    label = { Text(stringResource(Res.string.label_email)) },
    isError = emailError != null,
    supportingText = emailError?.let { { Text(it) } },
    modifier = Modifier
        .fillMaxWidth()
        .semantics {
            if (emailError != null) {
                error(emailError)
            }
        },
)
```

### paneTitle

Announces a screen or pane change. Use when navigating to a new screen or when a significant portion of the screen content changes.

```kotlin
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val screenTitle = stringResource(Res.string.screen_settings)
    Column(
        modifier = modifier
            .fillMaxSize()
            .semantics { paneTitle = screenTitle }
            .padding(AppTheme.spacing.lg),
    ) {
        // screen content
    }
}
```

## Merged vs Unmerged Semantics Tree

Compose maintains two semantics trees: the **unmerged tree** (every composable with semantics is a node) and the **merged tree** (children merged into parent nodes). Accessibility services read the merged tree.

### mergeDescendants = true

Merges all descendant semantic nodes into the parent. The parent becomes a single focusable unit for the screen reader.

```kotlin
// List item: icon + title + subtitle announced as one unit
Row(
    modifier = Modifier
        .semantics(mergeDescendants = true) {}
        .clickable(
            onClickLabel = stringResource(Res.string.cd_open_item),
            onClick = onItemClick,
        )
        .padding(AppTheme.spacing.lg),
    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    verticalAlignment = Alignment.CenterVertically,
) {
    Icon(
        imageVector = Icons.Default.Person,
        contentDescription = null, // merged into parent
        modifier = Modifier.size(AppTheme.sizing.iconMd),
    )
    Column {
        Text(
            text = userName,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = userEmail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
```

**When to use `mergeDescendants = true`:**
- List items where icon + text + metadata should be a single announcement
- Card surfaces that act as a single tappable unit
- Composite controls (label + value displayed together)

**When NOT to use `mergeDescendants = true`:**
- Containers with multiple independently interactive children (buttons inside a card)
- Form layouts where each field needs individual focus
- Navigation bars where each tab must be independently selectable

### clearAndSetSemantics {}

Replaces the entire subtree's semantics with only what you specify. Children are completely removed from the accessibility tree.

```kotlin
// Custom chip with delete button -- provide a single description with actions
Row(
    modifier = Modifier
        .clearAndSetSemantics {
            contentDescription = stringResource(Res.string.cd_filter_chip, filterName)
            role = Role.Button
        }
        .clickable(onClick = onChipClick)
        .padding(
            horizontal = AppTheme.spacing.md,
            vertical = AppTheme.spacing.sm,
        ),
) {
    Text(text = filterName)
    Spacer(Modifier.width(AppTheme.spacing.xs))
    Icon(
        imageVector = Icons.Default.Close,
        contentDescription = null, // cleared by parent
        modifier = Modifier.size(AppTheme.sizing.iconSm),
    )
}
```

**Warning:** If the delete icon needs to be independently interactive (e.g., tap chip to select, tap X to remove), do NOT use `clearAndSetSemantics`. Use `mergeDescendants = true` with `CustomAccessibilityAction` instead. See [touch-and-interaction.md](touch-and-interaction.md).

## progressBarRangeInfo

Communicates progress bar values to screen readers.

```kotlin
import androidx.compose.ui.semantics.progressBarRangeInfo

// Determinate progress
LinearProgressIndicator(
    progress = { downloadProgress },
    modifier = Modifier
        .fillMaxWidth()
        .semantics {
            progressBarRangeInfo = ProgressBarRangeInfo(
                current = downloadProgress,
                range = 0f..1f,
                steps = 0,
            )
            contentDescription = stringResource(
                Res.string.cd_download_progress,
                (downloadProgress * 100).toInt(),
            )
        },
)

// Indeterminate progress
CircularProgressIndicator(
    modifier = Modifier
        .size(AppTheme.sizing.iconLg)
        .semantics {
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            contentDescription = stringResource(Res.string.cd_loading)
        },
)
```

**Important:** Never set `ProgressBarRangeInfo(current = 0f, range = 0f..0f)`. This causes division-by-zero on some accessibility services. Use `ProgressBarRangeInfo.Indeterminate` for unknown progress.

## collectionInfo and collectionItemInfo

Communicate list/grid structure to screen readers so users hear "item 3 of 10" instead of just the item content.

```kotlin
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo

@Composable
fun AccessibleList(
    items: List<String>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.semantics {
            collectionInfo = CollectionInfo(
                rowCount = items.size,
                columnCount = 1,
            )
        },
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        itemsIndexed(items, key = { _, item -> item }) { index, item ->
            Text(
                text = item,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        collectionItemInfo = CollectionItemInfo(
                            rowIndex = index,
                            rowSpan = 1,
                            columnIndex = 0,
                            columnSpan = 1,
                        )
                    }
                    .padding(AppTheme.spacing.md),
            )
        }
    }
}
```

## isTraversalGroup and traversalIndex

Control the order in which the screen reader traverses elements.

### isTraversalGroup

Groups elements so the screen reader reads all children of one group before moving to the next.

```kotlin
// Top bar reads entirely before body content
Column(modifier = Modifier.fillMaxSize()) {
    // Group 1: Top bar
    Row(
        modifier = Modifier
            .semantics { isTraversalGroup = true }
            .fillMaxWidth()
            .padding(AppTheme.spacing.lg),
    ) {
        Text(
            text = stringResource(Res.string.screen_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onSettingsClick) {
            Icon(
                Icons.Default.Settings,
                contentDescription = stringResource(Res.string.cd_settings),
            )
        }
    }

    // Group 2: Body
    Column(
        modifier = Modifier
            .semantics { isTraversalGroup = true }
            .fillMaxWidth()
            .padding(AppTheme.spacing.lg),
    ) {
        // body content
    }
}
```

### traversalIndex

Overrides the default reading order within a traversal group. Lower values are read first. Default is `0f`.

```kotlin
// Force the error message to be read before the input field
Column {
    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        modifier = Modifier.semantics { traversalIndex = 1f },
    )
    if (emailError != null) {
        Text(
            text = emailError,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { traversalIndex = 0f },
        )
    }
}
```

**Important:** `traversalIndex` only affects order within the same `isTraversalGroup`. To control order between groups, set `traversalIndex` on the group containers, not on individual elements.

## Complete Imports

```kotlin
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import {your.package}.ui.theme.AppTheme
```
