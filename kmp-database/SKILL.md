---
name: kmp-database
description: >
  Use this skill when setting up or extending the Room KMP database layer. Activate
  when the user asks to "add an entity," "create a DAO," "set up Room," "add a
  migration," "wire database into DI," or "troubleshoot Room KSP." Covers database
  class definition, entities, DAOs, type converters, AutoMigration specs, platform-
  specific builders (Android Context / iOS NSDocumentDirectory), DatabaseFactory
  pattern, and Koin DI wiring. Does NOT cover paging internals (see kmp-paging),
  DataStore preferences (see kmp-datastore), or general DI setup (see
  kmp-dependency-injection).
compatibility: >
  KMP with Compose Multiplatform. Requires Room KMP, KSP, and SQLite bundled driver.
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---

# KMP Database (Room)

## When to use

- Setting up Room KMP in a new project
- Adding a new entity and DAO pair
- Creating or modifying type converters
- Adding schema migrations (auto or manual)
- Wiring the database into Koin DI
- Creating platform-specific database builders
- Implementing custom `clearAllTables()` logic
- Troubleshooting Room KSP code generation

## Depends on

- **kmp-project-setup** -- KMP project with Compose Multiplatform
- **kmp-dependency-injection** -- Koin platform modules for DatabaseFactory registration

## Workflow

1. Add Room, KSP, and SQLite dependencies --> [setup.md](references/setup.md)
   _Skip if Room is already configured in the project._
2. Define database schema (AppDatabase, Constructor, Converters, DatabaseFactory) --> [database-schema.md](references/database-schema.md)
3. Define entities and DAOs --> [entities-and-daos.md](references/entities-and-daos.md)
4. Create platform builders and wire DI --> [platform-builders.md](references/platform-builders.md)
5. Add migrations --> [migrations.md](references/migrations.md)
   _Read when changing the schema after initial setup._
6. Understand integration points --> [integration.md](references/integration.md)
   _Read when wiring a new feature that uses the database._
7. Scaffold a new entity + DAO pair --> `assets/templates/new-entity-dao.kt.template`
   _Load only when adding an entirely new entity._

## Gotchas

1. **Room KSP auto-generates `actual` for `expect object AppDatabaseConstructor`.** Never write manual actual declarations -- causes `DuplicateClassException` at compile time and blocks the entire build.
2. **`@Upsert` internally does DELETE + INSERT on conflict.** If the entity has foreign keys with `ON DELETE CASCADE`, this cascades to child tables, silently destroying related data that appears unrelated to the upsert.
3. **`BundledSQLiteDriver` adds ~2 MB to APK size.** Necessary for consistent SQLite behavior across Android API levels. iOS uses the system-linked SQLite via `-lsqlite3` linker flag -- no bundled driver needed.
4. **KSP must be configured per-target, not generically.** Using `ksp(libs.room.compiler)` instead of `add("kspAndroid", libs.room.compiler)` silently skips code generation, producing "Cannot find implementation for AppDatabase" at runtime.
5. **Custom `clearAllTables()` must use `useWriterConnection` with `immediateTransaction`.** Using a regular transaction allows WAL checkpoint races where a concurrent reader sees partially cleared state, causing UI to display stale rows from some tables but not others.
6. **Room `@Query` returning `Flow` on iOS dispatches on `Dispatchers.Default`, not `Main`.** Collecting this Flow and updating UI state directly causes `NSInternalInconsistencyException` for UIKit or silent state corruption for Compose. Always switch to the main dispatcher before emitting to UI state.
7. **`roomSchemas/` directory must be committed to version control.** Without the previous version JSON, AutoMigrations fail at runtime with "IllegalStateException: A migration from X to Y was required but not found," crashing the app on upgrade.
8. **iOS framework must link system SQLite.** Missing `linkerOpts.add("-lsqlite3")` in the iOS framework block causes an undefined symbol linker error that only surfaces during iOS archive builds, not during Kotlin compilation.
9. **Manual migrations use `SQLiteConnection`, not `SupportSQLiteDatabase`.** Room KMP dropped the Android-only `SupportSQLiteDatabase` API. Using it causes unresolved reference errors that only appear when compiling commonMain.

## Assets

| Path | Load when... |
|-------|-------------|
| [setup.md](references/setup.md) | Adding Room dependencies to a project |
| [database-schema.md](references/database-schema.md) | Defining AppDatabase, Constructor, Converters, DatabaseFactory |
| [entities-and-daos.md](references/entities-and-daos.md) | Adding or modifying entities and DAOs |
| [platform-builders.md](references/platform-builders.md) | Creating platform builders or wiring DI |
| [migrations.md](references/migrations.md) | Adding schema migrations after initial setup |
| [integration.md](references/integration.md) | Wiring a new feature that uses the database |
| [new-entity-dao.kt.template](assets/templates/new-entity-dao.kt.template) | Scaffolding a new entity + DAO pair |

## Validation

### A. Build and compilation
- [ ] KSP configured per-target (not generic `ksp(...)`)
- [ ] `roomSchemas/` directory exists and is committed

### B. Database correctness
- [ ] No manual `actual object AppDatabaseConstructor` declarations
- [ ] All queries returning reactive data use `Flow` (not blocking calls)
- [ ] Migrations numbered sequentially with no gaps
- [ ] Multi-statement operations wrapped in `useWriterConnection` + `immediateTransaction`
- [ ] No raw SQL in Kotlin code outside `clearAllTables()` (all queries in `@Query` annotations)
- [ ] No `android.*` imports in commonMain source sets
- [ ] `Dispatchers.IO` used for Android database context, `Dispatchers.Default` for iOS
- [ ] `linkerOpts.add("-lsqlite3")` present in iOS framework configuration
- [ ] Type converters registered on AppDatabase via `@TypeConverters`

### C. Security
- [ ] All DAO queries use parameterized arguments (`:paramName`), never string concatenation
- [ ] `clearAllTables()` uses prepared statements for system queries
- [ ] Sensitive data at rest evaluated for encryption needs (SQLCipher if required)

### D. Performance
- [ ] Entities with frequent lookups have appropriate `@ColumnInfo(index = true)` or `@Entity(indices = [...])`
- [ ] No N+1 query patterns (e.g., querying related entities in a loop)
- [ ] Large result sets use `PagingSource` instead of `Flow<List<*>>`
- [ ] `BundledSQLiteDriver` set on Android builder (enables WAL mode by default)

### E. Integration
- [ ] `AppDatabase` registered as Koin `single` (not `factory`)
- [ ] `DatabaseFactory` implementations exist for both Android and iOS
- [ ] New entities added to `@Database(entities = [...])` annotation
- [ ] New DAOs exposed as abstract properties on `AppDatabase`
