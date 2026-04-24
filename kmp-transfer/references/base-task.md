# BaseTransferTask

**File:** `core/transfer/core/BaseTransferTask.kt`

Abstract base providing the full transfer lifecycle: progress tracking, pause/resume/cancel, callback chaining.

```kotlin
package {your.package}.core.transfer.core

import {your.package}.core.transfer.util.SpeedEstimator
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore

abstract class BaseTransferTask(
    parentScope: CoroutineScope,
    parallelism: Int,
    protected val chunkSize: Int,
) {
    protected val taskJob = SupervisorJob(parentScope.coroutineContext[Job])
    protected val taskScope = CoroutineScope(parentScope.coroutineContext + taskJob)
    protected val stateFlow = MutableStateFlow(
        TransferSnapshot(bytesTransferred = 0, totalBytes = 0, state = TaskState.IDLE),
    )
    private var childJob: Job? = null
    private var paused = false
    protected val estimator = SpeedEstimator()
    protected val semaphore = Semaphore(parallelism)
    protected val progressMutex = Mutex()
    protected var bytesTransferredInternal: Long = 0L

    private var progressCallback: ((Long, Long, Long, Int) -> Unit)? = null
    private var successCallback: (() -> Unit)? = null
    private var failureCallback: ((Throwable) -> Unit)? = null
    private var cancelCallback: (() -> Unit)? = null
    private var pauseCallback: (() -> Unit)? = null

    init {
        taskScope.launch {
            stateFlow.collect { snap ->
                progressCallback?.invoke(
                    snap.bytesTransferred,
                    snap.totalBytes,
                    snap.speedBytesPerSec,
                    if (snap.totalBytes == 0L) 0
                    else ((snap.bytesTransferred.toFloat() / snap.totalBytes) * 100).roundToInt(),
                )
                when (snap.state) {
                    TaskState.SUCCESS -> successCallback?.invoke()
                    TaskState.FAILED -> failureCallback?.invoke(snap.error!!)
                    TaskState.CANCELED -> cancelCallback?.invoke()
                    TaskState.PAUSED -> pauseCallback?.invoke()
                    else -> Unit
                }
            }
        }
    }

    suspend fun await(): TaskResult {
        if (stateFlow.value.state == TaskState.IDLE ||
            stateFlow.value.state == TaskState.PAUSED
        ) {
            paused = false
            childJob = taskScope.launch {
                stateFlow.update { it.copy(state = TaskState.RUNNING) }
                try {
                    execute()
                    stateFlow.update { it.copy(state = TaskState.SUCCESS) }
                } catch (_: CancellationException) {
                    val newState = if (paused) TaskState.PAUSED else TaskState.CANCELED
                    stateFlow.update { it.copy(state = newState) }
                } catch (exception: Throwable) {
                    stateFlow.update {
                        it.copy(state = TaskState.FAILED, error = exception)
                    }
                }
            }
        }
        val final = stateFlow.first {
            it.state in setOf(TaskState.SUCCESS, TaskState.FAILED, TaskState.CANCELED)
        }
        return TaskResult(state = final.state, error = final.error)
    }

    protected abstract suspend fun execute()

    fun pause() {
        paused = true
        childJob?.cancel()
        stateFlow.update { it.copy(state = TaskState.PAUSED) }
    }

    fun cancel() {
        paused = false
        childJob?.cancel()
        taskJob.cancel()
        stateFlow.update { it.copy(state = TaskState.CANCELED) }
    }

    fun onProgress(l: (Long, Long, Long, Int) -> Unit) = apply { progressCallback = l }
    fun onSuccess(l: () -> Unit) = apply { successCallback = l }
    fun onFailure(l: (Throwable) -> Unit) = apply { failureCallback = l }
    fun onCanceled(l: () -> Unit) = apply { cancelCallback = l }
    fun onPaused(l: () -> Unit) = apply { pauseCallback = l }
}
```

## Key Design Decisions

- `SupervisorJob(parent)` links to parent scope while isolating child chunk failures
- `pause()` cancels only `childJob`; `cancel()` cancels both `childJob` and `taskJob`
- CancellationException is caught to distinguish pause vs cancel via `paused` flag
- `stateFlow.first { predicate }` suspends until a terminal state is emitted
- Callbacks chained via `apply` for fluent builder pattern
- Progress callback params: bytesTransferred, totalBytes, speedBytesPerSec, progressPercent
