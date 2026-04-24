---
name: kmp-mobile
description: >
  Orchestrator skill for Kotlin Multiplatform mobile development targeting Android and iOS.
  Routes to 37 specialized skills covering project setup, networking, database, UI, navigation,
  platform integration, security, testing, CI/CD, and release. Activate on ANY mobile development
  request: new project, new feature, new screen, API integration, database, authentication,
  push notifications, background work, file handling, image loading, animations, accessibility,
  adaptive layouts, localization, analytics, performance, testing, CI/CD, app release, or any
  Kotlin/KMP/Compose Multiplatform question. This is the entry point for all KMP mobile work.
compatibility: >
  KMP with Compose Multiplatform targeting Android and iOS. Kotlin 2.3+, Android 16 (API 36), iOS 26.
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---

# KMP Mobile

## Scope

Entry point for all Kotlin Multiplatform mobile development. This skill contains zero implementation -- it routes to specialized skills that own each domain. Read the routing table below, identify which skill(s) match the task, then open their SKILL.md files for implementation guidance.

## When to use

- Any mobile development request involving Kotlin Multiplatform
- Creating a new KMP project or adding features to an existing one
- Building UI screens, networking, database, or platform integrations
- Setting up authentication, security, push notifications, or background work
- Adding animations, accessibility, adaptive layouts, or localization
- Writing tests, configuring CI/CD, or preparing a release
- Any question about KMP architecture, patterns, or best practices
- When unsure which specific skill applies to a task

## Depends on

None -- this is the root orchestrator.

## Workflow

1. Read the user's request and identify the domain(s) involved
2. Match against the routing table below to find the right skill(s)
3. Open the matched skill's SKILL.md for detailed workflow and references
4. For cross-cutting tasks, open multiple skills in the order listed

## Routing Table

### Project Foundation

| Intent / Keywords | Route to |
|---|---|
| New project, scaffold, Gradle setup, version catalog, source sets, iOS framework | [kmp-project-setup](../kmp-project-setup/SKILL.md) |
| Build config, API URLs, environments, feature flags, BuildKonfig, flavors | [kmp-build-config](../kmp-build-config/SKILL.md) |
| Clean architecture, ViewModel, Repository, UseCase, UiState, UDF, expect/actual | [kmp-architecture](../kmp-architecture/SKILL.md) |
| Koin, dependency injection, modules, platform wiring, ViewModel scoping | [kmp-dependency-injection](../kmp-dependency-injection/SKILL.md) |
| Kotlin conventions, naming, KDoc, type safety, null safety, sealed interface, value class | [kmp-kotlin](../kmp-kotlin/SKILL.md) |
| Build commands, run Android/iOS, test, dependency verification, troubleshooting | [kmp-build](../kmp-build/SKILL.md) |

### Networking and Data

| Intent / Keywords | Route to |
|---|---|
| HTTP, API, Ktor, Ktorfit, REST, Bearer auth, token refresh, pagination, ApiResult | [kmp-networking](../kmp-networking/SKILL.md) |
| Database, Room, entities, DAOs, migrations, type converters | [kmp-database](../kmp-database/SKILL.md) |
| DataStore, preferences, key-value storage, reactive reads | [kmp-datastore](../kmp-datastore/SKILL.md) |
| Paging, pagination, RemoteMediator, PagingSource, infinite scroll, offline-first list | [kmp-paging](../kmp-paging/SKILL.md) |
| Network monitor, offline, connectivity, sync-on-reconnect, offline queue | [kmp-connectivity](../kmp-connectivity/SKILL.md) |
| File upload/download, chunked transfer, resumable, progress, pause/resume | [kmp-transfer](../kmp-transfer/SKILL.md) |

### UI and Presentation

| Intent / Keywords | Route to |
|---|---|
| Compose UI, Material 3, components, design tokens, dark theme, shimmer, loading, previews | [kmp-compose-ui](../kmp-compose-ui/SKILL.md) |
| Adaptive, responsive, tablet, foldable, window size classes, NavigationSuiteScaffold, list-detail, RTL | [kmp-adaptive-ui](../kmp-adaptive-ui/SKILL.md) |
| Accessibility, semantics, VoiceOver, TalkBack, content descriptions, touch targets, focus | [kmp-accessibility](../kmp-accessibility/SKILL.md) |
| Animation, AnimatedVisibility, AnimatedContent, shared element, gestures, Lottie, motion | [kmp-animation](../kmp-animation/SKILL.md) |
| Navigation, Navigation 3, NavDisplay, NavKey, back stack, deep links, auth gate, routes | [kmp-navigation](../kmp-navigation/SKILL.md) |
| Resources, drawables, strings, fonts, composeResources, Res class, density qualifiers | [kmp-resources-management](../kmp-resources-management/SKILL.md) |
| Localization, i18n, plurals, locale switching, RTL, translations, error messages | [kmp-localization](../kmp-localization/SKILL.md) |
| Image loading, Coil, caching, SVG, signed URL, AsyncImage | [kmp-image-loader](../kmp-image-loader/SKILL.md) |

