---
name: kmp-kotlin-coroutines
description: >
  Implement all coroutine and Flow patterns in Kotlin Multiplatform: CoroutineContext,
  Dispatchers, Job/SupervisorJob, CoroutineScope, withContext, coroutineScope vs supervisorScope,
  CoroutineExceptionHandler, cooperative cancellation, async/Deferred, structured concurrency,
  StateFlow/SharedFlow state management, Mutex/Semaphore synchronization, platform dispatcher
  selection, exponential backoff retry, Channel-based one-shot events, and reactive data
  pipelines. Includes SKIE bridge for iOS: suspend→async/await, Flow→AsyncSequence,
  StateFlow→@Published, and sealed class→exhaustive Swift enum.
  Activate when adding any async operation, creating observable state, implementing parallel
  processing, adding retry logic, managing cancellable operations, choosing between
  coroutineScope and supervisorScope, handling CancellationException, setting up dispatcher
  switching, or bridging Kotlin coroutines to idiomatic Swift consumption.
compatibility: >
  KMP with kotlinx-coroutines-core. SKIE sections require co.touchlab.skie Gradle plugin.
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---

# KMP Kotlin Coroutines

## When to use

- Adding any async operation (network call, database query, file I/O)
- Understanding CoroutineContext composition, inheritance, and element overrides
- Choosing the correct dispatcher (Main, IO, Default) for platform-specific work
- Managing Job lifecycle, parent-child relationships, or cancellation propagation
- Creating or managing CoroutineScope (viewModelScope, custom scopes, lifecycle)
- Using withContext for dispatcher switching or NonCancellable cleanup
- Deciding between coroutineScope and supervisorScope for parallel work
- Setting up CoroutineExceptionHandler for uncaught exception logging
- Implementing cooperative cancellation (ensureActive, yield, isActive)
- Using async/Deferred for parallel decomposition with awaitAll
- Handling CancellationException correctly (suspendRunCatching pattern)
- Creating observable state for UI consumption with StateFlow or SharedFlow
- Implementing parallel processing with bounded concurrency (Semaphore)
- Adding retry logic with exponential backoff
- Synchronizing concurrent access with Mutex (e.g., token refresh)
- Sending one-shot UI events via Channel (navigation, snackbar)
- Swift code needs to call Kotlin suspend functions with async/await
- Swift code needs to observe Kotlin Flow or StateFlow
- SwiftUI views need to react to Kotlin StateFlow changes
- Swift code needs exhaustive switch on Kotlin sealed classes
- Replacing manual FlowCollector wrappers with SKIE
- Setting up SKIE Gradle plugin for the first time
- Implementing reactive search with debounce and automatic cancellation
- Choosing between StateFlow, SharedFlow, and Channel for UI communication

## Depends on

- **kmp-project-setup** -- `kotlinx-coroutines-core` dependency, source set layout

## Workflow

1. Verify coroutines dependency and dispatcher availability --> [references/setup.md](references/setup.md)
2. Review coroutine fundamentals (context, scopes, cancellation, async) --> [references/coroutine-fundamentals.md](references/coroutine-fundamentals.md)
3. Add concurrency primitives (Mutex, Semaphore, retry, Channel) --> [references/concurrency-patterns.md](references/concurrency-patterns.md)
4. Look up Flow operators and patterns --> [references/flow-patterns.md](references/flow-patterns.md)
5. Wire coroutines into networking, database, and transfer layers --> [references/integration.md](references/integration.md)
6. Set up SKIE bridge for iOS --> [references/ios-swift-bridge.md](references/ios-swift-bridge.md)
   _Skip if not consuming coroutines from Swift._

## Gotchas

