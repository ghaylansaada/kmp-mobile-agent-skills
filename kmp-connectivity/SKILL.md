---
name: kmp-connectivity
description: >
  Add network connectivity monitoring to a KMP project using Android
  ConnectivityManager/NetworkCallback and iOS NWPathMonitor. Provides reactive
  Flow-based status, offline request queuing with deduplication and retry,
  sync-on-reconnect, and offline UX patterns (banner, pending count). Activate
  when adding offline support, connectivity banners, or queue-and-sync logic.
compatibility: >
  KMP with platform network monitoring APIs. Android ConnectivityManager, iOS NWPathMonitor.
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---

# KMP Connectivity

## When to use

- Adding network connectivity monitoring to a KMP project
- Building offline-capable features with request queuing
- Implementing offline/online banners and UX patterns
- Creating sync-on-reconnect logic for queued operations
- Detecting network availability before making API calls
- Showing online/offline indicators in the UI
- Queuing failed operations for retry when connectivity returns
- Handling airplane mode or restricted network transitions

## Depends on

- **kmp-architecture** -- expect/actual pattern for ConnectivityObserver
- **kmp-dependency-injection** -- Koin wiring for observer, queue, and sync manager
- `kotlinx-coroutines-core` and `kotlinx-serialization-json` (already in template)

## Workflow

1. **Add Android manifest permission and verify platform requirements** --> [setup.md](references/setup.md)
2. **Create shared types** (ConnectivityStatus, expect ConnectivityObserver, PendingRequest) --> [shared-types.md](references/shared-types.md)
3. **Implement Android actual ConnectivityObserver** --> [android-implementation.md](references/android-implementation.md)
4. **Implement iOS actual ConnectivityObserver** --> [ios-implementation.md](references/ios-implementation.md)
5. **Build offline queue, sync manager, and offline-aware repository** --> [offline-queue.md](references/offline-queue.md)
6. **Wire DI, start monitoring, and integrate with repositories** --> [integration.md](references/integration.md)
7. **Add ConnectivityViewModel and offline banner UI** --> [offline-ui.md](references/offline-ui.md)
   _Skip if not showing offline UX to users._
8. **Use template to scaffold offline-capable features** --> `assets/templates/offline-feature.kt.template`

## Gotchas

1. **NetworkCallback fires `onAvailable()` on registration.** Android's `registerNetworkCallback()` immediately emits `Available` if a network exists. Guard reconnection logic (queue draining) with a `wasOffline` flag or you drain on every app start.

2. **NWPathMonitor needs its own DispatchQueue.** Using the main queue delays or drops path updates during heavy UI work. Always create a dedicated queue with `dispatch_queue_create`. The monitor also fires immediately on start.

3. **"Connected" does not mean internet works.** Both `NET_CAPABILITY_INTERNET` and `nw_path_status_satisfied` only mean an interface is up. Use `NET_CAPABILITY_VALIDATED` on Android for true reachability. iOS has no built-in equivalent -- make an HTTP probe.

4. **Airplane mode delay on iOS.** NWPathMonitor can take 1-3 seconds after airplane mode toggle to update. `isConnected()` returns stale data during this window. Use the reactive Flow, not synchronous checks, for critical decisions.

5. **Captive portals appear as connected.** Hotel/airport WiFi intercepts HTTP. Android detects this via `NET_CAPABILITY_VALIDATED` (false for portals). iOS sees the network as `satisfied`. Make a HEAD request to your server to verify genuine connectivity.

6. **`callbackFlow` without `awaitClose` leaks native monitors.** On Android, the `NetworkCallback` stays registered and the process holds a wakelock. On iOS, `nw_path_monitor_cancel` is never called and the monitor's dispatch queue keeps running. Always pair with `awaitClose { cleanup() }`.

7. **Offline queue is in-memory only.** A force-kill or crash loses all queued requests. If durability matters, persist `PendingRequest` list to DataStore or a local database and reload on startup.

8. **`distinctUntilChanged` hides rapid state flaps.** When WiFi drops and LTE takes over within milliseconds, the observer may emit `Available -> Available` (two different networks). `distinctUntilChanged` suppresses the second emission, so downstream never sees the network switch. This is usually desired but can hide failover events.

