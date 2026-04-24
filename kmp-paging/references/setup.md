# Setup: Paging Dependencies

> Always use the latest stable versions. Check the official release pages for current versions.

## Version Catalog

Add to `gradle/libs.versions.toml`:

```toml
[versions]
paging = "..."
room   = "..."

[libraries]
paging-common  = { module = "androidx.paging:paging-common",  version.ref = "paging" }
paging-runtime = { module = "androidx.paging:paging-runtime", version.ref = "paging" }
paging-compose = { module = "androidx.paging:paging-compose", version.ref = "paging" }
room-paging    = { module = "androidx.room:room-paging",      version.ref = "room" }
```

## build.gradle.kts (composeApp)

### commonMain

All multiplatform paging dependencies go in `commonMain.dependencies`:

```kotlin
commonMain.dependencies {
    // Paging (KMP core + Compose)
    implementation(libs.paging.common)
    implementation(libs.paging.compose)

    // Room + Room-Paging bridge
    implementation(libs.room.runtime)
    implementation(libs.room.paging)
    implementation(libs.sqlite.bundled)
}
```

### androidMain

The Android-specific paging runtime goes in `androidMain.dependencies`:

```kotlin
androidMain.dependencies {
    implementation(libs.paging.runtime)
}
```

### iosMain

No additional paging dependencies needed. The `paging-common` and `paging-compose` artifacts from `commonMain` provide full iOS support.

## Dependency Summary

| Artifact | Source Set | Purpose |
|---|---|---|
| `paging-common` | commonMain | Core paging API: Pager, PagingConfig, PagingSource, RemoteMediator, PagingData |
| `paging-compose` | commonMain | `collectAsLazyPagingItems()`, LazyPagingItems for Compose |
| `paging-runtime` | androidMain | Android-specific paging runtime (lifecycle integration) |
| `room-paging` | commonMain | Bridge: Room generates `PagingSource<Int, Entity>` from @Query |

## Version Alignment

- `paging-common`, `paging-compose`, and `paging-runtime` must all use the same version
- `room-paging` uses the Room version, not the Paging version
- Both libraries target Paging 3 API; no compatibility shim is needed
