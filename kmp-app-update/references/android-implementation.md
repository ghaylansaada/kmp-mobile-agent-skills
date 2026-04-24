# Android: Play Core In-App Updates

```kotlin
package {your.package}.core.update

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.isFlexibleUpdateAllowed
import com.google.android.play.core.ktx.isImmediateUpdateAllowed
import com.google.android.play.core.ktx.requestCompleteUpdate
import kotlinx.coroutines.tasks.await
import java.lang.ref.WeakReference

actual class PlatformAppUpdateManager(
    activity: Activity,
    private val forceUpdateMinVersion: String = "",
    private val updateResultLauncher: ActivityResultLauncher<IntentSenderRequest>? = null,
) {
    private val activityRef = WeakReference(activity)
    private val playUpdateManager = AppUpdateManagerFactory.create(activity)

    actual suspend fun checkForUpdate(): AppUpdateInfo {
        return try {
            val info = playUpdateManager.appUpdateInfo.await()
            when (info.updateAvailability()) {
                UpdateAvailability.UPDATE_AVAILABLE -> {
                    val activity = activityRef.get()
                    val currentVersion = activity
                        ?.packageManager
                        ?.getPackageInfo(activity.packageName, 0)
                        ?.versionName ?: "unknown"
                    val priority = if (
                        info.updatePriority() >= 4 ||
                        isVersionBelow(currentVersion, forceUpdateMinVersion)
                    ) UpdatePriority.CRITICAL else UpdatePriority.NORMAL

                    AppUpdateInfo.UpdateAvailable(
                        currentVersion = currentVersion,
                        availableVersion = info.availableVersionCode().toString(),
                        priority = priority,
                    )
                }
                else -> AppUpdateInfo.NoUpdate
            }
        } catch (e: Exception) {
            AppUpdateInfo.Error(e)
        }
    }

    actual suspend fun startUpdate(updateType: UpdateType) {
        val activity = activityRef.get() ?: return
        val info = playUpdateManager.appUpdateInfo.await()
        val playType = when (updateType) {
            UpdateType.IMMEDIATE -> AppUpdateType.IMMEDIATE
            UpdateType.FLEXIBLE -> AppUpdateType.FLEXIBLE
        }
        val allowed = when (updateType) {
            UpdateType.IMMEDIATE -> info.isImmediateUpdateAllowed
            UpdateType.FLEXIBLE -> info.isFlexibleUpdateAllowed
        }
        if (!allowed) return

        val options = AppUpdateOptions.newBuilder(playType).build()
        if (updateResultLauncher != null) {
            playUpdateManager.startUpdateFlowForResult(
                info,
                updateResultLauncher,
                options,
            )
        } else {
            @Suppress("DEPRECATION")
            playUpdateManager.startUpdateFlowForResult(
                info,
                activity,
                options,
                UPDATE_REQUEST_CODE,
            )
        }
    }

    actual suspend fun completeUpdate() {
        try {
            playUpdateManager.requestCompleteUpdate()
        } catch (_: Exception) {
            // Ignored: completeUpdate can fail if no update is pending
        }
    }

    companion object {
        const val UPDATE_REQUEST_CODE = 1001
    }
}
```

## Registering the ActivityResultLauncher

Register the launcher in `onCreate` before the activity is started. Pass it to `PlatformAppUpdateManager`:

```kotlin
class MainActivity : ComponentActivity() {
    private val updateResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            // User cancelled or update failed — handle gracefully
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Pass launcher when creating the manager
        val updateManager = PlatformAppUpdateManager(
            activity = this,
            forceUpdateMinVersion = "2.0.0",
            updateResultLauncher = updateResultLauncher,
        )
        getKoin().declare(updateManager)
    }
}
```

## Key Notes

- **WeakReference**: Activity is wrapped to prevent memory leaks. Always null-check `activityRef.get()` before use.
- **updatePriority()**: Play Console allows setting priority 0-5 when publishing. Priority >= 4 triggers force update.
- **AppUpdateInfo validity**: The object from `playUpdateManager.appUpdateInfo` expires quickly. Always fetch fresh immediately before `startUpdateFlowForResult`. Never cache.
- **ActivityResultLauncher**: Preferred over the deprecated `startUpdateFlowForResult(info, activity, options, requestCode)` overload. Must be registered in `onCreate`.
- **completeUpdate()**: Must be called after flexible downloads finish. Without it, the downloaded update sits idle until the next app restart.
- **isVersionBelow**: Imported from commonMain (`shared-types.md`). Not duplicated here.
