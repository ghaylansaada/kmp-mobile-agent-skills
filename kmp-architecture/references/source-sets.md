# Source Set Organization

## Directory Structure

```
composeApp/src/
  commonMain/kotlin/{your/package}/
    core/
      platform/
        Platform.kt                # expect fun getPlatform(): Platform
        PlatformContext.kt         # expect class PlatformContext
      ui/
        UiState.kt                 # Generic sealed UiState<T>
    data/
      local/
        AppDatabase.kt
        AppDatabaseConstructor.kt  # expect object (KSP-generated actuals)
        dao/AccountDao.kt
        entity/AccountEntity.kt
      remote/
        service/AuthService.kt
        dto/AccountDto.kt
        util/ApiCall.kt, ApiResult.kt
      repository/
        AccountRepository.kt
        AccountRepositoryImpl.kt
    domain/
      model/AccountSummary.kt
      usecase/GetAccountUseCase.kt
    presentation/
      user/
        AccountScreen.kt
        AccountUiEvent.kt
        AccountUiState.kt
        AccountViewModel.kt
    di/
      PlatformModule.kt            # expect fun platformModule(...)
      ViewModelModule.kt
      RepositoryModule.kt

  androidMain/kotlin/{your/package}/
    core/platform/
      Platform.android.kt          # actual fun getPlatform()
      PlatformContext.android.kt   # actual class PlatformContext
    di/
      PlatformModule.android.kt    # actual fun platformModule(...)

  iosMain/kotlin/{your/package}/
    core/platform/
      Platform.ios.kt              # actual fun getPlatform()
      PlatformContext.ios.kt       # actual class PlatformContext
    di/
      PlatformModule.ios.kt        # actual fun platformModule(...)

  commonTest/kotlin/{your/package}/
    core/platform/
      PlatformTest.kt              # Tests for expect/actual pairs
    presentation/
      AccountViewModelTest.kt
```

## What Goes Where

### commonMain

All shared business logic, data layer, presentation layer, and UI:
- Repository interfaces and implementations (using only KMP-compatible types)
- UseCase classes
- ViewModels with StateFlow
- UiState sealed interfaces and data classes
- Screen composables
- Expect declarations for platform-specific APIs
- DI module definitions (except platform-specific bindings)

**Rule:** No `android.*`, `java.*`, `platform.Foundation.*`, or `platform.UIKit.*` imports allowed. Only Kotlin stdlib, KMP libraries, and expect declarations.

### androidMain

Android-specific actual implementations:
- `actual fun getPlatform()` using `android.os.Build`
- `actual class PlatformContext` wrapping `android.content.Context`
- `actual fun platformModule()` providing Android-specific DI bindings
- Any code requiring `android.*` or `java.*` imports

### iosMain

iOS-specific actual implementations:
- `actual fun getPlatform()` using `platform.UIKit.UIDevice`
- `actual class PlatformContext` (empty marker on iOS)
- `actual fun platformModule()` providing iOS-specific DI bindings
- NSData/ByteArray conversion helpers using `kotlinx.cinterop`
- Any code requiring `platform.*` imports

**Rule:** Place actuals in `iosMain` (shared), not `iosArm64Main` or `iosSimulatorArm64Main`, unless you genuinely need per-architecture differences. Missing actuals in one architecture target produce confusing error messages pointing at `iosMain`.

### commonTest

Shared tests that run on all platforms:
- ViewModel tests with Turbine
- Repository behavior tests with fakes
- Expect/actual contract tests (verify behavior, not implementation)

## File Naming Conventions

Two strategies are used:
- **Platform suffix** (`.android.kt` / `.ios.kt`) -- for lightweight expect/actual functions or simple classes
- **Same file name** (no suffix) -- for expect classes with substantial actual implementations

The Kotlin compiler matches expect to actual by fully qualified name, not by file name. The `package` declaration must be identical across all source sets.

## Platform Boundary Rules

1. **Never import platform types in commonMain.** This includes `android.content.Context`, `java.io.File`, `platform.Foundation.NSData`, `platform.UIKit.UIDevice`.
2. **Use expect/actual or interfaces to bridge.** When common code needs platform behavior, declare an `expect` in commonMain and implement `actual` in each platform source set.
3. **Repository and UseCase interfaces must be pure Kotlin/KMP types.** Return `Flow`, `suspend` functions, data classes -- never `LiveData`, `Context`, or `NSObject`.
4. **ViewModel must not import platform packages.** No `android.*` or `platform.*` in ViewModel code. Platform behavior flows through Repository/UseCase abstractions.
