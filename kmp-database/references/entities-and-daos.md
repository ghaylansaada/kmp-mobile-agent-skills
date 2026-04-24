# Entities and DAOs

## Entity Pattern

```kotlin
package {your.package}.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import {your.package}.core.paging.PageableEntity

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey
    override val id: String,
    val name: String,
) : PageableEntity
```

## PageableEntity Interface

For entities that support paging:

```kotlin
package {your.package}.core.paging

interface PageableEntity {
    val id: String
}
```

## RemoteKeyEntity

Used by `BaseRemoteMediator` for pagination cursors. Canonical definition in
`kmp-paging/references/base-remote-mediator.md`. Must be included in `@Database(entities = [...])`.

## DAO Pattern

```kotlin
package {your.package}.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import {your.package}.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts LIMIT 1")
    fun selectAccount(): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts ORDER BY id ASC")
    fun pagingSource(): PagingSource<Int, AccountEntity>

    @Upsert
    suspend fun upsertAll(accounts: List<AccountEntity>)

    @Query("DELETE FROM accounts")
    suspend fun clear()
}
```

## RemoteKeyDao

Canonical definition in `kmp-paging/references/base-remote-mediator.md`. Add
`abstract val remoteKeyDao: RemoteKeyDao` to `AppDatabase`.

## Adding a New Entity

1. Create entity class in `data/local/entity/`
2. Create DAO interface in `data/local/dao/`
3. Add entity to `@Database(entities = [...])` in AppDatabase
4. Add abstract DAO property in AppDatabase
5. Bump database version and add migration spec
