---
name: kmp-paging
description: >
  Offline-first pagination with Room as single source of truth and BaseRemoteMediator
  for network sync -- PageableEntity contract, remote key management, PagingExt helpers,
  PagingState enum, and Compose LazyColumn consumption. Use when adding a paginated list,
  implementing offline-first pagination with network sync, troubleshooting paging behavior,
  or understanding RemoteMediator lifecycle. Does NOT cover database setup (see kmp-database),
  networking stack (see kmp-networking), or DI wiring (see kmp-dependency-injection).
compatibility: >
  KMP with Compose Multiplatform. Requires AndroidX Paging 3 multiplatform
  (paging-common, paging-compose), paging-runtime for Android, and room-paging bridge.
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---

# KMP Paging

## Scope

Covers AndroidX Paging 3 multiplatform setup: BaseRemoteMediator with remote key management,
PageableEntity contract, PagingExt factory helpers, PagingState enum for UI state mapping,
Compose LazyPagingItems consumption, and offline-first data flow (Network -> Room -> PagingSource
-> Pager -> ViewModel -> Compose). Does not cover Room database internals, Ktorfit service
definitions, or Koin module setup.

## When to use

- Adding a new paginated list feature
- Implementing offline-first pagination with network sync
- Troubleshooting paging behavior (empty states, duplicate loads, end-of-list)
- Understanding RemoteMediator lifecycle and remote key strategy
- Displaying paginated lists from API or database
- Implementing infinite scroll with automatic page loading
- Adding pull-to-refresh with paginated data
- Showing loading/error states at list boundaries (append/prepend)

## Depends on

- **kmp-database** -- AppDatabase, entities, DAOs, useWriterConnection, immediateTransaction
- **kmp-networking** -- Ktorfit service interfaces, ApiResult, ApiCallException
- **kmp-dependency-injection** -- Koin wiring for repositories, ViewModels

## Workflow

1. Add paging dependencies -> [setup.md](references/setup.md)
2. Understand BaseRemoteMediator and remote key management -> [base-remote-mediator.md](references/base-remote-mediator.md)
3. Review paging support types (PageableEntity, PagingExt, PagingState) -> [paging-types.md](references/paging-types.md)
4. Wire concrete entity, DAO, mediator, repository, ViewModel -> [vertical-slice.md](references/vertical-slice.md)
5. Register in Koin and connect upstream/downstream -> [integration.md](references/integration.md)

## Gotchas

1. **PagingSource is single-use -- factory must return a new instance every call.** Room PagingSource becomes invalid after the first data change. If the `pagingSourceFactory` lambda returns a cached instance, subsequent loads throw `IllegalStateException("PagingSource is invalid")` and the list goes blank.
2. **`cachedIn(viewModelScope)` must be called exactly once as a `val`.** Multiple `cachedIn` calls on the same flow create interfering cache layers that cause duplicate page loads and inconsistent item counts. Calling it on a computed property (`get()`) creates a new cached flow on every recomposition, leaking memory.
3. **RemoteMediator needs atomic transactions.** Remote key deletion, data clearing, key insertion, and data insertion must all happen inside a single `useWriterConnection { transactor -> transactor.immediateTransaction { } }` block. Without atomicity, a crash between clear and insert leaves the user with an empty list.
4. **`retry()` only retries APPEND, not REFRESH.** Calling `retry()` after a failed REFRESH does nothing visible. To retry REFRESH, call `refresh()` instead. Using the wrong method leaves the user stuck on an error screen with a non-functional retry button.
5. **`paging-runtime` is Android-only.** Placing `paging-runtime` in `commonMain.dependencies` causes an unresolved reference error on iOS compilation. Only `paging-common` and `paging-compose` go in `commonMain`.
6. **Page index starts at 0 but many APIs use 1-based pages.** If the API expects 1-based pages and the mediator sends page 0, the first two loads return the same data, producing duplicate items in the list.
7. **Room PagingSource auto-invalidates on table changes.** Calling `invalidate()` manually on top of Room's automatic invalidation causes a double-load: Room triggers one reload from the table change, and the manual call triggers another.
8. **RemoteMediator dispatcher differs per platform.** Android uses `Dispatchers.IO`; iOS uses `Dispatchers.Default`. Network calls inside `networkResponse()` must not assume a specific dispatcher. Dispatchers are typically handled by Ktor internally, but any direct disk I/O should use `withContext`.
9. **`enablePlaceholders = true` requires stable item counts from Room.** If the DAO query uses dynamic filtering that changes the total count between pages, placeholders cause index-out-of-bounds crashes in LazyColumn. Disable placeholders for filtered queries.

## Assets

| Path | Load when... |
|------|-------------|
| [references/setup.md](references/setup.md) | Adding paging dependencies |
| [references/base-remote-mediator.md](references/base-remote-mediator.md) | Understanding core mediator and remote keys |
| [references/paging-types.md](references/paging-types.md) | Working with paging support types |
| [references/vertical-slice.md](references/vertical-slice.md) | Wiring a concrete paginated feature |
| [references/integration.md](references/integration.md) | DI wiring, cross-skill connections |
| [assets/snippets/paging-screen.kt](assets/snippets/paging-screen.kt) | Compose screen with LazyPagingItems |
| [assets/templates/new-remote-mediator.kt.template](assets/templates/new-remote-mediator.kt.template) | Scaffolding a new RemoteMediator |

## Validation

### A. Build and compilation
- [ ] No unresolved imports in any code snippet

### B. Paging correctness
- [ ] `pagingSourceFactory` lambda returns a new PagingSource instance on every call
- [ ] `PagingData` flow cached with `cachedIn(viewModelScope)` exactly once, as a `val`
- [ ] `PagingSource` handles errors gracefully (returns `LoadResult.Error`)
- [ ] `RemoteMediator.load()` wraps clear + insert in a single atomic transaction
- [ ] RemoteMediator handles REFRESH, APPEND, and PREPEND load types correctly
- [ ] Page size configured appropriately (`pageSize`, `initialLoadSize`, `prefetchDistance`)
- [ ] Placeholder support considered (`enablePlaceholders` matches query stability)
- [ ] No `android.*` imports in commonMain code
- [ ] `retry()` used only for APPEND errors; `refresh()` used for REFRESH errors
- [ ] Page index convention (0-based vs 1-based) consistent between mediator and API

### C. Performance
- [ ] `pageSize` and `initialLoadSize` set to reasonable values (10-30)
- [ ] `prefetchDistance` set to trigger pre-loading before end of visible list
- [ ] `cachedIn` prevents redundant network/database loads across configuration changes
- [ ] No redundant `invalidate()` calls on Room-backed PagingSource
- [ ] RemoteMediator does not perform blocking I/O on the main thread

### D. Security
- [ ] No secrets, API keys, or hardcoded credentials in any file
- [ ] No hardcoded base URLs in service calls or mediators

### E. Integration
- [ ] Depends-on references match actual skill directory names (kmp-database, kmp-networking, kmp-dependency-injection)
- [ ] Integration reference lists correct upstream and downstream skills
- [ ] Template placeholders are consistent and documented
- [ ] Cross-skill file dependency table is accurate
