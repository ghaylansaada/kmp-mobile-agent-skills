package {your.package}.navigation.snippets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.navigation3.NavKey
import kotlinx.serialization.Serializable
import {your.package}.core.session.SessionManager
import {your.package}.core.session.SessionState
import org.koin.compose.koinInject

// --- NavKey with required argument + entry wiring ---

@Serializable
data class UserProfileKey(val userId: String) : NavKey

// In entry factory:
// entry<UserProfileKey> { key ->
//     UserProfileContent(userId = key.userId, onBack = { backStack.removeLastOrNull() })
// }

// --- NavKey with multiple arguments (required + optional with defaults) ---

@Serializable
data class SearchResultsKey(
    val query: String,
    val category: String? = null,
    val page: Int = 1,
) : NavKey

// backStack.add(SearchResultsKey(query = "kotlin", category = "tutorials", page = 2))
// backStack.add(SearchResultsKey(query = "kotlin"))  // defaults applied

// --- Back stack utilities ---

fun <T : NavKey> SnapshotStateList<T>.popBackStack() {
    removeLastOrNull()
}

fun <T : NavKey> SnapshotStateList<T>.popBackStackSafe() {
    if (size > 1) removeLastOrNull()
}

fun <T : NavKey> SnapshotStateList<T>.navigateAndClear(key: T) {
    clear()
    add(key)
}

fun <T : NavKey> SnapshotStateList<T>.replaceCurrent(key: T) {
    removeLastOrNull()
    add(key)
}

inline fun <T : NavKey, reified Target : T> SnapshotStateList<T>.popBackStackTo(
    inclusive: Boolean = false,
) {
    val index = indexOfLast { it is Target }
    if (index >= 0) {
        val removeFrom = if (inclusive) index else index + 1
        while (size > removeFrom) {
            removeLastOrNull()
        }
    }
}

// --- Conditional navigation based on state ---

@Serializable
data object OnboardingKey : NavKey

@Serializable
data object DashboardKey : NavKey

fun <T : NavKey> SnapshotStateList<T>.navigateAfterSplash(
    isOnboardingComplete: Boolean,
    onboardingKey: T,
    dashboardKey: T,
) {
    clear()
    add(if (isOnboardingComplete) dashboardKey else onboardingKey)
}

// --- AuthGate composable (composable-level auth switching) ---

@Composable
fun AuthGate(
    sessionManager: SessionManager = koinInject(),
    unauthenticatedContent: @Composable () -> Unit,
    authenticatedContent: @Composable () -> Unit,
) {
    val sessionState by sessionManager.sessionState.collectAsState(
        initial = SessionState.Unauthenticated,
    )
    when (sessionState) {
        SessionState.Authenticated -> authenticatedContent()
        else -> unauthenticatedContent()
    }
}

// --- Placeholder for snippet compilation ---

@Composable
private fun UserProfileContent(
    userId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.Text(stringResource(Res.string.label_profile, userId))
}
