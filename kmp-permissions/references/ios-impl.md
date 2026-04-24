# iOS Implementation

## IOSPermissionManager

```kotlin
// composeApp/src/iosMain/kotlin/{your/package}/core/permissions/IOSPermissionManager.kt
package {your.package}.core.permissions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.NSObject
import kotlin.coroutines.resume

actual class PermissionManager {

    actual suspend fun checkPermission(type: PermissionType): PermissionStatus =
        when (type) {
            PermissionType.CAMERA -> checkCameraPermission()
            PermissionType.LOCATION -> checkLocationPermission()
            PermissionType.NOTIFICATIONS -> checkNotificationPermission()
        }

    actual suspend fun requestPermission(type: PermissionType): PermissionStatus =
        when (type) {
            PermissionType.CAMERA -> requestCameraPermission()
            PermissionType.LOCATION -> requestLocationPermission()
            PermissionType.NOTIFICATIONS -> requestNotificationPermission()
        }

    actual fun openAppSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(
            url,
            options = emptyMap<Any?, Any>(),
            completionHandler = null,
        )
    }

    // --- Camera ---

    private fun checkCameraPermission(): PermissionStatus =
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> PermissionStatus.Granted
            AVAuthorizationStatusDenied,
            AVAuthorizationStatusRestricted,
            -> PermissionStatus.PermanentlyDenied
            AVAuthorizationStatusNotDetermined -> PermissionStatus.NotDetermined
            else -> PermissionStatus.NotDetermined
        }

    private suspend fun requestCameraPermission(): PermissionStatus {
        val current = checkCameraPermission()
        if (current != PermissionStatus.NotDetermined) return current
        return suspendCancellableCoroutine { continuation ->
            AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                val status = if (granted) PermissionStatus.Granted
                else PermissionStatus.PermanentlyDenied
                if (continuation.isActive) continuation.resume(status)
            }
        }
    }

    // --- Location (uses CLLocationManagerDelegate) ---

    private fun checkLocationPermission(): PermissionStatus =
        mapLocationStatus(CLLocationManager.authorizationStatus())

    private suspend fun requestLocationPermission(): PermissionStatus {
        val current = checkLocationPermission()
        if (current != PermissionStatus.NotDetermined) return current

        // CLLocationManager must be created and called on the main thread.
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val manager = CLLocationManager()
                val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
                    override fun locationManagerDidChangeAuthorization(
                        manager: CLLocationManager,
                    ) {
                        val status = mapLocationStatus(manager.authorizationStatus)
                        if (status != PermissionStatus.NotDetermined &&
                            continuation.isActive
                        ) {
                            continuation.resume(status)
                        }
                    }
                }
                manager.delegate = delegate
                manager.requestWhenInUseAuthorization()
            }
        }
    }

    private fun mapLocationStatus(status: CLAuthorizationStatus): PermissionStatus =
        when (status) {
            kCLAuthorizationStatusAuthorizedAlways,
            kCLAuthorizationStatusAuthorizedWhenInUse,
            -> PermissionStatus.Granted
            kCLAuthorizationStatusDenied,
            kCLAuthorizationStatusRestricted,
            -> PermissionStatus.PermanentlyDenied
            kCLAuthorizationStatusNotDetermined -> PermissionStatus.NotDetermined
            else -> PermissionStatus.NotDetermined
        }

    // --- Notifications ---

    private suspend fun checkNotificationPermission(): PermissionStatus =
        suspendCancellableCoroutine { continuation ->
            UNUserNotificationCenter.currentNotificationCenter()
                .getNotificationSettingsWithCompletionHandler { settings ->
                    val status = when (settings?.authorizationStatus) {
                        UNAuthorizationStatusAuthorized -> PermissionStatus.Granted
                        UNAuthorizationStatusDenied -> PermissionStatus.PermanentlyDenied
                        UNAuthorizationStatusNotDetermined -> PermissionStatus.NotDetermined
                        else -> PermissionStatus.NotDetermined
                    }
                    if (continuation.isActive) continuation.resume(status)
                }
        }

    private suspend fun requestNotificationPermission(): PermissionStatus {
        val current = checkNotificationPermission()
        if (current != PermissionStatus.NotDetermined) return current
        return suspendCancellableCoroutine { continuation ->
            val options = UNAuthorizationOptionAlert or
                UNAuthorizationOptionSound or UNAuthorizationOptionBadge
            UNUserNotificationCenter.currentNotificationCenter()
                .requestAuthorizationWithOptions(options) { granted, _ ->
                    val status = if (granted) PermissionStatus.Granted
                    else PermissionStatus.PermanentlyDenied
                    if (continuation.isActive) continuation.resume(status)
                }
        }
    }
}
```
