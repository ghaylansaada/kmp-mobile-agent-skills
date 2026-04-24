---
name: kmp-testing
description: >
  Comprehensive testing for KMP projects -- commonTest with kotlin.test, Turbine, 
  and fakes; Android testing with Compose UI, Robolectric, MockK, Room DAOs, and
  Roborazzi screenshots; iOS testing with XCTest, Swift Flow collection, XCUITest,
  and SKIE async bridge; integration testing with MockEngine, Koin test modules,
  and full-stack flows. Activate when writing tests, setting up test infrastructure,
  creating fakes, or debugging test failures.
compatibility: >
  KMP with Compose Multiplatform. Requires kotlinx-coroutines-test and Turbine for common,
  MockK for Android, XCTest for iOS.
metadata:
  domain: kotlin-multiplatform
  targets: android, ios, jvm
---

# KMP Testing

## Scope

Covers the full testing stack for Kotlin Multiplatform projects: commonTest with
kotlin.test, kotlinx-coroutines-test, Turbine, fakes, and builders; Android testing
with Compose UI, Robolectric, MockK, Room DAOs, and Roborazzi screenshots; iOS
testing with XCTest, Swift Flow collection, XCUITest, and SKIE async bridges;
integration testing with MockEngine, Koin test modules, and full-stack flows from
HTTP through ViewModel.

## When to use

- Writing tests in commonTest, androidUnitTest, iosTest, or Swift test targets
- Creating fake repositories or network clients for shared test code
- Testing ViewModels, Repositories, or SessionManager from common or platform code
- Setting up kotlinx-coroutines-test with runTest and TestDispatcher
- Testing Flow emissions with Turbine or Swift FlowWrapper/SKIE
- Writing Compose UI tests with ComposeTestRule and semantic matchers
- Testing Room DAOs with in-memory databases under Robolectric
- Writing XCTests that consume shared KMP APIs from Swift
- Writing XCUITests for iOS UI automation with accessibility identifiers
- Setting up MockEngine with route-based JSON response fixtures
- Creating Koin test modules that override production dependencies
- Adding screenshot / golden-file testing with Roborazzi
- Debugging test failures: runTest hangs, coroutine leaks, platform-specific issues

## Depends on

- **kmp-project-setup** -- source set layout and Gradle configuration
- **kmp-architecture** -- ViewModel, Repository contracts to test against

## Workflow

1. Set up test dependencies and source sets --> [references/setup.md](references/setup.md)
2. Write common shared tests --> [references/common-tests.md](references/common-tests.md)
3. Write Android-specific tests --> [references/android-tests.md](references/android-tests.md)
   _Skip if not targeting Android-specific code._
4. Write iOS-specific tests --> [references/ios-tests.md](references/ios-tests.md)
   _Skip if not targeting iOS-specific code._
5. Write integration tests --> [references/integration-tests.md](references/integration-tests.md)
   _Skip if not testing full-stack flows._
6. Add screenshot tests --> [references/screenshot-tests.md](references/screenshot-tests.md)
   _Skip if not using Roborazzi._

## Gotchas

