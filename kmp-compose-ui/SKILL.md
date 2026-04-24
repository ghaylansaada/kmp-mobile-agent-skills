---
name: kmp-compose-ui
description: >
  Compose Multiplatform UI layer and Material 3 design system. Covers App composable 
  entry points, design token system (spacing, sizing, corners, elevation, motion), dark theme 
  implementation, shimmer/skeleton loading patterns, screen composable patterns, reusable 
  Material 3 components (buttons, inputs, dialogs, loading states), responsive layout, 
  accessibility, performance optimization, @Preview support, and Koin injection in composables. 
  Activate when building screens, creating reusable components, setting up the Compose entry 
  point, implementing dark theme, adding loading placeholders, or implementing Material 3 
  design patterns.
compatibility: >
  KMP with Compose Multiplatform, Material 3.
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---

# KMP Compose UI

## When to use

- Creating new screen composables in the shared module
- Setting up or modifying the App composable entry point
- Configuring Android (MainActivity) or iOS (MainViewController) platform entry points
- Defining or modifying design tokens (spacing, sizing, corners, elevation, motion)
- Implementing dark theme with light/dark color schemes
- Implementing dynamic color support on Android 12+
- Adding shimmer/skeleton loading placeholders
- Building loading overlay, pull-to-refresh, or skeleton screen patterns
- Building reusable UI components (buttons, inputs, feedback, state displays) for the shared module
- Adding Material 3 components or @Preview support to screens
- Wiring Koin dependency injection inside @Composable functions
- Displaying field-level validation errors from ApiError on form inputs
- Showing loading, empty, or error states driven by PagingState
- Adding dialogs, snackbars, or bottom sheets with consistent Material 3 styling
- Implementing responsive layouts that adapt to phone/tablet screen sizes
- Ensuring accessibility (contentDescription, semantics) across all interactive elements
- Optimizing composable performance (stability, recomposition skipping, lambda stability)
- Choosing between remember, rememberSaveable, and rememberUpdatedState
- Scoping ViewModels per page in HorizontalPager

## Depends on

- **kmp-project-setup** -- Gradle config
- **kmp-architecture** -- Architecture patterns, state management
- **kmp-resources-management** -- Compose resources for drawables, strings
- **kmp-dependency-injection** -- Koin Compose injection

## Workflow

1. Add Compose Multiplatform and Material 3 dependencies --> [setup.md](references/setup.md)
2. Define design tokens (spacing, sizing, corners, elevation, motion) and AppTheme wrapper --> [design-tokens.md](references/design-tokens.md)
3. Implement dark theme with light/dark color schemes and dynamic color support --> [theming.md](references/theming.md)
4. Create App composable and platform entry points --> [app-and-entry-points.md](references/app-and-entry-points.md)
5. Build reusable Material 3 components --> [components.md](references/components.md)
6. Implement shimmer/skeleton loading and loading state patterns --> [loading-states.md](references/loading-states.md)
7. Create screen composables with state hoisting --> [screen-patterns.md](references/screen-patterns.md)
8. Implement responsive layout and accessibility --> [layout-and-a11y.md](references/layout-and-a11y.md)
9. Optimize composable performance --> [performance.md](references/performance.md)
10. Set up @Preview support --> [previews.md](references/previews.md)

## Gotchas

1. **`remember` uses identity (`===`) on iOS for mutable objects.** On iOS, Compose checks object identity rather than structural equality for `remember` keys. Passing a mutable list or map as a key causes recomposition on every frame. Always use immutable/data-class state or explicit `key()` calls.

2. **`LaunchedEffect` and configuration changes behave asymmetrically.** On Android, configuration changes (rotation, locale) destroy and recreate the Activity, cancelling all `LaunchedEffect` coroutines. On iOS there is no configuration change concept -- effects survive orientation changes. Guard one-shot effects with `remember { mutableStateOf(false) }` flags if they must not re-fire.

