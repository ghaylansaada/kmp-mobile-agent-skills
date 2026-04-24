# DI Module Organization

## ViewModel Module

```kotlin
package com.example.app.di

import com.example.app.presentation.user.AccountViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun viewModelModule() = module {
    viewModel<AccountViewModel> { AccountViewModel(accountRepository = get()) }
}
```

## Repository Module

```kotlin
package com.example.app.di

import com.example.app.data.repository.AccountRepository
import com.example.app.data.repository.AccountRepositoryImpl
import org.koin.dsl.module

fun repositoryModule() = module {
    single<AccountRepository> {
        AccountRepositoryImpl(
            authService = get(),
            database = get(),
        )
    }
}
```

## UseCase Module (optional)

```kotlin
package com.example.app.di

import com.example.app.domain.usecase.GetAccountUseCase
import com.example.app.domain.usecase.TransformAccountUseCase
import org.koin.dsl.module

fun useCaseModule() = module {
    factory { GetAccountUseCase(accountRepository = get()) }
    factory { TransformAccountUseCase(accountRepository = get()) }
}
```

Use `factory` for UseCases (stateless), `single` for Repositories (stateful with database connections), `viewModel` for ViewModels (lifecycle-scoped).

## Registration

```kotlin
package com.example.app.di

import org.koin.core.context.startKoin

fun initKoin() = startKoin {
    modules(
        repositoryModule(),
        useCaseModule(),
        viewModelModule(),
    )
}
```

## Compose Injection

```kotlin
import androidx.compose.runtime.Composable
import com.example.app.presentation.user.AccountViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AccountScreen(viewModel: AccountViewModel = koinViewModel()) {
    // ...
}
```

## Platform Type Verification

When modifying architecture, verify no platform types leak through shared interfaces:

- Repository interfaces: only Kotlin/KMP types (`Flow`, `suspend`, data classes)
- UiState sealed interfaces: no Android or iOS imports
- ViewModel: no `android.*` or `platform.*` imports
- Entity classes in shared code: no platform annotations
