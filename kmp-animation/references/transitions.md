# Transition Animations

## AnimatedVisibility

Animate composable enter and exit with combinable transition effects.

### Basic Usage

```kotlin
@Composable
fun NotificationBanner(isVisible: Boolean, message: String) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(AppTheme.motion.durationMedium)) + slideInVertically(
            initialOffsetY = { -it },  // Slide from top
            animationSpec = tween(AppTheme.motion.durationMedium),
        ),
        exit = fadeOut(tween(AppTheme.motion.durationShort)) + slideOutVertically(
            targetOffsetY = { -it },  // Slide to top
            animationSpec = tween(AppTheme.motion.durationShort),
        ),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.lg),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(AppTheme.spacing.lg),
            )
        }
    }
}
```

### Enter Transitions

Combine with `+` operator:

```kotlin
// Fade + slide from bottom
fadeIn(tween(AppTheme.motion.durationMedium)) + slideInVertically(initialOffsetY = { it })

// Fade + expand from center
fadeIn(tween(AppTheme.motion.durationMedium)) + expandIn(expandFrom = Alignment.Center)

// Fade + scale up from 0
fadeIn(tween(AppTheme.motion.durationMedium)) + scaleIn(initialScale = 0f)

// Slide from left edge
slideInHorizontally(initialOffsetX = { -it })

// Expand vertically from top
expandVertically(expandFrom = Alignment.Top)
```

### Exit Transitions

```kotlin
// Fade + slide to bottom
fadeOut(tween(AppTheme.motion.durationShort)) + slideOutVertically(targetOffsetY = { it })

// Fade + shrink to center
fadeOut(tween(AppTheme.motion.durationShort)) + shrinkOut(shrinkTowards = Alignment.Center)

// Fade + scale to 0
fadeOut(tween(AppTheme.motion.durationShort)) + scaleOut(targetScale = 0f)

// Slide to right edge
slideOutHorizontally(targetOffsetX = { it })

// Shrink vertically to top
shrinkVertically(shrinkTowards = Alignment.Top)
```

### Deferred Entry with MutableTransitionState

Trigger the enter animation after composition:

```kotlin
@Composable
fun AppearOnLoad(content: @Composable () -> Unit) {
    val visibleState = remember { MutableTransitionState(false) }

    LaunchedEffect(Unit) {
        visibleState.targetState = true
    }

    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(tween(AppTheme.motion.durationLong)) + slideInVertically(
            initialOffsetY = { it / 3 },
            animationSpec = tween(AppTheme.motion.durationLong),
        ),
    ) {
        content()
    }
}
```

### Accessing Transition State Inside AnimatedVisibility

Use `transition.isRunning` or `transition.currentState` to coordinate child animations:

```kotlin
AnimatedVisibility(
    visible = isVisible,
    enter = fadeIn() + expandVertically(),
    exit = fadeOut() + shrinkVertically(),
) {
    // `this` is AnimatedVisibilityScope -- access the transition
    val isFullyVisible = transition.currentState == EnterExitState.Visible

    Column {
        Text(stringResource(Res.string.label_shown_during_animation)) // Res.string.label_shown_during_animation
        if (isFullyVisible) {
            // Only show after enter animation completes
            Button(onClick = onAction) { Text(stringResource(Res.string.action_action)) } // Res.string.action_action
        }
    }
}
```

## AnimatedContent

Animates between different content composables with content transforms.

### Basic Tab Switching

```kotlin
@Composable
fun TabContent(selectedTab: Int) {
    AnimatedContent(
        targetState = selectedTab,
        transitionSpec = {
            // Determine direction for slide
            val direction = if (targetState > initialState) 1 else -1

            slideInHorizontally(
                initialOffsetX = { fullWidth -> direction * fullWidth },
                animationSpec = tween(AppTheme.motion.durationMedium),
            ) + fadeIn(tween(AppTheme.motion.durationMedium)) togetherWith slideOutHorizontally(
                targetOffsetX = { fullWidth -> -direction * fullWidth },
                animationSpec = tween(AppTheme.motion.durationMedium),
            ) + fadeOut(tween(AppTheme.motion.durationMedium)) using SizeTransform(clip = false)
        },
        label = "tabContent",
    ) { tab ->
        when (tab) {
            0 -> HomeTabContent()
            1 -> SearchTabContent()
            2 -> ProfileTabContent()
        }
    }
}
```

### Counter Animation

```kotlin
@Composable
fun AnimatedCounter(count: Int, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = count,
        transitionSpec = {
            if (targetState > initialState) {
                // Count up: slide new number from bottom
                slideInVertically(initialOffsetY = { it }) + fadeIn() togetherWith
                    slideOutVertically(targetOffsetY = { -it }) + fadeOut()
            } else {
                // Count down: slide new number from top
                slideInVertically(initialOffsetY = { -it }) + fadeIn() togetherWith
                    slideOutVertically(targetOffsetY = { it }) + fadeOut()
            } using SizeTransform(clip = true)
        },
        label = "counter",
    ) { targetCount ->
        Text(
            text = "$targetCount",
            style = MaterialTheme.typography.displayLarge,
            modifier = modifier,
        )
    }
}
```

### Content with Different Sizes

`SizeTransform` smoothly animates the container size when incoming and outgoing content differ:

