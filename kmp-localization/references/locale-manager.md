# Locale Manager and AppTheme

## LocaleManager

```kotlin
package {your.package}.core.localization

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class LocaleManager(private val dataStore: DataStore<Preferences>) {
    private companion object {
        val LOCALE_KEY = stringPreferencesKey("app.locale")
        const val DEFAULT_LOCALE = "en"
    }

    private val _currentLocale = MutableStateFlow(DEFAULT_LOCALE)
    val currentLocale: StateFlow<String> = _currentLocale.asStateFlow()

    suspend fun initialize() {
        val stored = dataStore.data.map { it[LOCALE_KEY] ?: DEFAULT_LOCALE }.first()
        _currentLocale.value = stored
    }

    suspend fun setLocale(languageTag: String) {
        dataStore.edit { it[LOCALE_KEY] = languageTag }
        _currentLocale.value = languageTag
    }

    fun supportedLocales(): List<SupportedLocale> = listOf(
        SupportedLocale("en", "English"),
        SupportedLocale("ar", "\u0627\u0644\u0639\u0631\u0628\u064a\u0629"),
        SupportedLocale("fr", "Fran\u00e7ais"),
        SupportedLocale("es", "Espa\u00f1ol"),
    )
}

data class SupportedLocale(val tag: String, val displayName: String)
```

## AppTheme with RTL Support

```kotlin
package {your.package}.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import {your.package}.core.localization.LocaleManager

@Composable
fun AppTheme(
    localeManager: LocaleManager,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val locale by localeManager.currentLocale.collectAsState()
    val layoutDirection = when (locale) {
        "ar", "he", "fa", "ur" -> LayoutDirection.Rtl
        else -> LayoutDirection.Ltr
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(),
            content = content,
        )
    }
}
```

On iOS, `LocalLayoutDirection` defaults to LTR regardless of system locale -- this explicit mapping is required.

## Using String Resources

```kotlin
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.app_name
import mobile.composeapp.generated.resources.greeting_user
import mobile.composeapp.generated.resources.items_count
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

// Simple string
Text(text = stringResource(Res.string.app_name))

// String with argument
Text(text = stringResource(Res.string.greeting_user, userName))

// Plural string
Text(text = pluralStringResource(Res.plurals.items_count, count, count))
```

## Feature-Scoped String Organization

Use naming prefixes (`auth_login_title`, `profile_edit_button`) or split into multiple XML files in the same `values/` directory:

```
composeResources/values/
    strings.xml              (common)
    strings_auth.xml         (auth feature)
    strings_profile.xml      (profile feature)
    plurals.xml              (all plurals)
```

The Compose resource compiler merges all XML files in the same `values` directory.

## DI Module

```kotlin
import org.koin.dsl.module
import {your.package}.core.localization.LocaleManager

fun localizationModule() = module {
    single { LocaleManager(dataStore = get()) }
}
```
