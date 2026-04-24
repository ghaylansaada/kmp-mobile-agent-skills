# Setup: Connectivity

> Always use the latest stable versions. Check the official release pages for current versions.

## No Additional Library Dependencies

Connectivity APIs are platform-native. The only requirements are `kotlinx-coroutines-core` and `kotlinx-serialization-json`, both already in the template.

## Android: AndroidManifest.xml

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
</manifest>
```

This is a normal permission (not dangerous) -- no runtime request needed.

## iOS: No Additional Configuration

NWPathMonitor is part of the Network framework, available on iOS 12+. No Info.plist entries, entitlements, or CocoaPods/SPM dependencies needed.

## Source Files to Create

### commonMain

```
composeApp/src/commonMain/kotlin/{your/package}/core/connectivity/
    ConnectivityStatus.kt
    ConnectivityObserver.kt
    PendingRequest.kt
    System.kt
    OfflineRequestQueue.kt
    ConnectivityAwareSync.kt
    OfflineAwareRepository.kt
```

### androidMain

```
composeApp/src/androidMain/kotlin/{your/package}/core/connectivity/
    ConnectivityObserver.android.kt
    System.android.kt
```

### iosMain

```
composeApp/src/iosMain/kotlin/{your/package}/core/connectivity/
    ConnectivityObserver.ios.kt
    System.ios.kt
```

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| `SecurityException: ACCESS_NETWORK_STATE` | Missing permission | Add permission to AndroidManifest.xml |
| `Unresolved reference: ConnectivityManager` | Wrong import | Use `android.net.ConnectivityManager` |
| iOS `NWPathMonitor` callback never fires | Monitor not started | Call `nw_path_monitor_start` after `set_queue` |
| Flow never emits on iOS | Missing `awaitClose` | Add `awaitClose { nw_path_monitor_cancel(monitor) }` |
| Shows "Available" in airplane mode | WiFi still on | Check `NET_CAPABILITY_VALIDATED`, not just INTERNET |
