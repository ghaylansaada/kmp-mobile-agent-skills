# Skill Creation Prompt

Copy the prompt below and replace `{{SKILL_TOPIC}}` with your skill topic (e.g., "in-app purchases", "WebSocket real-time communication", "QR/barcode scanning"). Paste the result to an AI coding agent.

---

## The Prompt

```
Create a new enterprise-grade agentskills.io-compliant skill for a Kotlin Multiplatform (KMP) project targeting Android and iOS.

## Skill Topic

{{SKILL_TOPIC}}

## Output Location

Create the skill at: agent-skills/kmp-{{skill-name}}/

## Existing Skill Ecosystem (38 skills)

This skill will join 38 existing skills in a KMP skill set. It MUST integrate cleanly with them. Each skill owns ONLY its domain -- never duplicate content that belongs to another skill. Cross-reference by name instead.

### Orchestrator

| Skill | What it owns |
|-------|-------------|
| kmp-mobile | Entry point / router. Routes any KMP request to the right specialized skill(s). Update its routing table when adding a new skill. |

### Project Foundation

| Skill | What it owns |
|-------|-------------|
| kmp-project-setup | Gradle scaffold, version catalog, source sets, iOS framework embedding |
| kmp-build-config | BuildKonfig, multi-environment config, API URLs, feature flags, flavors |
| kmp-architecture | Clean architecture (ViewModel, Repository, UseCase, UiState, UDF), state management (StateFlow, sealed state, one-shot events, flow combining), expect/actual, source sets, platform boundaries |
| kmp-dependency-injection | Koin modules, platform wiring, typed qualifiers, ViewModel scoping |
| kmp-kotlin | Kotlin 2.3+ conventions (naming, formatting, KDoc, type safety, null safety, guard conditions, context parameters, modern syntax, error handling, anti-patterns). Does NOT cover coroutines. |
| kmp-build | ALL Gradle/build commands (Android/iOS builds, test execution, dependency verification, troubleshooting) |

### Networking and Data

| Skill | What it owns |
|-------|-------------|
| kmp-networking | Ktor + Ktorfit, ApiResult sealed error handling, Bearer auth, token refresh, pagination |
| kmp-database | Room KMP (entities, DAOs, migrations, type converters, platform builders) |
| kmp-datastore | Jetpack DataStore preferences (typed keys, reactive reads, platform file paths) |
| kmp-paging | Offline-first pagination (RemoteMediator, PagingSource, Room as SSOT) |
| kmp-connectivity | Network monitoring, offline queue, sync-on-reconnect |
| kmp-transfer | Chunked resumable uploads/downloads with progress, pause/resume/cancel, retry |

### UI and Presentation

| Skill | What it owns |
|-------|-------------|
| kmp-compose-ui | Compose Multiplatform UI (Material 3, design tokens, dark theme, shimmer/skeleton loading, screen patterns, remember APIs, Compose stability, previews) |
| kmp-adaptive-ui | Window size classes, NavigationSuiteScaffold, ListDetailPaneScaffold, foldables, edge-to-edge, RTL |
| kmp-accessibility | Semantics API, content descriptions, VoiceOver/TalkBack, touch targets, dynamic type, focus management, reduced motion |
| kmp-animation | Compose animations (AnimatedVisibility, AnimatedContent, shared elements, gestures, Lottie, motion tokens) |
| kmp-navigation | Navigation 3 (NavDisplay, NavKey, user-owned back stack, auth gates, deep links, Nav2 migration) |
| kmp-resources-management | Compose resources (drawables, strings, fonts, density qualifiers, generated Res class) |
| kmp-localization | String resources, plurals, runtime locale switching, RTL, error message resolution |
| kmp-image-loader | Coil 3 (caching, SVG, signed URL interceptor, AppAsyncImage composable) |

### Platform Integration

| Skill | What it owns |
|-------|-------------|
| kmp-platform-integration | Platform entry points (MainActivity, MainViewController), iOS privacy manifests, Android predictive back, edge-to-edge |
| kmp-notifications | Push notifications (FCM/APNs, local scheduling, deep links, channels) |
| kmp-permissions | Runtime permissions (camera, location, notifications), Android 16 changes, iOS ATT, privacy manifests |
| kmp-background-job | Background tasks (WorkManager, BGTaskScheduler, foreground service limits, BGContinuedProcessingTask) |
| kmp-app-update | In-app updates (version checking, force update gates, Play Core, iTunes Lookup) |

### Async and Data Flow

| Skill | What it owns |
|-------|-------------|
| kmp-kotlin-coroutines | ALL coroutine and Flow patterns: CoroutineContext, Dispatchers, Job/SupervisorJob, CoroutineScope, withContext, coroutineScope vs supervisorScope, CoroutineExceptionHandler, cooperative cancellation, async/Deferred, Mutex, Semaphore, Channel, StateFlow/SharedFlow, retry, SKIE iOS bridge |
| kmp-datetime | kotlinx-datetime (timezone handling, platform formatters, serializers) |
| kmp-logging | Kermit (tagged loggers, severity filtering, release stripping, Ktor integration) |

### Security

| Skill | What it owns |
|-------|-------------|
| kmp-security | Keystore storage, certificate pinning, encrypted DB, root/jailbreak detection |
| kmp-session-management | Session lifecycle (sealed SessionState, secure token storage, Mutex token refresh, logout) |
| kmp-biometrics | Biometric auth (BiometricPrompt/LAContext, credential storage, feature gating) |
| kmp-crypto | Cryptographic operations (SHA-256, HMAC, AES-GCM, PBKDF2, secure random) |

### Testing

| Skill | What it owns |
|-------|-------------|
| kmp-testing | ALL testing (common kotlin.test, Turbine, fakes, Android Compose/Robolectric/Roborazzi, iOS XCTest/Swift, integration MockEngine/Koin) |

### Analytics and Observability

| Skill | What it owns |
|-------|-------------|
| kmp-analytics-crashlytics | Firebase Analytics + Crashlytics (event tracking, crash reporting, consent) |
| kmp-performance | Startup optimization, Baseline Profiles, memory leaks, Compose stability, StrictMode, profiling |

### Release and CI/CD

| Skill | What it owns |
|-------|-------------|
| kmp-ci-cd | GitHub Actions + GitLab CI/CD (dual-platform builds, caching, Fastlane, version bumping) |
| kmp-release | Android and iOS release (signing, ProGuard/R8, store deployment, TestFlight, dSYM) |

## Tech Stack

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
| Testing | kotlin.test, Turbine, MockEngine, Robolectric, Roborazzi |
| Build | Gradle (Kotlin DSL), Version Catalogs |
| CI/CD | GitHub Actions, GitLab CI/CD, Fastlane |

## Directory Structure

```
kmp-{{skill-name}}/
  SKILL.md              # Routing document -- ZERO code blocks
  references/           # Implementation guides WITH code examples
  assets/
    templates/          # .kt.template files with {{Placeholder}} conventions
    snippets/           # Standalone .kt/.swift/.pro files
