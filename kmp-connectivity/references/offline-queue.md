# Offline Queue and Sync (commonMain)

## OfflineRequestQueue

```kotlin
// composeApp/src/commonMain/kotlin/{your/package}/core/connectivity/OfflineRequestQueue.kt
package {your.package}.core.connectivity

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class OfflineRequestQueue(
    private val onExecute: suspend (PendingRequest) -> Boolean,
) {
    private val mutex = Mutex()
    private val _queue = mutableListOf<PendingRequest>()
    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    suspend fun enqueue(request: PendingRequest) {
        mutex.withLock {
            // Deduplicate by (operationType, resourceId) when resourceId is non-null.
            // Null resourceId means "create" -- each create is unique.
            _queue.removeAll { existing ->
                existing.operationType == request.operationType &&
                    existing.resourceId == request.resourceId &&
                    existing.resourceId != null
            }
            _queue.add(request)
            _pendingCount.value = _queue.size
        }
    }

    suspend fun drainOnReconnect() {
        // Snapshot-then-iterate: lock is NOT held during onExecute network calls.
        val snapshot = mutex.withLock { _queue.toList() }
        for (request in snapshot) {
            val success = try {
                onExecute(request)
            } catch (_: Exception) {
                false
            }
            mutex.withLock {
                if (success) {
                    _queue.removeAll { it.id == request.id }
                } else if (request.canRetry) {
                    val index = _queue.indexOfFirst { it.id == request.id }
                    if (index >= 0) {
                        _queue[index] = request.withIncrementedRetry()
                    }
                } else {
                    // Max retries exhausted -- drop the request
                    _queue.removeAll { it.id == request.id }
                }
                _pendingCount.value = _queue.size
            }
        }
    }

    suspend fun clear() {
        mutex.withLock {
            _queue.clear()
            _pendingCount.value = 0
        }
    }

    suspend fun peek(): List<PendingRequest> = mutex.withLock {
        _queue.toList()
    }
}
```

## ConnectivityAwareSync

Monitors connectivity and drains the queue on reconnection. Uses a `wasOffline` flag to avoid draining on the initial `Available` emission (see gotcha #1 in SKILL.md).

```kotlin
// composeApp/src/commonMain/kotlin/{your/package}/core/connectivity/ConnectivityAwareSync.kt
package {your.package}.core.connectivity

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ConnectivityAwareSync(
    private val connectivityObserver: ConnectivityObserver,
    private val requestQueue: OfflineRequestQueue,
    private val scope: CoroutineScope,
) {
    fun startMonitoring() {
        scope.launch {
            var wasOffline = false
            // Using collect (not collectLatest) so an in-flight drain
            // is not cancelled if a new emission arrives mid-execution.
            connectivityObserver.observe().collect { status ->
                when (status) {
                    ConnectivityStatus.Available -> {
                        if (wasOffline) {
                            requestQueue.drainOnReconnect()
                        }
                        wasOffline = false
                    }
                    ConnectivityStatus.Unavailable,
                    ConnectivityStatus.Lost,
                    -> wasOffline = true
                    ConnectivityStatus.Losing -> { /* no-op */ }
                }
            }
        }
    }
}
```

## OfflineAwareRepository

Mixin interface for repositories that need offline support:

```kotlin
// composeApp/src/commonMain/kotlin/{your/package}/core/connectivity/OfflineAwareRepository.kt
package {your.package}.core.connectivity

interface OfflineAwareRepository {
    val connectivityObserver: ConnectivityObserver
    val requestQueue: OfflineRequestQueue

    suspend fun <T> executeOrQueue(
        operationType: String,
        resourceId: String? = null,
        payload: String,
        onlineAction: suspend () -> T,
    ): T? {
        return if (connectivityObserver.isConnected()) {
            try {
                onlineAction()
            } catch (_: Exception) {
                enqueueRequest(operationType, resourceId, payload)
                null
            }
        } else {
            enqueueRequest(operationType, resourceId, payload)
            null
        }
    }

    private suspend fun enqueueRequest(
        operationType: String,
        resourceId: String?,
        payload: String,
    ) {
        requestQueue.enqueue(
            PendingRequest(
                id = "${operationType}_${resourceId ?: System.currentTimeMillis()}",
                operationType = operationType,
                resourceId = resourceId,
                payload = payload,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }
}
```

The `System` expect object is declared in [shared-types.md](shared-types.md) with actuals in [android-implementation.md](android-implementation.md) and [ios-implementation.md](ios-implementation.md).
