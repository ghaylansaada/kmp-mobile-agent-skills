---
name: kmp-app-update
description: >
  Use this skill when implementing in-app update flows in a Kotlin Multiplatform
  project — version checking, force update gates, flexible update banners, Play
  Core In-App Updates on Android, iTunes Lookup API on iOS, or App Store review
  prompts. Activate when the user mentions "check for updates," "force update,"
  "app version," "update prompt," or "store review" in a KMP context. Does NOT
  cover OTA/CodePush-style hot updates, Firebase Remote Config, or A/B testing
  frameworks.
compatibility: >
  KMP with Android + iOS targets. Android requires Play Core library and Play Store installation.
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---

# KMP App Update

## Scope

Covers a complete in-app update system for KMP: shared types and expect/actual managers, Android Play Core integration (immediate and flexible flows), iOS iTunes Lookup API with App Store redirect, force update blocking screen, flexible update banner, ViewModel-driven state management, DI wiring, and testing with fakes. Does not cover OTA updates, remote config, or A/B testing.

## When to use

- Implementing in-app update prompts (flexible or immediate)
- Adding force-update gates for critical version requirements
- Checking app version against store listing (Play Store, App Store)
- Showing update-available dialogs with skip/remind-later options
- Integrating Play Core in-app updates API on Android
- Checking iTunes Lookup API for iOS version availability
- Handling update flow lifecycle across configuration changes
- Blocking app usage until a mandatory update is installed

## Depends on

- KMP project with Android and iOS targets (see `kmp-project-setup`)
- Koin for dependency injection (see `kmp-dependency-injection`)

## Workflow

1. **Add Play Core dependency** → read `references/setup.md`
   _Skip if Play Core is already in the project._
2. **Define shared types and interfaces** → read `references/shared-types.md`
3. **Implement Android update manager** → read `references/android-implementation.md`
   _Skip if only working on iOS._
4. **Implement iOS update manager** → read `references/ios-implementation.md`
   _Skip if only working on Android._
5. **Build UI components and ViewModel** → read `references/ui-and-viewmodel.md`
   _Skip if only modifying the platform layer._
6. **Wire into DI and integrate with App.kt** → read `references/integration.md`

## Gotchas

1. **Play Core returns `UPDATE_NOT_AVAILABLE` for every sideloaded build.** In-App Updates only work when installed from Google Play Store. Test using Play Console internal testing track with two version codes.

2. **`AppUpdateInfo` from Play Core has a short validity window.** If cached and reused after a delay, `startUpdateFlowForResult` silently fails. Always fetch fresh immediately before starting the update flow.

3. **`completeUpdate()` must be called for flexible updates.** After download finishes, the update is NOT installed automatically. Without this call, the update sits idle until the next app restart.

4. **iOS has no equivalent of In-App Updates.** The only option is checking the iTunes Lookup API for a newer version and redirecting to the App Store URL.

5. **`SKStoreReviewController.requestReview()` is rate-limited by iOS.** The system decides whether to show the prompt. No callback exists. Do not gate UI logic on whether the prompt appeared.

6. **The Android `PlatformAppUpdateManager` holds an Activity reference.** Using a strong reference causes a memory leak. Always wrap in `WeakReference` and null-check before use.

7. **Force update screens can be bypassed via the system back button on Android.** On Android, intercept with `BackHandler(enabled = true) {}`. On iOS, disable the swipe-back gesture.

8. **Android `startUpdateFlowForResult` with a request code is deprecated.** Register an `ActivityResultLauncher` with `StartIntentSenderForResult` contract in `onCreate` instead.

9. **iTunes Lookup API has undocumented rate limits.** Cache the result per session. Calling on every resume causes throttling and empty responses.

10. **Version comparison must handle unequal segment counts (e.g., "1.0" vs "1.0.1").** Pad missing segments with zero or the comparison silently treats them as equal.

## Assets

| Path | Load when... |
|---|---|
| references/setup.md | Adding Play Core dependency or configuring project for in-app updates |
| references/shared-types.md | Defining shared update types and interfaces in commonMain |
| references/android-implementation.md | Implementing Android Play Core in-app update manager |
| references/ios-implementation.md | Implementing iOS iTunes Lookup API version check |
| references/ui-and-viewmodel.md | Building update UI components or ViewModel state management |
| references/integration.md | Wiring update manager into DI and integrating with App.kt |

## Validation

### A. Kotlin and KMP correctness
- [ ] No unresolved imports in any source file
- [ ] `AppUpdateInfo` uses `sealed interface` (not `sealed class`)
- [ ] `AppUpdateUiState` uses `sealed interface` (not `sealed class`)
- [ ] `PlatformAppUpdateManager` uses `expect class` / `actual class` pattern
- [ ] `AppUpdateManagerContract` interface exists for testability
- [ ] `checkForUpdate()` returns a `Flow` or suspend function producing `AppUpdateInfo`
- [ ] Version comparison helper is in commonMain (not duplicated per platform)
- [ ] Android actual wraps Activity in `WeakReference`
- [ ] Android actual uses `ActivityResultLauncher` (not deprecated request code)
- [ ] iOS actual uses `Dispatchers.IO` for network call in `fetchAppStoreVersion`
- [ ] iOS `parseVersion` casts to `NSDictionary`/`NSArray` (not Kotlin Map/List)
- [ ] `ForceUpdateScreen` uses multiplatform back-press handling (not Android-only `BackHandler`)
- [ ] ViewModel exposes `StateFlow`, UI collects via `collectAsState()`

### B. Security
- [ ] No hardcoded force-update version; sourced from backend or config
- [ ] `WeakReference` null-check before every Activity access on Android
- [ ] No plaintext secrets or API keys in reference files

### C. Integration
- [ ] Koin module registers `AppUpdateManagerContract` (not concrete class) as binding
- [ ] `platformModule` loads before `appUpdateModule` in `startKoin`
- [ ] ProGuard rules keep Play Core classes when minification is enabled
