---
name: kmp-release
description: >
  Release preparation and distribution for both Android and iOS. Android: AAB signing, 
  R8/ProGuard rules, Play Store deployment, Firebase distribution. iOS: archive, code 
  signing, App Store Connect, TestFlight, dSYM upload. Activate when preparing a release 
  build, configuring signing, or deploying to stores.
compatibility: >
  KMP with Compose Multiplatform targeting Android (Play Store) and iOS (App Store).
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---

# KMP Release

## When to use

- Creating a signed release build (AAB, APK, or IPA) for a KMP project
- Configuring ProGuard/R8 rules for the KMP dependency stack
- Setting up Play Store deployment or Firebase App Distribution (Android)
- Generating XCFramework, archiving, and submitting to App Store Connect or TestFlight (iOS)
- Managing version codes, build numbers, and version names
- Configuring code signing (keystore for Android, certificates/profiles for iOS)
- Uploading dSYMs or mapping.txt for crash symbolication
- Troubleshooting release-only crashes caused by minification or signing

## Depends on

- [kmp-project-setup](../kmp-project-setup/SKILL.md) -- Gradle structure, iOS framework configuration, version catalog

## Workflow

1. **Configure Android keystore and signing** -- Read [references/setup.md](references/setup.md)
2. **Add ProGuard/R8 rules for the KMP stack** -- Read [references/android-proguard.md](references/android-proguard.md)
3. **Configure Android versioning and build commands** -- Read [references/android-versioning.md](references/android-versioning.md)
4. **Configure Android signing in build.gradle.kts** -- Read [references/android-signing.md](references/android-signing.md)
5. **Configure iOS framework targets and Xcode project** -- Read [references/setup.md](references/setup.md)
6. **Set up iOS code signing (match, manual, or CI)** -- Read [references/ios-signing.md](references/ios-signing.md)
7. **Build iOS archive, export IPA, manage versions** -- Read [references/ios-archive.md](references/ios-archive.md)
8. **Submit to App Store Connect, TestFlight, or Firebase** -- Read [references/ios-distribution.md](references/ios-distribution.md)
9. **Set up Play Store and Firebase distribution (Android)** -- Read [references/integration.md](references/integration.md)
10. **Troubleshoot iOS-specific failures** -- Read [references/ios-troubleshooting.md](references/ios-troubleshooting.md)

## Gotchas

1. **R8 + kotlinx.serialization requires explicit keep rules.** Without them, R8 strips the generated `$$serializer` classes and deserialization crashes with `ClassNotFoundException` at runtime. Default ProGuard rules do NOT cover KMP serialization.
2. **`isMinifyEnabled = true` without correct ProGuard rules is the #1 cause of release-only crashes.** Always test a release build on a real device before publishing. Debug builds bypass R8 entirely, so they mask these failures.
3. **Play Store requires AAB, Firebase requires APK.** Since August 2021, Play Store only accepts AAB. Firebase App Distribution only accepts APK. You need both build tasks: `bundleRelease` for Play Store, `assembleRelease` for Firebase.
4. **`versionCode` must strictly increase.** Play Store rejects uploads where versionCode is less than or equal to the last published value. Use an explicit incrementing integer in `gradle.properties`, not derived from git tags or timestamps.
5. **Signing config credentials must never be hardcoded.** Use environment variables on CI and a gitignored `keystore.properties` file locally. A leaked keystore means anyone can sign APKs as your app.
6. **ProGuard strips Koin module definitions.** Koin resolves dependencies via reflection at runtime -- R8 sees registered classes as "unused" and removes them, causing `NoBeanDefFoundException` in release only.
7. **Room `_Impl` classes get stripped by R8.** The generated database and DAO implementations must be kept or the app crashes with "Cannot find implementation."
8. **Upload `mapping.txt` for every Android release.** Without it, crash stack traces from production are obfuscated and unreadable. This file changes with every build, so it must be uploaded alongside each release.
9. **`embedAndSignAppleFrameworkForXcode` must run BEFORE "Compile Sources" in Xcode build phases.** If placed after, the linker fails with `framework not found`. On CI, do NOT use this task -- it depends on Xcode environment variables not set outside Xcode. Build the framework separately with `linkReleaseFrameworkIosArm64`.
10. **App Store rejects binaries containing simulator slices.** Fat frameworks bundle all architectures together and will be rejected. Always use XCFramework for distribution.
11. **Bitcode is deprecated since Xcode 14.** Set `ENABLE_BITCODE = NO` everywhere. Projects migrating from Xcode 13 must remove all Bitcode flags or the archive fails.
12. **`isStatic = true` links the framework into the app binary -- simpler but larger app.** Dynamic frameworks require embedding AND `@rpath` config; missing either causes a launch-time crash (`dyld: Library not loaded`).
13. **iOS build number must strictly increase per TestFlight upload.** App Store Connect rejects duplicates. Use `latest_testflight_build_number + 1` in Fastlane to auto-increment.
14. **dSYMs are NOT generated unless `DEBUG_INFORMATION_FORMAT = dwarf-with-dsym`.** Without dSYMs, production crash reports are unsymbolicated.
15. **Missing `Info.plist` privacy usage descriptions causes immediate App Store rejection.** Apple validates these at submission time and rejects silently if any used permission lacks a description string.
16. **`xcrun altool` is deprecated since Xcode 14 and removed in Xcode 16.** Use the App Store Connect API via Fastlane `pilot`/`deliver`, or Apple's Transporter app.
17. **Fastlane match on CI requires the `--keychain` flag pointing to the build keychain.** Without it, match writes to the login keychain which may be locked, causing silent signing failures.
18. **`sed -i` in the bump script behaves differently on macOS vs Linux.** GNU sed (Linux) accepts `sed -i "s/..."` but BSD sed (macOS) requires `sed -i '' "s/..."`. CI runners and developer machines may differ.

