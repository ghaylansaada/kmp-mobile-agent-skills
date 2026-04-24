# Integration: Paging Cross-Skill Connections

## Upstream Dependencies

### kmp-database (Room Database)

| Provides | Consumed by Paging |
|---|---|
| AppDatabase class | Passed to BaseRemoteMediator constructor |
| @Entity classes | Must implement PageableEntity interface |
| @Dao interfaces | Must expose `pagingSource(): PagingSource<Int, Entity>` |
| @Upsert / @Query("DELETE") | Called by mediator's saveResult() and clearCachedDataAfterRefresh() |
| RemoteKeyEntity + RemoteKeyDao | Managed entirely by BaseRemoteMediator |
| useWriterConnection / immediateTransaction | Atomic multi-table writes in the mediator |
| room-paging artifact | Bridges Room @Query return type to PagingSource |

Integration points:
- Every pageable entity must be added to AppDatabase's `@Database(entities = [...])` array
- RemoteKeyEntity must be in the entities array
- RemoteKeyDao must be exposed as an abstract property on AppDatabase
- The DAO's `pagingSource()` return type `PagingSource<Int, Entity>` requires room-paging

### kmp-networking (Ktorfit / API Layer)

| Provides | Consumed by Paging |
|---|---|
| Ktorfit service interfaces | Called in networkResponse() |
| ApiResult sealed class | Return type of networkResponse(); success/error branching in load() |
| ApiResult.Error subtypes | Wrapped in ApiCallException for MediatorResult.Error |
| ApiCallException | Surfaces HTTP errors through Paging's LoadState.Error |
| ApiResult.Error.InternetError | Checked in UI for "No internet" message |

The Ktorfit service must have a paginated endpoint returning `ApiResult<List<Entity>>` accepting `page: Int` and `limit: Int` parameters.

### kmp-dependency-injection (Koin)

| Provides | Consumed by Paging |
|---|---|
| AppDatabase singleton | Injected into repository, passed to mediator |
| Ktorfit service singletons | Injected into repository, passed to mediator |
| Repository bindings | e.g., AccountRepositoryImpl bound to AccountRepository |
| ViewModel factory | e.g., AccountViewModel registered for Koin injection |

Typical Koin wiring:

```kotlin
// In repository module
single<AccountRepository> {
    AccountRepositoryImpl(
        authService = get(),
        database = get(),
    )
}

// In viewModel module
viewModel {
    AccountViewModel(accountRepository = get())
}
```

The RemoteMediator is typically not registered in Koin. It is instantiated inside the repository's `pagingFlow()` method because it is tightly coupled to the specific Pager instance.

## Downstream Consumers

### Compose UI

| Paging Provides | Consumed by UI |
|---|---|
| `Flow<PagingData<Entity>>` from ViewModel | collectAsLazyPagingItems() in Composable |
| PagingState enum | when expression for loading/error/empty/content states |
| toPaginationUiState() extension | Called on LazyPagingItems for state mapping |
| LazyPagingItems.refresh() | Pull-to-refresh or retry buttons |
| LazyPagingItems.retry() | Error retry buttons (retries failed APPEND only) |

### Architecture Patterns

| Pattern | How Paging Implements It |
|---|---|
| Offline-first | Room is single source of truth; network writes to Room, UI reads from Room |
| Unidirectional data flow | Network -> Room -> PagingSource -> Pager -> ViewModel -> Compose |
| Repository pattern | pagingFlow() hides Pager creation behind the repository interface |
| ViewModel scoping | cachedIn(viewModelScope) ties paging lifecycle to ViewModel |
| Atomic transactions | useWriterConnection + immediateTransaction for data consistency |

## Cross-Skill File Dependencies

| File | Depends on |
|---|---|
| BaseRemoteMediator | AppDatabase (kmp-database), ApiResult (kmp-networking), ApiCallException (kmp-networking) |
| AccountRemoteMediator | AuthService (kmp-networking), AccountDao (kmp-database), AppDatabase (kmp-database) |
| AccountRepositoryImpl | AuthService (kmp-networking), AppDatabase (kmp-database), Koin wiring (kmp-dependency-injection) |
| AccountViewModel | AccountRepository, Koin injection (kmp-dependency-injection) |
| AccountScreen | AccountViewModel, Compose components, Koin koinInject (kmp-dependency-injection) |
| PagingExt | PagingSource from Room (kmp-database) |
