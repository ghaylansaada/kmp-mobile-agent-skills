# Responsive Layout and Accessibility

## WindowSizeClass

Package: `ui.components.layout` in `commonMain`

```kotlin
enum class WindowSizeClass {
    Compact,   // < 600dp (phones portrait)
    Medium,    // 600-839dp (tablets portrait, foldables)
    Expanded,  // >= 840dp (tablets landscape, desktops)
}

fun windowSizeClassFromWidth(width: Dp): WindowSizeClass = when {
    width < 600.dp -> WindowSizeClass.Compact
    width < 840.dp -> WindowSizeClass.Medium
    else -> WindowSizeClass.Expanded
}
```

## ResponsiveLayout

```kotlin
@Composable
fun ResponsiveLayout(
    modifier: Modifier = Modifier,
    content: @Composable BoxWithConstraintsScope.(WindowSizeClass) -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        content(windowSizeClassFromWidth(maxWidth))
    }
}
```

## Adaptive Helpers

These helpers use design tokens from `AppTheme` combined with `WindowSizeClass` to adapt layout. See [design-tokens.md](design-tokens.md) for responsive token overrides that achieve the same result via `CompositionLocalProvider`.

```kotlin
@Composable
fun adaptivePadding(windowSizeClass: WindowSizeClass): Dp = when (windowSizeClass) {
    WindowSizeClass.Compact -> AppTheme.spacing.lg
    WindowSizeClass.Medium -> AppTheme.spacing.xl
    WindowSizeClass.Expanded -> AppTheme.spacing.xxl
}

@Composable
fun adaptiveMaxWidth(windowSizeClass: WindowSizeClass): Dp = when (windowSizeClass) {
    WindowSizeClass.Compact -> Dp.Infinity
    WindowSizeClass.Medium -> 600.dp
    WindowSizeClass.Expanded -> 840.dp
}
```

Note: `adaptiveMaxWidth` returns layout constraint breakpoints (600dp, 840dp) which are part of the Material Design window class specification, not visual spacing. These are not design tokens.

## Accessibility Guidelines

### Content Descriptions

Every interactive component must have a `contentDescription` via the `semantics` modifier. Use `stringResource` for the label:

```kotlin
Button(
    onClick = onSubmit,
    modifier = Modifier
        .heightIn(min = AppTheme.sizing.minTouchTarget)
        .semantics { contentDescription = stringResource(Res.string.submit_form) },
) {
    Text(stringResource(Res.string.submit))
}
```

- **Interactive icons**: Provide meaningful `contentDescription`
- **Decorative icons**: Use `contentDescription = null`
- **Containers**: Do not set `contentDescription` on parent Column/Row when children have individual descriptions. Use `mergeDescendants = true` only for single announced elements.

### Error Announcements

Text fields must announce errors to screen readers:

```kotlin
OutlinedTextField(
    // ...
    modifier = Modifier.semantics {
        contentDescription = stringResource(Res.string.email_input)
        if (isError) error(errorMessage.orEmpty())
    },
)
```

### Touch Targets

All interactive elements must meet the minimum touch target size defined by `AppTheme.sizing.minTouchTarget` (48dp). Material 3 components handle this by default, but custom composables must explicitly set minimum size:

```kotlin
IconButton(
    onClick = onAction,
    modifier = Modifier.size(AppTheme.sizing.minTouchTarget),
) {
    Icon(
        Icons.Default.Close,
        contentDescription = stringResource(Res.string.close),
        modifier = Modifier.size(AppTheme.sizing.iconMd),
    )
}
```

### Focus and Navigation

- Dialogs must trap focus -- `AlertDialog` handles this automatically
- Bottom sheets must be dismissible via accessibility gestures
- Use `Modifier.focusRequester()` for programmatic focus management in forms

## Imports

```kotlin
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import {your.package}.ui.theme.AppTheme
```
