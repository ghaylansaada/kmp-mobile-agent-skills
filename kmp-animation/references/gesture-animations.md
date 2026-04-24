# Gesture-Driven Animations

## Animatable -- Imperative Animation Control

`Animatable` is the core API for gesture-driven animations. It provides `snapTo()` for instant updates during drag, `animateTo()` for animated settling, and `stop()` for interruption.

### Basic Draggable Card

```kotlin
@Composable
fun DraggableCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    Card(
        modifier = modifier
            .offset {
                IntOffset(
                    x = offsetX.value.roundToInt(),
                    y = offsetY.value.roundToInt(),
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        // Stop any ongoing animation when drag starts
                        coroutineScope.launch { offsetX.stop() }
                        coroutineScope.launch { offsetY.stop() }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch { offsetX.snapTo(offsetX.value + dragAmount.x) }
                        coroutineScope.launch { offsetY.snapTo(offsetY.value + dragAmount.y) }
                    },
                    onDragEnd = {
                        // Spring back to origin
                        coroutineScope.launch {
                            offsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium,
                                ),
                            )
                        }
                        coroutineScope.launch {
                            offsetY.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium,
                                ),
                            )
                        }
                    },
                )
            },
    ) {
        content()
    }
}
```

### Animatable with Bounds

Constrain animation values within limits:

```kotlin
@Composable
fun BoundedSlider(maxOffset: Float = 300f) {
    val offsetX = remember {
        Animatable(0f).also {
            // Set bounds so the value cannot go outside [-maxOffset, maxOffset]
            // updateBounds is not suspending, safe to call in remember
        }
    }

    LaunchedEffect(maxOffset) {
        offsetX.updateBounds(lowerBound = -maxOffset, upperBound = maxOffset)
    }

    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .size(AppTheme.sizing.minTouchTarget)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        coroutineScope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessLow))
                        }
                    },
                )
            },
    )
}
```

## Fling with animateDecay

After a drag ends with velocity, `animateDecay` continues the motion with deceleration.

```kotlin
@Composable
fun FlingableItem() {
    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val decaySpec = remember(density) { splineBasedDecay<Float>(density) }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .size(AppTheme.sizing.iconXl) // flingable item size
            .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(AppTheme.corners.md))
            .pointerInput(Unit) {
                val velocityTracker = VelocityTracker()

                detectHorizontalDragGestures(
                    onDragStart = { velocityTracker.resetTracking() },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                        velocityTracker.addPosition(
                            change.uptimeMillis,
                            change.position,
                        )
                    },
                    onDragEnd = {
                        val velocity = velocityTracker.calculateVelocity().x
                        coroutineScope.launch {
                            offsetX.animateDecay(
                                initialVelocity = velocity,
                                animationSpec = decaySpec,
                            )
                        }
                    },
                )
            },
    ) {
        Text(
            text = stringResource(Res.string.label_fling_me), // Res.string.label_fling_me
            modifier = Modifier.align(Alignment.Center),
            color = MaterialTheme.colorScheme.onTertiary,
        )
    }
}
```

## Swipe-to-Dismiss

```kotlin
@Composable
fun SwipeToDismissItem(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    // Track whether the item is dismissed for fade-out
    var isDismissed by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (isDismissed) 0f else 1f,
        animationSpec = tween(AppTheme.motion.durationShort),
        label = "dismissAlpha",
        finishedListener = { if (isDismissed) onDismiss() },
    )

    Box(
        modifier = modifier
            .graphicsLayer { this.alpha = alpha }
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .pointerInput(Unit) {
                val dismissThreshold = size.width * 0.4f

                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            if (kotlin.math.abs(offsetX.value) > dismissThreshold) {
                                // Dismiss: animate off-screen
                                val targetX = if (offsetX.value > 0) size.width.toFloat()
                                              else -size.width.toFloat()
                                offsetX.animateTo(
                                    targetValue = targetX,
                                    animationSpec = tween(AppTheme.motion.durationShort),
                                )
                                isDismissed = true
                            } else {
                                // Snap back to origin
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                    ),
                                )
                            }
                        }
                    },
                )
            },
    ) {
        // Background reveal (delete icon)
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.error)
                .padding(horizontal = AppTheme.spacing.xl),
            contentAlignment = if (offsetX.value > 0) Alignment.CenterStart
                               else Alignment.CenterEnd,
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(Res.string.cd_delete), // Res.string.cd_delete
                tint = MaterialTheme.colorScheme.onError,
            )
        }

        // Foreground content
        content()
    }
}
```

