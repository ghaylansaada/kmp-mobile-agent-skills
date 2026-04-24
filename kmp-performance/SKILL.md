---
name: kmp-performance
description: >
  Optimize performance in Kotlin Multiplatform mobile apps with Compose
  Multiplatform. Covers startup optimization, lazy initialization, Android
  Baseline Profiles, memory leak detection, Compose recomposition profiling, and
  coroutine performance pitfalls. Use this skill when the app is slow, laggy,
  leaking memory, or when preparing for production -- even if the user just says
  "make it faster" or "fix the jank."
compatibility: >
  KMP with Compose Multiplatform. Baseline Profiles require Android AGP 8+.
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---

# KMP Performance

## When to use

- Optimizing app startup time on Android and iOS
- Reducing unnecessary Compose recompositions
- Generating and applying Android Baseline Profiles
- Detecting and fixing memory leaks
- Profiling coroutine dispatcher usage for performance
- Flattening deep Compose trees to avoid iOS frame drops
- Detecting main-thread disk/network violations with StrictMode
- Profiling CPU, memory, or recomposition performance

## Depends on

- [kmp-project-setup](../kmp-project-setup/SKILL.md) -- Gradle and source set structure
- [kmp-compose-ui](../kmp-compose-ui/SKILL.md) -- Compose Multiplatform setup

## Workflow

1. Add profiling dependencies and metrics config -- read [references/setup.md](references/setup.md)
2. Implement startup tracing and lazy init -- read [references/startup-optimization.md](references/startup-optimization.md)
3. Generate Android Baseline Profiles -- read [references/baseline-profiles.md](references/baseline-profiles.md)
4. Fix memory leaks -- read [references/memory-leaks.md](references/memory-leaks.md)
5. Profile and fix Compose recompositions -- read [references/compose-stability.md](references/compose-stability.md)
6. Optimize coroutine dispatchers and throttling -- read [references/coroutine-patterns.md](references/coroutine-patterns.md)
7. Enable StrictMode and use debugging tools -- read [references/debugging-tools.md](references/debugging-tools.md)
8. Wire profiling into app layers -- read [references/integration.md](references/integration.md)

Steps 2-7 are independent. Skip any step that does not apply to the current task.

## Gotchas

1. **`derivedStateOf` caches and only recomputes when its read state objects change.** `remember { someState.value + 1 }` recomputes on every recomposition. Using `remember` for derived values causes unnecessary work -- use `derivedStateOf` when the result depends on other Compose state.

2. **Kotlin/Native has no escape analysis.** On JVM, short-lived objects in hot loops can be stack-allocated. On Native, every allocation goes to heap and must be GC'd. Avoid allocating wrapper objects in tight loops in shared code targeting iOS.

3. **Baseline Profiles need BOTH `ProfileInstaller` library AND a Macrobenchmark test.** Adding one without the other means no profile is ever applied. The test must run on a physical device or API 28+ emulator.

4. **Compose Multiplatform on iOS renders via Metal/Skia.** Deeply nested composable trees (>30 levels) cause frame drops because render tree traversal is more expensive than Android's hardware-accelerated Canvas.

5. **Never profile on debug builds.** Debug builds disable R8, include debugging instrumentation, and run without ART compilation. Use release or benchmark build variants for production-representative numbers.

6. **Adding LeakCanary with `implementation` instead of `debugImplementation` ships leak detection UI to production.** Always use `debugImplementation` and verify with the release classpath.

7. **Koin's `single {}` is already lazy -- it creates the instance on first `get()`.** The real startup optimization is ensuring nothing triggers `get()` for non-critical singletons (ImageLoader, Database) during Application.onCreate.

8. **Use `SideEffect` with a counter to measure actual recomposition count per composable.** High counts (>10 per user action) indicate unstable parameters. Check Compose compiler metrics before adding `@Stable`/`@Immutable`.

9. **Flow throttling that only checks elapsed time silently drops the final emission.** When the final emission falls within the interval window, it is lost. Always flush the last value (e.g., via `onCompletion`) or the UI will show stale progress (e.g., 97% instead of 100%).

10. **R8/ProGuard rules must be tested with a release build after every rule change.** Missing keep rules on Ktor serialization models or Koin module classes cause runtime `ClassNotFoundException` that never appears in debug builds.

11. **StrictMode catches main-thread violations that cause ANR but only if explicitly enabled.** Many KMP operations (Room migration, DataStore first read, initial Ktor setup) perform disk I/O that silently degrades performance. Without StrictMode, these violations go unnoticed until users experience jank or ANR dialogs. Enable it in debug builds to surface violations early -- then fix by moving blocking work to a background dispatcher or using suspend functions.

## Assets

| Path | Load when... |
|------|-------------|
| [assets/templates/baseline-profile.kt.template](assets/templates/baseline-profile.kt.template) | Generating a baseline profile or startup benchmark |
| [assets/snippets/performance-patterns.kt](assets/snippets/performance-patterns.kt) | Implementing SuspendLazy, StableWrapper, selectors, batch updates, or throttling |

## Validation

### A. Build and compilation

- [ ] LeakCanary present in debug classpath only -- not shipped in release
- [ ] Compose compiler metrics generated in `composeApp/build/compose_metrics/`

### B. Performance correctness

- [ ] No blocking calls on main thread (Room migrations, file I/O wrapped in appropriate dispatcher)
- [ ] Lazy initialization for heavy objects (ImageLoader, Database not resolved during Application.onCreate)
- [ ] Proper dispatcher usage: IO for file I/O, Default for CPU-bound work, no wrapping Ktor calls in IO
- [ ] `@Stable`/`@Immutable` annotations on UI state data classes consumed by Compose
- [ ] `derivedStateOf` used for computed Compose state (not plain `remember`)
- [ ] Baseline profile generated and `baseline-prof.txt` is non-empty
- [ ] R8 rules tested with release build -- no runtime `ClassNotFoundException`
- [ ] Transfer progress throttled to prevent >10 StateFlow emissions per second
- [ ] Compose tree depth under 30 levels for iOS performance
- [ ] `SuspendLazy.initialized` field uses `@Volatile` for safe reads outside mutex on JVM
- [ ] StrictMode enabled in debug builds (Android) for disk and network detection
- [ ] No disk I/O on main thread (validated via StrictMode or profiler)

### C. Security

- [ ] LeakCanary uses `debugImplementation` only -- never `implementation`
- [ ] No performance logging in release builds that exposes internal timing data
- [ ] Heap dump functionality not accessible in production

### D. Integration

- [ ] StartupTracer.markPhase called at Koin init, DB migration, and first composition
- [ ] LogRecompositions wired into suspect composables during profiling
- [ ] Compose compiler metrics checked for stability regressions after model changes
- [ ] Slow startup reporting integrated with analytics/crash reporting skill
- [ ] `baseline-prof.txt` checked into source control after generation

### E. Cross-skill consistency

- [ ] Gradle setup references match `kmp-project-setup` patterns (version catalog, module structure)
- [ ] Compose annotations match `kmp-compose-ui` conventions
- [ ] Koin module structure matches `kmp-dependency-injection` patterns
- [ ] Transfer task patterns consistent with file transfer skill conventions
