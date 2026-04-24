# Dependency Injection Integration

## Upstream Dependencies

| Skill | Provides |
|-------|----------|
| kmp-project-setup | Version catalog, source set layout, build.gradle.kts |
| kmp-architecture | PlatformContext, PlatformConfig, expect/actual pattern |

## Downstream Consumers

| Skill | What it needs from DI |
|-------|----------------------|
| kmp-database | localStorageModule provides AppDatabase via DatabaseFactory |
| kmp-networking | ktorfitModule provides typed HttpClient instances and Ktorfit |
| kmp-datastore | localStorageModule provides DataStore via DataStoreFactory |
| kmp-architecture | viewModelModule registers ViewModels, repositoryModule registers repos; ViewModels with StateFlow resolved from Koin graph |
| kmp-compose-ui | Screen injection via koinInject() and koinViewModel() |

### Adding a New Feature

1. Define the interface in commonMain
2. Implement the class in the appropriate source set
3. Create a Koin module -- see [new-koin-module.kt.template](../assets/templates/new-koin-module.kt.template)
4. Add the module to `commonModules()` in CommonModules.kt
5. Inject via constructor parameters in other Koin-managed classes, or `koinInject()` in screens

## Wiring Diagram

```
kmp-project-setup
  -> kmp-architecture (PlatformContext, PlatformConfig)
    -> kmp-dependency-injection
      -> kmp-database, kmp-networking, kmp-datastore
      -> kmp-architecture, kmp-compose-ui
```

## Screen Injection

Use `koinViewModel<T>()` for ViewModels and `koinInject<T>()` for other dependencies:

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AccountScreen(viewModel: AccountViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
}

@Composable
fun SettingsScreen() {
    val sessionManager = koinInject<SessionManager>()
    val imageLoader = koinInject<ImageLoader>()
}
```

## Scope Management

- `single { }` -- app-scoped singletons (repositories, HTTP clients, databases)
- `viewModel { }` -- scoped to Compose navigation lifecycle
- `factory { }` -- new instance per injection site. Avoid for heavy objects.

## Module Loading Order

Recommended declaration order (least to most dependent):

1. coreModule -- no dependencies
2. localStorageModule -- depends on platform DatabaseFactory, DataStoreFactory
3. sessionModule -- depends on DataStore, AppDatabase
4. ktorfitModule -- depends on HttpClientEngineFactory, SessionManager
5. externalStorageModule -- standalone
6. imageLoaderModule -- depends on SessionManager, HttpClient
7. repositoryModule -- depends on services, session, database
8. viewModelModule -- depends on repositories
