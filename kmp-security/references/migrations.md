# Security -- Migrations

## Token Migration: Plain DataStore to Secure Storage

One-time migration from plain DataStore tokens to SecureTokenStorage.
Call at app startup before any token reads.

```kotlin
package {your.package}.security

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

class TokenMigration(
    private val plainDataStore: DataStore<Preferences>,
    private val secureTokenStorage: SecureTokenStorage,
) {
    private val migrationCompleteKey =
        stringPreferencesKey("token_migration_complete")
    private val accessTokenKey =
        stringPreferencesKey("access_token")
    private val refreshTokenKey =
        stringPreferencesKey("refresh_token")

    suspend fun migrateIfNeeded() {
        val prefs = plainDataStore.data.first()
        if (prefs[migrationCompleteKey] == "true") return

        prefs[accessTokenKey]?.let {
            secureTokenStorage.saveToken("access_token", it)
        }
        prefs[refreshTokenKey]?.let {
            secureTokenStorage.saveToken("refresh_token", it)
        }

        plainDataStore.edit { mutablePrefs ->
            mutablePrefs.remove(accessTokenKey)
            mutablePrefs.remove(refreshTokenKey)
            mutablePrefs[migrationCompleteKey] = "true"
        }
    }
}
```

## Encrypted Room Database Migration

Encrypts an existing plaintext Room database with SQLCipher.
Run ONCE at app startup before opening the database.

**Warning:** The passphrase is interpolated into a SQL string. Ensure it does not
contain single quotes or other SQL-injection characters. The `getOrCreatePassphrase`
in `EncryptedDatabaseFactory` generates only alphanumeric + safe special characters.

```kotlin
package {your.package}.security

import android.content.Context
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File

object DatabaseEncryptionMigration {
    fun migrateToEncrypted(
        context: Context,
        databaseName: String,
        passphrase: String,
    ) {
        val dbFile = context.getDatabasePath(databaseName)
        if (!dbFile.exists()) return

        val tempFile = File(dbFile.parent, "${databaseName}_encrypted")
        val unencryptedDb = SQLiteDatabase.openDatabase(
            dbFile.absolutePath,
            "",
            null,
            SQLiteDatabase.OPEN_READWRITE,
        )
        unencryptedDb.rawExecSQL(
            "ATTACH DATABASE '${tempFile.absolutePath}' AS encrypted KEY '$passphrase'",
        )
        unencryptedDb.rawExecSQL("SELECT sqlcipher_export('encrypted')")
        unencryptedDb.rawExecSQL("DETACH DATABASE encrypted")
        unencryptedDb.close()

        dbFile.delete()
        tempFile.renameTo(dbFile)
    }
}
```
