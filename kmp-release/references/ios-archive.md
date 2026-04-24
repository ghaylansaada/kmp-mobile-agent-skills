# iOS Build and Archive: IPA Export, Versioning, and dSYMs

## 1. Archive the iOS App

```bash
xcodebuild archive \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Release \
  -archivePath build/iosApp.xcarchive \
  -destination "generic/platform=iOS" \
  ENABLE_BITCODE=NO \
  CODE_SIGN_IDENTITY="Apple Distribution" \
  PROVISIONING_PROFILE_SPECIFIER="{your.package} AppStore" \
  MARKETING_VERSION="1.0" \
  CURRENT_PROJECT_VERSION="1"
```

The KMP framework must be built BEFORE archiving. On CI, run
`linkReleaseFrameworkIosArm64` first (do NOT use `embedAndSignAppleFrameworkForXcode`).

## 2. Export IPA

```bash
xcodebuild -exportArchive \
  -archivePath build/iosApp.xcarchive \
  -exportOptionsPlist iosApp/ExportOptions.plist \
  -exportPath build/ipa
```

Result: `build/ipa/iosApp.ipa`

See `assets/templates/ExportOptions.plist` for the export config. Key fields:

- `method`: `app-store` for App Store/TestFlight, `ad-hoc` for Firebase distribution
- `teamID`: Your Apple Developer Team ID
- `uploadBitcode`: `false` (deprecated since Xcode 14)
- `uploadSymbols`: `true` (enables crash symbolication)
- `signingStyle`: `manual` (for CI reproducibility)

## 3. XCFramework Generation

App Store REQUIRES XCFramework -- fat frameworks with simulator slices are rejected.

Or use the manual script at `assets/snippets/xcframework-build.sh`.

When building manually with `xcodebuild -create-xcframework`, do NOT include both
iosSimulatorArm64 and iosX64 -- they share the same platform identifier and the command
will fail. The Gradle `assembleComposeAppXCFramework` task handles this correctly.

## 4. Fastlane Build Lanes

See `assets/templates/Fastfile-ios` for the complete Fastfile. Key lanes:

| Lane              | Purpose                                  |
|-------------------|------------------------------------------|
| `sync_certificates` | Fetch signing certificates via match  |
| `build`           | Build KMP framework + archive iOS app    |
| `beta`            | Build and upload to TestFlight           |
| `release`         | Build and submit to App Store            |
| `distribute_qa`   | Build ad-hoc and distribute via Firebase |
| `upload_dsyms`    | Upload dSYMs to Firebase Crashlytics     |
| `bump_build`      | Auto-increment build number from TestFlight |
| `bump_version`    | Increment marketing version              |

## 5. Version Management

| Key                       | Description       | Initial |
|---------------------------|-------------------|---------|
| `MARKETING_VERSION`       | User-facing (1.0) | 1.0     |
| `CURRENT_PROJECT_VERSION` | Build number (1)   | 1       |

### Bump via Fastlane

```bash
fastlane ios bump_build                # latest TestFlight + 1
fastlane ios bump_version type:patch   # 1.0 -> 1.0.1
fastlane ios bump_version type:minor   # 1.0 -> 1.1
```

### Bump via agvtool

```bash
cd iosApp
agvtool new-version -all 2            # set build number
agvtool new-marketing-version 1.1.0   # set marketing version
```

Build number must strictly increase per TestFlight upload. Duplicates are rejected.

## 6. dSYM Upload for Crash Symbolication

dSYMs require `DEBUG_INFORMATION_FORMAT = dwarf-with-dsym` for Release. Without this,
production crash reports are unsymbolicated.

### Firebase Crashlytics

```bash
firebase crashlytics:symbols:upload \
  --app=<FIREBASE_IOS_APP_ID> \
  build/iosApp.xcarchive/dSYMs
```

Or via Fastlane: `fastlane ios upload_dsyms`

### Sentry

```bash
sentry-cli upload-dif --org your-org --project your-project \
  build/iosApp.xcarchive/dSYMs
```
