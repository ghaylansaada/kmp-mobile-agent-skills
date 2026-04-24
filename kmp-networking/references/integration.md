# Integration: Networking Connections

## Upstream Dependencies

### kmp-session-management

- `SessionManager` is injected into `HttpClientFactory.createAuthorizedClient()` for token loading, refresh, and revocation.
- `SessionManager.getTokens()` is called in the Auth plugin's `loadTokens` block before every request.
- `SessionManager.setTokens()` persists new tokens after a successful refresh.
- `SessionManager.revokeSession()` is called when refresh fails, triggering logout.

### kmp-dependency-injection

- `ktorfitModule()` uses Koin `module {}` DSL to register HttpClient instances, Ktorfit, and services.
- Named qualifiers (`refreshHttpClient`, `authorizedHttpClient`) distinguish the two HttpClient singletons.
- `HttpClientEngineFactory` is resolved from platform modules via `get<HttpClientEngineFactory>()`.
- Each Ktorfit service is registered as `single<XxxService> { get<Ktorfit>().create() }`.

## Downstream Consumers

### kmp-database (repositories)

- Repositories consume `ApiResult` from Ktorfit services and persist data locally via Room DAOs.
- Inside `ApiCall.onSuccess()`, the repository upserts fetched data into the database.
- `ApiResult.Error` is handled in `onFailure()` for logging or UI notification.

### kmp-paging

- `BaseRemoteMediator` calls Ktorfit services that return `ApiResult<List<T>>`.
- `ApiResultPaging.currentPage` and `ApiResultPaging.totalPages` determine whether more pages exist.

### kmp-transfer

- Uses the authorized `HttpClient` for HTTP operations with Range and Content-Range headers.

### Error handling

- `ApiResult.Error.InternetError` maps to "No internet connection" UI state.
- `ApiResult.Error.HttpError` with status 401 triggers session expiry handling.
- `ApiResult.Error.HttpError` with `errors: List<ApiError>` provides field-level validation feedback.
- `ApiResult.Error.ParsingError` maps to a generic error message.

## Module Load Order

Koin modules must be loaded in this order:

1. Platform module (provides `HttpClientEngineFactory`)
2. Local storage module (provides `DataStore<Preferences>`, `AppDatabase`)
3. Session module (provides `SessionManager`)
4. `ktorfitModule()` (consumes all above, provides HttpClients, Ktorfit, services)
5. Repository modules (consume services)
6. ViewModel modules (consume repositories)

## Error Propagation Path

```
Ktorfit Service --> KtorfitFactory.CustomConverter --> ApiResult
     |                                                      |
     v                                                      v
ApiCall.await() --> onSuccess() / onFailure() --> Repository
     |                                                      |
     v                                                      v
ViewModel --> UiState --> Compose UI
```

## Token Refresh Sequence

```
Request with expired token --> 401 response
     |
     v
Auth plugin calls refreshTokens block
     |
     v
Mutex.withLock (double-check)
     |
     +-- Token already refreshed? --> Return existing BearerTokens
     |
     +-- Need refresh? --> refreshClient.post(REFRESH_AUTH_PATH)
              |
              +-- Success --> SessionManager.setTokens() --> Return new BearerTokens
              |
              +-- Failure --> SessionManager.revokeSession() --> Return null (logout)
```
