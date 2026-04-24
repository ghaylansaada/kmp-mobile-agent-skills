---
name: kmp-datastore
description: >
  Implement Jetpack DataStore with KMP for type-safe, asynchronous local
  persistence. Covers DataStoreFactory interface, platform file path resolution
  (Android filesDir, iOS NSDocumentDirectory), common createDataStore helper,
  type-safe preference keys, reactive reads via Flow, transactional edits,
  and Koin DI wiring. Activate when adding local preferences, migrating from
  SharedPreferences/NSUserDefaults, or troubleshooting DataStore corruption.
compatibility: >
  KMP with Jetpack DataStore. Requires kotlinx-serialization for typed preferences.
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---

# KMP DataStore

## When to use

- Adding new user preferences or app settings to local storage
- Setting up DataStore for a new KMP project
- Creating type-safe preference key groups
- Migrating from SharedPreferences (Android) or NSUserDefaults (iOS)
- Storing user preferences (theme, locale, onboarding flags)
- Persisting simple key-value pairs across app sessions
- Reading preferences reactively with Flow
- Providing default values and handling missing preference keys

## Depends on

- kmp-dependency-injection -- Koin platform modules for DataStoreFactory registration

## Workflow

1. Verify DataStore artifacts in version catalog --> [setup.md](references/setup.md)
2. Create DataStoreFactory, createDataStore helper, and platform implementations --> [core-setup.md](references/core-setup.md)
3. Wire DI and integrate with app layers --> [integration.md](references/integration.md)
4. Migrate from SharedPreferences or NSUserDefaults --> [migrations.md](references/migrations.md)
   _Skip if not migrating from legacy storage._
5. Define type-safe preference keys --> `assets/templates/preference-keys.kt.template`

## Gotchas

1. **Both platforms must use identical filename.** Android uses `context.filesDir`, iOS uses `NSDocumentDirectory`. Both must resolve to the same filename (`session.preferences_pb`). A mismatch causes data to silently not persist across platforms, making shared test data or cross-platform debugging impossible.
2. **Not thread-safe for cross-process writes on Android.** DataStore is single-process only. Concurrent writes from a second process (e.g., a WorkManager worker in a `:work` process) corrupt the file with an `IOException`. Use `MultiProcessDataStoreFactory` if multi-process access is required.
3. **Multiple instances for same file = corruption.** Creating two `DataStore<Preferences>` pointing at the same file causes `IllegalStateException` at runtime. Always register the DataStore as a Koin `single`, never `factory`. Two modules accidentally registering the same path is the most common trigger.
4. **DataStore reads are cold Flows.** `dataStore.data` reads from disk on each new collection -- there is no in-memory cache. The first read can take tens of milliseconds on cold start. UI code must handle the initial empty/loading state to avoid showing stale or default values.
5. **SharedPreferences migration runs exactly once.** If the `sharedPreferencesName` string is wrong (typo, different build variant name), the migration silently completes with zero keys transferred and the original SharedPreferences file is deleted. Data is permanently lost. Always verify the exact name from legacy code before migrating.
6. **iOS files in NSDocumentDirectory are included in iCloud backups.** Preference files stored here are backed up to iCloud by default. For sensitive data, exclude the file via `NSURLIsExcludedFromBackupKey` or use the Keychain instead.
7. **Storing secrets in plain DataStore is insecure.** DataStore files are stored as unencrypted protobuf on disk. Access tokens, refresh tokens, or API keys stored in plain DataStore are readable by any process with file-system access on a rooted/jailbroken device. Use platform Keychain/EncryptedSharedPreferences for secrets.
8. **edit {} is a suspend function -- never call from Main without a dispatcher.** Calling `dataStore.edit {}` on `Dispatchers.Main` blocks the UI thread during the disk write. Always call from a coroutine on `Dispatchers.IO` or from a ViewModel/repository scope that uses an IO dispatcher.

## Assets

| Path | Load when... |
|---|---|
| [preference-keys.kt.template](assets/templates/preference-keys.kt.template) | Type-safe preference key groups with sealed interfaces |

## Validation

### A. DataStore correctness
- [ ] `DataStore<Preferences>` registered as Koin `single`, not `factory`
- [ ] Only one `DataStore` instance created per file path across all modules
- [ ] All reads use `dataStore.data` (Flow-based), never blocking `runBlocking` reads
- [ ] Correct key factory used for each type (`stringPreferencesKey`, `intPreferencesKey`, etc.)
- [ ] `createDataStore` helper called with consistent filename on both platforms
- [ ] No `android.*` imports in commonMain source sets
- [ ] `edit {}` calls happen on IO dispatcher, not Main

### B. Security
- [ ] Tokens and secrets not stored in plain DataStore (use Keychain/EncryptedSharedPreferences)
- [ ] iOS DataStore file excluded from iCloud backup if it contains sensitive data

### C. Performance
- [ ] First DataStore read handled with loading state (cold Flow, disk I/O)
- [ ] No redundant `dataStore.data.first()` calls in hot paths -- cache in Flow/StateFlow

### D. Integration
- [ ] Module loading order: `platformModule()` before `localStorageModule()`
- [ ] Downstream skills (kmp-session-management, kmp-networking) can resolve `DataStore<Preferences>` from Koin