### Platform Integration

| Intent / Keywords | Route to |
|---|---|
| Platform layer, MainActivity, MainViewController, privacy manifests, predictive back | [kmp-platform-integration](../kmp-platform-integration/SKILL.md) |
| Push notifications, FCM, APNs, local notifications, channels, deep links from notification | [kmp-notifications](../kmp-notifications/SKILL.md) |
| Permissions, camera, location, runtime permission, rationale, Android 16 changes, iOS privacy | [kmp-permissions](../kmp-permissions/SKILL.md) |
| Background work, WorkManager, BGTaskScheduler, foreground service, periodic sync | [kmp-background-job](../kmp-background-job/SKILL.md) |
| App update, force update, in-app update, version check, Play Core, iTunes Lookup | [kmp-app-update](../kmp-app-update/SKILL.md) |

### Async and Data Flow

| Intent / Keywords | Route to |
|---|---|
| Coroutines, Flow, StateFlow, SharedFlow, async, suspend, Dispatchers, Job, Mutex, Channel, SKIE | [kmp-kotlin-coroutines](../kmp-kotlin-coroutines/SKILL.md) |
| Date, time, timezone, kotlinx-datetime, formatting, serializers | [kmp-datetime](../kmp-datetime/SKILL.md) |
| Logging, Kermit, tagged logger, severity, release stripping | [kmp-logging](../kmp-logging/SKILL.md) |

### Security

| Intent / Keywords | Route to |
|---|---|
| Security, keystore, certificate pinning, encrypted database, root/jailbreak detection | [kmp-security](../kmp-security/SKILL.md) |
| Session, login, logout, token storage, SessionState, token refresh | [kmp-session-management](../kmp-session-management/SKILL.md) |
| Biometrics, fingerprint, face ID, BiometricPrompt, LAContext, credential storage | [kmp-biometrics](../kmp-biometrics/SKILL.md) |
| Crypto, SHA-256, HMAC, AES-GCM, PBKDF2, hashing, encryption, secure random | [kmp-crypto](../kmp-crypto/SKILL.md) |

### Testing

| Intent / Keywords | Route to |
|---|---|
| Testing, unit test, UI test, kotlin.test, Turbine, MockEngine, fakes, Robolectric, XCTest | [kmp-testing](../kmp-testing/SKILL.md) |

### Analytics and Observability

| Intent / Keywords | Route to |
|---|---|
| Analytics, crashlytics, Firebase, event tracking, crash reporting, consent | [kmp-analytics-crashlytics](../kmp-analytics-crashlytics/SKILL.md) |
| Performance, startup, Baseline Profiles, memory leaks, Compose stability, StrictMode | [kmp-performance](../kmp-performance/SKILL.md) |

### Release and CI/CD

| Intent / Keywords | Route to |
|---|---|
| CI/CD, GitHub Actions, GitLab CI, Fastlane, version bumping, pipeline | [kmp-ci-cd](../kmp-ci-cd/SKILL.md) |
| Release, signing, ProGuard, R8, store deployment, TestFlight, dSYM, app bundle | [kmp-release](../kmp-release/SKILL.md) |

## Common Multi-Skill Workflows

| Task | Skills to open (in order) |
|---|---|
| New feature end-to-end | kmp-architecture → kmp-networking → kmp-database → kmp-compose-ui → kmp-navigation → kmp-dependency-injection |
| New project from scratch | kmp-project-setup → kmp-build-config → kmp-architecture → kmp-dependency-injection → kmp-compose-ui → kmp-navigation |
| Add authentication | kmp-session-management → kmp-networking → kmp-biometrics → kmp-security → kmp-navigation |
| Prepare for release | kmp-performance → kmp-testing → kmp-ci-cd → kmp-release |
| Add offline support | kmp-connectivity → kmp-database → kmp-paging → kmp-kotlin-coroutines |
| Internationalize the app | kmp-localization → kmp-resources-management → kmp-adaptive-ui → kmp-accessibility |

## Gotchas

1. **Do not implement from this skill.** This is a routing-only orchestrator. All code patterns, examples, and templates live in the child skills. If you find yourself writing code guided only by this file, you skipped a step -- open the child skill.
2. **Cross-cutting tasks need multiple skills.** A "new feature" touches architecture, networking, UI, navigation, and DI. Open each skill in sequence rather than guessing the combined pattern.
3. **Domain isolation is strict.** Each child skill owns exactly one domain. Do not mix content from one skill into another's implementation.

## Validation

### A. Routing correctness
- [ ] Every user request maps to at least one child skill
- [ ] Multi-skill workflows open skills in dependency order (foundation before UI)
- [ ] No implementation code was generated without consulting a child skill's references
- [ ] Cross-references between skills are followed, not skipped
