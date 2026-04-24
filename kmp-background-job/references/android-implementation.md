# Android Implementation: WorkManager

## BackgroundTaskScheduler -- actual

**File:** `androidMain/.../core/background/BackgroundTaskScheduler.android.kt`

```kotlin
package {your.package}.core.background

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import {your.package}.core.platform.PlatformContext
import java.util.concurrent.TimeUnit

actual class BackgroundTaskScheduler actual constructor(
    private val context: PlatformContext,
) {
    private val workManager: WorkManager
        get() = WorkManager.getInstance(context.applicationContext)

    actual fun schedule(config: BackgroundTaskConfig) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(
                if (config.requiresNetwork) NetworkType.CONNECTED else NetworkType.NOT_REQUIRED,
            )
            .setRequiresCharging(config.requiresCharging)
            .build()

        if (config.isPeriodic) {
            val request = PeriodicWorkRequestBuilder<AppWorker>(
                repeatInterval = config.intervalMinutes,
                repeatIntervalTimeUnit = TimeUnit.MINUTES,
            )
                .setConstraints(constraints)
                .setInputData(workDataOf(AppWorker.KEY_TASK_ID to config.taskId))
                .addTag(config.taskId)
                .build()

            workManager.enqueueUniquePeriodicWork(
                config.taskId,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        } else {
            val request = OneTimeWorkRequestBuilder<AppWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf(AppWorker.KEY_TASK_ID to config.taskId))
                .addTag(config.taskId)
                .build()

            workManager.enqueueUniqueWork(
                config.taskId,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }

    actual fun cancel(taskId: String) {
        workManager.cancelUniqueWork(taskId)
    }

    actual fun cancelAll() {
        workManager.cancelAllWork()
    }
}
```

## AppWorker (CoroutineWorker)

**File:** `androidMain/.../core/background/AppWorker.kt`

`doWork()` runs on `Dispatchers.Default`. IO operations must be wrapped in `withContext(Dispatchers.IO)`.

```kotlin
package {your.package}.core.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AppWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {

    private val syncTask: SyncTask by inject()

    override suspend fun doWork(): Result {
        val taskId = inputData.getString(KEY_TASK_ID) ?: return Result.failure()

        val task: BackgroundTask = when (taskId) {
            SyncTask.TASK_ID -> syncTask
            else -> return Result.failure()
        }

        return withContext(Dispatchers.IO) {
            when (task.execute()) {
                is BackgroundTaskResult.Success -> Result.success()
                is BackgroundTaskResult.Retry -> Result.retry()
                is BackgroundTaskResult.Failure -> Result.failure()
            }
        }
    }

    companion object {
        const val KEY_TASK_ID = "task_id"
    }
}
```

## Adding a New Task on Android

Add a `when` branch in `AppWorker.doWork()`:

```kotlin
NewTask.TASK_ID -> newTask  // injected via Koin
```

## Foreground Service Limits (Android 14+)

If your background work requires a foreground service (e.g., for long-running data sync with a user-visible notification), see [platform-limits.md](platform-limits.md) for mandatory `foregroundServiceType` declaration, the 6-hour timeout for `dataSync`/`mediaProcessing`, and `Service.onTimeout()` implementation requirements. Prefer WorkManager for deferrable background work.
