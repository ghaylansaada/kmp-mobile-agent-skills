---
name: kmp-logging
description: >
  Add structured multiplatform logging to a Kotlin Multiplatform app using Kermit.
  Covers tagged loggers, severity filtering, release-mode stripping, Ktor HTTP
  logging integration, and platform-specific log writers. Use this skill when
  adding logging, replacing println calls, integrating HTTP request logging, or
  stripping debug logs from release builds.
compatibility: >
  KMP with Compose Multiplatform. Requires co.touchlab:kermit. Ktor integration
  requires ktor-client-logging (already in the template). Koin required for DI
  wiring of logger instances.
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---

# KMP Logging

## Scope

Covers structured logging for a KMP project using Kermit: AppLogger singleton, severity-based filtering, release-mode log suppression, per-class tagged loggers, Ktor HTTP request logging with header sanitization, transfer/paging operation logging utilities, and test infrastructure for verifying log output. Does not cover crash reporting integration (see kmp-analytics-crashlytics) or performance profiling (see kmp-performance).

## When to use

- Adding structured logging to a KMP project
- Replacing ad-hoc println/printStackTrace calls with leveled logging
- Integrating HTTP request/response logging with Ktor
- Stripping debug logs from release builds for security
- Setting up per-class tagged loggers
- Logging transfer, paging, or API operations with structured context

## Depends on

- **kmp-project-setup** -- Gradle and source set structure
- **kmp-dependency-injection** -- Koin module wiring

## Workflow

1. Add Kermit dependency --> [setup.md](references/setup.md)
2. Create AppLogger, LogConfig, and Ktor integration --> [core-logging.md](references/core-logging.md)
3. Wire logging module into DI and platform entry points --> [integration.md](references/integration.md)

## Gotchas

1. **Ktor LogLevel.ALL logs Authorization headers and tokens.** Use `LogLevel.HEADERS` with sanitization. Even with Kermit release stripping, Ktor constructs log strings before passing them to the logger, so sensitive data still enters memory.
2. **Android Logcat truncates messages over ~4096 bytes.** Long JSON payloads are silently cut off. Split large payloads or log summaries instead of full bodies.
3. **iOS `os_log` truncates beyond 1024 bytes.** Keep messages short; log structured data as separate key-value pairs rather than single large strings.
4. **`println()` in Kotlin/Native does NOT appear in iOS device logs.** Only OSLog output via Kermit's OSLogWriter appears in Xcode console and Console.app. Leftover println calls are invisible in production.
5. **Default Severity.Verbose leaks all output if `isDebug` is hardcoded or loggingModule is not loaded.** Always derive isDebug from platform build config (`BuildConfig.DEBUG` on Android, `#if DEBUG` on iOS). A hardcoded `true` in release ships all debug logs to users.
6. **`Logger.withTag()` creates a new object on each call.** Cache the tagged logger as a class property. Calling withTag inside hot loops, Composable functions, or high-frequency callbacks allocates unnecessarily and pressures GC.
7. **Always use the lambda form `logger.d { "msg: $expensive" }`.** The string overload evaluates interpolation eagerly even when the severity is suppressed. In release builds with Severity.Assert, the lambda body is never invoked, avoiding wasted computation.
8. **`kotlin.system.getTimeMillis()` is not available in commonMain for JVM/Android.** Use `kotlin.time.TimeSource.Monotonic.markNow()` for elapsed-time measurement in common code. Platform-specific time APIs cause compilation failures on other targets.
9. **Kermit StaticConfig is immutable after creation.** Calling `AppLogger.configure()` after loggers have already been obtained via `withTag()` does not retroactively update those loggers. Always configure before any logger is accessed -- this is why `loggingModule` must load first in Koin.

## Assets

| Path | Load when... |
|------|-------------|
| [references/setup.md](references/setup.md) | Adding Kermit dependency |
| [references/core-logging.md](references/core-logging.md) | Implementing AppLogger, LogConfig, Ktor logging |
| [references/integration.md](references/integration.md) | Wiring into Koin, platform entry points, HttpClient |
| [assets/snippets/logging-patterns.kt](assets/snippets/logging-patterns.kt) | Structured logging, scoped operations, error categorization |
| [assets/templates/tagged-logger.kt.template](assets/templates/tagged-logger.kt.template) | Scaffold for tagged logger in any class |

## Validation

### A. Logging correctness
- [ ] Log levels used appropriately (debug for internal state, info for milestones, warn for recoverable errors, error for failures)
- [ ] No sensitive data logged (PII, tokens, passwords, Authorization headers)
- [ ] Log tags are consistent and meaningful (class name or subsystem name)
- [ ] Production builds suppress debug/verbose logs via `Severity.Assert` threshold
- [ ] Lambda form `logger.d { }` used everywhere (never string overload)
- [ ] No `android.*` imports in commonMain code
- [ ] No `println()` or `printStackTrace()` calls remain after migration
- [ ] `Logger.withTag()` result cached as class property, not called in loops or composables
- [ ] Ktor logging uses `LogLevel.HEADERS` not `LogLevel.ALL`
- [ ] Time measurement uses `kotlin.time.TimeSource.Monotonic` not platform-specific APIs

### B. Security
- [ ] No PII, secrets, or tokens appear in any log message format string
- [ ] Release builds use `Severity.Assert` to strip all normal log output
- [ ] `isDebug` derived from platform build config, never hardcoded
- [ ] Ktor HTTP logging does not log request/response bodies containing auth data
- [ ] Log messages do not include full file paths, internal IPs, or stack traces in production

### C. Performance
- [ ] Lambda logging form prevents string allocation when severity is suppressed
- [ ] Tagged loggers cached as class properties, not created per-call
- [ ] `logOperation` timing uses monotonic clock, not wall clock
- [ ] No logging inside tight loops or per-frame callbacks without severity gating

### D. Integration
- [ ] Depends-on references match actual skill directory names
- [ ] `loggingModule` loads first in Koin module list so logger is available to all other modules
- [ ] `initKoin()` passes platform-derived `isDebug` flag
- [ ] Connected skills (kmp-analytics-crashlytics, kmp-performance) listed in integration.md
- [ ] Template placeholders are consistent (`{your.package}`, `{{CLASS_NAME}}`, `{{TAG}}`)
