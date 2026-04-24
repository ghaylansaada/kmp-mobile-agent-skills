# iOS Release Troubleshooting

## Issue 1: `Framework not found ComposeApp` during Xcode build

**Symptom:** `ld: framework not found ComposeApp` or `No such module 'ComposeApp'`.

**Cause:** KMP framework not built before Xcode build, or framework search path is wrong.
The `embedAndSignAppleFrameworkForXcode` build phase may be after "Compile Sources".

**Fix:**
1. Ensure the Gradle build phase runs BEFORE "Compile Sources"
2. Verify "Framework Search Paths" includes:
   `$(SRCROOT)/../composeApp/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`
3. Clean derived data: `rm -rf ~/Library/Developer/Xcode/DerivedData/iosApp-*`

---

## Issue 2: `No signing certificate "Apple Distribution" found`

**Symptom:** Archive or export fails because the distribution certificate is not found.

**Fix:**
1. Check installed certificates: `security find-identity -v -p codesigning`
2. Re-import if missing:
   `security import certificate.p12 -k login.keychain -P "<PASSWORD>" -T /usr/bin/codesign`
3. With match: `fastlane match appstore --force_for_new_devices`
4. On CI, verify `security set-key-partition-list` was called after importing the cert

---

## Issue 3: `Provisioning profile doesn't include signing certificate`

**Cause:** The profile was generated with a different certificate.

**Fix:**
1. Verify the profile matches:
   `security cms -D -i ~/Library/MobileDevice/Provisioning\ Profiles/<UUID>.mobileprovision`
2. Regenerate the profile in Apple Developer Portal with the correct certificate
3. With match: `fastlane match nuke distribution && fastlane match appstore`

---

## Issue 4: TestFlight upload fails with `Redundant Binary Upload`

**Cause:** A build with the same build number already exists.

**Fix:**
1. Increment: `fastlane ios bump_build`
2. Rebuild and re-upload
3. Marketing version can stay the same; only the build number must be unique

---

## Issue 5: dSYMs not generated in the archive

**Cause:** `DEBUG_INFORMATION_FORMAT` is DWARF without dSYM for Release.

**Fix:**
1. Xcode > Build Settings > set `DEBUG_INFORMATION_FORMAT` to `dwarf-with-dsym` (Release)
2. Set `ENABLE_BITCODE = NO`
3. Rebuild the archive
4. Verify: `find build/iosApp.xcarchive/dSYMs -name "*.dSYM"`

---

## Issue 6: `embedAndSignAppleFrameworkForXcode` fails on CI

**Cause:** Task depends on Xcode env vars only set during an Xcode build.

**Fix:**
1. On CI, build the framework separately with `linkReleaseFrameworkIosArm64`
2. Then archive: `xcodebuild archive -project iosApp/iosApp.xcodeproj -scheme iosApp ...`
3. Alternatively, use `assembleComposeAppXCFramework` and embed statically

---

## Issue 7: App Store rejects binary with simulator slices

**Cause:** A fat framework with simulator slices was embedded in the IPA.

**Fix:**
1. Use XCFramework instead of fat framework (separates slices correctly)
2. If fat framework is required, strip simulators before archiving:
   `lipo -remove x86_64 ComposeApp.framework/ComposeApp -output ComposeApp.framework/ComposeApp`
3. Better: use `assembleComposeAppXCFramework` which handles this automatically
