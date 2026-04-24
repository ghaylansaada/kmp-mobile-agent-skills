# Screen Composable Patterns

All screen composables must use design tokens and string resources. No hardcoded dp values, strings, colors, or font sizes.

## UiState and ViewModel

```kotlin
package {your.package}.presentation.user

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import {your.package}.core.network.result.ApiResult
import {your.package}.data.local.entity.AccountEntity
import {your.package}.data.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// @Immutable tells Compose this class is deeply immutable after construction,
// enabling recomposition skipping when the value has not changed.
@Immutable
data class AccountUiState(
    val account: AccountEntity? = null,
    val isLoading: Boolean = false,
    val lastStatusCode: Int? = null,
    val lastMessage: String? = null,
)

class AccountViewModel(
    private val accountRepository: AccountRepository,
) : ViewModel() {

    val pagingFlow: Flow<PagingData<AccountEntity>> =
        accountRepository.pagingFlow().cachedIn(viewModelScope)

    private val _accountState = MutableStateFlow(AccountUiState())
    val accountState: StateFlow<AccountUiState> = _accountState.asStateFlow()

    init {
        viewModelScope.launch {
            accountRepository.observeAccount().collect { user ->
                _accountState.value = _accountState.value.copy(account = user)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _accountState.value = _accountState.value.copy(isLoading = true)
            val result = accountRepository.callApi()
            val error = result as? ApiResult.Error
            _accountState.value = _accountState.value.copy(
                isLoading = false,
                lastStatusCode = (error as? ApiResult.Error.HttpError)?.status?.value,
                lastMessage = error?.message ?: error?.exception?.message,
            )
        }
    }
}
```

- `cachedIn(viewModelScope)` caches PagingData across configuration changes
- Expose `StateFlow` (not `MutableStateFlow`) to composables

## Screen Composable (Stateful)

The stateful composable collects state and delegates to a stateless content composable. Do not add `@Preview` here -- `koinInject()` crashes without Koin initialization.

```kotlin
@Composable
fun AccountScreen(
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = koinInject(),
) {
    val accountState by viewModel.accountState.collectAsState()
    val pagingItems = viewModel.pagingFlow.collectAsLazyPagingItems()

    AccountScreenContent(
        modifier = modifier,
        state = accountState,
        pagingItems = pagingItems,
        onRefreshUser = { viewModel.refresh() },
        onRefreshList = { pagingItems.refresh() },
    )
}
```

## Screen Content Composable (Stateless)

The stateless composable receives all data as parameters. Safe for `@Preview`. Uses design tokens for all spacing and string resources for all user-facing text.

```kotlin
@Composable
fun AccountScreenContent(
    modifier: Modifier = Modifier,
    state: AccountUiState,
    pagingItems: LazyPagingItems<AccountEntity>,
    onRefreshUser: () -> Unit,
    onRefreshList: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppTheme.spacing.lg),
    ) {
        Text(
            text = stringResource(Res.string.account_screen_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(AppTheme.spacing.lg))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            AppPrimaryButton(
                text = stringResource(Res.string.refresh_user),
                onClick = onRefreshUser,
                enabled = !state.isLoading,
            )
            AppOutlinedButton(
                text = stringResource(Res.string.refresh_list),
                onClick = onRefreshList,
            )
        }
        Spacer(Modifier.height(AppTheme.spacing.lg))

        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(AppTheme.sizing.iconLg),
            )
            Spacer(Modifier.height(AppTheme.spacing.lg))
        }

        Text(
            stringResource(Res.string.account_db_user, state.account?.name ?: "---"),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            stringResource(Res.string.account_last_http, state.lastMessage ?: "---"),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(AppTheme.spacing.xl))

        // Paging error handling
        val error = (pagingItems.loadState.refresh as? LoadState.Error)?.error
            ?: (pagingItems.loadState.append as? LoadState.Error)?.error
        val errorMessage = when (error) {
            is ApiCallException -> when (error.error) {
                is ApiResult.Error.InternetError ->
                    stringResource(Res.string.error_no_connection_message)
                else -> error.message ?: stringResource(Res.string.error_generic_message)
            }
            else -> error?.message
        }
        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(
                count = pagingItems.itemCount,
                key = pagingItems.itemKey { it.id },
            ) { index ->
                val item = pagingItems[index] ?: return@items
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = AppTheme.spacing.sm),
                ) {
                    Text(
                        "id: ${item.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        item.name,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
```

