---
name: kmp-session-management
description: >
  Implement secure session management in a Kotlin Multiplatform application. Covers
  sealed interface SessionState for authentication state tracking, expect/actual
  SecureTokenStorage for platform keystore persistence (Android Keystore / iOS
  Keychain), automatic Bearer token refresh with Ktor Auth and Mutex double-check
  pattern, clean logout clearing both secure storage and Room database, and reactive
  session observation via StateFlow. Use this skill when implementing login/logout
  flows, wiring token refresh into the HTTP client, observing authentication state
  for navigation guards, debugging token refresh races or 401 loops, or clearing
  user data on session revocation.
compatibility: >
  KMP with Compose Multiplatform. Requires Ktor, kotlinx.coroutines, and Koin.
  Platform targets: Android (EncryptedSharedPreferences via AndroidX Security),
  iOS (Keychain via expect/actual).
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---

# KMP Session Management

## When to use

- Implementing login flow with secure token persistence
- Implementing logout flow that clears all user data (secure storage + Room)
- Wiring automatic Bearer token refresh into the Ktor HTTP client
- Observing authentication state reactively for navigation guards
- Debugging token refresh races between concurrent coroutines
- Debugging infinite 401 loops during token refresh
- Adding two-factor authentication state tracking
- Restoring session state after app restart from persisted tokens

## Depends on

- **kmp-architecture** -- Gradle and source set structure
- **kmp-networking** -- HttpClientFactory, refresh client, Auth plugin configuration
- **kmp-database** -- AppDatabase with clearAllTables for logout
- **kmp-datastore** -- DataStore Preferences for non-sensitive user preferences
- **kmp-dependency-injection** -- Koin sessionModule, module ordering

## Workflow

1. Verify dependencies are in place --> [setup.md](references/setup.md)
2. Create SessionState sealed interface (Unauthenticated, Authenticated, Expired, AwaitingSecondFactor) --> [session-state.md](references/session-state.md)
3. Create expect/actual SecureTokenStorage for platform keystore --> [secure-storage.md](references/secure-storage.md)
4. Create SessionManager with SecureTokenStorage and StateFlow for state --> [session-manager.kt.template](assets/templates/session-manager.kt.template)
5. Wire token loading and refresh into HttpClientFactory with Mutex double-check pattern --> [token-refresh.md](references/token-refresh.md)
6. Create Refresh Auth DTOs (request and response) --> [token-refresh.md](references/token-refresh.md)
7. Create sessionModule in Koin and register before ktorfitModule --> [di-wiring.md](references/di-wiring.md)
8. Connect to navigation guards, database clearing, and HTTP client --> [integration.md](references/integration.md)

## Gotchas

1. **Token refresh race -- N coroutines must share a single refresh.** When multiple API calls receive 401 simultaneously, N coroutines all enter the refreshTokens block. Without the Mutex double-check pattern, all N independently call the refresh endpoint, wasting requests and potentially invalidating tokens. The Mutex serializes them; the double-check ensures only the first actually refreshes while the rest reuse the result.
2. **DataStore reads are cold Flows -- session not available at startup.** The non-sensitive preferences are backed by a cold DataStore Flow. On cold start, the first DataStore emission triggers a disk read. If the disk read is slow (first launch, cold file cache), the first API request fires without tokens, receiving a 401 that triggers an unnecessary refresh. Pre-load tokens from SecureTokenStorage during app initialization.
3. **BearerTokens reference comparison.** The Ktor Auth plugin compares BearerTokens from loadTokens and refreshTokens using reference equality. If you return the same cached instance, the plugin considers the refresh a no-op and stops retrying. Always construct a new BearerTokens object in refreshTokens.
4. **Mutex double-check must compare the stale token, not just check non-blank.** Inside the Mutex lock, re-read tokens and verify the access token differs from the one that triggered the 401. Checking only isNullOrBlank fails when a revoked session clears tokens to empty and a concurrent coroutine sets new tokens between the lock acquisition and the check.
5. **sendWithoutRequest loop guard.** The sendWithoutRequest lambda controls which requests get Bearer headers. If it returns true for the refresh endpoint, the refresh request itself carries the expired token, gets a 401, triggers another refresh, and loops infinitely. The guard must return false for the refresh path.
6. **Logout ordering matters.** revokeSession clears secure storage first, then Room tables. If clearAllTables fails, tokens are already gone, preventing authenticated requests with stale cached data. Reversing the order risks a crash that leaves valid tokens in secure storage while Room data is partially cleared.
7. **MutableStateFlow vs SecureTokenStorage.** The sessionState uses MutableStateFlow (hot, in-memory) while token reads come from platform keystore (synchronous on Android, async on iOS). After setTokens, sessionState updates instantly, but a getTokens call immediately after may read stale values if the keystore write has not flushed. Use the in-memory StateFlow value as the source of truth for navigation; use getTokens only for HTTP header injection.
8. **Tokens must never appear in logs.** Ktor Logging plugin at LogLevel.ALL or LogLevel.HEADERS prints Authorization headers. Use LogLevel.INFO or a custom log sanitizer. Similarly, never log token values in SessionManager methods.
9. **Expired state must be distinct from Unauthenticated.** SessionState.Expired signals the UI to show a "session expired" message and redirect to login, while Unauthenticated means the user has not logged in yet. Collapsing these into one state loses context for the user experience.

