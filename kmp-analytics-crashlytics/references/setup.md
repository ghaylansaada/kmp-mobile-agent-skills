# Firebase Analytics + Crashlytics: Dependency Setup

> **Note:** Version numbers are intentionally omitted. Always use the latest stable release from the official documentation.

## Version Catalog (libs.versions.toml)

```toml
[versions]
firebase-bom = "..."
google-services = "..."
firebase-crashlytics-gradle = "..."

[libraries]
firebase-bom = { module = "com.google.firebase:firebase-bom", version.ref = "firebase-bom" }
firebase-analytics = { module = "com.google.firebase:firebase-analytics" }
firebase-crashlytics = { module = "com.google.firebase:firebase-crashlytics" }

[plugins]
googleServices = { id = "com.google.gms.google-services", version.ref = "google-services" }
firebaseCrashlytics = { id = "com.google.firebase.crashlytics", version.ref = "firebase-crashlytics-gradle" }
```

Do not specify `version.ref` on individual Firebase libraries when using BOM — the BOM manages them. Do not use the deprecated `-ktx` artifact suffixes — KTX extensions are included in the main artifacts since BoM 32.x.

## Root build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.googleServices) apply false
    alias(libs.plugins.firebaseCrashlytics) apply false
}
```

## Android App Module build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
}
```

For KMP shared module `androidMain` sourceset:

```kotlin
kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(platform(libs.firebase.bom))
            implementation(libs.firebase.analytics)
            implementation(libs.firebase.crashlytics)
        }
    }
}
```

## Config Files

**Android:** Download `google-services.json` from Firebase Console → Project Settings → Android app. Place at `composeApp/google-services.json` (same level as build.gradle.kts).

**iOS:** Download `GoogleService-Info.plist` from Firebase Console → Project Settings → iOS app. Add to Xcode project and verify it appears in "Copy Bundle Resources" build phase.

## iOS Firebase Installation

### CocoaPods (iosApp/Podfile)

```ruby
platform :ios, '16.0'
target 'iosApp' do
  use_frameworks!
  # Always use latest stable versions — check Firebase iOS SDK release page
  pod 'FirebaseAnalytics'
  pod 'FirebaseCrashlytics'
end
```

### Swift Package Manager

Xcode → File → Add Package Dependencies → `https://github.com/firebase/firebase-ios-sdk`. Add `FirebaseAnalytics` and `FirebaseCrashlytics` products.

## iOS AppDelegate Initialization

```swift
import FirebaseCore

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()
        return true
    }
}
```

`FirebaseApp.configure()` MUST be called before any Kotlin shared code accesses analytics or crashlytics — before Koin initialization.

## iOS dSYM Upload

Add a Run Script build phase in Xcode (after "Compile Sources"):

- **SPM:** `"${BUILD_DIR%/Build/*}/SourcePackages/checkouts/firebase-ios-sdk/Crashlytics/run"`
- **CocoaPods:** `"${PODS_ROOT}/FirebaseCrashlytics/run"`

Without dSYM upload, iOS crash reports show unsymbolicated hex addresses.

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| "File google-services.json is missing" | Place in Android app module root |
| "Default FirebaseApp is not initialized" (Android) | Apply google-services plugin to app module |
| "Default FirebaseApp is not initialized" (iOS) | Call `FirebaseApp.configure()` in AppDelegate before Koin init |
| Crashes not in Firebase console | Upload dSYMs (iOS); wait up to 24h for new apps |
| "Duplicate class com.google.firebase" | Use single BOM declaration |
