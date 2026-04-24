---
name: kmp-background-job
description: >
  Use this skill when scheduling or executing background tasks in KMP using
  Android WorkManager and iOS BGTaskScheduler. Activate when the user asks to
  "sync in the background," "run periodic tasks," "schedule work," "survive
  process death," "foreground service limits," or "BGContinuedProcessingTask."
  Covers shared task abstractions (expect/actual), CoroutineWorker,
  BGAppRefreshTaskRequest/BGProcessingTaskRequest/BGContinuedProcessingTask,
  Android foreground service type requirements and timeouts, constraints, power
  budgeting, and platform registration. Does NOT cover push notification handling
  (see kmp-notifications) or real-time sync via WebSockets (see kmp-networking).
compatibility: >
  KMP with Compose Multiplatform. Requires AndroidX WorkManager and iOS BGTaskScheduler.
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---

# KMP Background Jobs

## Scope

Covers scheduling and executing background tasks across Android (WorkManager) and iOS (BGTaskScheduler) using shared abstractions with expect/actual. Includes task configuration, platform schedulers, CoroutineWorker, iOS task registration/re-scheduling, DI wiring, Android foreground service limits and timeout handling, iOS BGContinuedProcessingTask (iOS 26+), and power budgeting constraints. Does not cover push notifications or WebSocket sync.

## When to use

- Scheduling periodic background sync (data refresh, cache cleanup)
- Implementing one-time background tasks (upload, processing)
- Using WorkManager for Android background work with constraints
- Configuring BGTaskScheduler for iOS background tasks
- Handling foreground service limits on Android 16+
- Using BGContinuedProcessingTask for long-running iOS tasks
- Syncing data when the app is not in the foreground
- Retrying failed background operations with backoff

## Depends on

- KMP project with Compose Multiplatform (see `kmp-project-setup`)
- expect/actual pattern (see `kmp-architecture`)
- Koin for dependency injection (see `kmp-dependency-injection`)

## Workflow

1. **Add dependencies and platform manifests** -- read `references/setup.md`
   _Skip if WorkManager and Info.plist identifiers are already configured._
2. **Define shared types** (BackgroundTaskConfig, BackgroundTaskResult, BackgroundTask interface, expect BackgroundTaskScheduler) -- read `references/shared-types.md`
3. **Implement Android scheduler and worker** -- read `references/android-implementation.md`
   _Skip if only working on iOS._
4. **Implement iOS scheduler and registration** -- read `references/ios-implementation.md`
   _Skip if only working on Android._
5. **Wire DI and app lifecycle** -- read `references/integration.md`
6. **Review platform limits and new APIs** -- read `references/platform-limits.md`
   _Covers Android foreground service timeouts, iOS BGContinuedProcessingTask, and power budgeting._
7. **Scaffold a new task** -- use template at `assets/templates/background-task.kt.template`
   _Load only when adding an entirely new background task._

## Gotchas

1. **`CoroutineWorker.doWork()` runs on `Dispatchers.Default`**, not `Dispatchers.IO`. Wrap IO operations in `withContext(Dispatchers.IO)` or the task silently runs blocking IO on the computation pool, starving CPU-bound coroutines.
2. **iOS `BGTaskSchedulerPermittedIdentifiers` in Info.plist must list every task ID before the app finishes launching.** Missing identifiers cause `submitTaskRequest` to fail silently with no error callback.
3. **`BGAppRefreshTaskRequest` has a hard ~30-second execution limit.** Use `BGProcessingTaskRequest` for longer work; otherwise the OS terminates the task mid-flight and data can be left in an inconsistent state.
4. **iOS background tasks are NOT guaranteed to execute.** The OS decides based on battery, usage patterns, and system load. Never depend on background execution for data correctness; treat it as an optimization.
5. **WorkManager minimum periodic interval is 15 minutes.** Shorter values are silently rounded up, so a 5-minute interval still fires at 15-minute cadence with no warning.
6. **iOS tasks must be re-scheduled inside the completion handler.** `BGTaskScheduler` does not auto-repeat like WorkManager periodic work. Forgetting this means the task runs exactly once and never again.
7. **Catching `Exception` inside a `suspend fun` swallows `CancellationException`.** Always rethrow `CancellationException` (or catch only non-cancellation exceptions with `runCatching` + `onFailure`) to preserve structured concurrency cancellation. See **kmp-kotlin-coroutines** skill for cancellation handling patterns.
8. **iOS `expirationHandler` must cancel in-flight work.** An empty expiration handler lets the coroutine keep running after the OS revokes execution time, wasting resources and risking a system-level kill of the app.
9. **Foreground services are NOT background tasks.** They have strict type requirements (`foregroundServiceType` in manifest), timeout limits (6 hours for `dataSync`/`mediaProcessing`), and must implement `Service.onTimeout()`. Using a foreground service as a workaround for background task limitations causes Play Store rejection.
10. **BGContinuedProcessingTask requires user-initiated action.** iOS 26 `BGContinuedProcessingTask` must be started in response to an explicit user action (tap, button press). The system rejects tasks not tied to user intent. It also requires measurable progress reporting -- users can monitor and cancel from the system UI.
11. **iOS Low Power Mode silently reduces background refresh frequency.** When Low Power Mode is active, the OS may skip scheduled `BGAppRefreshTaskRequest` and `BGProcessingTaskRequest` tasks entirely. Apps must not assume background tasks will execute on schedule. Design for graceful degradation when background execution is unavailable.
12. **Android dataSync/mediaProcessing foreground services share a 6-hour budget per 24 hours.** Multiple service instances of the same type count against the same budget. When the timeout fires via `onTimeout()`, the service must call `stopSelf()` within seconds or the system throws an ANR.
13. **Android 15+ restricts foreground service launch from BOOT_COMPLETED.** Camera, microphone, mediaProjection, and phoneCall foreground service types cannot be started from a `BOOT_COMPLETED` receiver. Use WorkManager for boot-time work.