```

---

# SKILL.md FORMAT (STRICT)

## Frontmatter (YAML)

```yaml
---
name: kmp-{{skill-name}}
description: >
  [1-1024 chars. Imperative phrasing. Describe WHAT it does AND WHEN to activate.
   Include specific trigger keywords. Be pushy -- list all contexts where this skill applies.
   End with "Activate when [list of triggers]."]
compatibility: >
  KMP with Compose Multiplatform. [Add specific library/platform requirements.]
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---
```

ALL FIVE frontmatter fields are REQUIRED: name, description, compatibility, metadata.domain, metadata.targets.

The description MUST end with "Activate when..." followed by a comma-separated list of trigger phrases. This is how agents discover the skill.

## Body Sections (EXACT order, EXACT heading casing)

### Section 1: Title
```
# KMP {{Skill Name}}
```
The H1 title MUST be the first line after the closing `---`.

### Section 2: Scope (recommended)
One paragraph: what this skill covers AND what it explicitly does NOT cover (with cross-references to skills that own those excluded topics). This prevents domain bleeding.

### Section 3: When to use
```
## When to use
```
NOTE: lowercase "use", NOT "Use". Bulleted list of 8-15 activation triggers. Focus on user intent:
```
- Adding [feature/behavior] to the app
- Implementing [specific pattern]
- Configuring [specific tool/library]
```

### Section 4: Depends on
```
## Depends on
```
NOTE: lowercase "on", NOT "On". Never use "Prerequisites". Format:
```
- **kmp-skill-name** -- one-line reason this dependency is needed
```
Only list skills whose content this skill directly builds on. Never reference non-existent skills.

### Section 5: Workflow
```
## Workflow
```
Numbered steps (NEVER checkboxes). Each step links to a reference file:
```
1. Do X --> [file.md](references/file.md)
2. Do Y --> [file.md](references/file.md)
   _Skip if condition._
