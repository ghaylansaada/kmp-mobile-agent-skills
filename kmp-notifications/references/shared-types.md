# Shared Types (commonMain)

## NotificationPayload

```kotlin
package {your.package}.core.notifications

import kotlinx.serialization.Serializable

@Serializable
data class NotificationPayload(
    val id: String,
    val title: String,
    val body: String,
    val deepLink: String? = null,
    val data: Map<String, String> = emptyMap(),
)
```

## NotificationType (sealed interface)

```kotlin
package {your.package}.core.notifications

sealed interface NotificationType {
    val channelId: String

    data object General : NotificationType {
        override val channelId: String = "default_channel"
    }

    data object Promotional : NotificationType {
        override val channelId: String = "promotional_channel"
    }

    data object Transactional : NotificationType {
        override val channelId: String = "transactional_channel"
    }
}
```

## NotificationEvent (Flow-friendly sealed interface)

```kotlin
package {your.package}.core.notifications

sealed interface NotificationEvent {
    data class Received(val payload: NotificationPayload) : NotificationEvent
    data class Tapped(val payload: NotificationPayload) : NotificationEvent
    data class Dismissed(val id: String) : NotificationEvent
    data class TokenRefreshed(val token: String) : NotificationEvent
}
```

## NotificationManager (expect)

```kotlin
package {your.package}.core.notifications

import kotlinx.coroutines.flow.Flow

expect class NotificationManager(context: PlatformContext) {
    fun requestPermission(onResult: (Boolean) -> Unit)
    fun showLocal(payload: NotificationPayload)
    fun cancel(id: String)
    fun cancelAll()
    val events: Flow<NotificationEvent>
}
```

## PushTokenHandler

```kotlin
package {your.package}.core.notifications

interface PushTokenHandler {
    suspend fun onNewToken(token: String)
    suspend fun onNotificationReceived(payload: NotificationPayload)
}
```

## NotificationChannelDef

```kotlin
package {your.package}.core.notifications

data class NotificationChannelDef(
    val id: String,
    val name: String,
    val description: String,
    val importance: ChannelImportance = ChannelImportance.DEFAULT,
)

enum class ChannelImportance { LOW, DEFAULT, HIGH }
```

## DefaultPushTokenHandler

```kotlin
package {your.package}.core.notifications

class DefaultPushTokenHandler : PushTokenHandler {
    override suspend fun onNewToken(token: String) {
        // Forward token to backend immediately -- never cache as source of truth
    }

    override suspend fun onNotificationReceived(payload: NotificationPayload) {
        // Handle notification data (e.g., update local cache)
    }
}
```
