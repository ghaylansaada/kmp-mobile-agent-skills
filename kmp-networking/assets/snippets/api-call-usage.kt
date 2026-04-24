package {your.package}.data.repository

import {your.package}.core.network.ApiCall
import {your.package}.core.network.result.ApiResult
import {your.package}.core.network.result.ApiResultPaging
import {your.package}.data.local.AppDatabase
import {your.package}.data.remote.dto.AccountDto
import {your.package}.data.remote.service.AuthService

/**
 * Repository pattern using ApiCall to fetch data from the network,
 * persist it locally on success, and handle errors.
 */
class AccountRepositoryExample(
    private val authService: AuthService,
    private val database: AppDatabase,
) {

    suspend fun fetchAccounts(): ApiResult<List<AccountDto>> {
        return object : ApiCall<List<AccountDto>>() {

            override suspend fun webserviceCall(): ApiResult<List<AccountDto>> {
                return authService.getAccounts(page = 0, limit = 20)
            }

            override suspend fun onSuccess(
                data: List<AccountDto>?,
                paging: ApiResultPaging?,
            ) {
                database.accountDao.upsertAll(data ?: return)
            }

            override suspend fun onFailure(error: ApiResult.Error) {
                error.exception?.printStackTrace()
            }

        }.await()
    }
}

// ViewModel usage:
//
// fun loadAccounts() {
//     viewModelScope.launch {
//         _uiState.value = UiState.Loading
//
//         when (val result = repository.fetchAccounts()) {
//             is ApiResult.Success -> {
//                 _uiState.value = UiState.Success(result.data.orEmpty())
//             }
//             is ApiResult.Error.HttpError -> {
//                 _uiState.value = UiState.Error("Server error: ${result.status}")
//             }
//             is ApiResult.Error.InternetError -> {
//                 _uiState.value = UiState.Error("No internet connection")
//             }
//             is ApiResult.Error.ParsingError -> {
//                 _uiState.value = UiState.Error("Unexpected response format")
//             }
//             is ApiResult.Error.UnknownError -> {
//                 _uiState.value = UiState.Error("Something went wrong")
//             }
//         }
//     }
// }
