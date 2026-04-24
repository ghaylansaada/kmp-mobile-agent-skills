---
name: kmp-adaptive-ui
description: >
  Responsive and adaptive UI for Compose Multiplatform targeting phones, tablets, foldables,
  and different orientations. Covers Material 3 adaptive window size classes, canonical
  layouts (list-detail, supporting pane), NavigationSuiteScaffold for automatic nav bar/rail/drawer
  switching, foldable posture detection, edge-to-edge insets handling, and responsive layout
  patterns. Activate when building multi-form-factor support, implementing adaptive navigation,
  list-detail panes, handling orientation changes, or adding edge-to-edge compliance.
compatibility: >
  KMP with Compose Multiplatform, Material 3 Adaptive, Android 16+ (API 36) edge-to-edge mandate.
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---

# KMP Adaptive UI

## When to use

- Supporting tablets, foldables, or large-screen devices alongside phones
- Implementing responsive navigation that switches between bottom bar, rail, and drawer
- Building list-detail (master-detail) canonical layouts
- Building supporting pane layouts
- Handling orientation changes and window resizing (split-screen, desktop resize)
- Adding edge-to-edge support (mandatory on Android 16 / API 36)
- Detecting foldable device postures (tabletop, book mode)
- Making layouts adapt to Compact, Medium, Expanded, Large, and Extra-large window size classes
- Ensuring compliance with Android 16 large-screen requirements (orientation/resizability restrictions ignored on sw>=600dp)

## Depends on

- **kmp-compose-ui** -- Compose runtime, MaterialTheme, design tokens, AppTheme
- **kmp-navigation** -- Navigation setup for route-aware adaptive scaffolds

## Workflow

1. Add Material 3 adaptive dependencies and configure window size class providers --> [window-size-classes.md](references/window-size-classes.md)
2. Set up NavigationSuiteScaffold for automatic navigation mode switching --> [navigation-suite.md](references/navigation-suite.md)
3. Implement list-detail and supporting pane canonical layouts --> [list-detail.md](references/list-detail.md)
4. Configure edge-to-edge display and WindowInsets handling --> [edge-to-edge.md](references/edge-to-edge.md)
5. Add foldable device posture detection and hinge-aware layouts --> [foldable-support.md](references/foldable-support.md)
6. Apply responsive layout patterns (adaptive grids, padding, dialogs, RTL) --> [responsive-patterns.md](references/responsive-patterns.md)

## Gotchas

1. **Android 16 (API 36) ignores orientation and resizability restrictions on large screens.** Apps targeting API 36 on devices with sw>=600dp will have `screenOrientation`, `resizeableActivity=false`, and aspect ratio limits silently ignored. Every screen must render correctly in any orientation and window size. There is no opt-out.

2. **Edge-to-edge opt-out is removed on Android 16.** `enableEdgeToEdge()` is now the default and cannot be disabled. Apps that relied on system bar backgrounds to hide content overlap will see visual regressions. Every composable near screen edges must handle WindowInsets explicitly.

3. **`NavigationSuiteScaffold` requires the adaptive-navigation-suite artifact, not the base material3 artifact.** Using `material3` alone gives `Unresolved reference: NavigationSuiteScaffold`. Add `material3-adaptive-navigation-suite` to your dependencies.

4. **`currentWindowAdaptiveInfo()` returns live values that change at runtime.** Window size class can change during rotation, split-screen entry/exit, or fold/unfold. Any code that caches the initial value and never re-reads it will display incorrectly after configuration changes.

5. **Never use device type detection (`isTablet`, `Configuration.SCREENLAYOUT_SIZE_LARGE`) for layout decisions.** A phone in split-screen has compact width. A tablet in portrait may have medium width. A foldable changes class when folded/unfolded. Always use window size class from `currentWindowAdaptiveInfo()`.

6. **`NavigableListDetailPaneScaffold` back behavior must match your navigation strategy.** `PopUntilScaffoldValueChange` pops until the visible pane combination changes (natural for detail-closes-on-back). `PopUntilContentChange` pops until the content key changes (natural for item-by-item back). Choosing wrong causes either stuck panes or skipped items.

7. **Foldable hinge detection is Android-only.** `WindowInfoTracker` from Jetpack Window Manager has no iOS equivalent. Use `expect`/`actual` to provide fold state on Android and a no-fold default on iOS. Do not gate entire UI features on fold detection.

8. **`Modifier.systemBarsPadding()` applied to both Scaffold and its content causes double padding.** If `Scaffold` already applies insets via its `contentWindowInsets` parameter, child composables should not add `systemBarsPadding()` again. Choose one location for inset handling.

9. **`WindowInsets.ime` does not update on iOS.** The IME (keyboard) insets API is Android-specific in Compose Multiplatform. On iOS, keyboard avoidance is handled by the UIKit layer. Do not rely on `Modifier.imePadding()` working cross-platform without `expect`/`actual` guards.

