# API Types and Converter

## ApiResult

```kotlin
package {your.package}.core.network.result

import io.ktor.http.HttpStatusCode

sealed interface ApiResult<out T> {
    data object Loading : ApiResult<Nothing>

    sealed interface Error : ApiResult<Nothing> {
        val message: String?
        val exception: Throwable?

        data class UnknownError(override val exception: Throwable) : Error {
            override val message: String? get() = exception.message
        }
        data class ParsingError(override val exception: Throwable) : Error {
            override val message: String? get() = exception.message
        }
        data class InternetError(override val exception: Throwable) : Error {
            override val message: String? get() = exception.message
        }
        data class HttpError(
            val status: HttpStatusCode,
            val errorCode: String? = null,
            override val message: String? = null,
            val errors: List<ApiError>? = null,
            override val exception: Throwable? = null,
        ) : Error
    }

    data class Success<T>(val data: T?, val pagination: ApiResultPaging?) : ApiResult<T>
}
```

## ApiResultPaging

```kotlin
package {your.package}.core.network.result

import kotlinx.serialization.Serializable

@Serializable
data class ApiResultPaging(
    val currentPage: Long = 0, 
    val totalItems: Long = 0,
    val pageSize: Long = 0, 
    val totalPages: Long = 0)
```

## ApiCall

```kotlin
package {your.package}.core.network

import {your.package}.core.network.result.ApiResult
import {your.package}.core.network.result.ApiResultPaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

abstract class ApiCall<T> {
    suspend fun await(): ApiResult<T> = withContext(Dispatchers.IO) {
        val result = webserviceCall()
        if (result is ApiResult.Success<T>) onSuccess(result.data, result.pagination)
        else if (result is ApiResult.Error) onFailure(result)
        result
    }

    abstract suspend fun webserviceCall(): ApiResult<T>
    open suspend fun onSuccess(data: T?, paging: ApiResultPaging?) {}
    open suspend fun onFailure(error: ApiResult.Error) {}
}
```

## ApiCallException

```kotlin
package {your.package}.core.network

import {your.package}.core.network.result.ApiResult

class ApiCallException(
    val error: ApiResult.Error, message: String?, cause: Throwable? = null,
) : RuntimeException(message, cause)
```

## HttpStatusCodeSerializer

```kotlin
package {your.package}.core.network

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object HttpStatusCodeSerializer : KSerializer<HttpStatusCode> {
    override val descriptor = PrimitiveSerialDescriptor("HttpStatusCode", PrimitiveKind.INT)
    override fun serialize(encoder: Encoder, value: HttpStatusCode) { encoder.encodeInt(value.value) }
    override fun deserialize(decoder: Decoder): HttpStatusCode = HttpStatusCode.fromValue(decoder.decodeInt())
}
```

## KtorfitFactory

```kotlin
package {your.package}.core.network

import de.jensklingenberg.ktorfit.Ktorfit
import de.jensklingenberg.ktorfit.converter.Converter
import de.jensklingenberg.ktorfit.converter.KtorfitResult
import de.jensklingenberg.ktorfit.converter.TypeData
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import io.ktor.serialization.JsonConvertException
import io.ktor.utils.io.errors.IOException
import {your.package}.core.network.result.ApiError
import {your.package}.core.network.result.ApiResult
import {your.package}.core.network.result.ApiResultPaging
import kotlinx.serialization.SerializationException

object KtorfitFactory {
    private const val X_TOTAL_COUNT = "X-Total-Count"
    private const val X_TOTAL_PAGES = "X-Total-Pages"
    private const val X_CURRENT_PAGE = "X-Current-Page"
    private const val X_PAGE_SIZE = "X-Page-Size"

    fun create(httpClient: HttpClient): Ktorfit = Ktorfit.Builder()
        .httpClient(httpClient).baseUrl(AppConfig.BASE_URL)
        .converterFactories(converterFactory()).build()

    private fun converterFactory() = object : Converter.Factory {
        override fun suspendResponseConverter(
            typeData: TypeData, ktorfit: Ktorfit,
        ): Converter.SuspendResponseConverter<HttpResponse, *>? {
            if (typeData.typeInfo.type == ApiResult::class) return CustomConverter(typeData, ktorfit)
            return super.suspendResponseConverter(typeData, ktorfit)
        }
    }

    private class CustomConverter(
        private val typeData: TypeData, private val ktorfit: Ktorfit,
    ) : Converter.SuspendResponseConverter<HttpResponse, ApiResult<*>> {
        override suspend fun convert(result: KtorfitResult): ApiResult<*> = when (result) {
            is KtorfitResult.Success -> handleSuccess(result.response)
            is KtorfitResult.Failure -> handleFailure(result.throwable)
        }

        private suspend fun handleSuccess(response: HttpResponse): ApiResult<*> {
            if (response.status.isSuccess()) {
                val pagination = ApiResultPaging(
                    currentPage = response.headers[X_CURRENT_PAGE]?.toLongOrNull() ?: 0,
                    totalItems = response.headers[X_TOTAL_COUNT]?.toLongOrNull() ?: 0,
                    pageSize = response.headers[X_PAGE_SIZE]?.toLongOrNull() ?: 0,
                    totalPages = response.headers[X_TOTAL_PAGES]?.toLongOrNull() ?: 0,
                )
                val body = runCatching {
                    ktorfit.nextSuspendResponseConverter(null, typeData.typeArgs.first())
                        ?.convert(response.body())
                }.getOrNull()
                return ApiResult.Success(body, pagination)
            }
            val errorBody = runCatching {
                val t = TypeData(
                    ApiFailedResponseDTO::class.qualifiedName!!,
                    typeInfo<ApiFailedResponseDTO>(),
                )
                ktorfit.nextSuspendResponseConverter(null, t)
                    ?.convert(response.body()) as? ApiFailedResponseDTO
            }.getOrNull()
            return ApiResult.Error.HttpError(
                status = response.status, errorCode = errorBody?.code,
                message = errorBody?.message, errors = errorBody?.errors,
            )
        }

        private fun handleFailure(e: Throwable): ApiResult.Error = when (e) {
            is ResponseException -> ApiResult.Error.HttpError(status = e.response.status, exception = e)
            is SerializationException, is JsonConvertException -> ApiResult.Error.ParsingError(e)
            is IOException -> ApiResult.Error.InternetError(e)
            else -> ApiResult.Error.UnknownError(e)
        }
    }
}
```
