# Window Size Classes

Window size classes are the canonical way to make layout decisions in Compose Multiplatform. They replace device-type detection (`isTablet`, screen density checks) with width/height breakpoints that respond to runtime changes (rotation, split-screen, fold/unfold).

## Dependencies

### Version Catalog (libs.versions.toml)

```toml
[libraries]
material3-adaptive = { module = "org.jetbrains.compose.material3.adaptive:adaptive", version.ref = "composeAdaptive" }
material3-adaptive-layout = { module = "org.jetbrains.compose.material3.adaptive:adaptive-layout", version.ref = "composeAdaptive" }
material3-adaptive-navigation = { module = "org.jetbrains.compose.material3.adaptive:adaptive-navigation", version.ref = "composeAdaptive" }
```

Use the JetBrains Compose Multiplatform adaptive artifacts (`org.jetbrains.compose.material3.adaptive`), NOT the AndroidX-only artifacts (`androidx.compose.material3.adaptive`). The JetBrains artifacts work on both Android and iOS.

### Shared Module build.gradle.kts

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.material3.adaptive)
            implementation(libs.material3.adaptive.layout)
            implementation(libs.material3.adaptive.navigation)
        }
    }
}
```

## Window Size Class API

### Reading Current Window Size Class

```kotlin
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.window.core.layout.WindowHeightSizeClass

@Composable
fun MyScreen() {
    val windowInfo = currentWindowAdaptiveInfo()
    val widthSizeClass = windowInfo.windowSizeClass.windowWidthSizeClass
    val heightSizeClass = windowInfo.windowSizeClass.windowHeightSizeClass

    when (widthSizeClass) {
        WindowWidthSizeClass.COMPACT -> CompactLayout()
        WindowWidthSizeClass.MEDIUM -> MediumLayout()
        WindowWidthSizeClass.EXPANDED -> ExpandedLayout()
        else -> ExpandedLayout() // LARGE, EXTRA_LARGE
    }
}
```

### Width Breakpoints

| Class | Width Range | Typical Devices |
|---|---|---|
| `COMPACT` | < 600dp | Phones portrait, small foldables folded |
| `MEDIUM` | 600dp -- 839dp | Tablets portrait, foldables unfolded, phones landscape |
| `EXPANDED` | 840dp -- 1199dp | Tablets landscape, desktop windows |
| `LARGE` | 1200dp -- 1599dp | Large desktop windows |
| `EXTRA_LARGE` | >= 1600dp | Ultra-wide displays |

### Height Breakpoints

| Class | Height Range | Typical Devices |
|---|---|---|
| `COMPACT` | < 480dp | Phones landscape |
| `MEDIUM` | 480dp -- 899dp | Phones portrait, tablets landscape |
| `EXPANDED` | >= 900dp | Tablets portrait, desktop windows |

### Breakpoint Check Methods

Use `isWidthAtLeastBreakpoint()` and `isHeightAtLeastBreakpoint()` for "at least" threshold checks instead of exact equality:

```kotlin
import androidx.window.core.layout.WindowSizeClass

@Composable
fun AdaptiveContent() {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass

    if (windowSizeClass.isWidthAtLeastBreakpoint(WindowWidthSizeClass.MEDIUM)) {
        // Applies to MEDIUM, EXPANDED, LARGE, EXTRA_LARGE
        TwoPaneLayout()
    } else {
        SinglePaneLayout()
    }
}
```

This is preferred over equality checks because new breakpoints (LARGE, EXTRA_LARGE) won't fall through to a compact fallback.

## Architecture: Passing Window Size Class Down

Read `currentWindowAdaptiveInfo()` once near the top of the composition tree and pass the result down as state. Do not call `currentWindowAdaptiveInfo()` in every leaf composable -- it scatters adaptive logic and makes testing harder.

```kotlin
@Composable
fun App() {
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val widthSizeClass = adaptiveInfo.windowSizeClass.windowWidthSizeClass

    AppTheme(windowSizeClass = widthSizeClass) {
        AppNavHost(widthSizeClass = widthSizeClass)
    }
}
```

Screen composables receive the class as a parameter:

```kotlin
@Composable
fun DashboardScreen(
    widthSizeClass: WindowWidthSizeClass,
    modifier: Modifier = Modifier,
) {
    when (widthSizeClass) {
        WindowWidthSizeClass.COMPACT -> DashboardCompactContent(modifier)
        else -> DashboardExpandedContent(modifier)
    }
}
```

## Responsive Token Overrides

Combine window size class with the design token system to scale spacing and sizing automatically. See kmp-compose-ui `design-tokens.md` for full token definitions.

```kotlin
import {your.package}.ui.theme.AppSpacing
import {your.package}.ui.theme.AppSizing
import {your.package}.ui.theme.LocalAppSpacing
import {your.package}.ui.theme.LocalAppSizing

