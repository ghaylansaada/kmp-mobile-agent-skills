// Snippet: Local notification scheduling patterns for KMP
// Copy and adapt into your ViewModel or use case layer.

package {your.package}.core.notifications.snippets

import {your.package}.core.notifications.NotificationManager
import {your.package}.core.notifications.NotificationPayload

// --- Snippet: Schedule a local notification with deep link ---

fun scheduleLocalNotification(
    notificationManager: NotificationManager,
    id: String,
    title: String,
    body: String,
    deepLink: String? = null,
) {
    notificationManager.requestPermission { granted ->
        if (granted) {
            notificationManager.showLocal(
                NotificationPayload(
                    id = id,
                    title = title,
                    body = body,
                    deepLink = deepLink,
                ),
            )
        }
    }
}

// --- Snippet: Cancel a specific notification ---

fun cancelNotification(
    notificationManager: NotificationManager,
    id: String,
) {
    notificationManager.cancel(id)
}

// --- Snippet: Build payload from FCM data map ---

fun payloadFromDataMap(
    messageId: String,
    data: Map<String, String>,
): NotificationPayload = NotificationPayload(
    id = messageId,
    title = data["title"] ?: "New notification",
    body = data["body"] ?: "",
    deepLink = data["deep_link"],
    data = data,
)
