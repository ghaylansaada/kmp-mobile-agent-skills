---
name: kmp-build-config
description: >
  Use this skill when configuring multi-environment build settings for KMP using
  BuildKonfig. Activate when the user asks to "add staging config," "create
  feature flags," "switch API URLs per environment," "set up build flavors," or
  "migrate hardcoded config." Covers BuildKonfig plugin setup, per-flavor values
  (dev/staging/prod), FeatureFlags registry, BuildEnvironment enum, AppConfig
  wrapper, and platform constants via expect/actual. Does NOT cover CI/CD
  pipelines (see kmp-ci-cd), ProGuard/R8 rules beyond BuildKonfig (see
  kmp-platform-integration), or remote feature flags (Firebase Remote Config).
compatibility: >
  KMP with Compose Multiplatform. Requires BuildKonfig Gradle plugin.
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---

# KMP Build Configuration

## Scope

Covers multi-environment build configuration using BuildKonfig: plugin setup, per-flavor field definitions (dev/staging/prod), BuildEnvironment enum, FeatureFlags registry, AppConfig wrapper, compile-time Constants, PlatformConstants via expect/actual, migration from hardcoded values, and integration with Ktor/Koin. Does not cover CI/CD, advanced ProGuard, or remote feature flags.

## When to use

- Configuring different API base URLs per environment (dev, staging, prod)
- Adding feature flags that vary by build variant or flavor
- Setting up BuildKonfig for compile-time configuration values
- Managing per-flavor signing, app IDs, or app names
- Injecting environment-specific secrets at build time
- Switching between mock and real service endpoints
- Adding build-time constants accessible from common code
- Configuring analytics or logging behavior per environment

## Depends on

- **kmp-project-setup** -- project structure, version catalog, plugin chain, Gradle properties, build variant management

## Workflow

1. **Install BuildKonfig plugin and configure flavors** --> read `references/setup.md`
   _Skip if BuildKonfig is already in the project._
2. **Create source files** (BuildEnvironment, FeatureFlags, AppConfig, Constants, PlatformConstants) --> read `references/source-files.md`
3. **Migrate hardcoded values and wire feature flags** --> read `references/integration.md`
4. **Copy-paste full BuildKonfig block** --> use template at `assets/templates/buildkonfig-block.gradle.kts`
   _Load when setting up BuildKonfig from scratch._

## Gotchas

1. **Android `BuildConfig` is NOT `BuildKonfig`.** Android's `BuildConfig` is generated per-module in the Android source set only. BuildKonfig generates into commonMain. Importing the wrong one causes `Unresolved reference` on iOS and in commonMain code.
2. **Android `productFlavors` do NOT propagate to commonMain.** BuildKonfig flavors are controlled by `-Pflavor=` and affect all KMP targets. Using Android `productFlavors` has zero effect on the shared module's generated config.
3. **BuildKonfig generates `val`, not `const val`.** Code requiring compile-time constants (annotation arguments, `when` exhaustive checks on strings) cannot use BuildKonfig fields. Use `Constants.kt` for those values, or the compiler rejects them with `Const val required`.
4. **Missing flavor field silently inherits the dev default.** Every non-default flavor must redefine every field. Omitting `BASE_URL` from the `prod` flavor means production traffic hits the dev server -- a data-leak and outage risk.
5. **Gradle caches stale BuildKonfig values.** When switching flavors, always run a clean build first. Without it, the old flavor's values remain in the build output and the app connects to the wrong environment.
6. **Never put secrets in BuildKonfig.** API keys, client secrets, and signing tokens placed in BuildKonfig end up as plain-text string literals in the compiled binary. Attackers can extract them with `strings` or a decompiler. Use `local.properties` or environment variables and read them at build time via `project.findProperty()`.
7. **iOS build fails on BuildKonfig/Kotlin version mismatch.** BuildKonfig uses a Kotlin compiler plugin internally. If you upgrade Kotlin without upgrading BuildKonfig (or vice versa), the iOS framework link step fails with `Incompatible ABI version` or `Unresolved reference`.
8. **Use expect/actual for per-PLATFORM values, BuildKonfig for per-ENVIRONMENT values.** Mixing these up (e.g., putting a database path in BuildKonfig) means the value is the same on both platforms, which breaks platform-specific filesystem conventions.

## Assets

| Path | Load when... |
|------|-------------|
| [references/setup.md](references/setup.md) | Adding BuildKonfig plugin, version catalog, flavor property |
| [references/source-files.md](references/source-files.md) | Creating BuildEnvironment, FeatureFlags, AppConfig, Constants, PlatformConstants |
| [references/integration.md](references/integration.md) | Migrating hardcoded values, wiring Ktor/Koin |
| [assets/templates/buildkonfig-block.gradle.kts](assets/templates/buildkonfig-block.gradle.kts) | Full BuildKonfig block for copy-paste setup |

## Validation

### A. Kotlin and KMP correctness
- [ ] No unresolved imports in any source file
- [ ] BuildKonfig fields use proper types (`BOOLEAN` for flags, `INT` for numbers, `STRING` for text) -- not all `STRING`
- [ ] Every field defined in `defaultConfigs` is also defined in every named flavor (staging, prod)
- [ ] `BuildEnvironment.current` maps every flavor string to the correct enum entry
- [ ] `AppConfig` wrapper exposes fields with correct Kotlin types matching BuildKonfig generation
- [ ] `PlatformConstants` uses expect/actual only for values that genuinely differ by platform
- [ ] `Constants.kt` uses `const val` for compile-time constants that do not vary by environment
- [ ] Version catalog entry for BuildKonfig includes both version and plugin ID

### B. Security
- [ ] No API keys, client secrets, or signing tokens appear in BuildKonfig fields
- [ ] No secrets in `gradle.properties`, `buildkonfig-block.gradle.kts`, or any reference file
- [ ] Secret handling guidance points to `local.properties` or environment variables

### C. Performance
- [ ] Dev flavor enables logging and network inspector; prod flavor disables both
- [ ] Prod timeout is lower than dev timeout (fail-fast in production)
- [ ] No unnecessary fields that increase generated code size

### D. Integration
- [ ] Ktor client example reads `BASE_URL` and `API_TIMEOUT_SECONDS` from `AppConfig`, not hardcoded
- [ ] Koin module uses `FeatureFlags` for mock/real implementation switching
- [ ] `references/integration.md` shows the migration path from hardcoded values