## Assets

| Path | Load when... |
|------|-------------|
| [assets/templates/proguard-rules.pro](assets/templates/proguard-rules.pro) | Configuring R8/ProGuard rules for the KMP stack |
| [assets/templates/keystore.properties.template](assets/templates/keystore.properties.template) | Setting up Android keystore signing config |
| [assets/snippets/signing-config.gradle.kts](assets/snippets/signing-config.gradle.kts) | Adding the Gradle signing block to build.gradle.kts |
| [assets/templates/ExportOptions.plist](assets/templates/ExportOptions.plist) | Configuring iOS archive export options |
| [assets/templates/Fastfile-ios](assets/templates/Fastfile-ios) | Setting up Fastlane build and release lanes for iOS |
| [assets/snippets/xcframework-build.sh](assets/snippets/xcframework-build.sh) | Building a multi-arch XCFramework |

## Validation

### A. Release correctness -- Android
- [ ] Signing config reads credentials from `keystore.properties` locally and env vars in CI
- [ ] `jarsigner -verify` confirms the AAB is signed with the correct certificate
- [ ] Release APK launches without `ClassNotFoundException` or `NoBeanDefFoundException`
- [ ] Network calls, Room queries, Coil images, and Koin DI all work in the release build
- [ ] `versionCode` in the built artifact is strictly greater than the last published value
- [ ] AAB format used for Play Store uploads (not APK)
- [ ] APK format used for Firebase App Distribution
- [ ] `mapping.txt` exists at `composeApp/build/outputs/mapping/release/mapping.txt`
- [ ] ProGuard rules tested with `isMinifyEnabled = true` and `isShrinkResources = true`

### B. Release correctness -- iOS
- [ ] Code signing identity is `Apple Distribution` for App Store builds
- [ ] Provisioning profile matches the bundle identifier and signing certificate
- [ ] `CURRENT_PROJECT_VERSION` (build number) is incremented from the last TestFlight upload
- [ ] `MARKETING_VERSION` is set to the intended release version
- [ ] `Info.plist` contains all required privacy usage descriptions for APIs used by the app
- [ ] `ENABLE_BITCODE = NO` in all build configurations
- [ ] `DEBUG_INFORMATION_FORMAT = dwarf-with-dsym` for Release configuration
- [ ] Archive validates for distribution (no missing entitlements, no simulator slices)
- [ ] dSYMs are present in the archive and uploaded for crash reporting
- [ ] ExportOptions.plist `teamID` and `provisioningProfiles` are set to real values
- [ ] App Store Connect pre-submission checklist completed (screenshots, description, privacy policy URL, app icon)

### C. Security
- [ ] Keystore file (`release.keystore`) is listed in `.gitignore`
- [ ] `keystore.properties` is listed in `.gitignore`
- [ ] No keystore passwords appear in any committed file (build scripts, CI configs, properties)
- [ ] CI keystore decoded from base64-encoded secret, not committed as a binary
- [ ] Fastlane `play-store-key.json` is gitignored
- [ ] Firebase CLI token stored as CI secret, not in config files
- [ ] iOS signing certificates and provisioning profiles are NOT committed to the repository
- [ ] Secrets (API keys, certificate passwords, match passphrase) are stored in CI secrets or a secrets manager
- [ ] Fastlane match repo is private with restricted access
- [ ] App Store Connect API `.p8` key file is not in the source tree
- [ ] CI keychain is temporary and deleted after the build

### D. Performance
- [ ] `isShrinkResources = true` enabled in release build type to strip unused resources
- [ ] `isMinifyEnabled = true` enabled in release build type for code shrinking
- [ ] Debug build type has `isMinifyEnabled = false` for fast iteration
- [ ] `-assumenosideeffects` strips verbose/debug/info logging in release
- [ ] Static framework (`isStatic = true`) is used unless dynamic framework is specifically required
- [ ] XCFramework does not contain unnecessary architectures
- [ ] Release build uses `-O` (Swift) and release Kotlin optimizations

### E. Integration
- [ ] Upstream dependency on kmp-project-setup documented
- [ ] ProGuard rules template consistent with libraries declared in the version catalog
- [ ] Versioning properties (`app.versionCode`, `app.versionName`) referenced consistently across setup.md, android-versioning.md, and signing-config snippet
- [ ] CI workflow secrets list consistent between setup.md (keystore) and integration.md (Firebase/Play Store)
- [ ] Fastlane lanes invoke `linkReleaseFrameworkIosArm64` (not `embedAndSignAppleFrameworkForXcode`) for CI builds
- [ ] `bump_build` lane correctly reads the latest TestFlight build number
- [ ] Firebase App Distribution uses ad-hoc profile (not App Store profile) for iOS
- [ ] dSYM upload lane path matches the actual Firebase Crashlytics upload-symbols binary location
