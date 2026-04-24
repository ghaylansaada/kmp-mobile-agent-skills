# Reusable Material 3 Components

For shimmer/skeleton loading patterns (animated placeholders, skeleton screens, loading overlays), see [loading-states.md](loading-states.md).

## Design Token and Resource Rules

Every component in this file follows these rules:

1. **No hardcoded dp values** -- use `AppTheme.spacing.*`, `AppTheme.sizing.*`, or `AppTheme.corners.*`
2. **No hardcoded strings** -- use `stringResource(Res.string.*)` from Compose resources
3. **No hardcoded colors** -- use `MaterialTheme.colorScheme.*`
4. **No hardcoded font sizes** -- use `MaterialTheme.typography.*`
5. **All touch targets >= 48dp** -- use `AppTheme.sizing.minTouchTarget`

See [design-tokens.md](design-tokens.md) for the full token system.

### Wrong vs Right

```kotlin
// WRONG -- hardcoded values everywhere
Modifier.padding(16.dp)
Spacer(Modifier.width(8.dp))
Modifier.size(18.dp)
CircularProgressIndicator(strokeWidth = 2.dp)
Text("Loading...")
Text("Cancel", color = Color.Red)
Text("Title", fontSize = 20.sp)

// RIGHT -- design tokens + resources + theme
Modifier.padding(AppTheme.spacing.lg)
Spacer(Modifier.width(AppTheme.spacing.sm))
Modifier.size(AppTheme.sizing.iconSm)
CircularProgressIndicator(strokeWidth = AppTheme.spacing.xxs)
Text(stringResource(Res.string.loading))
Text(stringResource(Res.string.cancel), color = MaterialTheme.colorScheme.error)
Text(stringResource(Res.string.title), style = MaterialTheme.typography.titleLarge)
```

## Package Structure Convention

Reusable components live in the shared `ui/components/` package. Screen-specific composables live under `feature/{name}/`.

```
commonMain/
  kotlin/{your.package}/
    ui/
      components/     <- Shared reusable composables (buttons, inputs, dialogs, states)
      theme/          <- MaterialTheme, colors, typography, shapes, design tokens
    feature/
      account/        <- Screen-specific composables for account feature
      settings/       <- Screen-specific composables for settings feature
```

## Button Variants

Package: `ui.components.buttons` in `commonMain`

### AppPrimaryButton

```kotlin
@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    accessibilityLabel: String = text,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = AppTheme.sizing.minTouchTarget)
            .semantics { contentDescription = accessibilityLabel },
        enabled = enabled && !isLoading,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(AppTheme.sizing.iconSm),
                strokeWidth = AppTheme.spacing.xxs,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.width(AppTheme.spacing.sm))
            Text(stringResource(Res.string.loading))
        } else {
            leadingIcon?.let {
                Icon(it, contentDescription = null, modifier = Modifier.size(AppTheme.sizing.iconSm))
                Spacer(Modifier.width(AppTheme.spacing.sm))
            }
            Text(text)
        }
    }
}
```

### AppSecondaryButton

Same signature as `AppPrimaryButton`. Uses `FilledTonalButton` with `onSecondaryContainer` spinner color.

```kotlin
@Composable
fun AppSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    accessibilityLabel: String = text,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = AppTheme.sizing.minTouchTarget)
            .semantics { contentDescription = accessibilityLabel },
        enabled = enabled && !isLoading,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(AppTheme.sizing.iconSm),
                strokeWidth = AppTheme.spacing.xxs,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.width(AppTheme.spacing.sm))
            Text(stringResource(Res.string.loading))
        } else {
            leadingIcon?.let {
                Icon(it, contentDescription = null, modifier = Modifier.size(AppTheme.sizing.iconSm))
                Spacer(Modifier.width(AppTheme.spacing.sm))
            }
            Text(text)
        }
    }
}
```

### AppTextButton and AppOutlinedButton

```kotlin
@Composable
fun AppTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accessibilityLabel: String = text,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = AppTheme.sizing.minTouchTarget)
            .semantics { contentDescription = accessibilityLabel },
        enabled = enabled,
    ) {
        Text(text)
    }
}

@Composable
fun AppOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    accessibilityLabel: String = text,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = AppTheme.sizing.minTouchTarget)
            .semantics { contentDescription = accessibilityLabel },
        enabled = enabled && !isLoading,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(AppTheme.sizing.iconSm),
                strokeWidth = AppTheme.spacing.xxs,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(AppTheme.spacing.sm))
            Text(stringResource(Res.string.loading))
        } else {
            leadingIcon?.let {
                Icon(it, contentDescription = null, modifier = Modifier.size(AppTheme.sizing.iconSm))
                Spacer(Modifier.width(AppTheme.spacing.sm))
            }
            Text(text)
        }
    }
}
```

## Text Input with Validation

Package: `ui.components.inputs` in `commonMain`

### AppTextField

