# App Composable and Platform Entry Points

## App.kt Root Composable

`composeApp/src/commonMain/kotlin/{your.package}/App.kt`:

```kotlin
package {your.package}

import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import {your.package}.core.session.SessionManager
import {your.package}.presentation.user.AccountScreen
import org.koin.compose.koinInject

@Composable
fun App() {
    val sessionManager: SessionManager = koinInject()
    val imageLoader = koinInject<ImageLoader>()

    setSingletonImageLoaderFactory { imageLoader }

    MaterialTheme {
        AccountScreen(modifier = Modifier)
    }
}
```

- `koinInject<T>()` obtains dependencies from the Koin container
- `setSingletonImageLoaderFactory` must be called before any `AsyncImage` composable
- The App composable is the composition root -- it does not accept parameters
- Do not add `@Preview` to `App()` because `koinInject()` crashes without Koin initialization (see gotcha #7)

## Android: MainActivity

`composeApp/src/androidMain/kotlin/{your.package}/MainActivity.kt`:

```kotlin
package {your.package}

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import {your.package}.core.config.PlatformConfig
import {your.package}.core.platform.Platform
import {your.package}.core.platform.PlatformContext
import {your.package}.di.initKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val config = PlatformConfig(Platform.OsType.ANDROID)
        val context = PlatformContext(this)
        initKoin(context = context, config = config)
        setContent { App() }
    }
}
```

- `initKoin()` must be called before `setContent` -- composables crash if Koin is not initialized
- `enableEdgeToEdge()` must come before `super.onCreate()` for correct inset handling

## iOS: MainViewController

`composeApp/src/iosMain/kotlin/{your.package}/MainViewController.kt`:

```kotlin
package {your.package}

import androidx.compose.ui.window.ComposeUIViewController
import {your.package}.core.config.PlatformConfig
import {your.package}.core.platform.Platform
import {your.package}.core.platform.PlatformContext
import {your.package}.di.initKoin

fun MainViewController() = ComposeUIViewController {
    initKoin(
        context = PlatformContext(),
        config = PlatformConfig(Platform.OsType.IOS),
    )
    App()
}
```

- The `ComposeUIViewController` lambda runs exactly once (see gotcha #5). `initKoin()` is safe here because it is idempotent, but do not place state-dependent branching inside this lambda.
- All reactive logic must live inside the `App()` composable, not in this lambda.