1. **`kotlin.test` uses different runners per platform.** JUnit 4 on Android, XCTest on iOS, JUnit on JVM. A test that imports `org.junit.Test` instead of `kotlin.test.Test` compiles on JVM but fails on iOS with an unresolved-reference error.
2. **`runTest` auto-advances virtual time for `delay()`, masking real timing bugs.** If the delay duration itself is under test (rate limiter, debounce), use `StandardTestDispatcher` with explicit `advanceTimeBy()`.
3. **MockK and Mockito do not work in commonTest.** They rely on JVM bytecode manipulation and throw `ClassNotFoundException` on iOS. Use hand-written fakes instead.
4. **`@Test` annotation resolution is ambiguous when both `kotlin.test.Test` and `org.junit.Test` are on the classpath.** Always use explicit `import kotlin.test.Test` in commonTest files.
5. **`assertEquals` uses structural equality on JVM but can surface identity-comparison issues on Kotlin/Native** when comparing objects that cross the interop boundary. Compare by structural properties if a test passes on JVM but fails on Native.
6. **`runTest` requires ALL launched coroutines to complete or be cancelled before the block returns.** A ViewModel that launches an infinite-collect coroutine in `init` causes `UncompletedCoroutinesError`. Inject a scope you control, or call `advanceUntilIdle()` followed by explicit cancellation.
7. **Backtick test names do not compile on Kotlin/Native.** `fun \`my test name\`()` works on JVM but produces a compilation error on iOS/Native targets. Use camelCase names in commonTest and iosTest.
8. **Turbine's `test {}` block has a default 3-second timeout.** Long-running Flow transformations silently hang until the timeout fires, producing a misleading "No value produced in 3s" error. Pass an explicit `timeout` parameter or ensure the test dispatcher controls virtual time.
9. **`Dispatchers.setMain` must be called BEFORE creating any ViewModel that launches coroutines in `init {}`.** Otherwise the ViewModel captures the real `Dispatchers.Main`, causing hangs or `UncompletedCoroutinesError` in tests.
10. **Version mismatch between `kotlinx-coroutines-core` and `kotlinx-coroutines-test` causes `NoClassDefFoundError`.** Both must share the same `version.ref` in the version catalog.
11. **`createComposeRule()` creates a headless compose environment (no Activity).** `createAndroidComposeRule<YourActivity>()` launches a real Activity. Default to `createComposeRule()` unless the test needs activity-level lifecycle. Missing `@RunWith(RobolectricTestRunner::class)` causes "No Activity found."
12. **Robolectric cannot fully test Room or DataStore** -- it shadows the Android framework but does not provide a real SQLite implementation. Room in-memory tests work under Robolectric because Room bundles its own SQLite. If you use `BundledSQLiteDriver` in production but not in tests, the DAO test silently uses a different SQLite version.
13. **`InstantTaskExecutorRule` is not needed with StateFlow.** StateFlow does not require the rule that LiveData needs. If migrating from LiveData-based tests, remove this rule -- it is unnecessary and adds test overhead.
14. **Hilt test rules (`HiltAndroidRule`) conflict with Koin.** This project uses Koin exclusively -- never add Hilt test dependencies.
15. **MockK's `mockk-android` artifact is required for Android-specific types like `Context`.** Using the plain `mockk` artifact compiles but throws `ClassCastException` at runtime when mocking Android framework classes.
16. **Framework must be built before Swift tests compile.** `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64` must run first. Without it, Xcode fails with "No such module 'ComposeApp'."
17. **Main thread deadlock with Kotlin suspend functions in XCTest.** XCTest runs test methods on the main thread. If a Kotlin suspend function does `withContext(Dispatchers.Main)` internally, it deadlocks. Use `XCTestExpectation` or ensure Kotlin code never re-dispatches to Main.
18. **FlowWrapper collector must attach before emission.** If you call `viewModel.loadAccounts()` before `wrapper.collect(onEach:)`, the emission fires into the void and the XCTestExpectation times out with no diagnostic output.
19. **FlowWrapper leaks if not cancelled in tearDown.** Forgetting `wrapper.cancel()` leaks the underlying CoroutineScope. On CI this causes memory warnings and eventual process termination.
20. **`@Throws(Exception::class)` is required on Kotlin suspend functions exposed to Swift.** Without it, Kotlin exceptions crash the process instead of being catchable as `NSError` in Swift.
21. **Compose Multiplatform accessibility identifiers use `Modifier.testTag()`.** XCUITests that query `app.buttons["my_id"]` will fail silently if the identifier was set with `Modifier.semantics {}` instead of `Modifier.testTag("my_id")`.
22. **Kotlin/Native object singleton state does not persist across test methods.** Each test gets fresh process state on Native, so an `object` with mutable state modified in one test will not carry that state to the next. This is the opposite of JVM behavior.
23. **MockEngine request order is non-deterministic.** Two concurrent coroutine requests arrive in dispatcher-scheduled order, not launch order. Tests asserting on request sequence will flake. Assert on the SET of requests via a `requestLog` list.
24. **MockEngine matches exact URL paths including query parameters.** If production calls `/api/accounts?page=1`, the handler key must be `"GET /api/accounts?page=1"` -- `"GET /api/accounts"` will not match and falls through to 404.
25. **`stopKoin()` throws if Koin never started.** Always wrap `stopKoin()` in try-catch in `@AfterTest`. Koin global state also means parallel test classes collide -- use `maxParallelForks = 1` or `koinApplication {}` for test-local scope.
26. **`checkModules()` misses runtime-only failures.** `checkModules()` validates the DI graph structurally but cannot detect runtime issues like a `SessionManager` that throws on construction. Pair `checkModules()` with at least one real integration test per Koin module.
27. **DataStore requires unique filenames per test.** Two tests writing to the same DataStore file race on file locks, producing `CorruptionException`. Use `"test_prefs_${testName}_${System.nanoTime()}.preferences_pb"`.
28. **Source-set boundaries break cross-platform integration tests.** A `commonTest` integration test that references `Room.inMemoryDatabaseBuilder` compiles on JVM but fails on iOS. Split: common integration in `commonTest`, Room integration in `androidUnitTest`.
29. **Room in-memory builder differs per platform.** On Android: `Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)`. On iOS (Room KMP): `Room.inMemoryDatabaseBuilder<AppDatabase>()` with `instantiateImpl()`. Wrong signature produces a misleading "missing overload" compile error.
30. **XCTest assertion parameter order differs from kotlin.test.** `XCTAssertEqual(a, b, "msg")` puts the message as the third parameter, while `assertEquals(expected, actual, message)` has different overloads. Confusing the order compiles fine but produces misleading failure output.

