# Animation API Decision Guide

## Decision Tree

Ask yourself: **"What am I animating?"**

| Scenario | Recommended API | Why |
|---|---|---|
| Single value changes when state changes (color, size, alpha) | `animate*AsState` | Simplest API, automatic lifecycle |
| Composable appearing or disappearing | `AnimatedVisibility` | Built-in enter/exit transitions |
| Switching between different content | `AnimatedContent` | Content transform with size animation |
| Simple content switch with fade only | `Crossfade` | Lighter than AnimatedContent |
| Multiple properties animating together on state change | `updateTransition` | Coordinated multi-property animations |
| Looping animation (spinner, pulse, shimmer) | `rememberInfiniteTransition` | No manual restart, lifecycle-aware |
| Gesture-driven animation (drag, fling, swipe) | `Animatable` | Imperative control, snap/stop/animate |
| Custom physics or frame-level control | `TargetBasedAnimation` / `DecayAnimation` | Low-level, manual frame timing |

## API Quick Reference

### `animate*AsState` -- Simple State-Driven Animations

Animates a single value whenever the target changes. Returns `State<T>` that automatically updates.

```kotlin
val backgroundColor by animateColorAsState(
    targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.surface,
    animationSpec = tween(durationMillis = AppTheme.motion.durationMedium),
    label = "backgroundColor",
)

Box(
    modifier = Modifier
        .background(backgroundColor)
        .padding(AppTheme.spacing.lg),
)
```

Available variants:
- `animateColorAsState` -- Color transitions
- `animateFloatAsState` -- Alpha, scale, rotation
- `animateDpAsState` -- Size, padding, elevation
- `animateIntAsState` -- Integer values
- `animateIntOffsetAsState` -- Position (IntOffset)
- `animateOffsetAsState` -- Position (Offset)
- `animateSizeAsState` -- Size values
- `animateRectAsState` -- Rect bounds
- `animateValueAsState` -- Custom types with `TwoWayConverter`

### `AnimatedVisibility` -- Enter/Exit Animations

Animates composable appearance and disappearance with combinable transitions.

```kotlin
AnimatedVisibility(
    visible = isVisible,
    enter = fadeIn(animationSpec = tween(AppTheme.motion.durationMedium)) + slideInVertically(),
    exit = fadeOut(animationSpec = tween(AppTheme.motion.durationMedium)) + slideOutVertically(),
) {
    Card { Text(stringResource(Res.string.label_appear_disappear)) } // Res.string.label_appear_disappear
}
```

### `AnimatedContent` -- Content Switching

Animates between different composables with content transforms.

```kotlin
AnimatedContent(
    targetState = selectedTab,
    transitionSpec = {
        fadeIn(tween(AppTheme.motion.durationMedium)) togetherWith fadeOut(tween(AppTheme.motion.durationMedium)) using
            SizeTransform(clip = false)
    },
    label = "tabContent",
) { tab ->
    when (tab) {
        Tab.Home -> HomeContent()
        Tab.Search -> SearchContent()
        Tab.Profile -> ProfileContent()
    }
}
```

### `Crossfade` -- Simple Content Switch

Lightweight alternative to `AnimatedContent` when you only need a fade.

```kotlin
Crossfade(
    targetState = currentScreen,
    animationSpec = tween(AppTheme.motion.durationMedium),
    label = "screenCrossfade",
) { screen ->
    when (screen) {
        Screen.Login -> LoginScreen()
        Screen.Dashboard -> DashboardScreen()
    }
}
```

### `updateTransition` -- Coordinated Multi-Property

Animates multiple properties in sync when a single state changes.

