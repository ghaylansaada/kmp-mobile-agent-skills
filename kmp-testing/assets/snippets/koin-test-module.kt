package {your.package}.snippets

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Koin test module patterns for integration tests.
 * See SKILL.md Gotchas for Koin global-state and stopKoin() caveats.
 */

// --- Pattern 1: Simple test module ---
fun simpleTestModule(): Module = module {
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
    single<CoroutineDispatcher> { UnconfinedTestDispatcher() }
    single {
        HttpClient(MockEngine { respondOk("{}") }) {
            install(ContentNegotiation) {
                json(get<Json>())
            }
        }
    }
}

// --- Pattern 2: Configurable test module ---
fun configurableTestModule(
    mockEngine: MockEngine = MockEngine { respondOk("{}") },
    baseUrl: String = "https://api.test.com",
): Module = module {
    single { Json { ignoreUnknownKeys = true } }
    single<CoroutineDispatcher> { UnconfinedTestDispatcher() }
    single {
        HttpClient(mockEngine) {
            install(ContentNegotiation) { json(get<Json>()) }
            defaultRequest {
                url(baseUrl)
                contentType(ContentType.Application.Json)
            }
        }
    }
    // Add repository bindings here
}

// --- Pattern 3: Override production module ---
// val productionModule = module {
//     single<AccountRepository> { AccountRepositoryImpl(get(), get()) }
//     single { SessionManager() }
// }
//
// val testOverrides = module {
//     single<HttpClientEngine> { mockEngine }       // override engine
//     single<AppDatabase> { inMemoryDatabase }       // override database
// }
//
// startKoin {
//     modules(productionModule, testOverrides)  // testOverrides wins
// }

// --- Pattern 4: KoinTest base class (avoids global state collisions) ---
abstract class BaseIntegrationTest : KoinTest {

    open fun testModule(): Module = simpleTestModule()

    @BeforeTest
    fun baseSetUp() {
        startKoin {
            modules(testModule())
        }
    }

    @AfterTest
    fun baseTearDown() {
        try { stopKoin() } catch (_: IllegalStateException) {}
    }
}

// Usage:
// class MyFeatureIntegrationTest : BaseIntegrationTest() {
//     override fun testModule() = configurableTestModule(myMockEngine)
//     private val viewModel: MyViewModel by inject()
//
//     @Test
//     fun testFeature() = runTest { ... }
// }
