# Platform Implementations (Android + iOS)

## Android (androidMain)

```kotlin
package {{your_package}}.core.analytics

import android.os.Bundle
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.crashlytics

class AndroidAnalyticsTracker : AnalyticsTracker {
    private val firebaseAnalytics = Firebase.analytics

    override fun trackEvent(event: AnalyticsEvent) = trackEvent(event.name, event.parameters)

    override fun trackEvent(name: String, params: Map<String, Any>) {
        val bundle = Bundle().apply {
            params.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Float -> putFloat(key, value)
                    is Boolean -> putBoolean(key, value)
                    else -> putString(key, value.toString())
                }
            }
        }
        firebaseAnalytics.logEvent(name, bundle)
    }

    override fun setUserProperty(name: String, value: String?) =
        firebaseAnalytics.setUserProperty(name, value)

    override fun setUserId(userId: String?) {
        firebaseAnalytics.setUserId(userId)
    }
}

class AndroidCrashReporter : CrashReporter {
    private val crashlytics = Firebase.crashlytics

    override fun recordException(throwable: Throwable, message: String?) {
        message?.let { crashlytics.log(it) }
        crashlytics.recordException(throwable)
    }

    override fun logBreadcrumb(message: String) = crashlytics.log(message)

    override fun setCustomKey(key: String, value: String) = crashlytics.setCustomKey(key, value)

    override fun setUserId(userId: String?) = crashlytics.setUserId(userId ?: "")

    override fun setCrashlyticsCollectionEnabled(enabled: Boolean) {
        crashlytics.setCrashlyticsCollectionEnabled(enabled)
    }
}

actual fun createAnalyticsTracker(): AnalyticsTracker = AndroidAnalyticsTracker()
actual fun createCrashReporter(): CrashReporter = AndroidCrashReporter()
```

## iOS (iosMain)

```kotlin
package {{your_package}}.core.analytics

import cocoapods.FirebaseAnalytics.FIRAnalytics
import cocoapods.FirebaseCrashlytics.FIRCrashlytics
import platform.Foundation.NSError
import platform.Foundation.NSLocalizedDescriptionKey
import platform.Foundation.NSNumber

class IosAnalyticsTracker : AnalyticsTracker {
    override fun trackEvent(event: AnalyticsEvent) = trackEvent(event.name, event.parameters)

    override fun trackEvent(name: String, params: Map<String, Any>) {
        val nsParams = params.mapValues { (_, value) ->
            when (value) {
                is String -> value as Any
                is Int -> NSNumber(int = value) as Any
                is Long -> NSNumber(longLong = value) as Any
                is Double -> NSNumber(double = value) as Any
                is Float -> NSNumber(float = value) as Any
                is Boolean -> NSNumber(bool = value) as Any
                else -> value.toString() as Any
            }
        }
        FIRAnalytics.logEventWithName(name, parameters = nsParams)
    }

    override fun setUserProperty(name: String, value: String?) =
        FIRAnalytics.setUserPropertyString(value, forName = name)

    override fun setUserId(userId: String?) = FIRAnalytics.setUserID(userId)
}

class IosCrashReporter : CrashReporter {
    private val crashlytics = FIRCrashlytics.crashlytics()

    override fun recordException(throwable: Throwable, message: String?) {
        message?.let { crashlytics.log(it) }
        crashlytics.recordError(throwable.toNSError())
    }

    override fun logBreadcrumb(message: String) = crashlytics.log(message)

    override fun setCustomKey(key: String, value: String) =
        crashlytics.setCustomValue(value, forKey = key)

    override fun setUserId(userId: String?) = crashlytics.setUserID(userId ?: "")

    override fun setCrashlyticsCollectionEnabled(enabled: Boolean) {
        crashlytics.setCrashlyticsCollectionEnabled(enabled)
    }
}

// Preserves Kotlin stack trace in iOS crash reports.
// Without this, all Kotlin exceptions appear as "KotlinException" with no stack.
private fun Throwable.toNSError(): NSError = NSError(
    domain = this::class.simpleName ?: "KotlinException",
    code = 0,
    userInfo = mapOf(
        NSLocalizedDescriptionKey to (message ?: "Unknown error"),
        "KotlinStackTrace" to stackTraceToString(),
    ),
)

actual fun createAnalyticsTracker(): AnalyticsTracker = IosAnalyticsTracker()
actual fun createCrashReporter(): CrashReporter = IosCrashReporter()
```

## Alternative: Swift Wrapper

If CocoaPods interop causes build issues with Firebase, create a Swift wrapper in the iOS project. Define a `FirebaseWrapper` class in `iosApp/iosApp/FirebaseWrapper.swift` with `@objc` static methods for `logEvent`, `setUserId`, `recordError`, `logBreadcrumb`, `setCustomKey`, and `setCrashlyticsEnabled`. Then call `FirebaseWrapper.logEvent(name, parameters)` from the Kotlin iOS implementation instead of using `FIRAnalytics` directly.
