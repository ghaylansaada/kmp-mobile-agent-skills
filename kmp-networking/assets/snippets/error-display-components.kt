package {your.package}.ui.components.error

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ErrorBanner(
    message: String?,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier.fillMaxWidth(),
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier.fillMaxWidth().padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(AppTheme.sizing.iconMd))
                Spacer(modifier = Modifier.width(AppTheme.spacing.md))
                Text(
                    text = message ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f),
                )
                if (onRetry != null) {
                    TextButton(onClick = onRetry) { Text(stringResource(Res.string.retry)) }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, stringResource(Res.string.cd_dismiss), tint = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
    }
}

@Composable
fun FullScreenError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(AppTheme.spacing.xxl),
        ) {
            Icon(Icons.Default.WifiOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(AppTheme.sizing.iconXl))
            Spacer(modifier = Modifier.height(AppTheme.spacing.lg))
            Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(AppTheme.spacing.xl))
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(AppTheme.sizing.iconMd))
                Spacer(modifier = Modifier.width(AppTheme.spacing.sm))
                Text(stringResource(Res.string.try_again))
            }
        }
    }
}

@Composable
fun ErrorSnackbarHost(
    errorMessage: String?,
    onRetry: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            val result = snackbarHostState.showSnackbar(
                message = errorMessage,
                actionLabel = if (onRetry != null) getString(Res.string.retry) else null,
                duration = SnackbarDuration.Long,
            )
            when (result) {
                SnackbarResult.ActionPerformed -> onRetry?.invoke()
                SnackbarResult.Dismissed -> onDismiss()
            }
        }
    }
    SnackbarHost(hostState = snackbarHostState, modifier = modifier)
}

@Composable
fun FieldError(
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = errorMessage != null,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
        modifier = modifier,
    ) {
        Text(
            text = errorMessage ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(start = AppTheme.spacing.lg, top = AppTheme.spacing.xs),
        )
    }
}