```
Steps must be in implementation order. 5-8 steps.

### Section 6: Gotchas
```
## Gotchas
```
Numbered paragraphs, 8-20 items. Each starts with a **bold first sentence** stating the pitfall, followed by explanation of the runtime consequence and how to fix it:
```
1. **Bold sentence stating the pitfall.** Explanation of what goes wrong at runtime and how to avoid it.
```

Gotcha quality rules:
- MUST state a concrete consequence ("causes crash", "silently drops events", "leaks memory", "breaks on iOS")
- MUST be actionable (reader knows what to fix)
- NEVER vague ("be careful with X" without saying what happens)
- Include platform-specific traps (Android vs iOS behavioral differences)
- When mentioning a topic owned by another skill, add a cross-reference: "See **kmp-xyz** skill for details."
- CancellationException gotchas belong in **kmp-kotlin-coroutines** -- just cross-reference

### Section 7: Assets
```
## Assets
```
Table with columns `| Path | Load when... |`. Link to EVERY reference and asset file:
```
| Path | Load when... |
|------|-------------|
| [references/setup.md](references/setup.md) | Setting up dependencies |
| [assets/templates/feature.kt.template](assets/templates/feature.kt.template) | Scaffolding a new feature |
```

### Section 8: Validation
```
## Validation
```
Grouped `- [ ]` checklists. ALWAYS include sections A and B:

```
### A. Core correctness
- [ ] [Domain-specific functional checks]

