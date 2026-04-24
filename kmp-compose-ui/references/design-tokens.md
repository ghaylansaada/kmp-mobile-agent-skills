# Design Token System

All visual values (spacing, sizing, corner radii, elevation) are defined as design tokens. Composables must never contain hardcoded `dp` values -- they must reference tokens through `AppTheme`.

## Spacing Tokens

```kotlin
package {your.package}.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class AppSpacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 48.dp,
)

val LocalAppSpacing = staticCompositionLocalOf { AppSpacing() }
```

## Sizing Tokens

```kotlin
@Immutable
data class AppSizing(
    val iconSm: Dp = 16.dp,
    val iconMd: Dp = 24.dp,
    val iconLg: Dp = 32.dp,
    val minTouchTarget: Dp = 48.dp,
    val buttonHeight: Dp = 48.dp,
    val inputHeight: Dp = 56.dp,
    val topBarHeight: Dp = 64.dp,
    val bottomBarHeight: Dp = 80.dp,
)

val LocalAppSizing = staticCompositionLocalOf { AppSizing() }
```

## Corner Radius Tokens

```kotlin
@Immutable
data class AppCorners(
    val sm: Dp = 4.dp,
    val md: Dp = 8.dp,
    val lg: Dp = 12.dp,
    val xl: Dp = 16.dp,
    val full: Dp = 100.dp,
)

val LocalAppCorners = staticCompositionLocalOf { AppCorners() }
```

## Elevation Tokens

```kotlin
@Immutable
data class AppElevation(
    val none: Dp = 0.dp,
    val sm: Dp = 2.dp,
    val md: Dp = 4.dp,
    val lg: Dp = 8.dp,
)

val LocalAppElevation = staticCompositionLocalOf { AppElevation() }
```

## Motion Tokens

```kotlin
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing

@Immutable
data class AppMotion(
    val durationShort: Int = 150,
    val durationMedium: Int = 300,
    val durationLong: Int = 500,
    val durationExtraLong: Int = 800,
    val easingStandard: Easing = FastOutSlowInEasing,
    val easingDecelerate: Easing = LinearOutSlowInEasing,
    val easingAccelerate: Easing = FastOutLinearInEasing,
)

val LocalAppMotion = staticCompositionLocalOf { AppMotion() }
```

## Theme Extension Object

Provides convenient access to all token sets inside `@Composable` scope:

```kotlin
object AppTheme {
    val spacing: AppSpacing
        @Composable get() = LocalAppSpacing.current

    val sizing: AppSizing
        @Composable get() = LocalAppSizing.current

    val corners: AppCorners
        @Composable get() = LocalAppCorners.current

    val elevation: AppElevation
        @Composable get() = LocalAppElevation.current

    val motion: AppMotion
        @Composable get() = LocalAppMotion.current
}
```

## Providing Tokens in the Theme

Wrap your `MaterialTheme` with `CompositionLocalProvider` so every descendant composable can access tokens via `AppTheme.*`:

```kotlin
@Composable
fun AppTheme(
    spacing: AppSpacing = AppSpacing(),
    sizing: AppSizing = AppSizing(),
    corners: AppCorners = AppCorners(),
    elevation: AppElevation = AppElevation(),
    motion: AppMotion = AppMotion(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAppSpacing provides spacing,
        LocalAppSizing provides sizing,
        LocalAppCorners provides corners,
        LocalAppElevation provides elevation,
        LocalAppMotion provides motion,
    ) {
        MaterialTheme(/* colorScheme, typography, shapes */) {
            content()
        }
    }
}
```

## Usage: Hardcoded vs Token

```kotlin
// WRONG -- hardcoded values
Modifier.padding(16.dp)
Spacer(modifier = Modifier.height(8.dp))
RoundedCornerShape(12.dp)
Modifier.size(48.dp)

// RIGHT -- design tokens
Modifier.padding(AppTheme.spacing.lg)
Spacer(modifier = Modifier.height(AppTheme.spacing.sm))
RoundedCornerShape(AppTheme.corners.lg)
Modifier.size(AppTheme.sizing.minTouchTarget)
```

```kotlin
// WRONG -- hardcoded strings
Text("Loading...")
Text("Something went wrong")
Button(onClick = onSubmit) { Text("Submit") }

// RIGHT -- Compose resources
Text(stringResource(Res.string.loading))
Text(stringResource(Res.string.generic_error))
Button(onClick = onSubmit) { Text(stringResource(Res.string.submit)) }
```

