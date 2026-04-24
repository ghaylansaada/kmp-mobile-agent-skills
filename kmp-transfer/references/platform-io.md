# Platform I/O & DI Module

## Expect Classes (commonMain)

**File:** `core/transfer/io/FileReader.kt`
```kotlin
package {your.package}.core.transfer.io

expect class FileReader {
    fun size(): Long
    suspend fun read(offset: Long, limit: Long): ByteArray?
}
```

**File:** `core/transfer/io/FileWriter.kt`
```kotlin
package {your.package}.core.transfer.io

expect class FileWriter {
    fun size(): Long
    suspend fun writeAt(position: Long, bytes: ByteArray)
}
```

**Factories:** `core/transfer/io/FileReaderFactory.kt` / `FileWriterFactory.kt`
```kotlin
package {your.package}.core.transfer.io

interface FileReaderFactory { fun create(path: String): FileReader }
interface FileWriterFactory { fun create(path: String): FileWriter }
```

## Android Actuals (androidMain)

**FileReader** -- `RandomAccessFile` + `Dispatchers.IO`:
```kotlin
package {your.package}.core.transfer.io

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile

actual class FileReader(private val file: File) {
    actual fun size(): Long = file.length()
    actual suspend fun read(offset: Long, limit: Long): ByteArray? = withContext(Dispatchers.IO) {
        if (!file.exists() || offset >= file.length()) return@withContext null
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(offset)
            val buf = ByteArray(minOf(limit, file.length() - offset).toInt())
            val n = raf.read(buf)
            if (n <= 0) null else if (n == buf.size) buf else buf.copyOf(n)
        }
    }
}
```

**FileWriter** -- `RandomAccessFile` + `Dispatchers.IO`:
```kotlin
package {your.package}.core.transfer.io

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile

actual class FileWriter(private val file: File) {
    actual fun size(): Long = file.length()
    actual suspend fun writeAt(position: Long, bytes: ByteArray) = withContext(Dispatchers.IO) {
        file.parentFile?.mkdirs()
        RandomAccessFile(file, "rw").use { raf -> raf.seek(position); raf.write(bytes) }
    }
}
```

**Factories:**
```kotlin
package {your.package}.core.transfer.io

import java.io.File

class AndroidFileReaderFactory : FileReaderFactory {
    override fun create(path: String) = FileReader(File(path))
}
class AndroidFileWriterFactory : FileWriterFactory {
    override fun create(path: String) = FileWriter(File(path))
}
```

## iOS Actuals (iosMain)

**Helpers** (`core/transfer/io/helpers.kt`):
```kotlin
package {your.package}.core.transfer.io

import kotlinx.cinterop.*
import platform.Foundation.NSData
import platform.Foundation.create
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
internal fun NSData.toByteArray(): ByteArray =
    ByteArray(length.toInt()).also { bytes ->
        memScoped { memcpy(bytes.refTo(0), this@toByteArray.bytes, length) }
    }

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal fun ByteArray.toNSData(): NSData =
    usePinned { NSData.create(bytes = it.addressOf(0), length = size.toULong()) }
```

**FileReader** -- `NSFileHandle` + `Dispatchers.Default`:
```kotlin
package {your.package}.core.transfer.io

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.*

actual class FileReader(private val path: String) {
    private val fm = NSFileManager.defaultManager
    @OptIn(ExperimentalForeignApi::class)
    actual fun size(): Long =
        (fm.attributesOfItemAtPath(path, null)?.get(NSFileSize) as? NSNumber)?.longLongValue ?: 0L
    actual suspend fun read(offset: Long, limit: Long): ByteArray? = withContext(Dispatchers.Default) {
        if (!fm.fileExistsAtPath(path)) return@withContext null
        val handle = NSFileHandle.fileHandleForReadingAtPath(path) ?: return@withContext null
        handle.seekToFileOffset(offset.toULong())
        val data = handle.readDataOfLength(limit.toULong())
        handle.closeFile()
        if (data.length == 0UL) null else data.toByteArray()
    }
}
```

**FileWriter** -- `NSFileHandle` + `Dispatchers.Default`:
```kotlin
package {your.package}.core.transfer.io

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.*

actual class FileWriter(private val path: String) {
    private val fm = NSFileManager.defaultManager
    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
    actual fun size(): Long =
        (fm.attributesOfItemAtPath(path, null)?.get(NSFileSize) as? NSNumber)?.longLongValue ?: 0L
    actual suspend fun writeAt(position: Long, bytes: ByteArray) = withContext(Dispatchers.Default) {
        if (!fm.fileExistsAtPath(path)) fm.createFileAtPath(path, null, null)
        val handle = NSFileHandle.fileHandleForWritingAtPath(path) ?: error("Cannot open file: $path")
        handle.seekToFileOffset(position.toULong())
        handle.writeData(bytes.toNSData())
        handle.closeFile()
    }
}
```

**Factories:**
```kotlin
package {your.package}.core.transfer.io

class IOSFileReaderFactory : FileReaderFactory { override fun create(path: String) = FileReader(path) }
class IOSFileWriterFactory : FileWriterFactory { override fun create(path: String) = FileWriter(path) }
```

## DI Module (commonMain)

**File:** `di/modules/ExternalStorageModule.kt`
```kotlin
package {your.package}.di.modules

import {your.package}.core.transfer.ExternalStorageFactory
import {your.package}.getPlatform
import {your.package}.Platform
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

fun externalStorageModule() = module {
    single<ExternalStorageFactory> {
        val dispatcher = when (getPlatform().osType) {
            Platform.OsType.ANDROID -> Dispatchers.IO
            Platform.OsType.IOS -> Dispatchers.Default
        }
        ExternalStorageFactory(
            scope = CoroutineScope(dispatcher + SupervisorJob()),
            client = HttpClient(),
            writerFactory = get(),
            readerFactory = get(),
        )
    }
}
```

Creates its own `HttpClient()` -- transfer URLs are typically pre-signed (no bearer token needed).