### B. Design token and resource compliance
- [ ] Zero hardcoded dp values -- all spacing uses `AppTheme.spacing.*`, sizing uses `AppTheme.sizing.*`, corners use `AppTheme.corners.*`
- [ ] Zero hardcoded user-facing strings -- all text uses `stringResource(Res.string.*)`
- [ ] Zero hardcoded content descriptions -- all accessibility labels use `stringResource(Res.string.cd_*)`
- [ ] Zero hardcoded colors -- all colors use `MaterialTheme.colorScheme.*`
- [ ] Zero hardcoded animation durations -- all use `AppTheme.motion.duration*`
```

Section B is REQUIRED for UI-producing skills. For pure infrastructure skills (build, CI/CD, crypto) with zero UI code, section B may be omitted.

Add sections C-F as needed (security, performance, platform, integration).

### Validation anti-patterns (NEVER include)
- "SKILL.md exists" / "YAML frontmatter present" / "directory structure correct"
- "references/ directory exists" / "all links resolve"
These are format concerns, not domain concerns.

---

# ABSOLUTE RULES (ZERO EXCEPTIONS)

## R1. SKILL.md contains ZERO code blocks
All code lives in references/ and assets/. SKILL.md is a routing document only. No triple-backtick blocks, no inline code examples, no code snippets. Only prose, bullet lists, numbered steps, tables, and links.

## R2. Zero hardcoded dp values
Every `.dp` value in code MUST use design tokens:

| Category | Value | Token |
|----------|-------|-------|
| Spacing | 2.dp | AppTheme.spacing.xxs |
| Spacing | 4.dp | AppTheme.spacing.xs |
| Spacing | 8.dp | AppTheme.spacing.sm |
| Spacing | 12.dp | AppTheme.spacing.md |
| Spacing | 16.dp | AppTheme.spacing.lg |
| Spacing | 24.dp | AppTheme.spacing.xl |
| Spacing | 32.dp | AppTheme.spacing.xxl |
| Spacing | 48.dp | AppTheme.spacing.xxxl |
| Icons | 16.dp | AppTheme.sizing.iconSm |
| Icons | 24.dp | AppTheme.sizing.iconMd |
| Icons | 48.dp | AppTheme.sizing.iconLg |
| Icons | 64.dp | AppTheme.sizing.iconXl |
| Touch | 48.dp | AppTheme.sizing.minTouchTarget |
| Stroke | 1.dp | AppTheme.sizing.strokeThin |
| Stroke | 2.dp | AppTheme.sizing.strokeMedium |
| Corners | 4.dp | AppTheme.corners.sm |
| Corners | 8.dp | AppTheme.corners.md |
| Corners | 12.dp | AppTheme.corners.lg |
| Corners | 16.dp | AppTheme.corners.xl |

Exception: `0.dp` (baseline zero) is allowed.

## R3. Zero hardcoded strings
Every user-facing string MUST use `stringResource(Res.string.xxx)`:
- Button labels, titles, descriptions, placeholders, hints
- Error messages, status messages, toast text, snackbar text
- Content descriptions: `stringResource(Res.string.cd_xxx)`
- Dialog titles, messages, button labels
- Tab labels, navigation labels, section headers

Exceptions: log messages, exception messages for developers, internal identifiers, constant keys.

## R4. Zero hardcoded colors
Every color MUST use `MaterialTheme.colorScheme.*`. Never use `Color.Red`, `Color.White`, `Color.Black`, `Color(0xFF...)`, etc. in composable code.

Exception: color scheme definitions in `lightColorScheme()` and `darkColorScheme()` declarations.

## R5. Zero hardcoded animation durations
Every animation timing MUST use motion tokens:

| Value | Token |
|-------|-------|
| 150ms | AppTheme.motion.durationShort |
| 300ms | AppTheme.motion.durationMedium |
| 500ms | AppTheme.motion.durationLong |
| 800ms | AppTheme.motion.durationExtraLong |
| FastOutSlowInEasing | AppTheme.motion.easingStandard |
| LinearOutSlowInEasing | AppTheme.motion.easingDecelerate |
| FastOutLinearInEasing | AppTheme.motion.easingAccelerate |

## R6. Zero hardcoded dependency versions
Use `"..."` placeholder for all version numbers in Gradle snippets. Add this note at the top of any reference file that includes dependencies:

> **Note:** Version numbers are intentionally omitted. Always use the latest stable release from the official documentation.

## R7. Domain isolation (CRITICAL)
Each skill talks ONLY about its own domain. When another skill's domain is relevant, cross-reference it:
- NO Gradle/build commands --> "see **kmp-build** skill"
- NO testing content --> "see **kmp-testing** skill"
- NO Kotlin language conventions --> "see **kmp-kotlin** skill"
- NO coroutine/Flow/dispatcher patterns --> "see **kmp-kotlin-coroutines** skill"
- NO architecture patterns (ViewModel, Repository, UDF) --> "see **kmp-architecture** skill"
- NO DI wiring beyond domain-specific bindings --> "see **kmp-dependency-injection** skill"
- NO accessibility patterns --> "see **kmp-accessibility** skill"
- NO navigation patterns --> "see **kmp-navigation** skill"
- NO adaptive layout patterns --> "see **kmp-adaptive-ui** skill"
- NO animation patterns --> "see **kmp-animation** skill"

Gotchas that mention another skill's domain MUST include: "See **kmp-xyz** skill for [topic]."

## R8. Workflow format
Numbered steps only. Never `- [ ]` checkboxes in the Workflow section. Checkboxes are ONLY for the Validation section.

## R9. No meta-validation
The Validation section checks FUNCTIONAL correctness, not structural correctness. Never include checks about SKILL.md format, directory structure, or link integrity.

## R10. Latest patterns (2026)
- Kotlin 2.3+ (guard conditions, context parameters, explicit backing fields, name-based destructuring)
- Compose Multiplatform 1.10+
- Navigation 3 (NavDisplay, NavKey, user-owned back stack) -- NEVER Navigation 2 (NavHost, NavGraph, NavController)
- Android 16 (API 36): edge-to-edge mandatory, predictive back mandatory, orientation locks ignored on large screens
- iOS 26: privacy manifests (PrivacyInfo.xcprivacy) mandatory, BGContinuedProcessingTask
- Window size classes for adaptive layouts (NEVER device-type detection)
- `sealed interface` over `sealed class` for state/result/error types
- `value class` for type-safe IDs and tokens: `@JvmInline value class UserId(val value: String)`
- Flow/StateFlow/SharedFlow (NEVER LiveData in shared code)
- Channel for one-shot events (NEVER SharedFlow(replay=0) for events)
- CancellationException ALWAYS rethrown (never caught and swallowed)
- `stateIn(SharingStarted.WhileSubscribed(5_000))` for upstream lifecycle management
- `update {}` for atomic StateFlow mutations (never direct `.value` assignment)
- `collectAsStateWithLifecycle()` on Android (not `collectAsState()`)
- Fakes over mocks in tests
- Explicit imports (no wildcard `*` imports)
- 4-space indent, trailing commas, named boolean arguments
- `ImmutableList<T>` from kotlinx.collections.immutable for stable Compose parameters
- `@Immutable` / `@Stable` annotations for custom types passed to composables
- `graphicsLayer {}` for animations (not `alpha`, `scale`, `rotation` modifiers directly)

---

# REFERENCE FILE GUIDELINES

Each file in references/ should:
- Start with a `# Title` heading
- Include version note if dependencies are mentioned: `> **Note:** Version numbers are intentionally omitted. Always use the latest stable release from the official documentation.`
- Contain focused, well-structured code examples with clear `## H2` section headings
- Use ALL design tokens (`AppTheme.spacing.*`, `AppTheme.sizing.*`, `AppTheme.corners.*`, `AppTheme.motion.*`) -- zero hardcoded dp/durations
- Use `stringResource(Res.string.*)` for every user-facing string and `stringResource(Res.string.cd_*)` for content descriptions
- Use `MaterialTheme.colorScheme.*` for every color -- zero `Color.Red`, `Color.White`, etc.
- Include file placement comments: **File:** `commonMain/kotlin/{your.package}/...`
- Keep individual files under 300 lines (split if larger)
- Cross-reference other skills when mentioning their domain