3. **`Modifier.pointerInput` has different touch-slop thresholds per platform.** Android uses the platform touch slop (~8dp) while iOS uses a separately calculated value that can differ. Custom gesture detectors (drag, long-press) may feel different cross-platform. Test touch interactions on both platforms explicitly.

4. **`dynamicColorScheme()` is Android-only.** Calling `dynamicColorScheme(context)` on iOS crashes at runtime. Always gate dynamic color behind an `expect`/`actual` function or `if (Build.VERSION.SDK_INT >= 31)`. Use a static color scheme as the cross-platform default.

5. **`ComposeUIViewController` factory lambda runs exactly once.** The lambda passed to `ComposeUIViewController { }` executes once and does not recompose. Do not place state-dependent branching inside it. Move all reactive logic inside the `App()` composable. `initKoin()` is safe here only because it is idempotent.

6. **`collectAsLazyPagingItems()` must be called inside the composable body.** Storing the result in a `remember` block or ViewModel causes `IllegalStateException` when navigating away. Always call it at the screen composable level as a local val. See **kmp-paging** skill for pagination patterns.

7. **`koinInject()` called inside `@Preview` crashes with `NoSuchElementException`.** Koin is not initialized in preview mode. Extract stateless UI into a separate content composable that accepts state as parameters, and preview that composable instead.

8. **iOS and Android render shadows, blur, and gradients differently.** Compose Multiplatform uses different rendering backends per platform. Pixel-level differences in anti-aliasing, shadows, and gradient fills are expected. Never rely on screenshot comparison tests being pixel-identical across platforms.

9. **TextField IME behavior differs on iOS.** `onImeAction` fires reliably on Android but may not trigger on iOS for all IME action types. The keyboard "Done" button may dismiss the keyboard without firing the callback. Always provide an explicit submit button alongside IME actions.

10. **`AnimatedVisibility` can drop frames on older iOS devices.** The Skia renderer is more CPU-intensive than Android's hardware compositor. Avoid nesting multiple `AnimatedVisibility` blocks or complex transition combinations. Prefer simple `fadeIn`/`fadeOut` over `expandVertically` + `fadeIn` combos. See **kmp-animation** skill for optimized animation patterns.

11. **AlertDialog is a Compose rendering, not native.** On iOS, `AlertDialog` does not use `UIAlertController` -- it renders as a Compose overlay with no native dismiss gesture (tap outside). If native look-and-feel is critical on iOS, use `expect`/`actual` to show `UIAlertController` instead.

12. **Bottom sheet gesture conflicts with iOS swipe-back navigation.** `ModalBottomSheet` intercepts horizontal swipes near the sheet edge, conflicting with iOS swipe-from-left-edge back gesture. Add horizontal padding to avoid gesture interception zones.

13. **Snackbar host must be inside a Scaffold.** Placing `SnackbarHost` outside of `Scaffold` causes it to render behind other content or not appear at all. Always pass it via the `snackbarHost` parameter of `Scaffold`.

14. **`Modifier.semantics` contentDescription on a container overrides children.** Setting `contentDescription` on a parent Column/Row makes TalkBack/VoiceOver skip individual child descriptions. Use `mergeDescendants = true` only for single announced elements, not for containers with multiple interactive children. See **kmp-accessibility** skill for full semantics patterns.

15. **Passing mutable collections to composable parameters triggers infinite recomposition.** `List<ApiError>` parameters are stable only when the list instance does not change. If the ViewModel emits a new `MutableList` on every state update, every composable receiving it recomposes. Always use persistent/immutable collections or `data class` wrappers that provide structural equality.

16. **`OutlinedTextField` supportingText slot recomposes independently of the field.** Changing the `supportingText` lambda reference on every recomposition (e.g., creating a new lambda inline) causes the supporting text to flicker. Hoist the error message into a `remember`/`derivedStateOf` to stabilize the lambda identity.

