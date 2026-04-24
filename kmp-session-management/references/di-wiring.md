# DI Wiring: Session + Ktorfit Modules

## sessionModule

**File:** `commonMain/kotlin/{your.package}/di/modules/SessionModule.kt`

Registers `SecureTokenStorage` and `SessionManager` as Koin singletons.
On Android, `SecureTokenStorage` receives `Context` from the graph. On iOS,
no additional dependency is needed.

`sessionModule()` **must** be registered before `ktorfitModule()` because the
authorized HttpClient resolves `SessionManager` during construction. If the
order is reversed, Koin throws a missing-definition error at runtime.

## ktorfitModule

**File:** `commonMain/kotlin/{your.package}/di/modules/KtorfitModule.kt`

Registers three singletons:

| Qualifier | Type | Purpose |
|-----------|------|---------|
| `QUALIFIER_NAME_REFRESH_CLIENT` | `HttpClient` | Auth-free client for token refresh |
| `QUALIFIER_NAME_AUTHORIZATION_CLIENT` | `HttpClient` | Authorized client with Bearer Auth |
| _(none)_ | `Ktorfit` | Ktorfit instance backed by authorized client |

The authorized client receives `SessionManager` from the graph and the
refresh client via `get(named(QUALIFIER_NAME_REFRESH_CLIENT))`.

## Module Registration Order

```
startKoin {
    modules(
        platformModule(context, config),
        localStorageModule(),    // DataStore, AppDatabase
        sessionModule(),         // SecureTokenStorage + SessionManager -- before ktorfitModule
        ktorfitModule(),         // HttpClients (depends on SessionManager)
        repositoryModule(),
        viewModelModule(),
    )
}
```

Ordering constraints:
- `platformModule` first (provides Android `Context` for SecureTokenStorage)
- `localStorageModule` before `sessionModule` (provides AppDatabase)
- `sessionModule` before `ktorfitModule` (provides SessionManager)
- `ktorfitModule` before `repositoryModule` (provides Ktorfit)
