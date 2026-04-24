# Platform Formatters

## PlatformDateTimeFormatter (expect -- commonMain)

```kotlin
package {your.package}.core.datetime

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone

expect class PlatformDateTimeFormatter() {
    fun formatDate(localDate: LocalDate, pattern: String): String
    fun formatDateTime(localDateTime: LocalDateTime, pattern: String): String
    fun formatInstant(instant: Instant, pattern: String, tz: TimeZone): String
}
```

**Why `TimeZone` on `formatInstant`?** An `Instant` is an absolute point in time. Formatting it requires choosing a timezone. Passing it explicitly prevents silent use of the device's system timezone, which causes bugs when the user travels or the server expects UTC.

## Android Actual (androidMain)

```kotlin
package {your.package}.core.datetime

import kotlinx.datetime.*
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

actual class PlatformDateTimeFormatter actual constructor() {
    actual fun formatDate(localDate: LocalDate, pattern: String): String =
        localDate.toJavaLocalDate()
            .format(DateTimeFormatter.ofPattern(pattern))

    actual fun formatDateTime(localDateTime: LocalDateTime, pattern: String): String =
        localDateTime.toJavaLocalDateTime()
            .format(DateTimeFormatter.ofPattern(pattern))

    actual fun formatInstant(instant: Instant, pattern: String, tz: TimeZone): String =
        DateTimeFormatter.ofPattern(pattern)
            .withZone(ZoneId.of(tz.id))
            .format(instant.toJavaInstant())
}
```

## iOS Actual (iosMain)

```kotlin
package {your.package}.core.datetime

import kotlinx.datetime.*
import kotlinx.datetime.toNSDateComponents
import platform.Foundation.*

actual class PlatformDateTimeFormatter actual constructor() {
    actual fun formatDate(localDate: LocalDate, pattern: String): String {
        val formatter = NSDateFormatter().apply {
            dateFormat = pattern
            timeZone = NSTimeZone.localTimeZone
        }
        val components = localDate.toNSDateComponents()
        val nsDate = NSCalendar.currentCalendar
            .dateFromComponents(components) ?: return ""
        return formatter.stringFromDate(nsDate)
    }

    actual fun formatDateTime(localDateTime: LocalDateTime, pattern: String): String {
        val formatter = NSDateFormatter().apply {
            dateFormat = pattern
            timeZone = NSTimeZone.localTimeZone
        }
        val instant = localDateTime.toInstant(TimeZone.currentSystemDefault())
        val nsDate = NSDate.dateWithTimeIntervalSince1970(
            instant.epochSeconds.toDouble(),
        )
        return formatter.stringFromDate(nsDate)
    }

    // Uses timeIntervalSince1970 (Unix epoch), NOT timeIntervalSinceReferenceDate (Apple 2001 epoch)
    actual fun formatInstant(instant: Instant, pattern: String, tz: TimeZone): String {
        val formatter = NSDateFormatter().apply {
            dateFormat = pattern
            timeZone = NSTimeZone.timeZoneWithName(tz.id)
        }
        return formatter.stringFromDate(
            NSDate.dateWithTimeIntervalSince1970(instant.epochSeconds.toDouble()),
        )
    }
}
```

**Warning:** `NSDateFormatter` creation is expensive. In hot paths (RecyclerView/LazyColumn cells), cache the formatter instance rather than creating one per call.

**Warning:** `NSDateFormatter` is locale-sensitive. For API serialization, set `formatter.locale = NSLocale("en_US_POSIX")`. For user-facing display, the default device locale is correct.

**Note on `formatDateTime` iOS:** `LocalDateTime` has no timezone, but `NSDate` requires one. We convert through `Instant` using `currentSystemDefault()` because `LocalDateTime` represents a user-facing display value that was already converted from `Instant` at the caller's chosen timezone. If this assumption does not hold for your use case, pass the timezone explicitly.

## RelativeTimeFormatter (commonMain)

```kotlin
package {your.package}.core.datetime

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.math.absoluteValue

class RelativeTimeFormatter(private val clock: Clock) {
    fun format(instant: Instant): String {
        val seconds = (clock.now() - instant).inWholeSeconds.absoluteValue
        return when {
            seconds < 5 -> "just now"
            seconds < 60 -> "$seconds seconds ago"
            seconds < 3600 -> {
                val m = seconds / 60
                if (m == 1L) "1 minute ago" else "$m minutes ago"
            }
            seconds < 86400 -> {
                val h = seconds / 3600
                if (h == 1L) "1 hour ago" else "$h hours ago"
            }
            seconds < 604800 -> {
                val d = seconds / 86400
                if (d == 1L) "1 day ago" else "$d days ago"
            }
            seconds < 2592000 -> {
                val w = seconds / 604800
                if (w == 1L) "1 week ago" else "$w weeks ago"
            }
            seconds < 31536000 -> {
                val mo = seconds / 2592000
                if (mo == 1L) "1 month ago" else "$mo months ago"
            }
            else -> {
                val y = seconds / 31536000
                if (y == 1L) "1 year ago" else "$y years ago"
            }
        }
    }
}
```

**Key change:** `Clock` is a required constructor parameter with no default. The DI module provides `Clock.System` at the wiring layer (see [integration.md](integration.md)). This ensures every test can use `FixedClock` without fighting default values.
