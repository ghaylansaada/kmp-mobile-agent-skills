# Build Configuration Source Files

## BuildEnvironment.kt (commonMain)

**File:** `config/BuildEnvironment.kt`

```kotlin
package {your.package}.config

import {your.package}.BuildKonfig

enum class BuildEnvironment(val label: String) {
    DEV("Development"),
    STAGING("Staging"),
    PROD("Production"),
    ;

    companion object {
        val current: BuildEnvironment
            get() = when (BuildKonfig.ENVIRONMENT) {
                "staging" -> STAGING
                "prod" -> PROD
                else -> DEV
            }
        val isDev: Boolean get() = current == DEV
        val isStaging: Boolean get() = current == STAGING
        val isProd: Boolean get() = current == PROD
    }
}
```

## FeatureFlags.kt (commonMain)

**File:** `config/FeatureFlags.kt`

```kotlin
package {your.package}.config

import {your.package}.BuildKonfig

object FeatureFlags {
    val loggingEnabled: Boolean = BuildKonfig.ENABLE_LOGGING
    val mockDataEnabled: Boolean = BuildKonfig.ENABLE_MOCK_DATA
    val analyticsEnabled: Boolean = BuildKonfig.ENABLE_ANALYTICS
    val crashReportingEnabled: Boolean = BuildKonfig.ENABLE_CRASH_REPORTING
    val networkInspectorEnabled: Boolean = BuildKonfig.ENABLE_NETWORK_INSPECTOR
}
```

## AppConfig.kt (commonMain)

**File:** `config/AppConfig.kt`

Wraps BuildKonfig to decouple the codebase from the code-generation layer:

```kotlin
package {your.package}.config

import {your.package}.BuildKonfig

object AppConfig {
    val BASE_URL: String = BuildKonfig.BASE_URL
    val APP_NAME: String = BuildKonfig.APP_NAME
    val API_TIMEOUT_SECONDS: Int = BuildKonfig.API_TIMEOUT_SECONDS
    val ENVIRONMENT: String = BuildKonfig.ENVIRONMENT
}
```

## Constants.kt (commonMain)

For values that do NOT change per environment or platform. Uses `const val` (unlike BuildKonfig which generates `val`):

```kotlin
package {your.package}.config

object Constants {
    const val DATABASE_NAME = "mobile_template.db"
    const val DATABASE_VERSION = 1
    const val DATASTORE_FILE_NAME = "settings.preferences_pb"
    const val MAX_RETRY_COUNT = 3
    const val PAGINATION_PAGE_SIZE = 20
}
```

## PlatformConstants -- expect/actual

For values that differ by PLATFORM (not environment). Use BuildKonfig for per-environment, expect/actual for per-platform:

```kotlin
// commonMain -- config/PlatformConstants.kt
package {your.package}.config

expect object PlatformConstants {
    val PLATFORM_NAME: String
    val DATABASE_DIR: String
}
```

```kotlin
// androidMain -- config/PlatformConstants.android.kt
package {your.package}.config

actual object PlatformConstants {
    actual val PLATFORM_NAME: String = "Android"
    actual val DATABASE_DIR: String = "" // resolved via Context at runtime
}
```

```kotlin
// iosMain -- config/PlatformConstants.ios.kt
package {your.package}.config

import platform.Foundation.NSHomeDirectory

actual object PlatformConstants {
    actual val PLATFORM_NAME: String = "iOS"
    actual val DATABASE_DIR: String = NSHomeDirectory() + "/Documents"
}
```
