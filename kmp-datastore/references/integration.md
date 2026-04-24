# Integration: DataStore

## Koin DI Wiring

### localStorageModule (commonMain)

```kotlin
package {your.package}.di.modules

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import {your.package}.core.datastore.DataStoreFactory
import org.koin.dsl.module

fun localStorageModule() = module {
    single<DataStore<Preferences>> { get<DataStoreFactory>().create() }
}
```

### Android platformModule

```kotlin
// Inside actual fun platformModule(...) module block:
import {your.package}.core.datastore.DataStoreFactory
import {your.package}.core.datastore.createDataStore

single<DataStoreFactory> {
    object : DataStoreFactory {
        override fun create() = createDataStore(context = get())
    }
}
```

### iOS platformModule

```kotlin
// Inside actual fun platformModule(...) module block:
import {your.package}.core.datastore.DataStoreFactory
import {your.package}.core.datastore.createDataStore

single<DataStoreFactory> {
    object : DataStoreFactory {
        override fun create() = createDataStore()
    }
}
```

## Module Loading Order

```kotlin
startKoin {
    modules(
        platformModule(),       // provides DataStoreFactory
        localStorageModule(),   // provides DataStore<Preferences>
        sessionModule(),        // provides SessionManager (depends on DataStore)
        ktorfitModule(),        // provides HttpClient (depends on SessionManager)
    )
}
```

## Common Usage Patterns

```kotlin
package {your.package}.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Atomic multi-write
suspend fun DataStore<Preferences>.setMultiple(
    block: MutablePreferences.() -> Unit,
) {
    edit { prefs -> block(prefs) }
}

// Atomic increment
suspend fun DataStore<Preferences>.increment(
    key: Preferences.Key<Int>,
    by: Int = 1,
): Int {
    var result = 0
    edit { prefs ->
        result = (prefs[key] ?: 0) + by
        prefs[key] = result
    }
    return result
}

// Toggle boolean
suspend fun DataStore<Preferences>.toggle(
    key: Preferences.Key<Boolean>,
): Boolean {
    var result = false
    edit { prefs ->
        result = !(prefs[key] ?: false)
        prefs[key] = result
    }
    return result
}

// Map preferences to domain model
fun DataStore<Preferences>.observeSession(): Flow<UserSession?> =
    data.map { prefs ->
        val access = prefs[SessionKeys.ACCESS_TOKEN] ?: return@map null
        val refresh = prefs[SessionKeys.REFRESH_TOKEN] ?: return@map null
        val userId = prefs[SessionKeys.USER_ID] ?: return@map null
        UserSession(access, refresh, userId)
    }
```

## Dependencies

| Direction | Skill | Relation |
|-----------|-------|----------|
| Upstream | kmp-dependency-injection | Koin platform modules provide DataStoreFactory |
| Downstream | kmp-session-management | SessionManager reads/writes tokens via DataStore |
| Downstream | kmp-networking | Ktor Auth plugin consumes SessionManager (which uses DataStore) |
