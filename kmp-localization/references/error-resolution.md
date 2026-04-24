# Error Message Resolution

Resolves `ApiResult.Error` into display strings, preferring server-provided messages with local string fallbacks.

## ErrorMessageResolver

```kotlin
package {your.package}.core.localization

import {your.package}.core.network.result.ApiResult
import {your.package}.core.network.result.ApiError
import io.ktor.http.HttpStatusCode
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.error_generic
import mobile.composeapp.generated.resources.error_network
import mobile.composeapp.generated.resources.error_not_found
import mobile.composeapp.generated.resources.error_server
import mobile.composeapp.generated.resources.error_timeout
import mobile.composeapp.generated.resources.error_unauthorized
import org.jetbrains.compose.resources.StringResource

object ErrorMessageResolver {
    fun resolve(error: ApiResult.Error): ResolvedError = when (error) {
        is ApiResult.Error.HttpError -> {
            val serverMsg = error.message?.takeIf { it.isNotBlank() }
                ?: error.errors?.firstOrNull()?.message?.takeIf { it.isNotBlank() }
            if (serverMsg != null) ResolvedError.ServerProvided(serverMsg)
            else ResolvedError.LocalFallback(httpStatusToResource(error.status))
        }
        is ApiResult.Error.InternetError -> ResolvedError.LocalFallback(Res.string.error_network)
        is ApiResult.Error.ParsingError -> ResolvedError.LocalFallback(Res.string.error_generic)
        is ApiResult.Error.UnknownError -> ResolvedError.LocalFallback(Res.string.error_generic)
    }

    private fun httpStatusToResource(status: HttpStatusCode): StringResource = when (status.value) {
        401 -> Res.string.error_unauthorized
        404 -> Res.string.error_not_found
        408 -> Res.string.error_timeout
        in 500..599 -> Res.string.error_server
        else -> Res.string.error_generic
    }
}

sealed interface ResolvedError {
    data class ServerProvided(val message: String) : ResolvedError
    data class LocalFallback(val resource: StringResource) : ResolvedError
}
```

## ErrorText Composable

```kotlin
package {your.package}.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import {your.package}.core.localization.ErrorMessageResolver
import {your.package}.core.localization.ResolvedError
import {your.package}.core.network.result.ApiResult

@Composable
fun ErrorText(error: ApiResult.Error, modifier: Modifier = Modifier) {
    val message = when (val resolved = ErrorMessageResolver.resolve(error)) {
        is ResolvedError.ServerProvided -> resolved.message
        is ResolvedError.LocalFallback -> stringResource(resolved.resource)
    }
    Text(text = message, modifier = modifier)
}
```

## Usage in ViewModel to UI

For non-@Composable contexts, pass `ResolvedError` to the UI layer and resolve there:

```kotlin
import {your.package}.core.localization.ErrorMessageResolver
import {your.package}.core.localization.ResolvedError
import {your.package}.core.network.result.ApiResult

// ViewModel
val resolvedError = ErrorMessageResolver.resolve(apiError)

// Composable
@Composable
fun DisplayError(resolved: ResolvedError) {
    val text = when (resolved) {
        is ResolvedError.ServerProvided -> resolved.message
        is ResolvedError.LocalFallback -> stringResource(resolved.resource)
    }
    Text(text = text)
}
```
