---
name: kmp-accessibility
description: >
  Comprehensive accessibility for Compose Multiplatform apps targeting Android and iOS.
  Covers Compose Semantics API, content descriptions, screen reader support (TalkBack and
  VoiceOver), touch target compliance, dynamic type and font scaling, reduced motion,
  high contrast, focus management, platform accessibility tree mapping, custom actions,
  live regions, heading hierarchy, and traversal order. Activate when adding accessibility
  support, content descriptions, screen reader compatibility, touch targets, focus management,
  dynamic type support, semantic properties, or auditing accessibility compliance.
compatibility: >
  KMP with Compose Multiplatform. Material 3. Android TalkBack. iOS VoiceOver.
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---

# KMP Accessibility

## Scope

This skill covers the full accessibility surface for Compose Multiplatform applications:
semantic annotations, content descriptions, screen reader behavior on both platforms,
touch target sizing, visual accessibility (dynamic type, contrast, reduced motion, color
blindness), focus management, platform-specific accessibility APIs, and practical patterns
for building inclusive screens. All code uses design tokens from kmp-compose-ui and
localized strings from kmp-resources-management.

## When to use

- Adding accessibility support to new or existing screens
- Writing content descriptions for interactive and decorative elements
- Ensuring screen reader compatibility (TalkBack on Android, VoiceOver on iOS)
- Meeting touch target size requirements (48dp Android, 44pt iOS)
- Implementing focus management and keyboard navigation
- Supporting dynamic type and font scaling at largest accessibility sizes
- Adding reduced motion support for users who disable animations
- Ensuring sufficient color contrast ratios (WCAG AA)
- Building heading hierarchy for screen reader section navigation
- Adding live region announcements for dynamic content updates
- Implementing custom accessibility actions (swipe alternatives, drag-drop alternatives)
- Auditing existing screens for accessibility compliance
- Configuring traversal order and semantic grouping

## Depends on

- **kmp-compose-ui** -- Design tokens, Material 3 components, screen patterns
- **kmp-resources-management** -- stringResource for all accessibility labels

## Workflow