17. **`CircularProgressIndicator` inside a `Button` must be sized explicitly.** Without a `Modifier.size()`, the indicator expands to fill the button and pushes text off-screen. Always constrain it (e.g., `Modifier.size(AppTheme.sizing.iconSm)`) and match `strokeWidth` to the button's visual density.

18. **Use `staticCompositionLocalOf` (not `compositionLocalOf`) for design tokens.** Design tokens (spacing, sizing, corners, elevation) rarely change after theme setup. `staticCompositionLocalOf` invalidates the entire subtree on change, which is cheaper when changes are rare. `compositionLocalOf` tracks individual readers, which adds overhead for values that almost never change.

19. **Mark all design token data classes with `@Immutable`.** Without `@Immutable`, the Compose compiler cannot prove that `AppSpacing`, `AppSizing`, `AppCorners`, and `AppElevation` are stable. This causes every composable reading from `CompositionLocal` to recompose even when the token values have not changed.

20. **Never access `AppTheme.*` outside a `@Composable` scope.** `AppTheme.spacing`, `AppTheme.sizing`, and `AppTheme.corners` read `CompositionLocal.current`, which is only available during composition. Accessing them in a regular function, `init` block, or class constructor causes `IllegalStateException`.

21. **`dynamicColorScheme()` is Android-only and requires API 31+.** Calling it on iOS or on Android < 31 crashes at runtime. Always gate it behind `expect`/`actual` with a `Build.VERSION.SDK_INT >= 31` check on Android and a static fallback on iOS.

22. **`Color.White` and `Color.Black` are not theme-aware.** Using them directly produces invisible text or backgrounds when the user switches between light and dark themes. Always use `MaterialTheme.colorScheme.surface`, `onSurface`, `background`, or `onBackground` instead.

23. **Dark theme previews require explicit `darkTheme = true`.** The `@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)` annotation only affects Android Studio rendering. For Compose Multiplatform previews, pass `darkTheme = true` to `AppTheme` explicitly.

24. **Creating multiple `rememberShimmerBrush()` instances causes desynchronized shimmer.** Each brush runs its own animation independently. Create one brush per screen and pass it to all `ShimmerBox` composables to keep the shimmer sweep synchronized.

25. **Shimmer brush without `remember` allocates on every frame.** The `Brush.linearGradient()` call inside the shimmer composable must use `remember` with the animation value as a key. Without it, a new brush object is allocated 60 times per second, causing GC pressure and dropped frames.

26. **Never hardcode animation duration integers.** Use `AppTheme.motion.durationShort`, `durationMedium`, `durationLong`, or `durationExtraLong` instead of literal `150`, `300`, `500`, or `800`. This ensures consistent timing across all animations and allows global adjustment.

27. **`remember` does not survive configuration changes -- use `rememberSaveable` for user input.** Form fields, search queries, and selected tabs stored with `remember` reset to their initial value on screen rotation. Always use `rememberSaveable` for state the user expects to persist across configuration changes and process death.

28. **`List<T>` parameters cause recomposition on every frame.** Compose cannot prove `List<T>` is immutable (it might be `MutableList`). Replace with `ImmutableList<T>` from `kotlinx-collections-immutable` or wrap in an `@Immutable` data class. One dependency, zero runtime overhead, massive performance gain.

## Assets

