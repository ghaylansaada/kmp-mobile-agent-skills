# Paging Types

## PageableEntity

Every pageable Room entity must implement this:

```kotlin
package {your.package}.core.paging

interface PageableEntity {
    val id: String
}
```

## PagingState Enum

```kotlin
package {your.package}.core.paging

enum class PagingState {
    InitialLoading,
    InitialError,
    Content,
    Empty,
    Appending,
    AppendError,
    EndOfPagination,
}
```

## PagingExt

```kotlin
package {your.package}.core.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.RemoteMediator
import androidx.paging.compose.LazyPagingItems

private const val NETWORK_PAGE_SIZE: Int = 10

@ExperimentalPagingApi
fun <Value : Any> createPager(
    pagingSourceFactory: () -> PagingSource<Int, Value>,
    remoteMediator: RemoteMediator<Int, Value>? = null,
    pageSize: Int = NETWORK_PAGE_SIZE,
): Pager<Int, Value> = Pager(
    config = PagingConfig(
        pageSize = pageSize,
        enablePlaceholders = true,
        initialLoadSize = pageSize,
        prefetchDistance = 2,
    ),
    remoteMediator = remoteMediator,
    pagingSourceFactory = pagingSourceFactory,
)

fun LazyPagingItems<*>.toPaginationUiState(): PagingState {
    val refresh = loadState.refresh
    val append = loadState.append
    return when {
        refresh is LoadState.Loading -> PagingState.InitialLoading
        refresh is LoadState.Error -> PagingState.InitialError
        itemCount == 0 && refresh is LoadState.NotLoading -> PagingState.Empty
        append is LoadState.Loading -> PagingState.Appending
        append is LoadState.Error -> PagingState.AppendError
        append.endOfPaginationReached -> PagingState.EndOfPagination
        else -> PagingState.Content
    }
}
```

## PagingConfig Rationale

| Parameter | Value | Rationale |
|---|---|---|
| pageSize | 10 | Keeps payloads small |
| enablePlaceholders | true | Stable item count for LazyColumn; avoids layout jumps |
| initialLoadSize | pageSize | Avoids oversized first load |
| prefetchDistance | 2 | Loads next page when 2 items from end |

## Why `createPager` is a top-level function

The factory is a `() -> PagingSource` lambda -- not an extension on `PagingSource`. This
guarantees a new PagingSource instance on every call, which is required because Room
PagingSource is single-use and becomes invalid after the first data change.
