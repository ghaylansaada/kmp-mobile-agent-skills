# Services and DTOs

## Error DTOs

```kotlin
package {your.package}.core.network.result

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ApiFailedResponseDTO(
    @SerialName("message") val message: String?,
    @Contextual @SerialName("status") val status: HttpStatusCode?,
    @SerialName("code") val code: String?,
    @SerialName("errors") val errors: List<ApiError>?,
)

@Serializable
data class ApiError(
    @SerialName("path") val path: String?,
    @SerialName("code") val code: ApiErrorCode?,
    @SerialName("message") val message: String?,
    @SerialName("location") val location: ApiErrorLocation?,
    @SerialName("data") val data: JsonElement?,
)

@Serializable
enum class ApiErrorCode {
    REQUIRED_VIOLATION,
    EQUALITY_VIOLATION,
    EMAIL_FORMAT_VIOLATION,
    PATTERN_VIOLATION,
    STRING_LENGTH_VIOLATION,
    PASSWORD_LENGTH_VIOLATION,
    // ... additional validation codes per API spec
}

@Serializable
enum class ApiErrorLocation { QUERY, HEADER, PATH, BODY, BUSINESS }
```

## Auth DTOs

```kotlin
package {your.package}.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RefreshAuthRequestDto(
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class RefreshAuthResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
)
```

## Service Interfaces

```kotlin
package {your.package}.data.remote.service

import {your.package}.core.network.result.ApiResult
import {your.package}.data.remote.dto.AccountDto
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Query

interface AuthService {
    @GET("accounts")
    suspend fun getAccounts(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): ApiResult<List<AccountDto>>
}
```

```kotlin
package {your.package}.data.remote.service

import {your.package}.core.network.result.ApiResult
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Query

interface StorageService {
    @GET("request-download")
    suspend fun getDownloadUrl(
        @Query("path") path: String,
    ): ApiResult<String>
}
```

Use the [new-service.kt.template](../assets/templates/new-service.kt.template) to scaffold additional services.

## KtorfitModule (DI)

```kotlin
package {your.package}.di

import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import {your.package}.core.network.HttpClientEngineFactory
import {your.package}.core.network.HttpClientFactory
import {your.package}.core.network.KtorfitFactory
import {your.package}.data.remote.service.AuthService
import {your.package}.data.remote.service.StorageService
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal const val QUALIFIER_NAME_REFRESH_CLIENT = "refreshHttpClient"
internal const val QUALIFIER_NAME_AUTHORIZATION_CLIENT = "authorizedHttpClient"

fun ktorfitModule() = module {
    single<HttpClient>(named(QUALIFIER_NAME_REFRESH_CLIENT)) {
        HttpClientFactory.createRefreshClient(
            get<HttpClientEngineFactory>().create(),
        )
    }

    single<HttpClient>(named(QUALIFIER_NAME_AUTHORIZATION_CLIENT)) {
        HttpClientFactory.createAuthorizedClient(
            engine = get<HttpClientEngineFactory>().create(),
            sessionManager = get(),
            refreshClient = get(named(QUALIFIER_NAME_REFRESH_CLIENT)),
        )
    }

    single<Ktorfit> {
        KtorfitFactory.create(
            httpClient = get(named(QUALIFIER_NAME_AUTHORIZATION_CLIENT)),
        )
    }

    single<AuthService> { get<Ktorfit>().create() }
    single<StorageService> { get<Ktorfit>().create() }
}
```
