package {your.package}.core.logging

// Logging utility patterns. For AppLogger/LogConfig setup, see references/core-logging.md.

import co.touchlab.kermit.Logger
import kotlin.time.TimeSource

// --- Structured API call logging ---

fun Logger.logApiCall(
    method: String,
    path: String,
    statusCode: Int? = null,
    durationMs: Long? = null,
) {
    d {
        buildString {
            append("API $method $path")
            statusCode?.let { append(" -> $it") }
            durationMs?.let { append(" (${it}ms)") }
        }
    }
}

// --- Scoped operation logging with timing ---

inline fun <T> Logger.logOperation(operationName: String, block: () -> T): T {
    d { "Starting: $operationName" }
    val mark = TimeSource.Monotonic.markNow()
    return try {
        val result = block()
        val elapsed = mark.elapsedNow().inWholeMilliseconds
        i { "Completed: $operationName (${elapsed}ms)" }
        result
    } catch (ex: Exception) {
        val elapsed = mark.elapsedNow().inWholeMilliseconds
        e(ex) { "Failed: $operationName (${elapsed}ms)" }
        throw ex
    }
}

// --- Error categorization ---

fun Logger.logError(
    context: String,
    error: Throwable,
    isRecoverable: Boolean = true,
) {
    if (isRecoverable) w(error) { "Recoverable error in $context" }
    else e(error) { "Fatal error in $context" }
}

// --- Paging operation logging ---

fun Logger.logPagingLoad(page: Int, pageSize: Int, itemCount: Int) {
    d { "Paging: loaded page $page (size=$pageSize, items=$itemCount)" }
}

fun Logger.logPagingExhausted(totalPages: Int, totalItems: Int) {
    i { "Paging: exhausted after $totalPages pages ($totalItems total items)" }
}

// --- Migration examples ---
// Before: println("Image failed: ${state.result.throwable}")
// After:  logger.e(state.result.throwable) { "Image load failed for path: $path" }
//
// Before: error.exception?.printStackTrace()
// After:  logger.e(error.exception) { "Account API error: ${error.message}" }
