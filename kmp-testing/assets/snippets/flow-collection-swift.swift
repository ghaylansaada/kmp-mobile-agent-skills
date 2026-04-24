import XCTest
@testable import ComposeApp

/// Patterns for collecting Kotlin Flows from Swift tests.
///
/// GOTCHA: Always attach the collector BEFORE triggering emission.
/// If you call viewModel.loadAccounts() before wrapper.collect(),
/// the emission fires into the void and the expectation times out.
///
/// GOTCHA: Always call wrapper.cancel() in teardown. Failing to cancel
/// leaks the coroutine scope and causes memory warnings.

// MARK: - Pattern 1: FlowWrapper with XCTestExpectation

final class FlowWrapperExampleTests: XCTestCase {

    private var wrapper: FlowWrapper<NSArray>?

    override func tearDown() {
        wrapper?.cancel()
        wrapper = nil
        super.tearDown()
    }

    func testCollectFlowEmitsValues() {
        let expectation = XCTestExpectation(description: "Flow emits a value")

        let viewModel = AccountViewModel(
            repository: FakeAccountRepositoryForSwift(),
        )
        var receivedValues: [Account] = []

        // Step 1: Attach collector FIRST
        wrapper = FlowWrapper<NSArray>(flow: viewModel.accountsFlow)
        wrapper?.collect(
            onEach: { value in
                if let accounts = value as? [Account], !accounts.isEmpty {
                    receivedValues = accounts
                    expectation.fulfill()
                }
            },
            onComplete: {},
            onError: { error in
                XCTFail("Unexpected error: \(error)")
            }
        )

        // Step 2: THEN trigger emission
        viewModel.loadAccounts()

        wait(for: [expectation], timeout: 5.0)
        XCTAssertFalse(receivedValues.isEmpty)
    }
}

// MARK: - Pattern 2: SKIE AsyncSequence (Kotlin 2.0+ with SKIE plugin)

final class SKIEAsyncSequenceExampleTests: XCTestCase {

    /// With the SKIE plugin, Kotlin StateFlow/SharedFlow become Swift
    /// AsyncSequence. No FlowWrapper needed.
    func testCollectFlowAsAsyncSequence() async {
        let viewModel = AccountViewModel(
            repository: FakeAccountRepositoryForSwift(),
        )

        viewModel.loadAccounts()

        var emissions: [AccountUiState] = []
        for await state in viewModel.uiState.prefix(2) {
            emissions.append(state)
        }

        XCTAssertEqual(emissions.count, 2)
    }
}

// MARK: - Pattern 3: Combine Publisher Wrapper

/// If the project exposes a Combine publisher bridge around Kotlin Flow,
/// use the standard Combine sink pattern with XCTestExpectation.
///
/// ```swift
/// import Combine
///
/// final class CombineFlowExampleTests: XCTestCase {
///
///     private var cancellables = Set<AnyCancellable>()
///
///     func testCollectFlowAsCombinePublisher() {
///         let expectation = XCTestExpectation(description: "Publisher emits")
///         let viewModel = AccountViewModel(
///             repository: FakeAccountRepositoryForSwift(),
///         )
///
///         viewModel.accountsPublisher
///             .sink { accounts in
///                 XCTAssertFalse(accounts.isEmpty)
///                 expectation.fulfill()
///             }
///             .store(in: &cancellables)
///
///         viewModel.loadAccounts()
///         wait(for: [expectation], timeout: 5.0)
///     }
/// }
/// ```
