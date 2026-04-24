# Integration: Compose UI

## Upstream Dependencies

The Compose Multiplatform UI layer requires correctly configured source sets (commonMain, androidMain, iosMain) and the Kotlin Multiplatform Gradle plugin. The Compose plugins (`composeMultiplatform`, `composeCompiler`) must be applied in the module's `build.gradle.kts`.

UI components require Compose runtime, foundation, material3, and ui dependencies in commonMain.

## Connected Skills

### kmp-resources-management

Screen composables reference shared resources via the generated `Res` class:

```kotlin
import org.jetbrains.compose.resources.painterResource
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.compose_multiplatform

@Composable
fun LogoSection() {
    Image(
        painter = painterResource(Res.drawable.compose_multiplatform),
        contentDescription = "Logo"
    )
}
```

### Image Loading (Coil)

The App composable sets up the Coil ImageLoader singleton via `setSingletonImageLoaderFactory`. Screen composables use `AppAsyncImage` for remote images:

```kotlin
AppAsyncImage(
    path = "/uploads/profile_123.jpg",
    modifier = Modifier.size(100.dp),
)
```

### Navigation

Scaffold from the navigation layer hosts snackbar and bottom sheet components:

```kotlin
@Composable
fun MainScaffold(onLogout: () -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    val tabNavController = rememberNavController()

    Scaffold(
        bottomBar = { AppBottomNavBar(navController = tabNavController) },
        snackbarHost = { AppSnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            // ... destinations
        }
    }
}
```

## Dependency Injection Integration

The module initialization order in `commonModules()`:

```kotlin
fun commonModules(context: PlatformContext) = listOf(
    coreModule(context),
    localStorageModule(),
    sessionModule(),
    ktorfitModule(),
    externalStorageModule(),
    imageLoaderModule(),
    repositoryModule(),
    viewModelModule()
)
```

ViewModels registered in `viewModelModule()` are injected via `koinInject()` in screen composables. See [screen-patterns.md](screen-patterns.md) for injection patterns.

## Platform Entry Point Integration

- **Android:** `MainActivity` calls `initKoin()` before `setContent { App() }`. Koin must be initialized before any composable tries to inject dependencies.
- **iOS:** `MainViewController` calls `initKoin()` inside the `ComposeUIViewController` lambda. On iOS, Koin initialization happens within the composition.

See [app-and-entry-points.md](app-and-entry-points.md) for full platform entry point code.

## Lifecycle Integration

### ViewModel Lifecycle

ViewModels use `androidx.lifecycle.ViewModel` from the KMP lifecycle library. The `viewModelScope` is tied to the composable lifecycle and cancels coroutines when the composable leaves the composition.

## Template Type Connections

### ApiError --> AppTextField

`AppTextField` accepts `fieldErrors: List<ApiError>` for field-level validation. Filter errors by field path:

```kotlin
@Composable
fun LoginForm(
    state: FormUiState,
    onEmailChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppTextField(
        value = state.email,
        onValueChange = onEmailChange,
        label = "Email",
        fieldErrors = state.errors.errorsForField("email"),
        keyboardType = KeyboardType.Email,
        modifier = modifier,
    )
}
```

### ApiCallException --> ErrorState / AppSnackbar

`ErrorStateFromThrowable` and `ErrorSnackbarEffect` handle `ApiCallException`:

```kotlin
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinInject(),
) {
    val error by viewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Full-screen error state
    if (error != null) {
        ErrorStateFromThrowable(
            error = error!!,
            onRetry = { viewModel.loadProfile() },
            modifier = modifier,
        )
    }

    // Or snackbar error
    ErrorSnackbarEffect(
        error = error,
        hostState = snackbarHostState,
        actionLabel = "Retry",
        onAction = { viewModel.loadProfile() },
    )
}
```

### PagingState --> PagingStateHandler

`PagingStateHandler` consumes the template's `PagingState` enum:

```kotlin
@Composable
fun AccountListScreen(
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = koinInject(),
) {
    val pagingItems = viewModel.pagingFlow.collectAsLazyPagingItems()
    val pagingState = pagingItems.toPaginationUiState()

    PagingStateHandler(
        pagingState = pagingState,
        onRetry = { pagingItems.refresh() },
        emptyTitle = "No accounts found",
        modifier = modifier,
    ) {
        LazyColumn {
            items(count = pagingItems.itemCount) { index ->
                val item = pagingItems[index] ?: return@items
                AccountRow(account = item)
            }
            item {
                PagingFooter(
                    pagingState = pagingState,
                    onRetry = { pagingItems.retry() },
                )
            }
        }
    }
}
```

## Downstream Usage Pattern

Every feature screen uses the shared component library:

1. **Buttons**: `AppPrimaryButton` for main actions, `AppOutlinedButton` for secondary, `AppTextButton` for tertiary
2. **Forms**: `AppTextField` with `errorsForField()` for each input
3. **Feedback**: `AppConfirmDialog` for destructive actions, `AppSnackbar` for transient messages, `AppBottomSheet` for contextual options
4. **States**: `PagingStateHandler` for list screens, `ErrorState` for non-paginated errors, `EmptyState` for empty content