## State Retention: remember vs rememberSaveable vs rememberUpdatedState

### remember — survives recomposition only

```kotlin
// Survives recomposition, LOST on configuration change (rotation)
var expanded by remember { mutableStateOf(false) }
```

Use for: transient UI state (dropdown open/close, animation triggers, tooltip visibility).

### rememberSaveable — survives configuration changes and process death

```kotlin
// Survives rotation, process death. Stored in Bundle.
var searchQuery by rememberSaveable { mutableStateOf("") }
```

Use for: user input (form fields, selected tabs, scroll positions, filter selections).

Constraint: value must be saveable to a Bundle (primitives, Parcelable, or custom Saver).

### rememberUpdatedState — keeps long-running effects current

```kotlin
// Problem: LaunchedEffect captures stale callback from first composition
// Solution: rememberUpdatedState always holds the latest value
@Composable
fun TimedMessage(
    message: String,
    onTimeout: () -> Unit,
) {
    val currentOnTimeout by rememberUpdatedState(onTimeout)

    LaunchedEffect(Unit) {
        delay(3_000)
        currentOnTimeout() // Always calls the LATEST onTimeout, not the stale one
    }

    Text(text = stringResource(Res.string.timed_message, message))
}
```

Use for: callbacks inside LaunchedEffect, DisposableEffect, or other long-running side effects where the lambda may change between recompositions.

### Comparison Table

```
| Function               | Survives                | Use case                         | Limitation                         |
|------------------------|-------------------------|----------------------------------|------------------------------------|
| `remember`             | Recomposition           | Transient UI state               | Lost on rotation/process death     |
| `rememberSaveable`     | Rotation + process death| User input, selections           | Value must be Bundle-saveable      |
| `rememberUpdatedState` | N/A (reference holder)  | Prevent stale closures in effects| Not a state container              |
```

Common mistake: using `remember` for form input that the user expects to survive rotation. Always use `rememberSaveable` for anything the user typed or selected.

## Per-Page ViewModel Scoping in Pagers

The standard approach in Compose Multiplatform for giving each pager page its own ViewModel instance:

```kotlin
@Composable
fun PagerScreen(
    viewModel: PagerHostViewModel = koinInject(),
) {
    val pagerState = rememberPagerState(pageCount = { viewModel.pageCount })

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        // Each page gets its own ViewModel instance via unique key
        val pageViewModel: PageViewModel = koinInject(
            parameters = { parametersOf(viewModel.pageIds[page]) },
        )

        PageContent(
            state = pageViewModel.state.collectAsState().value,
            onAction = pageViewModel::onAction,
        )
    }
}
```

- **Why default injection without a key shares the same instance across pages:** Koin (and most DI containers) resolve the same type to the same singleton or scoped instance. Without a distinguishing parameter or qualifier, every page receives the identical ViewModel, meaning page 2 mutates the state page 1 is displaying.
- **Using Koin `parametersOf` or `named` qualifiers to scope per page:** Pass a unique page identifier via `parametersOf` so the DI container creates a distinct instance per page. Alternatively, use `named(pageId)` qualifiers for explicit scoping.
- **ViewModel lifecycle:** Created when the page enters composition, cleared when the page is removed from composition (scrolled far enough away that HorizontalPager disposes it).
- **Warning:** Do not create ViewModels in `remember` -- use DI container scoping. Manually creating a ViewModel inside `remember { }` bypasses lifecycle management, leaks coroutine scopes, and breaks `viewModelScope` cancellation.

## File Placement

Screen composables live in their feature package, not in `ui/components/`:

```
commonMain/
  kotlin/{your.package}/
    feature/
      account/
        AccountScreen.kt        <- Stateful + stateless screen composables
        AccountViewModel.kt     <- ViewModel + UiState
      settings/
        SettingsScreen.kt
        SettingsViewModel.kt
    ui/
      components/               <- Shared reusable composables only
      theme/                    <- Design tokens, colors, typography
```
