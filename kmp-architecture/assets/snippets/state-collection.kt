package {your.package}.snippets

// =============================================================================
// Snippet 1: StateFlow collection with collectAsStateWithLifecycle() (Android)
// =============================================================================

/*
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AccountScreen(
    viewModel: AccountViewModel = koinViewModel(),
) {
    // collectAsStateWithLifecycle() stops collecting when lifecycle < STARTED,
    // saving resources when the app is in the background.
    // `by` delegation unwraps State<AccountUiState> into AccountUiState.
    val accountState by viewModel.accountState.collectAsStateWithLifecycle()

    Column {
        if (accountState.isLoading) {
            CircularProgressIndicator()
        }
        Text(stringResource(Res.string.label_user, accountState.account?.name ?: "--"))
        Text(stringResource(Res.string.label_status, accountState.lastMessage ?: stringResource(Res.string.status_ok)))
    }
}
*/

// =============================================================================
// Snippet 2: collectAsState() for common/multiplatform Compose code
// =============================================================================

/*
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AccountScreenCommon(
    viewModel: AccountViewModel = koinViewModel(),
) {
    // Use collectAsState() in commonMain where lifecycle-runtime-compose
    // is not available. On Android-only code, prefer collectAsStateWithLifecycle().
    val accountState by viewModel.accountState.collectAsState()

    Column {
        if (accountState.isLoading) {
            CircularProgressIndicator()
        }
        Text(stringResource(Res.string.label_user, accountState.account?.name ?: "--"))
        Text(stringResource(Res.string.label_status, accountState.lastMessage ?: stringResource(Res.string.status_ok)))
    }
}
*/

// =============================================================================
// Snippet 3: PagingData collection with collectAsLazyPagingItems()
// =============================================================================

/*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.paging.compose.collectAsLazyPagingItems
import {your.package}.core.paging.PagingState
import {your.package}.core.paging.toPaginationUiState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PaginatedListScreen(
    viewModel: AccountViewModel = koinViewModel(),
) {
    // collectAsLazyPagingItems() returns LazyPagingItems<T>, NOT State<T>.
    // Do NOT use `by` delegation here.
    val pagingItems = viewModel.pagingFlow.collectAsLazyPagingItems()
    val pagingState = pagingItems.toPaginationUiState()

    when (pagingState) {
        PagingState.InitialLoading -> FullScreenLoader()
        PagingState.InitialError -> FullScreenError(onRetry = { pagingItems.refresh() })
        PagingState.Empty -> EmptyState(message = "No items found")
        PagingState.Content,
        PagingState.Appending,
        PagingState.AppendError,
        PagingState.EndOfPagination -> {
            LazyColumn {
                items(count = pagingItems.itemCount) { index ->
                    val item = pagingItems[index] ?: return@items
                    ItemRow(item)
                }
            }
        }
    }
}
*/

// =============================================================================
// Snippet 4: Sealed UiState collection with exhaustive when
// =============================================================================

/*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import {your.package}.core.ui.UiState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Exhaustive when -- compiler enforces all branches are handled
    when (val state = uiState) {
        is UiState.Loading -> CircularProgressIndicator()
        is UiState.Success -> ProfileContent(profile = state.data)
        is UiState.Error -> ErrorMessage(
            message = state.message,
            onRetry = { viewModel.retry() },
        )
        is UiState.Empty -> EmptyState(message = "No profile found")
    }
}
*/

// =============================================================================
// Snippet 5: collectAsState with initial value for cold Flows
// =============================================================================

/*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.Flow

@Composable
fun SettingsScreen(
    settingsFlow: Flow<Settings>,  // cold Flow, not StateFlow
) {
    // Cold Flows require an initial value because they may not have emitted yet.
    val settings by settingsFlow.collectAsState(initial = Settings.DEFAULT)
    Text(stringResource(Res.string.label_theme, settings.theme))
}
*/