## Assets

| Path | Load when... |
|------|-------------|
| [setup.md](references/setup.md) | Dependencies, build.gradle.kts config, directory structure |
| [session-state.md](references/session-state.md) | SessionState sealed interface with all states |
| [secure-storage.md](references/secure-storage.md) | expect/actual SecureTokenStorage for platform keystore |
| [token-refresh.md](references/token-refresh.md) | HttpClientFactory Auth wiring, Mutex pattern, DTOs, refresh client |
| [di-wiring.md](references/di-wiring.md) | sessionModule, ktorfitModule, Koin registration order |
| [integration.md](references/integration.md) | Upstream/downstream connections, logout flow, thread safety |
| [session-manager.kt.template](assets/templates/session-manager.kt.template) | SessionManager class with SecureTokenStorage and database dependencies |
| [auth-flow.kt](assets/snippets/auth-flow.kt) | Complete auth flow: SessionState, SessionManager, SecureTokenStorage, HttpClient integration |

## Validation

### A. Session management correctness
- [ ] `sealed interface` used for SessionState with Unauthenticated, Authenticated, Expired, and AwaitingSecondFactor
- [ ] Tokens stored in platform keystore via expect/actual SecureTokenStorage (not plain DataStore Preferences)
- [ ] Token refresh uses Mutex double-check pattern to prevent concurrent refreshes
- [ ] Mutex double-check compares stale token against refreshed token (not just non-blank check)
- [ ] Session expiry handled gracefully with distinct Expired state and UI feedback
- [ ] Logout clears all sensitive data: secure storage wiped, Room tables cleared, StateFlow set to Unauthenticated
- [ ] `sendWithoutRequest` returns false for the refresh endpoint path
- [ ] Cold-start token loading completes before first API request
- [ ] `sessionState` exposed as `StateFlow<SessionState>` (not raw `Flow`)
- [ ] `value class` wrappers used for AccessToken and RefreshToken to prevent accidental swapping

### B. Security
- [ ] Tokens persisted in Android Keystore / iOS Keychain (not plain SharedPreferences or DataStore)
- [ ] Token rotation: new refresh token replaces old on every refresh call
- [ ] No tokens logged at any log level (Ktor LogLevel.INFO or lower, no println of token values)
- [ ] No secrets, API keys, or hardcoded credentials in any file
- [ ] Refresh client has no Auth plugin installed (prevents recursive refresh with expired token)

### C. Performance
- [ ] SessionManager registered as Koin `single` (not `factory`)
- [ ] StateFlow used for session state observation (no redundant Flow allocations)
- [ ] Keystore reads cached in memory after initial load (avoid repeated I/O on every API call)

### D. Integration
- [ ] Depends-on references match actual skill directory names
- [ ] Module load order: platform -> storage -> session -> ktorfit -> repositories
- [ ] Named qualifiers consistent between registration and resolution
- [ ] Downstream consumers (navigation guards, logout flow) documented
- [ ] Template placeholders use `{your.package}` consistently
