# Data Layer: Repository, ApiCall, UseCase

## Repository Interface

Repositories define a clean interface with `Flow` return types. Never use `LiveData` (Android-only).

```kotlin
package com.example.app.data.repository

import com.example.app.data.local.entity.AccountEntity
import com.example.app.data.remote.util.ApiResult
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun observeAccount(): Flow<AccountEntity?>
    suspend fun callApi(): ApiResult<List<AccountEntity>>
}
```

## Repository Implementation

```kotlin
package com.example.app.data.repository

import com.example.app.data.local.AppDatabase
import com.example.app.data.local.entity.AccountEntity
import com.example.app.data.remote.service.AuthService
import com.example.app.data.remote.util.ApiCall
import com.example.app.data.remote.util.ApiResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

class AccountRepositoryImpl(
    private val authService: AuthService,
    private val database: AppDatabase,
) : AccountRepository {

    override fun observeAccount(): Flow<AccountEntity?> =
        database.accountDao.selectAccount()
            .onStart { callApi() }

    override suspend fun callApi(): ApiResult<List<AccountEntity>> {
        return object : ApiCall<List<AccountEntity>>() {
            override suspend fun webserviceCall() = authService.getAccounts()
            override suspend fun onSuccess(data: List<AccountEntity>) {
                database.accountDao.insertAll(data)
            }
            override suspend fun onFailure(statusCode: Int, message: String?) { }
        }.execute()
    }
}
```

`observeAccount()` emits local data immediately via the DAO `Flow`, then triggers a network refresh via `onStart`. The `onStart` operator runs the API call when collection begins without blocking the downstream `Flow`. The repository depends only on interfaces injected via constructor.

## ApiCall Pattern

Each `ApiCall` encapsulates: network request (`webserviceCall`), persistence on success (`onSuccess`), and error handling (`onFailure`). The `execute()` method returns `ApiResult<T>`.

## UseCase Pattern

Add UseCases when repository exceeds ~200 lines or logic is shared across multiple ViewModels. Use `operator fun invoke()` for clean call sites.

```kotlin
package com.example.app.domain.usecase

import com.example.app.data.local.entity.AccountEntity
import com.example.app.data.repository.AccountRepository
import kotlinx.coroutines.flow.Flow

class GetAccountUseCase(
    private val accountRepository: AccountRepository,
) {
    operator fun invoke(): Flow<AccountEntity?> =
        accountRepository.observeAccount()
}
```

```kotlin
package com.example.app.domain.usecase

import com.example.app.data.repository.AccountRepository
import com.example.app.domain.model.AccountSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransformAccountUseCase(
    private val accountRepository: AccountRepository,
) {
    operator fun invoke(): Flow<AccountSummary> =
        accountRepository.observeAccount().map { entity ->
            AccountSummary(
                displayName = "${entity?.firstName} ${entity?.lastName}",
                isVerified = entity?.verifiedAt != null,
            )
        }
}
```

Register UseCases with `factory` (not `single`) in Koin -- they are lightweight and stateless.

## Directory Structure

```
data/
    local/
        AppDatabase.kt
        dao/AccountDao.kt
        entity/AccountEntity.kt
    remote/
        service/AuthService.kt
        dto/AccountDto.kt
        util/ApiCall.kt, ApiResult.kt
    repository/
        AccountRepository.kt
        AccountRepositoryImpl.kt
domain/
    model/
        AccountSummary.kt
    usecase/
        GetAccountUseCase.kt
        TransformAccountUseCase.kt
```
