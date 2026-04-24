---
name: kmp-analytics-crashlytics
description: >
  Use this skill when adding Firebase Analytics event tracking, Crashlytics
  crash reporting, screen view analytics, non-fatal error logging, or
  GDPR-compliant analytics consent to a Kotlin Multiplatform app. Activate
  when the user mentions "add analytics," "track crashes," "screen tracking,"
  "breadcrumbs," or "crash reporting" — even without naming Firebase explicitly.
  Also applies when wiring API error reporting into repositories or adding
  user identity tracking across login/logout. Does NOT cover third-party
  analytics providers (Amplitude, Mixpanel) or server-side analytics.
compatibility: >
  KMP with Android + iOS targets. Requires Firebase project with both platforms configured.
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---

# KMP Analytics and Crashlytics

## Scope

Covers end-to-end Firebase Analytics and Crashlytics integration in a KMP app: dependency setup, shared interfaces via expect/actual, platform implementations, DI wiring, usage patterns (screen tracking, API error reporting, breadcrumbs), GDPR consent, and testing with fakes. Does not cover third-party analytics SDKs or backend analytics pipelines.

## When to use

- Adding Firebase Analytics event tracking to the app
- Integrating Crashlytics for crash reporting and non-fatal error logging
- Setting up consent-based analytics gating (GDPR, ATT)
- Tracking screen views, user actions, or conversion funnels
- Logging non-fatal exceptions with custom keys and breadcrumbs
- Configuring debug vs release analytics behavior
- Adding user properties for audience segmentation
- Uploading dSYM or mapping files for crash symbolication

## Depends on

- **kmp-project-setup** -- KMP project with Android and iOS targets
- **kmp-architecture** -- expect/actual pattern for platform-specific Firebase calls
- **kmp-dependency-injection** -- Koin module wiring for analytics singletons

Also requires a Firebase project created with both Android and iOS apps registered.

## Workflow

1. **Add Firebase dependencies and config files** → read `references/setup.md`
2. **Define shared interfaces** → read `references/shared-interfaces.md`
3. **Implement platform actuals** → read `references/platform-implementations.md`
   _Skip if updating existing implementations — go directly to the file that needs changes._
4. **Wire into DI and integrate with app layers** → read `references/integration.md`
   _Covers Koin module, repository/ViewModel/Compose patterns, session identity, GDPR consent, and crash context utilities._
5. **Define new event types** → use template at `assets/analytics-event.kt.template`
   _Load only when creating new typed analytics events beyond ScreenViewEvent and UserActionEvent._

## Gotchas

1. **Firebase must be initialized before any analytics/crashlytics call.** On Android, the google-services plugin handles this. On iOS, `FirebaseApp.configure()` must be called in AppDelegate BEFORE Koin starts — creating AnalyticsTracker before Firebase init causes `IllegalStateException`.

2. **Kotlin exceptions appear as "KotlinException" in iOS crash reports with no useful stack trace.** The `Throwable.toNSError()` conversion must include `stackTraceToString()` in userInfo — see `references/platform-implementations.md`.

3. **Firebase Analytics silently drops events with names longer than 40 characters.** Parameter keys max 40 chars, values max 100 chars. No error — the event simply disappears.

4. **`recordException` has a queue limit.** Calling it in a tight loop fills the queue and drops later reports. Deduplicate — batch failures should log one exception with a count.

5. **Screen tracking needs `DisposableEffect`, not `LaunchedEffect`.** LaunchedEffect fires only on initial composition and cannot detect screen exit for accurate screen-time tracking.

6. **Non-fatal exceptions upload on NEXT app launch, not immediately.** First crash reports for new apps can take up to 24 hours to appear.

7. **Shared interfaces in commonMain must not expose Firebase types (`Bundle`, `FIRAnalytics`, etc.).** This breaks compilation on the other platform.

8. **The Firebase `-ktx` artifacts were deprecated in BoM 32.x and removed in BoM 33.x.** Use the non-KTX artifacts (`firebase-analytics`, `firebase-crashlytics`) — KTX extensions are now included in the main artifacts.

## Assets

| Path | Load when... |
|------|---------|
| `assets/analytics-event.kt.template` | Template for creating new typed analytics event classes |

## Validation

### A. Kotlin and KMP correctness
- [ ] No unresolved imports in any reference code
- [ ] Firebase dependencies use non-KTX artifacts (no `-ktx` suffixes)
- [ ] Firebase BOM manages all Firebase library versions (no individual `version.ref` on Firebase libs)
- [ ] No Firebase platform types (`Bundle`, `FIRAnalytics`, etc.) leak into commonMain interfaces
- [ ] Proper expect/actual declarations for `createAnalyticsTracker()` and `createCrashReporter()`
- [ ] `AnalyticsTracker` and `CrashReporter` interfaces expose only Kotlin stdlib types
- [ ] All `MutableList` fields in fakes are `private` with read-only public accessors

### B. Security
- [ ] No hardcoded API keys, Firebase project IDs, or credentials in any file
- [ ] Analytics user ID is cleared on logout (both tracker and crash reporter)
- [ ] GDPR consent function disables collection before any events are sent

### C. Performance
- [ ] `recordException` is not called in tight loops — batch failures log one exception with count
- [ ] `DisposableEffect` used for Compose screen tracking (not `LaunchedEffect`)
- [ ] Analytics calls are non-blocking (Firebase SDK handles async internally)

### D. Integration
- [ ] Koin module registers `AnalyticsTracker` and `CrashReporter` as singletons
- [ ] No circular dependencies between analytics module and other modules
- [ ] `analyticsModule()` is registered in the correct order (after logging, before feature modules)
- [ ] iOS `Throwable.toNSError()` includes `stackTraceToString()` in userInfo
