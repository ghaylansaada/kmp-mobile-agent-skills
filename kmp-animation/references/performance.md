# Animation Performance Optimization

## Use `graphicsLayer` for GPU-Accelerated Transforms

Transforms applied via `graphicsLayer` are handled by the GPU and do not trigger recomposition or relayout. This is the single most impactful optimization for animations.

### GPU-Accelerated (Preferred)

```kotlin
// Alpha, scale, rotation, translation -- all GPU-accelerated
Modifier.graphicsLayer {
    alpha = animatedAlpha.value
    scaleX = animatedScale.value
    scaleY = animatedScale.value
    rotationZ = animatedRotation.value
    translationX = animatedOffsetX.value
    translationY = animatedOffsetY.value
}
```

### Layout-Triggering (Avoid in Animations)

```kotlin
// These modifiers trigger relayout on every frame -- expensive
Modifier
    .padding(animatedPadding.value)     // Relayout
    .size(animatedSize.value)           // Relayout
    .offset(animatedX.value, animatedY.value)  // With Dp overload -- less efficient
```

### Comparison Table

| Property | GPU-Accelerated (`graphicsLayer`) | Layout-Triggering |
|---|---|---|
| Position | `translationX`, `translationY` | `Modifier.offset(x.dp, y.dp)` |
| Size | `scaleX`, `scaleY` | `Modifier.size()`, `Modifier.padding()` |
| Opacity | `alpha` | N/A (alpha is always graphicsLayer) |
| Rotation | `rotationX`, `rotationY`, `rotationZ` | N/A |
| Text size | `scaleX`, `scaleY` | `fontSize` in TextStyle |

## Defer State Reads to Lambda Modifiers

When the system reads an animated value, it subscribes to changes. Reading inside a lambda modifier defers the read to the layout or draw phase, avoiding recomposition.

### Correct: Deferred Read (Lambda Form)

```kotlin
val offsetX by animateIntAsState(targetValue = if (isActive) 200 else 0, label = "offset")

Box(
    modifier = Modifier
        // Lambda form: read is deferred to layout phase
        .offset { IntOffset(offsetX, 0) }
        .size(AppTheme.sizing.minTouchTarget)
        .background(MaterialTheme.colorScheme.primary),
)
```

### Incorrect: Immediate Read (Value Form)

```kotlin
val offsetX by animateDpAsState(targetValue = if (isActive) 200.dp else 0.dp, label = "offset")

Box(
    modifier = Modifier
        // Dp form: read during composition, triggers recomposition every frame
        .offset(x = offsetX, y = 0.dp)
        .size(AppTheme.sizing.minTouchTarget)
        .background(MaterialTheme.colorScheme.primary),
)
```

### drawBehind for Animated Drawing

```kotlin
val animatedColor by animateColorAsState(
    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
    label = "bgColor",
)

Box(
    modifier = Modifier
        .size(AppTheme.sizing.iconXl) // 100.dp equivalent — adjust token as needed
        // drawBehind reads state in draw phase -- no recomposition
        .drawBehind { drawRect(animatedColor) },
)
```

## Minimize Recomposition Scope

Animated values should be read in the smallest possible composable to limit the recomposition blast radius.

### Bad: Reading in Parent

```kotlin
@Composable
fun ParentScreen() {
    val alpha by animateFloatAsState(targetValue = if (isVisible) 1f else 0f, label = "alpha")

    // ENTIRE column recomposes on every animation frame
    Column {
        ExpensiveHeader()
        Text(modifier = Modifier.graphicsLayer { this.alpha = alpha }, text = stringResource(Res.string.label_animated)) // Res.string.label_animated
        ExpensiveFooter()
    }
}
```

### Good: Reading in Isolated Composable

```kotlin
@Composable
fun ParentScreen() {
    Column {
        ExpensiveHeader()
        AnimatedText(isVisible = isVisible)  // Only this recomposes
        ExpensiveFooter()
    }
}

@Composable
private fun AnimatedText(isVisible: Boolean) {
    val alpha by animateFloatAsState(targetValue = if (isVisible) 1f else 0f, label = "alpha")
    Text(modifier = Modifier.graphicsLayer { this.alpha = alpha }, text = stringResource(Res.string.label_animated)) // Res.string.label_animated
}
```

### Best: No Recomposition at All

```kotlin
@Composable
fun ParentScreen() {
    Column {
        ExpensiveHeader()
        // graphicsLayer lambda reads state in draw phase -- zero recomposition
        Text(
            modifier = Modifier.graphicsLayer { alpha = if (isVisible) 1f else 0f },
            text = stringResource(Res.string.label_animated), // Res.string.label_animated
        )
        ExpensiveFooter()
    }
}
```

## `derivedStateOf` for Computed Animation Values

Avoid recomputing derived values on every frame:

```kotlin
@Composable
fun ScrollHeader(scrollState: LazyListState) {
    // Without derivedStateOf: recomputes on every pixel scroll
    // val isScrolled = scrollState.firstVisibleItemIndex > 0

    // With derivedStateOf: only recomposes when the boolean changes
    val isScrolled by remember {
        derivedStateOf { scrollState.firstVisibleItemIndex > 0 }
    }

    val elevation by animateDpAsState(
        targetValue = if (isScrolled) AppTheme.spacing.sm else 0.dp,
        label = "headerElevation",
    )

    Surface(shadowElevation = elevation) {
        Text(stringResource(Res.string.label_header), modifier = Modifier.padding(AppTheme.spacing.lg))
    }
}
```

