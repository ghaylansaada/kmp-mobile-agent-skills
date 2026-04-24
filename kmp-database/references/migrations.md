# Database Migrations

## AutoMigration Specs

Room auto-migrations handle simple schema changes when `exportSchema = true` and `roomSchemas/` contains the previous version's JSON.

**File:** `commonMain/.../data/local/AppDatabaseMigrations.kt`

```kotlin
package {your.package}.data.local

import androidx.room.DeleteColumn
import androidx.room.DeleteTable
import androidx.room.RenameColumn
import androidx.room.RenameTable
import androidx.room.migration.AutoMigrationSpec

object AppDatabaseMigrations {

    // Simple additive changes (no spec needed):
    // AutoMigration(from = 1, to = 2)

    // Rename a column
    @RenameColumn(
        tableName = "accounts",
        fromColumnName = "name",
        toColumnName = "display_name",
    )
    class From1To2 : AutoMigrationSpec

    // Rename a table
    @RenameTable(
        fromTableName = "old_cache",
        toTableName = "new_cache",
    )
    class From2To3 : AutoMigrationSpec

    // Delete a column
    @DeleteColumn(
        tableName = "accounts",
        columnName = "deprecated_field",
    )
    class From3To4 : AutoMigrationSpec

    // Delete a table
    @DeleteTable(tableName = "temp_data")
    class From4To5 : AutoMigrationSpec

    // Combined changes in one version bump
    @RenameColumn(
        tableName = "accounts",
        fromColumnName = "bio",
        toColumnName = "description",
    )
    @DeleteColumn(
        tableName = "accounts",
        columnName = "temp_field",
    )
    class From5To6 : AutoMigrationSpec
}
```

## Registering AutoMigrations

```kotlin
@Database(
    version = 6,
    exportSchema = true,
    entities = [AccountEntity::class, RemoteKeyEntity::class],
    autoMigrations = [
        AutoMigration(from = 1, to = 2, spec = AppDatabaseMigrations.From1To2::class),
        AutoMigration(from = 2, to = 3, spec = AppDatabaseMigrations.From2To3::class),
        AutoMigration(from = 3, to = 4, spec = AppDatabaseMigrations.From3To4::class),
        AutoMigration(from = 4, to = 5, spec = AppDatabaseMigrations.From4To5::class),
        AutoMigration(from = 5, to = 6, spec = AppDatabaseMigrations.From5To6::class),
    ],
)
abstract class AppDatabase : RoomDatabase() { /* ... */ }
```

## Manual Migration (for complex data transforms)

Room KMP uses `SQLiteConnection` for manual migrations, not the legacy
`SupportSQLiteDatabase` API.

```kotlin
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE accounts ADD COLUMN email TEXT NOT NULL DEFAULT ''",
        )
    }
}

// Add to builder: .addMigrations(MIGRATION_6_7)
```

## AutoMigration Capabilities

| Supported | Not Supported |
|-----------|---------------|
| Add columns (with defaults) | Complex data transforms |
| Add tables | Column type changes |
| Add indices | Data migration between tables |
| Rename columns/tables | Conditional logic |
| Delete columns/tables | |

## Development Shortcut

During development only, use `fallbackToDestructiveMigration(true)` to drop and
recreate. Never use in production -- destroys all user data on schema change.
