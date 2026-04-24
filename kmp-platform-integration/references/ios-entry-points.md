# iOS Entry Points

## MainViewController

The Kotlin entry point that creates a `ComposeUIViewController` for hosting shared Compose UI.

```kotlin
// composeApp/src/iosMain/kotlin/{your/package}/MainViewController.kt
package {your.package}

import androidx.compose.ui.window.ComposeUIViewController
import {your.package}.core.config.PlatformConfig
import {your.package}.core.platform.Platform
import {your.package}.core.platform.PlatformContext
import {your.package}.di.initKoin

fun MainViewController() = ComposeUIViewController {
    initKoin(context = PlatformContext(), config = PlatformConfig(Platform.OsType.IOS))
    App()
}
```

Each call to `ComposeUIViewController { }` creates a new composition. If SwiftUI calls `makeUIViewController` again (e.g., on tab switch), Compose state is lost and Koin re-initializes. Store the UIViewController in a `@State` property.

## SwiftUI App Entry Point

```swift
// iosApp/iosApp/iOSApp.swift
import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        BackgroundTasksIosKt.registerAndScheduleBackgroundTasks()
    }
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
```

The `init()` block runs before the first view appears, ensuring background tasks are registered with iOS.

## ContentView (SwiftUI Host)

```swift
// iosApp/iosApp/ContentView.swift
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.keyboard)
    }
}
```

## @ObjCName for Clean Swift Naming

By default, Kotlin top-level functions export as `FileNameKt.functionName()`. Use `@ObjCName` to control the Swift-visible name:

```kotlin
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

@OptIn(ExperimentalObjCName::class)
@ObjCName("AppMainViewController")
fun MainViewController() = ComposeUIViewController {
    initKoin(context = PlatformContext(), config = PlatformConfig(Platform.OsType.IOS))
    App()
}
```

Swift then calls `AppMainViewController()` directly instead of `MainViewControllerKt.MainViewController()`.

## Kotlin-to-Swift Name Mapping

| Kotlin | Swift |
|--------|-------|
| `fun MainViewController()` | `MainViewControllerKt.MainViewController()` |
| `@ObjCName("AppMain") fun MainViewController()` | `AppMain()` |
| `class IOSPlatform` | `IOSPlatform` |
| `object Companion` | `FooCompanion` or `Foo.companion` |
| `sealed class Result` | `Result` (base class, not Swift enum -- see SKIE) |
| `suspend fun fetch()` | `fetch(completionHandler:)` (or `async` with SKIE) |
| `Flow<T>` | Not directly usable (needs SKIE) |

## Cross-Skill References

- **PlatformContext, IOSPlatform, getPlatform()** -- see kmp-architecture for `actual` implementations
- **platformModule()** -- see kmp-dependency-injection for full iOS DI bindings
- **Database.ios.kt, DataStore.ios.kt** -- see kmp-datastore for iOS storage setup
