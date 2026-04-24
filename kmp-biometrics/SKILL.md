---
name: kmp-biometrics
description: >
  Use this skill when adding biometric authentication to a KMP app using Android
  BiometricPrompt and iOS LocalAuthentication. Activate when the user asks to
  "add fingerprint login," "require Face ID," "gate a feature behind biometrics,"
  "secure credentials with biometrics," or "lock the app." Covers availability
  checks, authentication flows, device credential fallback, Keystore/Keychain
  credential storage, and feature gating patterns. Does NOT cover general
  encryption or cryptographic operations (see kmp-crypto), session token
  management without biometrics (see kmp-session-management), or permissions
  dialogs (see kmp-permissions).
compatibility: >
  KMP with Compose Multiplatform. Requires AndroidX Biometric and iOS LocalAuthentication.
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---

# KMP Biometrics

## When to use

- Adding fingerprint or face authentication to login or sensitive features
- Gating a screen or action behind biometric verification
- Storing credentials in platform keystore/keychain with biometric access control
- Implementing app-lock / re-authentication after background timeout
- Checking biometric availability and enrollment status before showing auth UI
- Protecting sensitive screens with biometric authentication
- Implementing fallback from biometric to PIN/password
- Handling biometric enrollment changes (new fingerprint invalidates keys)

## Depends on

- **kmp-project-setup** -- source set layout and Gradle configuration
- **kmp-architecture** -- expect/actual pattern for BiometricAuthenticator and SecureCredentialStore
- **kmp-dependency-injection** -- Koin module wiring

## Workflow

1. **Add dependencies and platform config** --> read `references/setup.md`
   _Skip if BiometricPrompt dependency and Info.plist NSFaceIDUsageDescription are already present._
2. **Define shared types** (BiometricResult, expect BiometricAuthenticator, expect SecureCredentialStore) --> read `references/shared-types.md`
3. **Implement Android actual** (BiometricPrompt + Keystore) --> read `references/android-implementation.md`
   _Skip if only working on iOS._
4. **Implement iOS actual** (LAContext + Keychain) --> read `references/ios-implementation.md`
   _Skip if only working on Android._
5. **Wire DI and implement feature gating** --> read `references/integration.md`
6. **Scaffold a gated feature** --> use template at `assets/templates/biometric-gate.kt.template`
   _Load only when creating an entirely new biometric-gated feature._

## Gotchas

1. **`BiometricPrompt` requires `FragmentActivity`, not `ComponentActivity`.** Casting a `ComponentActivity` to `FragmentActivity` throws `ClassCastException` at runtime. Use `AppCompatActivity` (which extends `FragmentActivity`) as your base Activity class.
2. **iOS Face ID requires `NSFaceIDUsageDescription` in Info.plist.** Without it, `LAContext.evaluatePolicy()` crashes immediately with no error or fallback. Touch ID does not require a plist entry.
3. **Samsung devices may return `BIOMETRIC_SUCCESS` from `canAuthenticate()` with no biometrics enrolled.** Always handle `ERROR_NO_BIOMETRICS` in the authentication callback rather than relying solely on the availability check.
4. **iOS `LAContext` is single-use after evaluation.** Calling `evaluatePolicy()` a second time on the same `LAContext` instance returns stale results. Create a new `LAContext()` for every authentication attempt.
5. **`onAuthenticationFailed()` fires on each failed attempt but is NOT terminal.** The BiometricPrompt stays visible. Only act on `onAuthenticationSucceeded()` or `onAuthenticationError()`. Resuming a coroutine continuation in `onAuthenticationFailed()` cancels the prompt prematurely.
6. **On Android API < 30 with `allowDeviceCredential = true`, do NOT call `setNegativeButtonText()`.** They are mutually exclusive -- throws `IllegalArgumentException` at runtime.
7. **Android Keystore keys are invalidated when the user enrolls a new biometric.** `KeyPermanentlyInvalidatedException` is thrown on decryption. Catch it, delete the stale key, and prompt the user to re-authenticate and re-store credentials.
8. **Catching generic `Exception` in crypto operations swallows `CancellationException`.** Structured concurrency breaks silently. Always rethrow `CancellationException` or use specific exception types. See **kmp-kotlin-coroutines** skill for cancellation handling patterns.
9. **Android BiometricPrompt has a 30-second inactivity timeout.** `ERROR_TIMEOUT` fires -- handle gracefully and allow retry rather than treating it as a terminal failure.

## Assets

| Path | Load when... |
|------|-------------|
| [references/setup.md](references/setup.md) | Adding biometric dependencies or configuring Info.plist |
| [references/shared-types.md](references/shared-types.md) | Defining BiometricResult, BiometricAuthenticator, SecureCredentialStore |
| [references/android-implementation.md](references/android-implementation.md) | Implementing Android BiometricPrompt + Keystore |
| [references/ios-implementation.md](references/ios-implementation.md) | Implementing iOS LAContext + Keychain |
| [references/integration.md](references/integration.md) | Wiring DI, feature gating, session management |
| [assets/templates/biometric-gate.kt.template](assets/templates/biometric-gate.kt.template) | Scaffolding a new biometric-gated feature |

## Validation

### A. Kotlin and KMP correctness
- [ ] `BiometricResult` is a `sealed interface`, not `sealed class`
- [ ] `BiometricResult` subtypes use `data object` (not `data class`) where they carry no data
- [ ] Android actual casts to `FragmentActivity`, not `ComponentActivity`
- [ ] `onAuthenticationFailed()` does NOT resume the continuation
- [ ] `suspendCancellableCoroutine` checks `continuation.isActive` before resuming
- [ ] `invokeOnCancellation` cancels the BiometricPrompt on coroutine cancellation
- [ ] iOS actual creates a new `LAContext()` per `authenticate()` call
- [ ] `CancellationException` is not swallowed by generic `catch (e: Exception)` blocks

### B. Security
- [ ] Android credentials encrypted with AES-256-GCM via AndroidKeyStore
- [ ] iOS credentials stored in Keychain (kSecClassGenericPassword)
- [ ] No plaintext secrets in SharedPreferences or UserDefaults
- [ ] `KeyPermanentlyInvalidatedException` caught and handled in Android credential retrieval
- [ ] Keychain service name is app-specific (no hardcoded generic names)

### C. Integration
- [ ] Koin module registers BiometricAuthenticator and SecureCredentialStore as singletons
- [ ] ViewModel uses `viewModelScope.launch` for authentication calls
- [ ] `MutableStateFlow` fields are private with public `StateFlow` via `asStateFlow()`