```kotlin
// WRONG -- hardcoded animation durations
animateFloatAsState(targetValue = alpha, animationSpec = tween(durationMillis = 300))
AnimatedVisibility(visible = show, enter = fadeIn(animationSpec = tween(durationMillis = 150)))

// RIGHT -- motion tokens
animateFloatAsState(targetValue = alpha, animationSpec = tween(durationMillis = AppTheme.motion.durationMedium))
AnimatedVisibility(visible = show, enter = fadeIn(animationSpec = tween(durationMillis = AppTheme.motion.durationShort)))
```

## Responsive Tokens

Override default token values based on `WindowSizeClass` to adapt spacing and sizing for different device form factors:

```kotlin
@Composable
fun responsiveSpacing(windowSizeClass: WindowSizeClass): AppSpacing = when (windowSizeClass) {
    WindowSizeClass.Compact -> AppSpacing()
    WindowSizeClass.Medium -> AppSpacing(
        lg = 24.dp,
        xl = 32.dp,
        xxl = 40.dp,
        xxxl = 56.dp,
    )
    WindowSizeClass.Expanded -> AppSpacing(
        lg = 32.dp,
        xl = 40.dp,
        xxl = 48.dp,
        xxxl = 64.dp,
    )
}

@Composable
fun responsiveSizing(windowSizeClass: WindowSizeClass): AppSizing = when (windowSizeClass) {
    WindowSizeClass.Compact -> AppSizing()
    WindowSizeClass.Medium -> AppSizing(
        buttonHeight = 52.dp,
        inputHeight = 60.dp,
    )
    WindowSizeClass.Expanded -> AppSizing(
        buttonHeight = 56.dp,
        inputHeight = 64.dp,
        topBarHeight = 72.dp,
    )
}
```

Use responsive tokens by computing the values in the theme wrapper:

```kotlin
@Composable
fun AppTheme(
    windowSizeClass: WindowSizeClass = WindowSizeClass.Compact,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAppSpacing provides responsiveSpacing(windowSizeClass),
        LocalAppSizing provides responsiveSizing(windowSizeClass),
        LocalAppCorners provides AppCorners(),
        LocalAppElevation provides AppElevation(),
        LocalAppMotion provides AppMotion(),
    ) {
        MaterialTheme(/* colorScheme, typography, shapes */) {
            content()
        }
    }
}
```

## File Placement

```
commonMain/
  kotlin/{your.package}/
    ui/
      theme/
        AppTheme.kt          <- AppTheme object, CompositionLocalProvider wrapper
        AppSpacing.kt         <- AppSpacing data class + LocalAppSpacing
        AppSizing.kt          <- AppSizing data class + LocalAppSizing
        AppCorners.kt         <- AppCorners data class + LocalAppCorners
        AppElevation.kt       <- AppElevation data class + LocalAppElevation
        AppMotion.kt          <- AppMotion data class + LocalAppMotion
        ResponsiveTokens.kt   <- responsiveSpacing(), responsiveSizing()
```

## Rules

1. Every token data class must be annotated with `@Immutable`. Without it, CompositionLocal reads trigger unnecessary recompositions because the Compose compiler cannot prove stability.
2. Use `staticCompositionLocalOf` (not `compositionLocalOf`) for tokens that rarely change at runtime. `staticCompositionLocalOf` invalidates the entire subtree on change (cheaper when changes are rare) while `compositionLocalOf` tracks readers individually (cheaper when changes are frequent). Design tokens almost never change after theme setup, so `staticCompositionLocalOf` is correct.
3. Never read `AppTheme.*` outside a `@Composable` scope. The properties read `CompositionLocal.current`, which is only available during composition.
4. Never hardcode `dp` literals in composable bodies. Always use `AppTheme.spacing.*`, `AppTheme.sizing.*`, or `AppTheme.corners.*`.
5. Never hardcode user-facing strings in composable bodies. Always use `stringResource(Res.string.*)` from Compose resources.
6. Never hardcode animation duration integers in composable bodies. Always use `AppTheme.motion.durationShort`, `AppTheme.motion.durationMedium`, `AppTheme.motion.durationLong`, or `AppTheme.motion.durationExtraLong`.
7. Never hardcode easing curves. Always use `AppTheme.motion.easingStandard`, `AppTheme.motion.easingDecelerate`, or `AppTheme.motion.easingAccelerate`.
