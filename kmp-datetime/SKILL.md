---
name: kmp-datetime
description: >
  Handle dates, times, and timezones in KMP with kotlinx-datetime. Covers
  timezone-aware operations, expect/actual platform formatters (java.time,
  NSDateFormatter), relative time display, ISO 8601 serializers, and
  Instant/NSDate bridging. Activate when formatting timestamps, converting
  timezones, serializing dates, or displaying relative time.
compatibility: >
  KMP with Compose Multiplatform. Requires kotlinx-datetime and
  kotlinx-serialization for serializers.
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---

# KMP DateTime

## Scope

Covers cross-platform date/time handling with kotlinx-datetime: bridge extensions
between kotlin.time.Instant and kotlinx.datetime.Instant, platform-native
formatters via expect/actual, relative time display, ISO 8601 serializers, and
FixedClock for testing. Does not cover calendar UI components, scheduling/alarms,
platform notification timing, Room date/time converters (see kmp-database), or DI wiring
(see kmp-dependency-injection).

## When to use

- Adding date/time handling to a KMP project
- Formatting dates with platform-native formatters
- Displaying relative time ("5 minutes ago")
- Serializing dates in ISO 8601 format for API DTOs
- Bridging kotlin.time.Instant and kotlinx.datetime.Instant
- Formatting dates/times for display in the user's locale
- Converting between timezones for scheduling features
- Injecting a testable Clock for deterministic unit tests

## Depends on

- **kmp-architecture** -- platform-specific formatters via expect/actual

> If you need Room date/time converters, see **kmp-database**.

## Workflow

1. Add kotlinx-datetime dependency --> [setup.md](references/setup.md)
2. Create bridge extensions, patterns, and utility functions --> [shared-types.md](references/shared-types.md)
3. Implement platform formatters and relative time --> [platform-formatters.md](references/platform-formatters.md)
4. Add ISO 8601 serializers --> [serializers.md](references/serializers.md)
   _Skip if not using custom serializers._
5. Understand integration points (Ktor, Compose) --> [integration.md](references/integration.md)

## Gotchas

1. **`Clock.System.now()` returns UTC, not local time.** Convert explicitly: `now().toLocalDateTime(tz)`. Forgetting this shows UTC times to users. Always pass an explicit `TimeZone` parameter -- never rely on `TimeZone.currentSystemDefault()` in library code.
2. **`LocalDateTime` has NO timezone.** Two instances cannot be compared for absolute time without timezone context. Store `Instant` for absolute time, convert to `LocalDateTime` at display time only.
3. **Storing `LocalDateTime` as epoch millis depends on device timezone.** If timezone changes between write and read, the restored value differs. Store `Instant` (UTC epoch millis) or `LocalDateTime` as ISO string instead.
4. **`DateTimePeriod` vs `DateTimeUnit`.** Adding `1.month` to Jan 31 gives Feb 28/29. Adding `30.days` always adds exactly 30 days. Use the right type for calendar math vs fixed-duration math.
5. **iOS `NSDate` epoch is 2001, not 1970.** Use `timeIntervalSince1970`, NOT `timeIntervalSinceReferenceDate`. Wrong one shifts dates by 31 years.
6. **iOS `NSDateFormatter` is locale-sensitive.** "MMM dd" produces "janv. 15" in French. For API serialization, set locale to `en_US_POSIX`. For user-facing display, this is correct behavior.
7. **Extension functions can shadow member functions.** Naming an extension `Instant.toLocalDateTime()` shadows the member function of the same name and causes infinite recursion at runtime. Use distinct names like `toLocalDateTimeIn()` for custom extensions.
8. **`Clock.System` direct calls are untestable.** Any function calling `Clock.System.now()` directly cannot have its time controlled in tests. Always inject `Clock` as a constructor parameter or function parameter with `Clock.System` as the default only at the DI boundary.

## Assets

| Path | Load when... |
|------|-------------|
| [references/setup.md](references/setup.md) | Adding kotlinx-datetime to the project |
| [references/shared-types.md](references/shared-types.md) | Creating bridge extensions and utility functions |
| [references/platform-formatters.md](references/platform-formatters.md) | Implementing platform-specific date formatting |
| [references/serializers.md](references/serializers.md) | Adding ISO 8601 JSON serializers |
| [references/integration.md](references/integration.md) | Wiring into Ktor or Compose |

## Validation

### A. DateTime correctness
- [ ] kotlinx-datetime used in commonMain (not java.time or NSDate directly)
- [ ] `Clock` injected as constructor/function parameter -- no direct `Clock.System` calls in library/domain code
- [ ] `TimeZone` passed explicitly to all conversion functions -- no hidden `currentSystemDefault()` in shared utilities
- [ ] `Instant` used for absolute timestamps -- `LocalDateTime` only at the display boundary
- [ ] Proper serialization of datetime types (built-in or custom `KSerializer`)
- [ ] No `android.*` or `java.*` imports in commonMain source sets
- [ ] No `platform.Foundation.*` imports in commonMain source sets
- [ ] Extension function names do not shadow kotlinx-datetime member functions
- [ ] Bridge functions between kotlin.time.Instant and kotlinx.datetime.Instant preserve millisecond precision

### B. Security
- [ ] No timezone-dependent token expiry checks (use UTC Instant comparison)
- [ ] Server-provided timestamps validated before parsing (malformed input handled gracefully)

### C. Performance
- [ ] `NSDateFormatter` instances reused or cached on iOS (creation is expensive)
- [ ] No unnecessary Instant-to-String-to-Instant round-trips in hot paths

### D. Integration
- [ ] Platform formatters not constructed inline in Composables (obtain from DI or remember)
- [ ] ISO 8601 serializer round-trips verified through JSON encode/decode
