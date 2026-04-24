// Snippet: Compose permission request patterns for KMP
// In production, replace all hardcoded strings with stringResource(Res.string.*).

package {your.package}.core.permissions.examples

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import {your.package}.ui.theme.AppTheme
import mobile.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import {your.package}.core.permissions.PermissionManager
import {your.package}.core.permissions.PermissionStatus
import {your.package}.core.permissions.PermissionType
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

// --- Pattern 1: Simple Permission Request ---

@Composable
fun SimpleCameraPermission(
    permissionManager: PermissionManager = koinInject(),
) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<PermissionStatus>(PermissionStatus.NotDetermined) }

    LaunchedEffect(Unit) {
        status = permissionManager.checkPermission(PermissionType.CAMERA)
    }

    when (status) {
        is PermissionStatus.Granted -> Text(stringResource(Res.string.camera_ready)) // "Camera ready!"
        else -> Button(onClick = {
            scope.launch {
                status = permissionManager.requestPermission(PermissionType.CAMERA)
            }
        }) { Text(stringResource(Res.string.camera_enable)) } // "Enable Camera"
    }
}

// --- Pattern 2: Permission with Rationale Dialog ---

@Composable
fun LocationWithRationale(
    permissionManager: PermissionManager = koinInject(),
) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<PermissionStatus>(PermissionStatus.NotDetermined) }
    var showRationale by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        status = permissionManager.checkPermission(PermissionType.LOCATION)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(AppTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (status) {
            is PermissionStatus.Granted -> Text(stringResource(Res.string.location_granted))
            is PermissionStatus.Denied -> {
                Text(stringResource(Res.string.location_rationale_short))
                Spacer(modifier = Modifier.height(AppTheme.spacing.sm))
                Button(onClick = { showRationale = true }) {
                    Text(stringResource(Res.string.location_why_needed))
                }
            }
            is PermissionStatus.PermanentlyDenied -> {
                Text(stringResource(Res.string.location_permanently_denied))
                Button(onClick = { permissionManager.openAppSettings() }) {
                    Text(stringResource(Res.string.action_open_settings))
                }
            }
            is PermissionStatus.NotDetermined -> {
                Button(onClick = {
                    scope.launch {
                        status = permissionManager.requestPermission(PermissionType.LOCATION)
                        if (status is PermissionStatus.Denied) showRationale = true
                    }
                }) { Text(stringResource(Res.string.location_enable)) }
            }
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text(stringResource(Res.string.location_permission_title)) },
            text = {
                Text(stringResource(Res.string.location_rationale_detail))
            },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    scope.launch {
                        status = permissionManager.requestPermission(PermissionType.LOCATION)
                    }
                }) { Text(stringResource(Res.string.action_grant_access)) }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) { Text(stringResource(Res.string.action_not_now)) }
            },
        )
    }
}

// --- Pattern 3: Notification Permission on First Action ---

@Composable
fun NotificationOptIn(
    permissionManager: PermissionManager = koinInject(),
    onPermissionResult: (PermissionStatus) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<PermissionStatus>(PermissionStatus.NotDetermined) }

    LaunchedEffect(Unit) {
        status = permissionManager.checkPermission(PermissionType.NOTIFICATIONS)
    }

    when (status) {
        is PermissionStatus.Granted -> Text(stringResource(Res.string.notifications_enabled))
        is PermissionStatus.PermanentlyDenied -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(Res.string.notifications_disabled_settings))
                Spacer(modifier = Modifier.height(AppTheme.spacing.sm))
                Button(onClick = { permissionManager.openAppSettings() }) {
                    Text(stringResource(Res.string.action_open_settings))
                }
            }
        }
        else -> Button(onClick = {
            scope.launch {
                status = permissionManager.requestPermission(PermissionType.NOTIFICATIONS)
                onPermissionResult(status)
            }
        }) { Text(stringResource(Res.string.notifications_enable)) }
    }
}
