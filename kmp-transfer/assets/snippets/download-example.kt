package {your.package}.example

import {your.package}.core.transfer.ExternalStorageFactory
import {your.package}.core.transfer.core.TaskResult
import {your.package}.core.transfer.core.TaskState
import {your.package}.core.transfer.download.DownloadTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// --- Example 1: Basic download with await ---

suspend fun downloadFile(
    factory: ExternalStorageFactory,
    url: String,
    localPath: String,
    totalBytes: Long,
) {
    val task = factory.createDownloadTask(
        url = url,
        path = localPath,
        totalBytes = totalBytes,
    )
    task.onProgress { bytes, total, speed, pct ->
        println("Download: $pct% ($bytes/$total) at ${speed / 1024} KB/s")
    }
    task.onSuccess { println("Download completed!") }
    task.onFailure { println("Download failed: ${it.message}") }

    val result = task.await()
    when (result.state) {
        TaskState.SUCCESS -> println("Saved to $localPath")
        TaskState.FAILED -> println("Error: ${result.error?.message}")
        TaskState.CANCELED -> println("Cancelled")
        else -> Unit
    }
}

// --- Example 2: Download with pause/resume ---

class DownloadManager(
    private val factory: ExternalStorageFactory,
    private val scope: CoroutineScope,
) {
    private var currentTask: DownloadTask? = null

    fun startDownload(url: String, path: String, totalBytes: Long) {
        val task = factory.createDownloadTask(
            url = url,
            path = path,
            totalBytes = totalBytes,
            chunkSize = 1024 * 1024,
            parallelism = 6,
        )
        task.onProgress { _, _, _, pct -> updateProgressUI(pct) }
        task.onPaused { updateStatusUI("Paused") }
        task.onCanceled { updateStatusUI("Cancelled") }
        currentTask = task
        scope.launch { handleResult(task.await()) }
    }

    fun pauseDownload() {
        currentTask?.pause()
    }

    fun resumeDownload() {
        scope.launch {
            currentTask?.await()?.let { handleResult(it) }
        }
    }

    fun cancelDownload() {
        currentTask?.cancel()
        currentTask = null
    }

    private fun handleResult(result: TaskResult) {
        when (result.state) {
            TaskState.SUCCESS -> updateStatusUI("Complete")
            TaskState.FAILED -> updateStatusUI("Failed: ${result.error?.message}")
            TaskState.CANCELED -> updateStatusUI("Cancelled")
            else -> Unit
        }
    }

    private fun updateProgressUI(progress: Int) { /* TODO: Compose state */ }
    private fun updateStatusUI(status: String) { /* TODO: Compose state */ }
}

// --- Example 3: Slow network tuning ---

suspend fun downloadOnSlowNetwork(
    factory: ExternalStorageFactory,
    url: String,
    path: String,
    totalBytes: Long,
) {
    val result = factory.createDownloadTask(
        url = url,
        path = path,
        totalBytes = totalBytes,
        chunkSize = 128 * 1024,
        parallelism = 2,
    ).await()
    if (result.state == TaskState.SUCCESS) println("Download complete: $path")
}

// --- Example 4: Upload ---

suspend fun uploadFile(
    factory: ExternalStorageFactory,
    signedUrl: String,
    filePath: String,
    contentType: String,
) {
    val task = factory.createUploadTask(
        url = signedUrl,
        filePath = filePath,
        contentType = contentType,
        chunkSize = 256 * 1024,
        parallelism = 4,
    )
    task.onProgress { _, _, speed, pct ->
        println("Upload: $pct% at ${speed / 1024} KB/s")
    }
    val result = task.await()
    when (result.state) {
        TaskState.SUCCESS -> println("Upload complete!")
        TaskState.FAILED -> println("Upload failed: ${result.error?.message}")
        else -> Unit
    }
}
