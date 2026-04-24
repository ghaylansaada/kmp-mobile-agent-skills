# Release Setup: Android Keystore and iOS Framework Configuration

> **Note:** Version numbers are intentionally omitted. Always use the latest stable release from the official documentation.

## Android: Create the Release Keystore

```bash
keytool -genkeypair \
  -v \
  -keystore composeApp/keystore/release.keystore \
  -alias release \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass <STORE_PASSWORD> \
  -keypass <KEY_PASSWORD> \
  -dname "CN=Your App, OU=Mobile, O=YourOrg, L=City, S=State, C=US"
```

## Android: Create keystore.properties

Copy [assets/templates/keystore.properties.template](../assets/templates/keystore.properties.template)
to `composeApp/keystore/keystore.properties` and fill in the passwords.

## Android: Add to .gitignore

```gitignore
# Release keystore -- NEVER commit
composeApp/keystore/release.keystore
composeApp/keystore/keystore.properties
```

The keystore and its passwords must NEVER be committed. A leaked keystore means anyone can
sign APKs as your app. Google Play's app signing enrollment can mitigate this, but the
upload key must still be kept secret.

## Android: CI Keystore Setup (GitHub Actions)

Store the keystore as a base64-encoded secret:

```bash
base64 -w 0 composeApp/keystore/release.keystore > keystore_base64.txt
```

| Secret Name         | Value                        |
|---------------------|------------------------------|
| `KEYSTORE_BASE64`   | Base64-encoded keystore file |
| `KEYSTORE_PASSWORD` | Store password               |
| `KEY_ALIAS`         | `release`                    |
| `KEY_PASSWORD`      | Key password                 |

Decode in the CI workflow:

```yaml
- name: Decode keystore
  run: |
    mkdir -p composeApp/keystore
    echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > composeApp/keystore/release.keystore
```

## iOS: KMP Framework Configuration

The `composeApp/build.gradle.kts` configures the iOS framework:

```kotlin
kotlin {
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
}
```

Static vs dynamic: static increases app size but simplifies embedding. Dynamic requires
an "Embed Frameworks" build phase in Xcode AND correct `@rpath`; missing either causes
a launch-time crash (`dyld: Library not loaded`).

## iOS: Xcode Project Integration

In Xcode > iosApp target > Build Phases, add a "Run Script" phase BEFORE "Compile Sources":

```bash
cd "$SRCROOT/.."
# See kmp-build skill for the full command reference
```

The Gradle task `embedAndSignAppleFrameworkForXcode` embeds and signs the framework using Xcode env vars. On CI, build the framework separately (see kmp-build skill for iOS build commands).

## iOS: Required Xcode Build Settings

| Setting                         | Value                                      |
|---------------------------------|--------------------------------------------|
| `PRODUCT_BUNDLE_IDENTIFIER`     | `{your.package}`                           |
| `MARKETING_VERSION`             | `1.0`                                      |
| `CURRENT_PROJECT_VERSION`       | `1`                                        |
| `CODE_SIGN_STYLE`               | `Manual` (CI) or `Automatic` (local)       |
| `ENABLE_BITCODE`                | `NO` (deprecated since Xcode 14)           |
| `DEBUG_INFORMATION_FORMAT`      | `dwarf-with-dsym` (Release)                |
