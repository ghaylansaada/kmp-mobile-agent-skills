# VoiceOver and TalkBack

## How Compose Multiplatform Maps to Platform Accessibility Trees

Compose Multiplatform generates a semantics tree that platform-specific code translates into native accessibility trees:

- **Android:** Each semantic node becomes an `AccessibilityNodeInfo` in the Android accessibility framework. TalkBack reads this tree.
- **iOS:** Each semantic node maps to a `UIAccessibilityElement` (or a view implementing `UIAccessibility` protocols). VoiceOver reads this tree.

The Compose runtime manages a merged semantics tree. Only nodes with semantic properties (contentDescription, role, heading, clickable, etc.) appear in the merged tree. Composables without semantics are invisible to accessibility services.

### What Becomes a Node

```kotlin
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import {your.package}.ui.theme.AppTheme

// This Text IS a node (has inherent text semantics)
Text("Hello") // Screen reader announces: "Hello"

// This Column is NOT a node (no semantics)
Column { /* children */ }

// This Column IS a node (has semantics modifier)
Column(modifier = Modifier.semantics { heading() }) { /* children */ }

// This Box IS a node (clickable adds semantics)
Box(modifier = Modifier.clickable { }) { /* children */ }
```

## TalkBack Behavior (Android)

### Navigation Gestures

| Gesture | Action |
|---|---|
| Swipe right | Move to next element |
| Swipe left | Move to previous element |
| Double tap | Activate focused element |
| Swipe up then right | Open local context menu (custom actions) |
| Swipe down then right | Navigate to next heading |
| Swipe up then left | Navigate to previous heading |
| Three-finger swipe | Scroll |

### How TalkBack Announces Elements

TalkBack constructs announcements from semantic properties in this order:

1. `contentDescription` (if set, replaces default text)
2. Element text content
3. `stateDescription` (e.g., "Selected", "Expanded")
4. `role` (e.g., "Button", "Checkbox")
5. Hint (from `onClickLabel`: "Double tap to [label]")
6. `error()` (if set: "Error: [message]")

Example announcement: "Submit form, Button, Double tap to submit"

### TalkBack Local Context Menu

Custom accessibility actions appear in the local context menu:

```kotlin
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions

// List item with custom actions
Row(
    modifier = Modifier
        .semantics(mergeDescendants = true) {
            customActions = listOf(
                CustomAccessibilityAction(
                    label = stringResource(Res.string.cd_action_delete),
                ) {
                    onDelete()
                    true
                },
                CustomAccessibilityAction(
                    label = stringResource(Res.string.cd_action_share),
                ) {
                    onShare()
                    true
                },
            )
        }
        .clickable(
            onClickLabel = stringResource(Res.string.cd_action_open),
            onClick = onClick,
        )
        .fillMaxWidth()
        .padding(AppTheme.spacing.lg),
) {
    Text(text = itemTitle, style = MaterialTheme.typography.bodyLarge)
}
```

User flow: Focus item -> Swipe up then right -> Menu shows "Delete", "Share" -> Select action.

### TalkBack Reading Controls

TalkBack supports different reading granularity modes (changed by swiping up/down):

- **Headings** -- jumps between elements with `semantics { heading() }`
- **Links** -- jumps between clickable text
- **Characters / Words / Lines** -- fine-grained text navigation
- **Controls** -- jumps between interactive elements

Ensure heading hierarchy is meaningful for heading navigation.

## VoiceOver Behavior (iOS)

### Navigation Gestures

| Gesture | Action |
|---|---|
| Swipe right | Move to next element |
| Swipe left | Move to previous element |
| Double tap | Activate focused element |
| Two-finger Z-scrub | Back / Dismiss (escape) |
| Rotor (two-finger rotation) | Switch navigation mode |
| Swipe up/down (with rotor) | Navigate by selected rotor item |
| Three-finger swipe | Scroll |

### How VoiceOver Announces Elements

VoiceOver constructs announcements from:

1. `accessibilityLabel` (mapped from `contentDescription`)
2. `accessibilityValue` (mapped from `stateDescription`)
3. `accessibilityTraits` (mapped from `role` and other semantics)
4. `accessibilityHint` (mapped from `onClickLabel`)

Example announcement: "Submit form. Button. Double tap to submit."

### VoiceOver Rotor

The rotor is a two-finger rotation gesture that selects navigation modes:

- **Headings** -- navigates elements with `semantics { heading() }`
- **Actions** -- shows `CustomAccessibilityAction` items
- **Containers** -- navigates between `isTraversalGroup` groups
- **Links** -- navigates clickable text

Custom actions you define via `customActions` appear in the Actions rotor item.

### VoiceOver Escape Gesture

The two-finger Z-scrub (escape) gesture normally triggers "Back" navigation on iOS. In Compose Multiplatform, this gesture is NOT automatically connected to the navigation back stack. Handle it in the iOS platform layer:

