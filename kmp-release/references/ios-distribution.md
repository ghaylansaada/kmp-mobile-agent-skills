# iOS Distribution: App Store Connect, TestFlight, and Firebase

## 1. App Store Connect Setup

### Prerequisites

- Apple Developer Program membership (active, $99/year)
- App record created in App Store Connect with matching bundle ID
- App Store Connect API key for CI authentication

### API Key (recommended for CI)

1. App Store Connect > Users and Access > Integrations > App Store Connect API
2. Create a key with "App Manager" role
3. Download the `.p8` key file (only downloadable once -- save it)
4. Note the Key ID and Issuer ID

Store as CI secrets:

| Secret Name                    | Description                  |
|--------------------------------|------------------------------|
| `APP_STORE_CONNECT_KEY_ID`    | API key ID                   |
| `APP_STORE_CONNECT_ISSUER_ID` | API issuer ID                |
| `APP_STORE_CONNECT_KEY_BASE64`| Base64-encoded .p8 key file  |

### Fastlane API key usage

```ruby
api_key = app_store_connect_api_key(
  key_id: ENV["APP_STORE_CONNECT_KEY_ID"],
  issuer_id: ENV["APP_STORE_CONNECT_ISSUER_ID"],
  key_content: Base64.decode64(ENV["APP_STORE_CONNECT_KEY_BASE64"]),
  is_key_content_base64: false
)
```

## 2. TestFlight Distribution

### Upload via Fastlane (recommended)

```bash
fastlane ios beta
```

### Build lifecycle

1. Upload IPA to App Store Connect
2. Build is processed (5-30 minutes)
3. Compliance questionnaire (export regulations) -- answer once per version
4. Build available to internal testers immediately
5. Submit for external testing (requires Beta App Review, ~24h first time)
6. External testers receive notification
7. Build expires after 90 days

## 3. App Store Submission

### Pre-submission checklist

1. All required screenshots (6.7", 6.5", 5.5" iPhones + iPad if universal)
2. App description, keywords, promotional text
3. Privacy policy URL
4. App Store icon (1024x1024, no alpha, no rounded corners)
5. Age rating questionnaire completed
6. App Review Information (contact info, demo account if needed)
7. Version and build number set correctly
8. dSYMs uploaded for crash symbolication

### Info.plist privacy descriptions

Any app using protected APIs must include usage description strings. Missing descriptions
cause immediate rejection. Common keys:

| Key                                  | Required when using           |
|--------------------------------------|-------------------------------|
| `NSCameraUsageDescription`           | Camera access                 |
| `NSPhotoLibraryUsageDescription`     | Photo library read            |
| `NSPhotoLibraryAddUsageDescription`  | Photo library write           |
| `NSLocationWhenInUseUsageDescription`| Location while in foreground  |
| `NSMicrophoneUsageDescription`       | Microphone access             |
| `NSFaceIDUsageDescription`           | Face ID authentication        |
| `NSUserTrackingUsageDescription`     | App Tracking Transparency     |

## 4. Firebase App Distribution

Requires an **ad-hoc** provisioning profile. App Store profiles will not work.

```bash
fastlane add_plugin firebase_app_distribution
fastlane ios distribute_qa
```

See the `distribute_qa` lane in `assets/templates/Fastfile-ios` for the full
implementation.

## 5. Bitcode Deprecation

Since Xcode 14: Bitcode is not accepted by App Store Connect. Set `ENABLE_BITCODE = NO`.
The KMP static framework is built without Bitcode by default. Since Apple no longer
recompiles from Bitcode, dSYM files must be generated and uploaded manually.
