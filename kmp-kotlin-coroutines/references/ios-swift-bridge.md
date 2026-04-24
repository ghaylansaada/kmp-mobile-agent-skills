# iOS Swift Bridge (SKIE)

SKIE (Swift Kotlin Interface Enhancer) transforms Kotlin coroutine APIs into idiomatic Swift at the framework level. No Kotlin code changes required.

## SKIE Setup

### Version Catalog

Add to `gradle/libs.versions.toml`:

```toml
[versions]
skie = "..."

[plugins]
skie = { id = "co.touchlab.skie", version.ref = "skie" }
```

### Build Scripts

Root `build.gradle.kts`:

```kotlin
plugins {
    // ... existing plugins ...
    alias(libs.plugins.skie) apply false
}
```

`composeApp/build.gradle.kts`:

```kotlin
plugins {
    // ... existing plugins ...
    alias(libs.plugins.skie)
}
```

### Configuration Block

Add after the `kotlin { }` block in `composeApp/build.gradle.kts`:

```kotlin
skie {
    features {
        coroutinesInterop = true       // suspend → async, Flow → AsyncSequence (default: true)
        sealedInterop = true           // sealed class → Swift enum (default: true)
        defaultArgumentInterop = true  // Kotlin default args in Swift (default: true)
        enableSwiftUIObservingPreview = true  // StateFlow → @Published (opt-in)
    }
}
```

For faster incremental builds, add to `gradle.properties`:

```properties
skie.incremental=true
```

After setup, clean Xcode build folder (Cmd+Shift+K). The first build after adding SKIE is slower as it generates the full Swift bridging layer.

### Build Impact

| Metric | Without SKIE | With SKIE |
|--------|-------------|-----------|
| Framework build time | baseline | ~+50% |
| Framework size | baseline | ~+12% |
| Swift API surface | Completion handlers | async/await, AsyncSequence |

### Compose-Only Apps

If the app is 100% Compose (no native SwiftUI screens consuming Kotlin APIs), SKIE provides no benefit and can be omitted.

## Suspend to async/await

```swift
import ComposeApp

func loadUser(repository: UserRepository, userId: String) async {
    do {
        let result = try await repository.fetchUser(userId: userId)
        switch onEnum(of: result) {
        case .success(let success):
            print("User: \(success.data)")
        case .error(let error):
            print("Error: \(error.message ?? "")")
        case .loading:
            break
        }
    } catch {
        print("Exception: \(error.localizedDescription)")
    }
}
```

Without SKIE, the same call uses a completion handler callback -- no `async/await`, no structured concurrency, manual thread dispatch required.

## Flow to AsyncSequence

```swift
class ItemListObserver: ObservableObject {
    private let repository: ItemRepository
    private var observationTask: Task<Void, Never>?

    @Published var items: [ItemEntity] = []

    init(repository: ItemRepository) {
        self.repository = repository
    }

    func startObserving() {
        observationTask = Task { [weak self] in
            guard let self else { return }
            for await items in self.repository.observeItems() {
                guard !Task.isCancelled else { break }
                await MainActor.run { [weak self] in
                    self?.items = items as? [ItemEntity] ?? []
                }
            }
        }
    }

    func stopObserving() {
        observationTask?.cancel()
        observationTask = nil
    }

    deinit { observationTask?.cancel() }
}
```

Always use `[weak self]` in Task closures and in nested `MainActor.run` closures.

## StateFlow to SwiftUI Observable

Requires `enableSwiftUIObservingPreview = true` in SKIE config.

```swift
import SwiftUI
import ComposeApp

struct SettingsView: View {
    @ObservedObject var viewModel: SettingsViewModel

    var body: some View {
        let state = viewModel.state.value

        Form {
            Toggle("Dark Mode", isOn: Binding(
                get: { state.isDarkMode },
                set: { _ in viewModel.toggleDarkMode() }
            ))
        }
    }
}
```

Use `@ObservedObject` (not `@StateObject`) for Kotlin-created ViewModels. `state.value` is reactive only because SKIE generates `@Published` wrappers -- without the feature flag, it returns a dead snapshot.

## Sealed Class to Swift Enum

SKIE generates `onEnum(of:)` for exhaustive `switch`:

```swift
func handleResult<T>(_ result: ApiResult<T>) -> String {
    switch onEnum(of: result) {
    case .success(let s): return "Success: \(s.data)"
    case .error(let e): return "Error: \(e.message ?? "Unknown")"
    case .loading: return "Loading..."
    }
}
```

No `default` case needed -- the compiler enforces exhaustiveness. `onEnum(of:)` is SKIE-generated and does not exist without the plugin.

