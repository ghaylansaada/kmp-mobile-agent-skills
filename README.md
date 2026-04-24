<p align="center">
  <img src="https://img.shields.io/badge/Skills-38-blue?style=for-the-badge" alt="38 Skills" />
  <img src="https://img.shields.io/badge/Kotlin-2.3+-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin 2.3+" />
  <img src="https://img.shields.io/badge/Compose_Multiplatform-1.10+-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Compose Multiplatform" />
  <img src="https://img.shields.io/badge/Android_16-API_36-34A853?style=for-the-badge&logo=android&logoColor=white" alt="Android 16" />
  <img src="https://img.shields.io/badge/iOS_26-Swift_5.9+-F05138?style=for-the-badge&logo=swift&logoColor=white" alt="iOS 26" />
  <img src="https://img.shields.io/badge/agentskills.io-compliant-000000?style=for-the-badge" alt="agentskills.io" />
</p>

# KMP Agent Skills

A collection of 38 agent skills (1 orchestrator + 37 domain skills) for building Kotlin Multiplatform apps targeting Android and iOS. They give AI coding agents structured guidance on project setup, networking, database, UI, navigation, platform integration, security, testing, CI/CD, and release.

Built on the open [agentskills.io](https://agentskills.io) standard. Works with Claude Code, Cursor, Windsurf, and any agent that supports the format.

---

## What this is

Each skill is a self-contained knowledge pack that an AI agent reads when you ask it to do something. Instead of the agent relying only on its training data, it loads specific implementation guides, code templates, and conventions for the task at hand.

The skills cover the full lifecycle of a KMP app -- from scaffolding a new project to shipping it to both app stores. They follow 2026 patterns: Navigation 3, Material 3 Adaptive, edge-to-edge, privacy manifests, and Kotlin latest language features.

---

## Quick start

### 1. Clone into your project

```bash
git clone https://github.com/ghaylansaada/kmp-mobile-agent-skills.git .agents/skills/
```

### 2. Point your agent to the skills

**Claude Code** -- skills are auto-discovered from `.agents/skills/`.

**Other agents** -- add to your agent config:
```
Skills directory: .agents/skills/
```

### 3. Start building

Just ask your agent to build something. It will pick up the relevant skills automatically.

---

## How skills work

Each skill is a directory with progressive disclosure:

```
kmp-<name>/
  SKILL.md              # Routing document (WHEN to activate, WHAT to read)
  references/           # Implementation guides with code examples
  assets/
    templates/          # Scaffolding files with {{Placeholder}} conventions
    snippets/           # Copy-paste-ready Kotlin/Swift patterns
```

`SKILL.md` contains **zero code**. It tells the agent when to activate and which reference files to load. All code lives in `references/` and `assets/`. This keeps context usage minimal -- the agent only loads detail when it actually needs it.

---

## Tech stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.3+, Swift 5.9+ |
| UI | Compose Multiplatform (Material 3), Material 3 Adaptive |
| Networking | Ktor + Ktorfit |
| Database | Room KMP |
| DI | Koin |
| Async | Kotlin Coroutines, Flow, SKIE |
| Navigation | Navigation 3 (NavDisplay, user-owned back stack) |
| Adaptive | Window Size Classes, NavigationSuiteScaffold, ListDetailPaneScaffold |
| Image loading | Coil 3 |
| Logging | Kermit |
| Testing | kotlin.test, Turbine, MockEngine |
| Build | Gradle (Kotlin DSL), Version Catalogs |
| CI/CD | GitHub Actions, GitLab CI/CD, Fastlane |

---

## Skill catalog

### Orchestrator

| Skill | Description |
|-------|-------------|
| [kmp-mobile](kmp-mobile/) | Entry point for all KMP work. Routes any request to the right specialized skill(s). |

### Project foundation

| Skill | Description |
|-------|-------------|
| [kmp-project-setup](kmp-project-setup/) | Scaffold a KMP project -- Gradle version catalog, source sets, iOS framework embedding |
| [kmp-build-config](kmp-build-config/) | Multi-environment config with BuildKonfig -- API URLs, feature flags, per-flavor values |
| [kmp-architecture](kmp-architecture/) | Clean architecture, state management, expect/actual, source set organization |
| [kmp-dependency-injection](kmp-dependency-injection/) | Koin modules, platform wiring, typed qualifiers, ViewModel scoping |
| [kmp-kotlin](kmp-kotlin/) | Kotlin 2.3+ conventions -- naming, type safety, null safety, context parameters, guard conditions |
| [kmp-build](kmp-build/) | Build commands -- Android/iOS builds, test execution, dependency verification, troubleshooting |

### Networking and data

| Skill | Description |
|-------|-------------|
| [kmp-networking](kmp-networking/) | Ktor + Ktorfit HTTP stack, ApiResult error handling, Bearer auth, token refresh, pagination |
| [kmp-database](kmp-database/) | Room KMP -- entities, DAOs, migrations, platform builders, type converters |
| [kmp-datastore](kmp-datastore/) | Jetpack DataStore preferences -- type-safe keys, reactive reads, platform file paths |
| [kmp-paging](kmp-paging/) | Offline-first pagination -- RemoteMediator, PagingSource, Room as single source of truth |
| [kmp-connectivity](kmp-connectivity/) | Network monitoring, offline queue, sync-on-reconnect |
| [kmp-transfer](kmp-transfer/) | Chunked resumable file uploads/downloads with progress, pause/resume/cancel, retry |

### UI and presentation

| Skill | Description |
|-------|-------------|
| [kmp-compose-ui](kmp-compose-ui/) | Compose Multiplatform UI -- Material 3 components, design tokens, dark theme, shimmer loading, previews |
| [kmp-adaptive-ui](kmp-adaptive-ui/) | Responsive layouts -- window size classes, NavigationSuiteScaffold, list-detail pane, foldables, RTL |
| [kmp-accessibility](kmp-accessibility/) | Semantics API, content descriptions, VoiceOver/TalkBack, touch targets, focus management |
| [kmp-animation](kmp-animation/) | Compose animations -- AnimatedVisibility, AnimatedContent, shared elements, gestures, Lottie |
| [kmp-navigation](kmp-navigation/) | Navigation 3 -- NavDisplay, user-owned back stack, @Serializable NavKey routes, deep links |
| [kmp-resources-management](kmp-resources-management/) | Compose resources -- drawables, strings, fonts, density qualifiers, generated Res class |
| [kmp-localization](kmp-localization/) | String resources, plurals, runtime locale switching, RTL support, error message resolution |
| [kmp-image-loader](kmp-image-loader/) | Coil 3 image loading -- caching, SVG, signed URL interceptor |

### Platform integration

| Skill | Description |
|-------|-------------|
| [kmp-platform-integration](kmp-platform-integration/) | Platform layer -- entry points, actual implementations, iOS privacy manifests, predictive back |
| [kmp-notifications](kmp-notifications/) | Push notifications -- FCM/APNs, local scheduling, deep links, notification channels |
| [kmp-permissions](kmp-permissions/) | Runtime permissions -- camera, location, notifications, rationale dialogs |
| [kmp-background-job](kmp-background-job/) | Background tasks -- WorkManager, BGTaskScheduler, foreground service limits |
| [kmp-app-update](kmp-app-update/) | In-app updates -- version checking, force update gates, Play Core, iTunes Lookup |

### Async and data flow

| Skill | Description |
|-------|-------------|
| [kmp-kotlin-coroutines](kmp-kotlin-coroutines/) | Coroutine patterns, Flow operators, structured concurrency, Mutex, Channel, SKIE bridge |
| [kmp-datetime](kmp-datetime/) | kotlinx-datetime -- timezone handling, platform formatters, serializers |
| [kmp-logging](kmp-logging/) | Kermit logging -- tagged loggers, severity filtering, release stripping |

### Security

| Skill | Description |
|-------|-------------|
| [kmp-security](kmp-security/) | Keystore storage, certificate pinning, encrypted DB, root/jailbreak detection |
| [kmp-session-management](kmp-session-management/) | Session lifecycle -- sealed SessionState, secure token storage, token refresh, logout |
| [kmp-biometrics](kmp-biometrics/) | Biometric auth -- BiometricPrompt/LAContext, credential storage, feature gating |
| [kmp-crypto](kmp-crypto/) | Cryptographic operations -- SHA-256, HMAC, AES-GCM, PBKDF2, secure random |

### Testing

| Skill | Description |
|-------|-------------|
| [kmp-testing](kmp-testing/) | Common tests (kotlin.test, Turbine, fakes), Android (Compose, Robolectric), iOS (XCTest), integration (MockEngine, Koin) |

### Analytics and observability

| Skill | Description |
|-------|-------------|
| [kmp-analytics-crashlytics](kmp-analytics-crashlytics/) | Firebase Analytics + Crashlytics -- event tracking, crash reporting, consent management |
| [kmp-performance](kmp-performance/) | Performance -- startup optimization, Baseline Profiles, memory leaks, Compose stability |

### Release and CI/CD

| Skill | Description |
|-------|-------------|
| [kmp-ci-cd](kmp-ci-cd/) | GitHub Actions + GitLab CI/CD -- dual-platform builds, caching, Fastlane, version bumping |
| [kmp-release](kmp-release/) | Android and iOS release -- signing, ProGuard/R8, store deployment, TestFlight, dSYM upload |

---

## Conventions

The skills share a consistent set of conventions across all code examples and templates:

| Area | Convention |
|------|-----------|
| State types | `sealed interface` over `sealed class` |
| IDs and tokens | `value class` wrappers |
| Reactive streams | `Flow`/`StateFlow`/`SharedFlow` (never `LiveData` in shared code) |
| One-shot events | `Channel` (not `SharedFlow(replay=0)`) |
| Lifecycle | `stateIn(SharingStarted.WhileSubscribed(5_000))` |
| Error handling | `CancellationException` always rethrown |
| Testing | `runTest`, `kotlin.test`, Turbine, fakes over mocks |
| Dimensions | `AppTheme.spacing.*` / `AppTheme.sizing.*` / `AppTheme.corners.*` (no hardcoded dp) |
| Strings | `stringResource(Res.string.*)` (no hardcoded text) |
| Colors | `MaterialTheme.colorScheme.*` (no hardcoded colors) |
| Animation timing | `AppTheme.motion.*` (no hardcoded durations) |
| Navigation | Navigation 3 (NavDisplay, NavKey) |
| Layouts | Window size classes (never device-type detection) |
| Platform | Edge-to-edge + predictive back (Android 16), privacy manifests (iOS) |

---

## SKILL.md structure

Every `SKILL.md` follows the same layout:

| Section | Purpose |
|---------|---------|
| **YAML frontmatter** | `name`, `description`, `compatibility`, `metadata` |
| **Title** | `# KMP <Skill Name>` |
| **Scope** | What this skill covers and does not cover |
| **When to use** | Activation triggers for the agent |
| **Depends on** | Upstream skill references |
| **Workflow** | Numbered steps with links to reference files |
| **Gotchas** | Common pitfalls with real consequences |
| **Assets** | Table linking to references, templates, and snippets |
| **Validation** | Checklists for correctness, tokens, security, performance |

`SKILL.md` contains **zero code blocks**. All code lives in `references/` and `assets/`.

---

## Creating new skills

A skill creation prompt is included at [`SKILL-CREATION-PROMPT.md`](SKILL-CREATION-PROMPT.md). It captures the conventions, token mappings, and structural rules so new skills stay consistent with the existing set.

---

## Contributing

Contributions are welcome. To keep things consistent:

1. Use [`SKILL-CREATION-PROMPT.md`](SKILL-CREATION-PROMPT.md) as your starting point
2. No hardcoded values (dp, strings, colors, durations, versions)
3. Each skill owns only its topic -- cross-reference others when needed
4. All internal links must resolve
5. Keep `SKILL.md` under 500 lines with zero code blocks

## License

[MIT](LICENSE)
