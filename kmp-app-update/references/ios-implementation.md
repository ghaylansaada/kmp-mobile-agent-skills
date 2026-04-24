# iOS: iTunes Lookup API + App Store Redirect

```kotlin
package {your.package}.core.update

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSJSONSerialization
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.dataTaskWithURL
import platform.StoreKit.SKStoreReviewController
import platform.UIKit.UIApplication
import kotlin.coroutines.resume

actual class PlatformAppUpdateManager(
    private val appStoreId: String,
    private val bundleId: String = NSBundle.mainBundle.bundleIdentifier ?: "",
    private val forceUpdateMinVersion: String = "",
) {
    actual suspend fun checkForUpdate(): AppUpdateInfo = withContext(Dispatchers.IO) {
        try {
            val storeVersion = fetchAppStoreVersion()
                ?: return@withContext AppUpdateInfo.NoUpdate
            val currentVersion = NSBundle.mainBundle
                .objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
                ?: return@withContext AppUpdateInfo.Error(
                    IllegalStateException("Cannot read current app version"),
                )
            if (isNewerVersion(storeVersion, currentVersion)) {
                val priority = if (isVersionBelow(currentVersion, forceUpdateMinVersion)) {
                    UpdatePriority.CRITICAL
                } else {
                    UpdatePriority.NORMAL
                }
                AppUpdateInfo.UpdateAvailable(currentVersion, storeVersion, priority)
            } else {
                AppUpdateInfo.NoUpdate
            }
        } catch (e: Exception) {
            AppUpdateInfo.Error(e)
        }
    }

    actual suspend fun startUpdate(updateType: UpdateType) {
        NSURL.URLWithString("https://apps.apple.com/app/id$appStoreId")
            ?.let { UIApplication.sharedApplication.openURL(it) }
    }

    actual suspend fun completeUpdate() { /* no-op on iOS */ }

    fun requestReview() {
        SKStoreReviewController.requestReview()
    }

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun fetchAppStoreVersion(): String? =
        suspendCancellableCoroutine { cont ->
            val url = NSURL.URLWithString(
                "https://itunes.apple.com/lookup?bundleId=$bundleId",
            ) ?: run {
                cont.resume(null)
                return@suspendCancellableCoroutine
            }
            val task = NSURLSession.sharedSession.dataTaskWithURL(url) { data, _, error ->
                if (error != null || data == null) {
                    cont.resume(null)
                    return@dataTaskWithURL
                }
                cont.resume(parseVersion(data))
            }
            cont.invokeOnCancellation { task.cancel() }
            task.resume()
        }

    @OptIn(ExperimentalForeignApi::class)
    @Suppress("UNCHECKED_CAST")
    private fun parseVersion(data: NSData): String? = try {
        val json = NSJSONSerialization.JSONObjectWithData(data, 0u, null)
                as? Map<Any?, Any?>
        val results = json?.get("results") as? List<Map<Any?, Any?>>
        results?.firstOrNull()?.get("version") as? String
    } catch (_: Exception) {
        null
    }
}
```

## Key Notes

- **iTunes Lookup API** has undocumented rate limits. Cache the result and check at most once per app session.
- **bundleId** must match the App Store listing exactly. If recently published, the app may not be indexed yet. Alternative: use numeric App Store ID `https://itunes.apple.com/lookup?id=123456789`.
- **SKStoreReviewController.requestReview()** is rate-limited by iOS. The system decides whether to show the prompt. No callback exists. Call after positive user actions, never on every launch.
- **No in-app update equivalent on iOS.** The only option is checking the store version and redirecting to the App Store URL. The update itself is handled entirely by the App Store.
- **isVersionBelow / isNewerVersion**: Imported from commonMain (`shared-types.md`). Not duplicated here.
- **Foundation interop casts**: `NSJSONSerialization` returns Objective-C collection types that bridge to Kotlin `Map<Any?, Any?>` and `List<*>`. Use `as?` with nullable key types to avoid `ClassCastException`.
