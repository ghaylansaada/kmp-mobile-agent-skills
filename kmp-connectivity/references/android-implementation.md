# Android: ConnectivityObserver

`composeApp/src/androidMain/kotlin/{your/package}/core/connectivity/ConnectivityObserver.android.kt`:

```kotlin
package {your.package}.core.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import {your.package}.core.platform.PlatformContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

actual class ConnectivityObserver actual constructor(
    private val context: PlatformContext,
) {
    private val connectivityManager = context.applicationContext
        .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    actual fun observe(): Flow<ConnectivityStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(ConnectivityStatus.Available)
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                trySend(ConnectivityStatus.Losing)
            }

            override fun onLost(network: Network) {
                trySend(ConnectivityStatus.Lost)
            }

            override fun onUnavailable() {
                trySend(ConnectivityStatus.Unavailable)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                val hasInternet = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET,
                )
                // VALIDATED = network can actually reach the internet.
                // Without this, captive portals appear as "connected".
                val validated = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED,
                )
                if (hasInternet && validated) {
                    trySend(ConnectivityStatus.Available)
                } else {
                    trySend(ConnectivityStatus.Unavailable)
                }
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Emit initial state so collectors don't wait for the first callback
        trySend(
            if (isConnected()) ConnectivityStatus.Available
            else ConnectivityStatus.Unavailable,
        )

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

    actual fun isConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
```

## Platform System Helper

```kotlin
// composeApp/src/androidMain/kotlin/{your/package}/core/connectivity/System.android.kt
package {your.package}.core.connectivity

actual object System {
    actual fun currentTimeMillis(): Long = java.lang.System.currentTimeMillis()
}
```
