# Database Integration

## Data Flow

```
+----------------+  +-------------------+  +------------------+
| kmp-paging     |->| kmp-database      |<-| Repositories     |
| (PagingSource, |  | (AppDatabase,     |  | (DAO access,     |
|  RemoteKeyDao) |  |  DAOs, Entities)  |  |  cache updates)  |
+----------------+  +--------+----------+  +------------------+
                             |
                    +--------+----------+
                    | kmp-dependency-inj |
                    | (DatabaseFactory,  |
                    |  platformModule)   |
                    +-------------------+
```

## Downstream Consumers

- **kmp-paging**: Consumes `PagingSource` from DAOs, uses `RemoteKeyDao` for pagination cursors.
- **Repositories**: Inject `AppDatabase`, access DAOs for local-first data operations.
- **kmp-session-management**: Calls `database.clearAllTables()` during logout.

## Adding a New Feature That Uses the Database

1. **This skill**: Add entity + DAO, register in `@Database`
2. **kmp-paging** (if paginated): Add `PagingSource` to DAO, create `RemoteMediator`
3. **Repository**: Wrap DAO operations with network/cache strategy
4. **kmp-dependency-injection**: Register repository in Koin module

After adding a new entity:
- Add to `@Database(entities = [...])` in AppDatabase
- Bump database version
- Add migration spec in `AppDatabaseMigrations`
- Add `AutoMigration` entry in `@Database` annotation