```kotlin
val transition = updateTransition(targetState = isExpanded, label = "cardTransition")

val cardHeight by transition.animateDp(label = "height") { expanded ->
    if (expanded) AppTheme.sizing.heroImageHeight else AppTheme.sizing.cardCollapsedHeight
}
val cardElevation by transition.animateDp(label = "elevation") { expanded ->
    if (expanded) AppTheme.spacing.sm else AppTheme.sizing.strokeMedium
}
val contentAlpha by transition.animateFloat(label = "alpha") { expanded ->
    if (expanded) 1f else 0f
}
```

### `rememberInfiniteTransition` -- Looping Animations

For animations that repeat forever (loading spinners, pulsing indicators).

```kotlin
val infiniteTransition = rememberInfiniteTransition(label = "loading")

val alpha by infiniteTransition.animateFloat(
    initialValue = 0.3f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = AppTheme.motion.durationExtraLong),
        repeatMode = RepeatMode.Reverse,
    ),
    label = "pulseAlpha",
)
```

### `Animatable` -- Imperative Control

For gesture-driven animations where you need `snapTo`, `stop`, and `animateTo`.

```kotlin
val offsetX = remember { Animatable(0f) }

Box(
    modifier = Modifier
        .offset { IntOffset(offsetX.value.roundToInt(), 0) }
        .pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    coroutineScope.launch {
                        offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                    }
                },
                onHorizontalDrag = { _, dragAmount ->
                    coroutineScope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                },
            )
        },
)
```

## AnimationSpec Types

| Spec | Use Case | Key Parameters |
|---|---|---|
| `spring()` | Natural motion, bouncing | `dampingRatio`, `stiffness` |
| `tween()` | Precise timing, easing curves | `durationMillis`, `easing` |
| `snap()` | Instant change, no animation | `delayMillis` |
| `keyframes()` | Multi-step animation with timing | Values at specific millisecond marks |
| `repeatable()` | Finite loop | `iterations`, `animation`, `repeatMode` |
| `infiniteRepeatable()` | Infinite loop | `animation`, `repeatMode` |

### Spring Presets

```kotlin
// Stiffness (higher = faster settling)
Spring.StiffnessHigh        // 10_000f  -- Snappy, immediate feel
Spring.StiffnessMedium       // 1500f   -- Default
Spring.StiffnessMediumLow    // 400f    -- Gentle
Spring.StiffnessLow          // 200f    -- Slow, heavy feel
Spring.StiffnessVeryLow      // 50f     -- Very slow

// Damping (lower = more bounce)
Spring.DampingRatioHighBouncy     // 0.2f  -- Very bouncy
Spring.DampingRatioMediumBouncy   // 0.5f  -- Moderate bounce
Spring.DampingRatioLowBouncy      // 0.75f -- Slight bounce
Spring.DampingRatioNoBouncy       // 1.0f  -- No overshoot (critically damped)
```

### Tween Easing Options

```kotlin
tween(durationMillis = AppTheme.motion.durationMedium, easing = LinearEasing)
tween(durationMillis = AppTheme.motion.durationMedium, easing = AppTheme.motion.easingStandard)     // Default -- decelerate
tween(durationMillis = AppTheme.motion.durationMedium, easing = AppTheme.motion.easingDecelerate)   // Enter
tween(durationMillis = AppTheme.motion.durationMedium, easing = AppTheme.motion.easingAccelerate)    // Exit
tween(durationMillis = AppTheme.motion.durationMedium, easing = EaseInOutCubic)
tween(durationMillis = AppTheme.motion.durationMedium, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)) // Custom
```

### Keyframes Example

```kotlin
val alpha by animateFloatAsState(
    targetValue = if (isVisible) 1f else 0f,
    animationSpec = keyframes {
        durationMillis = AppTheme.motion.durationLong
        0f at 0 using LinearEasing        // Start invisible
        0.3f at AppTheme.motion.durationShort using AppTheme.motion.easingStandard  // Fade in quickly
        0.8f at AppTheme.motion.durationMedium                        // Almost visible at midpoint
        1f at AppTheme.motion.durationLong                          // Fully visible at end
    },
    label = "keyframeAlpha",
)
```
