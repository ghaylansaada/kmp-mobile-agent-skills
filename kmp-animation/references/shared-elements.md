# Shared Element Transitions

Shared element transitions create visual continuity between two screens by animating a common element (image, card, text) from its source position to its destination position.

## Setup

Shared element transitions in Compose Multiplatform use `SharedTransitionLayout` with `AnimatedContent` or `AnimatedVisibility`. The layout must wrap both the source and target composables.

## SharedTransitionLayout with AnimatedContent

### Basic List-to-Detail Pattern

```kotlin
@Composable
fun ListDetailScreen() {
    var selectedItem by remember { mutableStateOf<Item?>(null) }

    SharedTransitionLayout {
        AnimatedContent(
            targetState = selectedItem,
            transitionSpec = {
                fadeIn(tween(AppTheme.motion.durationMedium)) togetherWith fadeOut(tween(AppTheme.motion.durationMedium))
            },
            label = "listDetail",
        ) { item ->
            if (item == null) {
                ItemListContent(
                    items = sampleItems,
                    onItemClick = { selectedItem = it },
                    animatedVisibilityScope = this@AnimatedContent,
                    sharedTransitionScope = this@SharedTransitionLayout,
                )
            } else {
                ItemDetailContent(
                    item = item,
                    onBack = { selectedItem = null },
                    animatedVisibilityScope = this@AnimatedContent,
                    sharedTransitionScope = this@SharedTransitionLayout,
                )
            }
        }
    }
}
```

### List Composable with Shared Elements

```kotlin
@Composable
fun ItemListContent(
    items: List<Item>,
    onItemClick: (Item) -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope,
) {
    with(sharedTransitionScope) {
        LazyColumn {
            items(items, key = { it.id }) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(item) }
                        .padding(AppTheme.spacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.title,
                        modifier = Modifier
                            .size(AppTheme.sizing.iconXl)
                            .clip(RoundedCornerShape(AppTheme.corners.md))
                            .sharedElement(
                                state = rememberSharedContentState(key = "image-${item.id}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                            ),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(Modifier.width(AppTheme.spacing.lg))
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(
                                key = "title-${item.id}",
                            ),
                            animatedVisibilityScope = animatedVisibilityScope,
                        ),
                    )
                }
            }
        }
    }
}
```

### Detail Composable with Matching Shared Elements

```kotlin
@Composable
fun ItemDetailContent(
    item: Item,
    onBack: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope,
) {
    with(sharedTransitionScope) {
        Column(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppTheme.sizing.heroImageHeight)
                    .sharedElement(
                        state = rememberSharedContentState(key = "image-${item.id}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                    ),
                contentScale = ContentScale.Crop,
            )
            Column(modifier = Modifier.padding(AppTheme.spacing.lg)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(
                            key = "title-${item.id}",
                        ),
                        animatedVisibilityScope = animatedVisibilityScope,
                    ),
                )
                Spacer(Modifier.height(AppTheme.spacing.sm))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onBack, modifier = Modifier.padding(AppTheme.spacing.lg)) {
                Text(stringResource(Res.string.action_back)) // Res.string.action_back
            }
        }
    }
}
```

## `sharedElement` vs `sharedBounds`

| API | Use Case | Behavior |
|---|---|---|
| `Modifier.sharedElement()` | Same visual element in both screens (image, icon) | Morphs the element directly -- same content renders at intermediate positions |
| `Modifier.sharedBounds()` | Conceptually same element but different rendering (text size/style changes) | Animates the bounds (position + size) while crossfading between source and target content |

### sharedBounds Example -- Text Size Transition

```kotlin
// In list: small title
Text(
    text = item.title,
    style = MaterialTheme.typography.bodyLarge,
    modifier = Modifier.sharedBounds(
        sharedContentState = rememberSharedContentState(key = "title-${item.id}"),
        animatedVisibilityScope = animatedVisibilityScope,
    ),
)

// In detail: large title
Text(
    text = item.title,
    style = MaterialTheme.typography.headlineMedium,
    modifier = Modifier.sharedBounds(
        sharedContentState = rememberSharedContentState(key = "title-${item.id}"),
        animatedVisibilityScope = animatedVisibilityScope,
    ),
)
```

