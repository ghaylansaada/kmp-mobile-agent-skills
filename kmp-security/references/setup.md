# S-01 Security -- Setup

> **Note:** Version numbers are intentionally omitted. Always use the latest stable release from the official documentation.

## 1. Gradle Dependencies

### libs.versions.toml

```toml
[versions]
# Always use latest stable versions — check official release pages
sqlcipher = "..."
androidxSecurity = "..."
datastore = "..."

[libraries]
# SQLCipher for encrypted Room database
sqlcipher-android = { module = "net.zetetic:sqlcipher-android", version.ref = "sqlcipher" }

# AndroidX Security for EncryptedSharedPreferences
androidx-security-crypto = { module = "androidx.security:security-crypto", version.ref = "androidxSecurity" }

# DataStore
datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
```

### shared/build.gradle.kts

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.datastore.preferences)
        }
        androidMain.dependencies {
            implementation(libs.sqlcipher.android)
            implementation(libs.androidx.security.crypto)
        }
        // iOS needs no extra binary dependencies; uses system frameworks
    }
}
```

## 2. iOS Linking

In `iosMain`, the Keychain and CommonCrypto APIs are available through the
Darwin platform libraries. No CocoaPods or SPM additions are required.

Ensure `Security.framework` is linked. In most KMP setups this is automatic.
If not, add to your Xcode project or via Gradle:

```kotlin
iosTarget.binaries.framework {
    linkerOpts("-framework", "Security")
}
```

## 3. Android Manifest

No special permissions are required for Keystore usage. For root detection
that checks installed packages:

```xml
<!-- Optional: for querying installed packages on API 30+ -->
<queries>
    <package android:name="com.topjohnwu.magisk" />
    <package android:name="eu.chainfire.supersu" />
</queries>
```

## 4. ProGuard Setup

Add the ProGuard rules file to your Android build:

```kotlin
// android/build.gradle.kts
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

Copy the rules from `assets/snippets/proguard-security-rules.pro` into your
`proguard-rules.pro`.

## 5. SQLCipher Room Integration

Room requires a `SupportSQLiteOpenHelper.Factory` to use SQLCipher. The
encrypted database factory is provided in the implementation. No schema
changes are needed -- SQLCipher is transparent to Room.

**Important:** Migrating an existing unencrypted Room database to SQLCipher
requires an export/re-import step. See `references/integration.md` for the
migration procedure.

## 6. Directory Structure After Setup

```
shared/src/
  commonMain/kotlin/{your/package}/security/
    SecureTokenStorage.kt
    DeviceIntegrityChecker.kt
    SecureDataStoreWrapper.kt
    EndpointObfuscator.kt
    CertificatePinningConfig.kt
  androidMain/kotlin/{your/package}/security/
    SecureTokenStorage.android.kt
    DeviceIntegrityChecker.android.kt
    CertificatePinningEngine.kt
    EncryptedDatabaseFactory.kt
  iosMain/kotlin/{your/package}/security/
    SecureTokenStorage.ios.kt
    DeviceIntegrityChecker.ios.kt
    CertificatePinningEngine.kt
```