```kotlin
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    fieldErrors: List<ApiError> = emptyList(),
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: (() -> Unit)? = null,
    isPassword: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    accessibilityLabel: String = label,
) {
    val errorMessage = fieldErrors.firstOrNull()?.message
    val isError = errorMessage != null

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it) } },
            isError = isError,
            enabled = enabled,
            singleLine = singleLine,
            maxLines = maxLines,
            visualTransformation = if (isPassword) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction,
            ),
            keyboardActions = KeyboardActions(
                onDone = onImeAction?.let { { it() } },
                onNext = onImeAction?.let { { it() } },
                onSearch = onImeAction?.let { { it() } },
            ),
            supportingText = if (isError) {
                {
                    Text(
                        text = errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            } else {
                null
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = accessibilityLabel
                    if (isError) error(errorMessage.orEmpty())
                },
        )
    }
}
```

### errorsForField Helper

Filters `List<ApiError>` by field path for binding to a specific `AppTextField`:

```kotlin
fun List<ApiError>.errorsForField(fieldPath: String): List<ApiError> =
    filter { it.path == fieldPath }
```

## Feedback Components

Package: `ui.components.feedback` in `commonMain`

### AppConfirmDialog

```kotlin
@Composable
fun AppConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = stringResource(Res.string.confirm),
    dismissText: String = stringResource(Res.string.cancel),
    isDestructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier
                    .heightIn(min = AppTheme.sizing.minTouchTarget)
                    .semantics { contentDescription = confirmText },
            ) {
                val color = if (isDestructive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
                Text(confirmText, color = color)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .heightIn(min = AppTheme.sizing.minTouchTarget)
                    .semantics { contentDescription = dismissText },
            ) { Text(dismissText) }
        },
        modifier = modifier,
    )
}
```

### AppAlertDialog

Single-button informational dialog:

```kotlin
@Composable
fun AppAlertDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    okText: String = stringResource(Res.string.ok),
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .heightIn(min = AppTheme.sizing.minTouchTarget)
                    .semantics { contentDescription = okText },
            ) { Text(okText) }
        },
        modifier = modifier,
    )
}
```

### AppSnackbarHost + ErrorSnackbarEffect

```kotlin
@Composable
fun AppSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        Snackbar(snackbarData = data, actionOnNewLine = false)
    }
}

suspend fun showAppSnackbar(
    hostState: SnackbarHostState,
    message: String,
    actionLabel: String? = null,
    withDismissAction: Boolean = false,
    duration: SnackbarDuration = SnackbarDuration.Short,
): SnackbarResult = hostState.showSnackbar(
    message = message,
    actionLabel = actionLabel,
    withDismissAction = withDismissAction,
    duration = duration,
)

@Composable
fun ErrorSnackbarEffect(
    error: Throwable?,
    hostState: SnackbarHostState,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onDismissed: (() -> Unit)? = null,
) {
    LaunchedEffect(error) {
        if (error == null) return@LaunchedEffect
        val message = extractErrorMessage(error)
        val result = showAppSnackbar(
            hostState = hostState,
            message = message,
            actionLabel = actionLabel,
            duration = if (actionLabel != null) SnackbarDuration.Indefinite else SnackbarDuration.Short,
        )
        when (result) {
            SnackbarResult.ActionPerformed -> onAction?.invoke()
            SnackbarResult.Dismissed -> onDismissed?.invoke()
        }
    }
}

fun extractErrorMessage(error: Throwable): String = when (error) {
    is ApiCallException -> when (error.error) {
        is ApiResult.Error.InternetError -> "No internet connection. Please check your network."
        is ApiResult.Error.HttpError -> "Server error (${(error.error as ApiResult.Error.HttpError).status.value}). Please try again."
        else -> error.message ?: "An unexpected error occurred."
    }
    else -> error.message ?: "An unexpected error occurred."
}
```

Note: `extractErrorMessage` returns runtime error messages from exception data. These are not user-facing strings that need localization via `stringResource` -- they come from the API layer. If your app requires localized error messages, map error types to string resources in a separate function.

