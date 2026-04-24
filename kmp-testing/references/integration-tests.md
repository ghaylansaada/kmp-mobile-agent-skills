# Integration Tests

End-to-end integration tests exercising the full stack from Ktor MockEngine
through Repository to ViewModel. Uses MockEngine as a fake HTTP server,
in-memory Room databases on Android, and real Koin dependency injection.

## MockEngineFactory

File: `composeApp/src/commonTest/kotlin/{your/package}/integration/MockEngineFactory.kt`

```kotlin
package {your.package}.integration

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

object MockEngineFactory {

    fun create(
        handlers: Map<String, MockRequestHandler> = defaultHandlers(),
    ): MockEngine = MockEngine { request ->
        val path = request.url.encodedPath
        val method = request.method
        val key = "${method.value} $path"
        val handler = handlers[key] ?: handlers[path] ?: { respondNotFound(path) }
        handler(request)
    }

    fun defaultHandlers(): Map<String, MockRequestHandler> = mapOf(
        "GET /api/accounts" to { _ -> respondJson(ACCOUNTS_JSON) },
        "GET /api/accounts/1" to { _ -> respondJson(SINGLE_ACCOUNT_JSON) },
        "POST /api/auth/login" to { _ -> respondJson(LOGIN_RESPONSE_JSON) },
        "POST /api/auth/logout" to { _ ->
            respond(content = "{}", status = HttpStatusCode.NoContent,
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        },
    )

    fun errorHandlers(
        statusCode: HttpStatusCode = HttpStatusCode.InternalServerError,
        message: String = "Server error",
    ): Map<String, MockRequestHandler> = mapOf(
        "GET /api/accounts" to { _ ->
            respondJson(
                content = """{"error": "$message", "code": ${statusCode.value}}""",
                status = statusCode,
            )
        },
    )

    private fun MockRequestHandleScope.respondJson(
        content: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): HttpResponseData = respond(content = content, status = status,
        headers = headersOf(HttpHeaders.ContentType, "application/json"))

    private fun MockRequestHandleScope.respondNotFound(path: String): HttpResponseData =
        respondError(status = HttpStatusCode.NotFound,
            content = """{"error": "No mock handler for $path"}""")
}

typealias MockRequestHandler = suspend MockRequestHandleScope.(
    request: HttpRequestData,
) -> HttpResponseData
```

## JSON Response Fixtures

```kotlin
const val ACCOUNTS_JSON = """
[
  {"id": "1", "username": "alice", "email": "alice@example.com",
   "display_name": "Alice", "avatar_url": null, "is_active": true},
  {"id": "2", "username": "bob", "email": "bob@example.com",
   "display_name": "Bob", "avatar_url": "https://example.com/bob.jpg", "is_active": true}
]"""

const val SINGLE_ACCOUNT_JSON = """
{"id": "1", "username": "alice", "email": "alice@example.com",
 "display_name": "Alice", "avatar_url": null, "is_active": true}"""

const val LOGIN_RESPONSE_JSON = """
{"token": "mock-jwt-token-123", "refresh_token": "mock-refresh-token-456",
 "expires_at": 9999999999,
 "account": {"id": "1", "username": "alice", "email": "alice@example.com",
  "display_name": "Alice", "avatar_url": null, "is_active": true}}"""
```

---

## IntegrationTestModule (Koin)

File: `composeApp/src/commonTest/kotlin/{your/package}/integration/IntegrationTestModule.kt`

```kotlin
package {your.package}.integration

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.serialization.json.Json
import org.koin.dsl.module

fun integrationTestModule(
    mockEngine: MockEngine = MockEngineFactory.create(),
) = module {
    single { Json { ignoreUnknownKeys = true; isLenient = true; prettyPrint = false } }
    single {
        HttpClient(mockEngine) {
            install(ContentNegotiation) { json(get<Json>()) }
            defaultRequest { url("https://api.example.com"); contentType(ContentType.Application.Json) }
        }
    }
    single<CoroutineDispatcher> { UnconfinedTestDispatcher() }
    single { SessionManager() }
    single<AccountRepository> { AccountRepositoryImpl(get(), get()) }
    factory { AccountViewModel(get(), get()) }
}
```

Validate the DI graph with `checkModules()`:

```kotlin
@Test
fun verifyKoinModule() {
    koinApplication {
        modules(integrationTestModule())
        checkModules()
    }
}
```

---

## Common Integration Tests

File: `composeApp/src/commonTest/kotlin/{your/package}/integration/AccountIntegrationTest.kt`

Verifies: MockEngine --> HttpClient --> Repository pipeline.

