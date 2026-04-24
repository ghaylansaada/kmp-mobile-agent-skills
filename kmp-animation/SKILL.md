---
name: kmp-animation
description: >
  Compose Multiplatform animations — animate* APIs, AnimatedVisibility, AnimatedContent,
  shared element transitions, gesture-driven animations, Lottie via Compottie, spring/tween/keyframe
  specs, infinite transitions, and performance optimization. Activate when adding animations,
  transitions, loading indicators, gesture feedback, shared element transitions between screens,
  or optimizing animation performance.
compatibility: >
  KMP with Compose Multiplatform. Material 3.
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---

# KMP Animation

## When to use

- Adding enter/exit animations to composables (appear, disappear, slide, fade)
- Animating state changes (color, size, position, alpha transitions)
- Implementing content switching with animated transitions (tabs, pages, counters)
- Building loading spinners, pulsing indicators, or looping animations
- Adding gesture-driven animations (swipe-to-dismiss, drag-and-drop, fling)
- Implementing shared element transitions between screens (hero image, expanding card)
- Integrating Lottie animations via Compottie (loading, success, onboarding)
- Optimizing animation performance (reducing recomposition, GPU-accelerated transforms)
- Coordinating multiple property animations together (size + color + alpha)
- Adding spring physics, tween timing, or keyframe-based animation specs

## Depends on

- **kmp-compose-ui** -- Compose Multiplatform setup, screen patterns
- **kmp-navigation** -- Shared element transitions between navigation destinations

## Workflow

1. Choose the right animation API --> [animation-guide.md](references/animation-guide.md)
2. Implement state-driven animations --> [state-animations.md](references/state-animations.md)
3. Implement transition animations --> [transitions.md](references/transitions.md)
4. Add gesture-driven animations --> [gesture-animations.md](references/gesture-animations.md)
5. Implement shared element transitions --> [shared-elements.md](references/shared-elements.md)
6. Add Lottie animations with Compottie --> [lottie.md](references/lottie.md)
7. Optimize animation performance --> [performance.md](references/performance.md)

## Gotchas

1. **`animateContentSize()` must be placed BEFORE `padding()` in the modifier chain.** Otherwise padding animates too, causing the content to visually bounce or shift during the size transition. Always chain as `.animateContentSize().padding(16.dp)`, not `.padding(16.dp).animateContentSize()`.

2. **`AnimatedVisibility` with `MutableTransitionState(false)` starts immediately visible if you set `targetState = true` in the same composition.** The state change is synchronous, so the enter animation never plays. Use `LaunchedEffect(Unit) { state.targetState = true }` to defer the target change and trigger the animation.

3. **`SharedTransitionLayout` must wrap BOTH the source and target composables.** Shared elements outside the layout scope are invisible during the transition. When using with navigation, the `SharedTransitionLayout` must wrap the `NavHost`, not individual screens.

4. **`spring()` has no fixed duration -- it depends on physics.** You cannot query "how long will this spring take?". Use `tween(durationMillis = 300)` when you need exact timing for coordinated animations or accessibility announcements.

5. **`InfiniteTransition` animations stop when the composable leaves composition, but the `InfiniteTransition` object itself is not garbage collected if held externally.** If you store the transition in a ViewModel or singleton, the animation state leaks memory. Always create infinite transitions inside composable scope with `rememberInfiniteTransition()`.

6. **`Animatable.animateTo()` is a suspend function.** It must be called from a coroutine, typically `LaunchedEffect` or `rememberCoroutineScope().launch {}`. Calling it from a regular function causes a compilation error.

7. **Animating `fontSize` directly causes text to recompose on every frame.** Each frame produces a new `TextStyle`, triggering full text layout. Instead, use `Modifier.graphicsLayer { scaleX = scale; scaleY = scale }` which only transforms the render layer without recomposition.

8. **`animateFloatAsState` returns a `State<Float>`, not a raw `Float`.** Reading `.value` outside composition (e.g., in a click handler) captures the value once and does not update. Use the state directly in modifier lambdas like `Modifier.graphicsLayer { alpha = animatedAlpha.value }` to defer the read.

