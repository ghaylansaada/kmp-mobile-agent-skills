# Advanced Gradle Configuration

## Gradle Properties and Daemon Tuning

See [gradle-properties-optimized.properties](../assets/snippets/gradle-properties-optimized.properties)
for the complete, production-ready `gradle.properties` with inline documentation.

Key properties summary:

| Property | Value | Purpose |
|---|---|---|
| `org.gradle.jvmargs` | `-Xmx4096M -Dfile.encoding=UTF-8 -XX:+UseG1GC` | Gradle daemon heap (4GB min for KMP) |
| `kotlin.daemon.jvmargs` | `-Xmx3072M -XX:+UseG1GC` | Kotlin compiler daemon (separate process) |
| `org.gradle.caching` | `true` | Build cache for cross-target reuse |
| `org.gradle.configuration-cache` | `false` | Disabled -- KMP plugins incompatible |
| `org.gradle.parallel` | `true` | Android and iOS compilations run concurrently |

## Android Build Types

In `composeApp/build.gradle.kts`:

```kotlin
android {
    namespace = "{your.package}"
    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

## iOS Build Configurations

Build variants are managed through Xcode schemes and Kotlin/Native compiler flags:

```kotlin
kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            if (buildType == NativeBuildType.DEBUG) {
                freeCompilerArgs += listOf("-Xadd-light-debug=enable")
            }
        }
    }
}
```

## KSP Per-Target Configuration

KSP processors must be declared per compilation target. The generic `ksp` configuration
only runs for metadata compilation.

```kotlin
dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
    add("kspCommonMainMetadata", libs.room.compiler)
}
```

### Room KSP Arguments

```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/roomSchemas")
    arg("room.generateKotlin", "true")
}
```

### Ktorfit Compiler Plugin

Ktorfit uses a Kotlin compiler plugin (not KSP). Apply the Ktorfit Gradle plugin from
the version catalog:

```kotlin
plugins {
    alias(libs.plugins.ktorfit)
}
```

The Ktorfit Gradle plugin version may differ from the Ktorfit library version -- the plugin
tracks Kotlin compiler internals while the library tracks API changes.

## ProGuard / R8

When `isMinifyEnabled = true`, R8 processes the APK. KMP libraries use reflection,
serialization, and code generation that R8 cannot analyze statically. Place
`proguard-rules.pro` in `composeApp/` -- see
[proguard-rules.pro.template](../assets/templates/proguard-rules.pro.template)
for the complete rule set reference.

Library-specific concerns:
- **Room**: Entity, Dao, Database classes use reflection for instantiation
- **Ktor**: ServiceLoader for engine discovery and content negotiation plugins
- **Koin**: Runtime type resolution for all module declarations
- **Ktorfit**: Generated `_KtorfitImpl` classes must not be removed
- **kotlinx.serialization**: Companion object serializers from `@Serializable`
- **Coil**: Reflection-based decoder and fetcher discovery

## Gradle Caching

### Local Build Cache

Enabled via `org.gradle.caching=true`. Cache lives in `~/.gradle/caches/build-cache-1/`.

### Remote Build Cache

For team environments and CI:

```kotlin
// settings.gradle.kts
buildCache {
    local { isEnabled = true }
    remote<HttpBuildCache> {
        url = uri("https://cache.example.com/cache/")
        isPush = System.getenv("CI") != null
        credentials {
            username = System.getenv("CACHE_USER") ?: ""
            password = System.getenv("CACHE_PASSWORD") ?: ""
        }
    }
}
```

### Cache Invalidation

Common causes of unexpected misses:
- Changing `gradle.properties` values (invalidates all tasks)
- Upgrading Kotlin or KSP (invalidates all compilation tasks)
- Modifying `build.gradle.kts` (may invalidate dependent tasks)
- Different JDK versions between machines (use foojay resolver)

## Composite Builds

For shared library development without publishing:

```kotlin
// settings.gradle.kts
includeBuild("../shared-kmp-library") {
    dependencySubstitution {
        substitute(module("com.example:shared-library"))
            .using(project(":shared-library"))
    }
}
```

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| KSP generated code missing for target | `ksp<Target>` dependency not declared | Add per-target KSP deps |
| `OutOfMemoryError` during compilation | Insufficient daemon/compiler heap | Increase to `-Xmx4096M` / `-Xmx3072M` |
| Release crashes with `ClassNotFoundException` | R8 removed reflected class | Check `usage.txt`, add keep rule |
| `Configuration cache state could not be cached` | Plugin doesn't support config cache | Keep `configuration-cache=false` |
| Ktorfit compilation failure | Wrong `compilerPluginVersion` | Match version to Ktorfit docs |
| `Room cannot verify data integrity` | Missing schema JSON or migration | Export schemas via KSP args |
| Cache misses when nothing changed | Non-deterministic inputs (timestamps) | Avoid timestamps in BuildKonfig |
| `allprojects {}` deprecation warning | Using deprecated Gradle API | Use convention plugins |
