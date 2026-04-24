# State Management

## Sealed Interface for Finite State Machines

When states are mutually exclusive, use a sealed interface. Each state is an object (no data) or a data class (with associated data). Exhaustive `when` guarantees compile-time safety.

### Generic Sealed UiState Pattern

For screens that follow a strict loading-then-result lifecycle, use a generic sealed interface. See the full template at [sealed-ui-state.kt.template](../assets/templates/sealed-ui-state.kt.template).

```kotlin
package {your.package}.core.ui

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(
        val message: String,
        val exception: Throwable? = null,
    ) : UiState<Nothing>
    data object Empty : UiState<Nothing>
}
```

Usage in a ViewModel:

```kotlin
package {your.package}.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import {your.package}.core.ui.UiState
import {your.package}.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Profile>>(UiState.Loading)
    val uiState: StateFlow<UiState<Profile>> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val profile = profileRepository.getProfile()
                _uiState.value = if (profile != null) {
                    UiState.Success(profile)
                } else {
                    UiState.Empty
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(
                    message = e.message ?: "Failed to load profile",
                    exception = e,
                )
            }
        }
    }
}
```

---

## Data Class UiState

Use a flat data class when multiple state dimensions coexist (data + loading + error) and the UI needs simultaneous access to all of them.

```kotlin
package {your.package}.presentation.user

import {your.package}.data.local.entity.AccountEntity

data class AccountUiState(
    val account: AccountEntity? = null,
    val isLoading: Boolean = false,
    val lastStatusCode: Int? = null,
    val lastMessage: String? = null,
)
```

When to use data class vs sealed interface:
- **Data class** -- the screen needs multiple concurrent state dimensions (loading + data + error). Fields are independently updated with `copy()`.
- **Sealed interface** -- states are mutually exclusive and each state has zero or few fields.

---

## StateFlow in ViewModel

The ViewModel owns a private MutableStateFlow and exposes a read-only StateFlow. All state mutations happen inside `viewModelScope.launch` blocks using `update {}`.

```kotlin
package {your.package}.presentation.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import {your.package}.data.repository.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccountViewModel(
    private val accountRepository: AccountRepository,
) : ViewModel() {

    private val _accountState = MutableStateFlow(AccountUiState())
    val accountState: StateFlow<AccountUiState> = _accountState.asStateFlow()

    init {
        viewModelScope.launch {
            accountRepository.observeAccount().collect { user ->
                _accountState.update { it.copy(account = user) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _accountState.update { it.copy(isLoading = true) }
            val result = accountRepository.callApi()
            _accountState.update {
                it.copy(
                    isLoading = false,
                    lastStatusCode = result.statusCode,
                    lastMessage = result.message,
                )
            }
        }
    }
}
```

Key patterns:
- `_accountState` is private; only the ViewModel can mutate it
- `accountState` is the public read-only view via `asStateFlow()`
- `update {}` is atomic and prevents lost concurrent updates
- `init {}` launches a coroutine that observes a Room Flow and updates state

---

## One-Shot Events

For events that must be consumed exactly once (navigation, snackbar, toast), use a `Channel` instead of `SharedFlow(replay=0)`. Channel buffers events until consumed, so events are not lost during configuration changes.

```kotlin
package {your.package}.presentation.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface AccountEvent {
    data class ShowSnackbar(val message: String) : AccountEvent
    data object NavigateToLogin : AccountEvent
}

class AccountViewModel(
    private val accountRepository: AccountRepository,
) : ViewModel() {

    private val _events = Channel<AccountEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun logout() {
        viewModelScope.launch {
            accountRepository.logout()
            _events.send(AccountEvent.NavigateToLogin)
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            val result = accountRepository.deleteAccount()
            if (result.isSuccess) {
                _events.send(AccountEvent.NavigateToLogin)
            } else {
                _events.send(AccountEvent.ShowSnackbar("Delete failed"))
            }
        }
    }
}
```

Collect events in Compose with `LaunchedEffect`:

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AccountScreen(viewModel: AccountViewModel) {
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is AccountEvent.ShowSnackbar -> { /* show snackbar */ }
                is AccountEvent.NavigateToLogin -> { /* navigate */ }
            }
        }
    }
}
```

---

## Combining Flows

When a screen depends on multiple data sources, combine them into a single StateFlow.

```kotlin
package {your.package}.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(
    private val userRepository: UserRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)

    val dashboardState: StateFlow<DashboardUiState> = combine(
        userRepository.observeUser(),
        settingsRepository.observeSettings(),
        _isRefreshing,
    ) { user, settings, isRefreshing ->
        DashboardUiState(
            userName = user?.name,
            isDarkMode = settings.isDarkMode,
            isRefreshing = isRefreshing,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(),
    )
}

data class DashboardUiState(
    val userName: String? = null,
    val isDarkMode: Boolean = false,
    val isRefreshing: Boolean = false,
)
```

The 5-second `WhileSubscribed` timeout keeps the upstream alive during configuration changes while cleaning up when the screen is truly gone.

**Use combine when:** the UI renders all data sources together in a single composable, you want a single `collectAsState()` call, or state dimensions are tightly coupled.

**Keep separate StateFlows when:** different parts of the screen collect independently, one flow emits much more frequently, or state dimensions are truly independent.

---

## PagingState Consumption

The `PagingState` enum and `toPaginationUiState()` extension are defined in the **kmp-paging** skill. This covers consumption patterns in composables.

```kotlin
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
```

---

## Configuration Change Survival

On Android, configuration changes destroy and recreate the Activity. State survives because:

1. **AndroidX ViewModel** -- retained by ViewModelStore across configuration changes
2. **StateFlow inside ViewModel** -- because the ViewModel survives, StateFlow and its current value survive too
3. **PagingData cached in viewModelScope** -- `cachedIn(viewModelScope)` prevents refetch on config change
4. **Channel events survive** -- buffered Channel retains unsent events across config changes since the ViewModel is retained

On iOS there are no configuration changes. The ViewModel lives as long as the SwiftUI/Compose view hierarchy retains it.

---

## Exposing StateFlow to iOS via SKIE

By default, Kotlin StateFlow is exported to Swift as an opaque type. SKIE transforms `StateFlow<T>` into a Swift AsyncSequence and provides a `.value` property.

```swift
let viewModel = AccountViewModel(accountRepository: repository)

// Direct value access
let currentState = viewModel.accountState.value

// Async iteration
for await state in viewModel.accountState {
    updateUI(state)
}
```

| Kotlin type | Swift type with SKIE |
|------------|---------------------|
| `StateFlow<T>` | AsyncSequence with `.value` property |
| Sealed interface | Swift enum with exhaustive switch |
| Data class UiState | Swift struct |

Refer to the SKIE bridge skill for full Gradle configuration.
