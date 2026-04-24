# Core Logging

## AppLogger

```kotlin
package {your.package}.core.logging

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.StaticConfig

object AppLogger {
    private var baseLogger: Logger = Logger(
        config = StaticConfig(minSeverity = Severity.Verbose),
        tag = "App",
    )

    fun configure(minSeverity: Severity, defaultTag: String = "App") {
        baseLogger = Logger(
            config = StaticConfig(minSeverity = minSeverity),
            tag = defaultTag,
        )
    }

    fun withTag(tag: String): Logger = baseLogger.withTag(tag)
    fun logger(): Logger = baseLogger
}
```

## LogConfig

```kotlin
package {your.package}.core.logging

import co.touchlab.kermit.Severity

object LogConfig {
    fun minSeverity(isDebug: Boolean): Severity =
        if (isDebug) Severity.Verbose else Severity.Assert
}
```

Debug: all levels visible. Release: `Severity.Assert` suppresses everything (Assert is never used for regular logging).

## Koin Module

```kotlin
package {your.package}.di.modules

import co.touchlab.kermit.Logger
import {your.package}.core.logging.AppLogger
import {your.package}.core.logging.LogConfig
import org.koin.dsl.module

fun loggingModule(isDebug: Boolean) = module {
    single {
        AppLogger.configure(minSeverity = LogConfig.minSeverity(isDebug))
        AppLogger.logger()
    }
}
```

## Tagged Logger Extension

```kotlin
package {your.package}.core.logging

import co.touchlab.kermit.Logger
import org.koin.core.component.KoinComponent

inline fun <reified T : KoinComponent> T.taggedLogger(): Lazy<Logger> {
    val tag = T::class.simpleName ?: "Unknown"
    return lazy { AppLogger.withTag(tag) }
}

fun taggedLogger(tag: String): Logger = AppLogger.withTag(tag)
```

## Ktor HTTP Logging

```kotlin
package {your.package}.core.network

import {your.package}.core.logging.AppLogger
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging

private val httpLogger = AppLogger.withTag("HttpClient")

fun HttpClientConfig<*>.installKermitLogging() {
    install(Logging) {
        logger = object : io.ktor.client.plugins.logging.Logger {
            override fun log(message: String) {
                httpLogger.d { message }
            }
        }
        level = LogLevel.HEADERS
    }
}
```

Use `LogLevel.HEADERS`, NOT `LogLevel.ALL`. ALL logs request/response bodies including auth tokens.

## Transfer Task Logging

```kotlin
package {your.package}.core.transfer.core

import co.touchlab.kermit.Logger
import {your.package}.core.logging.AppLogger

fun transferLogger(taskType: String): Logger = AppLogger.withTag("Transfer:$taskType")

fun Logger.logStateTransition(
    taskId: String,
    from: String,
    to: String,
    bytes: Long = 0,
    total: Long = 0,
) {
    i { "Task[$taskId] $from -> $to (${bytes}/${total} bytes)" }
}
```
