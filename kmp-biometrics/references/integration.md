# Biometrics Integration

## Koin DI Module

**File:** `di/modules/BiometricModule.kt`

```kotlin
package {your.package}.di.modules

import {your.package}.core.biometrics.BiometricAuthenticator
import {your.package}.core.biometrics.SecureCredentialStore
import org.koin.dsl.module

fun biometricModule() = module {
    single<BiometricAuthenticator> { BiometricAuthenticator(context = get()) }
    single<SecureCredentialStore> { SecureCredentialStore(context = get()) }
}
```

## Feature Gating: ViewModel

**File:** `presentation/payment/PaymentViewModel.kt`

```kotlin
package {your.package}.presentation.payment

import {your.package}.core.biometrics.BiometricAuthenticator
import {your.package}.core.biometrics.BiometricResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PaymentUiState(
    val isAuthenticated: Boolean = false,
    val biometricAvailable: Boolean = false,
    val errorMessage: String? = null,
)

class PaymentViewModel(
    private val biometricAuthenticator: BiometricAuthenticator,
) : ViewModel() {
    private val _state = MutableStateFlow(PaymentUiState())
    val state: StateFlow<PaymentUiState> = _state.asStateFlow()

    init {
        _state.update {
            it.copy(biometricAvailable = biometricAuthenticator.canAuthenticate())
        }
    }

    fun authenticateForPayment() {
        viewModelScope.launch {
            val result = biometricAuthenticator.authenticate(
                title = getString(Res.string.biometric_title_confirm_payment),
                subtitle = getString(Res.string.biometric_subtitle_authenticate_to_proceed),
                allowDeviceCredential = true,
            )
            _state.update { current ->
                when (result) {
                    is BiometricResult.Success -> current.copy(
                        isAuthenticated = true,
                        errorMessage = null,
                    )
                    is BiometricResult.Cancelled -> current.copy(errorMessage = null)
                    is BiometricResult.NotAvailable -> current.copy(
                        errorMessage = getString(Res.string.biometric_not_available),
                    )
                    is BiometricResult.NotEnrolled -> current.copy(
                        errorMessage = getString(Res.string.biometric_setup_in_settings),
                    )
                    is BiometricResult.Failure -> current.copy(
                        errorMessage = result.message,
                    )
                }
            }
        }
    }
}
```

## Feature Gating: Screen

**File:** `presentation/payment/PaymentScreen.kt`

```kotlin
package {your.package}.presentation.payment

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import {your.package}.ui.theme.AppTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PaymentScreen(viewModel: PaymentViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier.fillMaxSize().padding(AppTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.isAuthenticated) {
            Text(stringResource(Res.string.payment_form_placeholder))
        } else {
            Text(stringResource(Res.string.authentication_required), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(AppTheme.spacing.lg))
            Button(onClick = { viewModel.authenticateForPayment() }) {
                Text(stringResource(Res.string.action_authenticate))
            }
            state.errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
```

## Secure Session Manager

**File:** `core/biometrics/SecureSessionManager.kt`

```kotlin
package {your.package}.core.biometrics

class SecureSessionManager(
    private val authenticator: BiometricAuthenticator,
    private val credentialStore: SecureCredentialStore,
) {
    fun storeSessionToken(token: String) {
        credentialStore.store("session_token", token)
    }

    suspend fun retrieveSessionToken(): String? {
        val result = authenticator.authenticate(
            title = getString(Res.string.biometric_title_session_access),
            subtitle = getString(Res.string.biometric_subtitle_authenticate_to_resume),
        )
        return if (result is BiometricResult.Success) {
            credentialStore.retrieve("session_token")
        } else {
            null
        }
    }

    fun clearSession() {
        credentialStore.delete("session_token")
    }
}
```

## Re-authentication After Background Timeout

**File:** `core/biometrics/BiometricSessionGuard.kt`

```kotlin
package {your.package}.core.biometrics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock

class BiometricSessionGuard(
    private val authenticator: BiometricAuthenticator,
    private val timeoutMinutes: Long = 5,
) {
    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var lastAuthenticatedAt = Clock.System.now().toEpochMilliseconds()

    fun onAppResumed() {
        val elapsed = Clock.System.now().toEpochMilliseconds() - lastAuthenticatedAt
        if (elapsed > timeoutMinutes * 60_000) {
            _isLocked.value = true
        }
    }

    suspend fun unlock(): BiometricResult {
        val result = authenticator.authenticate(
            title = getString(Res.string.biometric_title_welcome_back),
            subtitle = getString(Res.string.biometric_subtitle_authenticate_to_continue),
        )
        if (result is BiometricResult.Success) {
            _isLocked.value = false
            lastAuthenticatedAt = Clock.System.now().toEpochMilliseconds()
        }
        return result
    }
}
```
