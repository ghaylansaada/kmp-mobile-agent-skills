# Android Signing Configuration

## Configure signing in build.gradle.kts

Copy the signing block from
[assets/snippets/signing-config.gradle.kts](../assets/snippets/signing-config.gradle.kts)
into `composeApp/build.gradle.kts` inside the `android {}` block.

Add the `defaultConfig` block alongside:

```kotlin
defaultConfig {
    applicationId = "{your.package}"
    minSdk = 23
    versionCode = (project.findProperty("app.versionCode") as? String)?.toInt() ?: 1
    versionName = project.findProperty("app.versionName") as? String ?: "1.0"
}
```

## Verify Signing

```bash
# Verify AAB
jarsigner -verify -verbose -certs \
  composeApp/build/outputs/bundle/release/composeApp-release.aab

# Verify APK
apksigner verify --print-certs \
  composeApp/build/outputs/apk/release/composeApp-release.apk
```

## Troubleshooting: Keystore was tampered with

**Symptom:** `bundleRelease` fails during signing.

**Fix:**
1. Verify keystore exists: `ls -la composeApp/keystore/release.keystore`
2. Verify password: `keytool -list -v -keystore composeApp/keystore/release.keystore`
3. Check `keystore.properties` passwords match the keytool creation values
4. In CI, verify base64 decode: `file composeApp/keystore/release.keystore` (expect "Java KeyStore")

Empty signing passwords fail at signing time, not configuration time. If `keystore.properties`
is missing AND CI env vars are unset, Gradle configures successfully but fails during
`bundleRelease` with a cryptic "Keystore was tampered with, or password was incorrect" error.
