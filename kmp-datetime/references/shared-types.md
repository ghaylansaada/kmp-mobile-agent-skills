# Shared Types and Extensions (commonMain)

## Bridge: kotlin.time.Instant <-> kotlinx.datetime.Instant

Extends the template's existing `DatetimeExt.kt`:

```kotlin
package {your.package}.ext

import kotlinx.datetime.Instant as KotlinxInstant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant as kdtToInstant
import kotlinx.datetime.toLocalDateTime as kdtToLocalDateTime
import kotlin.time.Instant as KotlinInstant

// Existing template functions (preserved)
fun Long.toInstant(): KotlinInstant =
    KotlinInstant.fromEpochMilliseconds(this)

fun KotlinInstant.toEpochMilli(): Long =
    this.toEpochMilliseconds()

// Bridge functions
fun KotlinInstant.toKotlinxInstant(): KotlinxInstant =
    KotlinxInstant.fromEpochMilliseconds(this.toEpochMilliseconds())

fun KotlinxInstant.toKotlinInstant(): KotlinInstant =
    KotlinInstant.fromEpochMilliseconds(this.toEpochMilliseconds())

// kotlinx.datetime convenience -- explicit TimeZone, no defaults
fun KotlinxInstant.toLocalDateTimeIn(tz: TimeZone): LocalDateTime =
    this.kdtToLocalDateTime(tz)

fun KotlinxInstant.toLocalDateIn(tz: TimeZone): LocalDate =
    this.kdtToLocalDateTime(tz).date

fun LocalDateTime.toInstantIn(tz: TimeZone): KotlinxInstant =
    this.kdtToInstant(tz)

fun Long.toKotlinxInstant(): KotlinxInstant =
    KotlinxInstant.fromEpochMilliseconds(this)
```

**Why no default `TimeZone` parameter?** Using `TimeZone.currentSystemDefault()` as a default silently couples code to device timezone. Callers must choose explicitly: pass `TimeZone.UTC` for storage/serialization, or `TimeZone.currentSystemDefault()` at the UI layer.

**Why distinct names (`toLocalDateTimeIn`, `toInstantIn`)?** kotlinx-datetime already defines `Instant.toLocalDateTime(TimeZone)` and `LocalDateTime.toInstant(TimeZone)` as member/extension functions. Naming our extensions identically causes infinite recursion at runtime because the extension calls itself instead of the library function.

## DateTimePatterns

```kotlin
package {your.package}.core.datetime

object DateTimePatterns {
    const val DATE_SHORT = "MM/dd/yyyy"
    const val DATE_LONG = "MMMM dd, yyyy"
    const val DATE_ISO = "yyyy-MM-dd"
    const val TIME_12H = "hh:mm a"
    const val TIME_24H = "HH:mm"
    const val DATETIME_SHORT = "MM/dd/yyyy hh:mm a"
    const val DATETIME_LONG = "MMMM dd, yyyy HH:mm"
    const val DATETIME_ISO = "yyyy-MM-dd'T'HH:mm:ss"
}
```

## FixedClock (for testing)

```kotlin
package {your.package}.core.datetime

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class FixedClock(private val fixedInstant: Instant) : Clock {
    override fun now(): Instant = fixedInstant
}
```

## Utility Functions

```kotlin
package {your.package}.core.datetime

import kotlinx.datetime.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

fun Instant.isToday(
    tz: TimeZone,
    clock: Clock,
): Boolean =
    this.toLocalDateTime(tz).date == clock.todayIn(tz)

fun Instant.isBetween(start: Instant, end: Instant): Boolean =
    this >= start && this <= end

fun isExpired(
    issuedAt: Instant,
    ttl: Duration,
    clock: Clock,
): Boolean =
    clock.now() > issuedAt + ttl

fun LocalDate.startOfDay(tz: TimeZone): Instant =
    this.atStartOfDayIn(tz)

fun LocalDate.endOfDay(tz: TimeZone): Instant =
    this.plus(1, DateTimeUnit.DAY)
        .atStartOfDayIn(tz)
        .minus(1.milliseconds)
```

**Key changes from naive implementations:**
- `isToday` requires both `TimeZone` and `Clock` -- no hidden `Clock.System` or `currentSystemDefault()`.
- `isExpired` requires `Clock` -- fully testable with `FixedClock`.
- `endOfDay` uses `minus(1.milliseconds)` (kotlin.time.Duration) instead of the non-existent `minus(Long, DateTimeUnit.MILLISECOND, TimeZone)` overload.
