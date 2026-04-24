# Predictive Back Gesture

## Overview

Android's predictive back gesture provides a system-level back animation that previews the destination before the user commits to navigating back. Starting with Android 13, apps can opt in to predictive back. Starting with Android 16, predictive back is mandatory -- the system no longer calls `onBackPressed` and the old `OnBackPressedCallback` dispatch is replaced by the new predictive back system.

## Manifest Opt-In

Enable predictive back in `AndroidManifest.xml` at the application level:

```xml
<application
    android:enableOnBackInvokedCallback="true"
    ...>
```

This flag is required on Android 13-15 for the predictive back animation to play. Without it, the back gesture works but skips the preview animation. On Android 16+, predictive back is always active regardless of this flag, but the flag should remain for backward compatibility.

## System Predictive Back Animations

The system provides three built-in animations that require no code changes:

- **Back-to-home**: When the user is on the root activity, the system shows a preview of the home screen behind the app. The app shrinks and moves toward the edge.
- **Cross-activity**: When navigating back to a previous activity, the system shows the previous activity behind the current one with a shared element transition.
- **Cross-task**: When navigating back to a previous task (e.g., returning to the app that launched this one), the system shows the previous task's top activity.

These animations are automatic once `enableOnBackInvokedCallback` is set to `true`.

## PredictiveBackHandler Composable

For custom back animations in Compose, use the `PredictiveBackHandler` composable. This replaces the older `BackHandler` for cases where a custom animation is needed during the back gesture progress:

The `PredictiveBackHandler` receives a `Flow<BackEventCompat>` that emits progress events as the user swipes. The flow completes when the user commits the back gesture or cancels with a `CancellationException`. Use the progress values to animate custom transitions (e.g., sheet dismissal, cross-fade).

For simple back interception without animation, `BackHandler` still works and is simpler.

## Migration from OnBackPressedDispatcher

Legacy code using `OnBackPressedDispatcher` and `OnBackPressedCallback` continues to function on Android 13-15 with the opt-in flag, but without the preview animation. On Android 16, `onBackPressed()` is no longer called by the system. Migration steps:

1. Replace `OnBackPressedCallback` with `BackHandler` (Compose) for simple interception.
2. Replace `OnBackPressedCallback` with `PredictiveBackHandler` (Compose) when custom animation during the gesture is needed.
3. For non-Compose code, use `OnBackInvokedCallback` registered via `OnBackInvokedDispatcher`.
4. Remove overrides of `Activity.onBackPressed()` -- they are dead code on Android 16.
5. Remove calls to `Activity.onBackPressed()` from custom navigation logic.

## Android 16 Breaking Changes

- **Predictive back is mandatory**: `onBackPressed()` is no longer invoked. Apps relying on `onBackPressed()` for navigation or confirmation dialogs must migrate.
- **`OnBackPressedCallback` still works** for intercepting back but does not animate. Only `PredictiveBackHandler` supports the gesture animation.
- **Testing**: On Android 16 devices with 3-button navigation, long-pressing the back button triggers the predictive back animation preview instead of immediately navigating back. This allows testing the animation without gesture navigation enabled.

## Compose Navigation Integration

Compose Navigation handles predictive back automatically when `enableOnBackInvokedCallback` is set. The navigation library provides cross-fade and shared element transitions for back navigation. No additional code is needed for standard navigation graph back-stack pops.

Custom `PredictiveBackHandler` is only needed when:
- A bottom sheet or dialog needs custom dismissal animation during the back gesture.
- A full-screen overlay needs a custom shrink/fade animation.
- The app needs to conditionally block back navigation (e.g., unsaved changes confirmation).

## Cross-Skill References

- **kmp-navigation** -- Navigation back-stack integration with predictive back is handled by the navigation library.
- **kmp-platform-integration** -- The manifest flag `android:enableOnBackInvokedCallback="true"` is documented in `references/setup.md`.
