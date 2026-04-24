# Error Extensions and Display

## Error Extensions

**File:** `commonMain/kotlin/{your/package}/core/network/result/ApiResultExtensions.kt`

```kotlin
package {your.package}.core.network.result

import {your.package}.data.remote.dto.error.ApiError

fun ApiResult.Error.toUserMessage(): String = when (this) {
    is ApiResult.Error.InternetError -> "No internet connection. Please check your network."
    is ApiResult.Error.ParsingError -> "Unable to process the server response."
    is ApiResult.Error.HttpError -> when (status.value) {
        400 -> message ?: "Invalid request."
        401 -> "Your session has expired. Please log in again."
        403 -> "You do not have permission to perform this action."
        404 -> "The requested resource was not found."
        409 -> message ?: "A conflict occurred."
        422 -> formatValidationErrors(errors)
        429 -> "Too many requests. Please try again later."
        in 500..599 -> "Server error. Please try again later."
        else -> message ?: "An unexpected error occurred (${status.value})."
    }
    is ApiResult.Error.UnknownError -> "An unexpected error occurred."
}

private fun formatValidationErrors(errors: List<ApiError>?): String {
    if (errors.isNullOrEmpty()) return "Validation failed."
    return errors.mapNotNull { it.message }.joinToString("\n").ifEmpty { "Validation failed." }
}

fun ApiResult.Error.HttpError.fieldErrors(): Map<String, String> =
    errors?.filter { it.path != null && it.message != null }
        ?.associate { it.path!! to it.message!! } ?: emptyMap()

fun ApiResult.Error.isRetryable(): Boolean = when (this) {
    is ApiResult.Error.InternetError -> true
    is ApiResult.Error.HttpError -> status.value in 500..599 || status.value == 429
    is ApiResult.Error.UnknownError -> true
    is ApiResult.Error.ParsingError -> false
}
```

## suspendRunCatching Helper

Standard `runCatching` swallows `CancellationException`. Use this in all suspend contexts:

```kotlin
package {your.package}.core.util

import kotlin.coroutines.cancellation.CancellationException

inline fun <T> suspendRunCatching(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }
```

## TransferException

Used by the file transfer layer to bridge HTTP failures:

```kotlin
package {your.package}.core.transfer.core

import io.ktor.http.HttpStatusCode

class TransferException(
    val status: HttpStatusCode,
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause)
```

## Error Propagation Layers

1. **Network** -- KtorfitFactory maps exceptions to `ApiResult.Error`. No exceptions escape.
2. **Repository** -- `ApiCall.onFailure` handles side effects (logging). Returns `ApiResult` as-is.
3. **ViewModel** -- Inspects `ApiResult`, extracts error metadata into UiState fields.
4. **Compose UI** -- Reads UiState, conditionally displays error content.
5. **Paging** -- `ApiCallException` wraps errors for `MediatorResult.Error`.

## Paging Error Integration

`ApiCallException` wraps `ApiResult.Error` for paging's `MediatorResult.Error(Throwable)`:

```kotlin
// In BaseRemoteMediator:
val response = apiResponse as? ApiResult.Error
    ?: return MediatorResult.Error(Exception("Unknown error"))

ApiCallException(
    error = response, message = response.message, cause = response.exception
).let { MediatorResult.Error(it) }
```

UI consumption:

```kotlin
val refreshError = lazyPagingItems.loadState.refresh as? LoadState.Error
val apiError = (refreshError?.error as? ApiCallException)?.error
```

## runCatching Guidelines

- **Suspend context:** always use `suspendRunCatching` (rethrows `CancellationException`).
- **Non-suspend, non-critical only:** token refresh fallback, image URL resolution.
- **Never** in repositories or ViewModels -- errors must propagate through `ApiResult.Error`.
