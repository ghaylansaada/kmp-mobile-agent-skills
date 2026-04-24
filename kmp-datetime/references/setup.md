# Setup: DateTime Gradle Dependencies

> Always use the latest stable versions. Check the official release pages for current versions.

## Version Catalog (libs.versions.toml)

```toml
[versions]
kotlinx-datetime = "..."

[libraries]
kotlinx-datetime = { group = "org.jetbrains.kotlinx", name = "kotlinx-datetime", version.ref = "kotlinx-datetime" }
```

## Shared Module build.gradle.kts

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)
        }
    }
}
```

## Platform Notes

### Android

No additional artifacts required. kotlinx-datetime handles its own platform implementation internally. Do not use `java.time` directly in shared code.

### iOS

kotlinx-datetime uses Foundation's `NSDate` and `NSCalendar` internally on iOS/Native. No CocoaPods or SPM additions needed.

**WARNING:** iOS `NSDate` reference date is January 1, 2001, not Unix epoch (1970). When interoperating with NSDate manually, always use `timeIntervalSince1970`, never `timeIntervalSinceReferenceDate`. Getting this wrong shifts dates by 31 years.

## Gradle Sync Verification

After adding the dependency, sync the project and verify that `kotlinx-datetime` appears in the dependency tree for `commonMainImplementation`.

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| `Unresolved reference: kotlinx.datetime` | Missing dependency | Add `kotlinx-datetime` to `commonMain.dependencies` |
| `Duplicate class kotlinx.datetime.Instant` | Version conflict | Align all kotlinx-datetime versions in version catalog |
| `NoClassDefFoundError` on Android API < 26 | Using java.time directly | Use kotlinx-datetime APIs in shared code, not java.time |
