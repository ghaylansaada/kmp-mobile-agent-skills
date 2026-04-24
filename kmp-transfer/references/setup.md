# Setup: Transfer Dependencies

> Always use the latest stable versions. Check the official release pages for current versions.

## No Additional Dependencies

The transfer system uses only libraries already present via upstream skills:

- `io.ktor:ktor-client-core` -- HTTP requests with Range/Content-Range headers (from kmp-networking)
- `org.jetbrains.kotlinx:kotlinx-coroutines-core` -- SupervisorJob, Semaphore, Mutex, Flow (from kmp-kotlin-coroutines)
- `io.insert-koin:koin-core` -- DI module registration (from kmp-dependency-injection)
- `kotlin.time.Clock` -- Speed estimation timing (stdlib, Kotlin 2.1+)

No extra version catalog entries or build.gradle.kts additions are required.

## Source Set Layout

```
composeApp/src/
  commonMain/kotlin/{your.package}/core/transfer/
    core/
      BaseTransferTask.kt       -- Abstract base with progress, pause/resume/cancel
      TransferSnapshot.kt       -- Progress snapshot data class
      TaskState.kt              -- Enum: IDLE, RUNNING, PAUSED, CANCELED, FAILED, SUCCESS
      TaskResult.kt             -- Result wrapper (state + error)
      FileChunk.kt              -- Byte range data class
      TransferException.kt      -- HTTP status exception
      RetryPolicy.kt            -- retryWithBackoff utility
    download/
      DownloadTask.kt           -- Parallel chunked download with Range header
    upload/
      UploadTask.kt             -- Resumable parallel chunked upload
    io/
      FileReader.kt             -- expect class for chunked file reading
      FileWriter.kt             -- expect class for offset-based file writing
      FileReaderFactory.kt      -- Factory interface for FileReader
      FileWriterFactory.kt      -- Factory interface for FileWriter
    util/
      SpeedEstimator.kt         -- Smoothed speed calculation
    ExternalStorageFactory.kt   -- Factory for creating upload/download tasks

  androidMain/kotlin/{your.package}/core/transfer/io/
    FileReader.kt               -- actual: RandomAccessFile + Dispatchers.IO
    FileWriter.kt               -- actual: RandomAccessFile + Dispatchers.IO
    AndroidFileReaderFactory.kt -- Factory wrapping java.io.File
    AndroidFileWriterFactory.kt -- Factory wrapping java.io.File

  iosMain/kotlin/{your.package}/core/transfer/io/
    FileReader.kt               -- actual: NSFileHandle + Dispatchers.Default
    FileWriter.kt               -- actual: NSFileHandle + Dispatchers.Default
    IOSFileReaderFactory.kt     -- Factory wrapping file path string
    IOSFileWriterFactory.kt     -- Factory wrapping file path string
    helpers.kt                  -- NSData.toByteArray() / ByteArray.toNSData()

  commonMain/kotlin/{your.package}/di/modules/
    ExternalStorageModule.kt    -- externalStorageModule() Koin module
```

## Platform Module Registration

Factory bindings are declared in the platform-specific `platformModule()`:

**Android** (`PlatformModule.android.kt`):
```kotlin
single<FileReaderFactory> { AndroidFileReaderFactory() }
single<FileWriterFactory> { AndroidFileWriterFactory() }
```

**iOS** (`PlatformModule.ios.kt`):
```kotlin
single<FileReaderFactory> { IOSFileReaderFactory() }
single<FileWriterFactory> { IOSFileWriterFactory() }
```