1. **`Dispatchers.IO` requires explicit import on non-JVM targets.** `import kotlinx.coroutines.IO` is needed in common and iOS source sets. Omitting it causes "Unresolved reference: IO" only on non-JVM targets.
2. **Kotlin/Native `Dispatchers.Default` may schedule on the main thread.** GCD work queues can map to the main thread, causing unexpected main-thread assertions in UIKit code called from `withContext(Dispatchers.Default)`.
3. **`runBlocking` deadlocks on Native if called on the main thread.** It blocks the thread that the Main dispatcher needs. Use `suspend fun` entry points or `MainScope().launch` instead.
4. **`Mutex` is non-reentrant.** A coroutine holding the lock that calls another suspend function acquiring the same `Mutex` deadlocks silently. Use a double-check pattern instead of nested locking.
5. **`withContext(NonCancellable)` is required for cleanup in `finally` blocks.** Without it, suspend calls inside `finally` of cancelled coroutines are immediately cancelled, leaving resources inconsistent.
6. **`SupervisorJob` cancellation is asymmetric.** Child failure does NOT propagate up, but cancelling the `SupervisorJob` itself cancels ALL children. This distinction matters for pause vs cancel in transfer tasks.
7. **Catching `Exception` swallows `CancellationException`.** In Kotlin, `CancellationException` extends `IllegalStateException`, so `catch (e: Exception)` silently breaks structured concurrency. Always rethrow `CancellationException` or use a dedicated `runCatching`-style wrapper.
8. **`SharedFlow(replay=0)` drops events when no collector is active.** One-shot UI events (navigation, snackbar) should use `Channel(Channel.BUFFERED)` with `receiveAsFlow()` to guarantee delivery even if the collector briefly detaches during configuration changes.
9. **`stateIn` without `WhileSubscribed` keeps upstream collecting forever.** Using `SharingStarted.Eagerly` or `Lazily` on a database or network Flow means the upstream never cancels, wasting resources. Use `WhileSubscribed(5_000)` to cancel the upstream 5 seconds after the last subscriber disappears.
10. **Without SKIE, suspend functions are completion-handler callbacks.** Swift cannot natively await Kotlin suspend functions. You lose structured concurrency and error handling becomes verbose.
11. **`StateFlow.value` without SKIE subscription gives a dead snapshot.** Reading `.value` directly returns the current state but Swift is never notified of changes. Requires `enableSwiftUIObservingPreview = true` AND `@ObservedObject` on the ViewModel.
12. **Swift Task cancellation does NOT cancel raw Kotlin Jobs.** SKIE's AsyncSequence propagates cancellation, but holding a raw `Job` reference requires cancelling both independently.
13. **`[weak self]` is mandatory in Task closures holding Kotlin objects.** Kotlin/Native GC interacts differently with Objective-C ARC, causing retain cycles if Swift objects are captured strongly.
14. **`Dispatchers.Main` and `@MainActor` share the same thread.** Both use `dispatch_get_main_queue`. Blocking either one blocks the other. Keep heavy work on `Dispatchers.IO`/`Default`.
15. **SKIE adds ~50% build time to iOS framework link step.** First build generates the full Swift bridging layer. Subsequent incremental builds are faster. Enable `skie.incremental=true` in `gradle.properties`.
16. **`onEnum(of:)` is SKIE-only.** Removing the SKIE plugin breaks all `switch onEnum(of:)` calls. It is not a standard Kotlin/Native API.
17. **SKIE version is tightly coupled to Kotlin version.** Each SKIE release supports a specific Kotlin version range. Mismatches cause `IncompatibleClassChangeError` at build time.
18. **`autoreleasepool` is required for tight loops collecting Flow emissions.** Without it, Objective-C temporaries accumulate per iteration and memory spikes. Wrap the loop body in `autoreleasepool { }`.
19. **SKIE generic preservation has limits.** Simple generics like `ApiResult<UserEntity>` are preserved, but deeply nested generics, variance annotations, and star projections may still erase to `AnyObject`.
20. **Launching a new coroutine inside an existing coroutine scope breaks structured concurrency.** Called functions should `suspend`, not `launch`. Nested launches create uncontrolled children that the parent cannot wait for, cancel, or receive exceptions from. See [references/concurrency-patterns.md](references/concurrency-patterns.md) ("Structured Concurrency: Never Launch Inside Launch").
21. **iOS re-subscription to StateFlow replays the latest value, causing duplicate event handling.** When a SwiftUI view re-appears, SKIE creates a new AsyncSequence collection that replays the current StateFlow value. Use Channel + receiveAsFlow() for one-shot events instead. See [references/concurrency-patterns.md](references/concurrency-patterns.md) ("StateFlow vs SharedFlow: Choosing the Right Type").

## Assets

