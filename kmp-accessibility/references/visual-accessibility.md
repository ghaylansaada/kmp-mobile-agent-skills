# Visual Accessibility

## Dynamic Type and Font Scaling

Compose Multiplatform text sizes defined in `sp` (scaled pixels) automatically scale with the system font size setting. Both Android and iOS support font scaling up to 200% or larger for accessibility.

### Using Scalable Text

Always use `MaterialTheme.typography.*` styles which are defined in `sp`:

```kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import {your.package}.ui.theme.AppTheme

// Correct: uses MaterialTheme typography (sp-based, scales automatically)
Text(
    text = stringResource(Res.string.welcome_title),
    style = MaterialTheme.typography.headlineMedium,
)

Text(
    text = stringResource(Res.string.welcome_body),
    style = MaterialTheme.typography.bodyLarge,
)
```

### Layout Resilience at Large Font Sizes

Fixed-height containers break when text scales beyond expectations. Use `heightIn(min = ...)` instead of `height(...)`:

```kotlin
// Bad: fixed height breaks at large font sizes
// Row(modifier = Modifier.height(56.dp)) { ... }

// Good: minimum height that expands with content
Row(
    modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = AppTheme.sizing.minTouchTarget)
        .padding(horizontal = AppTheme.spacing.lg),
    verticalAlignment = Alignment.CenterVertically,
) {
    Text(
        text = stringResource(Res.string.item_title),
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}
```

### Scrollable Containers for Scaled Content

At 200% font size, screens that fit without scrolling at normal size may overflow. Ensure scrollability:

```kotlin
@Composable
fun ScalableFormScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(AppTheme.spacing.lg),
    ) {
        Text(
            text = stringResource(Res.string.form_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(AppTheme.spacing.lg))
        // Form fields...
        // All content scrollable at largest font sizes
    }
}
```

### Text Truncation Strategy

When text may overflow, always define a truncation strategy:

```kotlin
// Title with ellipsis after 2 lines
Text(
    text = itemTitle,
    style = MaterialTheme.typography.titleMedium,
    maxLines = 2,
    overflow = TextOverflow.Ellipsis,
)

// Subtitle with single line truncation
Text(
    text = itemSubtitle,
    style = MaterialTheme.typography.bodySmall,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
)
```

## High Contrast Support

### WCAG AA Contrast Ratios

| Element Type | Minimum Contrast Ratio |
|---|---|
| Normal text (< 18sp, or < 14sp bold) | 4.5:1 |
| Large text (>= 18sp, or >= 14sp bold) | 3:1 |
| Non-text elements (icons, borders, focus indicators) | 3:1 |
| Disabled elements | No minimum (but recommended 2:1) |

### Using Theme Colors for Contrast Compliance

Material 3 color scheme pairs are designed to meet WCAG AA. Always use matching pairs:

```kotlin
import androidx.compose.material3.Surface

// on* colors are guaranteed to contrast with their surface
Surface(color = MaterialTheme.colorScheme.surface) {
    Text(
        text = stringResource(Res.string.body_text),
        color = MaterialTheme.colorScheme.onSurface, // 4.5:1+ against surface
    )
}

Surface(color = MaterialTheme.colorScheme.primaryContainer) {
    Text(
        text = stringResource(Res.string.highlight_text),
        color = MaterialTheme.colorScheme.onPrimaryContainer, // 4.5:1+ against primaryContainer
    )
}

// Error text on surface
Text(
    text = errorMessage,
    color = MaterialTheme.colorScheme.error, // designed for surface background
    style = MaterialTheme.typography.bodySmall,
)
```

### Custom Overlays and Scrims

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Color

// Scrim overlay: ensure text on top meets contrast
Box(
    modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)),
) {
    Text(
        text = stringResource(Res.string.overlay_title),
        color = MaterialTheme.colorScheme.inverseOnSurface,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(AppTheme.spacing.lg),
    )
}
```

## Reduced Motion

Users who experience vestibular disorders or motion sickness enable "Reduce Motion" (iOS) or "Remove animations" (Android). Your app must respect this setting.

### Detecting Reduced Motion in Compose Multiplatform

```kotlin
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable

// expect/actual for reduced motion detection
expect fun isReducedMotionEnabled(): Boolean

// Common composable that respects reduced motion
@Composable
fun MotionAwareAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (isReducedMotionEnabled()) {
        // Instant show/hide, no animation
        if (visible) {
            Box(modifier = modifier) { content() }
        }
    } else {
        AnimatedVisibility(
            visible = visible,
            modifier = modifier,
            enter = slideInVertically() + fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)),
        ) {
            content()
        }
    }
}
```

### Android actual

```kotlin
// androidMain
import android.provider.Settings

actual fun isReducedMotionEnabled(): Boolean {
    val context = /* application context from DI */
    val scale = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1.0f,
    )
    return scale == 0f
}
```

### iOS actual

```kotlin
// iosMain
import platform.UIKit.UIAccessibility