## AnchoredDraggable -- Snap to Predefined Positions

For draggable components that snap to discrete positions (bottom sheet, drawer, swipe actions).

```kotlin
enum class SheetValue { Hidden, HalfExpanded, Expanded }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnchoredBottomSheet(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current

    val anchoredDraggableState = remember {
        AnchoredDraggableState(
            initialValue = SheetValue.Hidden,
            positionalThreshold = { distance -> distance * 0.5f },
            velocityThreshold = { with(density) { 125.dp.toPx() } },
            snapAnimationSpec = spring(),
            decayAnimationSpec = splineBasedDecay(density),
        )
    }

    val sheetHeight = 600f
    val halfHeight = sheetHeight / 2

    LaunchedEffect(Unit) {
        anchoredDraggableState.updateAnchors(
            DraggableAnchors {
                SheetValue.Hidden at sheetHeight      // Fully off-screen
                SheetValue.HalfExpanded at halfHeight  // Half visible
                SheetValue.Expanded at 0f              // Fully visible
            },
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { sheetHeight.toDp() })
                .align(Alignment.BottomCenter)
                .offset {
                    IntOffset(
                        x = 0,
                        y = anchoredDraggableState.offset.roundToInt(),
                    )
                }
                .anchoredDraggable(
                    state = anchoredDraggableState,
                    orientation = Orientation.Vertical,
                )
                .clip(RoundedCornerShape(topStart = AppTheme.corners.xl, topEnd = AppTheme.corners.xl))
                .background(MaterialTheme.colorScheme.surface),
        ) {
            content()
        }
    }
}
```

## Spring-Back Animation

A common pattern where an element returns to its original position with physics:

```kotlin
@Composable
fun SpringBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scale = remember { Animatable(1f) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        coroutineScope.launch {
                            scale.animateTo(
                                targetValue = 0.9f,
                                animationSpec = tween(AppTheme.motion.durationShort),
                            )
                        }
                        val released = tryAwaitRelease()
                        coroutineScope.launch {
                            scale.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioHighBouncy,
                                    stiffness = Spring.StiffnessMedium,
                                ),
                            )
                        }
                        if (released) onClick()
                    },
                )
            },
    ) {
        content()
    }
}
```

## Pull-to-Refresh Animation

```kotlin
@Composable
fun PullToRefreshContainer(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val pullOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val refreshThreshold = 120f

    val rotation by animateFloatAsState(
        targetValue = if (isRefreshing) 360f else 0f,
        animationSpec = if (isRefreshing) {
            infiniteRepeatable(
                animation = tween(AppTheme.motion.durationExtraLong, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            )
        } else {
            tween(AppTheme.motion.durationMedium)
        },
        label = "refreshRotation",
    )

    Column(modifier = modifier) {
        // Pull indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(LocalDensity.current) { pullOffset.value.coerceAtLeast(0f).toDp() })
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (pullOffset.value > 0f || isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(AppTheme.sizing.iconMd)
                        .graphicsLayer { rotationZ = rotation },
                    strokeWidth = AppTheme.sizing.strokeMedium,
                    progress = {
                        if (isRefreshing) 0.75f
                        else (pullOffset.value / refreshThreshold).coerceIn(0f, 1f)
                    },
                )
            }
        }

        // Main content with drag detection
        Box(
            modifier = Modifier
                .weight(1f)
                .pointerInput(isRefreshing) {
                    if (isRefreshing) return@pointerInput
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            if (dragAmount > 0 || pullOffset.value > 0) {
                                change.consume()
                                val dampedDrag = dragAmount * 0.5f // Resistance
                                coroutineScope.launch {
                                    pullOffset.snapTo(
                                        (pullOffset.value + dampedDrag).coerceAtLeast(0f),
                                    )
                                }
                            }
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                if (pullOffset.value >= refreshThreshold) {
                                    onRefresh()
                                }
                                pullOffset.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                )
                            }
                        },
                    )
                },
        ) {
            content()
        }
    }
}
```