## Memory Management and Cancellation

### Retain Cycles

Kotlin/Native GC interacts differently with Objective-C ARC. Use `[weak self]` in every Task closure that holds Kotlin objects. Without it, the ViewModel is retained by the Task, which is retained by the coroutine scope, creating a cycle that leaks both.

### SKIE AsyncSequence Cancellation

Cancelling a Swift `Task` iterating a SKIE AsyncSequence propagates to the Kotlin coroutine:

```swift
class DataLoader {
    private var loadTask: Task<Void, Never>?

    func start() {
        loadTask = Task { [weak self] in
            guard let self else { return }
            for await items in self.repository.observeItems() {
                guard !Task.isCancelled else { return }
                await MainActor.run { [weak self] in
                    self?.items = items
                }
            }
        }
    }

    func stop() {
        loadTask?.cancel()  // Cancels Swift Task AND Kotlin coroutine
    }

    deinit { loadTask?.cancel() }
}
```

### Raw Kotlin Job Warning

Holding a raw Kotlin `Job` reference bypasses SKIE's cancellation bridge. Use SKIE's AsyncSequence for automatic cancellation. Only hold raw `Job` references when explicit lifecycle control is needed, and cancel both independently.

### Dispatchers.Main = @MainActor

`Dispatchers.Main` in Kotlin/Native uses `dispatch_get_main_queue()` -- the same thread as `@MainActor`. Blocking either one blocks the other. Keep heavy work on `Dispatchers.Default`. `Dispatchers.IO` does not exist on iOS -- use `Dispatchers.Default` for background work.

### autoreleasepool for Tight Loops

When collecting Flow emissions in a tight loop, Objective-C temporaries accumulate each iteration:

```swift
// CORRECT: autoreleasepool drains temporaries each iteration
for await batch in repository.observeLargeBatches() {
    autoreleasepool {
        processBatch(batch)
    }
}
```

This is critical for Flows that emit frequently (sensor data, large database result sets, real-time feeds). For infrequent Flows, `autoreleasepool` is optional.

## Generics Preservation

Without SKIE, Kotlin generics are erased to `AnyObject` in Swift. With SKIE, simple generic types are preserved:

```swift
// With SKIE: generic type preserved
let result: ApiResult<UserEntity> = try await repository.fetchUser(userId: "123")

// Without SKIE: erased
let result: ApiResult<AnyObject> = try await repository.fetchUser(userId: "123")
```

Caveat: deeply nested generics, variance annotations (`in`/`out`), and star projections may still erase to `AnyObject`.

## @ObjCName for Clean Swift API Surface

```kotlin
@ObjCName("UserProfile", exact = true)
data class UserProfileEntity(
    val id: String,
    val displayName: String,
)
```

Use `exact = true` to prevent SKIE from further renaming.

## Template Patterns Enhanced by SKIE

| Kotlin Pattern | Swift Before SKIE | Swift With SKIE |
|---------------|-------------------|-----------------|
| `ApiResult<T>` sealed class | `if let` chains | `switch onEnum(of:)` exhaustive |
| `SessionManager.sessionState: StateFlow` | Manual collection | `@ObservedObject` reactive |
| `ViewModel.state: StateFlow<UiState>` | Completion callbacks | `.state.value` observable |
| `Repository.suspend fun fetch()` | Completion handler | `try await` |
| `Repository.fun observe(): Flow` | FlowCollector wrapper | `for await` loop |

## Setup Troubleshooting

| Symptom | Fix |
|---------|-----|
| `Plugin [id: 'co.touchlab.skie'] was not found` | Ensure `mavenCentral()` in `pluginManagement.repositories` in `settings.gradle.kts` |
| Swift compilation errors in generated SKIE code | Clean build then rebuild framework, then Xcode Clean Build Folder |
| `IncompatibleClassChangeError` during build | SKIE/Kotlin version mismatch -- check SKIE compatibility table |
| `onEnum(of:)` not found | SKIE plugin not applied or framework not rebuilt |
| SwiftUI view not updating | Missing `enableSwiftUIObservingPreview` or wrong property wrapper -- use `@ObservedObject` not `@StateObject` |
| Generic type erased to `AnyObject` | Rebuild, check `@HiddenFromObjC`, simplify generic nesting |
| Kotlin coroutine runs after Swift Task cancel | Raw `Job` reference, not SKIE AsyncSequence -- use SKIE's bridged APIs or cancel Job explicitly |
| App freezes (main thread deadlock) | Blocking on `Dispatchers.Main` while `@MainActor` waits -- move heavy work to `Dispatchers.Default` |
