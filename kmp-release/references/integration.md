# Release Integration: Play Store and Firebase App Distribution

## Play Store deployment checklist

1. Signed release AAB built with production keystore
2. versionCode incremented from the last published version
3. ProGuard mapping.txt uploaded for deobfuscation
4. App tested on minSdk device/emulator
5. Store listing assets prepared (icon, screenshots, descriptions)
6. Privacy policy URL, content rating, and data safety form completed

## Local AAB testing with bundletool

```bash
java -jar bundletool.jar build-apks \
  --bundle=composeApp/build/outputs/bundle/release/composeApp-release.aab \
  --output=composeApp-release.apks \
  --ks=composeApp/keystore/release.keystore \
  --ks-pass=pass:<STORE_PASSWORD> \
  --ks-key-alias=release \
  --key-pass=pass:<KEY_PASSWORD>

java -jar bundletool.jar install-apks --apks=composeApp-release.apks
```

## Fastlane Play Store deployment

```ruby
# fastlane/Appfile
json_key_file("fastlane/play-store-key.json")
package_name("{your.package}")
```

```ruby
# fastlane/Fastfile
default_platform(:android)

platform :android do
  lane :deploy_internal do
    gradle(task: "bundle", build_type: "Release", project_dir: ".",
           properties: { "app.versionCode" => ENV["VERSION_CODE"] || "1",
                         "app.versionName" => ENV["VERSION_NAME"] || "1.0" })
    upload_to_play_store(
      track: "internal",
      aab: "composeApp/build/outputs/bundle/release/composeApp-release.aab",
      json_key: "fastlane/play-store-key.json",
      package_name: "{your.package}",
      skip_upload_metadata: true, skip_upload_images: true, skip_upload_screenshots: true,
      mapping: "composeApp/build/outputs/mapping/release/mapping.txt")
  end

  lane :promote_to_production do
    upload_to_play_store(
      track: "internal", track_promote_to: "production",
      json_key: "fastlane/play-store-key.json", package_name: "{your.package}",
      skip_upload_metadata: true, skip_upload_changelogs: true,
      skip_upload_images: true, skip_upload_screenshots: true)
  end
end
```

### Google Play API key setup

1. Google Play Console > Setup > API access
2. Create or link a service account with "Release manager" permissions
3. Download the JSON key to `fastlane/play-store-key.json` (gitignored)

## Firebase App Distribution (Android)

Firebase requires APKs -- it cannot process AABs. Build with `assembleRelease`, not
`bundleRelease`.

```bash
# Setup
npm install -g firebase-tools
firebase login
fastlane add_plugin firebase_app_distribution
```

```ruby
# Add to fastlane/Fastfile inside platform :android do
lane :distribute_release do
  gradle(task: "assemble", build_type: "Release", project_dir: ".",
         properties: { "app.versionCode" => ENV["VERSION_CODE"] || "1",
                       "app.versionName" => ENV["VERSION_NAME"] || "1.0" })
  firebase_app_distribution(
    app: ENV["FIREBASE_APP_ID"],
    apk_path: "composeApp/build/outputs/apk/release/composeApp-release.apk",
    groups: "stakeholders",
    release_notes: "Release #{ENV['VERSION_NAME']} (#{ENV['VERSION_CODE']})",
    firebase_cli_token: ENV["FIREBASE_CLI_TOKEN"])
end
```

### Direct CLI distribution (without Fastlane)

```bash
firebase appdistribution:distribute \
  composeApp/build/outputs/apk/release/composeApp-release.apk \
  --app <FIREBASE_APP_ID> \
  --groups "stakeholders" \
  --release-notes "Build $(date +%Y-%m-%d)"
```

### CI secrets

| Secret Name           | Description                                |
|-----------------------|--------------------------------------------|
| `FIREBASE_APP_ID`     | Firebase Android app ID (1:xxx:android:xxx)|
| `FIREBASE_CLI_TOKEN`  | Token from `firebase login:ci`             |

## Release flow summary

```
Developer                          CI (GitHub Actions)
---------                          -------------------
1. Bump version                    1. Checkout + decode keystore
2. Commit + tag                    2. Build signed AAB + APK
3. Push tag  ───────────────────>  3. Run tests
                                   4. Upload AAB to Play Store (internal)
                                   5. Distribute APK via Firebase
                                   6. Upload mapping.txt
                                   7. Create GitHub Release
```
