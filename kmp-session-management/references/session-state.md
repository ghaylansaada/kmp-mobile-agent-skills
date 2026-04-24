# SessionState Sealed Interface

## File Location

`commonMain/kotlin/{your.package}/core/session/SessionState.kt`

## Definition

```kotlin
package {your.package}.core.session

sealed interface SessionState {
    data object Unauthenticated : SessionState
    data object Authenticated : SessionState
    data object Expired : SessionState
    data object AwaitingSecondFactor : SessionState
}
```

## State Transitions

```
App Launch
  --> Load tokens from SecureTokenStorage
  --> Tokens exist?
      YES --> Authenticated
      NO  --> Unauthenticated

Login Success
  --> setTokens(access, refresh)
  --> Authenticated

Login Requires 2FA
  --> AwaitingSecondFactor
  --> User completes 2FA
  --> setTokens(access, refresh)
  --> Authenticated

Token Refresh Fails (401 from refresh endpoint)
  --> revokeSession()
  --> Expired (not Unauthenticated -- signals "was logged in, session ended")

User Taps Logout
  --> revokeSession()
  --> Unauthenticated
```

## Why Expired Is Distinct

`Expired` signals the UI to show a "session expired" message and redirect to login.
`Unauthenticated` means the user has never logged in or explicitly logged out.
Navigation guards use this distinction to show different screens:

- `Unauthenticated` --> onboarding / login screen
- `Expired` --> login screen with "Your session has expired" banner
- `Authenticated` --> main app
- `AwaitingSecondFactor` --> 2FA verification screen

## Observation

Observe `SessionManager.sessionState` (a `StateFlow<SessionState>`) in a top-level
Composable via `collectAsState()`:

```kotlin
val state by sessionManager.sessionState.collectAsState()

when (state) {
    SessionState.Unauthenticated -> LoginScreen()
    SessionState.Expired -> LoginScreen(showExpiredBanner = true)
    SessionState.Authenticated -> MainScreen()
    SessionState.AwaitingSecondFactor -> TwoFactorScreen()
}
```
