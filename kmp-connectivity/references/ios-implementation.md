# iOS: ConnectivityObserver

`composeApp/src/iosMain/kotlin/{your/package}/core/connectivity/ConnectivityObserver.ios.kt`:

```kotlin
package {your.package}.core.connectivity

import {your.package}.core.platform.PlatformContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.Network.nw_path_status_satisfiable
import platform.darwin.dispatch_queue_create

actual class ConnectivityObserver actual constructor(
    @Suppress("UNUSED_PARAMETER") context: PlatformContext,
) {
    // Track last-known status so isConnected() returns a meaningful value.
    // Defaults to true (optimistic) because NWPathMonitor only provides
    // async updates -- there is no synchronous path query on iOS.
    // The first callback fires ~50 ms after nw_path_monitor_start.
    @Volatile
    private var lastKnownConnected: Boolean = true

    actual fun observe(): Flow<ConnectivityStatus> = callbackFlow {
        val monitor = nw_path_monitor_create()
        // Dedicated queue -- using main queue delays/drops updates
        // during heavy UI work
        val queue = dispatch_queue_create(
            "{your.package}.connectivity",
            null,
        )

        nw_path_monitor_set_update_handler(monitor) { path ->
            val status = nw_path_get_status(path)
            val connectivityStatus = when (status) {
                nw_path_status_satisfied -> ConnectivityStatus.Available
                nw_path_status_satisfiable -> ConnectivityStatus.Losing
                else -> ConnectivityStatus.Unavailable
            }
            lastKnownConnected =
                connectivityStatus is ConnectivityStatus.Available
            trySend(connectivityStatus)
        }

        nw_path_monitor_set_queue(monitor, queue)
        nw_path_monitor_start(monitor)

        awaitClose {
            nw_path_monitor_cancel(monitor)
        }
    }.distinctUntilChanged()

    // NWPathMonitor is async-only -- no synchronous path query exists.
    // Returns the last value received from the update handler.
    // Before the first callback fires (~50 ms after start), this
    // optimistically returns true. See gotcha #9 in SKILL.md.
    actual fun isConnected(): Boolean = lastKnownConnected
}
```

## Platform System Helper

```kotlin
// composeApp/src/iosMain/kotlin/{your/package}/core/connectivity/System.ios.kt
package {your.package}.core.connectivity

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual object System {
    actual fun currentTimeMillis(): Long =
        (NSDate().timeIntervalSince1970 * 1000).toLong()
}
```
