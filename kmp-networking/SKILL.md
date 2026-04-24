---
name: kmp-networking
description: >
  Ktor + Ktorfit networking stack for KMP -- HttpClient with Bearer auth and
  Mutex-guarded token refresh, KtorfitFactory with ApiResult converter, sealed
  ApiResult error types with user-facing message mapping and retry strategies,
  ApiCall repository pattern, ApiCallException bridge for paging, error display
  components for Compose, pagination header extraction, domain error template,
  and platform engines (OkHttp/Darwin). Use when adding REST API endpoints,
  consuming API responses in repositories, handling API errors, mapping errors
  to user messages, implementing retry logic, customizing HTTP client behavior,
  debugging auth or token refresh issues, or adding pagination.
compatibility: >
  KMP with Compose Multiplatform. Requires Ktor, Ktorfit, kotlinx.serialization,
  and Koin.
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---

# KMP Networking

## When to use

- Adding a new REST API endpoint via a Ktorfit service interface
- Consuming API responses in a repository using the ApiCall abstraction
- Handling API errors by inspecting ApiResult.Error subtypes
- Implementing user-facing error messages for network failures
- Integrating paging with error propagation via ApiCallException
- Adding retry logic for transient errors
- Creating domain-specific error hierarchies that map from ApiResult.Error
- Debugging error mapping from HTTP responses through KtorfitFactory
- Handling form validation errors (422 with field-level ApiError list)
- Customizing HTTP client behavior (interceptors, headers, timeouts)
- Debugging authentication or token refresh issues (401 loops, stale tokens)
- Adding pagination support via response headers
- Configuring retry logic or timeout policies

## Depends on

- **kmp-architecture** -- Gradle and source set structure
- **kmp-session-management** -- SessionManager for token lifecycle in the Auth plugin
- **kmp-dependency-injection** -- Koin module wiring, named qualifiers for HttpClient instances
- **kmp-kotlin-coroutines** -- CancellationException handling, retry with delay

## Workflow

1. Verify Ktor, Ktorfit, and serialization artifacts --> [setup.md](references/setup.md)
2. Configure HttpClientFactory with auth and platform engines --> [http-client.md](references/http-client.md)
3. Implement ApiResult types and KtorfitFactory converter --> [api-types-and-converter.md](references/api-types-and-converter.md)
4. Define DTOs, service interfaces, and DI module --> [services-and-dtos.md](references/services-and-dtos.md)
5. Add error extensions, error propagation, and paging error integration --> [error-extensions.md](references/error-extensions.md)
6. Add error display components to screens --> [error-display-components.kt](assets/snippets/error-display-components.kt)
7. Wire upstream/downstream dependencies --> [integration.md](references/integration.md)

## Gotchas

1. **Ktor Auth 401 infinite loop.** If the refresh endpoint returns 401, the Auth plugin re-enters the refresh block indefinitely. `sendWithoutRequest` must return `false` for the refresh path. Missing this causes StackOverflowError or OOM.
2. **Mutex refresh deadlock with cancelled scope.** If the parent scope cancels while holding `refreshMutex`, waiting coroutines throw CancellationException. Never swallow CancellationException inside `refreshMutex.withLock`.
3. **BearerTokens compared by reference.** The Auth plugin compares old and new BearerTokens by reference. Returning the same object makes the plugin skip the retry. Always construct a new BearerTokens instance.
4. **`explicitNulls = false` is critical.** Without it, request bodies include `"field": null` for every nullable property, which many servers reject with 400 Bad Request.
5. **Darwin engine bound to ATS.** On iOS, plain HTTP is blocked by App Transport Security. No Ktor-level override exists -- add exceptions in Info.plist or the app crashes with a connection error on any non-HTTPS URL.
6. **OkHttp connection pool leaks.** Registering HttpClient as `factory` instead of `single` in Koin causes thread leaks from accumulated connection pools. Each factory call creates a new connection pool with its own thread, exhausting memory over time.
7. **Ktorfit KSP per-target.** Global `ksp(libs.ktorfit.ksp)` does not work. Must use `add("kspAndroid", ...)`, `add("kspIosArm64", ...)`, etc. Otherwise, generated code is silently missing and `.create()` throws at runtime.
8. **Missing timeout config causes indefinite hangs.** Ktor has no default request or socket timeout. A slow or unresponsive server blocks the coroutine forever. Always configure `HttpTimeout` with explicit connect, request, and socket values.
9. **No retry on transient failures by default.** Ktor does not retry failed requests. Without explicit `HttpRequestRetry` configuration, a single network blip fails the entire request instead of retrying with backoff.
10. **Hardcoded base URLs break environment switching.** Using a string literal for BASE_URL prevents switching between staging and production. Use BuildKonfig to inject the URL at build time from Gradle properties.
11. **`runCatching` catches `CancellationException`, breaking structured concurrency.** If you wrap a suspend call in `runCatching { suspendingWork() }.getOrNull()`, cancellation is swallowed and the coroutine continues when it should have been cancelled. Use the project's `suspendRunCatching` helper (which rethrows `CancellationException`) or limit `runCatching` to non-suspending code only. See **kmp-kotlin-coroutines** skill for the `suspendRunCatching` pattern.
12. **NSError bridging loses the Kotlin exception type.** When a Kotlin exception crosses to Swift, it becomes a generic `NSError`. Swift cannot catch specific subtypes. Design Swift-facing APIs to return sealed result types, not throw exceptions.
13. **`Result<T>` (stdlib) is invisible to Swift/Objective-C.** It is an inline class, so Swift sees the unwrapped `T` with no error info. Use a non-inline sealed hierarchy like `ApiResult<T>` for cross-platform error propagation.
14. **Platform exception types differ between Ktor engines.** OkHttp throws `java.net.SocketTimeoutException`, Darwin throws Foundation network errors. Both should map to `InternetError`, but test on BOTH platforms to confirm exception type hierarchy matches your `when` clauses.
15. **Server 422 responses can contain multiple `ApiError` entries.** Always iterate the full `errors` list when displaying form validation -- taking only the first error causes silent data loss for multi-field failures.
16. **`ApiErrorCode` enum deserialization fails on unknown server values.** If the server adds a new error code not in the enum, `kotlinx.serialization` throws `SerializationException`. Use `coerceInputValues = true` in `Json {}` config or add an `UNKNOWN` fallback entry.
17. **`ApiCall.await()` runs in the caller's coroutine context.** If the caller is on `Main`, the entire network call runs on `Main` unless `webserviceCall()` switches dispatchers internally. Always ensure the Ktor client or repository uses `Dispatchers.IO` (or `Dispatchers.Default` on iOS).
18. **Paging `LoadState.Error` wraps the exception generically.** You must cast `loadState.error` to `ApiCallException` to recover the typed `ApiResult.Error`. Forgetting the cast results in generic "unknown error" messages for all paging failures.

