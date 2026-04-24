# Platform Database Builders and DI Wiring

## Android Builder

**File:** `androidMain/.../core/database/Database.android.kt`

```kotlin
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

## iOS Builder

**File:** `iosMain/.../core/database/Database.ios.kt`

```kotlin
package {your.package}.core.database

import androidx.room.Room
import androidx.room.RoomDatabase
import {your.package}.data.local.AppDatabase
import {your.package}.data.local.AppDatabaseConstructor
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFilePath = documentDirectory() + "/app.db"
    return Room.databaseBuilder<AppDatabase>(
        name = dbFilePath,
        factory = AppDatabaseConstructor::initialize,
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(documentDirectory?.path)
}
```

## LocalStorageModule (commonMain)

**File:** `commonMain/.../di/modules/LocalStorageModule.kt`

```kotlin
package {your.package}.di.modules

import {your.package}.core.database.DatabaseFactory
import {your.package}.data.local.AppDatabase
import org.koin.dsl.module

fun localStorageModule() = module {
    single<AppDatabase> { get<DatabaseFactory<AppDatabase>>().create() }
}
```

## Android Platform Module (database portion)

```kotlin
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import {your.package}.core.database.DatabaseFactory
import {your.package}.core.database.getDatabaseBuilder
import {your.package}.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers

single<DatabaseFactory<AppDatabase>> {
    object : DatabaseFactory<AppDatabase> {
        override fun create(): AppDatabase = getDatabaseBuilder(context = get())
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }
}
```

## iOS Platform Module (database portion)

```kotlin
import {your.package}.core.database.DatabaseFactory
import {your.package}.core.database.getDatabaseBuilder
import {your.package}.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers

single<DatabaseFactory<AppDatabase>> {
    object : DatabaseFactory<AppDatabase> {
        override fun create(): AppDatabase = getDatabaseBuilder()
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
    }
}
```

## Platform Differences

| Aspect | Android | iOS |
|--------|---------|-----|
| SQLite driver | `BundledSQLiteDriver()` | System SQLite (`-lsqlite3`) |
| Coroutine context | `Dispatchers.IO` | `Dispatchers.Default` |
| Builder parameter | `context: Context` | None |
| Database path | `getDatabasePath("app.db")` | `NSDocumentDirectory + "/app.db"` |
