# Integration

## Dependency Graph

```
kmp-project-setup
  |
  |  provides: source set structure, Gradle config, compiler flags
  |
  +---> kmp-architecture
  |       |
  |       |  architecture: ViewModel, Repository, UseCase, UiState, UDF
  |       |  state: StateFlow, sealed state, Channel events, combine, stateIn
  |       |  expect/actual: PlatformContext, getPlatform(), AppDatabaseConstructor
  |       |
  |       +---> DI (Koin)          -- uses expect platformModule()
  |       +---> Database (Room)    -- uses expect AppDatabaseConstructor
  |       +---> File Transfer      -- uses expect FileReader, expect FileWriter
  |       +---> Platform detection -- uses expect getPlatform(), PlatformContext
  |       |
  |       +---> kmp-compose-ui (downstream)
  |       +---> kmp-skie-bridge (downstream)
  |
  +---> kmp-dependency-injection
          |
          |  provides: Koin module organization, platform wiring, ViewModel scoping
```

## Upstream Dependencies

### kmp-project-setup

- **Source set structure** -- `commonMain`, `androidMain`, `iosMain` directories
- **Compiler flag** -- `-Xexpect-actual-classes` in the `kotlin {}` block
- **KSP targets** -- Room's generated `actual object` requires per-target KSP

### kmp-dependency-injection

- **Koin modules** -- `platformModule()`, `repositoryModule()`, `viewModelModule()`
- **ViewModel scoping** -- `viewModel {}` DSL for lifecycle-scoped injection
- **Platform wiring** -- `expect fun platformModule()` for platform-specific DI bindings

## Downstream Dependencies

### kmp-compose-ui

What the Compose UI skill uses from this skill:
- `StateFlow<UiState>` exposed by ViewModels, collected via `collectAsStateWithLifecycle()`
- `PagingState` enum consumed in `when` expressions for rendering
- `toPaginationUiState()` extension called on LazyPagingItems
- The `by` delegation pattern to unwrap `State<T>` into plain `T`
- One-shot events collected via `LaunchedEffect` + `collectLatest`

### kmp-skie-bridge

What the SKIE bridge skill uses from this skill:
- `StateFlow<T>` properties that SKIE transforms into Swift AsyncSequence wrappers
- Sealed interface types that SKIE maps to Swift enums with exhaustive switch
- Data class UiState types that SKIE generates as Swift structs

## How DI Uses expect platformModule

1. App entry point calls `initKoin()`
2. `initKoin()` calls `platformModule(context, config)` to get platform-specific bindings
3. Module provides: `DataStoreFactory`, `HttpClientEngineFactory`, `DatabaseFactory`, `FileReaderFactory`, `FileWriterFactory`

## How Database Uses expect AppDatabaseConstructor

1. `AppDatabase` annotated with `@ConstructedBy(AppDatabaseConstructor::class)`
2. Room KSP compiler generates `actual object` for each platform target
3. At runtime, Room calls `AppDatabaseConstructor.initialize()`

If KSP is not configured for a target, that target fails to compile with a missing actual error.

## Cross-Skill Data Flow

```
Room DAO / DataStore
     |
     | Flow<Entity> / Flow<Preferences>
     v
Repository / SessionManager
     |
     | Flow<Entity> / MutableStateFlow<SessionState>
     v
ViewModel [architecture + state-management patterns]
     |
     | StateFlow<UiState>          Channel<Event>
     |       |                         |
     v       v                         v
Compose Screen                   LaunchedEffect
  [collectAsStateWithLifecycle]    [collectLatest]
     |
     | SKIE bridge
     v
Swift UI [async iteration]
```

## Module Loading Order

Koin modules resolve lazily, but the logical dependency order is:

1. platformModule -- platform-specific providers
2. localStorageModule -- DataStore, SessionManager
3. networkModule -- Ktor HttpClient
4. repositoryModule -- repositories consuming network + database
5. viewModelModule -- ViewModels consuming repositories

## Cross-References

| Skill | Relevance |
|-------|-----------|
| kmp-kotlin-coroutines | Dispatchers.IO vs Default in actual implementations |
| kmp-networking | Error types are commonMain-only, no expect/actual needed |
| kmp-paging | PagingState types and toPaginationUiState() extension |
| kmp-session-management | SessionState sealed interface and SessionManager |
