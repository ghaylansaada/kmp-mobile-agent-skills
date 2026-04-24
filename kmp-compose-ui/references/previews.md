# @Preview Support

## Preview Strategy

Composables using `koinInject()` crash in `@Preview`. Extract a stateless content composable and preview that:

```kotlin
@Preview
@Composable
fun AccountScreenPreview() {
    AppTheme {
        AccountScreenContent(
            state = AccountUiState(lastMessage = "Preview mode"),
            pagingItems = /* mock or omit paging for preview */,
            onRefreshUser = {},
            onRefreshList = {},
        )
    }
}
```

For screens with `LazyPagingItems`, preview the non-paging portions by extracting smaller composables (header, error bar, item row) and previewing those individually.

## Button Preview

```kotlin
@Preview
@Composable
private fun AppPrimaryButtonPreview() {
    AppTheme {
        Column(
            modifier = Modifier.padding(AppTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            AppPrimaryButton(text = "Submit", onClick = {})
            AppPrimaryButton(text = "Submitting", onClick = {}, isLoading = true)
            AppSecondaryButton(text = "Secondary", onClick = {})
            AppOutlinedButton(text = "Outlined", onClick = {})
            AppTextButton(text = "Cancel", onClick = {})
        }
    }
}
```

## Text Field Preview

```kotlin
@Preview
@Composable
private fun AppTextFieldPreview() {
    AppTheme {
        Column(modifier = Modifier.padding(AppTheme.spacing.lg)) {
            AppTextField(
                value = "",
                onValueChange = {},
                label = "Email",
                placeholder = "you@example.com",
            )
            Spacer(Modifier.height(AppTheme.spacing.md))
            AppTextField(
                value = "bad-email",
                onValueChange = {},
                label = "Email",
                fieldErrors = listOf(
                    ApiError(
                        path = "email",
                        code = ApiErrorCode.EMAIL_FORMAT_VIOLATION,
                        message = "Please enter a valid email address.",
                        location = ApiErrorLocation.BODY,
                        data = null,
                    ),
                ),
            )
        }
    }
}
```

## Responsive Layout Preview

```kotlin
@Preview
@Composable
private fun ResponsiveLayoutPreview() {
    AppTheme {
        ResponsiveLayout { windowSizeClass ->
            val padding = adaptivePadding(windowSizeClass)
            Text(
                text = "Window size: $windowSizeClass",
                modifier = Modifier.padding(padding),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
```

## Rules

1. Always wrap preview content in `AppTheme { }` (not bare `MaterialTheme { }`) -- this provides both Material theming and design tokens via CompositionLocal
2. Never add `@Preview` to composables that call `koinInject()` -- extract a stateless content composable instead
3. Use sample data that does not contain real user information
4. For composables with many parameters, create multiple previews showing different states (loading, error, empty, populated)

## Verifying Previews

1. Open the file containing the `@Preview`-annotated composable
2. Switch to Split or Design view in Android Studio
3. The preview should render the composable layout with sample data

## Common Issues

| Symptom | Cause | Fix |
|---------|-------|-----|
| `NoSuchElementException` in preview | `koinInject()` without Koin init | Extract stateless content composable, preview that instead |
| `@Preview` not rendering | Missing `AppTheme` wrapper in preview | Wrap preview content in `AppTheme { }` |
| Design tokens return defaults | Preview uses bare `MaterialTheme` | Switch to `AppTheme { }` which provides CompositionLocal tokens |
| UI stutters / excessive recomposition | Unstable state objects | Use `data class` with `@Immutable`; avoid new lambda instances per recomposition |
| iOS rendering differences | Different rendering backends per platform | Do not rely on pixel-identical screenshot tests across platforms |
