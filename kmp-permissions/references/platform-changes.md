# Platform Permission Changes (Android 14-16, iOS)

## Android 14+: Partial Photo Access

Android 14 introduced `READ_MEDIA_VISUAL_USER_SELECTED` for partial photo library access. When the user selects "Allow limited access" in the photo picker, only the selected photos are accessible. The permission model works as follows:

- `READ_MEDIA_IMAGES` grants full access to all photos (if the user chooses "Allow all").
- `READ_MEDIA_VISUAL_USER_SELECTED` is granted when the user selects specific photos. This is a one-time grant -- the app must re-request on next access.
- The system re-prompts the user on each app session when only partial access was granted.
- Apps should request both `READ_MEDIA_IMAGES` and `READ_MEDIA_VISUAL_USER_SELECTED` together and handle the partial-access case gracefully.

## Android 15: Foreground Service Restrictions from BOOT_COMPLETED

Android 15 restricts launching certain foreground service types from `BOOT_COMPLETED` receivers:

- `camera`, `microphone`, `mediaProjection`, and `phoneCall` foreground service types cannot be started from a `BOOT_COMPLETED` broadcast receiver.
- `dataSync` and `mediaProcessing` foreground services can still be started from `BOOT_COMPLETED` but are subject to the 6-hour timeout.
- Apps relying on boot-time foreground services for camera or microphone access must use alternative approaches (e.g., WorkManager with appropriate constraints).

## Android 16: Health and Fitness Permissions

Android 16 introduces a new health and fitness permission model:

- New `android.permissions.health.*` permissions replace `BODY_SENSORS` and `BODY_SENSORS_BACKGROUND` for granular health data access.
- Health Connect permissions (e.g., `android.permission.health.READ_HEART_RATE`, `android.permission.health.WRITE_STEPS`) provide per-data-type access control.
- `BODY_SENSORS` continues to work for existing apps but new apps should migrate to the granular `health.*` permissions.
- Health permissions use a separate permission request flow through Health Connect, not the standard `ActivityResultContracts.RequestPermission`.

## Android 16: announceForAccessibility Deprecated

`View.announceForAccessibility()` is deprecated in Android 16. The replacement is `AccessibilityManager.announce()` which provides:

- Non-interruptive announcements (unlike the deprecated method which interrupted the current speech).
- Support for announcement queuing.
- Apps still using `announceForAccessibility` will continue to work but should migrate to the new API.

## iOS: Privacy Manifests and Required-Reason APIs

iOS apps must declare reasons for accessing certain APIs in a privacy manifest (`PrivacyInfo.xcprivacy`). This is separate from runtime permission prompts but is required for App Store submission:

- `UserDefaults`, file timestamp APIs, disk space APIs, and system boot time APIs are "required-reason APIs."
- Each usage must be declared with an approved reason code in the privacy manifest.
- Missing declarations cause App Store rejection.
- See kmp-platform-integration `references/privacy-manifests.md` for full details on the privacy manifest structure and KMP framework embedding.

## iOS: Usage Description Localization

All permission usage description strings in `Info.plist` must be localized for every language the app supports:

- Create `InfoPlist.strings` files for each locale in the Xcode project (e.g., `en.lproj/InfoPlist.strings`, `ar.lproj/InfoPlist.strings`).
- Each `InfoPlist.strings` file contains key-value pairs mapping the plist keys to localized strings.
- If a usage description is not localized for the user's language, the system shows the base-language string, which may cause App Store review issues in non-English markets.
- Example entry in `InfoPlist.strings`: `NSCameraUsageDescription = "This app uses the camera to scan documents.";`

## iOS: Missing Usage Description Causes Immediate Crash

Requesting a runtime permission on iOS without the corresponding `NS*UsageDescription` entry in `Info.plist` causes an immediate `SIGABRT` crash. There is no error dialog, no exception, and no recovery. This applies to:

- `NSCameraUsageDescription` (camera)
- `NSLocationWhenInUseUsageDescription` (location when in use)
- `NSLocationAlwaysAndWhenInUseUsageDescription` (always-on location)
- `NSMicrophoneUsageDescription` (microphone)
- `NSPhotoLibraryUsageDescription` (photo library)
- `NSContactsUsageDescription` (contacts)

Always add the plist entry before writing code that requests the permission, even during development.

## iOS: App Tracking Transparency (ATT)

Apps that track users across apps and websites owned by other companies must use the App Tracking Transparency framework:

- Call `ATTrackingManager.requestTrackingAuthorization()` to show the system prompt.
- The prompt can only be shown once. If denied, subsequent calls return `.denied` immediately.
- The tracking permission status must be checked before initializing any tracking SDKs (analytics, advertising).
- `NSUserTrackingUsageDescription` must be present in `Info.plist` with a clear explanation of why tracking is needed.
- If the app does not track users, ATT is not required. The definition of "tracking" follows Apple's App Tracking Transparency guidelines.

## KMP Implications

When adding new permission types to the shared `PermissionType` enum:

- Android permissions that require special handling (like Health Connect) may need a different request flow than `ActivityResultContracts.RequestPermission`. Consider using a separate interface or strategy pattern for these cases.
- iOS privacy manifest requirements are compile-time/submission-time concerns, not runtime -- they do not affect the `PermissionManager` code directly but must be configured in the Xcode project.
- Localization of iOS usage descriptions should be part of the app's localization workflow, not the permission library itself.

## Cross-Skill References

- **kmp-platform-integration** -- Privacy manifests, predictive back, and Android 16 platform changes are covered in detail.
- **kmp-background-job** -- Foreground service restrictions from BOOT_COMPLETED affect background task scheduling strategies.
