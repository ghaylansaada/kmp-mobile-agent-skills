# UploadTask

**File:** `core/transfer/upload/UploadTask.kt`

Resumable chunked upload using GCS resumable session protocol.

```kotlin
package {your.package}.core.transfer.upload

import {your.package}.core.transfer.core.BaseTransferTask
import {your.package}.core.transfer.core.FileChunk
import {your.package}.core.transfer.core.TaskState
import {your.package}.core.transfer.core.TransferException
import {your.package}.core.transfer.core.retryWithBackoff
import {your.package}.core.transfer.io.FileReader
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

class UploadTask(
    scope: CoroutineScope,
    private val signedUrl: String,
    private val reader: FileReader,
    private val client: HttpClient,
    private val contentType: String,
    chunkSize: Int,
    parallelism: Int,
) : BaseTransferTask(scope, parallelism, chunkSize) {
    private var sessionUrl: String? = null

    override suspend fun execute() {
        ensureSessionUrl()
        val session = sessionUrl ?: error("Session URL not initialized")
        val totalBytes = reader.size()
        val alreadyUploaded = queryUploadedBytes(session)
        bytesTransferredInternal = alreadyUploaded
        val chunks = planUploadChunks(totalBytes, chunkSize, alreadyUploaded)
        stateFlow.update {
            it.copy(
                totalBytes = totalBytes,
                bytesTransferred = alreadyUploaded,
                state = TaskState.RUNNING,
                error = null,
            )
        }
        coroutineScope {
            chunks.forEach { chunk ->
                launch {
                    semaphore.withPermit {
                        ensureActive()
                        retryWithBackoff {
                            val data = reader.read(chunk.start, chunk.size)
                                ?: error("Failed to read chunk at offset ${chunk.start}")
                            uploadChunk(session, chunk.start, chunk.end, totalBytes, data)
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

    private suspend fun ensureSessionUrl(): String {
        if (sessionUrl == null) {
            val response = client.post(signedUrl) {
                header("x-goog-resumable", "start")
                header(HttpHeaders.ContentType, contentType)
            }
            if (!response.status.isSuccess()) {
                throw TransferException(
                    response.status,
                    "Failed to start resumable session for '$signedUrl'",
                    Exception(response.bodyAsText()),
                )
            }
            sessionUrl = response.headers[HttpHeaders.Location]
                ?: error("Failed to start resumable session")
        }
        return sessionUrl!!
    }

    private suspend fun uploadChunk(
        session: String,
        start: Long,
        end: Long,
        total: Long,
        data: ByteArray,
    ) {
        val response = client.put(session) {
            header(HttpHeaders.ContentType, contentType)
            header(HttpHeaders.ContentLength, data.size)
            header(HttpHeaders.ContentRange, "bytes $start-$end/$total")
            setBody(data)
        }
        if (!response.status.isSuccess()) {
            throw TransferException(
                response.status,
                "Upload failed for chunk $start-$end of $total bytes",
                Exception(response.bodyAsText()),
            )
        }
    }

    private suspend fun queryUploadedBytes(session: String): Long {
        val response = client.put(session) {
            header(HttpHeaders.ContentRange, "bytes */${reader.size()}")
        }
        if (!response.status.isSuccess()) {
            throw TransferException(
                response.status,
                "Unable to retrieve uploaded byte info",
                Exception(response.bodyAsText()),
            )
        }
        return response.headers[HttpHeaders.Range]
            ?.substringAfter("-")
            ?.toLongOrNull()
            ?.plus(1)
            ?: 0L
    }

    private fun planUploadChunks(
        totalBytes: Long,
        chunkSize: Int,
        uploadedBytes: Long,
    ): List<FileChunk> {
        val chunks = mutableListOf<FileChunk>()
        var start = uploadedBytes
        while (start < totalBytes) {
            val size = minOf(chunkSize.toLong(), totalBytes - start)
            chunks += FileChunk(start = start, end = start + size - 1, size = size)
            start += size
        }
        return chunks
    }
}
```

## Key Points

- `ensureSessionUrl()` initiates GCS resumable upload via POST with `x-goog-resumable: start`
- Upload Content-Range: `bytes START-END/TOTAL` (space after "bytes", no equals sign)
- `queryUploadedBytes()` uses `Content-Range: bytes */TOTAL` to query server progress
- Session URL survives app restarts for true resumability (persist it yourself)
- Progress update uses `currentTotal` from the Mutex-guarded counter (not `snap.bytesTransferred + chunk.size`) to avoid race conditions with parallel chunks

## ExternalStorageFactory -- Upload Path

```kotlin
fun createUploadTask(
    url: String,
    filePath: String,
    contentType: String,
    chunkSize: Int = 256 * 1024,
    parallelism: Int = 4,
): UploadTask = UploadTask(
    scope = scope,
    signedUrl = url,
    reader = readerFactory.create(filePath),
    client = client,
    contentType = contentType,
    chunkSize = chunkSize,
    parallelism = parallelism,
)
```
