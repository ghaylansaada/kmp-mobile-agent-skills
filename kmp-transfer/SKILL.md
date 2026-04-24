---
name: kmp-transfer
description: >
  Chunked, resumable, parallel file upload and download system with progress
  tracking, pause/resume/cancel, exponential backoff retry, and platform-specific
  file I/O via expect/actual. Use when implementing file uploads or downloads
  with progress, adding resumable transfer capabilities, creating custom transfer
  tasks, or integrating transfers with background job scheduling.
compatibility: >
  KMP with Compose Multiplatform. Requires Ktor, kotlinx-coroutines, and Koin.
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---

# KMP File Transfer

## When to use

- Implementing file upload or download with progress tracking
- Adding resumable or chunked transfer capabilities
- Creating custom transfer tasks by extending BaseTransferTask
- Configuring platform-specific file I/O for transfer operations
- Integrating file transfers with background job scheduling
- Showing upload/download progress indicators in the UI
- Implementing pause/resume for large file transfers
- Handling transfer failures with automatic retry and exponential backoff

## Depends on

- **kmp-networking** -- Ktor HttpClient for HTTP operations (Range, Content-Range headers)
- **kmp-architecture** -- FileReader/FileWriter expect classes with platform actuals
- **kmp-kotlin-coroutines** -- SupervisorJob, Semaphore, Mutex, MutableStateFlow, coroutineScope
- **kmp-dependency-injection** -- DI module for ExternalStorageFactory, FileReaderFactory, FileWriterFactory

## Workflow

1. Verify Ktor and coroutines dependencies are present --> see [setup.md](references/setup.md)
2. Review core data models (TaskState, TransferSnapshot, FileChunk, retry, speed) --> see [core-models.md](references/core-models.md)
3. Understand BaseTransferTask lifecycle (progress, pause/resume/cancel, await) --> see [base-task.md](references/base-task.md)
4. For downloads: use DownloadTask with chunked parallel Range requests --> see [download-task.md](references/download-task.md)
5. For uploads: use UploadTask with GCS resumable session protocol --> see [upload-task.md](references/upload-task.md)
6. Register platform FileReader/FileWriter and wire DI --> see [platform-io.md](references/platform-io.md)
7. For custom transfer protocols, extend BaseTransferTask --> use [template](assets/templates/custom-transfer-task.kt.template)
8. Wire ExternalStorageFactory and create tasks --> see [snippet](assets/snippets/download-example.kt)

## Gotchas

1. **SupervisorJob isolates chunk failures but not memory.** Each in-flight chunk loads its entire byte range into a ByteArray. With 4 parallel 512 KB chunks that is 2 MB minimum. On memory-constrained iOS devices, reduce chunkSize or parallelism or the app will be terminated by the OS.

2. **Calling cancel() when you mean pause() makes the task permanently unresumable.** `cancel()` cancels `taskJob` (the SupervisorJob), which enters a completed state and can never launch new coroutines. `pause()` only cancels `childJob` and sets the paused flag. Confusing the two is unrecoverable without creating a new task.

3. **Catching CancellationException inside withPermit leaks Semaphore permits.** When a task is cancelled, coroutines blocked on `semaphore.withPermit` are cancelled via CancellationException which correctly releases the permit. If you catch CancellationException without rethrowing, the permit leaks and subsequent tasks deadlock waiting for permits that never return. See **kmp-kotlin-coroutines** skill for cancellation handling patterns.

4. **Upload Content-Range header format differs from download Range header.** Upload uses `bytes START-END/TOTAL` (space after "bytes", no equals). Download uses `bytes=START-END` (equals, no space). Getting the upload format wrong causes 400 Bad Request on every chunk.

5. **Upload resume query parses a zero-based inclusive end offset.** `queryUploadedBytes()` sends `Content-Range: bytes */TOTAL` and parses the response Range header `bytes=0-N`. The +1 on the parsed end offset is critical -- omitting it causes overlapping chunks and data corruption.

6. **await() hangs forever if the task is paused externally.** `stateFlow.first { ... }` only resolves on SUCCESS/FAILED/CANCELED. If something calls `pause()` while `await()` is suspended, await never returns. Only call `pause()` when you plan to call `await()` again to resume.

7. **iOS background transfers have mismatched timeouts.** iOS BGTaskScheduler allows 7-day transfers, but Ktor defaults to 15-second request timeout. Individual chunks will timeout on slow networks unless you configure Ktor's timeout to match expected chunk transfer duration.