## Assets

| Path | Load when... |
|------|-------------|
| [assets/templates/common-test-class.kt.template](assets/templates/common-test-class.kt.template) | Creating a new test class with dispatcher setup and runTest |
| [assets/templates/fake-repository.kt.template](assets/templates/fake-repository.kt.template) | Creating a configurable fake implementing a repository interface |
| [assets/templates/compose-screen-test.kt.template](assets/templates/compose-screen-test.kt.template) | Creating a Robolectric + ComposeTestRule test class |
| [assets/templates/room-dao-test.kt.template](assets/templates/room-dao-test.kt.template) | Creating in-memory database DAO tests |
| [assets/templates/xctest-shared-module.swift.template](assets/templates/xctest-shared-module.swift.template) | Creating Swift XCTestCase consuming KMP shared code |
| [assets/templates/xcuitest-screen.swift.template](assets/templates/xcuitest-screen.swift.template) | Creating UI automation tests with accessibility identifiers |
| [assets/templates/full-stack-test.kt.template](assets/templates/full-stack-test.kt.template) | Creating Koin + MockEngine + ViewModel integration test |
| [assets/templates/mock-engine-test.kt.template](assets/templates/mock-engine-test.kt.template) | Creating standalone MockEngine with route-based responses |
| [assets/snippets/run-test-with-dispatcher.kt](assets/snippets/run-test-with-dispatcher.kt) | Choosing between UnconfinedTestDispatcher and StandardTestDispatcher |
| [assets/snippets/test-data-builder-pattern.kt](assets/snippets/test-data-builder-pattern.kt) | Creating factory functions with default parameters for test data |
| [assets/snippets/mockk-coroutine-mocking.kt](assets/snippets/mockk-coroutine-mocking.kt) | Using coEvery, coVerify, argument capture, sequential returns |
| [assets/snippets/flow-collection-swift.swift](assets/snippets/flow-collection-swift.swift) | FlowWrapper, SKIE async sequence, and Combine patterns |
| [assets/snippets/swift-fake-repositories.swift](assets/snippets/swift-fake-repositories.swift) | Fake repos for Swift-side testing (success and error paths) |
| [assets/snippets/koin-test-module.kt](assets/snippets/koin-test-module.kt) | Test module patterns: simple, configurable, override, base class |
| [assets/snippets/mock-engine-patterns.kt](assets/snippets/mock-engine-patterns.kt) | Route-based, stateful, sequential, and error simulation engines |

