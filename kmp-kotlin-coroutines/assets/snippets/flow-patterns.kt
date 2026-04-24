package {your.package}.examples

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

// For Channel-based one-shot events, see references/concurrency-patterns.md

// --- Flow operators: map, filter, combine, catch ---

class FlowOperators {
    fun userNames(usersFlow: Flow<List<User>>): Flow<List<String>> =
        usersFlow.map { users -> users.map { it.name } }

    fun activeUsers(usersFlow: Flow<List<User>>): Flow<List<User>> =
        usersFlow.map { users -> users.filter { it.isActive } }

    fun uniqueStates(stateFlow: Flow<UiState>): Flow<UiState> =
        stateFlow.distinctUntilChanged()

    fun combinedState(
        userFlow: Flow<User?>,
        settingsFlow: Flow<Settings>,
    ): Flow<DashboardState> = combine(userFlow, settingsFlow) { user, settings ->
        DashboardState(
            userName = user?.name ?: "Guest",
            theme = settings.theme,
            isLoggedIn = user != null,
        )
    }

    fun safeFlow(dataFlow: Flow<String>): Flow<String> =
        dataFlow.catch { cause -> emit("Error: ${cause.message}") }
}

// --- stateIn: Convert cold Flow to hot StateFlow ---

class StateInExample(
    private val repository: Repository,
    private val scope: CoroutineScope,
) {
    val accounts: StateFlow<List<String>> = repository.observeAccounts()
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )
}

// --- One-shot collection: first, firstOrNull ---

class OneShotCollection {
    suspend fun getFirstUser(usersFlow: Flow<User>): User = usersFlow.first()
    suspend fun findUser(usersFlow: Flow<User?>): User? = usersFlow.firstOrNull()
    suspend fun waitForAuth(sessionFlow: Flow<SessionState>): SessionState =
        sessionFlow.first { it is SessionState.Authenticated }
}

// --- Helper types ---

data class User(val name: String, val isActive: Boolean)
data class Settings(val theme: String)
data class UiState(val isLoading: Boolean = false, val data: String? = null)
data class DashboardState(
    val userName: String,
    val theme: String,
    val isLoggedIn: Boolean,
)

// SessionState: see kmp-session-management for the canonical sealed interface
sealed interface SessionState {
    data object Unauthenticated : SessionState
    data object Authenticated : SessionState
}

interface Repository {
    fun observeAccounts(): Flow<List<String>>
}

// --- Snippet 7: Reactive Search with debounce + mapLatest + stateIn ---

/**
 * Snippet 7 — Reactive Search with debounce + mapLatest + stateIn
 *
 * The gold standard for search implementation. debounce limits API calls
 * while the user types, mapLatest cancels stale requests when new input
 * arrives, and stateIn exposes a lifecycle-aware StateFlow.
 */
class SearchViewModel(
    private val searchUseCase: SearchUseCase,
) : ViewModel() {

    private val query = MutableStateFlow("")

    val searchResults: StateFlow<SearchState> = query
        .debounce(500.milliseconds)
        .distinctUntilChanged()
        .mapLatest { q ->
            if (q.isBlank()) return@mapLatest SearchState.Idle
            SearchState.Loading
            try {
                SearchState.Success(searchUseCase(q))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                SearchState.Error(e.message)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SearchState.Idle,
        )

    fun onQueryChanged(newQuery: String) {
        query.value = newQuery
    }
}

sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState
    data class Success(val results: List<SearchResult>) : SearchState
    data class Error(val message: String?) : SearchState
}

// --- Helper types for search ---

interface SearchUseCase {
    suspend operator fun invoke(query: String): List<SearchResult>
}

data class SearchResult(val title: String, val snippet: String)

// ViewModel and viewModelScope are provided by the lifecycle-viewmodel KMP artifact.
// See kmp-architecture for ViewModel setup.
abstract class ViewModel {
    val viewModelScope: CoroutineScope get() = TODO("Provided by lifecycle-viewmodel")
}
