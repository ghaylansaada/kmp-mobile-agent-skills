# iOS-Specific Tests

Covers Kotlin/Native iosTest source set tests, Swift XCTests for shared module
APIs, Flow collection from Swift, async/await bridge testing, and XCUITest UI
automation.

## Kotlin/Native Tests (iosTest source set)

Tests in `iosTest` exercise iOS-specific `actual` implementations. Run via:
`./gradlew :composeApp:iosSimulatorArm64Test`

### IosSessionStorageTest

File: `composeApp/src/iosTest/kotlin/{your/package}/platform/IosSessionStorageTest.kt`

```kotlin
package {your.package}.platform

import {your.package}.builders.buildSession
import {your.package}.data.local.IosSessionStorage
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IosSessionStorageTest {

    private val storage = IosSessionStorage()

    @AfterTest fun tearDown() { storage.clear() }

    @Test
    fun storeAndRetrieveSession() = runTest {
        storage.save(buildSession(token = "ios-token-123"))
        assertEquals("ios-token-123", storage.load()?.token)
    }

    @Test
    fun clearRemovesSession() = runTest {
        storage.save(buildSession())
        storage.clear()
        assertNull(storage.load())
    }

    @Test
    fun overwritesPreviousSession() = runTest {
        storage.save(buildSession(token = "first"))
        storage.save(buildSession(token = "second"))
        assertEquals("second", storage.load()?.token)
    }
}
```

### IosExternalStorageTest

File: `composeApp/src/iosTest/kotlin/{your/package}/platform/IosExternalStorageTest.kt`

```kotlin
package {your.package}.platform

import {your.package}.data.local.IosExternalStorageFactory
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IosExternalStorageTest {

    private val factory = IosExternalStorageFactory()

    @Test
    fun documentsDirectoryIsNotNull() { assertNotNull(factory.getDocumentsDirectory()) }

    @Test
    fun cacheDirectoryContainsCachePath() {
        val dir = factory.getCacheDirectory()
        assertNotNull(dir)
        assertTrue(dir.contains("Cache") || dir.contains("cache"))
    }

    @Test
    fun databasePathEndsWithDbExtension() {
        val path = factory.getDatabasePath("app.db")
        assertNotNull(path)
        assertTrue(path.endsWith("app.db"))
    }
}
```

---

## Swift XCTests

Swift unit tests that import the shared KMP framework. Prerequisites:
`./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`

### SharedModuleTests

File: `iosApp/iosAppTests/SharedModuleTests.swift`

```swift
import XCTest
@testable import ComposeApp

final class SharedModuleTests: XCTestCase {

    func testApiResultSuccessHoldsData() {
        let result = ApiResultSuccess(data: "hello" as NSString)
        XCTAssertEqual(result.data as? String, "hello")
    }

    func testApiResultErrorHoldsCodeAndMessage() {
        let result = ApiResultError(code: 404, message: "Not found")
        XCTAssertEqual(result.code, 404)
        XCTAssertEqual(result.message, "Not found")
    }

    func testSessionManagerInitiallyHasNoSession() {
        XCTAssertNil(SessionManager().currentSessionValue)
    }
}
```

### FlowCollectionTests

File: `iosApp/iosAppTests/FlowCollectionTests.swift`

Attach the collector BEFORE triggering the emission.

```swift
import XCTest
@testable import ComposeApp

final class FlowCollectionTests: XCTestCase {

    func testCollectFlowEmitsValues() {
        let expectation = XCTestExpectation(description: "Flow emits values")
        let viewModel = AccountViewModel(repository: FakeAccountRepositoryForSwift())
        var collected: [Account] = []

        let wrapper = FlowWrapper<NSArray>(flow: viewModel.accountsFlow)
        wrapper.collect(
            onEach: { accounts in
                if let list = accounts as? [Account], !list.isEmpty {
                    collected = list
                    expectation.fulfill()
                }
            },
            onComplete: {},
            onError: { error in XCTFail("Flow error: \(error)") }
        )

        viewModel.loadAccounts()
        wait(for: [expectation], timeout: 5.0)
        XCTAssertFalse(collected.isEmpty)
        wrapper.cancel()
    }
}
```

### AsyncBridgeTests

File: `iosApp/iosAppTests/AsyncBridgeTests.swift`

```swift
import XCTest
@testable import ComposeApp

final class AsyncBridgeTests: XCTestCase {

    func testFetchAccountsAsyncBridge() async throws {
        let accounts = try await FakeAccountRepositoryForSwift().fetchAccounts()
        XCTAssertEqual(accounts.count, 1)
    }

    func testAsyncBridgeThrowsOnError() async {
        do {
            _ = try await FailingRepository().fetchAccounts()
            XCTFail("Expected error to be thrown")
        } catch {
            XCTAssertNotNil(error)
        }
    }
}
```

### AccountViewModelSwiftTests

File: `iosApp/iosAppTests/AccountViewModelSwiftTests.swift`

