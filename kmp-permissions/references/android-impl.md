# Android Implementation

## AndroidPermissionManager

```kotlin
// composeApp/src/androidMain/kotlin/{your/package}/core/permissions/AndroidPermissionManager.kt
package {your.package}.core.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual class PermissionManager(
    private val context: Context,
) {
    private var activity: ComponentActivity? = null
    private var permissionLauncher: ActivityResultLauncher<String>? = null
    private var pendingCallback: ((Boolean) -> Unit)? = null

    /**
     * Must be called in [ComponentActivity.onCreate] before the Activity
     * reaches the STARTED state. [ActivityResultContracts] registration
     * requires the CREATED lifecycle state.
     */
    fun bindToActivity(activity: ComponentActivity) {
        this.activity = activity
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            pendingCallback?.invoke(isGranted)
            pendingCallback = null
        }
    }

    fun unbindFromActivity() {
        permissionLauncher?.unregister()
        activity = null
        permissionLauncher = null
        pendingCallback = null
    }

    actual suspend fun checkPermission(type: PermissionType): PermissionStatus {
        val permission = type.toAndroidPermission() ?: return PermissionStatus.Granted
        return when {
            ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED -> PermissionStatus.Granted

            activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, permission)
            } == true -> PermissionStatus.Denied

            else -> PermissionStatus.NotDetermined
        }
    }

    actual suspend fun requestPermission(type: PermissionType): PermissionStatus {
        val permission = type.toAndroidPermission() ?: return PermissionStatus.Granted
        if (ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
        ) return PermissionStatus.Granted

        val launcher = permissionLauncher
            ?: error("bindToActivity() must be called before requestPermission()")

        return suspendCancellableCoroutine { continuation ->
            pendingCallback = { isGranted ->
                val status = when {
                    isGranted -> PermissionStatus.Granted
                    activity?.let {
                        ActivityCompat.shouldShowRequestPermissionRationale(it, permission)
                    } == true -> PermissionStatus.Denied
                    else -> PermissionStatus.PermanentlyDenied
                }
                if (continuation.isActive) continuation.resume(status)
            }
            continuation.invokeOnCancellation { pendingCallback = null }
            launcher.launch(permission)
        }
    }

    actual fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun PermissionType.toAndroidPermission(): String? = when (this) {
        PermissionType.CAMERA -> Manifest.permission.CAMERA
        PermissionType.LOCATION -> Manifest.permission.ACCESS_FINE_LOCATION
        PermissionType.NOTIFICATIONS -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.POST_NOTIFICATIONS
            } else {
                null
            }
        }
    }
}
```

## Activity Lifecycle Binding

```kotlin
// In MainActivity.kt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val config = PlatformConfig(Platform.OsType.ANDROID)
        val context = PlatformContext(this)
        initKoin(context = context, config = config)

        // Must happen before setContent (before STARTED state).
        getKoin().get<PermissionManager>().bindToActivity(this)
        setContent { App() }
    }

    override fun onDestroy() {
        super.onDestroy()
        getKoin().get<PermissionManager>().unbindFromActivity()
    }
}
```
