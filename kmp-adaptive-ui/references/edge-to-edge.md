# Edge-to-Edge Display

Edge-to-edge means the app draws behind system bars (status bar, navigation bar) and handles insets explicitly. On Android 16 (API 36), edge-to-edge is mandatory with no opt-out. On iOS, the safe area insets model already enforces similar behavior.

## Android: enableEdgeToEdge()

Call `enableEdgeToEdge()` in `Activity.onCreate()` **before** `super.onCreate()`:

```kotlin
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            App()
        }
    }
}
```

`enableEdgeToEdge()` does:
- Sets system bar backgrounds to transparent
- Configures light/dark system bar icon colors based on the theme
- Extends the app content behind both the status bar and the navigation bar

On Android 16 (API 36), this is the default behavior. The `R.attr#windowOptOutEdgeToEdgeEnforcement` attribute has been removed. There is no way to opt out.

## WindowInsets in Compose

### Core Inset Types

| Inset | Description |
|---|---|
| `WindowInsets.statusBars` | Top system bar (clock, battery, notifications) |
| `WindowInsets.navigationBars` | Bottom system bar (gesture handle or button bar) |
| `WindowInsets.systemBars` | Combined status + navigation bars |
| `WindowInsets.ime` | Software keyboard (Android only in Compose Multiplatform) |
| `WindowInsets.safeDrawing` | Area safe to draw content (excludes system bars and display cutouts) |
| `WindowInsets.safeContent` | Area safe for interactive content (includes IME) |

### Applying Insets with Modifiers

```kotlin
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.imePadding

// Apply all system bar insets
Column(
    modifier = Modifier
        .fillMaxSize()
        .systemBarsPadding()
) {
    // Content is inset from status and navigation bars
}

// Apply specific insets
Column(
    modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.statusBars)
) {
    // Content is inset from status bar only
}

// Handle keyboard overlap for input forms
Column(
    modifier = Modifier
        .fillMaxSize()
        .systemBarsPadding()
        .imePadding()
) {
    // Content shifts up when keyboard appears (Android)
}
```

### Scaffold and Insets

`Scaffold` handles insets via its `contentWindowInsets` parameter. By default, it applies `WindowInsets.systemBars` to its content:

```kotlin
Scaffold(
    topBar = {
        TopAppBar(
            title = { Text(stringResource(Res.string.app_title)) },
            // TopAppBar handles status bar insets internally
        )
    },
    contentWindowInsets = WindowInsets.systemBars,
) { innerPadding ->
    // innerPadding already accounts for system bars AND the top bar
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = innerPadding,
    ) {
        // Content
    }
}
```

**Do not add `systemBarsPadding()` to content inside a Scaffold that already handles insets.** This causes double padding.

### Consuming Insets

When a parent composable handles an inset, child composables should not handle it again. Compose tracks inset consumption automatically when you use `Modifier.windowInsetsPadding()`:

```kotlin
Column(
    modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.statusBars)
) {
    // Status bar inset is consumed
    // Children using windowInsetsPadding(WindowInsets.systemBars) will only apply
    // the navigation bar portion, not the status bar again
}
```

## iOS: Safe Area Insets

On iOS, safe area insets are handled by UIKit at the view controller level. In Compose Multiplatform, the `ComposeUIViewController` respects safe area insets by default.

For KMP, use `WindowInsets.safeDrawing` which maps to:
- Android: system bars + display cutouts
- iOS: safe area insets

```kotlin
// Cross-platform safe content area
Column(
    modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
) {
    // Content avoids system bars on Android and safe area edges on iOS
}
```

## Common Patterns

### Full-Screen Content with Transparent Bars

Draw content behind system bars (e.g., images, maps) and add insets only to interactive elements:

```kotlin
Box(modifier = Modifier.fillMaxSize()) {
    // Background image draws behind system bars
    AsyncImage(
        model = imageUrl,
        contentDescription = stringResource(Res.string.cd_background_image),
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
    )

    // Interactive controls respect insets
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.Bottom,
    ) {
        FilledTonalButton(
            onClick = onAction,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AppTheme.sizing.minTouchTarget),
        ) {
            Text(stringResource(Res.string.action_continue))
        }
    }
}
```

### Form with Keyboard Handling

