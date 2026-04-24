# HTTP Client

## HttpClientEngineFactory

```kotlin
package {your.package}.core.network

import io.ktor.client.engine.HttpClientEngine

interface HttpClientEngineFactory {
    fun create(): HttpClientEngine
}
```

## HttpClientFactory

```kotlin
package {your.package}.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

object HttpClientFactory {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        serializersModule = SerializersModule {
            contextual(HttpStatusCode::class, HttpStatusCodeSerializer)
        }
    }
    private val refreshMutex = Mutex()
    private const val CONNECT_TIMEOUT_MS = 15_000L
    private const val REQUEST_TIMEOUT_MS = 60_000L
    private const val SOCKET_TIMEOUT_MS = 30_000L
    private const val MAX_RETRIES = 3

    fun createAuthorizedClient(
        engine: HttpClientEngine, sessionManager: SessionManager, refreshClient: HttpClient,
    ): HttpClient = HttpClient(engine) {
        install(ContentNegotiation) { json(json) }
        install(Logging) { logger = Logger.DEFAULT; level = LogLevel.INFO }
        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            socketTimeoutMillis = SOCKET_TIMEOUT_MS
        }
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = MAX_RETRIES)
            retryOnException(maxRetries = MAX_RETRIES, retryOnTimeout = true)
            exponentialDelay()
        }
        install(Auth) {
            bearer {
                loadTokens { sessionManager.getTokens() }
                refreshTokens {
                    val current = sessionManager.getTokens() ?: return@refreshTokens null
                    current.refreshToken ?: return@refreshTokens null
                    refreshMutex.withLock {
                        val existing = sessionManager.getTokens()?.accessToken
                        if (!existing.isNullOrBlank()) return@withLock BearerTokens(
                            accessToken = existing,
                            refreshToken = sessionManager.getTokens()?.refreshToken.orEmpty(),
                        )
                        val refreshed = refreshTokens(refreshClient, current.refreshToken!!) ?: run {
                            sessionManager.revokeSession()
                            return@refreshTokens null
                        }
                        sessionManager.setTokens(refreshed.accessToken, refreshed.refreshToken)
                        BearerTokens(refreshed.accessToken, refreshed.refreshToken)
                    }
                }
                sendWithoutRequest { request ->
                    !request.url.encodedPath.endsWith(AppConfig.REFRESH_AUTH_PATH.trimStart('/'))
                }
            }
        }
        defaultRequest { header(HttpHeaders.Accept, ContentType.Application.Json) }
    }

    fun createRefreshClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
        install(ContentNegotiation) { json(json) }
        install(Logging) { logger = Logger.DEFAULT; level = LogLevel.INFO }
        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            socketTimeoutMillis = SOCKET_TIMEOUT_MS
        }
        defaultRequest { header(HttpHeaders.Accept, ContentType.Application.Json) }
    }

    private suspend fun refreshTokens(
        refreshClient: HttpClient, refreshToken: String,
    ): RefreshAuthResponseDto? {
        val url = "${AppConfig.BASE_URL.trimEnd('/')}/${AppConfig.REFRESH_AUTH_PATH.trimStart('/')}"
        return runCatching {
            refreshClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(RefreshAuthRequestDto(refreshToken = refreshToken))
            }.body<RefreshAuthResponseDto>()
        }.getOrNull()
    }
}
```

## AppConfig

Base URL should be injected via BuildKonfig at build time. Replace with your BuildKonfig field:

```kotlin
package {your.package}.core.network

object AppConfig {
    // Replace with BuildKonfig.BASE_URL for environment switching
    const val BASE_URL: String = "https://api.example.com/"
    const val REFRESH_AUTH_PATH: String = "auth/refresh"
}
```

## Platform Engines

**Android (androidMain):**
```kotlin
package {your.package}.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import {your.package}.core.network.HttpClientEngineFactory
import org.koin.dsl.module

val androidEngineModule = module {
    single<HttpClientEngineFactory> {
        object : HttpClientEngineFactory {
            override fun create(): HttpClientEngine = OkHttp.create()
        }
    }
}
```

**iOS (iosMain):**
```kotlin
package {your.package}.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import {your.package}.core.network.HttpClientEngineFactory
import org.koin.dsl.module

val iosEngineModule = module {
    single<HttpClientEngineFactory> {
        object : HttpClientEngineFactory {
            override fun create(): HttpClientEngine = Darwin.create()
        }
    }
}
```
