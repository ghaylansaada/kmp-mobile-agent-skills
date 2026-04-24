# Dark Theme Implementation

All colors must come from `MaterialTheme.colorScheme.*`. Never use `Color.White`, `Color.Black`, or any hardcoded `Color(0xFF...)` value. See [design-tokens.md](design-tokens.md) for the full token system.

## Detecting System Theme

```kotlin
import androidx.compose.foundation.isSystemInDarkTheme

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    // ...
}
```

`isSystemInDarkTheme()` reads the system-level dark mode setting on both Android and iOS. It recomposes automatically when the user toggles system dark mode.

## Color Scheme Definitions

Define light and dark color schemes in `commonMain`. All color values are centralized here -- composables never reference colors directly.

```kotlin
package {your.package}.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
)
```

Note: Color literals belong only in these scheme definitions. Composables reference them via `MaterialTheme.colorScheme.*`.

## AppTheme Composable with Dark Mode Support

```kotlin
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    spacing: AppSpacing = AppSpacing(),
    sizing: AppSizing = AppSizing(),
    corners: AppCorners = AppCorners(),
    elevation: AppElevation = AppElevation(),
    motion: AppMotion = AppMotion(),
    content: @Composable () -> Unit,
) {
    val colorScheme = resolveColorScheme(darkTheme = darkTheme, dynamicColor = dynamicColor)

    CompositionLocalProvider(
        LocalAppSpacing provides spacing,
        LocalAppSizing provides sizing,
        LocalAppCorners provides corners,
        LocalAppElevation provides elevation,
        LocalAppMotion provides motion,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
        ) {
            content()
        }
    }
}
```

## Dynamic Color Support (Android 12+)

`dynamicColorScheme()` is Android-only. Gate it behind an `expect`/`actual` function so iOS uses the static scheme:

```kotlin
// commonMain
@Composable
expect fun resolveColorScheme(darkTheme: Boolean, dynamicColor: Boolean): ColorScheme

// androidMain
@Composable
actual fun resolveColorScheme(darkTheme: Boolean, dynamicColor: Boolean): ColorScheme {
    return when {
        dynamicColor && Build.VERSION.SDK_INT >= 31 -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
}

// iosMain
@Composable
actual fun resolveColorScheme(darkTheme: Boolean, dynamicColor: Boolean): ColorScheme {
    // Dynamic color is not available on iOS -- always use static schemes
    return if (darkTheme) DarkColors else LightColors
}
```

## Runtime Theme Toggle via DataStore

Store the user's theme preference in DataStore and expose it as a `StateFlow`:

```kotlin
enum class ThemePreference { SYSTEM, LIGHT, DARK }

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    private val themeKey = stringPreferencesKey("theme_preference")

    val themePreference: Flow<ThemePreference> = dataStore.data.map { prefs ->
        when (prefs[themeKey]) {
            "light" -> ThemePreference.LIGHT
            "dark" -> ThemePreference.DARK
            else -> ThemePreference.SYSTEM
        }
    }

    suspend fun setThemePreference(preference: ThemePreference) {
        dataStore.edit { prefs ->
            prefs[themeKey] = when (preference) {
                ThemePreference.SYSTEM -> "system"
                ThemePreference.LIGHT -> "light"
                ThemePreference.DARK -> "dark"
            }
        }
    }
}
```

Resolve the preference to a `darkTheme` boolean at the `App` composable level:

```kotlin
@Composable
fun App() {
    val settingsRepository: SettingsRepository = koinInject()
    val themePreference by settingsRepository.themePreference.collectAsState(initial = ThemePreference.SYSTEM)

    val darkTheme = when (themePreference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }

    AppTheme(darkTheme = darkTheme) {
        // Navigation host
    }
}
```

## Previewing Dark Theme

Use `uiMode` in `@Preview` to render dark theme previews on Android. On Compose Multiplatform, pass `darkTheme = true` explicitly:

```kotlin
@Preview
@Composable
private fun SettingsScreenLightPreview() {
    AppTheme(darkTheme = false) {
        SettingsScreenContent(state = SettingsUiState(), onToggleTheme = {})
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsScreenDarkPreview() {
    AppTheme(darkTheme = true) {
        SettingsScreenContent(state = SettingsUiState(), onToggleTheme = {})
    }
}
```

Always create both light and dark previews for screens that use color-sensitive layouts.

## Common Pitfall: Hardcoded Colors

```kotlin
// WRONG -- not adaptive, looks bad in dark theme
Text("Title", color = Color.Black)
Box(modifier = Modifier.background(Color.White))
Divider(color = Color.LightGray)

// RIGHT -- adapts to light/dark automatically
Text("Title", color = MaterialTheme.colorScheme.onSurface)
Box(modifier = Modifier.background(MaterialTheme.colorScheme.surface))
HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
```

`Color.White` and `Color.Black` are not theme-aware. They produce invisible text on matching backgrounds when the theme switches. Always use semantic color roles from `MaterialTheme.colorScheme`.

## File Placement

```
commonMain/
  kotlin/{your.package}/
    ui/
      theme/
        AppTheme.kt          <- AppTheme composable, CompositionLocalProvider wrapper
        Color.kt             <- LightColors, DarkColors definitions
        ResolveColorScheme.kt <- expect fun resolveColorScheme()
androidMain/
  kotlin/{your.package}/
    ui/
      theme/
        ResolveColorScheme.android.kt <- actual fun with dynamicColorScheme()
iosMain/
  kotlin/{your.package}/
    ui/
      theme/
        ResolveColorScheme.ios.kt     <- actual fun with static fallback
```

## Rules

1. All colors in composables must come from `MaterialTheme.colorScheme.*`. The only place `Color(0xFF...)` literals appear is in the `lightColorScheme()` and `darkColorScheme()` definitions.
2. Never use `Color.White`, `Color.Black`, `Color.Gray`, or `Color.Red` in composable code. These are not theme-aware and produce invisible content when the theme switches.
3. Gate `dynamicColorScheme()` behind `expect`/`actual`. Calling it on iOS crashes at runtime.
4. Always provide both light and dark `@Preview` variants for screens with color-sensitive layouts.
5. Store theme preference as a string enum in DataStore. Resolve it to a `darkTheme: Boolean` at the `App` composable level, not per-screen.
6. Pass `darkTheme` as a parameter to `AppTheme`, not as a CompositionLocal. This keeps the theme decision explicit and testable.