```kotlin
class AccountIntegrationTest : KoinTest {

    private val repository: AccountRepository by inject()

    @BeforeTest fun setUp() { startKoin { modules(integrationTestModule()) } }
    @AfterTest fun tearDown() {
        try { stopKoin() } catch (_: IllegalStateException) {}
    }

    @Test
    fun fetchAccountsReturnsParsedAccounts() = runTest {
        val result = repository.fetchAccounts()
        assertIs<ApiResult.Success<List<*>>>(result)
        assertEquals(2, result.data.size)
    }

    @Test
    fun fetchAccountsWithServerErrorReturnsApiError() = runTest {
        try { stopKoin() } catch (_: IllegalStateException) {}
        startKoin {
            modules(integrationTestModule(
                mockEngine = MockEngineFactory.create(
                    MockEngineFactory.errorHandlers(message = "Database unavailable")
                )
            ))
        }
        val repo: AccountRepository by inject()
        val result = repo.fetchAccounts()
        assertIs<ApiResult.Error>(result)
        assertEquals(500, result.code)
    }
}
```

## FullStackAccountTest

Verifies: MockEngine --> HttpClient --> API Service --> Repository --> ViewModel.

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class FullStackAccountTest : KoinTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val viewModel: AccountViewModel by inject()

    @BeforeTest fun setUp() {
        Dispatchers.setMain(testDispatcher)
        startKoin { modules(integrationTestModule()) }
    }

    @AfterTest fun tearDown() {
        try { stopKoin() } catch (_: IllegalStateException) {}
        Dispatchers.resetMain()
    }

    @Test
    fun loadAccountsFlowsThroughEntireStack() = runTest {
        viewModel.loadAccounts()
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
        assertEquals(2, state.accounts.size)
        assertNull(state.error)
    }

    @Test
    fun errorPropagatesThroughEntireStack() = runTest {
        try { stopKoin() } catch (_: IllegalStateException) {}
        startKoin {
            modules(integrationTestModule(
                mockEngine = MockEngineFactory.create(
                    MockEngineFactory.errorHandlers(message = "Service down")
                )
            ))
        }
        val errorVm: AccountViewModel by inject()
        errorVm.loadAccounts()
        val state = errorVm.uiState.first()
        assertEquals("Service down", state.error)
    }
}
```

---

## Android Integration Tests (Room + Robolectric)

File: `composeApp/src/androidUnitTest/kotlin/{your/package}/integration/RoomIntegrationTest.kt`

Exercises: MockEngine --> Repository --> Room Database.

```kotlin
@RunWith(RobolectricTestRunner::class)
class RoomIntegrationTest : KoinTest {

    private lateinit var database: AppDatabase
    private lateinit var accountDao: AccountDao

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        accountDao = database.accountDao()
        startKoin {
            androidContext(context)
            modules(integrationTestModule(), module {
                single<AppDatabase> { database }
                single<AccountDao> { accountDao }
            })
        }
    }

    @After fun tearDown() {
        database.close()
        try { stopKoin() } catch (_: IllegalStateException) {}
    }

    @Test fun fetchAndPersistAccounts() = runTest {
        val repository: AccountRepository by inject()
        assertIs<ApiResult.Success<*>>(repository.fetchAccounts())
        assertEquals(2, accountDao.observeAll().first().size)
    }
}
```

---

## CI Pipeline

Split test jobs for fast feedback:

1. `jvmTest --tests "!*integration*"` -- common unit tests (fastest)
2. `testDebugUnitTest --tests "!*integration*"` -- Android unit tests
3. `iosSimulatorArm64Test` -- iOS unit tests
4. `jvmTest --tests "*integration*"` -- common integration tests
5. `testDebugUnitTest --tests "*integration*"` -- Android integration tests

Integration tests use Koin global state -- use `maxParallelForks = 1` or
`koinApplication {}` for test-local scope.

---

## Troubleshooting

### Koin InstanceCreationException

Missing dependency in the test Koin module. Use `checkModules()` to validate
the graph. Check the error stacktrace for the missing binding.

### MockEngine returns 404 for a valid endpoint

URL path mismatch -- MockEngine matches exact path+query strings. Print the
actual request URL for debugging.

### stopKoin() throws "KoinApplication has not been started"

Test failed before `startKoin()` completed. Always wrap in try-catch:
`try { stopKoin() } catch (_: IllegalStateException) {}`

### Room "database is locked"

Database not closed between tests. Always close in teardown. Do not share
database instances between test classes.

### Tests pass locally but fail in CI

Environment differences (timezone, locale, CPU speed). Use `TimeZone.UTC`
explicitly, `containsAll` for unordered results, and increase timeouts.

---

## Checklist

- [ ] Real Koin DI container used (not manual construction)
- [ ] `MockEngine` used for network (not real HTTP calls)
- [ ] `Room.inMemoryDatabaseBuilder` used (not file-backed)
- [ ] `@AfterTest` calls `stopKoin()`, closes databases, resets `Dispatchers.Main`
- [ ] `checkModules()` validates DI graph in at least one test
- [ ] Happy path and error path both covered per feature
- [ ] Tests verify full stack (MockEngine through ViewModel)
- [ ] CI separates unit from integration tests
