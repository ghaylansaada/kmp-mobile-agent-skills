# Presentation Layer: UiState, UiEvent, ViewModel, Screen

## UiState (sealed interface)

Every screen has a dedicated UiState sealed interface modeling all possible states.

```kotlin
package com.example.app.presentation.user

import com.example.app.data.local.entity.AccountEntity

sealed interface AccountUiState {
    data object Loading : AccountUiState
    data class Success(
        val account: AccountEntity? = null,
        val isRefreshing: Boolean = false,
    ) : AccountUiState
    data class Error(
        val statusCode: Int,
        val message: String? = null,
    ) : AccountUiState
}
```

Using `sealed interface` (not `sealed class`) enables exhaustive `when` branches. Adding a new subtype causes compile errors wherever the state is not handled, preventing forgotten UI states at runtime.

## UiEvent (sealed interface)

Screen actions flow through a single sealed interface for traceability.

```kotlin
package com.example.app.presentation.user

sealed interface AccountUiEvent {
    data object Refresh : AccountUiEvent
    data object Retry : AccountUiEvent
}
```

## ViewModel

```kotlin
package com.example.app.presentation.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.data.repository.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccountViewModel(
    private val accountRepository: AccountRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AccountUiState>(AccountUiState.Loading)
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            accountRepository.observeAccount().collect { account ->
                _uiState.update { AccountUiState.Success(account = account) }
            }
        }
    }

    fun onEvent(event: AccountUiEvent) {
        when (event) {
            AccountUiEvent.Refresh -> refresh()
            AccountUiEvent.Retry -> refresh()
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _uiState.update {
                when (it) {
                    is AccountUiState.Success -> it.copy(isRefreshing = true)
                    else -> AccountUiState.Loading
                }
            }
            val result = accountRepository.callApi()
            if (result.isSuccess) {
                // Flow from observeAccount() will emit the updated data
            } else {
                _uiState.update {
                    AccountUiState.Error(
                        statusCode = result.statusCode ?: 0,
                        message = result.message,
                    )
                }
            }
        }
    }
}
```

- `_uiState` is private; screen sees read-only `uiState` via `asStateFlow()`
- Init block starts observing local database immediately
- All async work uses `viewModelScope` -- never `GlobalScope`
- Events flow through `onEvent()` for a single entry point

## Screen Composable

```kotlin
package com.example.app.presentation.user

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AccountScreen(
    viewModel: AccountViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is AccountUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is AccountUiState.Success -> {
            // Render account content
            // Delegate actions: onRefresh = { viewModel.onEvent(AccountUiEvent.Refresh) }
            state.account?.let { Text(it.toString()) }
        }
        is AccountUiState.Error -> {
            // Render error with retry
            Text(stringResource(Res.string.error_with_code, state.statusCode, state.message.orEmpty()))
        }
    }
}
```

## Unidirectional Data Flow

```
User Action --> UiEvent --> ViewModel.onEvent() --> Repository --> Database/Network
                                                                       |
Database change --> Flow emission --> StateFlow update --> Screen recomposition
```

Screens never call repository or database directly. All mutations go through ViewModel via `UiEvent`. All state flows down via `StateFlow<UiState>`.

## Directory Structure

```
presentation/
    user/
        AccountScreen.kt
        AccountUiEvent.kt
        AccountUiState.kt
        AccountViewModel.kt
```