9. **iOS animations may appear janky if the animation frame budget exceeds 8ms (120Hz devices).** Complex animations that run fine on Android at 60Hz can stutter on iOS ProMotion displays. Profile with Xcode Instruments and simplify: reduce nested animated composables, prefer `graphicsLayer` over layout-triggering modifiers.

10. **`Crossfade` does NOT support shared element transitions.** Crossfade performs a simple alpha blend between two composables and has no concept of matched elements. Use `AnimatedContent` with `ContentTransform` for any transition that requires shared elements or directional sliding.

11. **`updateTransition` label parameter is for debugging only.** The label string appears in Android Studio's Animation Preview inspector but has no runtime effect. Do not use it for logic or state tracking.

12. **`animateDecay` requires a platform-specific `DecayAnimationSpec`.** Use `splineBasedDecay(density)` from `rememberSplineBasedDecay()` on Android. On iOS, the same API works but the deceleration curve may feel different from native UIKit momentum scrolling. Test fling behavior on both platforms.

## Assets

| Path | Load when... |
|---|---|
| [templates/animated-screen.kt.template](assets/templates/animated-screen.kt.template) | Screen template with AnimatedVisibility, animateContentSize, and state-driven animations |
| [snippets/animation-patterns.kt](assets/snippets/animation-patterns.kt) | Reusable animation patterns: loading spinner, fade-in list, expandable card, pulse effect |

## Validation

### A. Animation Correctness

- [ ] `animate*AsState` functions use appropriate `AnimationSpec` (spring for natural motion, tween for precise timing)
- [ ] `AnimatedVisibility` enter/exit transitions match the UX intent (fade for subtle, slide for directional)
- [ ] `AnimatedContent` uses `SizeTransform` when content sizes differ to avoid layout jumps
- [ ] `InfiniteTransition` created with `rememberInfiniteTransition()`, not stored in ViewModel or singleton
- [ ] `Animatable` animations launched from `LaunchedEffect` or coroutine scope, not from composition
- [ ] Spring specs document the chosen stiffness/damping rationale (e.g., bouncy for playful, stiff for snappy)
- [ ] `animateContentSize()` placed before `padding()` and `clip()` in modifier chain
- [ ] Shared element keys are unique and consistent between source and target composables

### B. Performance

- [ ] Transform animations (alpha, scale, rotation, translation) use `Modifier.graphicsLayer { }` lambda form
- [ ] State reads deferred to lambda modifiers: `Modifier.offset { IntOffset(x, 0) }` not `Modifier.offset(x.dp, 0.dp)`
- [ ] No `fontSize` animation via `TextStyle` -- use `graphicsLayer { scaleX; scaleY }` instead
- [ ] `derivedStateOf` used for computed animation values that depend on multiple states
- [ ] No object allocation inside animation frame callbacks (avoid `Offset()`, `Size()` creation per frame)
- [ ] Recomposition scope minimized: animated values read in the smallest possible composable
- [ ] Complex animations profiled on both Android and iOS -- frame budget under 8ms for 120Hz

### C. Accessibility

- [ ] Reduced motion support: check `LocalReducedMotion` or system accessibility settings and simplify/skip animations
- [ ] `AnimatedVisibility` content remains accessible to screen readers after animation completes
- [ ] Loading animations include `semantics { contentDescription = "Loading" }` for screen readers
- [ ] Animation duration does not block user interaction -- no animations longer than 1 second without skip option
- [ ] `InfiniteTransition` indicators have accessible descriptions (not just visual meaning)

### D. Integration

- [ ] Depends-on references match actual skill directory names (`kmp-compose-ui`, `kmp-navigation`)
- [ ] Shared element transitions integrate with navigation NavHost correctly (`SharedTransitionLayout` wraps `NavHost`)
- [ ] Lottie composition files placed in `composeResources/files/` and loaded via Compose resources
- [ ] Animation state holders use `@Stable` or `@Immutable` annotations for recomposition skipping
- [ ] Compottie dependency declared in version catalog, not hardcoded in build.gradle.kts
- [ ] Animation cleanup verified: no running animations after composable leaves composition
