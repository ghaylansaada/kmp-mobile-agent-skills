package {your.package}.core.session

import {your.package}.data.local.AppDatabase
import {your.package}.data.local.clearAllTables
import io.ktor.client.plugins.auth.providers.BearerTokens
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// --- SESSION STATE ---

sealed interface SessionState {
    data object Unauthenticated : SessionState
    data object Authenticated : SessionState
    data object Expired : SessionState
    data object AwaitingSecondFactor : SessionState
}

// --- VALUE CLASS WRAPPERS ---

@JvmInline
value class AccessToken(val value: String)

@JvmInline
value class RefreshToken(val value: String)

// --- SESSION MANAGER ---

class SessionManager(
    private val secureTokenStorage: SecureTokenStorage,
    private val database: AppDatabase,
) {

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Unauthenticated)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    fun loadSession() {
        val access = secureTokenStorage.getAccessToken()
        val refresh = secureTokenStorage.getRefreshToken()
        if (access != null && refresh != null) {
            _sessionState.value = SessionState.Authenticated
        }
    }

    fun getTokens(): BearerTokens? {
        val access = secureTokenStorage.getAccessToken() ?: return null
        val refresh = secureTokenStorage.getRefreshToken() ?: return null
        return BearerTokens(
            accessToken = access.value,
            refreshToken = refresh.value,
        )
    }

    fun setTokens(accessToken: AccessToken, refreshToken: RefreshToken) {
        secureTokenStorage.setTokens(accessToken, refreshToken)
        _sessionState.value = SessionState.Authenticated
    }

    suspend fun revokeSession(reason: SessionState = SessionState.Unauthenticated) {
        secureTokenStorage.clear()
        database.clearAllTables()
        _sessionState.value = reason
    }

    suspend fun expireSession() {
        revokeSession(reason = SessionState.Expired)
    }
}

// --- HTTP CLIENT INTEGRATION ---
// The HttpClientFactory that consumes SessionManager for Bearer auth lives in
// kmp-networking/references/http-client.md. It calls:
//   sessionManager.getTokens()      -- in loadTokens and refreshTokens double-check
//   sessionManager.setTokens()      -- after successful refresh
//   sessionManager.expireSession()   -- when refresh fails (401 from refresh endpoint)
// See that skill for the full Mutex double-check implementation.