| Path | Load when... |
|------|-------------|
| [references/setup.md](references/setup.md) | Adding Compose Multiplatform and Material 3 dependencies |
| [references/design-tokens.md](references/design-tokens.md) | Defining or modifying design tokens (spacing, sizing, corners, elevation, motion) |
| [references/theming.md](references/theming.md) | Implementing dark theme, dynamic color, light/dark color schemes, or theme toggle |
| [references/app-and-entry-points.md](references/app-and-entry-points.md) | Creating or modifying App composable root or Android/iOS platform entry points |
| [references/components.md](references/components.md) | Building reusable Material 3 components (buttons, inputs, dialogs, states) |
| [references/loading-states.md](references/loading-states.md) | Implementing shimmer/skeleton loading, loading overlays, or pull-to-refresh patterns |
| [references/screen-patterns.md](references/screen-patterns.md) | Creating screen composables with state hoisting and ViewModel integration |
| [references/layout-and-a11y.md](references/layout-and-a11y.md) | Implementing responsive layout, WindowSizeClass, or accessibility |
| [references/performance.md](references/performance.md) | Optimizing composable performance, stability, or recomposition |
| [references/previews.md](references/previews.md) | Setting up @Preview support or troubleshooting previews |
| [references/integration.md](references/integration.md) | Integrating with navigation, DI, resources, or image loading |
| [assets/templates/screen-composable.kt.template](assets/templates/screen-composable.kt.template) | Scaffolding a new screen composable with ViewModel injection, state collection, and loading/error handling |
| [assets/templates/feature-screen.kt.template](assets/templates/feature-screen.kt.template) | Scaffolding a complete feature screen with buttons, text fields, dialogs, snackbar, and state handling |
| [assets/snippets/form-with-validation.kt](assets/snippets/form-with-validation.kt) | Adding a form with client-side and API validation error binding via AppTextField |

## Validation

### A. Compose Correctness

- [ ] `@Composable` functions follow PascalCase naming convention
- [ ] State hoisting pattern used: screen composable collects state and delegates to stateless content composable
- [ ] `remember` uses correct keys to avoid stale closures
- [ ] `LaunchedEffect` keys match recomposition needs (not `Unit` unless truly fire-once)
- [ ] `@Stable` or `@Immutable` annotations noted for data classes passed to composables
- [ ] No side effects in composition (network calls, logging, state mutation outside `LaunchedEffect`/`SideEffect`)
- [ ] `Modifier` parameter is first optional parameter in public composables
- [ ] `@Preview` annotations present on stateless content composables (not on composables using `koinInject()`)
- [ ] Material 3 components used (`androidx.compose.material3.*`), not Material 2

### B. Design Token and Resource Compliance

- [ ] Zero hardcoded dp values in composables -- all spacing uses `AppTheme.spacing.*`, all sizing uses `AppTheme.sizing.*`, all corner radii use `AppTheme.corners.*`
- [ ] Zero hardcoded user-facing strings in composables -- all text uses `stringResource(Res.string.*)`
- [ ] Zero hardcoded colors in composables -- all colors use `MaterialTheme.colorScheme.*`
- [ ] Zero hardcoded fontSize/textStyle in composables -- all text styling uses `MaterialTheme.typography.*`
- [ ] Zero hardcoded animation duration integers -- all use `AppTheme.motion.duration*`
- [ ] Zero hardcoded easing curves -- all use `AppTheme.motion.easing*`
- [ ] All interactive touch targets >= 48dp (`AppTheme.sizing.minTouchTarget`)
- [ ] All design token data classes annotated with `@Immutable`
- [ ] CompositionLocal for tokens uses `staticCompositionLocalOf` (not `compositionLocalOf`)
- [ ] `AppTheme` wrapper used in previews (not bare `MaterialTheme`)

### C. UI Component Correctness

- [ ] Accessibility labels present -- every interactive component has `semantics { contentDescription = ... }`
- [ ] Decorative icons use `contentDescription = null`; interactive icons have meaningful descriptions
- [ ] `OutlinedTextField` used with `supportingText` slot for error display (not separate `Text` below)
- [ ] Dialog buttons use `TextButton` with correct `onConfirm`/`onDismiss` wiring
- [ ] `SnackbarHost` placed inside `Scaffold.snackbarHost`, not outside
- [ ] `ModalBottomSheet` includes `windowInsetsPadding(WindowInsets.navigationBars)` for safe area

