# Setup: KMP Permissions

> **Note:** Version numbers are intentionally omitted. Always use the latest stable release from the official documentation.

## Android Manifest Permissions

Add required permissions to `composeApp/src/androidMain/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Camera permission -->
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera" android:required="false" />

    <!-- Location permissions -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

    <!-- Notification permission (Android 13+ / API 33+) -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application>
        <!-- ... existing application content ... -->
    </application>

</manifest>
```

Only declare permissions your app actually uses. Unused permissions trigger Play
Store warnings and may cause rejection.

## iOS Info.plist Usage Descriptions

Add permission usage descriptions to `iosApp/iosApp/Info.plist`:

```xml
<key>NSCameraUsageDescription</key>
<string>This app uses the camera to capture photos and scan documents.</string>

<key>NSLocationWhenInUseUsageDescription</key>
<string>This app uses your location to show nearby places.</string>

<key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
<string>This app uses your location in the background for navigation.</string>
```

**Critical**: iOS requires a non-empty usage description string for each permission.
Missing a `NSCameraUsageDescription` entry causes an immediate SIGABRT crash when
camera permission is requested -- no error dialog, no exception, just a crash.
Notifications do not require a plist entry.

## Source Files to Create

### commonMain

```
composeApp/src/commonMain/kotlin/{your/package}/core/permissions/
    PermissionType.kt          -- enum class
    PermissionStatus.kt        -- sealed interface
    PermissionManager.kt       -- expect class
    PermissionChecker.kt       -- interface (for testability)
    PermissionRationaleDialog.kt
    PermissionRequestState.kt
```

### androidMain

```
composeApp/src/androidMain/kotlin/{your/package}/core/permissions/
    AndroidPermissionManager.kt  -- actual class
```

### iosMain

```
composeApp/src/iosMain/kotlin/{your/package}/core/permissions/
    IOSPermissionManager.kt     -- actual class
```

## No Additional Dependencies

This implementation uses only platform APIs:
- **Android**: `ActivityResultContracts`, `ContextCompat.checkSelfPermission()`
- **iOS**: `AVFoundation`, `CoreLocation`, `UserNotifications`

No third-party permission libraries are needed.

## Koin Module Addition

Add permission bindings to the platform modules:

```kotlin
// Android -- PlatformModule.android.kt
single<PermissionManager> { PermissionManager(context = get()) }

// iOS -- PlatformModule.ios.kt
single<PermissionManager> { PermissionManager() }
```
