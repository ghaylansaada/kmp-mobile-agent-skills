# iOS Implementation

## NotificationManager (actual)

```kotlin
package {your.package}.core.notifications

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

actual class NotificationManager actual constructor(
    @Suppress("UNUSED_PARAMETER") context: PlatformContext,
) {
    private val center = UNUserNotificationCenter.currentNotificationCenter()
    private val _events = Channel<NotificationEvent>(Channel.BUFFERED)

    actual val events: Flow<NotificationEvent> = _events.receiveAsFlow()

    actual fun requestPermission(onResult: (Boolean) -> Unit) {
        center.requestAuthorizationWithOptions(
            options = UNAuthorizationOptionAlert or
                UNAuthorizationOptionSound or
                UNAuthorizationOptionBadge,
        ) { granted, _ -> onResult(granted) }
    }

    actual fun showLocal(payload: NotificationPayload) {
        val content = UNMutableNotificationContent().apply {
            setTitle(payload.title)
            setBody(payload.body)
            setUserInfo(
                buildMap {
                    if (payload.deepLink != null) {
                        put("deep_link", payload.deepLink)
                    }
                    putAll(payload.data)
                },
            )
        }
        val trigger = UNTimeIntervalNotificationTrigger
            .triggerWithTimeInterval(1.0, repeats = false)
        val request = UNNotificationRequest.requestWithIdentifier(
            payload.id,
            content,
            trigger,
        )
        center.addNotificationRequest(request, withCompletionHandler = null)
    }

    actual fun cancel(id: String) {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(id))
        center.removeDeliveredNotificationsWithIdentifiers(listOf(id))
    }

    actual fun cancelAll() {
        center.removeAllPendingNotificationRequests()
        center.removeAllDeliveredNotifications()
    }

    internal fun emitEvent(event: NotificationEvent) {
        _events.trySend(event)
    }
}
```

## Push Token Registration

```kotlin
package {your.package}.core.notifications

import platform.UIKit.UIApplication

fun registerForRemoteNotifications() {
    UIApplication.sharedApplication.registerForRemoteNotifications()
}
```

## Notification Delegate

Must be assigned to `UNUserNotificationCenter` before `didFinishLaunchingWithOptions`
returns. Setting it later causes foreground notifications to be silently dropped.

```kotlin
package {your.package}.core.notifications

import platform.Foundation.NSObject
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationPresentationOptionBanner
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol

class IosNotificationDelegate(
    private val onDeepLink: (String) -> Unit,
) : NSObject(), UNUserNotificationCenterDelegateProtocol {

    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (UNNotificationPresentationOptions) -> Unit,
    ) {
        withCompletionHandler(
            UNNotificationPresentationOptionBanner or
                UNNotificationPresentationOptionSound,
        )
    }

    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        didReceiveNotificationResponse: UNNotificationResponse,
        withCompletionHandler: () -> Unit,
    ) {
        val userInfo = didReceiveNotificationResponse
            .notification.request.content.userInfo
        val deepLink = userInfo["deep_link"] as? String
        if (deepLink != null) {
            onDeepLink(deepLink)
        }
        withCompletionHandler()
    }
}
```
