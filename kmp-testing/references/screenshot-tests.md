# Screenshot Tests (Roborazzi)

Golden-file screenshot testing with Roborazzi. Captures composable snapshots
and compares against committed baselines.

## Setup

```kotlin
// composeApp/build.gradle.kts
plugins {
    id("io.github.takahirom.roborazzi")
}
```

Golden files stored in: `composeApp/src/androidUnitTest/snapshots/`

---

## AccountScreenScreenshotTest

File: `composeApp/src/androidUnitTest/kotlin/{your/package}/screenshot/AccountScreenScreenshotTest.kt`

`@GraphicsMode(GraphicsMode.Mode.NATIVE)` and `@Config(qualifiers = "...")` are
required for deterministic pixel output. Without a fixed qualifier, Robolectric
picks a default screen configuration that varies across CI runners.

```kotlin
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import io.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class AccountScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun accountScreenLoadingState() {
        composeTestRule.setContent {
            AccountScreen(
                uiState = AccountUiState(isLoading = true),
                onLoginClick = { _, _ -> }, onLogoutClick = {}, onRefreshClick = {},
            )
        }
        composeTestRule.onRoot().captureRoboImage("AccountScreen_Loading.png")
    }

    @Test
    fun accountScreenWithData() {
        composeTestRule.setContent {
            AccountScreen(
                uiState = AccountUiState(isLoading = false, accounts = buildAccountList(3)),
                onLoginClick = { _, _ -> }, onLogoutClick = {}, onRefreshClick = {},
            )
        }
        composeTestRule.onRoot().captureRoboImage("AccountScreen_WithData.png")
    }

    @Test
    fun accountScreenErrorState() {
        composeTestRule.setContent {
            AccountScreen(
                uiState = AccountUiState(isLoading = false, error = "Connection timed out"),
                onLoginClick = { _, _ -> }, onLogoutClick = {}, onRefreshClick = {},
            )
        }
        composeTestRule.onRoot().captureRoboImage("AccountScreen_Error.png")
    }
}
```

---

## Commands

```bash
# Record baselines (first run or after intentional visual changes)
./gradlew :composeApp:recordRoborazziDebug

# Verify snapshots against committed baselines
./gradlew :composeApp:verifyRoborazziDebug

# Compare changes (generates diff images)
./gradlew :composeApp:compareRoborazziDebug
```

---

## CI Configuration

Screenshot tests should run in a separate CI job from unit tests:

```yaml
screenshot-tests:
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-java@v4
      with: { java-version: '21', distribution: 'temurin' }
    - uses: gradle/actions/setup-gradle@v4
    - run: ./gradlew :composeApp:verifyRoborazziDebug
    - uses: actions/upload-artifact@v4
      if: failure()
      with:
        name: screenshot-diffs
        path: composeApp/build/outputs/roborazzi/
```

---

## Troubleshooting

### Screenshot tests fail after dependency update

Rendering changes between Compose/Robolectric versions produce different pixel
output. Review diff images in `build/outputs/roborazzi/`, re-record with
`recordRoborazziDebug` if changes are expected, and commit updated golden files.

---

## Checklist

- [ ] `@GraphicsMode(GraphicsMode.Mode.NATIVE)` and `@Config(qualifiers = ...)` set
- [ ] Screenshot tests generate baseline images on first run
- [ ] Golden files committed to version control
- [ ] `verifyRoborazziDebug` passes against committed golden files
- [ ] Screenshot tests run in a separate CI job from unit tests
