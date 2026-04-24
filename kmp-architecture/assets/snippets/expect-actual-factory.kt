// =============================================================================
// Factory pattern for expect classes with different constructor parameters.
//
// When an expect class has platform-specific constructor arguments (e.g.,
// java.io.File on Android, String path on iOS), common code cannot call the
// constructor directly. Use a factory interface to bridge the gap.
//
// Replace {your.package} with your actual package name before use.
// =============================================================================

// -- commonMain -----------------------------------------------------------------
// File: composeApp/src/commonMain/kotlin/{your/package_path}/ExampleReader.kt

// package {your.package}.core.example

// /**
//  * Common factory interface for creating platform-specific instances.
//  * Register implementations via Koin in each platform's platformModule().
//  */
// interface ExampleFactory {
//     fun create(identifier: String): ExampleReader
// }
//
// /**
//  * Expect class with platform-specific constructor.
//  * Common code should obtain instances through [ExampleFactory], not
//  * by calling the constructor directly.
//  */
// expect class ExampleReader {
//     fun size(): Long
//     suspend fun read(offset: Long, limit: Long): ByteArray?
// }


// -- androidMain ----------------------------------------------------------------
// File: composeApp/src/androidMain/kotlin/{your/package_path}/ExampleReader.kt

// package {your.package}.core.example
//
// import java.io.File
// import kotlinx.coroutines.Dispatchers
// import kotlinx.coroutines.withContext
//
// class AndroidExampleFactory : ExampleFactory {
//     override fun create(identifier: String) = ExampleReader(File(identifier))
// }
//
// actual class ExampleReader(private val file: File) {
//     actual fun size(): Long = file.length()
//     actual suspend fun read(offset: Long, limit: Long): ByteArray? =
//         withContext(Dispatchers.IO) {
//             if (!file.exists()) return@withContext null
//             file.inputStream().use { stream ->
//                 stream.skip(offset)
//                 val buffer = ByteArray(limit.toInt())
//                 val bytesRead = stream.read(buffer)
//                 if (bytesRead <= 0) null else buffer.copyOf(bytesRead)
//             }
//         }
// }


// -- iosMain --------------------------------------------------------------------
// File: composeApp/src/iosMain/kotlin/{your/package_path}/ExampleReader.kt

// package {your.package}.core.example
//
// import kotlinx.coroutines.Dispatchers
// import kotlinx.coroutines.withContext
//
// class IOSExampleFactory : ExampleFactory {
//     override fun create(identifier: String) = ExampleReader(identifier)
// }
//
// actual class ExampleReader(private val path: String) {
//     actual fun size(): Long { /* NSFileManager attributesOfItem */ return 0L }
//     actual suspend fun read(offset: Long, limit: Long): ByteArray? =
//         withContext(Dispatchers.Default) {
//             /* NSFileHandle seekToOffset + readDataOfLength */
//             null
//         }
// }


// -- Register in platformModule() -----------------------------------------------
//
// // androidMain
// single<ExampleFactory> { AndroidExampleFactory() }
//
// // iosMain
// single<ExampleFactory> { IOSExampleFactory() }
