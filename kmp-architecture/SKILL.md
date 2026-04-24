---
name: kmp-architecture
description: >
  Core KMP patterns covering clean architecture (ViewModel, Repository, UseCase, UiState, UDF),
  state management (StateFlow, sealed UiState, one-shot events via Channel, flow combining,
  lifecycle collection), expect/actual mechanism, source set organization, and platform boundaries.
  Activate when adding a new feature, creating a ViewModel, structuring data flow, adding a
  repository, wiring up a screen, managing UI state, using expect/actual, or organizing source sets.
compatibility: >
  KMP with Compose Multiplatform. Requires AndroidX Lifecycle (JetBrains KMP build) and Koin.
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---

# KMP Architecture

## Scope

Covers architecture patterns (ViewModel, Repository, UseCase, UiState, Screen composable, unidirectional data flow), state management (sealed UiState, data class UiState, StateFlow, one-shot events via Channel, flow combining with stateIn, lifecycle collection, configuration change survival, SKIE bridge to iOS), expect/actual mechanism (expect class/fun/object, PlatformContext, getPlatform, AppDatabaseConstructor, factory pattern, actual typealias), source set organization (commonMain, androidMain, iosMain, what goes where), platform boundaries (no android.*/java.* in commonMain), and general KMP best practices for a single-module project. Does not cover navigation, UI component design, paging internals, or Kotlin language style.

## When to use

- Creating a new feature end-to-end (Repository, UseCase, ViewModel, Screen, DI module)
- Adding or modifying a ViewModel with observable UI state
- Adding loading/error/success states to a screen
- Defining a Repository interface and implementation with Flow return types
- Adding UseCases to encapsulate business logic
- Modeling a finite state machine (authentication, onboarding, wizard)
- Combining multiple Flows into a single UiState via combine + stateIn
- Sending one-shot events (navigation, snackbar) via Channel
- Collecting state in a Compose screen
- Mapping pagination LoadState to UI-friendly PagingState
- Bridging StateFlow to iOS via SKIE
- Declaring a new API in common code that requires different implementations per platform
- Adding platform-specific behavior (file I/O, networking engines, database drivers)
- Deciding between expect/actual and interface-based abstraction
- Troubleshooting "Expected declaration has no actual" or visibility mismatch errors
- Wrapping platform SDK types (Android Context, iOS UIDevice) for shared code
- Adding KSP-generated actuals (Room @ConstructedBy pattern)
- Understanding source set structure and what belongs in each source set
- Refactoring existing features to follow ViewModel/Repository patterns

## Depends on

- **kmp-project-setup** -- project scaffold, Gradle config, source set layout
- **kmp-dependency-injection** -- Koin module organization, platform wiring, ViewModel scoping

## Workflow

1. Add lifecycle and Koin dependencies --> [references/setup.md](references/setup.md)
   _Skip if already in the project._
2. Understand source set organization and platform boundaries --> [references/source-sets.md](references/source-sets.md)
3. Implement expect/actual patterns --> [references/expect-actual.md](references/expect-actual.md)
   _Read when adding platform-specific behavior or wrapping platform SDK types._
4. Define data layer (Repository interface, implementation, UseCase) --> [references/data-layer.md](references/data-layer.md)
5. Define presentation layer (UiState, UiEvent, ViewModel, Screen) --> [references/presentation-layer.md](references/presentation-layer.md)
6. Manage state (StateFlow, sealed state, one-shot events, flow combining) --> [references/state-management.md](references/state-management.md)
7. Wire DI modules --> [references/di-wiring.md](references/di-wiring.md)
8. Scaffold a new feature --> use template at [assets/templates/new-feature.kt.template](assets/templates/new-feature.kt.template)

## Gotchas