```kotlin
@Composable
fun ExpandableSection(isExpanded: Boolean, summary: String, details: String) {
    AnimatedContent(
        targetState = isExpanded,
        transitionSpec = {
            fadeIn(tween(AppTheme.motion.durationMedium)) togetherWith fadeOut(tween(AppTheme.motion.durationMedium)) using
                SizeTransform { initialSize, targetSize ->
                    if (targetState) {
                        // Expanding: animate height first, then width
                        keyframes {
                            durationMillis = AppTheme.motion.durationMedium
                            IntSize(initialSize.width, targetSize.height) at AppTheme.motion.durationShort
                        }
                    } else {
                        // Collapsing: animate width first, then height
                        keyframes {
                            durationMillis = AppTheme.motion.durationMedium
                            IntSize(targetSize.width, initialSize.height) at AppTheme.motion.durationShort
                        }
                    }
                }
        },
        label = "expandableSection",
    ) { expanded ->
        if (expanded) {
            Column(modifier = Modifier.padding(AppTheme.spacing.lg)) {
                Text(text = summary, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(AppTheme.spacing.sm))
                Text(text = details, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            Text(
                text = summary,
                modifier = Modifier.padding(AppTheme.spacing.lg),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
```

## Crossfade

Simpler alternative to `AnimatedContent` when you only need an alpha crossfade. Does not support sliding, scaling, or shared elements.

```kotlin
@Composable
fun ScreenCrossfade(currentRoute: Screen) {
    Crossfade(
        targetState = currentRoute,
        animationSpec = tween(AppTheme.motion.durationMedium),
        label = "screenCrossfade",
    ) { screen ->
        when (screen) {
            Screen.Login -> LoginScreen()
            Screen.Dashboard -> DashboardScreen()
            Screen.Settings -> SettingsScreen()
        }
    }
}
```

## updateTransition -- Coordinated Multi-Property Animations

Animate multiple properties in sync when a single state changes. All child animations share the same transition and are coordinated.

```kotlin
enum class CardState { Collapsed, Expanded }

@Composable
fun CoordinatedCard(
    state: CardState,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val transition = updateTransition(targetState = state, label = "cardTransition")

    val height by transition.animateDp(
        transitionSpec = {
            if (targetState == CardState.Expanded) {
                spring(dampingRatio = Spring.DampingRatioLowBouncy)
            } else {
                tween(durationMillis = AppTheme.motion.durationMedium)
            }
        },
        label = "height",
    ) { cardState ->
        if (cardState == CardState.Expanded) AppTheme.sizing.heroImageHeight else AppTheme.sizing.cardCollapsedHeight
    }

    val elevation by transition.animateDp(label = "elevation") { cardState ->
        if (cardState == CardState.Expanded) AppTheme.spacing.sm else AppTheme.sizing.strokeMedium
    }

    val contentAlpha by transition.animateFloat(label = "contentAlpha") { cardState ->
        if (cardState == CardState.Expanded) 1f else 0f
    }

    val cornerRadius by transition.animateDp(label = "cornerRadius") { cardState ->
        if (cardState == CardState.Expanded) AppTheme.corners.sm else AppTheme.corners.lg
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        shape = RoundedCornerShape(cornerRadius),
        onClick = onToggle,
    ) {
        Column(modifier = Modifier.padding(AppTheme.spacing.lg)) {
            Text(stringResource(Res.string.label_title), style = MaterialTheme.typography.titleMedium) // Res.string.label_title
            Spacer(Modifier.height(AppTheme.spacing.sm))
            Text(
                text = stringResource(Res.string.label_detailed_description), // Res.string.label_detailed_description
                modifier = Modifier.graphicsLayer { alpha = contentAlpha },
            )
        }
    }
}
```

### Using updateTransition with AnimatedVisibility and AnimatedContent

`updateTransition` provides `AnimatedVisibility` and `AnimatedContent` extensions:

```kotlin
val transition = updateTransition(targetState = isExpanded, label = "panelTransition")

transition.AnimatedVisibility(
    visible = { it },  // Lambda taking the target state
    enter = fadeIn() + expandVertically(),
    exit = fadeOut() + shrinkVertically(),
) {
    Text(stringResource(Res.string.label_coordinated_transition)) // Res.string.label_coordinated_transition
}

transition.AnimatedContent(
    transitionSpec = { fadeIn() togetherWith fadeOut() },
) { expanded ->
    if (expanded) {
        Text(stringResource(Res.string.label_expanded_content)) // Res.string.label_expanded_content
    } else {
        Text(stringResource(Res.string.label_collapsed)) // Res.string.label_collapsed
    }
}
```

## Complete Example: Bottom Sheet Appearance

```kotlin
@Composable
fun AnimatedBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    // Scrim
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(AppTheme.motion.durationMedium)),
        exit = fadeOut(tween(AppTheme.motion.durationMedium)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismiss,
                ),
        )
    }

    // Sheet content
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        ) + fadeIn(tween(AppTheme.motion.durationMedium)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(AppTheme.motion.durationMedium),
        ) + fadeOut(tween(AppTheme.motion.durationShort)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = AppTheme.corners.xl, topEnd = AppTheme.corners.xl))
                .background(MaterialTheme.colorScheme.surface)
                .padding(AppTheme.spacing.xl),
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = AppTheme.spacing.xxl, height = AppTheme.spacing.xs)
                    .clip(RoundedCornerShape(AppTheme.sizing.strokeMedium))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
            )
            Spacer(Modifier.height(AppTheme.spacing.lg))
            content()
        }
    }
}
```
