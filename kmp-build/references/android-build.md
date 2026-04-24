# Android Build Commands

## Quick Reference

| Command | Purpose |
|---------|---------|
| `./gradlew :composeApp:assembleDebug` | Build debug APK |
| `./gradlew :composeApp:assembleRelease` | Build release APK (requires signing) |
| `./gradlew :composeApp:compileDebugKotlinAndroid` | Compile Kotlin only (no APK packaging) |
| `./gradlew :composeApp:compileReleaseKotlinAndroid` | Compile Kotlin release (no APK packaging) |
| `./gradlew :composeApp:bundleRelease` | Build release AAB for Play Store |

## Debug Build

The most common development build. Does not require signing configuration.

```bash
./gradlew :composeApp:assembleDebug
```

Output location: `composeApp/build/outputs/apk/debug/composeApp-debug.apk`

To install directly on a connected device:

```bash
./gradlew :composeApp:installDebug
```

## Release Build

Requires a signing configuration in `composeApp/build.gradle.kts`. See the kmp-release skill for signing setup.

```bash
./gradlew :composeApp:assembleRelease
```

Output location: `composeApp/build/outputs/apk/release/composeApp-release.apk`

## Kotlin-Only Compilation

Faster feedback loop -- compiles Kotlin sources without APK packaging, resource processing, or dexing. Use this to quickly check for compilation errors.

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

## Build with Specific Options

```bash
# Build with stack trace for error details
./gradlew :composeApp:assembleDebug --stacktrace

# Build with verbose info logging
./gradlew :composeApp:assembleDebug --info

# Build with Gradle build scan
./gradlew :composeApp:assembleDebug --scan

# Skip specific tasks (e.g., lint)
./gradlew :composeApp:assembleDebug -x lint
```

## Common Android Build Failures

### Unresolved Android SDK

**Error:** `SDK location not found`

**Fix:** Create or verify `local.properties` at the project root:

```properties
sdk.dir=/Users/<username>/Library/Android/sdk
```

Or set the `ANDROID_HOME` environment variable.

### Dex Errors (Method Count)

**Error:** `Cannot fit requested classes in a single dex file`

**Fix:** Enable multidex in `composeApp/build.gradle.kts`:

```kotlin
android {
    defaultConfig {
        multiDexEnabled = true
    }
}
```

### Resource Compilation Failure

**Error:** `AAPT2 error: check logs for details`

**Fix:** Run with `--info` to get detailed AAPT2 output:

```bash
./gradlew :composeApp:assembleDebug --info 2>&1 | grep -A5 "AAPT2"
```

Common causes: invalid XML in resources, missing referenced drawables, or duplicate resource names across source sets.

### JVM Target Mismatch

**Error:** `Inconsistent JVM-target compatibility detected`

**Fix:** Ensure the Kotlin JVM target matches the Java compile target in `build.gradle.kts`:

```kotlin
android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
}
```

### Compose Compiler Version Mismatch

**Error:** `This version of the Compose Compiler requires Kotlin version X but you appear to be using Kotlin version Y`

**Fix:** With Kotlin 2.0+, the Compose compiler is bundled as a Kotlin compiler plugin. Ensure you are using the `org.jetbrains.kotlin.plugin.compose` Gradle plugin and remove any explicit `composeCompiler` version overrides.
