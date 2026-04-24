# Platform Integration: Cross-Platform Wiring

## Upstream Dependencies

| Skill | Provides |
|-------|----------|
| kmp-project-setup | `iosArm64()`, `iosSimulatorArm64()` targets, framework configuration |
| kmp-architecture | PlatformContext, IOSPlatform, getPlatform() actual implementations |
| kmp-dependency-injection | platformModule(), initKoin(), all platform DI bindings |

## Swift-Kotlin Bridge Architecture

```
Swift (iosApp/)                     Kotlin (composeApp/src/iosMain/)
----------------------------------  ----------------------------------
iOSApp.swift
    +-- init()
    |   +-- BackgroundTasksIosKt    -> BackgroundTasks.ios.kt
    |       .registerAndSchedule()
    +-- WindowGroup
        +-- ContentView
            +-- ComposeView
                +-- MainViewController  -> MainViewController.kt
                    Kt.MainView         |-- initKoin(...)
                    Controller()        |-- App()
```

## Adding a New iOS Platform Binding

1. Define an interface/factory in `commonMain`.
2. Add the `actual` binding in `PlatformModule.ios.kt`:

```kotlin
import org.koin.dsl.module

single<NewServiceFactory> {
    object : NewServiceFactory {
        override fun create(): NewService = IOSNewService()
    }
}
```

3. If the implementation uses iOS frameworks, add to `iosMain`:

```kotlin
import platform.Foundation.NSFileManager

class IOSNewService {
    fun doSomething() {
        val fm = NSFileManager.defaultManager
    }
}
```

## autoreleasepool for Tight Loops

When Kotlin/Native code creates many temporary Objective-C objects in a loop, wrap the loop body in `autoreleasepool` to prevent memory spikes:

```kotlin
import kotlinx.cinterop.autoreleasepool

fun processBatch(items: List<String>) {
    items.forEach { item ->
        autoreleasepool {
            val nsData = item.encodeToByteArray().toNSData()
            processData(nsData)
        }
    }
}
```

## Framework Export

Only `public` declarations in the framework module are visible to Swift. For additional module exports:

```kotlin
iosTarget.binaries.framework {
    baseName = "ComposeApp"
    isStatic = true
    linkerOpts.add("-lsqlite3")
    // export(project(":shared"))
}
```

`isStatic = true` produces a smaller binary. Dead code elimination is automatic in release builds.

## Android Wiring Diagram

```
MainActivity.onCreate()
    +-- PlatformContext(this) ---------> wraps Activity context
    +-- PlatformConfig(ANDROID) ------> marks platform identity
    +-- initKoin(context, config)
    |       +-- commonModules(context) + platformModule(context, config)
    +-- setContent { App() }
```

## Connected Skills

- **kmp-kotlin-coroutines** -- SKIE for async/await, AsyncSequence, sealed class enums
- **kmp-background-job** -- Background task registration and scheduling
- **kmp-permissions** -- Runtime permission flows; iOS Info.plist usage descriptions are separate from privacy manifests but both are required for App Store submission

## Platform-Specific Considerations (Android 16 / iOS)

- **Privacy manifests**: See [privacy-manifests.md](privacy-manifests.md) for PrivacyInfo.xcprivacy requirements. Every app and SDK must declare required-reason API usage.
- **Predictive back**: See [predictive-back.md](predictive-back.md) for migration from `onBackPressed()` to `PredictiveBackHandler`. Mandatory on Android 16.
- **Edge-to-edge enforcement**: Android 16 removes the opt-out. All apps targeting Android 16 are edge-to-edge. Ensure window insets are handled correctly.
- **Orientation/resizability locks ignored on large screens**: Android 16 ignores `android:screenOrientation` and `android:resizeableActivity="false"` on devices with smallest width >= 600dp.

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| `KoinApplicationAlreadyStartedException` | Double init after process death | Guard `initKoin()` with flag |
| `dyld: Library not loaded: ComposeApp.framework` | Dynamic framework not embedded | Use `isStatic = true` or embed in app bundle |
| `NullPointerException` on Koin resolve | `MainViewController()` not called | Ensure `initKoin()` runs first |
| `FileNameKt.functionName()` in Swift | Missing `@ObjCName` annotation | Add `@ObjCName("DesiredName")` |
| Room "Cannot find implementation" | R8 stripped `_Impl` in release | Add Room keep rule to `proguard-rules.pro` |
| Context leak (LeakCanary) | Activity retained by singleton | Use `context.androidContext.applicationContext` |
| Edge-to-edge broken | `enableEdgeToEdge()` after `super.onCreate()` | Call before `super.onCreate()` |
| Kotlin exception stack trace lost in Swift | NSError wrapping | Log on Kotlin side, use `@Throws` |
| Xcode builds take 5+ minutes | Full Kotlin/Native recompilation | Enable caching, target only simulator |
