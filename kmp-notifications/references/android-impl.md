# Android Implementation

## NotificationManager (actual)

```kotlin
package {your.package}.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

actual class NotificationManager actual constructor(
    private val context: PlatformContext,
) {
    private val androidContext = context.applicationContext
    private val mgr = NotificationManagerCompat.from(androidContext)
    private val _events = Channel<NotificationEvent>(Channel.BUFFERED)

    actual val events: Flow<NotificationEvent> = _events.receiveAsFlow()

    init { createDefaultChannel() }

    actual fun requestPermission(onResult: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // NOTE: This only checks current status. Full runtime request requires
            // ActivityResultLauncher in the hosting Activity. See integration.md
            // for the Activity-based permission request pattern.
            val granted = ContextCompat.checkSelfPermission(
                androidContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            onResult(granted)
        } else {
            onResult(true)
        }
    }

    actual fun showLocal(payload: NotificationPayload) {
        val id = payload.id.hashCode()
        val intent = payload.deepLink?.let {
            Intent(Intent.ACTION_VIEW, Uri.parse(it)).apply {
                setPackage(androidContext.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        } ?: androidContext.packageManager
            .getLaunchIntentForPackage(androidContext.packageName)

        val pending = PendingIntent.getActivity(
            androidContext,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(
            androidContext,
            DEFAULT_CHANNEL_ID,
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(payload.title)
            .setContentText(payload.body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                androidContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            mgr.notify(id, notification)
        }
    }

    actual fun cancel(id: String) {
        mgr.cancel(id.hashCode())
    }

    actual fun cancelAll() {
        mgr.cancelAll()
    }

    fun createChannel(def: NotificationChannelDef) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = when (def.importance) {
                ChannelImportance.LOW ->
                    android.app.NotificationManager.IMPORTANCE_LOW
                ChannelImportance.DEFAULT ->
                    android.app.NotificationManager.IMPORTANCE_DEFAULT
                ChannelImportance.HIGH ->
                    android.app.NotificationManager.IMPORTANCE_HIGH
            }
            mgr.createNotificationChannel(
                NotificationChannel(def.id, def.name, importance).apply {
                    description = def.description
                },
            )
        }
    }

    internal fun emitEvent(event: NotificationEvent) {
        _events.trySend(event)
    }

    private fun createDefaultChannel() {
        createChannel(
            NotificationChannelDef(
                DEFAULT_CHANNEL_ID,
                "General",
                "General notifications",
            ),
        )
    }

    companion object {
        const val DEFAULT_CHANNEL_ID = "default_channel"
    }
}
```

## FCM Service

```kotlin
package {your.package}.core.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class AppFirebaseMessagingService : FirebaseMessagingService() {
    private val pushTokenHandler: PushTokenHandler by inject()
    private val notificationManager: NotificationManager by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch { pushTokenHandler.onNewToken(token) }
        notificationManager.emitEvent(NotificationEvent.TokenRefreshed(token))
    }

    override fun onMessageReceived(msg: RemoteMessage) {
        super.onMessageReceived(msg)
        val payload = NotificationPayload(
            id = msg.messageId ?: msg.sentTime.toString(),
            title = msg.notification?.title ?: msg.data["title"] ?: "",
            body = msg.notification?.body ?: msg.data["body"] ?: "",
            deepLink = msg.data["deep_link"],
            data = msg.data,
        )
        scope.launch { pushTokenHandler.onNotificationReceived(payload) }
        notificationManager.emitEvent(NotificationEvent.Received(payload))
        if (msg.notification != null) {
            notificationManager.showLocal(payload)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
```
