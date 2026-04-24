# Deep Link Handling

## Overview

Navigation 3 does not have built-in deep link annotation support like Nav2's `navDeepLink`. Instead, you manually parse incoming URIs and push the appropriate NavKey onto the user-owned back stack. This gives you full control over how deep links map to destinations.

## Common Deep Link Handler

Define a shared handler in commonMain that maps URI paths to NavKey entries:

```kotlin
package {your.package}.navigation

import androidx.compose.runtime.snapshots.SnapshotStateList
import {your.package}.navigation.routes.*

fun handleDeepLink(backStack: SnapshotStateList<AppNavKey>, uri: String) {
    val path = uri
        .substringAfter("://")
        .substringAfter("/")
        .trim('/')
    val segments = path.split("/")

    val targetKey: AppNavKey? = when {
        segments.size >= 2 && segments[0] == "detail" -> DetailKey(id = segments[1])
        segments.firstOrNull() == "profile" -> ProfileKey
        segments.firstOrNull() == "settings" -> SettingsKey
        segments.firstOrNull() == "search" && segments.size >= 2 ->
            SearchResultsKey(query = segments[1])
        else -> null
    }

    if (targetKey != null) {
        // Ensure we land on the deep link destination with Home as the root
        backStack.clear()
        backStack.add(HomeKey)
        if (targetKey !is HomeKey) {
            backStack.add(targetKey)
        }
    }
}
```

## Android Intent Deep Links

On Android, deep links arrive as Intent data. Forward the URI to the shared handler from your Activity:

```kotlin
// androidMain - MainActivity.kt
package {your.package}

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App(
                deepLinkUri = intent?.data?.toString(),
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle deep links when app is already running
        // Forward to a shared state holder or event channel
    }
}
```

In the shared App composable, process the deep link on first composition:

```kotlin
@Composable
fun AppNavDisplay(
    deepLinkUri: String? = null,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(HomeKey)

    LaunchedEffect(deepLinkUri) {
        if (deepLinkUri != null) {
            handleDeepLink(backStack, deepLinkUri)
        }
    }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        // ... entry provider
    )
}
```

Required AndroidManifest.xml intent filter (see [setup.md](setup.md) for full configuration):

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="myapp" android:host="template" />
</intent-filter>
```

## iOS Universal Links

On iOS, deep links arrive through the UIApplicationDelegate or SceneDelegate. Forward the URL to shared Kotlin code:

```swift
// iosMain - integrate in your Swift AppDelegate or SceneDelegate

// In SceneDelegate:
func scene(_ scene: UIScene, openURLContexts URLContexts: Set<UIOpenURLContext>) {
    guard let url = URLContexts.first?.url else { return }
    // Forward url.absoluteString to your shared Kotlin deep link handler
    // via a shared state holder or callback
}

// For universal links:
func scene(_ scene: UIScene, continue userActivity: NSUserActivity) {
    guard let url = userActivity.webpageURL else { return }
    // Forward url.absoluteString to shared Kotlin handler
}
```

On the Kotlin side, expose a function that iOS can call to inject the deep link URI:

```kotlin
// commonMain
class DeepLinkHolder {
    private val _pendingUri = MutableStateFlow<String?>(null)
    val pendingUri: StateFlow<String?> = _pendingUri.asStateFlow()

    fun onDeepLink(uri: String) {
        _pendingUri.value = uri
    }

    fun consume() {
        _pendingUri.value = null
    }
}
```

Collect `pendingUri` in AppNavDisplay and call `handleDeepLink()` when a URI arrives.

## Deep Link URI Patterns

| URI Pattern | Maps to |
|-------------|---------|
| `myapp://template/detail/{id}` | `DetailKey(id = "{id}")` |
| `myapp://template/profile` | `ProfileKey` |
| `myapp://template/settings` | `SettingsKey` |
| `myapp://template/search/{query}` | `SearchResultsKey(query = "{query}")` |

## Auth-Aware Deep Links

If a deep link targets an authenticated destination but the user is not logged in, save the intended destination and redirect to login first:

```kotlin
fun handleDeepLinkWithAuth(
    backStack: SnapshotStateList<AppNavKey>,
    uri: String,
    isAuthenticated: Boolean,
    pendingDeepLink: MutableState<String?>,
) {
    if (!isAuthenticated) {
        pendingDeepLink.value = uri
        backStack.clear()
        backStack.add(LoginKey)
    } else {
        handleDeepLink(backStack, uri)
    }
}
```

After successful login, check `pendingDeepLink` and navigate accordingly.
