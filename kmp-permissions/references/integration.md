# Integration: KMP Permissions

## Koin DI Wiring

### Android Platform Module

```kotlin
// PlatformModule.android.kt
actual fun platformModule(
    context: PlatformContext,
    config: PlatformConfig,
): Module = module {
    single<PermissionManager> {
        PermissionManager(context = context.androidContext.applicationContext)
    }
}
```

### iOS Platform Module

```kotlin
// PlatformModule.ios.kt
actual fun platformModule(
    context: PlatformContext,
    config: PlatformConfig,
): Module = module {
    single<PermissionManager> { PermissionManager() }
}
```

## Platform Notes

- **Android**: `PermissionManager` needs `Context` for `ContextCompat.checkSelfPermission()`.
  Must call `bindToActivity()` in `onCreate()` and `unbindFromActivity()` in `onDestroy()`.
  See `android-impl.md` for the full Activity lifecycle binding code.
- **iOS**: No lifecycle binding needed. Platform APIs (AVFoundation, CoreLocation,
  UserNotifications) are called directly. CLLocationManager must be used on the main
  thread -- the implementation handles this via `Dispatchers.Main`.

## Wiring Diagram

```
commonMain                           androidMain / iosMain
-----------                          ---------------------
PermissionType (enum)                AndroidPermissionManager (actual)
PermissionStatus (sealed interface)      |-- ActivityResultContracts
PermissionManager (expect)               |-- shouldShowRationale()
    |                                IOSPermissionManager (actual)
    |                                    |-- AVCaptureDevice
    v                                    |-- CLLocationManager + delegate
PlatformModule (Koin)                    |-- UNUserNotificationCenter
    |
    v
Feature Screen (Compose)
    |-- rememberPermissionState()
    |-- PermissionRationaleDialog
    |-- openAppSettings()
```

## Adding a New Permission Type

Five changes are required. Example: adding MICROPHONE.

1. Add variant to `PermissionType` enum in commonMain:
   ```kotlin
   MICROPHONE,
   ```
2. Add Android mapping in `AndroidPermissionManager.toAndroidPermission()`:
   ```kotlin
   PermissionType.MICROPHONE -> Manifest.permission.RECORD_AUDIO
   ```
3. Add Android manifest entry:
   ```xml
   <uses-permission android:name="android.permission.RECORD_AUDIO" />
   ```
4. Add iOS handler methods in `IOSPermissionManager` (check + request):
   ```kotlin
   PermissionType.MICROPHONE -> checkMicrophonePermission()
   ```
5. Add iOS plist entry:
   ```xml
   <key>NSMicrophoneUsageDescription</key>
   <string>This app needs microphone access for voice recording.</string>
   ```
