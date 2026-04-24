// Reusable Swift utilities for observing Kotlin Flows via SKIE.
// For pattern explanations, see references/swift-consumption.md.

import SwiftUI
import ComposeApp

// MARK: - Multi-Flow Observer

/// Manages multiple Flow observation tasks with automatic cleanup.
class FlowObserver: ObservableObject {
    private var tasks: [Task<Void, Never>] = []

    func observe<S: AsyncSequence>(
        _ sequence: S,
        onEach: @escaping @MainActor @Sendable (S.Element) -> Void,
    ) {
        let task = Task { [weak self] in
            guard self != nil else { return }
            do {
                for try await value in sequence {
                    guard !Task.isCancelled else { break }
                    await onEach(value)
                }
            } catch {}
        }
        tasks.append(task)
    }

    /// Observe with autoreleasepool for high-frequency emissions.
    /// Use this for Flows that emit rapidly (sensor data, large batches).
    func observeFrequent<S: AsyncSequence>(
        _ sequence: S,
        onEach: @escaping @MainActor @Sendable (S.Element) -> Void,
    ) {
        let task = Task { [weak self] in
            guard self != nil else { return }
            do {
                for try await value in sequence {
                    guard !Task.isCancelled else { break }
                    autoreleasepool {
                        Task { @MainActor in onEach(value) }
                    }
                }
            } catch {}
        }
        tasks.append(task)
    }

    func cancelAll() {
        tasks.forEach { $0.cancel() }
        tasks.removeAll()
    }

    deinit { cancelAll() }
}

// MARK: - @MainActor Async Loader

/// Generic loader for Kotlin suspend functions in SwiftUI.
@MainActor
class AsyncLoader<T>: ObservableObject {
    @Published var data: T?
    @Published var isLoading = false
    @Published var errorMessage: String?

    func load(_ operation: @escaping () async throws -> T) async {
        isLoading = true
        defer { isLoading = false }
        do {
            data = try await operation()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

// MARK: - Timeout Helper

/// Wraps an async operation with a deadline. Throws CancellationError on timeout.
/// Use in tests to prevent Flow-backed AsyncSequence tests from hanging.
func withTimeout<T: Sendable>(
    seconds: TimeInterval,
    operation: @escaping @Sendable () async throws -> T,
) async throws -> T {
    try await withThrowingTaskGroup(of: T.self) { group in
        group.addTask { try await operation() }
        group.addTask {
            try await Task.sleep(nanoseconds: UInt64(seconds * 1_000_000_000))
            throw CancellationError()
        }
        let result = try await group.next()!
        group.cancelAll()
        return result
    }
}
