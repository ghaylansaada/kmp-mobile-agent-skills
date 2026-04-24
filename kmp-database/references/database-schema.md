# Database Schema: AppDatabase Core

## AppDatabase

**File:** `commonMain/.../data/local/AppDatabase.kt`

```kotlin
package {your.package}.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.execSQL
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import {your.package}.data.local.dao.AccountDao
import {your.package}.data.local.dao.RemoteKeyDao
import {your.package}.data.local.entity.AccountEntity
import {your.package}.data.local.entity.RemoteKeyEntity

@ConstructedBy(AppDatabaseConstructor::class)
@TypeConverters(AppDatabaseConverters::class)
@Database(
    version = 1,
    exportSchema = true,
    entities = [
        AccountEntity::class,
        RemoteKeyEntity::class,
    ],
)
abstract class AppDatabase : RoomDatabase() {
    abstract val accountDao: AccountDao
    abstract val remoteKeyDao: RemoteKeyDao
}

suspend fun AppDatabase.clearAllTables() {
    useWriterConnection { transactor ->
        transactor.immediateTransaction {
            val tables = mutableListOf<String>()
            usePrepared(
                "SELECT name FROM sqlite_master WHERE type='table' AND name != 'room_master_table'",
            ) { stmt ->
                while (stmt.step()) tables.add(stmt.getText(0))
            }
            for (table in tables) execSQL("DELETE FROM `$table`")
        }
    }
}
```

## AppDatabaseConstructor

**File:** `commonMain/.../data/local/AppDatabaseConstructor.kt`

Room KSP generates the actual implementations. Never write manual `actual` declarations.

```kotlin
package {your.package}.data.local

import androidx.room.RoomDatabaseConstructor

expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
```

## AppDatabaseConverters

**File:** `commonMain/.../data/local/AppDatabaseConverters.kt`

Uses extension functions from `DatetimeExt.kt` (see `kmp-datetime`).

```kotlin
@file:OptIn(kotlin.time.ExperimentalTime::class)

package {your.package}.data.local

import androidx.room.TypeConverter
import {your.package}.ext.toEpochMilli
import {your.package}.ext.toInstant
import kotlin.time.Instant

class AppDatabaseConverters {

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.toInstant()

    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()
}
```

## DatabaseFactory Interface

**File:** `commonMain/.../core/database/DatabaseFactory.kt`

```kotlin
package {your.package}.core.database

import androidx.room.RoomDatabase

interface DatabaseFactory<T : RoomDatabase> {

    fun create(): T
}
```
