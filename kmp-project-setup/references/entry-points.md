# Entry Points Reference -- KMP Project Setup

## Android Entry Point

File: `composeApp/src/androidMain/kotlin/{your/package}/MainActivity.kt`

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

---

## iOS Entry Point

File: `composeApp/src/iosMain/kotlin/{your/package}/MainViewController.kt`

`initKoin()` must be called **before** `ComposeUIViewController` is constructed, not inside
the content lambda. The content lambda re-executes on every recomposition -- calling
`initKoin()` there would reinitialize the DI container on every frame.

```kotlin
package {your.package}

import androidx.compose.ui.window.ComposeUIViewController
import {your.package}.core.config.PlatformConfig
import {your.package}.core.platform.Platform
import {your.package}.core.platform.PlatformContext
import {your.package}.di.initKoin

fun MainViewController(): UIViewController {
    initKoin(context = PlatformContext(), config = PlatformConfig(Platform.OsType.IOS))
    return ComposeUIViewController { App() }
}
```

---

## iOS Swift Side

File: `iosApp/iosApp/iOSApp.swift`

```swift
import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
```

---

## Xcode Integration

The `iosApp/` directory is a standard Xcode SwiftUI project that consumes the `ComposeApp`
framework produced by Kotlin/Native.

- **Framework Search Path** must point to the Kotlin/Native output directory.
- Since `isStatic = true`, the framework is statically linked and does NOT need embedding.
- A "Run Script" build phase should invoke Gradle to build the framework before Xcode links.
- The `-lsqlite3` linker option is embedded in the framework metadata automatically.
