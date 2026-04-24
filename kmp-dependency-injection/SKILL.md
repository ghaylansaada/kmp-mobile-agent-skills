---
name: kmp-dependency-injection
description: >
  Koin-based dependency injection for Kotlin Multiplatform -- module organization,
  platform wiring, typed qualifiers, ViewModel scoping, and screen-level injection.
  Use when setting up DI, adding Koin modules, wiring platform implementations,
  registering ViewModels, injecting into Composable screens, or debugging
  resolution failures. Does NOT cover architecture patterns (see kmp-architecture),
  HTTP client configuration (see kmp-networking), or database setup (see kmp-database).
compatibility: >
  KMP with Compose Multiplatform. Requires Koin BOM, koin-core, koin-compose, and
  koin-compose-viewmodel.
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---

# KMP Dependency Injection

## Scope

Covers Koin DI setup and extension for a KMP project: module organization, platform-specific wiring via expect/actual, typed qualifiers, ViewModel registration with the viewModel DSL, screen injection with koinViewModel() and koinInject(), initialization guards, and module graph testing with checkModules(). Does not cover architecture patterns, networking stack, or database internals.

## When to use

- Setting up DI for a new KMP project
- Adding a new Koin module for a new feature
- Wiring platform-specific implementations into the DI graph
- Registering a new ViewModel with the viewModel DSL
- Injecting dependencies into Composable screens
- Disambiguating multiple instances of the same type with typed qualifiers
- Debugging Koin resolution failures

## Depends on

- **kmp-project-setup** -- source set layout and Gradle configuration
- **kmp-architecture** -- PlatformContext and PlatformConfig expect/actual types

## Workflow

1. Add Koin BOM + core + compose + compose-viewmodel to version catalog -- see [setup.md](references/setup.md)
2. Add Koin dependencies to composeApp build.gradle.kts -- see [setup.md](references/setup.md)
3. Create KoinInitializer with initKoin() and double-init guard -- see [initializer.md](references/initializer.md)
4. Implement common modules (core, localStorage, session, ktorfit, etc.) -- see [module-definitions.md](references/module-definitions.md)
5. Declare expect fun platformModule() and implement per platform -- see [platform-modules.md](references/platform-modules.md)
6. Call initKoin() from Android MainActivity and iOS MainViewController -- see [platform-modules.md](references/platform-modules.md#android-entry-point)
7. Inject into screens with koinViewModel() and koinInject() -- see [integration.md](references/integration.md#screen-injection)

## Gotchas

1. **Koin single is lazy -- ANR risk on main thread.** Singletons are created on first access, not at startup. If a heavy singleton (database, HTTP client) is first accessed on the main thread during a user interaction, it blocks the UI thread and causes ANR on Android. Use `createdAtStart()` only for truly critical singletons, or warm them in a background coroutine at app launch.
2. **expect fun platformModule() must be a function, not a property.** Using `expect val platformModule: Module` compiles but causes issues with Koin's lazy initialization on iOS. Always use `expect fun platformModule(): Module`.
3. **String qualifiers are typo-prone and cause runtime crashes.** Named qualifiers like `named("authorizedHttpClient")` must match exactly between registration and resolution. A single character difference causes runtime NoBeanDefFoundException with no compile-time warning. Use enum-based qualifiers for compile-time safety -- see [koin-qualifiers.kt](assets/snippets/koin-qualifiers.kt).
4. **koinViewModel() scope must match lifecycle.** Using koinViewModel() in a NavHost destination scopes the ViewModel to that destination. Using it in a parent composable scopes it to the parent. Mismatched scoping causes premature cleanup (lost state on back-navigation) or memory leaks (ViewModel outlives its screen).
5. **iOS Koin singletons capturing NSObject cause retain cycles.** If a singleton holds a strong reference to an NSObject (e.g., UIViewController), it creates a retain cycle because the singleton is never released. This leaks memory for the lifetime of the app. Use weak references or restructure to avoid holding platform objects.
6. **Module loading order matters for overrides.** When the same type is registered in multiple modules, the last-loaded module wins silently. Useful for test overrides but dangerous if accidental -- a misplaced module can shadow a production binding with no warning.
7. **Never call startKoin more than once.** Calling startKoin() a second time throws KoinAppAlreadyStartedException, crashing the app. Always use an AtomicBoolean guard (`if (!koinStarted.compareAndSet(false, true)) return`) in initKoin() for thread safety.
8. **viewModel DSL is different from single DSL.** Registering a ViewModel with `single<MyViewModel>` bypasses lifecycle scoping -- it lives forever as an app-scoped singleton instead of being tied to navigation/composition lifecycle. This prevents garbage collection and breaks SavedStateHandle.
9. **Constructor injection required everywhere.** Using `get()` or `inject()` inside business logic classes (repositories, use cases) is service locator pattern. It hides dependencies, makes testing harder, and breaks when Koin context is unavailable. Always declare dependencies as constructor parameters.
10. **createdAtStart() runs on the main thread.** Marking a singleton with `createdAtStart()` causes it to initialize during startKoin(), which runs on the calling thread (typically main). Heavy initialization (network, disk I/O) blocks the UI. Only use for lightweight configuration objects.

## Assets

| Path | Load when... |
|------|-------------|
| [references/setup.md](references/setup.md) | Adding Koin dependencies to a project |
| [references/initializer.md](references/initializer.md) | Setting up KoinInitializer and module registration |
| [references/module-definitions.md](references/module-definitions.md) | Adding or modifying Koin modules |
| [references/platform-modules.md](references/platform-modules.md) | Wiring platform-specific bindings or entry points |
| [references/integration.md](references/integration.md) | Injecting into screens, understanding scope and loading order |
| [assets/snippets/koin-qualifiers.kt](assets/snippets/koin-qualifiers.kt) | Using typed qualifier patterns instead of raw strings |
| [assets/templates/new-koin-module.kt.template](assets/templates/new-koin-module.kt.template) | Creating a new feature module from scratch |

## Validation

### A. DI correctness
- [ ] Constructor injection used everywhere -- no `get()` or `inject()` inside business logic classes
- [ ] No service locator pattern in repositories, use cases, or ViewModels
- [ ] `koinViewModel()` used in Compose screens (not manual `getViewModel` or `get()`)
- [ ] Typed qualifiers used (enum or object-based, not raw string `named("...")`)
- [ ] Module organization matches feature boundaries (one module per feature area)
- [ ] No circular dependencies between Koin modules
- [ ] `createdAtStart()` only used for lightweight, truly critical singletons
- [ ] Platform modules use `expect fun` / `actual fun` (not `expect val`)
- [ ] `viewModel { }` DSL used for all ViewModels (not `single { }`)
- [ ] `initKoin()` has a thread-safe double-init guard

### B. Security
- [ ] No secrets, API keys, or hardcoded credentials in any file
- [ ] No hardcoded base URLs in module definitions

### C. Performance
- [ ] Heavy singletons not marked `createdAtStart()` (database, HTTP client)
- [ ] iOS singletons do not hold strong references to NSObject types
- [ ] Factory scope not used for expensive objects (HTTP clients, database connections)

### D. Integration
- [ ] Depends-on references match actual skill directory names
- [ ] Integration reference lists correct upstream and downstream skills
- [ ] Template placeholders are consistent and documented
- [ ] Module loading order documented and matches dependency graph
