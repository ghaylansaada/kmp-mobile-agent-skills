# Integration: Transfer Cross-Skill Connections

## Upstream Dependencies

### kmp-project-structure

The transfer module lives within the single-module composeApp architecture. All source sets (commonMain, androidMain, iosMain) follow the standard directory layout.

### kmp-architecture

FileReader and FileWriter are **expect classes** in commonMain with **actual implementations** in androidMain and iosMain.

| expect class | Android actual | iOS actual |
|---|---|---|
| FileReader | RandomAccessFile, Dispatchers.IO | NSFileHandle, Dispatchers.Default |
| FileWriter | RandomAccessFile, Dispatchers.IO | NSFileHandle/NSData, Dispatchers.Default |

The factory interfaces (FileReaderFactory, FileWriterFactory) use the interface + platform implementation pattern rather than expect/actual, since factories are injected via DI.

### kmp-kotlin-coroutines

| Primitive | Usage in transfer |
|---|---|
| SupervisorJob | Isolates chunk failures in BaseTransferTask.taskJob |
| CoroutineScope | taskScope for all task coroutines |
| MutableStateFlow | Progress tracking and state management |
| Semaphore.withPermit | Bounds parallel chunk operations |
| Mutex.withLock | Thread-safe progress counter updates |
| coroutineScope | Structured parallel chunk processing |
| ensureActive() | Cooperative cancellation checks |
| CancellationException | Distinguishes pause from cancel |
| delay() | Exponential backoff in retryWithBackoff |
| withContext | Platform dispatcher switching in file I/O |

### kmp-networking

Ktor HttpClient is used for all HTTP operations:

- **Download:** client.get(url) with Range header for partial content
- **Upload:** client.post(signedUrl) for session init, client.put(session) for chunks
- **Status validation:** HttpStatusCode.PartialContent, HttpStatusCode.OK, isSuccess()
- **Headers:** Range, ContentRange, ContentType, ContentLength, Location

The transfer module creates its own HttpClient() instance rather than reusing the authorized client, because transfer URLs are typically pre-signed.

### kmp-dependency-injection

DI wiring connects the transfer system:

1. **Platform modules** register factory bindings:
   - Android: `single<FileReaderFactory> { AndroidFileReaderFactory() }`
   - iOS: `single<FileReaderFactory> { IOSFileReaderFactory() }`
   - Same for FileWriterFactory

2. **externalStorageModule()** resolves factories via get() and provides ExternalStorageFactory as a singleton

3. **commonModules()** includes externalStorageModule() in the module list

## Downstream Consumers

### Background Jobs

Transfer tasks are designed to integrate with background job schedulers:

- **Android:** WorkManager workers inject ExternalStorageFactory, create tasks, call await() within doWork()
- **iOS:** BGTaskScheduler handlers create tasks and use await() for completion

The pause() and cancel() methods support background job lifecycle management. TaskResult maps directly to worker result types:

| TaskState | Worker Result |
|---|---|
| SUCCESS | Worker success |
| FAILED | Worker retry or failure |
| CANCELED | Worker stopped |

## Integration Flow

```
Platform Module (Android/iOS)
    |
    v
FileReaderFactory / FileWriterFactory  (platform-specific singletons)
    |
    v
externalStorageModule()  (resolves factories, creates ExternalStorageFactory)
    |
    v
ExternalStorageFactory  (creates UploadTask / DownloadTask)
    |
    v
UploadTask / DownloadTask  (uses BaseTransferTask, HttpClient, FileReader/FileWriter)
    |
    v
Background Job / ViewModel  (calls await(), onProgress, pause/cancel)
```

## Cross-Skill File Dependencies

| File | Depends on Skill |
|---|---|
| FileReader.kt (expect) | kmp-architecture |
| FileReader.kt (android actual) | kmp-architecture |
| FileReader.kt (ios actual) | kmp-architecture |
| BaseTransferTask.kt | kmp-kotlin-coroutines (SupervisorJob, Semaphore, Mutex) |
| RetryPolicy.kt | kmp-kotlin-coroutines (delay) |
| DownloadTask.kt | kmp-networking (Ktor HttpClient) |
| UploadTask.kt | kmp-networking (Ktor HttpClient) |
| ExternalStorageModule.kt | kmp-dependency-injection (module DSL) |
| PlatformModule.android.kt | kmp-dependency-injection (factory bindings) |
| PlatformModule.ios.kt | kmp-dependency-injection (factory bindings) |
