# Offline UX: ViewModel and Banner

## ConnectivityViewModel

```kotlin
// composeApp/src/commonMain/kotlin/{your/package}/presentation/common/ConnectivityViewModel.kt
package {your.package}.presentation.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import {your.package}.core.connectivity.ConnectivityObserver
import {your.package}.core.connectivity.ConnectivityStatus
import {your.package}.core.connectivity.OfflineRequestQueue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ConnectivityUiState(
    val isOnline: Boolean = true,
    val pendingRequestCount: Int = 0,
    val showReconnectedBanner: Boolean = false,
)

class ConnectivityViewModel(
    private val connectivityObserver: ConnectivityObserver,
    private val requestQueue: OfflineRequestQueue,
) : ViewModel() {

    private val _showReconnected = MutableStateFlow(false)

    val state: StateFlow<ConnectivityUiState> = combine(
        connectivityObserver.observe(),
        requestQueue.pendingCount,
        _showReconnected,
    ) { status, pendingCount, reconnected ->
        ConnectivityUiState(
            isOnline = status is ConnectivityStatus.Available,
            pendingRequestCount = pendingCount,
            showReconnectedBanner = reconnected,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ConnectivityUiState(),
    )

    init {
        viewModelScope.launch {
            var wasOffline = false
            connectivityObserver.observe().collect { status ->
                if (status is ConnectivityStatus.Available && wasOffline) {
                    _showReconnected.value = true
                    delay(3_000)
                    _showReconnected.value = false
                }
                wasOffline = status !is ConnectivityStatus.Available
            }
        }
    }

    fun dismissReconnectedBanner() {
        _showReconnected.value = false
    }
}
```

## ConnectivityBanner

```kotlin
// composeApp/src/commonMain/kotlin/{your/package}/ui/components/ConnectivityBanner.kt
package {your.package}.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import {your.package}.presentation.common.ConnectivityViewModel
import org.koin.compose.koinInject

@Composable
fun ConnectivityBanner(
    viewModel: ConnectivityViewModel = koinInject(),
) {
    val state by viewModel.state.collectAsState()

    AnimatedVisibility(
        visible = !state.isOnline,
        enter = slideInVertically(),
        exit = slideOutVertically(),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.you_are_offline),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (state.pendingRequestCount > 0) {
                    Spacer(Modifier.width(AppTheme.spacing.sm))
                    Text(
                        text = stringResource(Res.string.pending_count, state.pendingRequestCount),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }

    AnimatedVisibility(
        visible = state.showReconnectedBanner,
        enter = slideInVertically(),
        exit = slideOutVertically(),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.sm),
        ) {
            Text(
                text = stringResource(Res.string.back_online),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
```

Place `ConnectivityBanner()` at the top of your root layout (inside `App()` or the main scaffold).
