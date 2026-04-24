# Setup: Performance Tooling Dependencies

> **Note:** Version numbers are intentionally omitted. Always use the latest stable release from the official documentation.

## Version Catalog (libs.versions.toml)

```toml
[versions]
leakcanary = "..."
macrobenchmark = "..."
profileInstaller = "..."

[libraries]
leakcanary = { module = "com.squareup.leakcanary:leakcanary-android", version.ref = "leakcanary" }
macrobenchmark = { module = "androidx.benchmark:benchmark-macro-junit4", version.ref = "macrobenchmark" }
profileInstaller = { module = "androidx.profileinstaller:profileinstaller", version.ref = "profileInstaller" }

[plugins]
baselineprofile = { id = "androidx.baselineprofile", version.ref = "macrobenchmark" }
```

## Android App Module build.gradle.kts

### LeakCanary (debug only)

```kotlin
dependencies {
    debugImplementation(libs.leakcanary)
}
```

LeakCanary is zero-config on Android. Adding the dependency automatically installs leak detection. No code changes needed.

**CRITICAL: Use `debugImplementation`, never `implementation`.** Shipping LeakCanary in release exposes heap dump functionality to production users.

### Profile Installer (for Baseline Profiles)

```kotlin
dependencies {
    implementation(libs.profileInstaller)
}
```

Both ProfileInstaller AND a generated baseline-prof.txt are required. ProfileInstaller without a profile does nothing. A profile without ProfileInstaller is never installed.

## Macrobenchmark Module Setup (Android)

Baseline Profiles require a separate `:macrobenchmark` module.

### settings.gradle.kts

```kotlin
include(":macrobenchmark")
```

### macrobenchmark/build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "{your.package}.macrobenchmark"
    compileSdk = 35

    defaultConfig {
        minSdk = 28
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":composeApp"
}

dependencies {
    implementation(libs.macrobenchmark)
}
```

## iOS: Xcode Instruments

No additional dependencies. Xcode Instruments provides:

- **Time Profiler**: CPU usage and hot code paths
- **Allocations**: Memory allocation tracking
- **Leaks**: Memory leak detection
- **System Trace**: Thread activity and scheduling

To use: Product -> Profile (Cmd+I) in Xcode, then select the instrument.

**Always profile on Release builds.** Debug builds on iOS include debugging instrumentation that distorts measurements.

## Compose Compiler Metrics

Add to `composeApp/build.gradle.kts`:

```kotlin
composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_metrics")
    metricsDestination = layout.buildDirectory.dir("compose_metrics")
}
```

Generates stability reports at build time:

```
composeApp/build/compose_metrics/
  composeApp_release-classes.txt     -- stability per class
  composeApp_release-composables.txt -- restartability per composable
  composeApp_release-module.json     -- summary metrics
```

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| LeakCanary in release build | `implementation` used | Change to `debugImplementation(libs.leakcanary)` |
| "No baseline profile" warning | Profile not generated | Generate baseline profile via Macrobenchmark module |
| Compose metrics not generated | `reportsDestination` not set | Add `composeCompiler` block |
| Macrobenchmark device error | Emulator below API 28 | Use physical device or API 28+ emulator |
| Xcode Instruments "Recording failed" | Profiling debug build | Use Release scheme |