10. **`NavigationSuiteScaffold` does not preserve back stack across navigation mode switches.** When the window transitions from compact (bottom bar) to expanded (rail/drawer) during rotation, the scaffold rebuilds. Ensure your navigation state is hoisted in a ViewModel or `rememberSaveable` to survive these transitions.

11. **Predictive back animation in `NavigableListDetailPaneScaffold` requires the activity to opt in.** Add `android:enableOnBackInvokedCallback="true"` in the Android manifest. Without it, the built-in predictive back animation in the list-detail scaffold does not trigger.

12. **RTL layout affects start/end padding but not left/right.** Using `Modifier.padding(start = ...)` correctly flips in RTL. Using `Modifier.padding(left = ...)` does not. Always use start/end for horizontal padding and alignment, never left/right.

## Assets

| Path | Load when... |
|---|---|
| [snippets/adaptive-scaffold.kt](assets/snippets/adaptive-scaffold.kt) | Complete adaptive scaffold with NavigationSuiteScaffold, window size class detection, responsive content area, all using AppTheme tokens and stringResource |
| [templates/adaptive-list-detail.kt.template](assets/templates/adaptive-list-detail.kt.template) | List-detail screen template with NavigableListDetailPaneScaffold, responsive item layout, back handling, all using AppTheme tokens and stringResource |

## Validation

### A. Window Size Class Compliance

- [ ] Window size class obtained from `currentWindowAdaptiveInfo()`, not device detection
- [ ] No `isTablet`, `Configuration.SCREENLAYOUT_SIZE_*`, or screen-density checks for layout decisions
- [ ] Layout adapts correctly for Compact, Medium, and Expanded width classes at minimum
- [ ] Window size class changes at runtime (rotation, split-screen, fold/unfold) are handled reactively
- [ ] Window size class passed as state from top-level, not scattered `currentWindowAdaptiveInfo()` calls in leaf composables

### B. Design Token and Resource Compliance

- [ ] Zero hardcoded dp values -- all spacing uses `AppTheme.spacing.*`, all sizing uses `AppTheme.sizing.*`, all corner radii use `AppTheme.corners.*`
- [ ] Zero hardcoded user-facing strings -- all text uses `stringResource(Res.string.*)`
- [ ] Zero hardcoded content descriptions -- all accessibility labels use `stringResource(Res.string.cd_*)`
- [ ] Zero hardcoded colors -- all colors use `MaterialTheme.colorScheme.*`
- [ ] Responsive token overrides applied via `CompositionLocalProvider` based on window size class

### C. Adaptive Navigation

- [ ] `NavigationSuiteScaffold` used for primary navigation (not manual bottom bar/rail switching)
- [ ] Navigation items use `stringResource` for labels and content descriptions
- [ ] Navigation mode correctly switches: bottom bar (Compact), rail (Medium), drawer (Expanded) or custom override
- [ ] Navigation state survives window size class transitions (rotation, fold/unfold)

### D. Canonical Layouts

- [ ] List-detail uses `NavigableListDetailPaneScaffold` (not manual two-pane logic)
- [ ] Back navigation strategy set explicitly (`PopUntilScaffoldValueChange` or `PopUntilContentChange`)
- [ ] Predictive back animation enabled via manifest attribute `android:enableOnBackInvokedCallback="true"`
- [ ] Single pane shown on Compact, side-by-side on Medium/Expanded

### E. Edge-to-Edge

- [ ] `enableEdgeToEdge()` called in `Activity.onCreate()` before `super.onCreate()`
- [ ] Content does not render behind system bars without explicit inset handling
- [ ] `Modifier.windowInsetsPadding()` or `Modifier.systemBarsPadding()` applied at correct level (not doubled)
- [ ] IME insets handled on Android; iOS keyboard avoidance delegated to UIKit
- [ ] No opaque system bar backgrounds that break edge-to-edge appearance

### F. Foldable Support

- [ ] Fold detection uses `expect`/`actual` pattern (Android `WindowInfoTracker`, iOS no-op)
- [ ] Tabletop posture splits content at hinge position
- [ ] Layout does not break when fold state changes at runtime
- [ ] No UI features gated exclusively on fold detection (graceful fallback on non-foldable devices)

### G. Responsive Patterns

- [ ] Horizontal padding/margins scale with window size class
- [ ] Grid column count adapts to available width
- [ ] Dialogs show as full-screen on Compact, centered dialog on Medium+
- [ ] Start/end used for horizontal layout (not left/right) to support RTL
- [ ] `BoxWithConstraints` used for component-level responsive sizing when window size class is insufficient

### H. Platform and Integration

- [ ] No `android.*` imports in `commonMain` code (except through `expect`/`actual`)
- [ ] Depends-on references match actual skill directory names
- [ ] Cross-references to kmp-accessibility, kmp-animation, kmp-compose-ui are accurate
- [ ] Templates and snippets compile against declared dependencies
