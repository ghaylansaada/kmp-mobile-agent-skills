# Common Shared Tests

Covers fakes, test data builders, parameterized helpers, and tests for domain,
data, presentation, and network layers in the `commonTest` source set.

## FakeAccountRepository

File: `composeApp/src/commonTest/kotlin/{your/package}/fakes/FakeAccountRepository.kt`

```kotlin
package {your.package}.fakes

import {your.package}.data.repository.AccountRepository
import {your.package}.domain.model.Account
import {your.package}.network.ApiResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAccountRepository : AccountRepository {
    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    var fetchResult: ApiResult<List<Account>> = ApiResult.Success(emptyList())
    var getAccountResult: ApiResult<Account>? = null
    var loginResult: ApiResult<Account>? = null
    var logoutCalled = false

    fun emitAccounts(accounts: List<Account>) { _accounts.value = accounts }
    override fun observeAccounts(): Flow<List<Account>> = _accounts.asStateFlow()
    override suspend fun fetchAccounts(): ApiResult<List<Account>> = fetchResult
    override suspend fun getAccount(id: String): ApiResult<Account> {
        return getAccountResult ?: ApiResult.Error(code = 404, message = "Not configured in fake")
    }
    override suspend fun login(username: String, password: String): ApiResult<Account> {
        return loginResult ?: ApiResult.Error(code = 401, message = "Not configured in fake")
    }
    override suspend fun logout() { logoutCalled = true }
}
```

---

## FakeNetworkClient

File: `composeApp/src/commonTest/kotlin/{your/package}/fakes/FakeNetworkClient.kt`

```kotlin
package {your.package}.fakes

import {your.package}.network.ApiResult

class FakeNetworkClient {
    private val responses = mutableMapOf<String, ApiResult<*>>()

    fun <T> enqueue(endpoint: String, result: ApiResult<T>) { responses[endpoint] = result }

    @Suppress("UNCHECKED_CAST")
    suspend fun <T> execute(endpoint: String): ApiResult<T> {
        return responses[endpoint] as? ApiResult<T>
            ?: ApiResult.Error(code = 500, message = "No fake response for $endpoint")
    }
    fun reset() { responses.clear() }
}
```

---

## Test Data Builders

File: `composeApp/src/commonTest/kotlin/{your/package}/builders/TestDataBuilders.kt`

```kotlin
package {your.package}.builders

import {your.package}.domain.model.Account
import {your.package}.domain.model.Session

fun buildAccount(
    id: String = "test-id-1",
    username: String = "testuser",
    email: String = "test@example.com",
    displayName: String = "Test User",
    avatarUrl: String? = null,
    isActive: Boolean = true,
): Account = Account(id, username, email, displayName, avatarUrl, isActive)

fun buildAccountList(count: Int = 3): List<Account> =
    (1..count).map { i ->
        buildAccount(id = "test-id-$i", username = "user$i", email = "user$i@example.com", displayName = "User $i")
    }

fun buildSession(
    token: String = "fake-token-abc123",
    refreshToken: String = "fake-refresh-xyz789",
    expiresAt: Long = Long.MAX_VALUE,
    account: Account = buildAccount(),
): Session = Session(token, refreshToken, expiresAt, account)
```

---

## Parameterized Test Helper

File: `composeApp/src/commonTest/kotlin/{your/package}/helpers/ParameterizedTestHelper.kt`

`kotlin.test` has no built-in parameterized runner. This helper collects all
failures before reporting so you see every failing case in one run.

```kotlin
package {your.package}.helpers

import kotlin.test.fail

data class TestCase<I, E>(val name: String, val input: I, val expected: E)

fun <I, E> parameterizedTest(
    cases: List<TestCase<I, E>>,
    assertion: (input: I, expected: E) -> Unit,
) {
    val failures = mutableListOf<String>()
    for (case in cases) {
        try { assertion(case.input, case.expected) }
        catch (e: AssertionError) { failures.add("[${case.name}]: ${e.message}") }
    }
    if (failures.isNotEmpty()) {
        fail("${failures.size}/${cases.size} case(s) failed:\n${failures.joinToString("\n")}")
    }
}
```