## Validation

### A. Common testing correctness

- [ ] All test files in commonTest use `kotlin.test.Test`, not `org.junit.Test`
- [ ] All coroutine tests use `runTest`, not `runBlocking`
- [ ] Fakes preferred over mocks -- no MockK or Mockito in commonTest
- [ ] Flow tests use Turbine `test {}` blocks, not `flow.first()`
- [ ] All tests in commonTest compile for all targets (JVM, Android, iOS)
- [ ] No platform-specific imports (`android.*`, `java.*`, `platform.darwin.*`) in commonTest
- [ ] No backtick function names in commonTest (fails on Kotlin/Native)
- [ ] `Dispatchers.setMain()` called before ViewModel creation in `@BeforeTest`
- [ ] `Dispatchers.resetMain()` called in `@AfterTest`
- [ ] `assertIs<>()`, `assertContains()`, `assertContentEquals()` used where appropriate
- [ ] `kotlinx-coroutines-test` shares `version.ref` with `kotlinx-coroutines-core`
- [ ] Tests use `UnconfinedTestDispatcher` for simple tests, `StandardTestDispatcher` only for timing tests
- [ ] Parameterized test helper collects all failures before reporting

### B. Android testing correctness

- [ ] Compose UI tests use `createComposeRule()` with `@RunWith(RobolectricTestRunner::class)`
- [ ] `robolectric.properties` exists at `composeApp/src/androidUnitTest/resources/`
- [ ] `isIncludeAndroidResources = true` set in `android.testOptions.unitTests`
- [ ] `mockk-android` (not plain `mockk`) in androidUnitTest dependencies
- [ ] Room DAO tests use `allowMainThreadQueries()` and close the database in `@After`
- [ ] Suspend functions stubbed with `coEvery`/`coVerify` (not `every`/`verify`)
- [ ] Fakes used for repository/data-source dependencies; MockK reserved for Android framework types
- [ ] `@GraphicsMode(GraphicsMode.Mode.NATIVE)` and `@Config(qualifiers = ...)` set on screenshot test classes
- [ ] Golden files committed to version control

### C. iOS testing correctness

- [ ] No backtick test names in any Kotlin/Native test file (iosTest or commonTest)
- [ ] FlowWrapper collector attached before triggering emission in every flow test
- [ ] All FlowWrappers cancelled in tearDown to prevent coroutine leaks
- [ ] `@Throws(Exception::class)` on all Kotlin suspend functions called from Swift
- [ ] Async bridge tests cover both success and error paths
- [ ] XCUITests use `waitForExistence(timeout:)` instead of `Thread.sleep()`
- [ ] Kotlin/Native tests use `@BeforeTest`/`@AfterTest`, not JUnit annotations
- [ ] Test names use camelCase for Kotlin/Native compatibility

### D. Integration testing correctness

- [ ] Real Koin DI container used in integration tests (not manual construction)
- [ ] Database tests use `Room.inMemoryDatabaseBuilder` (not file-backed)
- [ ] Network tests use `MockEngine` (not real HTTP calls)
- [ ] Tests are isolated: `@AfterTest` calls `stopKoin()`, closes databases, resets `Dispatchers.Main`
- [ ] `checkModules()` validates the DI graph in at least one test
- [ ] No shared mutable state between test classes
- [ ] Happy path and error path both covered per feature
- [ ] Tests verify full stack (MockEngine through ViewModel), not just one layer

### E. Security

- [ ] No hardcoded credentials, tokens, or API keys in test fixtures
- [ ] Fake tokens use obviously-fake values (e.g., `fake-token-abc123`)
- [ ] JSON fixtures do not contain real PII

### F. Integration and cross-skill consistency

- [ ] Depends-on skills (`kmp-project-setup`, `kmp-architecture`) exist and are referenced correctly
- [ ] Fakes from commonTest importable from `androidUnitTest` and `iosTest` source sets
- [ ] CI pipeline runs `jvmTest` as the first gate before platform tests
- [ ] Test results uploaded as CI artifacts on failure
- [ ] Tests pass identically on JVM, Android, and iOS targets
