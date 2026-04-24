# Setup Reference -- KMP Project Setup

> Always use the latest stable versions. Check the official release pages for current versions.

## Version Catalog

The version catalog lives at `gradle/libs.versions.toml` and is the single source of truth
for every dependency and plugin used in the project. All module build scripts reference it via the type-safe `libs.*` accessor
that Gradle generates when `TYPESAFE_PROJECT_ACCESSORS` is enabled in `settings.gradle.kts`.

Full artifact coordinates are in [version-catalog-starter.toml](../assets/snippets/version-catalog-starter.toml).

### Key Compatibility Constraints

- Since Kotlin 2.0, the Compose Compiler plugin uses the same version as Kotlin itself
  (`version.ref = "kotlin"`). There is no separate Compose Compiler version to track.
- The KSP version must be compatible with the Kotlin version (same major.minor prefix).
- Material3 (`org.jetbrains.compose.material3:material3`) is versioned independently from
  the Compose Multiplatform BOM -- it has its own version entry in the catalog.
- The Ktorfit compiler plugin version may differ from the Ktorfit runtime library version.
  The `ktorfit { compilerPluginVersion.set("...") }` block in the module build script
  controls the compiler plugin version explicitly.

---

## Gradle Plugin Chain

The project uses a two-level plugin application pattern. The root build script declares
all plugins with `apply false` to load them into the classpath once.

### Plugin Application Order (module level)

1. **kotlinMultiplatform** -- establishes the multiplatform project model
2. **androidApplication** -- configures the Android application target
3. **composeMultiplatform** -- adds Compose Multiplatform runtime and resource generation
4. **composeCompiler** -- applies the Kotlin Compose compiler plugin
5. **kotlinSerialization** -- enables `@Serializable` annotation processing
6. **ksp** -- KSP symbol processing for Room and Ktorfit code generation
7. **ktorfitGradle** -- Ktorfit Gradle plugin for compiler plugin configuration

---

## Repository Configuration

Repository declarations live in `settings.gradle.kts` and apply to both plugin resolution
and dependency resolution. The `google()` repository uses `mavenContent` filters to restrict
which groups it resolves, improving build performance.

- `pluginManagement` includes `gradlePluginPortal()` for resolving Gradle plugins.
- `dependencyResolutionManagement` excludes `gradlePluginPortal()` to prevent non-plugin
  artifacts from being resolved through it.
- The `foojay-resolver-convention` plugin enables automatic JDK toolchain downloads but
  does NOT provision Xcode or the iOS toolchain.

---

## Gradle Properties

Key properties in `gradle.properties`:

- `org.gradle.configuration-cache=false` -- Disabled because KSP and some KMP plugins
  do not yet fully support configuration cache.
- `kotlin.daemon.jvmargs=-Xmx3072M` -- Kotlin compiler daemon needs generous heap for KMP.
- `org.gradle.jvmargs=-Xmx4096M` -- Gradle daemon heap must be larger than Kotlin daemon.
- `org.gradle.caching=true` -- Enables the Gradle build cache for incremental builds.
- `android.builtInKotlin=false` -- Prevents AGP from bundling its own Kotlin, avoiding
  conflicts with KMP Kotlin.