Use `XCTestExpectation` with `DispatchQueue.main.asyncAfter` to yield the main
thread and let Kotlin ViewModel emissions arrive.

```swift
import XCTest
@testable import ComposeApp

final class AccountViewModelSwiftTests: XCTestCase {

    private var viewModel: AccountViewModel!

    override func setUp() {
        super.setUp()
        viewModel = AccountViewModel(repository: FakeAccountRepositoryForSwift())
    }
    override func tearDown() { viewModel = nil; super.tearDown() }

    func testViewModelInitialStateIsLoading() {
        XCTAssertTrue(viewModel.uiStateValue.isLoading)
    }

    func testLoadAccountsUpdatesState() {
        let exp = XCTestExpectation(description: "State updates")
        viewModel.loadAccounts()
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            XCTAssertFalse(self.viewModel.uiStateValue.isLoading)
            exp.fulfill()
        }
        wait(for: [exp], timeout: 3.0)
    }
}
```

---

## XCUITests

XCUITests run the full app -- they cannot mock dependencies. Use launch
arguments (e.g., `--uitesting`) to configure the app for test mode.

### AccountScreenUITests

File: `iosApp/iosAppUITests/AccountScreenUITests.swift`

```swift
import XCTest

final class AccountScreenUITests: XCTestCase {

    private var app: XCUIApplication!

    override func setUp() {
        super.setUp()
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments.append("--uitesting")
        app.launch()
    }
    override func tearDown() { app = nil; super.tearDown() }

    func testAccountListIsVisible() {
        XCTAssertTrue(app.scrollViews["account_list"].waitForExistence(timeout: 10))
    }

    func testErrorStateShowsRetryButton() {
        app.terminate()
        app.launchArguments.append("--simulate-network-error")
        app.launch()
        XCTAssertTrue(app.staticTexts["error_message"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.buttons["retry_button"].exists)
    }
}
```

### LoginFlowUITests

File: `iosApp/iosAppUITests/LoginFlowUITests.swift`

```swift
import XCTest

final class LoginFlowUITests: XCTestCase {

    private var app: XCUIApplication!

    override func setUp() {
        super.setUp()
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments.append("--uitesting")
        app.launch()
    }
    override func tearDown() { app = nil; super.tearDown() }

    func testSuccessfulLoginFlow() {
        let username = app.textFields["username_field"]
        guard username.waitForExistence(timeout: 10) else { return XCTFail("Not found") }
        username.tap(); username.typeText("testuser")
        app.secureTextFields["password_field"].tap()
        app.secureTextFields["password_field"].typeText("password123")
        app.buttons["login_button"].tap()
        XCTAssertTrue(app.buttons["logout_button"].waitForExistence(timeout: 10))
    }

    func testLogoutFlow() {
        testSuccessfulLoginFlow()
        app.buttons["logout_button"].tap()
        XCTAssertTrue(app.textFields["username_field"].waitForExistence(timeout: 10))
    }
}
```

### Accessibility Identifiers

Compose Multiplatform: `Modifier.testTag("my_button")`
SwiftUI: `.accessibilityIdentifier("my_button")`
Verify in Accessibility Inspector (Xcode > Developer Tools).

---

## CI Configuration

iOS tests require macOS runners. Split jobs to isolate failures:

1. `./gradlew :composeApp:iosSimulatorArm64Test` -- Kotlin/Native tests
2. `xcodebuild test -only-testing:iosAppTests` -- Swift unit tests
3. `xcodebuild test -only-testing:iosAppUITests` -- UI tests (slowest)

Pre-boot simulator for faster execution: `xcrun simctl boot "iPhone 16"`

---

## Troubleshooting

### "No such module 'ComposeApp'"

Run `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64` and verify
Framework Search Paths in Xcode Build Settings.

### Flow collection test times out

Emission happened before collector was attached. Attach collector BEFORE
triggering the action. Increase timeout for slow CI simulators.

### Kotlin suspend function throws unexpected error in Swift

Add `@Throws(Exception::class)` on Kotlin suspend functions. Catch as `NSError`.

### Kotlin/Native object singleton state leaks between tests

Kotlin/Native `object` singletons do NOT persist state across test methods
(opposite of JVM). Use instance variables in `@BeforeTest`.

### XCUITest elements not found by accessibility identifier

Use `Modifier.testTag("my_button")` in Compose, `.accessibilityIdentifier("my_button")`
in SwiftUI. Verify in Accessibility Inspector.

---

## Checklist

- [ ] Kotlin/Native tests run on iOS simulator
- [ ] No backtick test names in iosTest or commonTest
- [ ] FlowWrapper collector attached before triggering emission
- [ ] All FlowWrappers cancelled in tearDown
- [ ] `@Throws(Exception::class)` on suspend functions called from Swift
- [ ] Async bridge tests cover both success and error paths
- [ ] XCUITests use `waitForExistence(timeout:)` instead of `Thread.sleep()`
- [ ] Test data builders from commonTest importable in iosTest
