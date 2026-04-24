import Foundation
@testable import ComposeApp

/// Fake repositories for Swift-side testing of shared KMP APIs.
/// Used by FlowCollectionTests, AsyncBridgeTests, and AccountViewModelSwiftTests.

// MARK: - Fake Account Repository (success path)

class FakeAccountRepositoryForSwift: AccountRepository {
    func observeAccounts() -> any Kotlinx_coroutines_coreFlow {
        fatalError("Use FlowWrapper pattern instead")
    }
    func fetchAccounts() async throws -> [Account] {
        [Account(id: "1", username: "swift-test", email: "test@example.com",
                 displayName: "Test", avatarUrl: nil, isActive: true)]
    }
    func login(username: String, password: String) async throws -> Account {
        Account(id: "1", username: username, email: "\(username)@example.com",
                displayName: username, avatarUrl: nil, isActive: true)
    }
    func logout() async throws {}
}

// MARK: - Failing Repository (error path)

class FailingRepository: AccountRepository {
    func observeAccounts() -> any Kotlinx_coroutines_coreFlow { fatalError() }
    func fetchAccounts() async throws -> [Account] {
        throw NSError(domain: "TestError", code: 500,
                      userInfo: [NSLocalizedDescriptionKey: "Server error"])
    }
    func login(username: String, password: String) async throws -> Account {
        throw NSError(domain: "TestError", code: 401, userInfo: nil)
    }
    func logout() async throws {}
}
