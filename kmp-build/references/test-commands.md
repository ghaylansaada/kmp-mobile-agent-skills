# Test Commands

## Quick Reference

| Command | Purpose |
|---------|---------|
| `./gradlew :composeApp:allTests` | Run ALL tests across all targets |
| `./gradlew :composeApp:testDebugUnitTest` | Android unit tests (debug) |
| `./gradlew :composeApp:testReleaseUnitTest` | Android unit tests (release) |
| `./gradlew :composeApp:jvmTest` | JVM target tests |
| `./gradlew :composeApp:iosSimulatorArm64Test` | iOS simulator tests (Apple Silicon) |
| `./gradlew :composeApp:iosX64Test` | iOS simulator tests (Intel) |
| `./gradlew :composeApp:verifyRoborazziDebug` | Screenshot comparison tests |

## Run All Tests

The most comprehensive test command. Runs tests across all configured targets (Android, iOS, JVM).

```bash
./gradlew :composeApp:allTests
```

Test reports are generated at: `composeApp/build/reports/tests/`

## Android Unit Tests

```bash
# Debug variant
./gradlew :composeApp:testDebugUnitTest

# Release variant
./gradlew :composeApp:testReleaseUnitTest
```

Test reports: `composeApp/build/reports/tests/testDebugUnitTest/`

## JVM Tests

Runs tests in the `jvmTest` source set. Fastest target for shared logic tests.

```bash
./gradlew :composeApp:jvmTest
```

## iOS Tests

Requires a macOS machine with Xcode installed. The test binary runs on a simulated iOS device.

```bash
# Apple Silicon simulators
./gradlew :composeApp:iosSimulatorArm64Test

# Intel simulators
./gradlew :composeApp:iosX64Test
```

Note: iOS tests are significantly slower than JVM/Android tests due to Kotlin/Native compilation time. Run JVM tests first for fast feedback, then iOS tests for platform-specific verification.

## Screenshot Tests (Roborazzi)

Roborazzi captures and compares screenshots for UI regression testing.

```bash
# Record new reference screenshots
./gradlew :composeApp:recordRoborazziDebug

# Verify current screenshots against references
./gradlew :composeApp:verifyRoborazziDebug

# Compare and generate diff images
./gradlew :composeApp:compareRoborazziDebug
```

Reference screenshots are stored in the project and committed to version control. The `verify` task fails if any screenshot differs from its reference beyond the configured threshold.

## Filtering Tests

### Run a specific test class

```bash
./gradlew :composeApp:testDebugUnitTest --tests "com.example.app.UserViewModelTest"
```

### Run a specific test method

```bash
./gradlew :composeApp:testDebugUnitTest --tests "com.example.app.UserViewModelTest.fetchUserReturnsSuccess"
```

### Run tests matching a pattern

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*ViewModel*"
```

## Test Options

```bash
# Continue running tests even if some fail
./gradlew :composeApp:allTests --continue

# Re-run all tests (ignore up-to-date cache)
./gradlew :composeApp:allTests --rerun-tasks

# Show test output in console
./gradlew :composeApp:testDebugUnitTest -i

# Run with stacktrace on failure
./gradlew :composeApp:allTests --stacktrace
```

## CI Pipeline Test Commands

A typical CI pipeline runs tests in this order:

```bash
# 1. Fast feedback -- JVM and Android tests
./gradlew :composeApp:jvmTest :composeApp:testDebugUnitTest

# 2. iOS verification (macOS runners only)
./gradlew :composeApp:iosSimulatorArm64Test

# 3. Screenshot regression
./gradlew :composeApp:verifyRoborazziDebug

# 4. Full suite (alternative to running steps 1-3 separately)
./gradlew :composeApp:allTests :composeApp:verifyRoborazziDebug
```

## Common Test Failures

### Tests pass locally but fail on CI

Likely causes: timezone differences, locale-dependent formatting, or missing environment variables. Use `Clock.System` injection and avoid `Locale.getDefault()` in test assertions.

### iOS tests crash with signal 11

Usually an OOM in the Kotlin/Native test runner. Increase memory limits and ensure tests do not retain large data structures across test methods.

### Roborazzi diffs on CI

Rendering differences between macOS versions or JDK versions cause pixel-level mismatches. Pin the CI runner image version and JDK distribution to match the recording environment.
