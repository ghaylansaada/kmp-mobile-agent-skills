# iOS Build Commands

## Quick Reference

| Command | Purpose |
|---------|---------|
| `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64` | Debug framework for Apple Silicon simulator |
| `./gradlew :composeApp:linkReleaseFrameworkIosSimulatorArm64` | Release framework for Apple Silicon simulator |
| `./gradlew :composeApp:linkDebugFrameworkIosArm64` | Debug framework for physical device |
| `./gradlew :composeApp:linkReleaseFrameworkIosArm64` | Release framework for physical device |
| `./gradlew :composeApp:linkDebugFrameworkIosX64` | Debug framework for Intel simulator |

## Debug Framework (Simulator)

The most common iOS development build. Links a debug framework for the Apple Silicon simulator.

```bash
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

Output location: `composeApp/build/bin/iosSimulatorArm64/debugFramework/`

## Release Framework (Device)

Links a release-optimized framework for physical iOS devices.

```bash
./gradlew :composeApp:linkReleaseFrameworkIosArm64
```

Output location: `composeApp/build/bin/iosArm64/releaseFramework/`

## XCFramework Builds

An XCFramework bundles multiple architectures into a single distributable framework. Required for distribution via CocoaPods or SPM.

```bash
# Build XCFramework for all iOS targets
./gradlew :composeApp:assembleXCFramework

# Debug XCFramework only
./gradlew :composeApp:assembleComposeAppDebugXCFramework

# Release XCFramework only
./gradlew :composeApp:assembleComposeAppReleaseXCFramework
```

Output location: `composeApp/build/XCFrameworks/`

### XCFramework Gradle configuration

The XCFramework task requires explicit registration in `build.gradle.kts`:

```kotlin
kotlin {
    val xcf = XCFramework("ComposeApp")
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            xcf.add(this)
        }
    }
}
```

## Architecture Targets

| Target | Use case |
|--------|---------|
| `IosSimulatorArm64` | Apple Silicon Mac simulators (M1/M2/M3) |
| `IosX64` | Intel Mac simulators |
| `IosArm64` | Physical iOS devices |

CI environments on Intel Macs need `IosX64` targets. Apple Silicon CI runners (GitHub Actions `macos-14+`) use `IosSimulatorArm64`.

## Common iOS Build Failures

### Missing Xcode Command Line Tools

**Error:** `xcode-select: error: tool 'xcodebuild' requires Xcode`

**Fix:**

```bash
xcode-select --install
# or point to full Xcode
sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
```

### Framework Not Found in Xcode

**Error:** `framework not found ComposeApp`

**Fix:** Verify that:
1. The `baseName` in Gradle matches the framework name Xcode expects
2. The framework search path in Xcode points to the correct build output directory
3. The framework has been built for the correct architecture (SimulatorArm64 vs Arm64)

### Missing `-lsqlite3` Linker Flag

**Error:** `Undefined symbols for architecture arm64: _sqlite3_open`

**Fix:** Add the linker flag in the framework configuration:

```kotlin
iosArm64 {
    binaries.framework {
        linkerOpts("-lsqlite3")
    }
}
```

This is required when using Room with `BundledSQLiteDriver` on iOS.

### Kotlin/Native Compiler OOM

**Error:** `java.lang.OutOfMemoryError: Java heap space` during iOS compilation

**Fix:** Increase memory in `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx6g
kotlin.daemon.jvmargs=-Xmx4g
```

iOS (Kotlin/Native) compilation is significantly more memory-intensive than JVM/Android compilation.

### cinterop Failures

**Error:** `Cannot infer a compiler / linker for platform`

**Fix:** Ensure Xcode is fully installed (not just command line tools) and the correct SDK path is set:

```bash
xcrun --show-sdk-path --sdk iphonesimulator
```

If the path is incorrect, reset with `xcode-select -s`.