## Assets

| Path | Load when... |
|------|-------------|
| [setup.md](references/setup.md) | Adding dependencies or KSP processor registration |
| [http-client.md](references/http-client.md) | Configuring HttpClient auth, platform engines, timeouts |
| [api-types-and-converter.md](references/api-types-and-converter.md) | Working with ApiResult, ApiCall, converter |
| [services-and-dtos.md](references/services-and-dtos.md) | Adding endpoints, DTOs, DI wiring |
| [error-extensions.md](references/error-extensions.md) | Working with toUserMessage, fieldErrors, isRetryable, suspendRunCatching, error propagation, or paging error integration |
| [integration.md](references/integration.md) | Wiring dependencies, module load order |
| [api-call-usage.kt](assets/snippets/api-call-usage.kt) | Repository pattern with ApiCall |
| [new-service.kt.template](assets/templates/new-service.kt.template) | Scaffolding a new Ktorfit service |
| [error-display-components.kt](assets/snippets/error-display-components.kt) | Adding error banners, full-screen errors, snackbars, or field errors |
| [paging-error-footer.kt](assets/snippets/paging-error-footer.kt) | Adding paging error footer with LoadState mapping |
| [domain-error.kt.template](assets/templates/domain-error.kt.template) | Creating a feature-specific error hierarchy |

## Validation

### A. Networking correctness
- [ ] Ktor engine selected per platform (OkHttp for Android, Darwin for iOS)
- [ ] Content negotiation configured with `kotlinx.serialization` Json
- [ ] `HttpTimeout` installed with explicit connect, request, and socket values
- [ ] `HttpRequestRetry` installed with exponential backoff for transient failures
- [ ] Auth token injection via Ktor Auth Bearer plugin with `sendWithoutRequest` guard
- [ ] Token refresh uses Mutex double-check pattern to prevent concurrent refreshes
- [ ] `sealed interface` used for ApiResult response types
- [ ] No hardcoded base URLs -- BuildKonfig or AppConfig injected at build time
- [ ] `explicitNulls = false` set in Json configuration
- [ ] `ignoreUnknownKeys = true` set in Json configuration
- [ ] MockEngine used in all networking tests
- [ ] Pagination extracted from response headers (not body)

### B. Error handling correctness
- [ ] `ApiResult` top-level type is a sealed interface
- [ ] `ApiResult.Error` subtypes use sealed interface (shared properties via extensions or constructor params)
- [ ] `CancellationException` is never caught and swallowed -- `suspendRunCatching` used instead of `runCatching` in suspend contexts
- [ ] Exhaustive `when` on all sealed types (no `else` branch)
- [ ] `ApiResult` used consistently as the return type from network through repository layers
- [ ] Error mapping occurs at layer boundaries (network-to-domain, domain-to-UI)
- [ ] No `android.*` imports in `commonMain` source sets
- [ ] Template file uses sealed interface, not sealed class
- [ ] `toUserMessage()` and `isRetryable()` exist only in error-extensions.md (no duplication across references)

### C. Security
- [ ] HTTPS enforced for all API endpoints (no plain HTTP URLs)
- [ ] Certificate pinning documented or referenced for production builds
- [ ] No secrets, API keys, or hardcoded credentials in any file
- [ ] Refresh token not logged (LogLevel.INFO does not log headers by default, but verify)
- [ ] No secrets, tokens, or credentials in error messages or logs
- [ ] 401 handling triggers session revocation, not infinite retry
- [ ] Server error bodies are not displayed raw to users

### D. Performance
- [ ] HttpClient registered as Koin `single` (not `factory`) to reuse connection pools
- [ ] Timeout values reasonable (connect 10-15s, request 30-60s, socket 15-30s)
- [ ] Connection pooling via OkHttp on Android (default behavior when singleton)
- [ ] Json instance reused (not created per-request)
- [ ] `runCatching` limited to non-suspending, non-critical operations
- [ ] No unnecessary object allocations in hot-path error mapping
- [ ] Paging error footer does not trigger recomposition loops

### E. Integration
- [ ] Depends-on references match actual skill directory names
- [ ] Module load order documented: platform -> storage -> session -> ktorfit -> repositories
- [ ] Named qualifiers consistent between registration and resolution
- [ ] Downstream consumers (kmp-database, kmp-paging, kmp-transfer) documented
- [ ] Template placeholders consistent and documented
- [ ] Paging integration uses `ApiCallException` to bridge `ApiResult.Error` to `Throwable`
- [ ] Transfer integration uses `TransferException` with proper `CancellationException` rethrow
- [ ] UI components consume `UiState` error fields, not raw `ApiResult.Error`
