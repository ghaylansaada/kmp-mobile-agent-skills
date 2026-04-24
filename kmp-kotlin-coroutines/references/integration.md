# Integration

## Upstream Dependency

**kmp-project-setup** provides `kotlinx-coroutines-core` in the version catalog and `commonMain.dependencies`.

## Integration Flow

```
Room DAO (Flow<T>)
    |
    v
Repository (flow { emitAll(...) })
    |
    v
ViewModel (viewModelScope.launch { collect })
    |
    v
StateFlow --> Compose UI (collectAsState)

HttpClientFactory (Mutex.withLock)
    |
    v
SessionManager (Flow<BearerTokens?>, MutableStateFlow<SessionState>)

ExternalStorageModule (CoroutineScope + SupervisorJob)
    |
    v
BaseTransferTask (SupervisorJob, Semaphore, Job cancellation)
    |
    v
DownloadTask / UploadTask (coroutineScope { launch { withPermit } })
```

## Dispatcher Selection

| Layer | Dispatcher | Rationale |
|-------|-----------|-----------|
| ViewModel | `Dispatchers.Main` (implicit) | UI state updates |
| Repository | Inherited from collector | No explicit switch |
| Network (Ktor) | Ktor internal | Ktor manages threads |
| Android File I/O | `Dispatchers.IO` | Thread pool for blocking I/O |
| iOS File I/O | `Dispatchers.Default` | Legacy convention |
| Transfer scope | Platform-dependent | Set in DI module |

## Connected Skills

- **kmp-networking** -- Mutex for token refresh, suspend functions for API calls
- **kmp-database** -- Room DAO returning Flow, suspend write operations
- **kmp-transfer** -- SupervisorJob + Semaphore for parallel chunk transfer
- **kmp-architecture** -- StateFlow collection in Compose, collectAsState
- **kmp-networking** -- CancellationException handling, retry strategies