```kotlin
Column(
    modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .imePadding()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = AppTheme.spacing.lg),
) {
    Spacer(Modifier.height(AppTheme.spacing.xl))

    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text(stringResource(Res.string.email_label)) },
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(AppTheme.spacing.lg))

    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text(stringResource(Res.string.password_label)) },
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(AppTheme.spacing.xl))

    Button(
        onClick = onSubmit,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AppTheme.sizing.minTouchTarget),
    ) {
        Text(stringResource(Res.string.action_sign_in))
    }
}
```

Note: `imePadding()` only works on Android in Compose Multiplatform. On iOS, keyboard avoidance is handled by the UIKit hosting layer. If you need custom iOS keyboard behavior, use `expect`/`actual` to provide platform-specific handling.

### Lazy List with Edge-to-Edge

```kotlin
LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(
        start = AppTheme.spacing.lg,
        end = AppTheme.spacing.lg,
        top = AppTheme.spacing.lg,
        bottom = AppTheme.spacing.lg,
    ) + WindowInsets.navigationBars.asPaddingValues(),
    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
) {
    items(data, key = { it.id }) { item ->
        ItemCard(item = item)
    }
}
```

Using `contentPadding` with navigation bar insets ensures the last items scroll above the navigation bar while the scrolling content itself draws behind it.

## Status Bar / Navigation Bar Styling

### Transparent Bars with Contrast Protection

`enableEdgeToEdge()` sets transparent bars by default. For content that may blend with the status bar, use a scrim:

```kotlin
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge

enableEdgeToEdge(
    statusBarStyle = SystemBarStyle.auto(
        lightScrim = android.graphics.Color.TRANSPARENT,
        darkScrim = android.graphics.Color.TRANSPARENT,
    ),
    navigationBarStyle = SystemBarStyle.auto(
        lightScrim = android.graphics.Color.TRANSPARENT,
        darkScrim = android.graphics.Color.TRANSPARENT,
    ),
)
```

This is Android-specific and lives in `androidMain`. On iOS, the system manages status bar appearance based on the view controller's `preferredStatusBarStyle`.

## Common Pitfalls

### Content Behind System Bars

Without inset handling, text and buttons render behind the status bar and navigation bar, making them unreadable or untappable:

```kotlin
// WRONG -- content behind system bars
Column(modifier = Modifier.fillMaxSize()) {
    Text(stringResource(Res.string.title))
}

// RIGHT -- content respects system bars
Column(
    modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
) {
    Text(stringResource(Res.string.title))
}
```

### Double Inset Padding

```kotlin
// WRONG -- double padding from Scaffold + systemBarsPadding
Scaffold { innerPadding ->
    Column(
        modifier = Modifier
            .padding(innerPadding)
            .systemBarsPadding() // Doubled!
    ) {
        // Content shifted down twice
    }
}

// RIGHT -- use only the Scaffold's inner padding
Scaffold { innerPadding ->
    Column(modifier = Modifier.padding(innerPadding)) {
        // Correct single inset
    }
}
```

### Keyboard Overlapping Input Fields

```kotlin
// WRONG -- keyboard covers the submit button
Column(
    modifier = Modifier
        .fillMaxSize()
        .systemBarsPadding()
) {
    TextField(/* ... */)
    Button(onClick = onSubmit) { Text(stringResource(Res.string.submit)) }
}

// RIGHT -- imePadding shifts content above keyboard
Column(
    modifier = Modifier
        .fillMaxSize()
        .systemBarsPadding()
        .imePadding()
        .verticalScroll(rememberScrollState())
) {
    TextField(/* ... */)
    Button(onClick = onSubmit) { Text(stringResource(Res.string.submit)) }
}
```

## Rules

1. **Call `enableEdgeToEdge()` before `super.onCreate()`.** Calling it after may cause a flash of non-edge-to-edge content.
2. **Never apply `systemBarsPadding()` inside a Scaffold that handles insets.** Check `contentWindowInsets` first.
3. **Use `WindowInsets.safeDrawing` for cross-platform code.** It maps correctly on both Android and iOS.
4. **Handle `imePadding()` with platform awareness.** It only works on Android. Use `expect`/`actual` for custom iOS keyboard avoidance.
5. **All spacing values must use `AppTheme.spacing.*`.** Do not hardcode padding dp values for inset-adjacent content.
6. **All strings must use `stringResource()`.** Button labels, titles, and accessibility descriptions must come from Compose resources.

## Imports

```kotlin
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
```
