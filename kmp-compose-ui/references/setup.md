# Setup: Compose Multiplatform and Material 3 Dependencies

> **Note:** Version numbers are intentionally omitted. Always use the latest stable release from the official documentation.

## Version Catalog (libs.versions.toml)

### Libraries

```toml
[libraries]
# Compose Multiplatform core
compose-runtime = { module = "org.jetbrains.compose.runtime:runtime", version.ref = "composeMultiplatform" }
compose-foundation = { module = "org.jetbrains.compose.foundation:foundation", version.ref = "composeMultiplatform" }
compose-material3 = { module = "org.jetbrains.compose.material3:material3", version.ref = "material3" }
compose-ui = { module = "org.jetbrains.compose.ui:ui", version.ref = "composeMultiplatform" }
compose-components-resources = { module = "org.jetbrains.compose.components:components-resources", version.ref = "composeMultiplatform" }
compose-uiToolingPreview = { module = "org.jetbrains.compose.ui:ui-tooling-preview", version.ref = "composeMultiplatform" }

# Lifecycle
androidx-lifecycle-runtimeCompose = { module = "org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose", version.ref = "androidx-lifecycle" }
androidx-lifecycle-viewmodelCompose = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "androidx-lifecycle" }

# Koin (for composable injection)
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin" }
koin-viewmodel = { module = "io.insert-koin:koin-core-viewmodel", version.ref = "koin" }

# Paging (for LazyPagingItems)
paging-common = { module = "androidx.paging:paging-common", version.ref = "paging" }
paging-compose = { module = "androidx.paging:paging-compose", version.ref = "paging" }
paging-runtime = { module = "androidx.paging:paging-runtime", version.ref = "paging" }

# Android-specific
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "androidx-activity" }
```

### Optional: Material Icons Extended

If you need icons beyond the default set, add the extended icons artifact:

```toml
[libraries]
compose-material3-icons-extended = { module = "org.jetbrains.compose.material:material-icons-extended", version.ref = "composeMultiplatform" }
```

Warning: material-icons-extended is large (~30MB). Only add it if you need icons not in the default set. For production apps, consider importing only the specific icon files you need.

### Plugins

```toml
[plugins]
composeMultiplatform = { id = "org.jetbrains.compose", version.ref = "composeMultiplatform" }
composeCompiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

## build.gradle.kts Plugin Configuration

Both the Compose Multiplatform plugin and the Compose Compiler plugin must be applied:

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktorfitGradle)
}
```

## Source Set Dependencies

### commonMain

```kotlin
sourceSets {
    commonMain.dependencies {
        implementation(libs.compose.runtime)
        implementation(libs.compose.foundation)
        implementation(libs.compose.material3)
        implementation(libs.compose.ui)
        implementation(libs.compose.components.resources)
        implementation(libs.compose.uiToolingPreview)
        implementation(libs.androidx.lifecycle.viewmodelCompose)
        implementation(libs.androidx.lifecycle.runtimeCompose)

        implementation(libs.koin.core)
        implementation(libs.koin.compose)
        implementation(libs.koin.viewmodel)

        implementation(libs.paging.common)
        implementation(libs.paging.compose)
    }
}
```

### androidMain

```kotlin
sourceSets {
    androidMain.dependencies {
        implementation(libs.compose.uiToolingPreview)
        implementation(libs.androidx.activity.compose)
        implementation(libs.paging.runtime)
    }
}
```

`compose-uiToolingPreview` appears in both source sets because the Compose Multiplatform version is the cross-platform abstraction while Android also needs it for Android Studio preview support.

## iOS Framework Configuration

The iOS framework must be static for Compose Multiplatform:

```kotlin
kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
}
```

`isStatic = true` is required because Compose Multiplatform does not support dynamic frameworks on iOS.

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| `Unresolved reference: ModalBottomSheet` | Material3 version too old | Verify material3 version in catalog |
| `Unresolved reference: Icons.Default.X` | Icon not in default set | Add `material-icons-extended` or use a different icon |
| `@Preview not working on iOS` | Preview is Android-only at runtime | Use `@Preview` from `compose-uiToolingPreview` for IDE preview only |
| `Modifier.semantics` not found | Missing UI dependency | Ensure `compose-ui` is in dependencies |
