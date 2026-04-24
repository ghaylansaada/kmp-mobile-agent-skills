# Setup: DataStore Dependencies

> Always use the latest stable versions. Check the official release pages for current versions.

## Version Catalog (libs.versions.toml)

```toml
[versions]
datastore = "..."

[libraries]
datastore = { module = "androidx.datastore:datastore", version.ref = "datastore" }
datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
```

> Okio is a transitive dependency of DataStore KMP. You do not need to declare it
> explicitly unless you require a specific Okio version for other uses.

## build.gradle.kts (composeApp)

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.datastore)
            implementation(libs.datastore.preferences)
        }
    }
}
```

## Platform Notes

### Android

No additional Android-specific DataStore artifact is required beyond the
multiplatform core libraries declared above. The Android `Context` is provided
through Koin DI at runtime.

### iOS

DataStore KMP uses Okio's `Path` abstraction internally. The iOS source set
resolves the file path via Foundation's `NSDocumentDirectory`. No extra CocoaPods
or SPM dependencies are needed.

## Directory Structure

```
composeApp/src/
  commonMain/kotlin/{your.package}/
    core/
      datastore/
        DataStoreFactory.kt
        AppDataStoreConstructor.kt
    di/
      modules/
        LocalStorageModule.kt
  androidMain/kotlin/{your.package}/
    core/datastore/
      DataStore.android.kt
    di/
      PlatformModule.android.kt
  iosMain/kotlin/{your.package}/
    core/datastore/
      DataStore.ios.kt
    di/
      PlatformModule.ios.kt
```

## Verification

After adding the dependencies, sync the project and verify that `datastore` and `datastore-preferences` appear in the dependency tree for `commonMainImplementation`.

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| `Unresolved reference: PreferenceDataStoreFactory` | Missing `datastore-preferences` | Add `datastore-preferences` to `commonMain.dependencies` |
| `Unresolved reference: toPath` | Okio not on classpath | Ensure `datastore` artifact is resolved (it transitively includes Okio) |
| Duplicate class errors on Android | Both `datastore-preferences` (Android-only) and `datastore-preferences-core` (KMP) declared | Use only `datastore-preferences` for KMP projects (it bundles the core module) |