## Assets

| Path | Load when... |
|---|---|
| references/setup.md | Adding WorkManager or BGTaskScheduler dependencies and manifest configuration |
| references/shared-types.md | Defining shared background task types and expect/actual scheduler interface |
| references/android-implementation.md | Implementing Android WorkManager scheduler and CoroutineWorker |
| references/ios-implementation.md | Implementing iOS BGTaskScheduler registration and task handlers |
| references/integration.md | Wiring background task DI modules and app lifecycle hooks |
| references/platform-limits.md | Reviewing foreground service timeouts, BGContinuedProcessingTask, or power budgeting |
| assets/templates/background-task.kt.template | Scaffolding a new background task implementation |

## Validation

### A. Kotlin and KMP correctness
- [ ] `BackgroundTaskResult` uses `sealed interface`, not `sealed class`
- [ ] Android uses `WorkManager` (not `AlarmManager` or `JobScheduler` directly)
- [ ] iOS uses `BGTaskScheduler` for background task registration and scheduling
- [ ] `AppWorker.doWork()` wraps IO operations in `withContext(Dispatchers.IO)`
- [ ] `CancellationException` is rethrown in every `catch (e: Exception)` block inside `suspend` functions
- [ ] iOS `expirationHandler` cancels the in-flight coroutine `Job`
- [ ] iOS task handlers re-schedule the task in the completion block
- [ ] `memScoped` is used for native memory allocation (no `nativeHeap.alloc` without `free`)
- [ ] `@OptIn(ExperimentalForeignApi::class)` applied where `kotlinx.cinterop` APIs are used
- [ ] Retry strategy is implemented (WorkManager `Result.retry()` / `BackgroundTaskResult.Retry`)
- [ ] Android foreground services declare `foregroundServiceType` in manifest
- [ ] `Service.onTimeout(int, int)` implemented for `dataSync`/`mediaProcessing` foreground services
- [ ] `stopSelf()` called promptly in `onTimeout()` handler to avoid ANR
- [ ] iOS `BGContinuedProcessingTask` only used in response to explicit user actions (iOS 26+)
- [ ] Low Power Mode degradation handled -- app works correctly when background tasks are skipped

### B. Security
- [ ] No sensitive data logged in background task completion or failure messages
- [ ] Task IDs do not expose internal architecture details

### C. Performance
- [ ] Periodic intervals are >= 15 minutes (WorkManager minimum)
- [ ] Long-running iOS tasks use `BGProcessingTaskRequest`, not `BGAppRefreshTaskRequest`
- [ ] No unbounded coroutine scopes that could leak on iOS
- [ ] Android `dataSync`/`mediaProcessing` foreground services stay within the 6-hour per 24-hour budget
- [ ] iOS BGContinuedProcessingTask reports measurable progress to the system
- [ ] App handles Low Power Mode gracefully (background refresh frequency may be reduced to zero)

### D. Integration
- [ ] Koin module registers `BackgroundTaskScheduler` and all task implementations
- [ ] iOS `registerAndScheduleBackgroundTasks()` is called before app finishes launching
- [ ] Android `Configuration.Provider` is implemented if using a custom `Application` class
- [ ] All task IDs in iOS code have matching entries in Info.plist `BGTaskSchedulerPermittedIdentifiers`
- [ ] Foreground service types declared in AndroidManifest.xml match the types used in `startForeground()` calls
- [ ] BOOT_COMPLETED receiver does not launch restricted foreground service types (camera, microphone, mediaProjection, phoneCall) on Android 15+
