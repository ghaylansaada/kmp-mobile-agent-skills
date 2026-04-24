# App Update: Dependencies and Setup

> **Note:** Version numbers are intentionally omitted. Always use the latest stable release from the official documentation.

## Version Catalog (libs.versions.toml)

```toml
[versions]
play-app-update = "..."

[libraries]
play-app-update = { group = "com.google.android.play", name = "app-update", version.ref = "play-app-update" }
play-app-update-ktx = { group = "com.google.android.play", name = "app-update-ktx", version.ref = "play-app-update" }
```

The `-ktx` artifact provides coroutine extensions for Play Core's callback-based `Task` API.

## Module Dependencies (composeApp/build.gradle.kts)

```kotlin
kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.play.app.update)
            implementation(libs.play.app.update.ktx)
        }
    }
}
```

No additional dependencies needed for commonMain or iosMain. iOS uses Foundation and StoreKit from the platform SDK.

## ProGuard Rules

Add to `composeApp/proguard-rules.pro` if `isMinifyEnabled = true`:

```proguard
-keep class com.google.android.play.core.** { *; }
-dontwarn com.google.android.play.core.**
```

Without these, R8 strips Play Core classes loaded via reflection, causing `ClassNotFoundException` at runtime.

## Platform Notes

**Android:** Play Core In-App Updates requires installation from Google Play Store. Sideloaded builds always return no update. Google Play Services must be available.

**iOS:** No extra dependencies. Uses Foundation `NSURLSession` for iTunes Lookup API, StoreKit `SKStoreReviewController` for review prompts, `UIApplication.openURL()` for App Store redirect.
