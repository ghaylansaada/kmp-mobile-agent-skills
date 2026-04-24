---
name: kmp-platform-integration
description: >
  Platform-specific integration layer for Android and iOS. Android: MainActivity, 
  actual implementations, manifest, ProGuard. iOS: MainViewController, framework 
  config, @ObjCName, Swift bridging. Activate when working on the platform layer,
  entry points, or actual implementations.
compatibility: >
  KMP with Compose Multiplatform.
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---

# KMP Platform Integration

## When to use

- Setting up or modifying the Android entry point (MainActivity)
- Setting up or modifying the iOS entry point (MainViewController)
- Writing `actual` implementations for PlatformContext, Platform, Database, DataStore
- Configuring AndroidManifest.xml, ProGuard/R8 rules, or the android block in Gradle
- Adding or modifying platform-specific DI bindings in the platform module (Android or iOS)
- Configuring the Kotlin/Native framework (baseName, linker flags, static vs dynamic)
- Bridging Kotlin code to Swift via ComposeApp framework
- Working with Xcode project settings, Info.plist, or build phases
- Hosting Compose UI inside SwiftUI via UIViewControllerRepresentable
- Adding `@ObjCName` annotations for cleaner Swift-facing APIs
- Wiring WorkManager or other Android-only background primitives
- Debugging platform-specific build or runtime issues
- Creating or updating iOS privacy manifests (PrivacyInfo.xcprivacy)
- Implementing predictive back gesture support on Android
- Adapting to Android 16 platform changes (edge-to-edge, orientation, predictive back)

## Depends on

- **[kmp-architecture](../kmp-architecture/SKILL.md)** -- core KMP concepts and patterns
- **[kmp-project-setup](../kmp-project-setup/SKILL.md)** -- Gradle structure, version catalogs, source set hierarchy
- **kmp-dependency-injection** -- Koin module DSL, commonModules, platformModule pattern
- **kmp-architecture** -- expect/actual pattern for PlatformContext, Platform, factories

## Workflow

1. **Configure Android build, manifest, and ProGuard** -- Read [references/setup.md](references/setup.md)
2. **Set up Android Activity and platform actuals** -- Read [references/android-entry-point.md](references/android-entry-point.md)
3. **Wire Android platform DI bindings** -- Read [references/android-platform-module.md](references/android-platform-module.md)
4. **Configure iOS framework, Xcode project, and compiler options** -- Read [references/ios-setup.md](references/ios-setup.md)
5. **Implement iOS entry points and Swift bridge** -- Read [references/ios-entry-points.md](references/ios-entry-points.md)
6. **Wire Swift-Kotlin interop and add new bindings** -- Read [references/integration.md](references/integration.md)
7. **Add iOS privacy manifest** -- Read [references/privacy-manifests.md](references/privacy-manifests.md). Required for App Store submission.
8. **Implement predictive back gesture support** -- Read [references/predictive-back.md](references/predictive-back.md). Required for Android 16 compatibility.

## Gotchas

1. **Activity context in Koin singletons causes memory leaks.** Koin singletons outlive any Activity. Passing an Activity context into a singleton binding retains the Activity forever. Always extract `applicationContext` from `PlatformContext.androidContext` before storing in any singleton.
2. **Android 14+ requires `foregroundServiceType` in the manifest.** Without it, `startForeground()` throws `MissingForegroundServiceTypeException` at runtime.
3. **R8 strips Room constructors, Ktor engines, and serialization companions.** Release builds work with zero code changes but crash at runtime with `RuntimeException: Cannot find implementation`. ProGuard keep rules are mandatory for release builds. See **kmp-release** skill for ProGuard/R8 rule configuration.
4. **Double `initKoin()` after process death throws `KoinApplicationAlreadyStartedException`.** Android recreates the Activity from saved state, calling `onCreate()` again. Guard with an internal flag.
5. **`enableEdgeToEdge()` must be called before `super.onCreate()`.** Calling it after results in a colored status bar instead of transparent, and the window insets API behaves inconsistently.
6. **`BundledSQLiteDriver` adds 3-4 MB to APK size.** It bundles SQLite native libraries for all ABIs. Use ABI splits if size is critical. See **kmp-database** skill for database driver configuration.
7. **Predictive back gesture requires opt-in on Android 13+.** Without `android:enableOnBackInvokedCallback="true"` in the manifest, the predictive back animation does not play.
8. **iOS lifecycle does NOT map 1:1 to Android.** `didFinishLaunchingWithOptions` is roughly `onCreate` but runs before any UI. There is NO iOS equivalent of `onDestroy` -- apps are suspended, not destroyed.
9. **Kotlin exceptions crossing the Swift boundary lose their stack trace.** The runtime wraps them in `NSError`. Log the full exception before it crosses. Consider returning `Result<T>` instead of throwing.
10. **Each `ComposeUIViewController { }` call creates a new composition.** If SwiftUI calls `makeUIViewController` again (e.g., on tab switch), Koin re-initializes and Compose state is lost. Store the UIViewController in a `@State` property.
11. **Without SKIE, suspend functions become completion handlers.** `Flow<T>` is not usable from Swift without a wrapper. Sealed classes become class hierarchies, not Swift enums.
12. **iOS has no `Dispatchers.IO`.** Use `Dispatchers.Default` for Room and background work on iOS. `Dispatchers.IO` in `iosMain` causes a compilation error.
13. **`@ObjCName` is required for clean Swift naming.** Without it, Kotlin top-level functions export as `FileNameKt.functionName()`.
14. **`autoreleasepool` is needed in tight Kotlin/Native loops that create Objective-C objects.** Without it, temporary NSObjects accumulate until the outermost pool drains, causing memory spikes.
15. **`isStatic = true` and `isStatic = false` have different Xcode requirements.** Static linking avoids a "Copy Frameworks" build phase. Switching to dynamic requires both a "Copy Frameworks" phase AND codesigning the `.framework` bundle.
16. **iOS privacy manifests are required for App Store submission.** Missing `PrivacyInfo.xcprivacy` causes App Store rejection. Every SDK and framework must have its own privacy manifest declaring required-reason API usage. See [references/privacy-manifests.md](references/privacy-manifests.md).
17. **Android 16 makes predictive back mandatory.** `Activity.onBackPressed()` is no longer called by the system. Apps using `onBackPressed()` for navigation or confirmation dialogs must migrate to `BackHandler` or `PredictiveBackHandler`. See [references/predictive-back.md](references/predictive-back.md).
18. **Android 16 ignores orientation and resizability locks on large screens.** On devices with smallest width >= 600dp, `android:screenOrientation` and `android:resizeableActivity="false"` are ignored. Apps must handle both portrait and landscape layouts on tablets and foldables.
19. **Android 16 removes the edge-to-edge opt-out.** Apps targeting Android 16 are always edge-to-edge. The `R.attr#windowOptOutEdgeToEdgeEnforcement` attribute is deprecated and has no effect. Apps must handle window insets correctly for all UI elements.

