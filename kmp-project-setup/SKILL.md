---
name: kmp-project-setup
description: >
  Scaffold and configure a Kotlin Multiplatform project with Compose Multiplatform
  targeting Android and iOS. Covers Gradle version catalog, root and module build files,
  source set organization, iOS framework embedding, initial project skeleton, build variant
  management (debug/release), per-target KSP processor registration, ProGuard/R8 keep rules,
  Gradle daemon tuning, build caching, and composite builds. Use this skill when starting a
  new KMP project, adding a KMP module, troubleshooting Gradle sync failures, configuring
  Xcode to consume the shared framework, or optimizing build performance.
compatibility: >
  KMP with Compose Multiplatform. Requires Gradle 8.x+ with Kotlin DSL. JDK 21.
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---

# KMP Project Setup

## When to use

- Starting a new Kotlin Multiplatform project from scratch
- Adding a new KMP module to an existing project
- Troubleshooting Gradle sync or configuration failures
- Setting up the version catalog and plugin management
- Configuring Xcode to consume the KMP iOS framework
- Understanding or modifying source set organization (commonMain, androidMain, iosMain)
- Adjusting Android SDK versions, JVM target, or iOS architecture targets
- Configuring Android build types (debug/release) with ProGuard
- Adding KSP processors and needing per-target declarations
- Tuning Gradle or Kotlin daemon memory to fix OOM errors
- Setting up build caching (local or remote)
- Debugging slow builds or unexpected cache misses
- Configuring composite builds for multi-repo development

## Depends on

None -- this is the root skill.

## Workflow

1. Set up version catalog and Gradle plugin chain -- [references/setup.md](references/setup.md)
2. Create root build.gradle.kts and settings.gradle.kts -- [references/build-files.md](references/build-files.md)
3. Scaffold composeApp module and source set layout -- [references/project-structure.md](references/project-structure.md)
4. Wire Android and iOS entry points -- [references/entry-points.md](references/entry-points.md)
5. Configure advanced Gradle: build variants, KSP, ProGuard, caching, daemon tuning -- [references/gradle-advanced.md](references/gradle-advanced.md)
6. Connect to downstream skills and modules -- [references/integration.md](references/integration.md)

## Gotchas

1. **Configuration cache breaks KSP.** Gradle configuration cache is incompatible with most KMP code-generation plugins (Room KSP, Ktorfit KSP). If enabled, you get cryptic serialization errors at configuration time. Disable it with `org.gradle.configuration-cache=false` in `gradle.properties`.
2. **TYPESAFE_PROJECT_ACCESSORS requires sync.** Adding a new module requires a Gradle sync before `projects.moduleName` becomes available. Calling it before sync produces an "unresolved reference" that looks like a typo.
3. **foojay does not provision Xcode.** The `foojay-resolver-convention` plugin auto-provisions JDKs but NOT the iOS toolchain. CI builds relying on foojay will still fail iOS compilation without Xcode pre-installed.
4. **Missing `-lsqlite3` crashes iOS.** Required when using Room with `BundledSQLiteDriver`. Without it, the iOS linker fails with `Undefined symbols for architecture arm64: _sqlite3_open`.
5. **`isStatic = true` vs `false`.** Static means no "Copy Frameworks" phase needed. Switching to dynamic requires a "Copy Frameworks" build phase AND codesigning the `.framework` -- missing either causes a launch-time crash.
6. **KSP processors must be declared per-target.** Using generic `ksp` only runs for metadata compilation. Forgetting a target means no code generation for that platform -- builds compile but crash at runtime with `ClassNotFoundException` for generated classes (e.g., Room `_Impl`).
7. **Material3 versioned independently.** `org.jetbrains.compose.material3:material3` follows its own release cadence, separate from the Compose Multiplatform BOM version.
8. **`initKoin()` in iOS must run once.** Placing `initKoin()` inside the `ComposeUIViewController` content lambda causes it to execute on every recomposition. Call it before constructing the controller or guard with a flag.
9. **Ktorfit compiler plugin version.** The `ktorfit { compilerPluginVersion.set("...") }` block is required in the module build script. Omitting it causes the Ktorfit compiler plugin to default to an incompatible version. See **kmp-networking** skill for Ktorfit setup.
10. **`allprojects {}` and `subprojects {}` break project isolation.** Both cause cross-project configuration coupling that defeats configuration cache and slows parallel execution. Use convention plugins in `build-logic/` to share configuration instead.
11. **Version catalog must live at `gradle/libs.versions.toml` exactly.** Placing it elsewhere causes Gradle to silently ignore it and all `libs.*` references become unresolved.
12. **Setting `workers.max` above physical CPU cores causes GC pressure.** Each worker holds compilation state in memory. Exceeding physical cores triggers excessive context switching and garbage collection, making builds slower, not faster. Set to physical cores minus one.

