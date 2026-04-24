# Shared Types and Interfaces (commonMain)

## AppUpdateInfo

```kotlin
package {your.package}.core.update

sealed interface AppUpdateInfo {
    data object NoUpdate : AppUpdateInfo
    data class UpdateAvailable(
        val currentVersion: String,
        val availableVersion: String,
        val priority: UpdatePriority,
    ) : AppUpdateInfo
    data class Error(val exception: Throwable) : AppUpdateInfo
}

enum class UpdatePriority { NORMAL, CRITICAL }

enum class UpdateType {
    IMMEDIATE,  // Blocks app while updating (Android only, critical updates)
    FLEXIBLE,   // Downloads in background (Android), opens App Store (iOS)
}
```

## PlatformAppUpdateManager (expect)

```kotlin
package {your.package}.core.update

expect class PlatformAppUpdateManager {
    suspend fun checkForUpdate(): AppUpdateInfo
    suspend fun startUpdate(updateType: UpdateType = UpdateType.FLEXIBLE)
    suspend fun completeUpdate()
}
```

## Testable Contract Interface

Wraps `PlatformAppUpdateManager` for testability. In tests, use `FakeAppUpdateManager` instead.

```kotlin
package {your.package}.core.update

interface AppUpdateManagerContract {
    suspend fun checkForUpdate(): AppUpdateInfo
    suspend fun startUpdate(updateType: UpdateType = UpdateType.FLEXIBLE)
    suspend fun completeUpdate()
}

class AppUpdateManagerWrapper(
    private val delegate: PlatformAppUpdateManager,
) : AppUpdateManagerContract {
    override suspend fun checkForUpdate() = delegate.checkForUpdate()
    override suspend fun startUpdate(updateType: UpdateType) = delegate.startUpdate(updateType)
    override suspend fun completeUpdate() = delegate.completeUpdate()
}
```

## Version Comparison Utility

Shared helper used by both Android and iOS actual implementations. Lives in commonMain to avoid duplication.

```kotlin
package {your.package}.core.update

internal fun isVersionBelow(current: String, minimum: String): Boolean {
    if (minimum.isBlank()) return false
    val c = current.split(".").mapNotNull { it.toIntOrNull() }
    val m = minimum.split(".").mapNotNull { it.toIntOrNull() }
    for (i in 0 until maxOf(c.size, m.size)) {
        val cv = c.getOrElse(i) { 0 }
        val mv = m.getOrElse(i) { 0 }
        if (cv < mv) return true
        if (cv > mv) return false
    }
    return false
}

internal fun isNewerVersion(store: String, current: String): Boolean {
    val s = store.split(".").mapNotNull { it.toIntOrNull() }
    val c = current.split(".").mapNotNull { it.toIntOrNull() }
    for (i in 0 until maxOf(s.size, c.size)) {
        val sv = s.getOrElse(i) { 0 }
        val cv = c.getOrElse(i) { 0 }
        if (sv > cv) return true
        if (sv < cv) return false
    }
    return false
}
```