## Assets

| Path | Load when... |
|------|-------------|
| [assets/templates/main-view-controller.kt.template](assets/templates/main-view-controller.kt.template) | Scaffolding MainViewController |
| [assets/snippets/ios-app-swift.swift](assets/snippets/ios-app-swift.swift) | Setting up the SwiftUI app entry point |
| [references/privacy-manifests.md](references/privacy-manifests.md) | Adding or updating iOS PrivacyInfo.xcprivacy |
| [references/predictive-back.md](references/predictive-back.md) | Implementing Android predictive back gesture |

## Validation

### A. Android integration correctness
- [ ] `ComponentActivity` used (not `AppCompatActivity`)
- [ ] `enableEdgeToEdge()` called before `super.onCreate()`
- [ ] `DataStore` used for preferences (never `SharedPreferences`)
- [ ] `applicationContext` extracted before storing in Koin singletons
- [ ] `android:enableOnBackInvokedCallback="true"` present in manifest
- [ ] Version catalog entries used for all dependencies (no hardcoded coordinates)
- [ ] `BundledSQLiteDriver` with `Dispatchers.IO` for Room queries
- [ ] No wildcard imports in code snippets
- [ ] No `Activity.onBackPressed()` overrides (dead code on Android 16)
- [ ] `PredictiveBackHandler` used for custom back animations (not legacy `OnBackPressedCallback`)
- [ ] Window insets handled correctly for edge-to-edge (no opt-out on Android 16)

### B. iOS integration correctness
- [ ] Framework `baseName` matches the `import` statement in Swift files
- [ ] `isStatic = true` set and no "Copy Frameworks" build phase exists (or dynamic with proper embedding)
- [ ] `-lsqlite3` linker flag present when using Room/SQLite
- [ ] `@ObjCName` applied to public-facing APIs for clean Swift naming
- [ ] Framework export configuration includes all required module exports
- [ ] `ComposeUIViewController` stored in `@State` to prevent recreation on SwiftUI re-render
- [ ] `initKoin()` called inside `ComposeUIViewController` lambda before `App()`
- [ ] `Config.xcconfig` has correct `KOTLIN_FRAMEWORK_BUILD_TYPE` for build variant
- [ ] `Info.plist` has required permissions and background mode identifiers
- [ ] `PrivacyInfo.xcprivacy` present in the app target with required-reason API declarations
- [ ] KMP framework bundle includes its own `PrivacyInfo.xcprivacy` if it uses required-reason APIs
- [ ] `NSPrivacyAccessedAPITypes` lists all required-reason API categories used by the app and each SDK
- [ ] Privacy nutrition labels in App Store Connect match `NSPrivacyCollectedDataTypes` in the manifest

### C. Security
- [ ] ProGuard/R8 keep rules present for Room, kotlinx-serialization
- [ ] No secrets or API keys in AndroidManifest.xml or Gradle files
- [ ] `android:allowBackup` reviewed (consider `false` for sensitive apps)
- [ ] No hardcoded credentials or API keys in iOS platform code
- [ ] ATS exceptions in `Info.plist` are scoped to specific domains, not blanket disabled

### D. Performance
- [ ] `BundledSQLiteDriver` APK size impact documented
- [ ] `Dispatchers.IO` used for database query context on Android (not `Main`)
- [ ] No blocking calls on the main thread in platform module bindings
- [ ] `autoreleasepool` used in tight loops creating Objective-C objects from Kotlin/Native
- [ ] `org.gradle.caching=true` enabled for faster incremental iOS builds
- [ ] Development builds target only `iosSimulatorArm64` (not all architectures)

### E. Integration
- [ ] `Depends on` references point to real skills that exist in agent-skills/
- [ ] Platform module pattern consistent with `kmp-dependency-injection` skill
- [ ] expect/actual pattern consistent with `kmp-architecture` skill
- [ ] SKIE plugin configured in kmp-kotlin-coroutines for clean Swift interop (not duplicated here)
- [ ] Connected skills (kmp-kotlin-coroutines, kmp-background-job) referenced but not duplicated
