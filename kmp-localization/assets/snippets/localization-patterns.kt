package {your.package}.ui.examples

// Localized UI component patterns.
// For LocaleManager/AppTheme, see references/locale-manager.md.
// For ErrorMessageResolver, see references/error-resolution.md.

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import {your.package}.ui.theme.AppTheme
import kotlinx.coroutines.launch
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.action_cancel
import mobile.composeapp.generated.resources.action_retry
import mobile.composeapp.generated.resources.app_name
import mobile.composeapp.generated.resources.error_network
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import {your.package}.core.localization.LocaleManager

// --- Locale Picker ---

@Composable
fun LocalePicker(localeManager: LocaleManager, modifier: Modifier = Modifier) {
    val currentLocale by localeManager.currentLocale.collectAsState()
    val scope = rememberCoroutineScope()

    Column(modifier = modifier) {
        localeManager.supportedLocales().forEach { locale ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { scope.launch { localeManager.setLocale(locale.tag) } }
                    .padding(vertical = AppTheme.spacing.sm, horizontal = AppTheme.spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = currentLocale == locale.tag,
                    onClick = { scope.launch { localeManager.setLocale(locale.tag) } },
                )
                Text(text = locale.displayName, modifier = Modifier.padding(start = AppTheme.spacing.sm))
            }
        }
    }
}

// --- Localized Error Dialog ---

@Composable
fun ErrorDialog(message: String, onDismiss: () -> Unit, onRetry: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(Res.string.app_name)) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = onRetry) { Text(stringResource(Res.string.action_retry)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        },
    )
}

// --- Network Error Snackbar ---

@Composable
fun NetworkErrorSnackbar(onRetry: () -> Unit) {
    Snackbar(
        action = {
            TextButton(onClick = onRetry) { Text(stringResource(Res.string.action_retry)) }
        },
    ) {
        Text(text = stringResource(Res.string.error_network))
    }
}

// --- Onboarding Page with StringResource Parameter ---

@Composable
fun OnboardingPage(
    titleRes: StringResource,
    descriptionRes: StringResource,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(AppTheme.spacing.xl)) {
        Text(text = stringResource(titleRes), style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(AppTheme.spacing.sm))
        Text(text = stringResource(descriptionRes), style = MaterialTheme.typography.bodyMedium)
    }
}
