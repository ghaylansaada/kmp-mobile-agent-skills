# BaseRemoteMediator

## RemoteKeyEntity

```kotlin
package {your.package}.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "remote_keys")
data class RemoteKeyEntity(
    @PrimaryKey val id: String,
    val type: String,
    val prevKey: Int?,
    val nextKey: Int?,
)
```

## RemoteKeyDao

```kotlin
package {your.package}.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import {your.package}.data.local.entity.RemoteKeyEntity

@Dao
interface RemoteKeyDao {
    @Upsert
    suspend fun upsert(remoteKeys: List<RemoteKeyEntity>)

    @Query("SELECT * FROM remote_keys WHERE id = :id AND type = :type")
    suspend fun selectById(id: String, type: String): RemoteKeyEntity?

    @Query("DELETE FROM remote_keys WHERE type = :type")
    suspend fun deleteByType(type: String)
}
```

## BaseRemoteMediator

```kotlin
package {your.package}.core.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import {your.package}.core.network.ApiCallException
import {your.package}.core.network.result.ApiResult
import {your.package}.data.local.AppDatabase

@OptIn(ExperimentalPagingApi::class)
abstract class BaseRemoteMediator<Type : PageableEntity>(
    private val database: AppDatabase,
    private val type: String,
) : RemoteMediator<Int, Type>() {

    override suspend fun initialize() = InitializeAction.LAUNCH_INITIAL_REFRESH

    @Suppress("UNCHECKED_CAST")
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, Type>,
    ): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> {
                remoteKeyClosest(state)?.nextKey?.minus(1) ?: 0
            }
            LoadType.PREPEND -> {
                val key = remoteKeyForFirst(state)
                key?.prevKey
                    ?: return MediatorResult.Success(endOfPaginationReached = key != null)
            }
            LoadType.APPEND -> {
                val key = remoteKeyForLast(state)
                key?.nextKey
                    ?: return MediatorResult.Success(endOfPaginationReached = key != null)
            }
        }

        val apiResponse = networkResponse(page, state)

        return if (apiResponse is ApiResult.Success<*>) {
            val result = (apiResponse.data as? List<Type>) ?: emptyList()
            val endReached = result.isEmpty()

            database.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    if (loadType == LoadType.REFRESH) {
                        database.remoteKeyDao.deleteByType(type)
                        clearCachedDataAfterRefresh()
                    }
                    val prevKey = if (page == 0) null else page - 1
                    val nextKey = if (endReached) null else page + 1
                    database.remoteKeyDao.upsert(
                        result.map {
                            RemoteKeyEntity(it.id, type, prevKey, nextKey)
                        },
                    )
                    saveResult(result)
                }
            }
            MediatorResult.Success(endOfPaginationReached = endReached)
        } else {
            val error = apiResponse as? ApiResult.Error
                ?: return MediatorResult.Error(Exception("Unknown error"))
            MediatorResult.Error(
                ApiCallException(error, error.message, apiResponse.exception),
            )
        }
    }

    private suspend fun remoteKeyForLast(state: PagingState<Int, Type>) =
        state.pages.lastOrNull { it.data.isNotEmpty() }
            ?.data?.lastOrNull()
            ?.let { database.remoteKeyDao.selectById(it.id, type) }

    private suspend fun remoteKeyForFirst(state: PagingState<Int, Type>) =
        state.pages.firstOrNull { it.data.isNotEmpty() }
            ?.data?.firstOrNull()
            ?.let { database.remoteKeyDao.selectById(it.id, type) }

    private suspend fun remoteKeyClosest(state: PagingState<Int, Type>) =
        state.anchorPosition?.let { pos ->
            state.closestItemToPosition(pos)?.id
                ?.let { database.remoteKeyDao.selectById(it, type) }
        }

    abstract suspend fun networkResponse(
        page: Int,
        state: PagingState<Int, Type>,
    ): ApiResult<List<Type>>

    abstract suspend fun clearCachedDataAfterRefresh()

    abstract suspend fun saveResult(newData: List<Type>)
}
```

## LoadType Behavior

| LoadType | Behavior |
|---|---|
| REFRESH | Finds remote key closest to scroll position. Falls back to page 0. Deletes keys and cached data before inserting. |
| PREPEND | Looks up key for first visible item. Returns endOfPaginationReached if prevKey is null. |
| APPEND | Looks up key for last visible item. Returns endOfPaginationReached if nextKey is null. |
