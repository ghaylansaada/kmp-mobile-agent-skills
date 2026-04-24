# Platform-Specific Limits and New APIs

## iOS: BGContinuedProcessingTask (iOS 26+)

iOS 26 introduces `BGContinuedProcessingTask` for long-running tasks that continue after the user leaves the app. This is distinct from `BGProcessingTaskRequest` and `BGAppRefreshTaskRequest`:

**Requirements:**
- Must be initiated by an explicit user action (e.g., tapping a "Download" or "Export" button). The system rejects tasks not tied to user intent.
- Must report measurable progress to the system. The task provides a progress object that the OS uses to estimate completion time.
- Users can monitor progress and cancel the task from the system UI (Lock Screen, Dynamic Island, or Control Center).
- The task must be meaningful and visible -- it is not a mechanism for silent background processing.

**Failure Strategies:**
- `.fail` -- If the system needs to reclaim resources, the task fails immediately. Use for tasks where partial completion has no value (e.g., atomic database migration).
- `.queue` -- If the system needs to reclaim resources, the task is suspended and re-queued for later execution. Use for tasks where partial progress is retained (e.g., file downloads with resume support).

**Resource Access:**
- GPU access is available on iPad only. On iPhone, GPU access is not granted to `BGContinuedProcessingTask`.
- Network access is available on all devices.
- The task runs with elevated priority compared to standard background tasks but below foreground priority.

**KMP Considerations:**
- `BGContinuedProcessingTask` is an iOS-only API with no Android equivalent. The shared `BackgroundTask` interface does not map to this -- use platform-specific code in `iosMain`.
- For the Android side, the closest equivalent is a foreground service with a notification showing progress, but the lifecycle and user interaction model differ significantly.

## Android: Foreground Service Limits (Android 14+)

Android 14 and later impose strict requirements on foreground services:

**Mandatory foregroundServiceType:**
- Every foreground service must declare a `foregroundServiceType` in the manifest. Calling `startForeground()` without it throws `MissingForegroundServiceTypeException`.
- Valid types: `camera`, `connectedDevice`, `dataSync`, `health`, `location`, `mediaPlayback`, `mediaProjection`, `microphone`, `phoneCall`, `remoteMessaging`, `shortService`, `specialUse`, `systemExempted`, `mediaProcessing`.
- Each type has specific permission requirements (e.g., `location` requires `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION`).

**Timeout Enforcement (dataSync and mediaProcessing):**
- `dataSync` foreground services have a 6-hour execution timeout per 24-hour rolling window.
- `mediaProcessing` foreground services have a 6-hour execution timeout per 24-hour rolling window.
- When the timeout is reached, the system calls `Service.onTimeout(int foregroundServiceType, int reason)`.
- The service MUST call `stopSelf()` within a few seconds of `onTimeout()`. If it does not, the system throws an ANR and force-stops the app.
- The 6-hour budget is per-type, not per-service instance. Multiple `dataSync` services share the same 6-hour budget.

**Implementing onTimeout:**
- Override `Service.onTimeout(int, int)` in every foreground service that uses `dataSync` or `mediaProcessing`.
- In `onTimeout`, save partial progress, clean up resources, and call `stopSelf()`.
- Do not start a new foreground service of the same type from within `onTimeout` -- the budget is exhausted.

**BOOT_COMPLETED Restrictions (Android 15+):**
- `camera`, `microphone`, `mediaProjection`, and `phoneCall` foreground service types cannot be started from a `BOOT_COMPLETED` broadcast receiver.
- `dataSync` and `mediaProcessing` can still be started from `BOOT_COMPLETED` but are subject to the 6-hour timeout.
- Use WorkManager for boot-time background work that does not require a foreground service.

**SYSTEM_ALERT_WINDOW Exemption Narrowed:**
- Previously, apps with `SYSTEM_ALERT_WINDOW` permission could start foreground services from the background.
- Android 15+ narrows this exemption: the overlay window must be visible AND the app must have recently been in the foreground.
- Do not rely on `SYSTEM_ALERT_WINDOW` as a general workaround for background foreground-service launch restrictions.

## Scheduling Constraints and Power Budgeting

**Android WorkManager:**
- Minimum periodic interval: 15 minutes. Shorter values are silently rounded up.
- Battery optimization (Doze mode) defers work until a maintenance window. Use `setExpedited()` for time-sensitive one-time work.
- Network constraints respect data saver settings. `NetworkType.CONNECTED` may not fire if the user has data saver enabled and the app is not exempted.

**iOS BGTaskScheduler:**
- `BGAppRefreshTaskRequest`: approximately 30 seconds of execution time. The OS may grant less.
- `BGProcessingTaskRequest`: several minutes of execution time, but only when the device is charging and connected to Wi-Fi (if `requiresExternalPower` and `requiresNetworkConnectivity` are set).
- Background task execution frequency is determined by the OS based on app usage patterns, battery level, and system load. Frequently used apps get more background time.
- Low Power Mode reduces background app refresh frequency significantly. The OS may skip scheduled tasks entirely while Low Power Mode is active.
- The `earliestBeginDate` is a hint, not a guarantee. The OS may delay execution well beyond the requested time.

## Foreground Services vs Background Tasks

Foreground services and background tasks (WorkManager / BGTaskScheduler) serve different purposes:

- **Foreground services** are visible to the user via a persistent notification. They run with higher priority but have strict type requirements, timeout limits, and must be tied to a specific user-visible purpose.
- **Background tasks** (WorkManager / BGTaskScheduler) are invisible to the user, deferrable, and subject to OS power management. They are appropriate for sync, cleanup, and periodic maintenance.
- Do not use a foreground service as a workaround for background task limitations. The Play Store reviews foreground service usage and may reject apps that misuse them.

## Cross-Skill References

- **kmp-platform-integration** -- Android manifest configuration for `foregroundServiceType`. Privacy manifests for iOS.
- **kmp-permissions** -- Foreground service types may require specific permissions (e.g., `location` type requires location permission). Android 15+ BOOT_COMPLETED restrictions affect permission-gated services.
