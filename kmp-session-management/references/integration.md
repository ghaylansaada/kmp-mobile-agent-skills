# Integration: Session Management Connections

## Upstream Dependencies

### kmp-networking

- `HttpClientFactory` receives a `SessionManager` instance to wire `loadTokens`,
  `refreshTokens`, and `sendWithoutRequest` in the Auth plugin.
- The refresh client (`QUALIFIER_NAME_REFRESH_CLIENT`) does NOT have the Auth
  plugin -- deliberately stripped to prevent recursive token refresh loops.
- `RefreshAuthRequestDto` and `RefreshAuthResponseDto` are used for the
  refresh endpoint call.

### kmp-database

- `AppDatabase` is injected into `SessionManager` for calling
  `clearAllTables()` during logout.
- Secure storage is cleared first, then Room tables. This ordering ensures that
  even if `clearAllTables()` fails, tokens are already gone.

### kmp-datastore

- `DataStore<Preferences>` is used for non-sensitive user preferences (userId,
  theme, onboarding state). Tokens are NOT stored in DataStore.
- Tokens are stored in `SecureTokenStorage` (platform keystore).

### kmp-dependency-injection

- `sessionModule()` registers `SecureTokenStorage` and `SessionManager` as
  Koin singletons.
- `sessionModule()` must be registered before `ktorfitModule()` because the
  authorized HttpClient resolves `SessionManager` during construction.
- See [di-wiring.md](di-wiring.md) for full module registration order.

## Downstream Consumers

### Navigation guards

- `SessionManager.sessionState` is a `StateFlow<SessionState>` that navigation
  guards observe to determine routing.
- Observe via `collectAsState()` in a top-level Composable, then use a `when`
  branch to route to the auth graph, main graph, expired screen, or 2FA graph.

## Logout Flow

```
User taps "Logout"
  --> ViewModel calls sessionManager.revokeSession()
  --> secureTokenStorage.clear()
      Clears: accessToken, refreshToken from platform keystore
  --> database.clearAllTables()
      Wipes all Room entity tables
  --> _sessionState.value = SessionState.Unauthenticated
      All collectors receive Unauthenticated
  --> App-level Composable observes state change
      Navigates to login
```

## Session Expiry Flow

```
Token refresh fails (401 from refresh endpoint)
  --> HttpClientFactory calls sessionManager.expireSession()
  --> secureTokenStorage.clear()
  --> database.clearAllTables()
  --> _sessionState.value = SessionState.Expired
      All collectors receive Expired
  --> App-level Composable shows "Session expired" banner + login
```

## Thread Safety

- `SessionManager._sessionState` uses `MutableStateFlow` (thread-safe).
- `SecureTokenStorage` on Android uses `EncryptedSharedPreferences` (internally
  synchronized). On iOS, Keychain operations are thread-safe.
- Token refresh uses `Mutex` in `HttpClientFactory` to serialize concurrent
  refresh attempts.
- `database.clearAllTables()` is a suspend function called from a coroutine
  scope -- no main-thread blocking.
