# Shared Analytics Interfaces (commonMain)

All types below belong in `{{your_package}}.core.analytics` in the `commonMain` sourceset.

## AnalyticsEvent Interface

```kotlin
interface AnalyticsEvent {
    val name: String              // snake_case, max 40 chars — Firebase silently drops longer names
    val parameters: Map<String, Any>  // keys max 40 chars, string values max 100 chars
        get() = emptyMap()
}

data class ScreenViewEvent(
    val screenName: String,
    val screenClass: String? = null,
) : AnalyticsEvent {
    override val name: String = "screen_view"
    override val parameters: Map<String, Any>
        get() = buildMap {
            put("screen_name", screenName)
            screenClass?.let { put("screen_class", it) }
        }
}

data class UserActionEvent(
    val action: String,
    val target: String? = null,
    val value: String? = null,
) : AnalyticsEvent {
    override val name: String = "user_action"
    override val parameters: Map<String, Any>
        get() = buildMap {
            put("action", action)
            target?.let { put("target", it) }
            value?.let { put("value", it) }
        }
}
```

## AnalyticsTracker Interface

```kotlin
interface AnalyticsTracker {
    fun trackEvent(event: AnalyticsEvent)
    fun trackEvent(name: String, params: Map<String, Any> = emptyMap())
    fun setUserProperty(name: String, value: String?)
    fun setUserId(userId: String?)
    fun trackScreenView(screenName: String, screenClass: String? = null) {
        trackEvent(ScreenViewEvent(screenName, screenClass))
    }
}
```

## CrashReporter Interface

```kotlin
interface CrashReporter {
    fun recordException(throwable: Throwable, message: String? = null)
    fun logBreadcrumb(message: String)   // max 1024 chars, appears in crash report timeline
    fun setCustomKey(key: String, value: String)
    fun setUserId(userId: String?)
    fun setCrashlyticsCollectionEnabled(enabled: Boolean)
}
```

## expect Declarations

```kotlin
expect fun createAnalyticsTracker(): AnalyticsTracker
expect fun createCrashReporter(): CrashReporter
```