actual fun isReducedMotionEnabled(): Boolean {
    return UIAccessibility.isReduceMotionEnabled
}
```

### Simplifying Complex Animations

When reduced motion is enabled, replace complex animations with simple fades or instant transitions:

```kotlin
@Composable
fun AdaptiveTransition(
    targetState: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val reducedMotion = isReducedMotionEnabled()

    AnimatedVisibility(
        visible = targetState,
        modifier = modifier,
        enter = if (reducedMotion) {
            fadeIn(animationSpec = tween(0))
        } else {
            slideInVertically() + fadeIn(animationSpec = tween(300))
        },
        exit = if (reducedMotion) {
            fadeOut(animationSpec = tween(0))
        } else {
            fadeOut(animationSpec = tween(300))
        },
    ) {
        content()
    }
}
```

## Color-Blind Safety

Approximately 8% of men and 0.5% of women have some form of color vision deficiency. Never rely on color as the sole means of conveying information.

### Pair Color with Icons or Text

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon

// Bad: status conveyed by color alone
// Text(text = status, color = if (isError) Color.Red else Color.Green)

// Good: status conveyed by color + icon + text
@Composable
fun StatusIndicator(
    status: Status,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(AppTheme.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val (icon, tint, label) = when (status) {
            Status.Success -> Triple(
                Icons.Default.CheckCircle,
                MaterialTheme.colorScheme.primary,
                stringResource(Res.string.status_success),
            )
            Status.Warning -> Triple(
                Icons.Default.Warning,
                MaterialTheme.colorScheme.tertiary,
                stringResource(Res.string.status_warning),
            )
            Status.Error -> Triple(
                Icons.Default.Error,
                MaterialTheme.colorScheme.error,
                stringResource(Res.string.status_error),
            )
        }
        Icon(
            imageVector = icon,
            contentDescription = null, // text label provides the info
            tint = tint,
            modifier = Modifier.size(AppTheme.sizing.iconMd),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = tint,
        )
    }
}
```

### Form Validation Without Color Alone

```kotlin
// Error state includes icon + text + border color
OutlinedTextField(
    value = email,
    onValueChange = onEmailChange,
    label = { Text(stringResource(Res.string.label_email)) },
    isError = emailError != null,
    supportingText = emailError?.let {
        {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    modifier = Modifier.size(AppTheme.sizing.iconSm),
                    tint = MaterialTheme.colorScheme.error,
                )
                Text(it)
            }
        }
    },
    modifier = Modifier.fillMaxWidth(),
)
```

## Focus Indicators and Keyboard Navigation

Focus indicators must be visible for keyboard and switch-access users.

### Default Focus Indication

Material 3 components provide focus indicators automatically. For custom composables:

```kotlin
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

@Composable
fun FocusAwareCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .then(
                if (isFocused) {
                    Modifier.border(
                        width = AppTheme.sizing.strokeMedium,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(AppTheme.corners.md),
                    )
                } else {
                    Modifier
                },
            )
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClickLabel = stringResource(Res.string.cd_action_select),
                onClick = onClick,
            )
            .padding(AppTheme.spacing.lg),
    ) {
        content()
    }
}
```

### Programmatic Focus Management

```kotlin
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.LaunchedEffect

@Composable
fun ErrorFocusForm(
    emailError: String?,
    modifier: Modifier = Modifier,
) {
    val emailFocusRequester = remember { FocusRequester() }

    // Move focus to error field when error appears
    LaunchedEffect(emailError) {
        if (emailError != null) {
            emailFocusRequester.requestFocus()
        }
    }

    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        isError = emailError != null,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(emailFocusRequester),
    )
}
```

**Platform note:** On Android, `FocusRequester.requestFocus()` moves both keyboard focus and TalkBack focus. On iOS, it only moves keyboard focus -- VoiceOver cursor is unaffected. See [platform-specific.md](platform-specific.md) for iOS VoiceOver focus workarounds.

## Dark Theme Accessibility

Dark themes often introduce contrast issues that are not present in light themes.

### Testing Checklist for Dark Theme

```kotlin
// Ensure all color pairs maintain contrast in dark theme
@Composable
fun DarkThemeAccessibleCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(AppTheme.corners.md),
    ) {
        Column(modifier = Modifier.padding(AppTheme.spacing.lg)) {
            // Primary text: onSurfaceVariant on surfaceVariant -- verify 4.5:1
            Text(
                text = stringResource(Res.string.card_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(AppTheme.spacing.sm))
            // Secondary text: onSurfaceVariant on surfaceVariant -- verify 4.5:1
            Text(
                text = stringResource(Res.string.card_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
```

Common dark theme pitfalls:
- `onSurface` at reduced alpha (0.6f or lower) may not meet 4.5:1 contrast on `surface`
- `outline` color may be too close to `surface` in dark mode
- `error` on `errorContainer` may have different contrast than in light theme
- Card elevation shadows are invisible on dark backgrounds -- rely on surface tint instead

## Complete Imports

```kotlin
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalIndication
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import {your.package}.ui.theme.AppTheme
```
