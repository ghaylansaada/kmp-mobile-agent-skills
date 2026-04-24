# App Update: DI Wiring and App Integration

## Koin Module (commonMain)

```kotlin
package {your.package}.di.modules

import {your.package}.core.update.AppUpdateManagerContract
import {your.package}.core.update.AppUpdateManagerWrapper
import {your.package}.core.update.PlatformAppUpdateManager
import {your.package}.presentation.update.AppUpdateViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun appUpdateModule() = module {
    single<AppUpdateManagerContract> {
        AppUpdateManagerWrapper(delegate = get<PlatformAppUpdateManager>())
    }
    viewModel { AppUpdateViewModel(updateManager = get()) }
}
```

## Android Platform Module

The Android `PlatformAppUpdateManager` requires an Activity reference and an `ActivityResultLauncher`. Create the manager in `MainActivity.onCreate` and register with Koin:

```kotlin
// In MainActivity.onCreate, after startKoin
getKoin().declare(
    PlatformAppUpdateManager(
        activity = this,
        forceUpdateMinVersion = config.minSupportedVersion,
        updateResultLauncher = updateResultLauncher,
    ),
)
```

## iOS Platform Module

```kotlin
// In platformModule(context, config)
single {
    PlatformAppUpdateManager(
        appStoreId = "123456789",
        forceUpdateMinVersion = config.minSupportedVersion,
    )
}
```

## Module Loading Order

```kotlin
startKoin {
    modules(
        platformModule(context, config),  // provides PlatformAppUpdateManager
        appUpdateModule(),                // provides Contract + ViewModel
    )
}
```

`platformModule` must load before `appUpdateModule` because `AppUpdateManagerWrapper` resolves `PlatformAppUpdateManager` via `get()`.

## App.kt Integration

```kotlin
package {your.package}

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import {your.package}.presentation.update.AppUpdateUiState
import {your.package}.presentation.update.AppUpdateViewModel
import {your.package}.presentation.update.ForceUpdateScreen
import {your.package}.presentation.update.UpdateBanner
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    val updateViewModel: AppUpdateViewModel = koinViewModel()
    val updateState by updateViewModel.updateState.collectAsState()

    LaunchedEffect(Unit) { updateViewModel.checkForUpdate() }

    when (val state = updateState) {
        is AppUpdateUiState.ForceUpdate -> ForceUpdateScreen(
            currentVersion = state.currentVersion,
            requiredVersion = state.requiredVersion,
            onUpdateClick = { updateViewModel.startForceUpdate() },
        )
        else -> MainContent(
            updateBanner = {
                if (state is AppUpdateUiState.OptionalUpdate) {
                    UpdateBanner(
                        availableVersion = state.availableVersion,
                        onUpdateClick = { updateViewModel.startUpdate() },
                        onDismiss = { updateViewModel.dismissBanner() },
                    )
                }
            },
        )
    }
}
```

## Force Update Version from Backend

In production, source `forceUpdateMinVersion` from a backend API or remote config, not hardcoded:

```kotlin
package {your.package}.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppConfigResponse(
    @SerialName("min_supported_version") val minSupportedVersion: String,
    @SerialName("force_update") val forceUpdate: Boolean,
)
```

Fetch at startup before the update check. If the config fetch fails, default to no force update.
