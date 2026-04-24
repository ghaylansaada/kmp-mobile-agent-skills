package {your.package}.presentation.user

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import {your.package}.core.network.ApiCallException
import {your.package}.core.network.result.ApiResult
import {your.package}.core.paging.PagingState
import {your.package}.core.paging.toPaginationUiState
import org.koin.compose.koinInject

@Composable
fun AccountScreen(
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = koinInject(),
) {
    val pagingItems = viewModel.pagingFlow.collectAsLazyPagingItems()
    val uiState = pagingItems.toPaginationUiState()

    val error = (pagingItems.loadState.refresh as? LoadState.Error)?.error
        ?: (pagingItems.loadState.append as? LoadState.Error)?.error
    val errorMessage = when (error) {
        is ApiCallException -> when (error.error) {
            is ApiResult.Error.InternetError -> "No internet connection."
            else -> error.message ?: "Request failed."
        }
        else -> error?.message
    }

    Column(modifier = modifier.fillMaxSize().padding(AppTheme.spacing.lg)) {
        Text(stringResource(Res.string.accounts), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(AppTheme.spacing.md))

        if (errorMessage != null) {
            Text(
                text = stringResource(Res.string.error_message, errorMessage),
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(AppTheme.spacing.sm))
        }

        when (uiState) {
            PagingState.InitialLoading -> CircularProgressIndicator()
            PagingState.InitialError -> {
                Text(stringResource(Res.string.failed_to_load))
                Button(onClick = { pagingItems.refresh() }) { Text(stringResource(Res.string.retry)) }
            }
            PagingState.Empty -> Text(stringResource(Res.string.no_accounts_found))
            else -> {}
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(count = pagingItems.itemCount) { index ->
                val item = pagingItems[index] ?: return@items
                Column(Modifier.fillMaxWidth().padding(vertical = AppTheme.spacing.sm)) {
                    Text(stringResource(Res.string.id_label, item.id))
                    Text(stringResource(Res.string.name_label, item.name))
                }
            }
            if (uiState == PagingState.Appending) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.lg),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            if (uiState == PagingState.AppendError) {
                item {
                    Column(Modifier.fillMaxWidth().padding(AppTheme.spacing.lg)) {
                        Text(stringResource(Res.string.failed_to_load_more))
                        Button(onClick = { pagingItems.retry() }) {
                            Text(stringResource(Res.string.retry))
                        }
                    }
                }
            }
            if (uiState == PagingState.EndOfPagination) {
                item {
                    Text(
                        text = stringResource(Res.string.all_items_loaded),
                        modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.lg),
                    )
                }
            }
        }
    }
}
