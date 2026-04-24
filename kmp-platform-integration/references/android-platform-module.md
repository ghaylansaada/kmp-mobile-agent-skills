# Android Platform DI Module

## PlatformModule.android.kt

```kotlin
// composeApp/src/androidMain/kotlin/{your/package}/di/PlatformModule.android.kt
package {your.package}.di

import android.content.Context
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import {your.package}.core.config.PlatformConfig
import {your.package}.core.database.DatabaseFactory
import {your.package}.core.database.getDatabaseBuilder
import {your.package}.core.datastore.DataStoreFactory
import {your.package}.core.datastore.createDataStore
import {your.package}.core.network.HttpClientEngineFactory
import {your.package}.core.platform.PlatformContext
import {your.package}.core.transfer.io.AndroidFileReaderFactory
import {your.package}.core.transfer.io.AndroidFileWriterFactory
import {your.package}.core.transfer.io.FileReaderFactory
import {your.package}.core.transfer.io.FileWriterFactory
import {your.package}.data.local.AppDatabase
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(
    context: PlatformContext,
    config: PlatformConfig,
): Module = module {
    single<Context> { context.androidContext.applicationContext }
    single<DataStoreFactory> {
        object : DataStoreFactory {
            override fun create() = createDataStore(context = get())
        }
    }
    single<HttpClientEngineFactory> {
        object : HttpClientEngineFactory {
            override fun create() = OkHttp.create()
        }
    }
    single<DatabaseFactory<AppDatabase>> {
        object : DatabaseFactory<AppDatabase> {
            override fun create(): AppDatabase = getDatabaseBuilder(context = get())
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        }
    }
    single<FileReaderFactory> { AndroidFileReaderFactory() }
    single<FileWriterFactory> { AndroidFileWriterFactory() }
}
```

## Database Builder

```kotlin
// composeApp/src/androidMain/kotlin/{your/package}/core/database/Database.android.kt
package {your.package}.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import {your.package}.data.local.AppDatabase
import {your.package}.data.local.AppDatabaseConstructor

fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("app.db")
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = dbFile.absolutePath,
        factory = AppDatabaseConstructor::initialize,
    )
}
```

## DataStore

```kotlin
// composeApp/src/androidMain/kotlin/{your/package}/core/datastore/DataStore.android.kt
package {your.package}.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

fun createDataStore(context: Context): DataStore<Preferences> = createDataStore(
    producePath = { context.filesDir.resolve(dataStoreFileName).absolutePath },
)
```

## Wiring Diagram

```
MainActivity.onCreate()
    +-- PlatformContext(this) ---------> wraps Activity context
    +-- PlatformConfig(ANDROID) ------> marks platform identity
    +-- initKoin(context, config)
    |       +-- commonModules(context)
    |       |       +-- coreModule()
    |       |       +-- localStorageModule()
    |       |       +-- ktorfitModule()
    |       |       +-- repositoryModules()
    |       |       +-- viewModelModule()
    |       |       +-- sessionModule()
    |       +-- platformModule(context, config)
    |               +-- Context (applicationContext)
    |               +-- DataStoreFactory
    |               +-- HttpClientEngineFactory (OkHttp)
    |               +-- DatabaseFactory<AppDatabase>
    |               +-- FileReaderFactory
    |               +-- FileWriterFactory
    +-- setContent { App() }
```

## Adding a New Platform Binding

1. Define a factory interface in `commonMain`:
   ```kotlin
   interface NewServiceFactory {
       fun create(): NewService
   }
   ```
2. Add the Android binding in `PlatformModule.android.kt`:
   ```kotlin
   single<NewServiceFactory> {
       object : NewServiceFactory {
           override fun create(): NewService = AndroidNewService(get())
       }
   }
   ```
3. Add the corresponding iOS binding in `PlatformModule.ios.kt`
4. Inject via Koin in shared code: `val factory: NewServiceFactory by inject()`
