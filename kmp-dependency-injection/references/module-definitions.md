# Koin Module Definitions

All modules in `commonMain/kotlin/{your/package}/di/modules/`. Each module is a separate file.

## coreModule

```kotlin
package {your.package}.di.modules

import {your.package}.platform.PlatformContext
import org.koin.core.module.Module
import org.koin.dsl.module

fun coreModule(context: PlatformContext): Module = module {
    single<PlatformContext> { context }
}
```

## localStorageModule

```kotlin
package {your.package}.di.modules

import org.koin.dsl.module

val localStorageModule = module {
    single<AppDatabase> { get<DatabaseFactory>().create() }
    single<DataStore<Preferences>> { get<DataStoreFactory>().create() }
}
```

## sessionModule

```kotlin
package {your.package}.di.modules

import org.koin.dsl.module

val sessionModule = module {
    single<SessionManager> {
        SessionManager(dataStore = get(), database = get())
    }
}
```

## ktorfitModule

Uses enum-based qualifiers for compile-time safety. See
[koin-qualifiers.kt](../assets/snippets/koin-qualifiers.kt) for alternative patterns.

```kotlin
package {your.package}.di.modules

import io.ktor.client.HttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

enum class HttpClientType { REFRESH, AUTHORIZED }

val ktorfitModule = module {
    single<HttpClient>(named(HttpClientType.REFRESH.name)) {
        createRefreshHttpClient(engineFactory = get())
    }

    single<HttpClient>(named(HttpClientType.AUTHORIZED.name)) {
        createAuthorizedHttpClient(
            engineFactory = get(),
            sessionManager = get(),
            refreshHttpClient = get(named(HttpClientType.REFRESH.name)),
        )
    }

    single<Ktorfit> {
        Ktorfit.Builder()
            .httpClient(get<HttpClient>(named(HttpClientType.AUTHORIZED.name)))
            .build()
    }

    single<AuthService> { get<Ktorfit>().create() }
    single<StorageService> { get<Ktorfit>().create() }
}
```

## externalStorageModule

```kotlin
package {your.package}.di.modules

import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module

val externalStorageModule = module {
    single<ExternalStorageFactory> {
        ExternalStorageFactory(dispatcher = Dispatchers.Default)
    }
}
```

## imageLoaderModule

Canonical definition in `kmp-image-loader/references/image-loader-module.md`.
Registers `ImageAuthInterceptor` and `ImageLoader` with Ktor fetcher, SVG
decoder, and memory/disk cache. Include `imageLoaderModule` in `initKoin()`.

## repositoryModule

```kotlin
package {your.package}.di.modules

import org.koin.dsl.module

val repositoryModule = module {
    single<AccountRepository> {
        AccountRepositoryImpl(
            authService = get(),
            storageService = get(),
            sessionManager = get(),
            database = get(),
        )
    }
}
```

## viewModelModule

```kotlin
package {your.package}.di.modules

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel<AccountViewModel> {
        AccountViewModel(accountRepository = get())
    }
}
```