1. **Single-module means no compiler-enforced layer boundaries.** Nothing prevents a Screen from importing a DAO directly. Enforce layering through naming conventions, package structure, and code review.
2. **Repository must return Flow, never LiveData.** LiveData is Android-only and will not compile on iOS. The iOS linker will fail with unresolved symbol errors.
3. **MutableStateFlow must stay private.** Exposing it publicly breaks unidirectional data flow and allows any composable to mutate state, causing race conditions and impossible-to-trace bugs. Always expose read-only `StateFlow` via `asStateFlow()`.
4. **All async work must use viewModelScope.** Never use `GlobalScope` or create standalone `CoroutineScope` in a ViewModel -- it leaks coroutines past the ViewModel lifecycle, causing crashes when coroutines write to a destroyed UI.
5. **cachedIn(viewModelScope) is required for PagingData flows.** Without it, configuration changes restart the entire paging pipeline from page one, causing visible data flicker and redundant network calls. See **kmp-paging** skill for full pagination setup.
6. **ViewModel lifecycle differs on iOS.** There is no process death scenario, so `onCleared` timing differs. Do not rely on it for critical cleanup that must happen symmetrically on both platforms.
7. **Clean architecture must not leak platform types.** Never expose Android `Context`, iOS `NSObject`, or any platform type through Repository or UseCase interfaces. This causes compilation failures on the opposite platform target.
8. **emitAll on a non-completing Flow blocks forever.** If a DAO returns a long-lived `Flow` (e.g., Room observe query), calling `emitAll(dao.observe())` then `callApi()` sequentially in a `flow {}` builder means `callApi()` is never reached. Launch the network call in a separate coroutine or use `onStart {}`.
9. **sealed interface UiState requires exhaustive when branches.** If you add a new subtype to a sealed interface UiState, every `when` block that matches on it becomes a compile error until updated. This is intentional -- it prevents forgotten UI states at compile time rather than runtime.
10. **Use `WhileSubscribed(5_000)` for `stateIn` in ViewModels.** Using `Eagerly` or `Lazily` keeps upstream flows alive after the last subscriber disconnects, leaking resources. For detailed Flow operator behavior, see **kmp-kotlin-coroutines** skill.
11. **collectAsState() does not respect Android lifecycle.** It continues collecting when the app is in the background. Use `collectAsStateWithLifecycle()` from lifecycle-runtime-compose on Android to automatically stop collection when the lifecycle drops below STARTED.
12. **Use Channel for one-shot events, not SharedFlow.** `SharedFlow(replay=0)` drops events when no collector is active. Use `Channel(Channel.BUFFERED)` with `receiveAsFlow()`. For detailed StateFlow/SharedFlow/Channel comparison, see **kmp-kotlin-coroutines** skill.
13. **update {} is atomic, direct .value assignment is not.** Always prefer `_state.update { it.copy(...) }` over `_state.value = _state.value.copy(...)`. The latter creates a read-modify-write race condition when multiple coroutines update state concurrently.
14. **derivedStateOf vs remember for computed values.** Use `derivedStateOf` only for values derived from Compose state that should skip recomposition when the derived result is unchanged. Using `remember { expensive() }` recomputes on every recomposition.
15. **K2 compiler tightens expect/actual matching.** `actual typealias` cannot add supertypes the `expect` does not have. Code that compiled under K1 may fail under K2.
16. **`-Xexpect-actual-classes` is on a deprecation path.** The flag will become unnecessary when expect/actual classes stabilize. Monitor Kotlin release notes to remove it before it triggers errors.
17. **Room auto-generates `actual object` for `@ConstructedBy`.** Adding a hand-written actual alongside Room's KSP-generated one causes a duplicate symbol error.
18. **`expect class` constructors are not callable from commonMain.** Common code cannot call `Foo(42)` because the constructor may differ per platform. Use a factory interface registered via DI instead.
19. **Visibility modifiers must match exactly between expect and actual.** An `expect fun` declared as `public` paired with an `actual internal fun` causes a compilation error.
20. **`Dispatchers.IO` is unavailable on iOS without an explicit import.** Kotlin/Native does not auto-resolve `Dispatchers.IO`. Add `import kotlinx.coroutines.IO` in iOS actuals or use `Dispatchers.Default`. For complete dispatcher reference, see **kmp-kotlin-coroutines** skill.
21. **KSP must be configured for every target.** If Room KSP is only configured for Android, the iOS build fails with "Expected declaration has no actual" and no mention of Room.
22. **Intermediate source sets can hide missing actuals.** If you place an actual in `iosArm64Main` only, `iosSimulatorArm64Main` fails. Provide actuals in the shared `iosMain` source set.
23. **Lambda parameter shadowing in NSData helpers.** In `NSData.toByteArray()`, name the lambda parameter `dest` (not `bytes`) to avoid shadowing the NSData `bytes` property.
24. **rememberSaveable does not survive process death.** On Android, process death destroys the ViewModel and all in-memory state. For truly persistent state, use DataStore or a database.

## Assets

