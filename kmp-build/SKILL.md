---
name: kmp-build
description: >
  Centralized build and compilation commands for KMP projects. All Gradle commands
  for building, testing, verifying dependencies, linking iOS frameworks, and debugging
  build failures. Activate when building the project, running tests via Gradle,
  checking dependencies, or troubleshooting build errors.
compatibility: >
  KMP with Gradle (Kotlin DSL).
metadata:
  domain: kotlin-multiplatform
  targets: android, ios, jvm
---

# KMP Build Commands

## Scope

Centralized location for ALL Gradle build commands in this KMP project. Covers Android builds, iOS framework linking, test execution, dependency verification, and build failure troubleshooting. Does not cover project setup (see kmp-project-setup), CI/CD pipeline configuration (see kmp-ci-cd), or release signing (see kmp-release).

## When to use

- Building Android debug or release APKs
- Linking iOS frameworks for simulator or device
- Running unit tests, integration tests, or screenshot tests
- Checking dependency resolution or version conflicts
- Debugging Gradle build failures, cache issues, or compiler errors
- Verifying the build after Kotlin or dependency version upgrades

## Depends on

- **kmp-project-setup** -- project scaffold and Gradle configuration

## Workflow

1. Build Android target --> [references/android-build.md](references/android-build.md)
2. Build iOS target --> [references/ios-build.md](references/ios-build.md)
3. Run tests --> [references/test-commands.md](references/test-commands.md)
4. Verify dependencies --> [references/dependency-verification.md](references/dependency-verification.md)
5. Debug build failures --> [references/troubleshooting.md](references/troubleshooting.md)

## Gotchas

1. **KSP version must match Kotlin version exactly.** A KSP version built for Kotlin 2.0.0 will fail with Kotlin 2.0.10. Check the KSP releases page for the matching version (e.g., `2.0.10-1.0.24` for Kotlin `2.0.10`). Mismatches produce cryptic "incompatible plugin" errors during compilation.
2. **iOS framework name must match Xcode project reference.** The `baseName` in `framework { baseName = "ComposeApp" }` must exactly match what Xcode references in its "Link Binary With Libraries" and "Framework Search Paths" settings. A mismatch causes Xcode to fail with "framework not found" at link time.
3. **Gradle daemon memory settings affect build stability.** If `org.gradle.jvmargs` is too low (< 2GB), Kotlin compilation of iOS targets causes OOM crashes. Set it to at least `-Xmx4g` in `gradle.properties`. The Kotlin daemon memory (`kotlin.daemon.jvmargs`) should never exceed the Gradle daemon memory.
4. **Clean build required after Kotlin version upgrades.** Incremental compilation caches are not compatible across Kotlin versions. Run `./gradlew clean` after changing the Kotlin version, or builds may fail with "incompatible class version" or deserialization errors.
5. **`assembleRelease` requires signing configuration.** The Android release build fails without a keystore configured. Use `assembleDebug` for development verification. Release signing is covered by the kmp-release skill.
6. **iOS Simulator vs Device architectures differ.** `IosSimulatorArm64` targets Apple Silicon simulators, `IosArm64` targets physical devices. CI runners on Intel Macs need `IosX64` targets. Using the wrong target causes "unsupported architecture" errors.
7. **Configuration cache is incompatible with KSP.** If `org.gradle.configuration-cache=true` is set, KSP-based code generation (Room, Ktorfit) fails with serialization errors. Keep it disabled.
8. **Parallel execution can cause flaky tests.** If tests share mutable state (databases, files), `org.gradle.parallel=true` may cause intermittent failures. Run `--no-parallel` to diagnose test flakiness.

## Assets

| Path | Load when... |
|------|-------------|
| [references/android-build.md](references/android-build.md) | Building Android debug or release targets |
| [references/ios-build.md](references/ios-build.md) | Linking iOS frameworks for simulator or device |
| [references/test-commands.md](references/test-commands.md) | Running unit tests, integration tests, or screenshot tests |
| [references/dependency-verification.md](references/dependency-verification.md) | Checking dependency trees or resolving version conflicts |
| [references/troubleshooting.md](references/troubleshooting.md) | Diagnosing and fixing Gradle build failures |

## Validation

### A. Build correctness
- [ ] Android debug build succeeds: `./gradlew :composeApp:assembleDebug`
- [ ] iOS simulator framework links: `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`
- [ ] All tests pass: `./gradlew :composeApp:allTests`
- [ ] No unresolved imports in any source file
- [ ] Version catalog has no duplicate or conflicting entries