### AppBottomSheet

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = AppTheme.spacing.lg)
                .padding(bottom = AppTheme.spacing.lg),
        ) {
            title?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = AppTheme.spacing.lg),
                )
            }
            content()
        }
    }
}
```

## State Display Components

Package: `ui.components.states` in `commonMain`

### Loading Components

```kotlin
@Composable
fun FullScreenLoading(
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    Box(
        modifier
            .fillMaxSize()
            .semantics {
                contentDescription = message ?: "Loading"
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(AppTheme.sizing.minTouchTarget))
            message?.let {
                Spacer(Modifier.height(AppTheme.spacing.lg))
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun InlineLoading(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(AppTheme.spacing.lg)
            .semantics { contentDescription = "Loading more items" },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(AppTheme.sizing.iconMd),
            strokeWidth = AppTheme.spacing.xxs,
        )
    }
}

@Composable
fun AppLinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.spacing.lg)
            .semantics {
                contentDescription = label ?: "Progress: ${(progress * 100).toInt()}%"
            },
    ) {
        label?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(AppTheme.spacing.xs))
        }
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
    }
}
```

### EmptyState

```kotlin
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(AppTheme.spacing.xxl)
            .semantics { contentDescription = title },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        icon?.let {
            Icon(
                it,
                contentDescription = null,
                Modifier.size(AppTheme.sizing.iconLg * 2),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(AppTheme.spacing.lg))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        subtitle?.let {
            Spacer(Modifier.height(AppTheme.spacing.sm))
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(AppTheme.spacing.xl))
            AppPrimaryButton(text = actionText, onClick = onAction)
        }
    }
}
```

### ErrorState + ErrorStateFromThrowable

```kotlin
@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(Res.string.error_generic_title),
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(AppTheme.spacing.xxl)
            .semantics { contentDescription = "$title. $message" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.Warning,
            contentDescription = null,
            Modifier.size(AppTheme.sizing.iconLg * 2),
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(AppTheme.spacing.lg))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(AppTheme.spacing.sm))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(AppTheme.spacing.xl))
        AppPrimaryButton(
            text = stringResource(Res.string.retry),
            onClick = onRetry,
        )
    }
}

@Composable
fun ErrorStateFromThrowable(
    error: Throwable,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (title, message) = when (error) {
        is ApiCallException -> when (error.error) {
            is ApiResult.Error.InternetError ->
                stringResource(Res.string.error_no_connection_title) to
                    stringResource(Res.string.error_no_connection_message)
            is ApiResult.Error.HttpError ->
                stringResource(Res.string.error_server_title) to
                    stringResource(Res.string.error_server_message)
            else ->
                stringResource(Res.string.error_generic_title) to
                    (error.message ?: stringResource(Res.string.error_generic_message))
        }
        else ->
            stringResource(Res.string.error_generic_title) to
                (error.message ?: stringResource(Res.string.error_generic_message))
    }
    ErrorState(title = title, message = message, onRetry = onRetry, modifier = modifier)
}
```

### InlineError

```kotlin
@Composable
fun InlineError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .padding(AppTheme.spacing.lg)
            .semantics { contentDescription = "Error: $message" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(AppTheme.spacing.sm))
        AppOutlinedButton(
            text = stringResource(Res.string.retry),
            onClick = onRetry,
        )
    }
}
```

### PagingStateHandler + PagingFooter

```kotlin
@Composable
fun PagingStateHandler(
    pagingState: PagingState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    emptyTitle: String = stringResource(Res.string.empty_default_title),
    emptySubtitle: String? = null,
    errorMessage: String = stringResource(Res.string.error_load_content),
    content: @Composable () -> Unit,
) {
    when (pagingState) {
        PagingState.InitialLoading -> FullScreenLoading(
            message = stringResource(Res.string.loading),
            modifier = modifier,
        )
        PagingState.InitialError -> ErrorState(
            message = errorMessage,
            onRetry = onRetry,
            modifier = modifier,
        )
        PagingState.Empty -> EmptyState(
            title = emptyTitle,
            subtitle = emptySubtitle,
            modifier = modifier,
        )
        PagingState.Content,
        PagingState.Appending,
        PagingState.AppendError,
        PagingState.EndOfPagination,
        -> content()
    }
}

@Composable
fun PagingFooter(
    pagingState: PagingState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    appendErrorMessage: String = stringResource(Res.string.error_load_more),
) {
    when (pagingState) {
        PagingState.Appending -> InlineLoading(modifier = modifier)
        PagingState.AppendError -> InlineError(
            message = appendErrorMessage,
            onRetry = onRetry,
            modifier = modifier,
        )
        PagingState.EndOfPagination -> Box(
            modifier
                .fillMaxWidth()
                .padding(AppTheme.spacing.lg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(Res.string.end_of_list),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        else -> {}
    }
}
```

## Required String Resources

Components in this file reference the following string resource keys. Define them in `commonMain/composeResources/values/strings.xml`:

```xml
<resources>
    <string name="loading">Loading...</string>
    <string name="confirm">Confirm</string>
    <string name="cancel">Cancel</string>
    <string name="ok">OK</string>
    <string name="retry">Retry</string>
    <string name="error_generic_title">Something went wrong</string>
    <string name="error_generic_message">An unexpected error occurred.</string>
    <string name="error_no_connection_title">No connection</string>
    <string name="error_no_connection_message">Check your internet connection and try again.</string>
    <string name="error_server_title">Server error</string>
    <string name="error_server_message">The server returned an error. Please try again later.</string>
    <string name="error_load_content">Failed to load content.</string>
    <string name="error_load_more">Failed to load more items.</string>
    <string name="empty_default_title">No items found</string>
    <string name="end_of_list">You've reached the end</string>
</resources>
```