---

# ASSET FILE GUIDELINES

## Templates (.kt.template)
- Use `{{Placeholder}}` conventions:
  - `{{FeatureName}}` -- PascalCase type names
  - `{{featureName}}` -- camelCase variable/function names
  - `{{feature_name}}` -- snake_case (rare)
  - `{{FEATURE_NAME}}` -- SCREAMING_CASE constants
  - `{{your_package}}` -- package name
- Include all necessary imports (explicit, no wildcards)
- Follow ALL rules R1-R10 in template code

## Snippets (.kt, .swift)
- Self-contained, copy-paste ready
- KDoc header explaining the pattern
- Include all necessary imports (explicit, no wildcards)
- Follow ALL rules R1-R10 in snippet code

---

# KOTLIN CODE STANDARDS

In ALL code across references, templates, and snippets:

## Types
- `sealed interface` over `sealed class` (unless shared mutable state is needed in base)
- `value class` for type-safe wrappers: `@JvmInline value class UserId(val value: String)`
- `when` on sealed/enum types: exhaustive, NO `else` branch (defeats exhaustiveness checking)
- Explicit return types on all public functions
- `typealias` only for complex generic signatures, never as substitute for value class

## Coroutines (reference only -- details belong in kmp-kotlin-coroutines)
- `CancellationException` ALWAYS rethrown in any `catch (e: Exception)` block
- `withContext(Dispatchers.IO)` for I/O, `withContext(Dispatchers.Default)` for CPU
- `stateIn(SharingStarted.WhileSubscribed(5_000))` for derived flows
- `update {}` for atomic state mutations, never `_state.value = _state.value.copy(...)`
- `Channel(Channel.BUFFERED)` + `receiveAsFlow()` for one-shot events
- `supervisorScope` when child failures must not cancel siblings
- `ensureActive()` before expensive operations in loops
- No `GlobalScope` -- ever