The bounds (position and size) animate smoothly while the text style crossfades.

## Custom boundsTransform

Control how the shared element bounds animate between source and target:

```kotlin
Modifier.sharedElement(
    state = rememberSharedContentState(key = "image-${item.id}"),
    animatedVisibilityScope = animatedVisibilityScope,
    boundsTransform = { initialBounds, targetBounds ->
        // Arc motion -- the element follows a curved path
        keyframes {
            durationMillis = AppTheme.motion.durationLong
            initialBounds at 0 using LinearEasing
            Rect(
                left = (initialBounds.left + targetBounds.left) / 2,
                top = minOf(initialBounds.top, targetBounds.top) - 100f,
                right = (initialBounds.right + targetBounds.right) / 2,
                bottom = (initialBounds.bottom + targetBounds.bottom) / 2,
            ) at AppTheme.motion.durationMedium
            targetBounds at AppTheme.motion.durationLong
        }
    },
)
```

## Navigation Integration

When using shared elements with navigation, wrap `NavHost` in `SharedTransitionLayout` and pass scopes through composable parameters.

```kotlin
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = ListRoute,
            modifier = modifier,
        ) {
            composable<ListRoute> {
                ItemListScreen(
                    onItemClick = { id -> navController.navigate(DetailRoute(id)) },
                    animatedVisibilityScope = this@composable,
                    sharedTransitionScope = this@SharedTransitionLayout,
                )
            }
            composable<DetailRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<DetailRoute>()
                ItemDetailScreen(
                    itemId = route.id,
                    onBack = { navController.popBackStack() },
                    animatedVisibilityScope = this@composable,
                    sharedTransitionScope = this@SharedTransitionLayout,
                )
            }
        }
    }
}
```

## Persistent UI During Transitions

Use `renderInSharedTransitionScopeOverlay` to keep UI elements (bottom bar, FAB) visible and non-animating during shared element transitions:

```kotlin
@Composable
fun MainScreen(sharedTransitionScope: SharedTransitionScope) {
    with(sharedTransitionScope) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.renderInSharedTransitionScopeOverlay(
                        zIndexInOverlay = 1f,
                    ),
                ) {
                    // Bottom nav items -- rendered on top of the transition
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentTab == tab,
                            onClick = { onTabSelected(tab) },
                            icon = { Icon(tab.icon, tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            },
        ) { paddingValues ->
            // Screen content participates in shared transitions
            NavHost(
                navController = navController,
                modifier = Modifier.padding(paddingValues),
            ) {
                // ...
            }
        }
    }
}
```

## Clipping During Transition

Control how shared elements are clipped during their transition:

```kotlin
Modifier.sharedElement(
    state = rememberSharedContentState(key = "card-${item.id}"),
    animatedVisibilityScope = animatedVisibilityScope,
    // Clip the shared element to its current animated bounds
    clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(AppTheme.corners.xl)),
)
```

## Cross-Platform Considerations

1. **iOS rendering performance:** Shared element transitions are CPU-intensive on iOS because Compose uses Skia rendering. Keep the number of simultaneously animating shared elements low (2-3 max). Prefer simple shapes over complex clipping.

2. **Image loading timing:** If the destination image has not loaded yet, the shared element may flash or show a placeholder. Use a memory cache (Coil `ImageLoader` with memory cache) to ensure the image is available immediately.

3. **Transition duration:** Keep shared element transitions between 300-500ms. Shorter feels jarring; longer feels sluggish. Use `tween(400, easing = FastOutSlowInEasing)` as a starting point.

4. **Back gesture:** On iOS, the system swipe-back gesture does not automatically integrate with shared element transitions. The transition only plays when using in-app navigation (back button, `popBackStack()`). On Android, predictive back gesture support is available via `AnimatedContent` integration.
