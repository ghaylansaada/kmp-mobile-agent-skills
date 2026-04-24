# iOS Implementation: BGTaskScheduler

## BackgroundTaskScheduler -- actual

**File:** `iosMain/.../core/background/BackgroundTaskScheduler.ios.kt`

```kotlin
package {your.package}.core.background

import {your.package}.core.platform.PlatformContext
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.dateWithTimeIntervalSinceNow

@OptIn(ExperimentalForeignApi::class)
actual class BackgroundTaskScheduler actual constructor(
    @Suppress("UNUSED_PARAMETER") context: PlatformContext,
) {
    actual fun schedule(config: BackgroundTaskConfig) {
        val request = if (config.isPeriodic) {
            BGAppRefreshTaskRequest(identifier = config.taskId).apply {
                earliestBeginDate = NSDate.dateWithTimeIntervalSinceNow(
                    config.intervalMinutes * 60.0,
                )
            }
        } else {
            BGProcessingTaskRequest(identifier = config.taskId).apply {
                requiresNetworkConnectivity = config.requiresNetwork
                requiresExternalPower = config.requiresCharging
                earliestBeginDate = NSDate.dateWithTimeIntervalSinceNow(
                    config.intervalMinutes * 60.0,
                )
            }
        }

        memScoped {
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()
            val submitted = BGTaskScheduler.sharedScheduler.submitTaskRequest(
                taskRequest = request,
                error = errorPtr.ptr,
            )
            if (!submitted) {
                println(
                    "iOS: Failed to schedule task ${config.taskId}: " +
                        "${errorPtr.value?.localizedDescription}",
                )
            }
        }
    }

    actual fun cancel(taskId: String) {
        BGTaskScheduler.sharedScheduler.cancelTaskRequestWithIdentifier(taskId)
    }

    actual fun cancelAll() {
        BGTaskScheduler.sharedScheduler.cancelAllTaskRequests()
    }
}
```

## Task Registration and Handling

**File:** `iosMain/.../core/background/BackgroundTasksIos.kt`

Called from `iOSApp.swift init()` -- must execute before the app finishes launching.

```kotlin
package {your.package}.core.background

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGTask
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.dateWithTimeIntervalSinceNow

fun registerAndScheduleBackgroundTasks() {
    registerSyncTask()
    scheduleSyncTask()
}

private fun registerSyncTask() {
    BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(
        identifier = SyncTask.TASK_ID,
        usingQueue = null,
    ) { task -> if (task != null) handleSyncTask(task) }
}

private fun handleSyncTask(task: BGTask) {
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    task.expirationHandler = {
        scope.cancel()
    }

    scope.launch {
        val result = SyncTask().execute()
        task.setTaskCompletedWithSuccess(result is BackgroundTaskResult.Success)
        scheduleSyncTask()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun scheduleSyncTask() {
    val request = BGAppRefreshTaskRequest(identifier = SyncTask.TASK_ID).apply {
        earliestBeginDate = NSDate.dateWithTimeIntervalSinceNow(15.0 * 60.0)
    }

    memScoped {
        val errorPtr = alloc<ObjCObjectVar<NSError?>>()
        val submitted = BGTaskScheduler.sharedScheduler.submitTaskRequest(
            taskRequest = request,
            error = errorPtr.ptr,
        )
        if (!submitted) {
            println(
                "iOS: Failed to schedule sync task: " +
                    "${errorPtr.value?.localizedDescription}",
            )
        }
    }
}
```

## Adding a New Task on iOS

1. Add the task ID to `Info.plist` under `BGTaskSchedulerPermittedIdentifiers`.
2. Add `register<Task>Task()` and `handle<Task>Task()` functions following the pattern above.
3. Call `register<Task>Task()` from `registerAndScheduleBackgroundTasks()`.
4. Re-schedule in the completion handler -- iOS does NOT auto-repeat.

## iOS 26+: BGContinuedProcessingTask

For long-running user-initiated tasks on iOS 26+, see [platform-limits.md](platform-limits.md). `BGContinuedProcessingTask` is a distinct API from `BGProcessingTaskRequest` -- it requires explicit user action, measurable progress reporting, and supports user monitoring/cancellation from the system UI. It has no direct Android equivalent in the shared `BackgroundTask` abstraction.