| Path | Load when... |
|------|-------------|
| [references/setup.md](references/setup.md) | Setting up coroutines dependency and dispatchers |
| [references/coroutine-fundamentals.md](references/coroutine-fundamentals.md) | CoroutineContext, dispatchers, Job/SupervisorJob, scopes, withContext, cancellation, async/Deferred, suspend conventions |
| [references/concurrency-patterns.md](references/concurrency-patterns.md) | Adding Mutex, Semaphore, retry, Channel, structured concurrency, or parallel processing |
| [references/flow-patterns.md](references/flow-patterns.md) | Flow operator reference (transformation, filtering, combining, terminal) |
| [references/integration.md](references/integration.md) | Wiring coroutines into networking, database, transfer |
| [references/ios-swift-bridge.md](references/ios-swift-bridge.md) | Setting up SKIE, consuming coroutines from Swift |
| [assets/snippets/flow-patterns.kt](assets/snippets/flow-patterns.kt) | Flow operators, stateIn, one-shot collection |
| [assets/snippets/flow-consumption.swift](assets/snippets/flow-consumption.swift) | Reusable Swift utilities for Flow observation via SKIE |
| [assets/templates/coroutine-scope-setup.kt.template](assets/templates/coroutine-scope-setup.kt.template) | Long-lived CoroutineScope with SupervisorJob |
| [assets/templates/skie-gradle-setup.kts.template](assets/templates/skie-gradle-setup.kts.template) | Copy-paste SKIE Gradle configuration |

## Validation

### A. Coroutines correctness
- [ ] `CancellationException` is never caught and swallowed (rethrown in every `catch (e: Exception)` block)
- [ ] Proper structured concurrency (`coroutineScope`, `supervisorScope`, no unstructured `launch`)
- [ ] `supervisorScope` used where independent child failures must not cancel siblings
- [ ] `Mutex` used for thread-safe shared state (not `synchronized` or `AtomicReference`)
- [ ] `Channel` used for one-shot UI events (not `SharedFlow(replay=0)`)
- [ ] `flowOn` used for dispatcher switching on Flow producers (not `withContext` around `flow {}` builders)
- [ ] `stateIn` uses `SharingStarted.WhileSubscribed(5_000)` to avoid upstream leaks
- [ ] Retry logic rethrows `CancellationException` instead of treating it as a transient failure
- [ ] `ensureActive()` called before expensive operations in loops
- [ ] No `GlobalScope` usage

### B. iOS Bridge correctness
- [ ] SKIE `coroutinesInterop = true` configured for Flow to AsyncSequence
- [ ] SKIE `sealedInterop = true` configured for sealed class to Swift enum
- [ ] Suspend functions exposed as async/await in Swift (not completion handlers)
- [ ] `enableSwiftUIObservingPreview = true` set for StateFlow to `@Published`
- [ ] Cancellation propagates from Swift Task to Kotlin coroutine via SKIE AsyncSequence
- [ ] No raw Kotlin `Job` references held without explicit cancellation handling
- [ ] `@ObjCName` annotations used where Swift API naming needs customization
- [ ] `autoreleasepool` wraps tight loop bodies collecting Flow emissions in Swift
- [ ] `[weak self]` used in all Task closures holding Kotlin objects

### C. Security
- [ ] No secrets or tokens in code snippets
- [ ] Token refresh patterns reference kmp-networking (not inlined)
- [ ] Kotlin exceptions logged before crossing Swift boundary (stack traces lost in NSError)

### D. Performance
- [ ] `WhileSubscribed(5_000)` used to release upstream resources when UI is backgrounded
- [ ] `Semaphore` bounds parallel operations to prevent resource exhaustion
- [ ] `distinctUntilChanged()` used where appropriate to avoid redundant recomposition
- [ ] `Dispatchers.Main` used only for UI updates (not blocking work)
- [ ] `autoreleasepool` used in tight Flow collection loops to prevent memory spikes
- [ ] SKIE incremental builds enabled to reduce link time

### E. Integration and cross-skill consistency
- [ ] Depends-on references existing skills (kmp-project-setup)
- [ ] Connected skill references all exist (kmp-networking, kmp-database, kmp-transfer, kmp-architecture)
- [ ] Section order matches project convention (frontmatter, When to use, Depends on, Workflow, Gotchas, Assets, Validation)