## Stable Types for Animation State

Annotate animation state holders to enable recomposition skipping:

```kotlin
@Stable
class AnimationState(
    val isExpanded: Boolean,
    val selectedIndex: Int,
) {
    // @Stable tells Compose: if the instance is the same, the values have not changed.
    // This prevents unnecessary recomposition of composables that receive this state.
}

@Immutable
data class AnimatedItemState(
    val alpha: Float,
    val scale: Float,
    val offsetY: Dp,
)
```

## Avoid Object Allocation in Animation Frames

Creating new objects every frame generates garbage collection pressure, causing frame drops.

### Bad: New Object Per Frame

```kotlin
// Creates a new IntOffset on every frame
Modifier.offset {
    IntOffset(
        x = animatedX.value.roundToInt(),
        y = animatedY.value.roundToInt(),
    )
}
```

This is generally acceptable because `IntOffset` is lightweight. But avoid creating heavier objects:

### Bad: Heavy Object Per Frame

```kotlin
// Creates a new Path on every frame -- very expensive
Modifier.drawBehind {
    val path = Path().apply {
        moveTo(0f, 0f)
        lineTo(size.width * progress, 0f)
        lineTo(size.width * progress, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(path, color)
}
```

### Good: Reuse Objects

```kotlin
// Reuse the Path object
val path = remember { Path() }

Modifier.drawBehind {
    path.reset()
    path.moveTo(0f, 0f)
    path.lineTo(size.width * progress, 0f)
    path.lineTo(size.width * progress, size.height)
    path.lineTo(0f, size.height)
    path.close()
    drawPath(path, color)
}
```

## Profiling Animations

### Android

1. **Layout Inspector** -- Enable "Show recomposition counts" to see which composables recompose during animation
2. **Composition tracing** -- Add `androidx.compose.runtime.tracing` dependency and use System Trace in Android Studio Profiler
3. **GPU rendering** -- Enable "Profile GPU Rendering" in developer options to visualize frame times

### iOS

1. **Xcode Instruments** -- Use the Time Profiler instrument to identify hot frames
2. **Metal System Trace** -- Check GPU utilization during animations
3. **Frame rate** -- Monitor with `CADisplayLink` or Xcode frame rate overlay to ensure 120fps on ProMotion

### Common Profiling Checklist

- [ ] No composable recomposes more than once per animation frame
- [ ] Frame render time stays under 16ms (60Hz) or 8ms (120Hz)
- [ ] No GC pauses during animation (check logcat for GC events)
- [ ] GPU rendering bar stays below the 16ms green line

## Common Mistakes

### 1. Animating Padding Instead of Offset

```kotlin
// BAD: padding triggers relayout of all children on every frame
Modifier.padding(start = animatedPadding)

// GOOD: offset only moves the render layer
Modifier.offset { IntOffset(x = animatedOffset.roundToInt(), y = 0) }
// OR
Modifier.graphicsLayer { translationX = animatedOffset }
```

### 2. Animating fontSize

```kotlin
// BAD: creates new TextStyle per frame, triggers text layout
Text(
    text = stringResource(Res.string.label_hello), // Res.string.label_hello
    fontSize = animatedFontSize.sp,  // Recompose + relayout every frame
)

// GOOD: scale the text render layer
Text(
    text = stringResource(Res.string.label_hello), // Res.string.label_hello
    style = MaterialTheme.typography.bodyMedium,
    modifier = Modifier.graphicsLayer {
        val scale = animatedScale
        scaleX = scale
        scaleY = scale
        transformOrigin = TransformOrigin(0f, 0f)
    },
)
```

### 3. Unstable Lambda in LazyColumn Items

```kotlin
// BAD: new lambda instance per item per recomposition
LazyColumn {
    items(list) { item ->
        AnimatedItem(
            onAnimate = { viewModel.animate(item.id) },  // New lambda every recomposition
        )
    }
}

// GOOD: stable callback reference
LazyColumn {
    items(list) { item ->
        val onAnimate = remember(item.id) { { viewModel.animate(item.id) } }
        AnimatedItem(onAnimate = onAnimate)
    }
}
```

### 4. Reading Animated State Outside graphicsLayer

```kotlin
// BAD: reads .value during composition -- recomposes on every frame
val scale by animateFloatAsState(targetValue = target, label = "scale")
Box(modifier = Modifier.size((48 * scale).dp))  // Recomposition every frame

// GOOD: read inside graphicsLayer -- draw phase only
val scale by animateFloatAsState(targetValue = target, label = "scale")
Box(
    modifier = Modifier
        .size(AppTheme.sizing.minTouchTarget)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
)
```

## Platform-Specific Considerations

### iOS

- **Skia rendering overhead:** Compose Multiplatform uses Skia on iOS, which is more CPU-intensive than Android's hardware-accelerated compositor. Budget extra headroom for animations.
- **120Hz ProMotion:** iPhone Pro devices run at 120Hz. Animations must complete frame work in under 8ms. Test with real devices, not simulators.
- **Memory pressure:** iOS is more aggressive about killing background apps. Complex animations that allocate memory may trigger low-memory warnings. Profile with Instruments.

### Android

- **Hardware acceleration:** Most `graphicsLayer` operations are fully hardware-accelerated. Android handles transform animations efficiently by default.
- **Older devices:** Budget for 60Hz (16ms frame budget) on mid-range and older devices. Test on a representative low-end device.
- **Compose Compiler metrics:** Generate Compose compiler reports (`-Pcompose.compiler.reports=true`) to verify stability annotations are effective.
