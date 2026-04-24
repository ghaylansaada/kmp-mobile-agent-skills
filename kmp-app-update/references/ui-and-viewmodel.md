# UI Components and ViewModel

> In production, replace all hardcoded UI strings with `stringResource(Res.string.*)` from Compose Multiplatform resources.

## AppUpdateUiState

```kotlin
package {your.package}.presentation.update

sealed interface AppUpdateUiState {
    data object Idle : AppUpdateUiState
    data object Checking : AppUpdateUiState
    data class ForceUpdate(
        val currentVersion: String,
        val requiredVersion: String,
    ) : AppUpdateUiState
    data class OptionalUpdate(
        val availableVersion: String,
    ) : AppUpdateUiState
}
```

## AppUpdateViewModel

```kotlin
package {your.package}.presentation.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import {your.package}.core.update.AppUpdateInfo
import {your.package}.core.update.AppUpdateManagerContract
import {your.package}.core.update.UpdatePriority
import {your.package}.core.update.UpdateType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppUpdateViewModel(
    private val updateManager: AppUpdateManagerContract,
) : ViewModel() {
    private val _updateState = MutableStateFlow<AppUpdateUiState>(AppUpdateUiState.Idle)
    val updateState: StateFlow<AppUpdateUiState> = _updateState.asStateFlow()

    fun checkForUpdate() {
        viewModelScope.launch {
            _updateState.value = AppUpdateUiState.Checking
            when (val info = updateManager.checkForUpdate()) {
                is AppUpdateInfo.UpdateAvailable -> {
                    _updateState.value = when (info.priority) {
                        UpdatePriority.CRITICAL -> AppUpdateUiState.ForceUpdate(
                            currentVersion = info.currentVersion,
                            requiredVersion = info.availableVersion,
                        )
                        UpdatePriority.NORMAL -> AppUpdateUiState.OptionalUpdate(
                            availableVersion = info.availableVersion,
                        )
                    }
                }
                is AppUpdateInfo.Error,
                AppUpdateInfo.NoUpdate,
                -> _updateState.value = AppUpdateUiState.Idle
            }
        }
    }

    fun startUpdate(type: UpdateType = UpdateType.FLEXIBLE) {
        viewModelScope.launch { updateManager.startUpdate(type) }
    }

    fun startForceUpdate() {
        viewModelScope.launch { updateManager.startUpdate(UpdateType.IMMEDIATE) }
    }

    fun dismissBanner() {
        _updateState.value = AppUpdateUiState.Idle
    }
}
```

## ForceUpdateScreen

Blocks the entire app for critical updates. Must be shown as root composable. Uses multiplatform Compose APIs only.

```kotlin
package {your.package}.presentation.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ForceUpdateScreen(
    currentVersion: String,
    requiredVersion: String,
    onUpdateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(AppTheme.spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.update_required),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(AppTheme.spacing.lg))
        Text(
            text = stringResource(Res.string.update_required_message),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(AppTheme.spacing.sm))
        Text(
            text = stringResource(Res.string.version_info, currentVersion, requiredVersion),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(AppTheme.spacing.xxl))
        Button(onClick = onUpdateClick) {
            Text(stringResource(Res.string.update_now))
        }
    }
}
```

Back-press interception is platform-specific. On Android, wrap this composable with `BackHandler(enabled = true) {}` in the Android source set or in `App.kt` gated by platform. See `references/integration.md` for the wiring.

## UpdateBanner

Non-blocking banner for optional updates.

```kotlin
package {your.package}.presentation.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun UpdateBanner(
    availableVersion: String,
    onUpdateClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.sm),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.version_available, availableVersion),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.later)) }
            TextButton(onClick = onUpdateClick) { Text(stringResource(Res.string.update)) }
        }
    }
}
```
