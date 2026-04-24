# Shared Background Task Types (commonMain)

## BackgroundTaskConfig

**File:** `core/background/BackgroundTaskConfig.kt`

```kotlin
package {your.package}.core.background

data class BackgroundTaskConfig(
    val taskId: String,
    val intervalMinutes: Long = 15,
    val requiresNetwork: Boolean = false,
    val requiresCharging: Boolean = false,
    val isPeriodic: Boolean = true,
)
```

## BackgroundTaskResult

**File:** `core/background/BackgroundTaskResult.kt`

```kotlin
package {your.package}.core.background

sealed interface BackgroundTaskResult {
    data object Success : BackgroundTaskResult
    data object Retry : BackgroundTaskResult
    data class Failure(val reason: String) : BackgroundTaskResult
}
```

## BackgroundTask Interface

**File:** `core/background/BackgroundTask.kt`

```kotlin
package {your.package}.core.background

interface BackgroundTask {
    val taskId: String
    suspend fun execute(): BackgroundTaskResult
}
```

## BackgroundTaskScheduler -- expect Declaration

**File:** `core/background/BackgroundTaskScheduler.kt`

```kotlin
package {your.package}.core.background

import {your.package}.core.platform.PlatformContext

expect class BackgroundTaskScheduler(context: PlatformContext) {
    fun schedule(config: BackgroundTaskConfig)
    fun cancel(taskId: String)
    fun cancelAll()
}
```

## Example Task: SyncTask

**File:** `core/background/SyncTask.kt`

```kotlin
package {your.package}.core.background

import kotlin.coroutines.cancellation.CancellationException

class SyncTask : BackgroundTask {
    override val taskId: String = TASK_ID

    override suspend fun execute(): BackgroundTaskResult {
        return try {
            // TODO: Add sync logic here
            BackgroundTaskResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            BackgroundTaskResult.Retry
        }
    }

    companion object {
        const val TASK_ID = "{your.package}.sync"
    }
}
```
