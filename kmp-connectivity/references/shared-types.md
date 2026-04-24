# Shared Types (commonMain)

## ConnectivityStatus

```kotlin
// composeApp/src/commonMain/kotlin/{your/package}/core/connectivity/ConnectivityStatus.kt
package {your.package}.core.connectivity

sealed interface ConnectivityStatus {
    data object Available : ConnectivityStatus
    data object Unavailable : ConnectivityStatus
    data object Losing : ConnectivityStatus
    data object Lost : ConnectivityStatus
}
```

Using `sealed interface` instead of `enum class` allows future extension (e.g., `data class Metered(val kbps: Int) : ConnectivityStatus`) without breaking exhaustive `when` checks at compile time.

## ConnectivityObserver (expect)

```kotlin
// composeApp/src/commonMain/kotlin/{your/package}/core/connectivity/ConnectivityObserver.kt
package {your.package}.core.connectivity

import {your.package}.core.platform.PlatformContext
import kotlinx.coroutines.flow.Flow

expect class ConnectivityObserver(context: PlatformContext) {
    fun observe(): Flow<ConnectivityStatus>
    fun isConnected(): Boolean
}
```

## PendingRequest

```kotlin
// composeApp/src/commonMain/kotlin/{your/package}/core/connectivity/PendingRequest.kt
package {your.package}.core.connectivity

import kotlinx.serialization.Serializable

@Serializable
data class PendingRequest(
    val id: String,
    val operationType: String,
    val resourceId: String? = null,
    val payload: String,
    val createdAt: Long,
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
) {
    val canRetry: Boolean get() = retryCount < maxRetries

    fun withIncrementedRetry(): PendingRequest =
        copy(retryCount = retryCount + 1)
}
```

- `operationType` + `resourceId` pair is used for deduplication in the queue
- `maxRetries` defaults to 3; after exhaustion the request is dropped
- `@Serializable` enables persistence if you need to survive app restarts

## System (expect)

```kotlin
// composeApp/src/commonMain/kotlin/{your/package}/core/connectivity/System.kt
package {your.package}.core.connectivity

expect object System {
    fun currentTimeMillis(): Long
}
```

Platform actuals are in [android-implementation.md](android-implementation.md) and [ios-implementation.md](ios-implementation.md).
