# Dependencies

> Always use the latest stable versions. Check the official release pages for current versions.

## AndroidX Lifecycle ViewModel (KMP)

The JetBrains-published KMP-compatible lifecycle library provides `ViewModel`, `viewModelScope`, and Compose integration across all targets.

```toml
[versions]
androidx-lifecycle = "..."

[libraries]
androidx-lifecycle-viewmodel-compose = { group = "org.jetbrains.androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "androidx-lifecycle" }
androidx-lifecycle-runtime-compose = { group = "org.jetbrains.androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "androidx-lifecycle" }
```

```kotlin
commonMain.dependencies {
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
```

The `org.jetbrains.androidx.lifecycle` artifact is a KMP repackaging of AndroidX Lifecycle. Same API surface, compiles for all Kotlin targets.

## Koin ViewModel Integration

```toml
[libraries]
koin-bom = { group = "io.insert-koin", name = "koin-bom", version.ref = "koin" }
koin-core = { group = "io.insert-koin", name = "koin-core" }
koin-compose = { group = "io.insert-koin", name = "koin-compose" }
koin-compose-viewmodel = { group = "io.insert-koin", name = "koin-compose-viewmodel" }
```

```kotlin
commonMain.dependencies {
    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
}
```

## Coroutines and StateFlow

All state management primitives (`MutableStateFlow`, `StateFlow`, `asStateFlow`, `Channel`) come from `kotlinx-coroutines-core`. The lifecycle-runtime-compose artifact provides `collectAsStateWithLifecycle()`.

```toml
[versions]
kotlinxCoroutines = "..."

[libraries]
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinxCoroutines" }
```

```kotlin
commonMain.dependencies {
    implementation(libs.kotlinx.coroutines.core)
}
```

## Expect/Actual Compiler Flag

The template enables `expect`/`actual` classes via a free compiler argument in `composeApp/build.gradle.kts`:

```kotlin
kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
```

This flag is required for `expect class` and `expect object`. The `expect fun` form does not require it. The flag applies to all source sets because it is set at the top-level `kotlin {}` block.

## Verification

After applying dependencies, verify these imports resolve without errors:

```kotlin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.channels.Channel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
```