---

## Dispatcher Setup Rule

`Dispatchers.setMain` MUST be called BEFORE creating the ViewModel. If the ViewModel
launches coroutines in `init`, those coroutines capture the real `Dispatchers.Main`
before the test swaps it, causing hangs or `UncompletedCoroutinesError`.

---

## AccountViewModelTest

File: `composeApp/src/commonTest/kotlin/{your/package}/presentation/AccountViewModelTest.kt`

```kotlin
package {your.package}.presentation

import {your.package}.builders.buildAccount
import {your.package}.builders.buildAccountList
import {your.package}.fakes.FakeAccountRepository
import {your.package}.network.ApiResult
import {your.package}.presentation.viewmodel.AccountViewModel
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeRepository = FakeAccountRepository()
    private lateinit var viewModel: AccountViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AccountViewModel(repository = fakeRepository, dispatcher = testDispatcher)
    }

    @AfterTest
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun initialStateIsLoading() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isLoading)
            assertTrue(state.accounts.isEmpty())
            assertNull(state.error)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun loadAccountsUpdatesStateWithData() = runTest {
        val accounts = buildAccountList(2)
        fakeRepository.fetchResult = ApiResult.Success(accounts)
        fakeRepository.emitAccounts(accounts)
        viewModel.loadAccounts()
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(2, state.accounts.size)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun loadAccountsSetsErrorOnFailure() = runTest {
        fakeRepository.fetchResult = ApiResult.Error(code = 500, message = "Internal server error")
        viewModel.loadAccounts()
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals("Internal server error", state.error)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun logoutDelegatesToRepository() = runTest {
        viewModel.logout()
        assertTrue(fakeRepository.logoutCalled)
    }
}
```

---

## SessionManagerTest

File: `composeApp/src/commonTest/kotlin/{your/package}/domain/SessionManagerTest.kt`

Tests SessionManager using builders and Turbine for Flow assertions. Covers
initial state, set, clear, expired, and account reflection.

---

## Network Tests

Files in `composeApp/src/commonTest/kotlin/{your/package}/network/`:

- **ApiCallTest**: Covers success, exception wrapping, and exception type preservation.
- **ApiResultTest**: Covers all variants, map, getOrNull, and parameterized error codes.
- **PagingStateMapperTest**: Covers middle page, first page, and last page boundary.

---

## Troubleshooting

### `runTest` hangs indefinitely

A coroutine uses `Dispatchers.IO` or `Dispatchers.Default` instead of the test
dispatcher. Fix: inject dispatchers via constructor so tests can substitute.

### `UncompletedCoroutinesError`

A coroutine launched but neither completed nor cancelled before `runTest` returned.
Call `advanceUntilIdle()` to let coroutines finish, or cancel the scope.

### Tests pass on JVM but fail on iOS

Common causes: `object` singletons with mutable state, `assertEquals` identity
issues on Native, backtick test names. Use camelCase names, compare by structural
properties, and use `AtomicReference` for mutable singleton state.

### Turbine "No value produced in 3s" timeout

The ViewModel dispatches on `Dispatchers.Main` and the test has not called
`Dispatchers.setMain(testDispatcher)`. Ensure `@BeforeTest` sets the main
dispatcher before creating the ViewModel.

---

## Checklist

- [ ] All tests use `kotlin.test.Test` and `runTest`
- [ ] No backtick function names (camelCase only for Kotlin/Native compatibility)
- [ ] `Dispatchers.setMain` called BEFORE ViewModel creation in `@BeforeTest`
- [ ] `Dispatchers.resetMain` called in `@AfterTest`
- [ ] Flow assertions use Turbine `test {}` blocks with `awaitItem()`
- [ ] Fakes compile and implement production interfaces
- [ ] TestDataBuilders produce valid domain objects with sensible defaults
- [ ] ParameterizedTestHelper reports all failures, not just the first