## Assets

| Path | Load when... |
|------|-------------|
| [assets/templates/project-skeleton.gradle.kts.template](assets/templates/project-skeleton.gradle.kts.template) | Creating a new composeApp module |
| [assets/snippets/version-catalog-starter.toml](assets/snippets/version-catalog-starter.toml) | Setting up a minimal libs.versions.toml with core KMP dependencies |
| [assets/snippets/gradle-properties-optimized.properties](assets/snippets/gradle-properties-optimized.properties) | Configuring production-ready gradle.properties |
| [assets/templates/proguard-rules.pro.template](assets/templates/proguard-rules.pro.template) | Adding ProGuard rules (pointer to kmp-release's canonical template) |

## Validation

### A. Project setup correctness

- [ ] `gradle/libs.versions.toml` exists with all artifact coordinates and latest stable versions
- [ ] Version catalog has separate version entries for independently-versioned artifacts (Material3, KSP)
- [ ] Root `build.gradle.kts` declares all plugins with `apply false`
- [ ] `settings.gradle.kts` has `TYPESAFE_PROJECT_ACCESSORS`, foojay plugin, filtered repositories
- [ ] `dependencyResolutionManagement` excludes `gradlePluginPortal()`
- [ ] `gradle.properties` has `org.gradle.configuration-cache=false` and adequate memory settings
- [ ] `gradle.properties` has `android.builtInKotlin=false` to prevent AGP Kotlin conflicts
- [ ] Gradle wrapper files committed (`gradle/wrapper/gradle-wrapper.jar`, `gradle-wrapper.properties`)
- [ ] `.gitignore` covers `build/`, `.gradle/`, `.idea/`, `*.iml`, `local.properties`
- [ ] Kotlin block declares `androidTarget` with JVM 25 and both iOS targets with static framework
- [ ] All four source set dependency blocks (`commonMain`, `commonTest`, `androidMain`, `iosMain`) present
- [ ] `-Xexpect-actual-classes` compiler flag is set
- [ ] KSP processors registered per-target (not via generic `ksp`)
- [ ] `ktorfit { compilerPluginVersion.set("...") }` block present
- [ ] `ksp { arg("room.schemaLocation", ...) }` block present with incremental processing
- [ ] `proguard-rules.pro` keeps Room database implementation classes

### B. Gradle correctness

- [ ] Version catalog used consistently -- no hardcoded versions in build files
- [ ] Convention plugins avoid hardcoded values; read from version catalog or extension properties
- [ ] No deprecated Gradle APIs (`allprojects {}`, `subprojects {}` without isolation)
- [ ] No deprecated Kotlin/Native properties (`kotlin.native.binary.memoryModel`)
- [ ] Build cache enabled via `org.gradle.caching=true`
- [ ] Gradle wrapper pinned to a specific version
- [ ] JDK toolchain version set to 21 (current LTS)
- [ ] `org.gradle.parallel=true` enabled
- [ ] Task dependencies are correct -- no `mustRunAfter` hacks for ordering

### C. Entry points

- [ ] `MainActivity.kt` calls `enableEdgeToEdge()`, `initKoin()`, and `setContent { App() }`
- [ ] `MainViewController.kt` returns `ComposeUIViewController` with `initKoin()` and `App()`
- [ ] `initKoin()` is NOT called inside the `ComposeUIViewController` content lambda

### D. Security

- [ ] No hardcoded API keys, secrets, or credentials in build files or templates
- [ ] ProGuard/R8 enabled for release builds with `isMinifyEnabled = true`
- [ ] Remote build cache credentials read from environment variables, not hardcoded

### E. Performance

- [ ] Gradle daemon heap (`org.gradle.jvmargs`) >= Kotlin daemon heap (`kotlin.daemon.jvmargs`)
- [ ] Gradle daemon heap set to 4096M minimum
- [ ] Kotlin compiler daemon heap set to 3072M minimum
- [ ] Build cache enabled (`org.gradle.caching=true`)
- [ ] Google repository uses `mavenContent` filters to limit resolution scope
- [ ] `ksp.incremental=true` enabled
- [ ] `org.gradle.vfs.watch=true` for local development
- [ ] No timestamp-based BuildKonfig fields that invalidate cache

### F. Integration

- [ ] Downstream skills (`kmp-architecture`, `kmp-kotlin-coroutines`, `kmp-networking`) reference this skill in their `Depends on` section
- [ ] Dependencies declared here (coroutines, Ktor, Koin, Room) are consistent with what downstream skills consume
- [ ] ProGuard template pointer references kmp-release correctly
- [ ] Integration checklist covers cross-skill verification steps
