# Setup: Image Loader Dependencies

> Always use the latest stable versions. Check the official release pages for current versions.

## Version Catalog

Add to `gradle/libs.versions.toml`:

```toml
[versions]
coil = "..."

[libraries]
coil-compose = { module = "io.coil-kt.coil3:coil-compose",       version.ref = "coil" }
coil-network = { module = "io.coil-kt.coil3:coil-network-ktor3", version.ref = "coil" }
coil-svg     = { module = "io.coil-kt.coil3:coil-svg",           version.ref = "coil" }
```

## build.gradle.kts (composeApp)

All three Coil dependencies are declared in `commonMain.dependencies`:

```kotlin
sourceSets {
    commonMain.dependencies {
        implementation(libs.coil.compose)
        implementation(libs.coil.network)
        implementation(libs.coil.svg)
    }
}
```

No platform-specific Coil dependencies are needed. Coil 3 provides native KMP support across Android and iOS from the common source set.

## Dependency Breakdown

| Library | Purpose |
|---|---|
| `coil-compose` | AsyncImage composable, LocalPlatformContext, setSingletonImageLoaderFactory |
| `coil-network-ktor3` | KtorNetworkFetcherFactory for HTTP image fetching via Ktor |
| `coil-svg` | SvgDecoder.Factory() for rendering SVG images |

## Transitive Dependencies

Coil 3 brings in:
- `io.ktor:ktor-client-core` (via coil-network-ktor3)
- `org.jetbrains.compose.runtime:runtime` (via coil-compose)
- `okio` (for FileSystem and disk cache)

All are already present in the template; no version conflict resolution needed.

## Koin Integration Requirements

The ImageLoader is provided as a Koin singleton. Required Koin dependencies:

```kotlin
implementation(libs.koin.core)    // Module DSL, single<T>, get<T>
implementation(libs.koin.compose) // koinInject<T>() in composables
```

## Required Companion Dependencies

The ImageAuthInterceptor depends on:
- `StorageService` (Ktorfit-generated API service for signed URL resolution)
- Authorized `HttpClient` (Ktor client with authentication configured)

These are provided by other Koin modules (ktorfitModule) and must be initialized before imageLoaderModule.

## Platform Notes

- **Android:** No additional Coil configuration needed. Coil 3 uses the Android Context provided by the PlatformContext Koin singleton.
- **iOS:** Coil 3 uses NSTemporaryDirectory() internally when FileSystem.SYSTEM_TEMPORARY_DIRECTORY is used for disk cache. This is the correct location for iOS cache files.