@Composable
fun responsiveSpacing(widthSizeClass: WindowWidthSizeClass): AppSpacing =
    when (widthSizeClass) {
        WindowWidthSizeClass.COMPACT -> AppSpacing()
        WindowWidthSizeClass.MEDIUM -> AppSpacing(
            lg = 24.dp,
            xl = 32.dp,
            xxl = 40.dp,
            xxxl = 56.dp,
        )
        else -> AppSpacing(
            lg = 32.dp,
            xl = 40.dp,
            xxl = 48.dp,
            xxxl = 64.dp,
        )
    }

@Composable
fun responsiveSizing(widthSizeClass: WindowWidthSizeClass): AppSizing =
    when (widthSizeClass) {
        WindowWidthSizeClass.COMPACT -> AppSizing()
        WindowWidthSizeClass.MEDIUM -> AppSizing(
            buttonHeight = 52.dp,
            inputHeight = 60.dp,
        )
        else -> AppSizing(
            buttonHeight = 56.dp,
            inputHeight = 64.dp,
            topBarHeight = 72.dp,
        )
    }
```

Provide responsive tokens in the theme wrapper:

```kotlin
@Composable
fun AppTheme(
    windowSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.COMPACT,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAppSpacing provides responsiveSpacing(windowSizeClass),
        LocalAppSizing provides responsiveSizing(windowSizeClass),
    ) {
        MaterialTheme(/* colorScheme, typography, shapes */) {
            content()
        }
    }
}
```

After this setup, every composable that reads `AppTheme.spacing.*` or `AppTheme.sizing.*` automatically gets scaled values for the current window size -- no per-component branching needed.

## BoxWithConstraints for Component-Level Sizing

When window size class is too coarse for a specific component, use `BoxWithConstraints` to read pixel-level constraints:

```kotlin
@Composable
fun AdaptiveCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        val cardPadding = if (maxWidth > 400.dp) {
            AppTheme.spacing.xl
        } else {
            AppTheme.spacing.lg
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding),
            shape = RoundedCornerShape(AppTheme.corners.lg),
        ) {
            content()
        }
    }
}
```

Note: The `400.dp` threshold in `BoxWithConstraints` is a component-level layout breakpoint, not a design token. Layout breakpoints are distinct from visual tokens.

## Runtime Changes

Window size class is not static. It changes when:

- The device rotates
- The user enters or exits split-screen / multi-window mode
- A foldable device folds or unfolds
- A desktop window is resized

`currentWindowAdaptiveInfo()` is a `@Composable` function that triggers recomposition when the value changes. All layout decisions based on window size class automatically update.

## Android 16 (API 36) Implications

On API 36 with devices where `sw >= 600dp`:

- `android:screenOrientation` is **ignored** -- the app can be rotated freely
- `android:resizeableActivity="false"` is **ignored** -- the app can be resized
- Aspect ratio limits are **ignored** -- the app can be any aspect ratio

This means every screen **must** handle all window size classes correctly. There is no way to lock to portrait or prevent resizing on large-screen devices.

## Rules

1. **Always use `currentWindowAdaptiveInfo()`** from Material 3 adaptive. Do not use `LocalConfiguration.current.screenWidthDp` or custom breakpoint enums for primary layout decisions.
2. **Never use device type detection.** No `isTablet`, no `Configuration.SCREENLAYOUT_SIZE_LARGE`, no `DisplayMetrics.densityDpi` checks. Window size class is the only correct signal.
3. **Read window size class once at the top.** Pass it down as a parameter or provide it via `CompositionLocal`. Do not scatter `currentWindowAdaptiveInfo()` calls throughout the tree.
4. **Use `isWidthAtLeastBreakpoint()` for threshold checks.** This future-proofs against new breakpoints being added above EXPANDED.
5. **Handle at least three width classes.** Compact, Medium, and Expanded are the minimum. LARGE and EXTRA_LARGE can fall through to the Expanded layout.

## Imports

```kotlin
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.window.core.layout.WindowHeightSizeClass
```