### D. Performance

- [ ] `PagingData` flow uses `cachedIn(viewModelScope)` to survive configuration changes
- [ ] `collectAsLazyPagingItems()` called at screen composable level, not inside `remember`
- [ ] Avoid creating new lambda instances per recomposition in hot paths (use `remember` for callbacks in LazyColumn items)
- [ ] State classes are `data class` to enable structural equality for recomposition skipping
- [ ] No unnecessary recomposition triggers (mutable collections as state, unstable types in parameters)
- [ ] `CircularProgressIndicator` inside buttons is explicitly sized (`Modifier.size()`)
- [ ] `derivedStateOf` or `remember` used for computed values that should not trigger recomposition
- [ ] No mutable collections passed as composable parameters (use immutable lists)
- [ ] `remember` used for expensive calculations (regex, formatters, shape instances)
- [ ] `key` parameter provided for `LazyColumn`/`LazyRow` items
- [ ] `Modifier.graphicsLayer` used for GPU-accelerated transforms (alpha, scale, rotation) instead of recomposition
- [ ] No allocations in composable body without `remember` in hot paths (`listOf()`, `RoundedCornerShape()`)
- [ ] `LazyColumn`/`LazyRow` used for lists (never `Column` + `forEach` for large lists)
- [ ] `ImmutableList<T>` used instead of `List<T>` for composable parameters holding collections
- [ ] `rememberSaveable` used for user input that must survive configuration changes

### E. Dark Theme Compliance

- [ ] No `Color.White`, `Color.Black`, `Color.Gray`, or `Color.Red` in composable code -- all use `MaterialTheme.colorScheme.*`
- [ ] `Color(0xFF...)` literals appear only in `lightColorScheme()` and `darkColorScheme()` definitions
- [ ] `dynamicColorScheme()` gated behind `expect`/`actual` with `Build.VERSION.SDK_INT >= 31` check
- [ ] Both light and dark `@Preview` variants exist for color-sensitive screens
- [ ] Theme preference stored in DataStore, resolved to `darkTheme: Boolean` at `App` level
- [ ] Shimmer colors use `MaterialTheme.colorScheme.surfaceVariant` and `surface` (not hardcoded)

### F. Loading State Compliance

- [ ] Shimmer brush created once per screen via `rememberShimmerBrush()` and shared across all skeleton items
- [ ] Shimmer animation duration uses `AppTheme.motion.durationExtraLong`
- [ ] `ContentWithPlaceholder` uses `AnimatedContent` for crossfade between skeleton and content
- [ ] Skeleton shapes match real content layout dimensions (heights, widths, corner radii)
- [ ] Skeletons used for initial loads only -- pull-to-refresh shows existing content with indicator
- [ ] `LoadingOverlay` uses `MaterialTheme.colorScheme.scrim` for background (not hardcoded alpha)
- [ ] Skeleton item count limited to visible viewport (5-8 items, not unbounded)

### G. Security

- [ ] No hardcoded API keys, tokens, or secrets in composable code or preview data
- [ ] Preview sample data does not contain real user information

### H. Integration

- [ ] Koin module initialization order documented (`commonModules()` list)
- [ ] Platform entry points call `initKoin()` before any composable injection
- [ ] Resource access via `Res` class matches kmp-resources-management patterns
- [ ] ViewModel injection pattern (`koinInject()`) is consistent with project dependencies
- [ ] `AppTextField` shows error text when `fieldErrors` is non-empty
- [ ] `PagingStateHandler` renders correct composable for each `PagingState` value
- [ ] `ErrorSnackbarEffect` correctly maps `ApiCallException` to user-facing messages
- [ ] `ErrorStateFromThrowable` handles `InternetError`, `HttpError`, and generic errors
- [ ] Depends-on references match actual skill directory names
- [ ] Template types (`ApiError`, `PagingState`, `ApiCallException`) match actual source definitions