```kotlin
// In iosMain ComposeUIViewController setup
// See platform-specific.md for full implementation
```

## Common Pitfalls

### Custom Drawn Elements Invisible to Screen Readers

Canvas-based custom drawing (e.g., charts, custom progress) produces no accessibility nodes. Manually annotate with semantics:

```kotlin
import androidx.compose.foundation.Canvas
import androidx.compose.ui.semantics.Role

// Bad: chart is invisible to screen readers
Canvas(modifier = Modifier.size(AppTheme.sizing.iconXl)) {
    // draw pie chart...
}

// Good: chart has semantic description
Canvas(
    modifier = Modifier
        .size(AppTheme.sizing.iconXl)
        .semantics {
            contentDescription = stringResource(
                Res.string.cd_chart_summary,
                completedPercent,
                remainingPercent,
            )
            role = Role.Image
        },
) {
    // draw pie chart...
}
```

### Overlapping Touch Targets

When elements overlap visually or their touch targets overlap, the screen reader may focus the wrong element or skip one entirely:

```kotlin
// Bad: FAB overlaps list item touch targets
// The last item in the list may be unreachable

// Good: Add bottom padding to list content to clear the FAB
LazyColumn(
    contentPadding = PaddingValues(bottom = AppTheme.sizing.iconXl + AppTheme.spacing.xl),
) {
    items(data) { item ->
        ListItem(item)
    }
}
```

## LiveRegion for Dynamic Content

LiveRegion causes screen readers to announce content changes automatically, without the user navigating to the element.

### Polite vs Assertive

```kotlin
// Polite: queued after current speech
// Use for: cart count, form status, non-urgent messages
Text(
    text = stringResource(Res.string.cart_count, itemCount),
    modifier = Modifier.semantics {
        liveRegion = LiveRegionMode.Polite
    },
)

// Assertive: interrupts current speech immediately
// Use for: critical errors, urgent alerts
// WARNING: Overuse is disruptive. Reserve for truly urgent content.
Text(
    text = stringResource(Res.string.connection_lost),
    modifier = Modifier.semantics {
        liveRegion = LiveRegionMode.Assertive
    },
    color = MaterialTheme.colorScheme.error,
)
```

### Debouncing Live Region Updates

A live region that recomposes frequently causes announcement spam:

```kotlin
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

// Bad: announces every keystroke
// Text(
//     text = "${text.length}/200",
//     modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
// )

// Good: only announces at thresholds
@Composable
fun CharacterCounter(
    currentLength: Int,
    maxLength: Int,
    modifier: Modifier = Modifier,
) {
    val thresholdText by remember(currentLength) {
        derivedStateOf {
            when {
                currentLength >= maxLength -> "$currentLength/$maxLength"
                currentLength >= maxLength - 10 -> "$currentLength/$maxLength"
                else -> null // no announcement for most typing
            }
        }
    }

    Text(
        text = "$currentLength/$maxLength",
        style = MaterialTheme.typography.bodySmall,
        color = if (currentLength >= maxLength) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = modifier.then(
            if (thresholdText != null) {
                Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            } else {
                Modifier
            },
        ),
    )
}
```

## announceForAccessibility Deprecation (Android 16)

`View.announceForAccessibility()` is deprecated in Android API 36 (Android 16). The recommended replacements are all available through Compose Semantics:

| Old Approach | Replacement |
|---|---|
| `announceForAccessibility("Loading complete")` | `semantics { liveRegion = LiveRegionMode.Polite }` on a Text that updates |
| `announceForAccessibility("Error: ...")` | `semantics { error("...") }` on the error field |
| `announceForAccessibility("Screen changed")` | `semantics { paneTitle = "..." }` on the screen container |

```kotlin
// Replacement for announceForAccessibility on screen change
@Composable
fun AnnouncingScreen(
    screenTitle: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .semantics { paneTitle = screenTitle }
            .padding(AppTheme.spacing.lg),
    ) {
        content()
    }
}
```

## Heading Semantics for Section Navigation

Headings enable screen reader users to jump between sections. Proper heading hierarchy is critical for large screens.

```kotlin
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(AppTheme.spacing.lg),
    ) {
        // Section heading
        Text(
            text = stringResource(Res.string.settings_section_account),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(AppTheme.spacing.md))
        // Setting items...

        Spacer(Modifier.height(AppTheme.spacing.xl))

        // Next section heading
        Text(
            text = stringResource(Res.string.settings_section_notifications),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(AppTheme.spacing.md))
        // Notification setting items...

        Spacer(Modifier.height(AppTheme.spacing.xl))

        // Next section heading
        Text(
            text = stringResource(Res.string.settings_section_privacy),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(AppTheme.spacing.md))
        // Privacy setting items...
    }
}
```

Screen reader users can then use heading navigation (TalkBack: swipe down then right; VoiceOver: rotor set to Headings, swipe down) to jump directly between sections.

## Complete Imports

```kotlin
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import {your.package}.ui.theme.AppTheme
```
