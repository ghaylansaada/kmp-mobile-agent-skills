# Biometrics Dependencies

> **Note:** Version numbers are intentionally omitted. Always use the latest stable release from the official documentation.

## Version Catalog (libs.versions.toml)

```toml
[libraries]
biometric = { module = "androidx.biometric:biometric", version.ref = "biometric" }
```

## Shared Module build.gradle.kts

```kotlin
kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.biometric)
        }
    }
}
```

## iOS: Info.plist

Face ID requires a usage description. The app crashes without it:

```xml
<key>NSFaceIDUsageDescription</key>
<string>We use Face ID to securely authenticate you.</string>
```

Touch ID does not require a separate plist entry.

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| `Unresolved reference: BiometricPrompt` | Missing dependency | Add `biometric` to `androidMain.dependencies` |
| FaceID crash on iOS | Missing `NSFaceIDUsageDescription` | Add the key to Info.plist |
| `BiometricPrompt` requires `FragmentActivity` | Using `ComponentActivity` | Use `AppCompatActivity` as base class |
| `BIOMETRIC_ERROR_NONE_ENROLLED` on emulator | No biometric enrolled | Enroll fingerprint in emulator Settings > Security |
| iOS simulator FaceID not working | Not enabled | Features > Face ID > Enrolled in Simulator |
