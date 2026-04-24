# Android Entry Point: MainActivity and Platform Actuals

## MainActivity

```kotlin
// composeApp/src/androidMain/kotlin/{your/package}/MainActivity.kt
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

### Startup Sequence

1. `enableEdgeToEdge()` -- must be called before `super.onCreate()` for transparent system bars
2. `super.onCreate()` -- framework lifecycle
3. `PlatformConfig(ANDROID)` -- marks platform type for conditional DI
4. `PlatformContext(this)` -- wraps Activity context (Koin extracts `applicationContext` internally)
5. `initKoin(...)` -- guarded by internal flag to prevent `KoinApplicationAlreadyStartedException` after process death
6. `setContent { App() }` -- enters shared Compose UI tree

Uses `ComponentActivity` (not `AppCompatActivity`) -- lighter, Compose-native.

## PlatformContext (Android actual)

```kotlin
// composeApp/src/androidMain/kotlin/{your/package}/core/platform/PlatformContext.android.kt
package {your.package}.core.platform

import android.content.Context

actual class PlatformContext(val androidContext: Context)
```

Always extract `applicationContext` from `androidContext` before storing in long-lived singletons to prevent Activity leaks.

## AndroidPlatform

```kotlin
// composeApp/src/androidMain/kotlin/{your/package}/core/platform/Platform.android.kt
package {your.package}.core.platform

import android.os.Build

class AndroidPlatform : Platform {
    override val deviceModel: String = Build.MODEL ?: "Android"
    override val manufacturer: String = Build.MANUFACTURER ?: "Unknown"
    override val osType: Platform.OsType = Platform.OsType.ANDROID
    override val osVersionCode: Int = Build.VERSION.SDK_INT
    override val osVersionName: String = Build.VERSION.RELEASE
        ?: Build.VERSION.CODENAME
        ?: Build.VERSION.SDK_INT.toString()
}

actual fun getPlatform(): Platform = AndroidPlatform()
```