| Path | Load when... |
|------|-------------|
| [references/setup.md](references/setup.md) | Adding lifecycle and Koin dependencies to the project |
| [references/source-sets.md](references/source-sets.md) | Understanding source set organization and platform boundaries |
| [references/expect-actual.md](references/expect-actual.md) | Adding platform-specific behavior or wrapping platform SDK types |
| [references/data-layer.md](references/data-layer.md) | Creating or modifying Repository, ApiCall, or UseCase classes |
| [references/presentation-layer.md](references/presentation-layer.md) | Creating or modifying ViewModel, UiState, or Screen composables |
| [references/state-management.md](references/state-management.md) | Working with StateFlow, sealed state, one-shot events, or flow combining |
| [references/di-wiring.md](references/di-wiring.md) | Wiring Koin modules for a feature |
| [references/integration.md](references/integration.md) | Understanding upstream/downstream skill connections and data flow |
| [assets/templates/new-feature.kt.template](assets/templates/new-feature.kt.template) | Scaffolding an entirely new feature from scratch |
| [assets/templates/sealed-ui-state.kt.template](assets/templates/sealed-ui-state.kt.template) | Creating a generic sealed UiState with extensions |
| [assets/templates/expect-actual-pair.kt.template](assets/templates/expect-actual-pair.kt.template) | Scaffolding a new expect/actual class, function, or object |
| [assets/snippets/expect-actual-factory.kt](assets/snippets/expect-actual-factory.kt) | Factory pattern for expect classes with different constructors |
| [assets/snippets/state-collection.kt](assets/snippets/state-collection.kt) | Compose state collection patterns |

## Validation

### A. KMP and architecture correctness
- [ ] UiState uses `sealed interface` with Loading/Success/Error subtypes
- [ ] All `MutableStateFlow` fields are private with public `StateFlow` via `asStateFlow()`
- [ ] All ViewModel async work uses `viewModelScope` -- no GlobalScope or standalone CoroutineScope
- [ ] Repository interfaces return `Flow` -- no LiveData
- [ ] UseCase classes use `operator fun invoke()` for clean call sites
- [ ] UseCases registered with `factory` in Koin, Repositories with `single`, ViewModels with `viewModel`
- [ ] No `android.*`, `platform.*`, or iOS imports in shared ViewModel/Repository/UseCase code
- [ ] All PagingData flows use `cachedIn(viewModelScope)`
- [ ] Screen composables use `collectAsStateWithLifecycle()` to observe StateFlow
- [ ] Repository implementation does not block `emitAll` before network calls (uses `onStart` or separate launch)

### B. State management correctness
- [ ] Combined flows use `stateIn` with `SharingStarted.WhileSubscribed(5_000)`
- [ ] One-shot events use `Channel(Channel.BUFFERED)`, not `SharedFlow(replay=0)`
- [ ] State mutations use `update {}` (not direct `.value` assignment)
- [ ] Sealed interface `when` expressions are exhaustive (no else branch)
- [ ] Data class UiState fields have sensible defaults for initial state
- [ ] `distinctUntilChanged()` applied to high-frequency flows before combine
- [ ] `derivedStateOf` used for computed Compose state that should skip recomposition

### C. Expect/actual correctness
- [ ] Every `expect` declaration has matching `actual` for both Android and iOS
- [ ] Factory interfaces preferred over calling `expect class` constructors from commonMain
- [ ] `@OptIn` annotations present for `ExperimentalForeignApi` and `BetaInteropApi` in iOS actuals
- [ ] `@OptIn` never applied to `expect` declarations in commonMain
- [ ] `actual typealias` used only where platform type matches expect signature exactly
- [ ] Source set hierarchy correct (`iosMain` for shared iOS, not `iosArm64Main`)
- [ ] KSP-generated actuals clearly marked as "do not hand-write"

### D. Security
- [ ] No secrets or API keys in any reference or template file
- [ ] No hardcoded base URLs in repository implementations
- [ ] No sensitive data (tokens, passwords) stored in UiState
- [ ] Error states do not expose raw exception messages to UI

### E. Performance
- [ ] StateFlow uses `WhileSubscribed(5_000)` where appropriate for derived flows
- [ ] No unnecessary recompositions from unstable lambda captures in Screen composables
- [ ] `collectAsStateWithLifecycle()` preferred over `collectAsState()` on Android
- [ ] PagingData uses `cachedIn(viewModelScope)` to avoid refetch on config change
- [ ] `memcpy`-based NSData conversion pinned correctly (no unguarded pointer access)

### F. Integration
- [ ] DI wiring registers all layers: Repository, UseCase (if present), ViewModel
- [ ] Template placeholders are consistent and documented
- [ ] Feature module function follows `featureNameModule()` naming convention
- [ ] Cross-references to kmp-dependency-injection and kmp-project-setup resolve correctly