1. Annotate composables with semantic properties --> [semantics.md](references/semantics.md)
2. Add content descriptions to all interactive and informative elements --> [content-descriptions.md](references/content-descriptions.md)
3. Ensure touch targets and interaction patterns are accessible --> [touch-and-interaction.md](references/touch-and-interaction.md)
4. Implement visual accessibility (dynamic type, contrast, reduced motion) --> [visual-accessibility.md](references/visual-accessibility.md)
5. Validate screen reader behavior on both platforms --> [screen-readers.md](references/screen-readers.md)
6. Apply platform-specific accessibility APIs where Compose semantics are insufficient --> [platform-specific.md](references/platform-specific.md)
7. Use provided templates and snippets to scaffold accessible screens --> [Assets table](#assets)

## Gotchas

1. **`Modifier.semantics { contentDescription = "..." }` on a parent container silences all children.** Setting contentDescription on a Column or Row causes TalkBack/VoiceOver to announce only the parent label and skip every child element. Never set contentDescription on layout containers that hold multiple interactive children. Use `mergeDescendants = true` only when the entire group should be announced as a single unit (e.g., a list item with icon + text + timestamp).

2. **`clearAndSetSemantics {}` removes the entire subtree from the accessibility tree.** This modifier is useful for custom composables where you want to provide a single announcement, but it makes every child invisible to screen readers. If any child is independently interactive (a button inside a card, a link inside text), those actions are lost. Use `mergeDescendants = true` with `CustomAccessibilityAction` instead to preserve actions.

3. **`announceForAccessibility` is deprecated in Android 16 (API 36).** Code that calls `view.announceForAccessibility()` via Android interop will log a deprecation warning and may stop working in future releases. Use Compose semantics instead: `Modifier.semantics { liveRegion = LiveRegionMode.Polite }` for status updates, `paneTitle` for screen transitions, and `error()` for validation errors.

4. **`traversalIndex` does not work across `isTraversalGroup` boundaries.** Setting `traversalIndex` only affects reading order within the same traversal group. If two elements are in different groups, only the group-level order matters. To control cross-group order, set `traversalIndex` on the group containers, not on individual elements within them.

5. **Compose Multiplatform maps semantics to UIAccessibility differently than to AccessibilityNodeInfo.** On Android, `Role.Button` adds `ACTION_CLICK` to the AccessibilityNodeInfo. On iOS, it maps to `UIAccessibilityTraitButton`. But not all Role values have iOS equivalents -- `Role.DropdownList` has no direct UIAccessibilityTrait mapping and may be announced as a generic element. Always test on both platforms.

6. **Material 3 components already handle minimum touch targets -- adding explicit sizing creates double padding.** `Button`, `IconButton`, `Checkbox`, `Switch`, and `RadioButton` already enforce minimum 48dp touch targets internally. Adding `Modifier.sizeIn(minHeight = AppTheme.sizing.minTouchTarget)` on these components results in extra padding. Only apply explicit touch target sizing to custom composables that do not use Material 3 components.

7. **`liveRegion` on a composable that recomposes frequently causes screen reader spam.** If a `Text` with `LiveRegionMode.Polite` recomposes on every character typed (e.g., a character counter), TalkBack/VoiceOver announces every intermediate value. Debounce the value with `derivedStateOf` or `snapshotFlow { ... }.debounce()` before exposing it to a live region composable.

8. **`Modifier.clickable {}` without `onClickLabel` makes TalkBack announce "Double tap to activate" with no context.** Always provide `onClickLabel = stringResource(Res.string.action_xxx)` so the screen reader says "Double tap to [action]" instead of a generic prompt. This is especially important for list items where multiple items have the same visual appearance.

9. **`progressBarRangeInfo` with `current = 0f` and `range = 0f..0f` causes a division-by-zero crash on some accessibility services.** Always validate that `range.endInclusive > range.start` before setting progress semantics. For indeterminate progress, use `ProgressBarRangeInfo.Indeterminate` instead of a zero-range.

10. **iOS VoiceOver escape gesture (two-finger Z-scrub) is not automatically handled by Compose navigation.** On native iOS apps, the escape gesture triggers "Back". In Compose Multiplatform, the gesture is not connected to the navigation back stack by default. You must handle it explicitly in the iOS platform layer by mapping the escape gesture to the navigation controller's popBackStack.

11. **`Modifier.focusRequester()` does not move VoiceOver focus on iOS.** On Android, calling `focusRequester.requestFocus()` moves both keyboard focus and TalkBack focus. On iOS, it only moves keyboard focus -- VoiceOver cursor stays where it was. To move VoiceOver focus programmatically, you need platform-specific `UIAccessibility.post(notification: .screenChanged, argument: targetView)`.

12. **Screen readers ignore elements with `size = 0.dp`.** A composable with `Modifier.size(0.dp)` or `Modifier.requiredSize(0.dp)` is removed from the accessibility tree entirely. If you need a visually hidden element that screen readers still announce (e.g., a hidden heading for structure), use `Modifier.alpha(0f)` with a real size, or use an offscreen position instead of zero size.

## Assets

| Path | Load when... |
|---|---|
| [templates/accessible-screen.kt.template](assets/templates/accessible-screen.kt.template) | Screen template with semantic structure, content descriptions, touch targets, heading hierarchy, error and loading announcements |
| [snippets/accessibility-patterns.kt](assets/snippets/accessibility-patterns.kt) | Common accessibility patterns: icon buttons, decorative images, toggles, list items, errors, progress, headings, live regions |

## Validation

### A. Semantics Correctness

- [ ] Every interactive element has `semantics { contentDescription = stringResource(...) }` or an inherent label
- [ ] Decorative images and icons use `contentDescription = null`
- [ ] `mergeDescendants = true` used only for elements that should be announced as a single unit
- [ ] `clearAndSetSemantics {}` not used on containers with independently interactive children
- [ ] `Role` correctly assigned: Button, Checkbox, Switch, RadioButton, Tab, Image as appropriate
- [ ] `stateDescription` set on toggles and stateful components (e.g., "Selected", "Expanded")
- [ ] `heading()` applied to section headings for screen reader navigation
- [ ] `error()` used on text fields with validation errors
- [ ] `liveRegion` used for dynamic content updates (counters, status messages, toasts)
- [ ] `traversalIndex` and `isTraversalGroup` used only when default reading order is incorrect

### B. Screen Reader

- [ ] TalkBack reads all interactive elements in logical order (tested on Android device/emulator)
- [ ] VoiceOver reads all interactive elements in logical order (tested on iOS device/simulator)
- [ ] No orphaned elements that TalkBack/VoiceOver cannot reach via swipe navigation
- [ ] Custom actions provided as alternatives to gesture-based interactions (swipe-to-dismiss, drag-drop)
- [ ] Live region announcements verified: content updates are announced without re-focusing
- [ ] Heading navigation works: screen reader users can jump between sections via headings

### C. Touch Targets

- [ ] All interactive elements meet minimum 48dp touch target (AppTheme.sizing.minTouchTarget)
- [ ] Custom composables use `Modifier.sizeIn(minWidth = AppTheme.sizing.minTouchTarget, minHeight = AppTheme.sizing.minTouchTarget)`
- [ ] Material 3 components do NOT have redundant touch target sizing applied
- [ ] Touch targets do not overlap (causes wrong element to receive activation)
- [ ] Sufficient spacing between adjacent touch targets (minimum AppTheme.spacing.sm)

### D. Dynamic Type

- [ ] All text uses `MaterialTheme.typography.*` styles (sp-based, scales with system font size)
- [ ] Screen layout does not break at largest accessibility font size (tested with 200% font scaling)
- [ ] Long text wraps or truncates gracefully -- no horizontal overflow
- [ ] Fixed-height containers use `heightIn(min = ...)` instead of `height(...)` to accommodate larger text
- [ ] Icon sizes remain usable at largest font scale (icons should not scale with text unless intentional)

### E. Visual Accessibility

- [ ] Text-to-background contrast ratio meets WCAG AA (4.5:1 for normal text, 3:1 for large text)
- [ ] Non-text elements (icons, borders, focus indicators) meet 3:1 contrast ratio
- [ ] Information is never conveyed through color alone (always paired with icon, text, or pattern)
- [ ] Reduced motion respected: animations simplified or skipped when system reduced motion is enabled
- [ ] Focus indicators visible on all interactive elements during keyboard/switch navigation
- [ ] Dark theme tested for contrast compliance (not just light theme)

### F. Platform-Specific

- [ ] iOS VoiceOver escape gesture navigates back (handled in platform layer)
- [ ] Android TalkBack custom actions appear in local context menu
- [ ] `testTag` applied to key elements for both UI testing and accessibility tool identification
- [ ] No `announceForAccessibility` calls (deprecated in Android 16 -- use semantics instead)
- [ ] Platform-specific accessibility labels verified for natural-language correctness on both platforms
- [ ] Accessibility service detection used only for non-intrusive UI adjustments (never to gate functionality)
