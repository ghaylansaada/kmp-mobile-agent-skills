# Auth-Gated Navigation

## Overview

In Navigation 3, auth gating is handled by observing session state and managing the back stack directly. Unlike Nav2 where you keyed a NavHost on the start destination, Nav3 gives you explicit control: clear the back stack and push the appropriate starting key when auth state changes.

## Auth-Gated AppNavDisplay

The root NavDisplay observes `SessionState` and resets the back stack on auth transitions:

```kotlin
package {your.package}.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.NavDisplay
import androidx.navigation3.rememberNavBackStack
import androidx.navigation3.entryProvider
import androidx.savedstate.rememberSavedStateRegistryNavEntryDecorator
import {your.package}.core.session.SessionManager
import {your.package}.core.session.SessionState
import {your.package}.navigation.routes.*
import org.koin.compose.koinInject

@Composable
fun AppNavDisplay(
    modifier: Modifier = Modifier,
    sessionManager: SessionManager = koinInject(),
) {
    val sessionState by sessionManager.sessionState.collectAsState(
        initial = SessionState.Unauthenticated,
    )
    val backStack = rememberNavBackStack(LoginKey)

    LaunchedEffect(sessionState) {
        when (sessionState) {
            SessionState.Authenticated -> {
                backStack.clear()
                backStack.add(HomeKey)
            }
            SessionState.Unauthenticated,
            SessionState.AwaitingSecondFactor,
            -> {
                backStack.clear()
                backStack.add(LoginKey)
            }
        }
    }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        entryDecorators = listOf(
            rememberSavedStateRegistryNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            // Auth entries
            entry<LoginKey> { key ->
                LoginScreen(
                    onLoginSuccess = {
                        // SessionManager.setTokens() triggers SessionState.Authenticated
                        // LaunchedEffect above handles the navigation
                    },
                    onNavigateToRegister = { backStack.add(RegisterKey) },
                    onNavigateToForgotPassword = { backStack.add(ForgotPasswordKey) },
                )
            }

            entry<RegisterKey> { key ->
                RegisterScreen(
                    onRegistrationSuccess = {
                        // SessionManager.setTokens() triggers state change
                    },
                    onNavigateBack = { backStack.removeLastOrNull() },
                )
            }

            entry<ForgotPasswordKey> { key ->
                ForgotPasswordScreen(
                    onNavigateBack = { backStack.removeLastOrNull() },
                )
            }

            // Main entries (only reachable when authenticated)
            entry<HomeKey> { key ->
                MainScaffold(
                    onLogout = {
                        // SessionManager.clearTokens() triggers SessionState.Unauthenticated
                        // LaunchedEffect above handles the navigation
                    },
                )
            }
        },
    )
}
```

## Entry Wrapper Pattern

For entries that require auth, wrap them with an auth check that redirects if the session expires mid-use:

```kotlin
@Composable
fun AuthRequiredEntry(
    sessionManager: SessionManager = koinInject(),
    onSessionExpired: () -> Unit,
    content: @Composable () -> Unit,
) {
    val sessionState by sessionManager.sessionState.collectAsState(
        initial = SessionState.Unauthenticated,
    )

    LaunchedEffect(sessionState) {
        if (sessionState != SessionState.Authenticated) {
            onSessionExpired()
        }
    }

    if (sessionState == SessionState.Authenticated) {
        content()
    }
}

// Usage in entry factory:
entry<ProfileKey> { key ->
    AuthRequiredEntry(
        onSessionExpired = {
            backStack.clear()
            backStack.add(LoginKey)
        },
    ) {
        ProfileScreen(
            onNavigateToSettings = { backStack.add(SettingsKey) },
        )
    }
}
```

## Composable-Level Auth Gate

An alternative pattern that switches between authenticated and unauthenticated content trees without using navigation at all:

```kotlin
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

// Usage in App.kt:
@Composable
fun App() {
    AppTheme {
        AuthGate(
            unauthenticatedContent = { AuthNavDisplay() },
            authenticatedContent = { MainNavDisplay() },
        )
    }
}
```

This pattern uses separate NavDisplay instances for auth and main flows, which cleanly isolates their back stacks.

## Connected Skills

- **kmp-session-management** -- SessionManager/SessionState drives auth gating
- **kmp-dependency-injection** -- Koin provides SessionManager and ViewModels
