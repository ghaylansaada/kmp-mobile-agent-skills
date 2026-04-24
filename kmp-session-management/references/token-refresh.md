# Token Refresh: Ktor Auth + Mutex Pattern

## HttpClientFactory -- Authorized Client

**File:** `commonMain/kotlin/{your.package}/core/network/HttpClientFactory.kt`

The authorized client installs the Ktor `Auth` plugin with a `bearer` block
containing three callbacks:

- **`loadTokens`** -- reads tokens from `SessionManager.getTokens()` (synchronous
  keystore read). Called once per client lifecycle on first request.
- **`refreshTokens`** -- called when a 401 is received. Uses Mutex
  double-check pattern to serialize concurrent refresh attempts.
- **`sendWithoutRequest`** -- gate that returns `false` for the refresh
  endpoint path to prevent the refresh request itself from carrying an
  expired Bearer token (which would cause infinite 401 loops).

## Mutex Double-Check Pattern

1. Capture `oldToken = oldTokens?.accessToken` (the stale token that triggered
   the 401).
2. Acquire `refreshMutex.withLock`.
3. Re-read tokens inside the lock via `sessionManager.getTokens()`.
4. If the access token differs from `oldToken` (another coroutine already
   refreshed it), return a **new** `BearerTokens` instance and exit.
5. Otherwise, call the refresh endpoint via `refreshClient`.
6. On success, persist new tokens via `sessionManager.setTokens()` and
   return a **new** `BearerTokens`.
7. On failure, call `sessionManager.expireSession()` and return `null`.

The double-check comparison must be against `oldToken` (the stale value),
not a simple `isNullOrBlank` check. Checking only non-blank fails when a
concurrent coroutine sets new tokens between lock acquisition and the check.

## sendWithoutRequest Guard

The guard must exclude the refresh endpoint:

```
sendWithoutRequest { request ->
    !request.url.encodedPath.endsWith(
        AppConfig.REFRESH_AUTH_PATH.trimStart('/')
    )
}
```

If this guard returns `true` for the refresh path, the refresh request
carries the expired token, gets a 401, and triggers another refresh --
infinite loop.

## Refresh Client

The refresh client (`QUALIFIER_NAME_REFRESH_CLIENT`) deliberately omits
the `Auth` plugin. It only has `ContentNegotiation`, `Logging`, and
`defaultRequest`. This prevents recursive refresh loops.

---

## Refresh Auth DTOs

### RefreshAuthRequestDto

**File:** `commonMain/kotlin/{your.package}/data/remote/dto/refreshauth/RefreshAuthRequestDto.kt`

- Single field: `refreshToken` serialized as `"refresh_token"`.
- Annotated with `@Serializable`.

### RefreshAuthResponseDto

**File:** `commonMain/kotlin/{your.package}/data/remote/dto/refreshauth/RefreshAuthResponseDto.kt`

- Two fields: `accessToken` (`"access_token"`), `refreshToken` (`"refresh_token"`).
- Annotated with `@Serializable`.
- Server should return a new refresh token on every refresh call (token rotation).

---

## BearerTokens Reference Equality Trap

The Ktor Auth plugin compares the `BearerTokens` returned by `loadTokens`
with the one from `refreshTokens` using reference equality. If you cache
and return the same object instance, the plugin considers the refresh a
no-op and stops retrying. Always construct a **new** `BearerTokens` in
both the double-check early return and the post-refresh return.

## Token Rotation

The refresh endpoint should return a new refresh token alongside the new
access token. The old refresh token is invalidated server-side. This limits
the window of vulnerability if a refresh token is compromised. Always persist
both the new access token and new refresh token via `sessionManager.setTokens()`.
