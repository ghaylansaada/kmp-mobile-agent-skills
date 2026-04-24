# Dependency Injection Setup

> Always use the latest stable versions. Check the official release pages for current versions.

## Version Catalog (libs.versions.toml)

```toml
[versions]
koin = "..."

[libraries]
koin-bom = { group = "io.insert-koin", name = "koin-bom", version.ref = "koin" }
koin-core = { group = "io.insert-koin", name = "koin-core" }
koin-compose = { group = "io.insert-koin", name = "koin-compose" }
koin-compose-viewmodel = { group = "io.insert-koin", name = "koin-compose-viewmodel" }
```

All Koin artifacts use the BOM for version alignment. Individual library entries
omit `version.ref` because the BOM manages versions centrally.

## Shared Module build.gradle.kts

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
        }
    }
}
```

## Dependency Breakdown

| Artifact | Purpose |
|----------|---------|
| `koin-bom` | Bill of Materials -- aligns all Koin artifact versions |
| `koin-core` | Core DI container: `module`, `single`, `factory`, `get()` |
| `koin-compose` | `koinInject<T>()` for Compose Multiplatform screens |
| `koin-compose-viewmodel` | `koinViewModel<T>()` and `viewModel { }` DSL for KMP |

## Platform Notes

### Android

No additional Android-specific Koin artifacts are required. The `koin-compose`
and `koin-compose-viewmodel` artifacts are multiplatform and include Android
targets. The Android Context is provided through the PlatformContext wrapper.

### iOS

Koin KMP works natively on iOS via Kotlin/Native. No CocoaPods or SPM
dependencies are needed.

## Gradle Sync Verification

After adding dependencies, sync the project and verify that all Koin artifacts appear in the dependency tree for `commonMainImplementation`.

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| `Unresolved reference: startKoin` | Missing koin-core | Add koin-core to commonMain.dependencies |
| `Unresolved reference: koinInject` | Missing koin-compose | Add koin-compose to commonMain.dependencies |
| `Unresolved reference: viewModel` | Missing koin-compose-viewmodel | Add koin-compose-viewmodel to commonMain.dependencies |
| Version conflict between Koin artifacts | Not using BOM | Use `project.dependencies.platform(libs.koin.bom)` |
| `NoSuchMethodError` at runtime | Mixed Koin versions | Ensure all artifacts come from the same BOM |
| iOS linker error mentioning Koin | Kotlin/Native cinterop issue | Clean build then rebuild |
