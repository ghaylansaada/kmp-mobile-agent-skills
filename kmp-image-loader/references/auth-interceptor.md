# ImageAuthInterceptor

**File:** `commonMain/kotlin/{your/package}/core/image/ImageAuthInterceptor.kt`

```kotlin
package {your.package}.core.image

import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.ImageResult
import {your.package}.core.network.result.ApiResult
import {your.package}.data.remote.service.StorageService
import io.ktor.http.Url

class ImageAuthInterceptor(
    private val storageService: StorageService,
) : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val data = request.data as? String ?: return chain.proceed()

        val resolvedUrl = if (data.startsWith("/")) {
            fetchNewUrl(data) ?: data
        } else data

        val stableCacheKey = urlToKey(data)

        var result = buildRequest(chain, request, resolvedUrl, stableCacheKey)

        if (result is ErrorResult && isUrlExpired(result.throwable)) {
            val freshUrl = fetchNewUrl(data) ?: return result
            result = buildRequest(chain, request, freshUrl, stableCacheKey)
        }

        return result
    }

    private suspend fun buildRequest(
        chain: Interceptor.Chain,
        request: ImageRequest,
        url: String,
        cacheKey: String?,
    ): ImageResult = chain.withRequest(
        request.newBuilder()
            .data(url)
            .diskCacheKey(cacheKey)
            .build()
    ).proceed()

    internal fun urlToKey(input: String): String = runCatching {
        val url = Url(input)
        if (url.protocol.name.isNotEmpty()) url.encodedPath else input
    }.getOrDefault(input)

    private suspend fun fetchNewUrl(path: String): String? = runCatching {
        val response = storageService.getDownloadUrl(path)
        (response as? ApiResult.Success)?.data
    }.getOrNull()

    internal fun isUrlExpired(throwable: Throwable): Boolean =
        throwable.message?.contains("401") == true ||
            throwable.message?.contains("403") == true
}
```

**Flow:** If data is a path starting with `/`, `fetchNewUrl()` resolves it to a signed URL via StorageService. `urlToKey()` extracts the stable path as cache key. If the request fails with 401/403, fetches a fresh signed URL and retries with the same cache key.

## StorageService Interface

```kotlin
package {your.package}.data.remote.service

import {your.package}.core.network.result.ApiResult
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Query

interface StorageService {
    @GET("/request-download")
    suspend fun getDownloadUrl(@Query("path") path: String): ApiResult<String>
}
```

Ktorfit-generated API client. Returns a signed URL with authentication token and expiration as query parameters.
