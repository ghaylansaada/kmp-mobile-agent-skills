# iOS Framework Configuration and Setup

## Framework Configuration

```kotlin
// composeApp/build.gradle.kts
listOf(
    iosArm64(),
    iosSimulatorArm64(),
).forEach { iosTarget ->
    iosTarget.binaries.framework {
        baseName = "ComposeApp"
        isStatic = true
        linkerOpts.add("-lsqlite3")
    }
}
```

- `baseName = "ComposeApp"` -- Swift imports as `import ComposeApp`
- `isStatic = true` -- static linking avoids runtime framework embedding and the "Copy Frameworks" build phase
- `-lsqlite3` -- links system SQLite for Room on iOS

## iOS Source Set Dependencies

```kotlin
iosMain.dependencies {
    implementation(libs.ktor.client.darwin)
}
```

The Darwin engine uses iOS-native `URLSession`, respects App Transport Security, and shares cookies with `WKWebView`.

## Xcode Project Structure

```
iosApp/
    Configuration/
        Config.xcconfig          -- build settings (TEAM_ID, etc.)
    iosApp.xcodeproj/
        project.pbxproj
    iosApp/
        iOSApp.swift             -- @main SwiftUI entry point
        ContentView.swift        -- hosts ComposeUIViewController
        Info.plist               -- app metadata and permissions
        Assets.xcassets/
```

## Xcode Build Configuration

`Config.xcconfig` connects the Kotlin framework to Xcode:

```xcconfig
TEAM_ID=
BUNDLE_ID={your.package}
APP_NAME=mobile
KOTLIN_FRAMEWORK_BUILD_TYPE=Debug
```

The framework is built by the Gradle `embedAndSignAppleFrameworkForXcode` task in the Xcode Run Script build phase.

## iosMain Source Set Layout

```
composeApp/src/iosMain/kotlin/{your/package}/
    MainViewController.kt
    di/PlatformModule.ios.kt
    core/
        platform/Platform.ios.kt, PlatformContext.ios.kt
        database/Database.ios.kt
        datastore/DataStore.ios.kt
        transfer/io/FileReader.kt, FileWriter.kt, helpers.kt
```

## Compiler Options

```kotlin
// composeApp/build.gradle.kts
compilerOptions {
    freeCompilerArgs.add("-Xexpect-actual-classes")
}
```

Required for `expect class PlatformContext`. This flag is on a deprecation path and will become unnecessary when expect/actual classes stabilize in a future Kotlin release.

## Framework Export for Multi-Module Projects

When the KMP project has multiple modules, export them so Swift can access their types:

```kotlin
iosTarget.binaries.framework {
    baseName = "ComposeApp"
    isStatic = true
    linkerOpts.add("-lsqlite3")
    export(project(":shared"))
}
```

Only `public` declarations in exported modules are visible to Swift. Non-exported modules remain internal to the framework.

## iOS-Specific Considerations

**App Transport Security**: If the API uses HTTP (not HTTPS), add an ATS exception to `Info.plist`. The Darwin Ktor engine respects ATS settings. Scope exceptions to specific domains rather than blanket-disabling ATS.

**Background Modes**: Register identifiers in `BGTaskScheduler` from the Kotlin `registerAndScheduleBackgroundTasks()` called in `iOSApp.init()`. Add `fetch` and `processing` to `UIBackgroundModes` in `Info.plist`.

**MainActor Safety**: Functions called from Swift's main thread run on `Dispatchers.Main` in Kotlin. Both use `dispatch_get_main_queue`. Blocking Kotlin code on `Dispatchers.Main` blocks the Swift UI thread. Keep heavy work on `Dispatchers.Default`.

**Privacy Manifests**: Apps submitted to the App Store must include a `PrivacyInfo.xcprivacy` privacy manifest. The KMP framework bundle also needs its own privacy manifest if it uses required-reason APIs (UserDefaults, file timestamps, disk space, system boot time). See [privacy-manifests.md](privacy-manifests.md) for full details.
