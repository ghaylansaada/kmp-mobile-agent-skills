package com.example.app.di.qualifiers

// Qualifier patterns for disambiguating multiple instances of the same type.
// The template uses enum-based qualifiers (Pattern 1) as the recommended default.
// See ktorfitModule in module-definitions.md for usage in context.

import io.ktor.client.HttpClient
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.QualifierValue
import org.koin.core.qualifier.named
import org.koin.dsl.module

// --- Pattern 1: Enum-Based Qualifiers (recommended) ---
// Compile-time safe. Typos cause compilation errors, not runtime crashes.

enum class HttpClientType { REFRESH, AUTHORIZED }

val enumQualifierModule = module {
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
}

// --- Pattern 2: Custom Qualifier Object (for complex disambiguation) ---
// Type-safe with no string matching. Best for versioned or parameterized bindings.

data class ApiVersion(val version: String) : Qualifier {
    override val value: QualifierValue = "api-$version"
}

val versionedApiModule = module {
    single<ApiClient>(ApiVersion("v1")) {
        ApiClient(baseUrl = "https://api.example.com/v1")
    }
    single<ApiClient>(ApiVersion("v2")) {
        ApiClient(baseUrl = "https://api.example.com/v2")
    }
}

// Resolution:
// single<FeatureRepository> {
//     FeatureRepositoryImpl(apiClient = get(ApiVersion("v2")))
// }

// --- Pattern 3: Const String Qualifiers (fallback only) ---
// Use only when interoperating with libraries that require string keys.
// Prefer Pattern 1 or 2 for application code.

object QualifierNames {
    const val REFRESH_HTTP_CLIENT = "refreshHttpClient"
    const val AUTHORIZED_HTTP_CLIENT = "authorizedHttpClient"
}

val constantQualifierModule = module {
    single<HttpClient>(named(QualifierNames.REFRESH_HTTP_CLIENT)) {
        createRefreshHttpClient(engineFactory = get())
    }
    single<HttpClient>(named(QualifierNames.AUTHORIZED_HTTP_CLIENT)) {
        createAuthorizedHttpClient(
            engineFactory = get(),
            sessionManager = get(),
            refreshHttpClient = get(named(QualifierNames.REFRESH_HTTP_CLIENT)),
        )
    }
}

// --- Anti-Pattern: Bare get<T>() with Multiple Registrations ---
// DO NOT use bare get<HttpClient>() when multiple HttpClient instances are registered.
// Koin resolves to the last-registered instance silently.
// ALWAYS use a typed qualifier: get(named(HttpClientType.AUTHORIZED.name))