8. **Transfer module creates its own HttpClient, not the authorized one.** Transfer URLs are typically pre-signed and do not need bearer tokens. If your transfer endpoint requires auth headers, you must pass the authorized HttpClient to ExternalStorageFactory instead.

9. **retryWithBackoff must not swallow CancellationException.** The retry utility catches `Exception` (not `Throwable`) so that CancellationException propagates immediately. If you modify it to catch `Throwable`, cancellation and pause stop working and the task hangs until all retries are exhausted.

10. **iOS NSFileHandle is not thread-safe.** Concurrent reads or writes to the same NSFileHandle from multiple coroutines cause data corruption. Each coroutine must open its own handle, which the current FileReader/FileWriter implementations already do per-call.

## Assets

| Path | Load when... |
|------|-------------|
| [setup.md](references/setup.md) | Verifying dependencies or source set layout |
| [core-models.md](references/core-models.md) | Working with TaskState, TransferSnapshot, FileChunk, retry, speed |
| [base-task.md](references/base-task.md) | Understanding task lifecycle, progress, pause/resume/cancel |
| [download-task.md](references/download-task.md) | Implementing chunked parallel downloads with Range requests |
| [upload-task.md](references/upload-task.md) | Implementing GCS resumable uploads |
| [platform-io.md](references/platform-io.md) | Configuring platform FileReader/FileWriter and DI wiring |
| [integration.md](references/integration.md) | Cross-skill dependencies and downstream consumers |
| [custom-transfer-task.kt.template](assets/templates/custom-transfer-task.kt.template) | Scaffolding a custom BaseTransferTask subclass |
| [download-example.kt](assets/snippets/download-example.kt) | Download/upload usage with progress, pause/resume, slow-network tuning |

## Validation

### A. Build and compilation
- [ ] No unresolved imports in any code snippet

### B. Transfer correctness
- [ ] Progress reported via MutableStateFlow with TransferSnapshot updates
- [ ] retryWithBackoff catches Exception (not Throwable) so CancellationException propagates
- [ ] Backoff sequence correct: 500 ms, 1 s, 2 s, 4 s, then final unguarded attempt
- [ ] BaseTransferTask transitions: IDLE -> RUNNING -> SUCCESS, IDLE -> RUNNING -> FAILED, IDLE -> RUNNING -> PAUSED -> RUNNING -> SUCCESS
- [ ] pause() cancels only childJob; cancel() cancels both childJob and taskJob
- [ ] DownloadTask resumes from writer.size() offset -- no re-downloading completed chunks
- [ ] UploadTask resumes from queryUploadedBytes() -- no re-uploading completed chunks
- [ ] Content-Range header format: `bytes START-END/TOTAL` (upload), `bytes=START-END` (download Range)
- [ ] queryUploadedBytes parses end offset with +1 to avoid overlapping chunks
- [ ] Chunks processed via streaming (per-chunk ByteArray), not loading entire file into memory
- [ ] ensureActive() called before each chunk transfer for cooperative cancellation

### C. Security
- [ ] No secrets, API keys, or hardcoded credentials in any file
- [ ] Platform FileWriter creates parent directories but does not set overly permissive file modes
- [ ] Temporary files or partial downloads stored in app-sandboxed paths only
- [ ] Transfer URLs assumed pre-signed -- no bearer tokens logged or stored

### D. Performance
- [ ] Semaphore bounds parallel chunk count (default 4) to prevent memory exhaustion
- [ ] SpeedEstimator uses exponential smoothing (0.8/0.2) to avoid UI jitter
- [ ] ExternalStorageFactory registered as Koin single to reuse CoroutineScope
- [ ] Chunk size defaults reasonable (512 KB download, 256 KB upload)
- [ ] Android file I/O uses Dispatchers.IO; iOS uses Dispatchers.Default

### E. Integration
- [ ] Depends-on references match actual skill directory names (kmp-dependency-injection, not kmp-koin)
- [ ] Platform module registers FileReaderFactory and FileWriterFactory bindings
- [ ] externalStorageModule() resolves factories via Koin get() and provides ExternalStorageFactory singleton
- [ ] Downstream consumers (background jobs, ViewModels) documented
- [ ] Template placeholders ({your.package}) consistent across all files