## Compose (reference only -- details belong in kmp-compose-ui)
- `collectAsStateWithLifecycle()` on Android (not `collectAsState()`)
- `ImmutableList<T>` for list parameters to composables (avoids recomposition)
- `@Immutable` / `@Stable` on custom types passed to composables
- `remember` for expensive computations, `rememberSaveable` for surviving config changes
- `derivedStateOf` for computed values that should skip recomposition when unchanged
- `graphicsLayer {}` for animation transforms (GPU-accelerated, no recomposition)
- Modifier chain order: layout → drawing → input → semantics

## Formatting
- 4-space indentation, no tabs
- 120-character line limit
- Trailing commas on all multi-line parameter lists
- Named arguments for boolean parameters: `enabled = true`, not just `true`
- Explicit imports only -- no wildcard `*` imports
- No double blank lines
- `suspend` functions throw exceptions; Flow-based APIs emit errors via sealed types

---

# QUALITY BAR

This skill will be published as a public enterprise-grade skill used by Google, JetBrains, and large organizations. Every file must be:

1. **Complete** -- an AI agent can implement the feature without external documentation
2. **Correct** -- code compiles and runs on both Android and iOS
3. **Modern** -- uses 2026 patterns (Kotlin 2.3+, Compose Multiplatform 1.10+, Android 16, iOS 26)
4. **Accessible** -- all UI includes semantics annotations, touch targets >= 48dp via `AppTheme.sizing.minTouchTarget`, content descriptions via `stringResource(Res.string.cd_*)`
5. **Adaptive** -- all UI works on phones, tablets, and foldables using window size classes (never device-type detection)
6. **Dark-theme-ready** -- all colors from `MaterialTheme.colorScheme.*` (zero hardcoded colors)
7. **Localization-ready** -- all strings from `stringResource(Res.string.*)` (zero hardcoded strings)
8. **Performance-conscious** -- stability annotations, `remember`, `derivedStateOf`, `graphicsLayer`, `ImmutableList`
9. **Secure** -- no secrets in code, tokens stored via keystore, error messages don't expose internals
10. **Domain-isolated** -- zero content that belongs to another skill; cross-references instead

---

# EXECUTION STEPS

