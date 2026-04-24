# Integration

## Koin Module

```kotlin
package {your.package}.di

import {your.package}.core.notifications.DefaultPushTokenHandler
import {your.package}.core.notifications.NotificationManager
import {your.package}.core.notifications.PushTokenHandler
import org.koin.dsl.module

fun notificationModule() = module {
    single<NotificationManager> { NotificationManager(context = get()) }
    single<PushTokenHandler> { DefaultPushTokenHandler() }
}
```

## Deep Link Navigation

```kotlin
package {your.package}.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
fun HandleDeepLink(
    deepLink: String?,
    onNavigate: (route: String) -> Unit,
) {
    LaunchedEffect(deepLink) {
        if (deepLink == null) return@LaunchedEffect
        val segments = deepLink.removePrefix("myapp://").split("/")
        when (segments.firstOrNull()) {
            "screen" -> segments.getOrNull(1)?.let {
                onNavigate("detail/$it")
            }
            "settings" -> onNavigate("settings")
        }
    }
}
```

## Android Activity

Handles deep links on both cold start (`onCreate`) and warm start (`onNewIntent`).
Includes runtime permission request for `POST_NOTIFICATIONS` on Android 13+.

```kotlin
package {your.package}

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) { /* show "enable in Settings" prompt */ }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        handleDeepLinkIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS,
            )
        }
    }

    private fun handleDeepLinkIntent(intent: Intent) {
        intent.data?.toString()?.let { deepLink ->
            // Pass deepLink to navigation controller
        }
    }
}
```

## iOS SwiftUI Entry Point

Delegate must be set before `didFinishLaunchingWithOptions` returns:

```swift
@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    var body: some Scene { WindowGroup { ContentView() } }
}

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions:
            [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        IosPushTokenRegistrationKt.registerForRemoteNotifications()
        return true
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        let token = deviceToken
            .map { String(format: "%02.2hhx", $0) }
            .joined()
        // Forward token to KMP layer via PushTokenHandler
    }
}
```

## Observing Notification Events from a ViewModel

```kotlin
package {your.package}.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import {your.package}.core.notifications.NotificationEvent
import {your.package}.core.notifications.NotificationManager
import {your.package}.core.notifications.NotificationPayload
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ExampleViewModel(
    private val notificationManager: NotificationManager,
) : ViewModel() {

    init {
        notificationManager.events
            .onEach { event ->
                when (event) {
                    is NotificationEvent.Received -> { /* update UI */ }
                    is NotificationEvent.Tapped -> { /* navigate */ }
                    is NotificationEvent.Dismissed -> { /* clean up */ }
                    is NotificationEvent.TokenRefreshed -> { /* sync */ }
                }
            }
            .launchIn(viewModelScope)
    }

    fun scheduleReminder(title: String, body: String) {
        notificationManager.showLocal(
            NotificationPayload(
                id = "reminder",
                title = title,
                body = body,
                deepLink = "myapp://screen/reminders",
            ),
        )
    }

    fun requestNotificationPermission() {
        notificationManager.requestPermission { granted ->
            if (!granted) { /* show "enable in Settings" prompt */ }
        }
    }
}
```

## Connected Skills

| Skill | Relation |
|-------|----------|
| kmp-architecture | expect/actual pattern for NotificationManager |
| kmp-dependency-injection | Koin wiring for NotificationManager and PushTokenHandler |
| kmp-background-job | Background jobs may trigger local notifications on completion |
