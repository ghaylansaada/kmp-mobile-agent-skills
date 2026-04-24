package {your.package}.ui.snippets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import {your.package}.data.remote.dto.error.ApiError
import {your.package}.data.remote.dto.error.ApiErrorCode
import {your.package}.ui.components.buttons.AppPrimaryButton
import {your.package}.ui.components.inputs.AppTextField
import {your.package}.ui.components.inputs.errorsForField
import {your.package}.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// -- Form UI State --

@Immutable
data class RegisterFormState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val fieldErrors: List<ApiError> = emptyList(),
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val generalError: String? = null,
)

// -- ViewModel with validation and submission --

class RegisterFormViewModel(
    // private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterFormState())
    val state: StateFlow<RegisterFormState> = _state.asStateFlow()

    fun onNameChange(value: String) {
        _state.value = _state.value.copy(
            name = value,
            fieldErrors = _state.value.fieldErrors.filter { it.path != "name" },
        )
    }

    fun onEmailChange(value: String) {
        _state.value = _state.value.copy(
            email = value,
            fieldErrors = _state.value.fieldErrors.filter { it.path != "email" },
        )
    }

    fun onPasswordChange(value: String) {
        _state.value = _state.value.copy(
            password = value,
            fieldErrors = _state.value.fieldErrors.filter { it.path != "password" },
        )
    }

    private fun validateLocally(): Boolean {
        val errors = mutableListOf<ApiError>()
        val current = _state.value
        if (current.name.isBlank()) {
            errors.add(
                ApiError("name", ApiErrorCode.REQUIRED_VIOLATION, "Name is required.", null, null),
            )
        }
        if (current.email.isBlank()) {
            errors.add(
                ApiError("email", ApiErrorCode.REQUIRED_VIOLATION, "Email is required.", null, null),
            )
        } else if (!current.email.contains("@")) {
            errors.add(
                ApiError("email", ApiErrorCode.EMAIL_FORMAT_VIOLATION, "Please enter a valid email address.", null, null),
            )
        }
        if (current.password.length < 8) {
            errors.add(
                ApiError("password", ApiErrorCode.PASSWORD_LENGTH_VIOLATION, "Password must be at least 8 characters.", null, null),
            )
        }
        if (errors.isNotEmpty()) {
            _state.value = current.copy(fieldErrors = errors)
            return false
        }
        return true
    }

    fun submit() {
        if (!validateLocally()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isSubmitting = true, generalError = null)
            // API call:
            // when (val result = authRepository.register(name, email, password)) {
            //     is ApiResult.Success -> _state.value = _state.value.copy(isSubmitting = false, isSuccess = true)
            //     is ApiResult.Error.ValidationError -> _state.value = _state.value.copy(isSubmitting = false, fieldErrors = result.errors)
            //     is ApiResult.Error -> _state.value = _state.value.copy(isSubmitting = false, generalError = result.message ?: "Registration failed.")
            // }
            delay(1000)
            _state.value = _state.value.copy(isSubmitting = false, isSuccess = true)
        }
    }
}

// -- Form Composable --

@Composable
fun RegisterForm(
    viewModel: RegisterFormViewModel,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    if (state.isSuccess) {
        onSuccess()
        return
    }

    Column(modifier = modifier.fillMaxWidth().padding(AppTheme.spacing.lg)) {
        Text(
            stringResource(Res.string.register_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(AppTheme.spacing.xl))
        AppTextField(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            label = stringResource(Res.string.register_label_name),
            fieldErrors = state.fieldErrors.errorsForField("name"),
            imeAction = ImeAction.Next,
            accessibilityLabel = stringResource(Res.string.register_label_name),
        )
        Spacer(Modifier.height(AppTheme.spacing.md))
        AppTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = stringResource(Res.string.register_label_email),
            fieldErrors = state.fieldErrors.errorsForField("email"),
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            accessibilityLabel = stringResource(Res.string.register_label_email),
        )
        Spacer(Modifier.height(AppTheme.spacing.md))
        AppTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = stringResource(Res.string.register_label_password),
            fieldErrors = state.fieldErrors.errorsForField("password"),
            isPassword = true,
            imeAction = ImeAction.Done,
            onImeAction = { viewModel.submit() },
            accessibilityLabel = stringResource(Res.string.register_label_password),
        )
        state.generalError?.let {
            Spacer(Modifier.height(AppTheme.spacing.md))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.height(AppTheme.spacing.xl))
        AppPrimaryButton(
            text = stringResource(Res.string.register_action_create),
            onClick = viewModel::submit,
            isLoading = state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
            accessibilityLabel = stringResource(Res.string.register_action_create),
        )
    }
}
