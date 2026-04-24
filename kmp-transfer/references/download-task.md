# DownloadTask

**File:** `core/transfer/download/DownloadTask.kt`

Parallel chunked download using HTTP Range requests. Resumes from `writer.size()`.

```kotlin
package {your.package}.core.transfer.download

import {your.package}.core.transfer.core.BaseTransferTask
import {your.package}.core.transfer.core.FileChunk
import {your.package}.core.transfer.core.TaskState
import {your.package}.core.transfer.core.TransferException
import {your.package}.core.transfer.core.retryWithBackoff
import {your.package}.core.transfer.io.FileWriter
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

class DownloadTask(
    scope: CoroutineScope,
    private val url: String,
    private val client: HttpClient,
    private val writer: FileWriter,
    private val totalBytes: Long,
    chunkSize: Int,
    parallelism: Int,
) : BaseTransferTask(scope, parallelism, chunkSize) {

    override suspend fun execute() {
        val existingBytes = writer.size()
        bytesTransferredInternal = existingBytes
        stateFlow.update {
            it.copy(
                totalBytes = totalBytes,
                bytesTransferred = existingBytes,
                state = TaskState.RUNNING,
                error = null,
            )
        }
        val chunks = planDownloadChunks(totalBytes, chunkSize, existingBytes)
        coroutineScope {
            chunks.forEach { chunk ->
                launch {
                    semaphore.withPermit {
                        ensureActive()
                        retryWithBackoff {
                            val data = downloadRange(chunk.start, chunk.end)
                            writer.writeAt(chunk.start, data)
                            val currentTotal = progressMutex.withLock {
                                bytesTransferredInternal += data.size
                                bytesTransferredInternal
                            }
                            stateFlow.update { snap ->
                                snap.copy(
                                    bytesTransferred = currentTotal,
                                    speedBytesPerSec = estimator.update(currentTotal),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun downloadRange(start: Long, end: Long): ByteArray {
        val response = client.get(url) {
            header(HttpHeaders.Range, "bytes=$start-$end")
        }
        if (response.status != HttpStatusCode.PartialContent &&
            response.status != HttpStatusCode.OK
        ) {
            throw TransferException(
                response.status,
                "Error downloading bytes [$start-$end] from '$url'",
                Exception(response.bodyAsText()),
            )
        }
        val data = response.body<ByteArray>()
        if (data.size.toLong() != (end - start + 1)) {
            throw TransferException(
                HttpStatusCode.InternalServerError,
                "Chunk size mismatch: expected ${end - start + 1}, got ${data.size}",
            )
        }
        return data
    }

    private fun planDownloadChunks(
        totalBytes: Long,
        chunkSize: Int,
        alreadyDownloaded: Long,
    ): List<FileChunk> {
        require(chunkSize > 0) { "Chunk size must be > 0" }
        val chunks = mutableListOf<FileChunk>()
        var start = alreadyDownloaded
        while (start < totalBytes) {
            val end = minOf(start + chunkSize - 1, totalBytes - 1)
            chunks += FileChunk(start = start, end = end, size = totalBytes)
            start = end + 1
        }
        return chunks
    }
}
```

## Key Points

- Resume: `planDownloadChunks` starts from `writer.size()` offset
- HTTP Range header: `bytes=start-end` (both inclusive)
- Validates response is 206 (Partial Content) or 200 (OK)
- `ensureActive()` checks for cancellation before each chunk
- Chunk size mismatch throws `TransferException` (not generic `Exception`) for consistent error handling

## ExternalStorageFactory -- Download Path

```kotlin
fun createDownloadTask(
    url: String,
    path: String,
    totalBytes: Long,
    chunkSize: Int = 512 * 1024,
    parallelism: Int = 4,
): DownloadTask = DownloadTask(
    scope = scope,
    url = url,
    client = client,
    writer = writerFactory.create(path),
    totalBytes = totalBytes,
    chunkSize = chunkSize,
    parallelism = parallelism,
)
```
