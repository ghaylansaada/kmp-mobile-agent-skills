# Migrations: DataStore

## Android: SharedPreferences Migration

```kotlin
package {your.package}.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

fun createDataStoreWithMigration(context: Context): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        migrations = listOf(
            SharedPreferencesMigration(
                context = context,
                sharedPreferencesName = "legacy_prefs",
            ),
        ),
        produceFile = {
            context.filesDir.resolve(dataStoreFileName).absolutePath.toPath()
        },
    )
```

Migration runs once on first access. If `sharedPreferencesName` is wrong, data is
silently lost -- verify the exact name from legacy code before deploying.

## iOS: NSUserDefaults Migration

No built-in migration utility exists for iOS. Implement a one-time manual migration:

```kotlin
package {your.package}.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import platform.Foundation.NSUserDefaults

suspend fun migrateFromUserDefaults(
    dataStore: DataStore<Preferences>,
    defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) {
    if (defaults.boolForKey("datastore_migration_complete")) return

    val accessToken = defaults.stringForKey("accessToken")
    val refreshToken = defaults.stringForKey("refreshToken")
    val userId = defaults.stringForKey("userId")

    if (accessToken != null && refreshToken != null && userId != null) {
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("session.accessToken")] = accessToken
            prefs[stringPreferencesKey("session.refreshToken")] = refreshToken
            prefs[stringPreferencesKey("session.userId")] = userId
        }
    }

    defaults.setBool(true, forKey = "datastore_migration_complete")
}
```

Call `migrateFromUserDefaults()` once at app startup (e.g., in `MainViewController`'s
`ComposeUIViewController` lambda) before any DataStore reads.
