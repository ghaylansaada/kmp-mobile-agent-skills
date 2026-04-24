---
name: kmp-permissions
description: >
  Use this skill when adding runtime permission flows to a KMP app -- requesting
  and checking camera, location, and notification permissions on Android and iOS.
  Activate when the user asks to "request camera permission," "check location
  access," "handle denied permissions," "open Settings for permissions," or "add
  a new permission type." Covers expect/actual abstraction, rationale dialogs,
  permanently-denied Settings redirect, and Compose state helpers. Does NOT cover
  biometric authentication (see kmp-biometrics), file-picker flows that happen
  after permission is granted, or push-notification registration (see
  kmp-notifications).
compatibility: >
  KMP with Compose Multiplatform. Uses only platform APIs: AndroidX Activity
  Result, ContextCompat (Android); AVFoundation, CoreLocation, UserNotifications
  (iOS). Koin for DI.
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---

# KMP Permissions

## Scope

Provides a multiplatform runtime permissions abstraction using expect/actual for
requesting and checking camera, location, and notification permissions on Android
and iOS. Includes rationale dialogs, permanently-denied Settings redirect, Compose
state helpers, and a scaffold template for permission-gated screens. Covers Android
14+ partial photo access, Android 16 health/fitness permission changes, iOS privacy
manifest requirements, and iOS usage description localization. Does not cover
biometric prompts, file pickers, or push-notification token registration.

## When to use

- Requesting runtime permissions (camera, location, microphone, storage)
- Showing permission rationale dialogs before or after denial
- Handling Android 16 permission changes (health, media, notification)
- Configuring iOS privacy manifest (NSPrivacyAccessedAPITypes)
- Implementing App Tracking Transparency (ATT) on iOS
- Checking permission status before accessing protected resources
- Handling permanently denied permissions with settings redirect
- Managing notification permission (POST_NOTIFICATIONS on Android 13+)

## Depends on

No other agent skills. Uses only platform APIs (no third-party libraries):
- **Android**: ActivityResultContracts, ContextCompat
- **iOS**: AVFoundation, CoreLocation, UserNotifications
- **DI**: Koin

## Workflow

1. Read [references/setup.md](references/setup.md) -- Android manifest entries, iOS Info.plist usage descriptions, source file layout.
2. Read [references/common-types.md](references/common-types.md) -- shared sealed interfaces, expect declaration, rationale dialog, Compose state helper.
3. Read [references/android-impl.md](references/android-impl.md) -- actual Android class with Activity lifecycle binding.
4. Read [references/ios-impl.md](references/ios-impl.md) -- actual iOS class using platform frameworks and CLLocationManagerDelegate.
5. Read [references/integration.md](references/integration.md) -- Koin wiring, wiring diagram, adding new permission types.
6. Read [references/platform-changes.md](references/platform-changes.md) -- Android 14-16 permission changes, iOS privacy manifests, ATT, usage description localization.
7. Use [assets/templates/permission-feature.kt.template](assets/templates/permission-feature.kt.template) to scaffold a permission-gated screen.
8. Use [assets/snippets/permission-request-compose.kt](assets/snippets/permission-request-compose.kt) for ready-to-use Compose patterns.

Skip steps 7-8 if the task does not involve scaffolding new screens.

## Gotchas

1. **shouldShowRequestPermissionRationale returns false in two opposite cases** --
   "never asked" and "Don't ask again" are indistinguishable via the API alone. The
   implementation resolves this by checking *after* the result callback: denied +
   rationale=false means PermanentlyDenied. If you check *before* requesting, you
   get a false NotDetermined.

2. **iOS .restricted vs .denied require different UX copy** -- `.restricted` means
   device policy (MDM / parental controls) prevents the user from changing the
   permission. Do not send users to Settings for `.restricted` because the toggle is
   greyed out. Both map to PermanentlyDenied but the user-facing message should
   differ.

3. **Missing NSCameraUsageDescription causes SIGABRT on iOS** -- Requesting camera
   access without the plist entry causes an immediate SIGABRT crash with no error
   dialog. Always add the plist entry before requesting, even during development.

4. **POST_NOTIFICATIONS requires API 33+** -- On pre-API 33 Android, requesting
   POST_NOTIFICATIONS throws SecurityException. The implementation returns null from
   `toAndroidPermission()` which maps to Granted (notifications are implicit pre-33).

5. **Permission state is not updated after returning from Settings** -- Neither
   platform provides a callback for Settings changes. Re-check in `onResume`
   (Android) or `willEnterForegroundNotification` (iOS). The `LaunchedEffect` in
   `rememberPermissionState` only fires on initial composition.

6. **bindToActivity must be called before STARTED** -- Calling `requestPermission()`
   on Android without prior `bindToActivity()` causes a NullPointerException on the
   launcher. The `registerForActivityResult` contract must be registered before
   the Activity reaches the STARTED state.

7. **CLLocationManager must be created on the main thread** -- Creating or calling
   `requestWhenInUseAuthorization()` from a background thread silently fails on iOS.
   The iOS implementation dispatches to the main queue via `Dispatchers.Main`.

8. **iOS notification authorization is one-shot** -- Once a user denies notification
   permission on iOS, calling `requestAuthorization` again returns `false`
   immediately without showing a dialog. The only recourse is to direct the user
   to Settings.

