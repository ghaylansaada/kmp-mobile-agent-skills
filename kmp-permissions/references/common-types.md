# Common Types (commonMain)

## PermissionType

```kotlin
// composeApp/src/commonMain/kotlin/{your/package}/core/permissions/PermissionType.kt
package {your.package}.core.permissions

enum class PermissionType {
    /** Camera access for photo/video capture. */
    CAMERA,

    /** Fine location access (GPS-level accuracy). */
    LOCATION,

    /** Push notification delivery permission. */
    NOTIFICATIONS,
}
```

## PermissionStatus

```kotlin
// composeApp/src/commonMain/kotlin/{your/package}/core/permissions/PermissionStatus.kt
package {your.package}.core.permissions

sealed interface PermissionStatus {
    /** Permission has been granted by the user. */
    data object Granted : PermissionStatus

    /** Permission was denied but can be requested again. */
    data object Denied : PermissionStatus

    /** Permission was permanently denied. User must enable it in system Settings. */
    data object PermanentlyDenied : PermissionStatus

    /** Permission has not been requested yet (iOS only -- Android conflates with Denied). */
    data object NotDetermined : PermissionStatus
}
```

## PermissionManager (expect declaration)

```kotlin
// composeApp/src/commonMain/kotlin/{your/package}/core/permissions/PermissionManager.kt
package {your.package}.core.permissions

expect class PermissionManager {
    suspend fun checkPermission(type: PermissionType): PermissionStatus
    suspend fun requestPermission(type: PermissionType): PermissionStatus
    fun openAppSettings()
}
```

## Rationale Dialog

```kotlin
// composeApp/src/commonMain/kotlin/{your/package}/core/permissions/PermissionRationaleDialog.kt
package {your.package}.core.permissions

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun PermissionRationaleDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(Res.string.action_grant_permission)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_not_now)) }
        },
    )
}
```

## PermissionRequestState (Compose helper)

```kotlin
// composeApp/src/commonMain/kotlin/{your/package}/core/permissions/PermissionRequestState.kt
package {your.package}.core.permissions

import androidx.compose.runtime.*

@Composable
fun rememberPermissionState(
    type: PermissionType,
    permissionManager: PermissionManager,
): PermissionRequestState {
    var status by remember { mutableStateOf<PermissionStatus>(PermissionStatus.NotDetermined) }
    var showRationale by remember { mutableStateOf(false) }

    LaunchedEffect(type) {
        status = permissionManager.checkPermission(type)
    }

    return PermissionRequestState(
        status = status,
        showRationale = showRationale,
        onRequest = {
            status = permissionManager.requestPermission(type)
            showRationale = status is PermissionStatus.Denied
        },
        onDismissRationale = { showRationale = false },
        onOpenSettings = { permissionManager.openAppSettings() },
    )
}

data class PermissionRequestState(
    val status: PermissionStatus,
    val showRationale: Boolean,
    val onRequest: suspend () -> Unit,
    val onDismissRationale: () -> Unit,
    val onOpenSettings: () -> Unit,
)
```