9. **iOS `isConnected()` has no synchronous API.** NWPathMonitor only provides async updates via a callback. The iOS actual tracks the last-emitted status, but it defaults to `true` before the first callback fires. Code that calls `isConnected()` before `observe()` has emitted gets a stale value.

10. **`collectLatest` in ConnectivityAwareSync cancels in-flight drains.** If the network flaps `Available -> Unavailable -> Available` quickly, the first `drainOnReconnect()` is cancelled mid-execution. Partially-drained requests stay in the queue with incremented retry counts. Use `collect` instead of `collectLatest` if atomic drain is required.

## Assets

| Path | Load when... |
|------|-------------|
| [references/setup.md](references/setup.md) | Configuring permissions and verifying platform requirements |
| [references/shared-types.md](references/shared-types.md) | Creating ConnectivityStatus, ConnectivityObserver, PendingRequest |
| [references/android-implementation.md](references/android-implementation.md) | Implementing Android actual ConnectivityObserver |
| [references/ios-implementation.md](references/ios-implementation.md) | Implementing iOS actual ConnectivityObserver |
| [references/offline-queue.md](references/offline-queue.md) | Building offline queue, sync manager, offline-aware repository |
| [references/integration.md](references/integration.md) | Wiring DI and integrating with repositories |
| [references/offline-ui.md](references/offline-ui.md) | Adding ConnectivityViewModel and offline banner |
| [assets/templates/offline-feature.kt.template](assets/templates/offline-feature.kt.template) | Scaffolding a new offline-capable feature |

## Validation

### A. Kotlin and KMP correctness
- [ ] Android: `ACCESS_NETWORK_STATE` permission declared in AndroidManifest.xml
- [ ] No additional library dependencies beyond kotlinx-coroutines-core and kotlinx-serialization-json
- [ ] `expect`/`actual` declarations match signatures exactly across source sets
- [ ] `ConnectivityStatus` is a `sealed interface` (not enum class or sealed class)
- [ ] `ConnectivityObserver` uses `expect`/`actual` with matching constructor signatures
- [ ] `observe()` returns `Flow<ConnectivityStatus>` -- no callbacks in shared code
- [ ] `callbackFlow` blocks have `awaitClose { ... }` with cleanup logic
- [ ] Android `awaitClose` calls `unregisterNetworkCallback`
- [ ] iOS `awaitClose` calls `nw_path_monitor_cancel`
- [ ] Both platform implementations apply `.distinctUntilChanged()`
- [ ] iOS implementation tracks last-known status for `isConnected()`
- [ ] `ConnectivityAwareSync` uses `wasOffline` flag to prevent drain on initial emission
- [ ] `OfflineRequestQueue` uses `Mutex` for thread-safe queue access
- [ ] `PendingRequest` deduplication keys on `(operationType, resourceId)` only when `resourceId != null`
- [ ] `SharingStarted.WhileSubscribed(5_000)` used for UI-facing StateFlows

### B. Security
- [ ] No sensitive data stored in `PendingRequest.payload` without encryption
- [ ] `ACCESS_NETWORK_STATE` is a normal permission -- no runtime request shown

### C. Performance
- [ ] `distinctUntilChanged()` applied to prevent redundant emissions
- [ ] Queue drain uses snapshot-then-iterate pattern (no lock held during network calls)
- [ ] CoroutineScope for sync uses `SupervisorJob()` to prevent cascading cancellation
- [ ] iOS NWPathMonitor uses a dedicated dispatch queue (not main queue)

### D. Integration
- [ ] Koin module registers `ConnectivityObserver`, `OfflineRequestQueue`, `ConnectivityAwareSync`
- [ ] `startMonitoring()` called from `LaunchedEffect(Unit)` in app composable
- [ ] `OfflineAwareRepository` interface usable as a mixin by any repository
- [ ] Upstream dependency on kmp-architecture and kmp-dependency-injection documented
- [ ] Downstream consumers (kmp-networking, kmp-notifications, kmp-background-job) listed
