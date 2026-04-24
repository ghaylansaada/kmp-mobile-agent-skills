# Setup

> Always use the latest stable versions. Check the official release pages for current versions.

## Gradle Dependencies

```toml
[versions]
kotlinxCoroutines = "..."

[libraries]
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinxCoroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinxCoroutines" }
```

```kotlin
commonMain.dependencies {
    implementation(libs.kotlinx.coroutines.core)
}

commonTest.dependencies {
    implementation(libs.kotlinx.coroutines.test)
}
```

No platform-specific coroutine dependencies needed.

## Dispatcher Availability

| Dispatcher | Android (JVM) | iOS (Native) | Notes |
|------------|--------------|-------------|-------|
| `Dispatchers.Default` | Available | Available | CPU-bound work |
| `Dispatchers.Main` | Available | Available | UI thread; Compose Multiplatform provides it on iOS |
| `Dispatchers.IO` | Available | Available | Requires `import kotlinx.coroutines.IO` on common/iOS |
| `Dispatchers.Unconfined` | Available | Available | Resumes in caller's context |

### Dispatchers.IO Import

On common and iOS source sets, `Dispatchers.IO` requires an explicit import:

```kotlin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
```

On Android/JVM it works without the extra import, but the explicit import is recommended for KMP code.

### Template Convention

The template uses `Dispatchers.IO` on Android and `Dispatchers.Default` on iOS for file I/O. This is a legacy convention from before `Dispatchers.IO` was available on Kotlin/Native. Both are valid.
