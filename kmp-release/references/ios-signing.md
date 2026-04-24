# iOS Code Signing: Certificates, Profiles, and CI Setup

## 1. Fastlane Match (Recommended for Teams)

```bash
fastlane match init
```

Creates `fastlane/Matchfile`:

```ruby
git_url("https://github.com/your-org/ios-certificates.git")
storage_mode("git")
type("appstore")
app_identifier("{your.package}")
team_id("YOUR_TEAM_ID")
```

Fetch certificates and profiles:

```bash
fastlane match development        # local testing
fastlane match appstore           # App Store distribution
```

### Match lifecycle commands

```bash
fastlane match development --readonly    # CI: fetch only, never create
fastlane match appstore --readonly       # CI: fetch only, never create

# Renew expired certificates
fastlane match nuke development
fastlane match nuke distribution
fastlane match development
fastlane match appstore
```

### Match on CI with custom keychain

On CI, match defaults to the login keychain which may be locked. Point match to the
build keychain created in the CI setup step:

```bash
fastlane match appstore --readonly --keychain_name build.keychain --keychain_password "$KEYCHAIN_PASSWORD"
```

Or in a Fastfile lane:

```ruby
match(
  type: "appstore",
  readonly: true,
  keychain_name: "build.keychain",
  keychain_password: ENV["KEYCHAIN_PASSWORD"]
)
```

## 2. Manual Signing

1. Apple Developer Portal > Certificates, Identifiers & Profiles
2. Create a Distribution Certificate (Apple Distribution)
3. Create an App ID matching your bundle identifier
4. Create a Provisioning Profile (App Store) linked to the certificate and App ID
5. Download and install the certificate in Keychain Access
6. Download the provisioning profile and double-click to install

## 3. Certificate and Profile Types

| Certificate Type     | Use Case                         | Validity |
|----------------------|----------------------------------|----------|
| Apple Development    | Run on device during development | 1 year   |
| Apple Distribution   | App Store + TestFlight           | 1 year   |

| Profile Type  | Use Case                                     |
|---------------|----------------------------------------------|
| Development   | Install on registered devices                |
| Ad Hoc        | Distribute to registered devices (Firebase)  |
| App Store     | Submit to App Store / TestFlight             |

## 4. CI Signing (GitHub Actions on macOS Runner)

### Required secrets

| Secret Name                     | Description                                  |
|---------------------------------|----------------------------------------------|
| `IOS_CERTIFICATE_BASE64`       | Base64-encoded .p12 distribution certificate |
| `IOS_CERTIFICATE_PASSWORD`     | Password for the .p12 file                   |
| `IOS_PROVISION_PROFILE_BASE64` | Base64-encoded .mobileprovision file         |
| `KEYCHAIN_PASSWORD`            | Temporary keychain password (any value)      |
| `MATCH_PASSWORD`               | Fastlane match encryption password           |
| `MATCH_GIT_TOKEN`              | Git token for the certificates repo          |

### CI keychain setup

```yaml
- name: Install signing certificate
  run: |
    security create-keychain -p "$KEYCHAIN_PASSWORD" build.keychain
    security default-keychain -s build.keychain
    security unlock-keychain -p "$KEYCHAIN_PASSWORD" build.keychain
    security set-keychain-settings -t 3600 build.keychain

    echo "$IOS_CERTIFICATE_BASE64" | base64 -d > certificate.p12
    security import certificate.p12 -k build.keychain \
      -P "$IOS_CERTIFICATE_PASSWORD" -T /usr/bin/codesign
    security set-key-partition-list -S apple-tool:,apple:,codesign: \
      -s -k "$KEYCHAIN_PASSWORD" build.keychain

    mkdir -p ~/Library/MobileDevice/Provisioning\ Profiles
    echo "$IOS_PROVISION_PROFILE_BASE64" | base64 -d \
      > ~/Library/MobileDevice/Provisioning\ Profiles/profile.mobileprovision
  env:
    KEYCHAIN_PASSWORD: ${{ secrets.KEYCHAIN_PASSWORD }}
    IOS_CERTIFICATE_BASE64: ${{ secrets.IOS_CERTIFICATE_BASE64 }}
    IOS_CERTIFICATE_PASSWORD: ${{ secrets.IOS_CERTIFICATE_PASSWORD }}
    IOS_PROVISION_PROFILE_BASE64: ${{ secrets.IOS_PROVISION_PROFILE_BASE64 }}
```

The `set-keychain-settings -t 3600` sets a lock timeout so the keychain stays unlocked
for the build duration. Without it, long builds may fail when the keychain auto-locks.
