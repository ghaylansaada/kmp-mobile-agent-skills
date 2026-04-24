---
name: kmp-notifications
description: >
  Unified notification abstraction for KMP -- local scheduling via expect/actual
  NotificationManager, FCM (Android) and APNs (iOS) push setup, notification
  channels, deep-link handling on tap, and shared NotificationPayload model with
  sealed notification types and Flow-based event streams. Activate when adding push
  notifications, local scheduling, notification channels, deep-link-on-tap handling,
  or FCM/APNs integration.
compatibility: >
  KMP targeting Android (FCM) and iOS (APNs). Requires Firebase Messaging and platform notification APIs.
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---

# KMP Notifications

## Scope

Covers local notification scheduling and push notification setup for KMP projects:
shared NotificationPayload model, sealed notification types, expect/actual
NotificationManager, FCM service (Android), APNs delegate (iOS), notification
channels, deep-link handling from taps, and Flow-based notification event streams.
Does not cover rich media attachments (requires platform extensions), in-app
messaging, or analytics event tracking.

## When to use

- Adding local or push notification support
- Scheduling or cancelling local notifications from shared code
- Setting up FCM (Android) or APNs (iOS) push notifications
- Creating notification channels for Android
- Handling deep links from notification taps
- Observing notification events as a Flow in shared code

## Depends on

- **kmp-architecture** -- expect/actual pattern for NotificationManager
- **kmp-dependency-injection** -- Koin wiring for NotificationManager and PushTokenHandler

## Workflow

1. Add dependencies, manifest entries, Xcode capabilities -- see [setup.md](references/setup.md)
2. Define shared payload, sealed types, expect NotificationManager, push handler -- see [shared-types.md](references/shared-types.md)
3. Implement Android actual + FCM service -- see [android-impl.md](references/android-impl.md)
4. Implement iOS actual + delegate + push registration -- see [ios-impl.md](references/ios-impl.md)
5. Wire DI, deep links, platform entry points -- see [integration.md](references/integration.md)

## Gotchas

1. **FCM token can change anytime.** Token rotates on app restore, reinstall, clear data, or server-side invalidation. Always handle `onNewToken()` and forward immediately to backend. Never cache as source of truth -- the cached value will silently become stale and push messages will stop arriving with no error.
2. **iOS delegate must be set before `didFinishLaunchingWithOptions` returns.** Setting it later causes foreground notifications to be silently dropped and tap handling to fail. There is no error or log message -- notifications simply vanish.
3. **Android channels cannot change importance after creation.** The system owns the channel once created. Calling `createNotificationChannel()` with a different importance on an existing channel ID is silently ignored. To change importance, create a new channel with a different ID and delete the old one.
4. **Local notifications do not survive iOS app updates.** Pending `UNNotificationRequest` items are removed on App Store update. Re-schedule all pending notifications in `didFinishLaunchingWithOptions` or users will miss scheduled reminders after updating.
5. **Rich attachments require platform extensions.** iOS needs a Notification Service Extension target. Android needs `BigPictureStyle` or `BigTextStyle`. Neither can be done from shared KMP code alone -- keep attachment logic in platform-specific code.
6. **`POST_NOTIFICATIONS` permission must be requested at runtime on Android 13+.** Declaring the permission in the manifest is not enough. Without a runtime request via `ActivityResultLauncher`, `NotificationManagerCompat.notify()` silently drops the notification. The check-only approach returns `false` but cannot prompt the user. See **kmp-permissions** skill for runtime permission request patterns.
7. **`hashCode()` for notification IDs can collide.** Using `String.hashCode()` to convert string IDs to integer IDs (required by Android) can produce collisions for different strings. Two notifications with colliding IDs will overwrite each other. Use a stable mapping or truncated hash with a collision-resistant scheme.
8. **CoroutineScope in FCM service leaks if not cancelled.** `FirebaseMessagingService` is a regular Android `Service`. A `CoroutineScope` created in the service must be cancelled in `onDestroy()`, or coroutines outlive the service lifecycle and leak memory.

## Assets

| Path | Load when... |
|------|-------------|
| [references/setup.md](references/setup.md) | Adding dependencies, manifest, Xcode capabilities |
| [references/shared-types.md](references/shared-types.md) | Defining payload model, sealed types, and expect declarations |
| [references/android-impl.md](references/android-impl.md) | Implementing Android actual and FCM service |
| [references/ios-impl.md](references/ios-impl.md) | Implementing iOS actual, delegate, push registration |
| [references/integration.md](references/integration.md) | Wiring DI, deep links, platform entry points |
| [assets/snippets/local-notification.kt](assets/snippets/local-notification.kt) | Quick local notification patterns |
| [assets/templates/notification-feature.kt.template](assets/templates/notification-feature.kt.template) | Scaffolding a new notification type |

## Validation

### A. Build and compilation
- [ ] All Kotlin snippets in references/ compile with Kotlin 2.0+ and KMP targets
- [ ] No deprecated APIs used (no removed coroutine builders, no legacy notification APIs)
- [ ] All imports shown explicitly in code blocks (no implicit star-import assumptions)

### B. Notification correctness
- [ ] Notification channels created before posting any notification (Android 8+)
- [ ] Runtime permission for `POST_NOTIFICATIONS` handled (Android 13+) -- not just checked
- [ ] Proper FCM/APNs token management -- `onNewToken()` forwards immediately, no stale cache
- [ ] Deep link handling covers both cold-start and warm-start (Activity `onCreate` + `onNewIntent`)
- [ ] `sealed interface` used for notification types in shared code
- [ ] `Flow` used for notification event streams (not just callbacks)
- [ ] No `android.*` or `platform.*` imports in commonMain code blocks
- [ ] iOS delegate set before `didFinishLaunchingWithOptions` returns
- [ ] `UNNotificationPresentationOptions` set for foreground display
- [ ] FCM service CoroutineScope cancelled in `onDestroy()`
- [ ] Notification ID collision risk documented or mitigated

### C. Security
- [ ] Push tokens not logged or stored in plaintext preferences
- [ ] Deep link URIs validated before navigation (no open-redirect)
- [ ] Notification payload data validated before use
- [ ] No hardcoded secrets, API keys, or server URLs in any file

### D. Performance
- [ ] FCM service scope uses `SupervisorJob` to isolate failures
- [ ] Heavy notification processing offloaded to background dispatcher
- [ ] Notification channel creation is idempotent and fast (no I/O on main thread)

### E. Integration and cross-skill consistency
- [ ] Depends-on references match actual skill directory names
- [ ] Connected skills table lists correct upstream and downstream skills
- [ ] Koin module registration consistent with kmp-dependency-injection patterns
- [ ] Template placeholders (`{your.package}`, `{{FEATURE}}`) are consistent and documented