1. Create the directory structure (`references/`, `assets/templates/`, `assets/snippets/`)
2. Write SKILL.md following the exact section order and heading casing above
3. Write each reference file (comprehensive, with production-ready code examples)
4. Write asset files (templates with `{{Placeholder}}` and/or copy-paste snippets)
5. Self-review checklist:
   - [ ] SKILL.md has ZERO code blocks (zero triple-backtick occurrences)
   - [ ] SKILL.md has all 8 sections in the correct order with correct heading casing
   - [ ] Frontmatter has all 5 required fields (name, description, compatibility, metadata.domain, metadata.targets)
   - [ ] Description ends with "Activate when [triggers]"
   - [ ] When-to-use has 8-15 items
   - [ ] Gotchas are 8-20 numbered paragraphs with bold first sentences and consequences
   - [ ] Assets table links to EVERY reference and asset file
   - [ ] Validation includes section B (design token compliance) for UI-producing skills
   - [ ] Zero hardcoded dp/strings/colors/durations/versions in ALL code files
   - [ ] Every cross-skill mention includes "See **kmp-xyz** skill"
   - [ ] All internal markdown links resolve to existing files
   - [ ] No content that belongs to another skill's domain
6. Update kmp-mobile/SKILL.md routing table with the new skill entry
```

---

## Quick Checklist After Generation

Run these checks on the generated skill:

```bash
SKILL=kmp-{{skill-name}}

# 1. Zero code blocks in SKILL.md
echo "Code blocks: $(grep -c '^\`\`\`' $SKILL/SKILL.md)"
# Expected: 0

# 2. All required sections present
for section in "^# KMP" "## When to use" "## Depends on" "## Workflow" "## Gotchas" "## Assets" "## Validation"; do
  grep -q "$section" $SKILL/SKILL.md && echo "OK: $section" || echo "MISSING: $section"
done

# 3. Frontmatter completeness
for field in "name:" "description:" "compatibility:" "domain:" "targets:"; do
  grep -q "$field" $SKILL/SKILL.md && echo "OK: $field" || echo "MISSING: $field"
done

# 4. Zero hardcoded dp
grep -rn '[0-9]\+\.dp' $SKILL/ --include="*.kt" --include="*.template" | grep -v 'AppTheme\|0\.dp\|// WRONG'
# Expected: empty

# 5. Zero hardcoded strings
grep -rn 'Text("[A-Z]' $SKILL/ --include="*.kt" --include="*.template" | grep -v 'stringResource\|Res\.\|// WRONG\|Preview'
# Expected: empty

# 6. Zero hardcoded contentDescription
grep -rn 'contentDescription = "' $SKILL/ --include="*.kt" --include="*.template" | grep -v 'stringResource\|null\|""'
# Expected: empty

# 7. Zero hardcoded colors
grep -rn 'Color\.\(Red\|Blue\|White\|Black\|Green\|Gray\)' $SKILL/ --include="*.kt" --include="*.template" | grep -v '// WRONG'
# Expected: empty

# 8. Zero hardcoded durations
grep -rn 'durationMillis = [0-9]' $SKILL/ --include="*.kt" --include="*.template" | grep -v 'AppTheme\.motion\|// WRONG'
# Expected: empty

# 9. Zero hardcoded versions
grep -rn ':[0-9]\+\.[0-9]\+' $SKILL/ --include="*.md" --include="*.kt" --include="*.template" | grep -v 'WRONG\|version\.ref\|"\.\.\."'
# Expected: empty

# 10. All internal links resolve
for md in $(find $SKILL -name "*.md"); do
  grep -oP '\]\((?!http)([^)]+)\)' "$md" | sed 's/\](//;s/)//' | while read link; do
    base=$(echo "$link" | cut -d'#' -f1)
    [ -n "$base" ] && [ ! -f "$(dirname $md)/$base" ] && echo "BROKEN: $md -> $link"
  done
done
# Expected: empty

# 11. No wrong heading casing
grep -n '## When to Use\|## Depends On\|## Prerequisites' $SKILL/SKILL.md
# Expected: empty

# 12. Gotchas are numbered with bold first sentence
grep -c '^[0-9]\+\. \*\*' $SKILL/SKILL.md
# Expected: matches gotcha count (8-20)
```
