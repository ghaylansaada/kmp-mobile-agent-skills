# Shimmer and Skeleton Loading Patterns

Skeleton screens show the layout structure with animated placeholders while content loads. This feels faster than a spinner because users see the shape of the incoming content. All components use design tokens from [design-tokens.md](design-tokens.md) -- no hardcoded dp, color, string, or duration values.

## Shimmer Effect Brush

The shimmer effect is an animated gradient that sweeps across placeholder shapes. The animation duration uses `AppTheme.motion.durationExtraLong` for timing.

```kotlin
package {your.package}.ui.components.loading

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import {your.package}.ui.theme.AppTheme

@Composable
fun rememberShimmerBrush(): Brush {
    val shimmerBaseColor = MaterialTheme.colorScheme.surfaceVariant
    val shimmerHighlightColor = MaterialTheme.colorScheme.surface
    val duration = AppTheme.motion.durationExtraLong

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = duration,
                easing = AppTheme.motion.easingStandard,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_translate",
    )

    return remember(translateAnimation, shimmerBaseColor, shimmerHighlightColor) {
        Brush.linearGradient(
            colors = listOf(shimmerBaseColor, shimmerHighlightColor, shimmerBaseColor),
            start = Offset(translateAnimation - 200f, 0f),
            end = Offset(translateAnimation, 0f),
        )
    }
}
```

The brush is remembered with its animation value as a key so it does not allocate a new `Brush` on every frame unless the animation position changes.

## ShimmerBox Composable

A single animated placeholder rectangle. Use this to build skeleton layouts:

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import {your.package}.ui.theme.AppTheme

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    brush: Brush = rememberShimmerBrush(),
    accessibilityLabel: String = stringResource(Res.string.cd_loading_placeholder),
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(AppTheme.corners.sm))
            .background(brush)
            .semantics { contentDescription = accessibilityLabel },
    )
}
```

## Skeleton Placeholder Composables

Build skeleton screens by composing `ShimmerBox` elements that mirror the real content layout:

```kotlin
@Composable
fun ListItemSkeleton(
    modifier: Modifier = Modifier,
    brush: Brush = rememberShimmerBrush(),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = AppTheme.spacing.lg,
                vertical = AppTheme.spacing.md,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar placeholder
        ShimmerBox(
            modifier = Modifier
                .size(AppTheme.sizing.minTouchTarget)
                .clip(RoundedCornerShape(AppTheme.corners.full)),
            brush = brush,
        )
        Spacer(Modifier.width(AppTheme.spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            // Title placeholder
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(AppTheme.spacing.lg),
                brush = brush,
            )
            Spacer(Modifier.height(AppTheme.spacing.sm))
            // Subtitle placeholder
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(AppTheme.spacing.md),
                brush = brush,
            )
        }
    }
}

@Composable
fun ListSkeleton(
    itemCount: Int = 6,
    modifier: Modifier = Modifier,
) {
    val brush = rememberShimmerBrush()
    Column(modifier = modifier) {
        repeat(itemCount) {
            ListItemSkeleton(brush = brush)
        }
    }
}
```

Share a single `rememberShimmerBrush()` across all skeleton items in one screen to keep the shimmer animation synchronized.

## ContentWithPlaceholder

Switches between skeleton placeholder and real content based on loading state:

```kotlin
@Composable
fun ContentWithPlaceholder(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    placeholder: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val duration = AppTheme.motion.durationMedium
    AnimatedContent(
        targetState = isLoading,
        modifier = modifier,
        transitionSpec = {
            fadeIn(
                animationSpec = tween(
                    durationMillis = duration,
                    easing = AppTheme.motion.easingStandard,
                ),
            ) togetherWith fadeOut(
                animationSpec = tween(
                    durationMillis = duration,
                    easing = AppTheme.motion.easingStandard,
                ),
            )
        },
        label = "content_placeholder_crossfade",
    ) { loading ->
        if (loading) {
            placeholder()
        } else {
            content()
        }
    }
}
```

Usage in a screen:

```kotlin
@Composable
fun AccountListScreenContent(
    state: AccountListUiState,
    modifier: Modifier = Modifier,
) {
    ContentWithPlaceholder(
        isLoading = state.isLoading,
        modifier = modifier,
        placeholder = { ListSkeleton() },
    ) {
        LazyColumn {
            items(state.accounts, key = { it.id }) { account ->
                AccountRow(account = account)
            }
        }
    }
}
```

## PullToRefresh with Loading State

Combine pull-to-refresh with shimmer skeletons for refresh flows:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullToRefreshList(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullToRefreshState,
        modifier = modifier,
    ) {
        content()
    }
}
```