9. **Android activity recreation resets the launcher** -- Configuration changes
   (rotation, dark mode) destroy and recreate the Activity. If `bindToActivity()`
   is not called again in the new `onCreate()`, the launcher reference is stale and
   `requestPermission()` throws IllegalStateException.

10. **iOS missing usage description causes immediate SIGABRT** -- Requesting any
    permission on iOS without the corresponding `NS*UsageDescription` in Info.plist
    causes an instant crash with no dialog, no exception, no recovery. Always add the
    plist entry before writing request code, even during development.

11. **iOS privacy manifests are separate from runtime permissions** -- The
    `PrivacyInfo.xcprivacy` privacy manifest declares required-reason API usage
    (UserDefaults, file timestamps, etc.) and is required for App Store submission.
    It does not affect runtime permission prompts. See kmp-platform-integration for
    privacy manifest details.

12. **iOS usage descriptions must be localized** -- All `NS*UsageDescription` strings
    in Info.plist must be localized via `InfoPlist.strings` for every language the app
    supports. Non-localized descriptions may cause App Store review issues in
    non-English markets.

13. **Android 16 replaces BODY_SENSORS with granular health permissions** -- New
    `android.permissions.health.*` permissions provide per-data-type access (heart
    rate, steps, etc.). Health Connect permissions use a separate request flow, not
    the standard `ActivityResultContracts.RequestPermission`.

14. **Android 14+ partial photo access requires handling two permission states** --
    `READ_MEDIA_VISUAL_USER_SELECTED` indicates the user granted access to specific
    photos only. The app must handle this partial-access state and re-request on
    next session.

15. **Android 16 deprecates announceForAccessibility** -- Replace
    `View.announceForAccessibility()` with `AccessibilityManager.announce()` which
    supports non-interruptive, queued announcements. See **kmp-accessibility** skill for accessible announcement patterns.

## Assets

| Path | Load when... |
|------|-------------|
| [references/setup.md](references/setup.md) | Adding manifest or plist permission entries |
| [references/common-types.md](references/common-types.md) | Creating shared permission types, sealed interfaces, Compose helpers |
| [references/android-impl.md](references/android-impl.md) | Implementing or modifying Android permission handling |
| [references/ios-impl.md](references/ios-impl.md) | Implementing or modifying iOS permission handling |
| [references/integration.md](references/integration.md) | Wiring Koin modules or adding new permission types |
| [references/platform-changes.md](references/platform-changes.md) | Android 14-16 permission changes, iOS privacy manifests, ATT, localization |
| [assets/templates/permission-feature.kt.template](assets/templates/permission-feature.kt.template) | Scaffolding a new permission-gated Compose screen |
| [assets/snippets/permission-request-compose.kt](assets/snippets/permission-request-compose.kt) | Copy-pasting Compose permission request patterns |

## Validation

### A. Build and compilation
- [ ] No unresolved imports in any code snippet

### B. Permission correctness
- [ ] `PermissionStatus` is a `sealed interface` (not enum class)
- [ ] `PermissionType` compiles as an enum in commonMain
- [ ] `PermissionManager` expect/actual compiles on both platforms
- [ ] Runtime permission is checked before use in every Compose pattern
- [ ] Rationale shown when `shouldShowRequestPermissionRationale` returns true
- [ ] Settings deep link offered for PermanentlyDenied state
- [ ] No `android.*` imports in commonMain
- [ ] No `platform.*` imports in commonMain (only in iosMain)
- [ ] `toAndroidPermission()` returns null for POST_NOTIFICATIONS on API < 33
- [ ] iOS camera check handles all four AVAuthorizationStatus values
- [ ] iOS location uses CLLocationManagerDelegate (not dispatch_after polling)
- [ ] iOS notification handles UNAuthorizationStatusProvisional if relevant
- [ ] iOS `Info.plist` usage descriptions localized via `InfoPlist.strings` for all supported languages
- [ ] iOS privacy manifest (`PrivacyInfo.xcprivacy`) present if app uses required-reason APIs
- [ ] Android 14+ partial photo access (`READ_MEDIA_VISUAL_USER_SELECTED`) handled if photo permissions are used
- [ ] Health/fitness permissions use `android.permissions.health.*` (not legacy `BODY_SENSORS`) for new implementations

### C. Security
- [ ] No secrets, API keys, or hardcoded bundle IDs in any file
- [ ] plist usage-description strings are generic placeholders, not real app copy
- [ ] No logging of permission status to analytics without user consent

### D. Performance
- [ ] `checkPermission()` does not trigger a system dialog (only `requestPermission` does)
- [ ] No redundant permission checks on every recomposition (LaunchedEffect guards the check)
- [ ] `suspendCancellableCoroutine` used (not `suspendCoroutine`) to support cancellation

### E. Integration
- [ ] Koin module provides `PermissionManager` as singleton on both platforms
- [ ] `bindToActivity()` called in `MainActivity.onCreate()` before `setContent`
- [ ] `unbindFromActivity()` called in `MainActivity.onDestroy()`
- [ ] Template placeholders (`{{FeatureName}}`, `{{permissionType}}`) are consistent and documented
- [ ] Adding a new permission type requires changes in exactly 5 places (documented in integration.md)
