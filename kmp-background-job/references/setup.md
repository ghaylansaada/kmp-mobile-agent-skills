# Background Job Dependencies

> **Note:** Version numbers are intentionally omitted. Always use the latest stable release from the official documentation.

## Version Catalog (libs.versions.toml)

WorkManager is already declared in the template:

```toml
[libraries]
work-runtime-ktx = { module = "androidx.work:work-runtime-ktx", version.ref = "work" }
```

## Shared Module build.gradle.kts

```kotlin
kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.work.runtime.ktx)
        }
    }
}
```

## iOS: Info.plist

Every task ID you schedule MUST appear here or scheduling fails silently:

```xml
<key>BGTaskSchedulerPermittedIdentifiers</key>
<array>
    <string>{your.package}.sync</string>
</array>
<key>UIBackgroundModes</key>
<array>
    <string>fetch</string>
    <string>processing</string>
</array>
```

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| `ClassNotFoundException: WorkManager` | Missing dependency | Verify `work-runtime-ktx` in `androidMain.dependencies` |
| iOS task never executes | Missing Info.plist identifiers | Add task IDs to `BGTaskSchedulerPermittedIdentifiers` |
| `IllegalStateException: WorkManager not initialized` | Custom Application class overriding default init | Implement `Configuration.Provider` in Application |
| `MissingForegroundServiceTypeException` | No `foregroundServiceType` in manifest | Declare type in `<service>` element (Android 14+) |
| Foreground service ANR after 6 hours | Missing `onTimeout()` implementation | Implement `Service.onTimeout(int, int)` and call `stopSelf()` |
| iOS background task never runs in Low Power Mode | OS skips background refresh | Design for degradation; inform user if sync is stale |

See [platform-limits.md](platform-limits.md) for detailed coverage of foreground service timeouts, BGContinuedProcessingTask, and power budgeting constraints.