Note: During pull-to-refresh, show the real content with the refresh indicator rather than replacing with skeletons. Skeletons are for initial loads only.

## Inline Loading Indicator (Button)

Show a spinner inside a button during submission. See `AppPrimaryButton` in [components.md](components.md) for the full pattern. The key rules:

- Size the `CircularProgressIndicator` explicitly with `Modifier.size(AppTheme.sizing.iconSm)`
- Match `strokeWidth` to visual density with `AppTheme.spacing.xxs`
- Disable the button while loading with `enabled = enabled && !isLoading`

## Full-Screen Loading Overlay

A semi-transparent overlay that blocks interaction during a blocking operation:

```kotlin
@Composable
fun LoadingOverlay(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    val duration = AppTheme.motion.durationMedium
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = duration)),
        exit = fadeOut(animationSpec = tween(durationMillis = duration)),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f))
                .semantics {
                    contentDescription = message
                        ?: stringResource(Res.string.cd_loading_overlay)
                },
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(AppTheme.corners.lg),
                tonalElevation = AppTheme.elevation.md,
                modifier = Modifier.padding(AppTheme.spacing.xxl),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(AppTheme.spacing.xl),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(AppTheme.sizing.minTouchTarget),
                    )
                    message?.let {
                        Spacer(Modifier.height(AppTheme.spacing.lg))
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}
```

## Error State with Retry

See `ErrorState` and `ErrorStateFromThrowable` in [components.md](components.md) for full-screen error handling. The pattern:

- Display an error icon, title, message, and retry button
- All text via `stringResource(Res.string.*)`
- All colors via `MaterialTheme.colorScheme.*`
- Retry button uses `AppPrimaryButton`

## Empty State

See `EmptyState` in [components.md](components.md). The pattern:

- Display an icon, title, optional subtitle, and optional action button
- All spacing via `AppTheme.spacing.*`
- All colors via `MaterialTheme.colorScheme.*`

## Required String Resources

Components in this file reference the following string resource keys. Define them in `commonMain/composeResources/values/strings.xml`:

```xml
<resources>
    <string name="cd_loading_placeholder">Loading placeholder</string>
    <string name="cd_loading_overlay">Loading, please wait</string>
</resources>
```

## File Placement

```
commonMain/
  kotlin/{your.package}/
    ui/
      components/
        loading/
          ShimmerBrush.kt             <- rememberShimmerBrush()
          ShimmerBox.kt               <- ShimmerBox composable
          ListItemSkeleton.kt         <- ListItemSkeleton, ListSkeleton
          ContentWithPlaceholder.kt   <- ContentWithPlaceholder composable
          LoadingOverlay.kt           <- LoadingOverlay composable
```

## Rules

1. Share a single `rememberShimmerBrush()` instance across all skeleton items on a screen. Creating multiple brushes causes desynchronized animations.
2. Use `AppTheme.motion.durationExtraLong` for shimmer sweep duration. Shorter durations feel jittery; longer durations feel sluggish.
3. Use `remember` for the shimmer brush. Without it, a new `Brush.linearGradient` allocates on every frame, causing GC pressure.
4. Use `AnimatedContent` (not conditional `if`/`else`) when switching between skeleton and real content. This provides a smooth crossfade transition.
5. Show skeletons only for initial loads. During pull-to-refresh, keep the existing content visible with the refresh indicator.
6. Match skeleton shapes to real content layout (same heights, widths, corner radii). Mismatched skeletons feel more jarring than a simple spinner.
7. All shimmer colors must come from `MaterialTheme.colorScheme.*` so they adapt to dark theme automatically.
8. Limit skeleton item count to what fits on screen (typically 5-8 items). Rendering 50 skeleton items wastes resources.
