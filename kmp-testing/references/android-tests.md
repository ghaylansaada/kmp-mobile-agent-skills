# Android-Specific Tests

Covers Compose UI testing with ComposeTestRule, Robolectric for Android API tests,
MockK for mocking, and Room DAO testing with in-memory databases.

## Compose UI Tests

File: `composeApp/src/androidUnitTest/kotlin/{your/package}/ui/AccountScreenTest.kt`

Use `createComposeRule()` (not `createAndroidComposeRule`) for headless Compose
testing. `@RunWith(RobolectricTestRunner::class)` is required.

```kotlin
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class AccountScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysLoadingIndicatorWhenLoading() {
        composeTestRule.setContent {
            AccountScreen(
                uiState = AccountUiState(isLoading = true),
                onLoginClick = {}, onLogoutClick = {}, onRefreshClick = {},
            )
        }
        composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
    }

    @Test
    fun displaysAccountListWhenDataLoaded() {
        val accounts = buildAccountList(3)
        composeTestRule.setContent {
            AccountScreen(
                uiState = AccountUiState(isLoading = false, accounts = accounts),
                onLoginClick = {}, onLogoutClick = {}, onRefreshClick = {},
            )
        }
        composeTestRule.onNodeWithText("User 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("User 2").assertIsDisplayed()
    }

    @Test
    fun loginButtonTriggersCallback() {
        var loginClicked = false
        composeTestRule.setContent {
            AccountScreen(
                uiState = AccountUiState(isLoading = false),
                onLoginClick = { _, _ -> loginClicked = true },
                onLogoutClick = {}, onRefreshClick = {},
            )
        }
        composeTestRule.onNodeWithTag("username_field").performTextInput("alice")
        composeTestRule.onNodeWithTag("password_field").performTextInput("secret")
        composeTestRule.onNodeWithTag("login_button").performClick()
        assertTrue(loginClicked)
    }
}
```

### Compose Test Helpers

File: `composeApp/src/androidUnitTest/kotlin/{your/package}/ui/ComposeTestHelpers.kt`

```kotlin
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag

fun ComposeTestRule.onAccountListItem(index: Int): SemanticsNodeInteraction =
    onAllNodesWithTag("account_item")[index]

fun ComposeTestRule.assertAccountItemDisplaysName(index: Int, name: String) {
    onAccountListItem(index).assertTextContains(name)
}

fun ComposeTestRule.assertEmptyState() { onNodeWithTag("empty_state").assertExists() }
fun ComposeTestRule.assertErrorState(message: String) {
    onNodeWithTag("error_message").assertTextContains(message)
}
```

---

## Robolectric Tests

Tests Android-specific APIs (SharedPreferences, Context, ExternalStorage).

File: `composeApp/src/androidUnitTest/kotlin/{your/package}/robolectric/SessionManagerAndroidTest.kt`

```kotlin
import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class SessionManagerAndroidTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        prefs = context.getSharedPreferences("test_session", Context.MODE_PRIVATE)
        sessionManager = SessionManager(prefs)
    }

    @After
    fun tearDown() { prefs.edit().clear().commit() }

    @Test
    fun sessionPersistsAcrossInstances() = runTest {
        sessionManager.setSession(buildSession(token = "persist-me"))
        val newManager = SessionManager(prefs)
        assertEquals("persist-me", newManager.getPersistedSession()?.token)
    }

    @Test
    fun clearSessionRemovesFromPreferences() = runTest {
        sessionManager.setSession(buildSession())
        sessionManager.clearSession()
        assertNull(prefs.getString("session_token", null))
    }
}
```

---

## MockK Tests

Prefer hand-written fakes (from commonTest) for repository and data-source
dependencies. Reserve MockK for Android framework types (`Context`,
`SharedPreferences`) that are impractical to fake.

File: `composeApp/src/androidUnitTest/kotlin/{your/package}/mock/AccountViewModelMockTest.kt`

```kotlin
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelMockTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockRepository = mockk<AccountRepository>(relaxed = true)
    private lateinit var viewModel: AccountViewModel

    @Before fun setUp() { Dispatchers.setMain(testDispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun loadAccountsCallsRepositoryFetch() = runTest {
        val accounts = buildAccountList(2)
        coEvery { mockRepository.fetchAccounts() } returns ApiResult.Success(accounts)
        every { mockRepository.observeAccounts() } returns flowOf(accounts)
        viewModel = AccountViewModel(repository = mockRepository, dispatcher = testDispatcher)
        viewModel.loadAccounts()
        coVerify(exactly = 1) { mockRepository.fetchAccounts() }
    }
}
```

---

## Room DAO Tests

Always call `database.close()` in `@After`. Leaving it open leaks the in-memory
SQLite connection.

File: `composeApp/src/androidUnitTest/kotlin/{your/package}/room/AccountDaoTest.kt`

```kotlin
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class AccountDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var accountDao: AccountDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        accountDao = database.accountDao()
    }

    @After fun tearDown() { database.close() }

    @Test
    fun insertAndRetrieveAccount() = runTest {
        val entity = AccountEntity(
            id = "1", username = "alice", email = "alice@example.com",
            displayName = "Alice", avatarUrl = null, isActive = true,
        )
        accountDao.insert(entity)
        assertEquals("alice", accountDao.getById("1")?.username)
    }

    @Test
    fun deleteRemovesAccount() = runTest {
        val entity = AccountEntity("1", "alice", "a@e.com", "Alice", null, true)
        accountDao.insert(entity)
        accountDao.delete(entity)
        assertNull(accountDao.getById("1"))
    }

    @Test
    fun deleteAllClearsTable() = runTest {
        accountDao.insert(AccountEntity("1", "a", "a@e.com", "A", null, true))
        accountDao.insert(AccountEntity("2", "b", "b@e.com", "B", null, true))
        accountDao.deleteAll()
        assertTrue(accountDao.observeAll().first().isEmpty())
    }
}
```

---

## Troubleshooting

### Robolectric "No such SDK" or download errors

Cache `~/.m2/repository/org/robolectric` in CI. Verify `robolectric.properties`
specifies a valid SDK.

### Compose test rule "No Activity" error

Test class is missing `@RunWith(RobolectricTestRunner::class)`. Also ensure
`isIncludeAndroidResources = true` in `build.gradle.kts`.

### MockK "Missing mocked calls" on suspend functions

Using `every { }` for suspend functions instead of `coEvery { }`. Use
`coEvery`/`coVerify` for all suspend functions.

### Room "Cannot access database on the main thread"

Add `allowMainThreadQueries()` to the in-memory database builder.

### `testDebugUnitTest` is slow (>2 minutes)

Use `@Config(sdk = [35])` consistently, enable parallel test execution with
`maxParallelForks`, and pre-download Robolectric jars in CI cache step.

---

## Checklist

- [ ] Tests use `createComposeRule()` + `@RunWith(RobolectricTestRunner::class)`
- [ ] `Dispatchers.setMain()` called before ViewModel creation
- [ ] `Dispatchers.resetMain()` called in `@After`
- [ ] `coEvery`/`coVerify` used for suspend functions (not `every`/`verify`)
- [ ] Room DAO tests use `allowMainThreadQueries()` and `database.close()` in `@After`
- [ ] SharedPreferences cleared in `@After` to avoid cross-test data leakage
- [ ] Fakes preferred for repository deps; MockK reserved for Android framework types
