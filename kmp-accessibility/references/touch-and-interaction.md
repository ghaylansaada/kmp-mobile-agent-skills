# Touch Targets and Interaction Accessibility

## Minimum Touch Target Sizes

The minimum touch target size is defined by `AppTheme.sizing.minTouchTarget` (48dp). This meets Android accessibility guidelines (48dp) and closely matches Apple HIG (44pt, approximately 44dp). All interactive elements must meet this minimum.

### Material 3 Components (Already Compliant)

These Material 3 components enforce minimum touch targets internally. Do NOT add redundant sizing:

- `Button`, `TextButton`, `OutlinedButton`, `ElevatedButton`
- `IconButton`, `FilledIconButton`, `OutlinedIconButton`
- `Checkbox`
- `Switch`
- `RadioButton`
- `Slider`
- `Tab`

### Custom Composables (Must Be Explicitly Sized)

```kotlin
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import {your.package}.ui.theme.AppTheme

// Custom clickable element with minimum touch target
@Composable
fun AccessibleCustomButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .sizeIn(
                minWidth = AppTheme.sizing.minTouchTarget,
                minHeight = AppTheme.sizing.minTouchTarget,
            )
            .clickable(
                onClickLabel = label,
                onClick = onClick,
            )
            .padding(AppTheme.spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
```

### Small Visual Elements with Large Touch Targets

When the visual element is smaller than the minimum touch target, expand the touch area without changing the visual size.

```kotlin
// Small icon with padded touch target
@Composable
fun AccessibleSmallIcon(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(AppTheme.sizing.minTouchTarget),
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = contentDescription,
            modifier = Modifier.size(AppTheme.sizing.iconSm),
        )
    }
}
```

### Inline Text Actions (Links)

```kotlin
// Ensure inline clickable text meets touch target height
@Composable
fun AccessibleTextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.primary,
        ),
        modifier = modifier
            .heightIn(min = AppTheme.sizing.minTouchTarget)
            .clickable(
                onClickLabel = text,
                onClick = onClick,
            )
            .padding(
                horizontal = AppTheme.spacing.xs,
                vertical = AppTheme.spacing.md,
            ),
    )
}
```

## Click Labels for Screen Readers

The `onClickLabel` parameter tells the screen reader what the click action does. Without it, TalkBack says "Double tap to activate" with no context.

```kotlin
// With click label: TalkBack says "Double tap to open settings"
Row(
    modifier = Modifier
        .fillMaxWidth()
        .clickable(
            onClickLabel = stringResource(Res.string.cd_action_open_settings),
            onClick = onOpenSettings,
        )
        .padding(AppTheme.spacing.lg),
) {
    Text(stringResource(Res.string.label_settings))
}

// Without click label: TalkBack says "Double tap to activate" (bad)
Row(
    modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onOpenSettings) // Missing onClickLabel!
        .padding(AppTheme.spacing.lg),
) {
    Text(stringResource(Res.string.label_settings))
}
```

## Custom Accessibility Actions

Custom actions provide screen reader users with alternatives to gesture-based interactions like swipe-to-dismiss, drag-and-drop, and long-press menus.

### Swipe-to-Dismiss Alternative

```kotlin
@Composable
fun SwipeToDismissItem(
    itemName: String,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val deleteActionLabel = stringResource(Res.string.cd_action_delete)
    val archiveActionLabel = stringResource(Res.string.cd_action_archive)

    Row(
        modifier = modifier
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(deleteActionLabel) {
                        onDelete()
                        true
                    },
                    CustomAccessibilityAction(archiveActionLabel) {
                        onArchive()
                        true
                    },
                )
            }
            .fillMaxWidth()
            .padding(AppTheme.spacing.lg),
    ) {
        content()
    }
}
```

On Android, these actions appear in TalkBack's local context menu (swipe up then right). On iOS, they appear in VoiceOver's actions rotor.

### Drag-and-Drop Alternative

```kotlin
@Composable
fun ReorderableListItem(
    itemName: String,
    index: Int,
    totalItems: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val moveUpLabel = stringResource(Res.string.cd_action_move_up)
    val moveDownLabel = stringResource(Res.string.cd_action_move_down)

    Row(
        modifier = modifier
            .semantics {
                customActions = buildList {
                    if (index > 0) {
                        add(
                            CustomAccessibilityAction(moveUpLabel) {
                                onMoveUp()
                                true
                            },
                        )
                    }
                    if (index < totalItems - 1) {
                        add(
                            CustomAccessibilityAction(moveDownLabel) {
                                onMoveDown()
                                true
                            },
                        )
                    }
                }
            }
            .fillMaxWidth()
            .padding(AppTheme.spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}
```

### Long Press Alternative

Long press is difficult for users with motor impairments. Always provide an alternative.

```kotlin
@Composable
fun LongPressItem(
    itemName: String,
    onClick: () -> Unit,
    onLongPressAction: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val moreOptionsLabel = stringResource(Res.string.cd_action_more_options)

    Row(
        modifier = modifier
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(moreOptionsLabel) {
                        onLongPressAction()
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
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
        // Visible alternative for long press
        IconButton(onClick = onLongPressAction) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(Res.string.cd_action_more_options),
            )
        }
    }
}
```

## Disabled State Handling

Disabled interactive elements must still be announced by screen readers with their disabled state.

```kotlin
import androidx.compose.material3.Button

// Material 3 Button handles disabled state automatically
Button(
    onClick = onSubmit,
    enabled = isFormValid,
) {
    Text(stringResource(Res.string.action_submit))
}
// TalkBack: "Submit, disabled, button" when isFormValid = false

// Custom disabled composable needs explicit handling
@Composable
fun CustomDisabledControl(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .sizeIn(
                minWidth = AppTheme.sizing.minTouchTarget,
                minHeight = AppTheme.sizing.minTouchTarget,
            )
            .then(
                if (enabled) {
                    Modifier.clickable(
                        onClickLabel = label,
                        onClick = onClick,
                    )
                } else {
                    Modifier.semantics {
                        role = Role.Button
                        contentDescription = label
                        stateDescription = stringResource(Res.string.cd_state_disabled)
                        // No clickable modifier: screen reader announces "disabled"
                    }
                },
            )
            .padding(AppTheme.spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
        )
    }
}
```

## Touch Target Spacing

Adjacent touch targets must have sufficient spacing to prevent accidental activation.

```kotlin
// Toolbar with properly spaced action buttons
Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = AppTheme.spacing.sm),
    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
) {
    IconButton(onClick = onEdit) {
        Icon(
            Icons.Default.Edit,
            contentDescription = stringResource(Res.string.cd_edit),
        )
    }
    IconButton(onClick = onDelete) {
        Icon(
            Icons.Default.Delete,
            contentDescription = stringResource(Res.string.cd_delete),
        )
    }
}
```

**Warning:** Overlapping touch targets cause the wrong element to receive activation. Use `Arrangement.spacedBy(AppTheme.spacing.sm)` as a minimum between adjacent interactive elements.
