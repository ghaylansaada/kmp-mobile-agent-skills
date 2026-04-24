# Integration: Connectivity

## Koin DI Module

```kotlin
// composeApp/src/commonMain/kotlin/{your/package}/di/modules/ConnectivityModule.kt
package {your.package}.di.modules

import {your.package}.core.connectivity.ConnectivityAwareSync
import {your.package}.core.connectivity.ConnectivityObserver
import {your.package}.core.connectivity.OfflineRequestQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

fun connectivityModule() = module {
    single<ConnectivityObserver> {
        ConnectivityObserver(context = get())
    }
    single<OfflineRequestQueue> {
        OfflineRequestQueue { request ->
            // Route to appropriate repository based on request.operationType.
            // Replace this stub with real dispatch logic.
            true
        }
    }
    single<ConnectivityAwareSync> {
        ConnectivityAwareSync(
            connectivityObserver = get(),
            requestQueue = get(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        )
    }
}
```

Register in your Koin setup and start monitoring at app launch:

```kotlin
// In App composable
val connectivitySync: ConnectivityAwareSync = koinInject()
LaunchedEffect(Unit) { connectivitySync.startMonitoring() }
```

## Repository Integration: Offline-Aware Writes

```kotlin
// composeApp/src/commonMain/kotlin/{your/package}/data/ExampleRepositoryImpl.kt
package {your.package}.data

import {your.package}.core.connectivity.ConnectivityObserver
import {your.package}.core.connectivity.OfflineAwareRepository
import {your.package}.core.connectivity.OfflineRequestQueue
import kotlinx.serialization.json.Json

class ExampleRepositoryImpl(
    override val connectivityObserver: ConnectivityObserver,
    override val requestQueue: OfflineRequestQueue,
    private val json: Json,
) : OfflineAwareRepository {

    suspend fun createItem(name: String, description: String): Boolean {
        val payload = json.encodeToString(
            kotlinx.serialization.serializer(),
            mapOf("name" to name, "description" to description),
        )
        val result = executeOrQueue(
            operationType = "create_item",
            resourceId = null,
            payload = payload,
        ) {
            // apiService.createItem(...)
            true
        }
        return result ?: false
    }

    suspend fun updateItem(id: String, name: String): Boolean {
        val payload = json.encodeToString(
            kotlinx.serialization.serializer(),
            mapOf("id" to id, "name" to name),
        )
        val result = executeOrQueue(
            operationType = "update_item",
            resourceId = id,
            payload = payload,
        ) {
            // apiService.updateItem(...)
            true
        }
        return result ?: false
    }
}
```

## Dependencies

| Direction | Skill | Relation |
|-----------|-------|----------|
| Upstream | kmp-architecture | expect/actual pattern for ConnectivityObserver |
| Upstream | kmp-dependency-injection | DI wiring for observer, queue, sync |
| Downstream | kmp-networking | HTTP client integrates with offline-aware repository |
| Downstream | kmp-notifications | May notify when sync completes after reconnect |
| Downstream | kmp-background-job | Background tasks check connectivity before executing |
