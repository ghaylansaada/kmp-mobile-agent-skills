# Setup: Kermit Logging Dependencies

> Always use the latest stable versions. Check the official release pages for current versions.

## Version Catalog (libs.versions.toml)

```toml
[versions]
kermit = "..."

[libraries]
kermit = { module = "co.touchlab:kermit", version.ref = "kermit" }
```

The `kermit` artifact includes platform-specific writers (LogcatWriter on Android, OSLogWriter on iOS) automatically via KMP source set resolution.

## Shared Module build.gradle.kts

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kermit)
        }
    }
}
```

## Platform Notes

### Android

No additional artifact required. Kermit uses LogcatWriter on Android automatically. Logs appear in Android Studio Logcat with the tag you specify.

### iOS

Kermit uses OSLogWriter on iOS, integrating with the unified logging system. Logs appear in Xcode console and can be filtered by subsystem and category. No CocoaPods or SPM additions needed beyond the KMP framework export.

## Gradle Sync Verification

After adding the dependency, sync the project and verify that `co.touchlab:kermit` appears in the dependency tree for `commonMainImplementation`.

## Ktor Logging Integration (Optional)

The template already includes `ktor-client-logging`. Kermit integrates via a custom Logger implementation passed to Ktor's Logging plugin (see core-logging.md). No additional Ktor dependencies needed.

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| `Unresolved reference: Logger` | Missing Kermit dependency | Add `implementation(libs.kermit)` to `commonMain.dependencies` |
| `Unresolved reference: LogcatWriter` | Using in commonMain | `LogcatWriter` is Android-only; use in `androidMain` or via expect/actual |
| Duplicate class errors | Multiple Kermit versions | Check version catalog for single `kermit` version; inspect dependency tree |
| iOS build fails with linker errors | Kermit not exported in framework | Ensure iOS framework exports the shared module that depends on Kermit |
