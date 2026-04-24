# Integration Reference -- KMP Project Setup

## Upstream Dependencies

None. This is the root skill. It must be completed before any other skill.

---

## Downstream Dependencies

Every other skill in the template depends on this one. Key consumers:

| Skill | What it needs from this skill |
|---|---|
| [kmp-architecture](../../kmp-architecture/SKILL.md) | Source set hierarchy, `-Xexpect-actual-classes` flag, PlatformContext/PlatformConfig types |
| [kmp-kotlin-coroutines](../../kmp-kotlin-coroutines/SKILL.md) | `kotlinx-coroutines-core` dependency in commonMain, dispatcher availability |
| [kmp-networking](../../kmp-networking/SKILL.md) | Ktor, Ktorfit, kotlinx-serialization dependencies; KtorfitFactory converter infrastructure |
| [kmp-release](../../kmp-release/SKILL.md) | ProGuard rules, build types, signing config, BuildKonfig production values |
| [kmp-build-config](../../kmp-build-config/SKILL.md) | Version catalog, plugin versions, repository declarations |

---

## Expect/Actual Connection

The project setup directly enables the expect/actual pattern:

1. **Source set hierarchy** -- `commonMain`, `androidMain`, `iosMain` establish the dependency relationship.
2. **Compiler flag** -- `-Xexpect-actual-classes` enables `expect class` / `actual class` syntax.
3. **Entry point wiring** -- Both `MainActivity.kt` and `MainViewController.kt` instantiate platform-specific types (`PlatformContext`, `PlatformConfig`) and pass them into shared code.

---

## KSP Processor Configuration per Target

KSP processors must be registered per-target because each target has its own compilation:

| KSP Configuration | Room Compiler | Ktorfit KSP |
|---|---|---|
| `kspCommonMainMetadata` | No | Yes |
| `kspAndroid` | Yes | Yes |
| `kspIosArm64` | Yes | Yes |
| `kspIosSimulatorArm64` | Yes | Yes |

Room is per-target because it generates platform-specific database driver code.
Ktorfit is in `kspCommonMainMetadata` because it generates common API interface implementations.

---

## Cross-References

- **Dependency Injection** -- `koin-core`, `koin-compose`, `koin-viewmodel` dependencies declared here are consumed by the DI layer. `initKoin()` call sites are in `MainActivity.kt` and `MainViewController.kt`.
- **Platform Modules** -- `androidMain` and `iosMain` source sets hold platform-specific Koin module bindings (HTTP engine, database driver, DataStore, file I/O).
- **Networking** -- Ktor engine dependencies (`okhttp` in androidMain, `darwin` in iosMain) and Ktorfit KSP registration are established here.
- **Database** -- Room runtime, paging, sqlite-bundled dependencies and KSP registration for all targets; `-lsqlite3` linker option for iOS.

---

## Integration Checklist

1. After changing KSP targets -- verify all targets have `ksp<Target>` dependencies
2. After modifying ProGuard -- run release build and verify app functions
3. After changing gradle.properties -- verify both local and CI builds
4. After updating plugin versions -- re-verify config cache and KSP compatibility
5. After adding BuildKonfig fields -- ensure release pipeline uses production values
