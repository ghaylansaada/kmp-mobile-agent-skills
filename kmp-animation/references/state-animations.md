# State-Driven Animations

## `animate*AsState` Functions

These are the simplest animation APIs. Provide a target value, and Compose automatically animates from the current value to the target whenever it changes.

### Color Animation

```kotlin
@Composable
fun ThemedCard(isDarkMode: Boolean) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isDarkMode) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface,
        animationSpec = tween(durationMillis = AppTheme.motion.durationLong),
        label = "cardBackground",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isDarkMode) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = AppTheme.motion.durationLong),
        label = "cardContent",
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
        ),
    ) {
        Text(stringResource(Res.string.label_theme_aware_card), modifier = Modifier.padding(AppTheme.spacing.lg)) // Res.string.label_theme_aware_card
    }
}
```

### Size Animation

```kotlin
@Composable
fun ExpandableButton(isExpanded: Boolean, onClick: () -> Unit) {
    val width by animateDpAsState(
        targetValue = if (isExpanded) AppTheme.sizing.buttonExpanded else AppTheme.sizing.buttonCollapsed,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "buttonWidth",
    )

    Button(
        onClick = onClick,
        modifier = Modifier.width(width),
    ) {
        if (isExpanded) Text(stringResource(Res.string.label_expanded)) else Icon(Icons.Default.Add, stringResource(Res.string.cd_expand)) // Res.string.label_expanded, Res.string.cd_expand
    }
}
```

### Float Animation (Alpha, Scale, Rotation)

```kotlin
@Composable
fun PressableCard(content: @Composable () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "pressScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.8f else 1f,
        animationSpec = tween(durationMillis = AppTheme.motion.durationShort),
        label = "pressAlpha",
    )

    Card(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                )
            },
    ) {
        content()
    }
}
```

## `animateContentSize()` Modifier

Smoothly animates size changes when content changes. Place it **before** `padding()` and `clip()` in the modifier chain.

```kotlin
@Composable
fun ExpandableCard(title: String, description: String) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
            .padding(AppTheme.spacing.sm),
        onClick = { isExpanded = !isExpanded },
    ) {
        Column(modifier = Modifier.padding(AppTheme.spacing.lg)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            if (isExpanded) {
                Spacer(Modifier.height(AppTheme.spacing.sm))
                Text(text = description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
```

**Common mistake:** `.padding(AppTheme.spacing.lg).animateContentSize()` -- the padding is outside the animated area, causing the entire padded box to jump instantly while only inner content animates.

## Custom `TwoWayConverter`

Animate custom types by defining how to convert to/from `AnimationVector`.

```kotlin
data class CardState(val elevation: Float, val cornerRadius: Float)

val CardStateConverter = TwoWayConverter<CardState, AnimationVector2D>(
    convertToVector = { state ->
        AnimationVector2D(state.elevation, state.cornerRadius)
    },
    convertFromVector = { vector ->
        CardState(elevation = vector.v1, cornerRadius = vector.v2)
    },
)

@Composable
fun AnimatedCard(isHighlighted: Boolean) {
    val cardState by animateValueAsState(
        targetValue = if (isHighlighted) CardState(8f, 16f) else CardState(2f, 4f),
        typeConverter = CardStateConverter,
        animationSpec = spring(),
        label = "cardState",
    )

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = cardState.elevation.dp),
        shape = RoundedCornerShape(cardState.cornerRadius.dp),
    ) {
        Text(stringResource(Res.string.label_custom_animated_card), modifier = Modifier.padding(AppTheme.spacing.lg)) // Res.string.label_custom_animated_card
    }
}
```

## `AnimationSpec` Deep Dive

### Spring -- Natural Physics-Based Motion

Springs feel natural because they model real-world physics. Use them as the default for most UI animations.

```kotlin
// Snappy button press feedback
spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessHigh,
)

// Playful bouncing card
spring<Float>(
    dampingRatio = Spring.DampingRatioHighBouncy,
    stiffness = Spring.StiffnessMedium,
)

// Smooth drawer slide
spring<Dp>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessLow,
)
```

### Tween -- Precise Duration Control

Use when you need exact timing (coordinated with other animations, accessibility announcements, or design specs).

```kotlin
// Standard Material motion
tween<Float>(durationMillis = AppTheme.motion.durationMedium, easing = AppTheme.motion.easingStandard)

// Delayed entrance
tween<Float>(durationMillis = AppTheme.motion.durationMedium, delayMillis = AppTheme.motion.durationShort, easing = AppTheme.motion.easingDecelerate)

// Quick snap for micro-interactions
tween<Float>(durationMillis = AppTheme.motion.durationShort, easing = LinearEasing)
```

### Keyframes -- Multi-Step Animations

Define exact values at specific time points.

```kotlin
// Attention-grabbing shake animation
val shakeOffset by animateFloatAsState(
    targetValue = if (hasError) 1f else 0f,
    animationSpec = keyframes {
        durationMillis = AppTheme.motion.durationMedium
        0f at 0
        -10f at 50
        10f at 100
        -10f at 150
        10f at 200
        -5f at 250
        5f at 300
        0f at AppTheme.motion.durationMedium
    },
    label = "shake",
)

TextField(
    value = text,
    onValueChange = onTextChange,
    modifier = Modifier.offset { IntOffset(shakeOffset.roundToInt(), 0) },
)
```

### Repeatable and InfiniteRepeatable

```kotlin
// Pulsing loading indicator (3 pulses then stops)
val pulseAlpha by animateFloatAsState(
    targetValue = 1f,
    animationSpec = repeatable(
        iterations = 3,
        animation = tween(durationMillis = AppTheme.motion.durationLong),
        repeatMode = RepeatMode.Reverse,
    ),
    label = "pulse",
)

// Continuous rotation (loading spinner)
val infiniteTransition = rememberInfiniteTransition(label = "spinner")
val rotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = AppTheme.motion.durationExtraLong, easing = LinearEasing),
        repeatMode = RepeatMode.Restart,
    ),
    label = "rotation",
)
```

## Complete Example: Expanding Card with Coordinated Animations

```kotlin
@Composable
fun AnimatedExpandableCard(
    title: String,
    description: String,
    imageUrl: String,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }

    val elevation by animateDpAsState(
        targetValue = if (isExpanded) AppTheme.spacing.sm else AppTheme.sizing.strokeMedium,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "elevation",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isExpanded) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.outline,
        animationSpec = tween(AppTheme.motion.durationMedium),
        label = "borderColor",
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
            .border(
                width = AppTheme.sizing.strokeThin,
                color = borderColor,
                shape = RoundedCornerShape(AppTheme.corners.lg),
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        shape = RoundedCornerShape(AppTheme.corners.lg),
        onClick = { isExpanded = !isExpanded },
    ) {
        Column(modifier = Modifier.padding(AppTheme.spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Thumbnail always visible
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(AppTheme.sizing.iconLg).clip(CircleShape),
                )
                Spacer(Modifier.width(AppTheme.spacing.md))
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess
                                  else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) stringResource(Res.string.cd_collapse) else stringResource(Res.string.cd_expand),
                )
            }

            // Expanded content
            if (isExpanded) {
                Spacer(Modifier.height(AppTheme.spacing.md))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
```
