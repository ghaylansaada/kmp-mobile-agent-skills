# Vertical Slice: Account Paging

## Entity

```kotlin
package {your.package}.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import {your.package}.core.paging.PageableEntity

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey override val id: String,
    val name: String,
) : PageableEntity
```

Must be registered in `AppDatabase`'s `@Database(entities = [...])` array.

## DAO

```kotlin
package {your.package}.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import {your.package}.data.local.entity.AccountEntity

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

`ORDER BY` ensures deterministic page ordering. `clear()` is called by the mediator during REFRESH.

## RemoteMediator

```kotlin
package {your.package}.data.pagination

import androidx.paging.PagingState
import {your.package}.core.network.result.ApiResult
import {your.package}.core.paging.BaseRemoteMediator
import {your.package}.data.local.AppDatabase
import {your.package}.data.local.entity.AccountEntity
import {your.package}.data.remote.service.AuthService

class AccountRemoteMediator(
    private val service: AuthService,
    private val database: AppDatabase,
) : BaseRemoteMediator<AccountEntity>(database, "accounts") {

    override suspend fun networkResponse(
        page: Int,
        state: PagingState<Int, AccountEntity>,
    ) = service.test(page = page, limit = state.config.pageSize)

    override suspend fun clearCachedDataAfterRefresh() {
        database.accountDao.clear()
    }

    override suspend fun saveResult(newData: List<AccountEntity>) {
        database.accountDao.upsertAll(newData)
    }
}
```

Each new feature needs: unique type string, `networkResponse()`, `clearCachedDataAfterRefresh()`, `saveResult()`.

## Repository

```kotlin
package {your.package}.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import {your.package}.core.paging.createPager
import {your.package}.data.local.AppDatabase
import {your.package}.data.local.entity.AccountEntity
import {your.package}.data.pagination.AccountRemoteMediator
import {your.package}.data.remote.service.AuthService

interface AccountRepository {
    fun pagingFlow(): Flow<PagingData<AccountEntity>>
}

class AccountRepositoryImpl(
    private val authService: AuthService,
    private val database: AppDatabase,
) : AccountRepository {
    @OptIn(ExperimentalPagingApi::class)
    override fun pagingFlow(): Flow<PagingData<AccountEntity>> =
        createPager(
            pagingSourceFactory = { database.accountDao.pagingSource() },
            remoteMediator = AccountRemoteMediator(
                service = authService,
                database = database,
            ),
        ).flow
}
```

## ViewModel

```kotlin
package {your.package}.presentation.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.Flow
import {your.package}.data.local.entity.AccountEntity
import {your.package}.data.repository.AccountRepository

class AccountViewModel(
    private val accountRepository: AccountRepository,
) : ViewModel() {
    val pagingFlow: Flow<PagingData<AccountEntity>> =
        accountRepository.pagingFlow().cachedIn(viewModelScope)
}
```

`cachedIn(viewModelScope)` caches paging data across configuration changes. Declared as a `val` -- Compose collects it.
