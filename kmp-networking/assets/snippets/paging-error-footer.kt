package {your.package}.ui.components.error

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import {your.package}.core.network.ApiCallException
import {your.package}.core.network.result.ApiResult

fun LoadState.Error.toUserMessage(): String {
    val apiError = (error as? ApiCallException)?.error
    return when (apiError) {
        is ApiResult.Error.InternetError -> "No internet connection. Pull to refresh."
        is ApiResult.Error.ParsingError -> "Unable to process the server response."
        is ApiResult.Error.HttpError -> when (apiError.status.value) {
            401 -> "Your session has expired."
            403 -> "You do not have permission."
            404 -> "The requested data was not found."
            in 500..599 -> "Server error. Please try again later."
            else -> apiError.message ?: "Error ${apiError.status.value}"
        }
        is ApiResult.Error.UnknownError -> "An unexpected error occurred."
        null -> error.message ?: "An unexpected error occurred."
    }
}

@Composable
fun PagingErrorFooter(
    loadState: LoadState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (loadState is LoadState.Error) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = modifier.fillMaxWidth().padding(AppTheme.spacing.lg),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = loadState.toUserMessage(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(AppTheme.spacing.sm))
                OutlinedButton(onClick = onRetry) { Text(stringResource(Res.string.retry)) }
            }
        }
    }
}
