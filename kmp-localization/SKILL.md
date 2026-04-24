---
name: kmp-localization
description: >
  Localization for Compose Multiplatform using composeResources -- string resources with format
  arguments, plurals via pluralStringResource, runtime locale switching with DataStore persistence,
  RTL layout direction, feature-scoped string organization, and server-vs-local error message
  resolution. Activate when adding user-facing strings, supporting multiple languages, implementing
  runtime locale switching, handling plurals, organizing strings by feature, or resolving
  server error messages to localized user-facing text.
compatibility: >
  KMP with Compose Multiplatform resources. Requires DataStore for locale persistence.
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---

# KMP Localization

## When to use

- Adding localized string resources for cross-platform UI text
- Supporting multiple languages with locale-qualified value directories
- Implementing runtime locale switching without app restart
- Handling plurals with quantity-dependent text
- Connecting server-provided error messages with local fallback strings
- Adding RTL (right-to-left) layout support for Arabic, Hebrew, etc.

## Depends on

- **kmp-resources-management** -- composeResources directory structure and Res class generation
- **kmp-compose-ui** -- MaterialTheme, CompositionLocalProvider, LocalLayoutDirection
- **kmp-dependency-injection** -- Koin module for LocaleManager

## Workflow

1. Create resource directories and string XML files --> [setup.md](references/setup.md)
2. Implement LocaleManager and AppTheme with RTL --> [locale-manager.md](references/locale-manager.md)
3. Add ErrorMessageResolver for server-vs-local errors --> [error-resolution.md](references/error-resolution.md)
4. Wire into Koin and App.kt --> [integration.md](references/integration.md)

## Gotchas

1. **`stringResource()` ignores iOS per-app language settings.** Compose Multiplatform uses the system-wide locale. To honor per-app language, read `Bundle.main.preferredLocalizations` and call `configureLocale()` at startup. Without this, users who set a per-app language in iOS Settings see the wrong language.
2. **Plurals use ICU rules, not simple one/other.** Arabic has 6 plural forms (zero, one, two, few, many, other). If you only define `one` and `other`, Arabic falls back to `other` for most counts, producing grammatically incorrect text for quantities like 2, 3-10, and 11-99.
3. **RTL layout is not auto-detected on iOS.** `LocalLayoutDirection` defaults to LTR regardless of system locale on iOS. You must explicitly set it via `CompositionLocalProvider` in `AppTheme`. Without this, Arabic and Hebrew UIs render left-to-right.
4. **Use positional arguments (`%1$s`) not sequential (`%s`).** Positional args let translators reorder arguments for grammar differences. Sequential args break when translators need a different word order, producing garbled strings at runtime.
5. **Dynamic locale changes require Activity recreate on Android.** `configureLocale()` triggers recomposition for Compose UI, but Android `Context`-based components (date pickers, system keyboard language) need `AppCompatDelegate.setApplicationLocales()` to reflect the new locale.
6. **Feature-scoped strings must be in the same `values/` directory.** The Compose resource compiler does not support subdirectories. Use naming prefixes (`auth_login_title`) or multiple XML files (`strings_auth.xml`) in the flat `values/` directory. Subdirectories are silently ignored.
7. **`stringResource()` is @Composable-only.** For ViewModels and data layer code, pass `Res.string.key` as `StringResource` and resolve later in a @Composable context. Calling `stringResource()` outside composition throws `IllegalStateException`.
8. **Missing locale directory keys fall back silently.** If `values-ar/strings.xml` omits a key defined in `values/strings.xml`, the default English string is shown with no compile-time warning. Untranslated strings appear as mixed-language UI at runtime.
9. **Locale tag format must be BCP 47 subtag, not Android qualifier.** Use `values-ar` not `values-ar-rSA`. The Compose resource compiler uses BCP 47, not the Android `r`-prefixed region format. Wrong format causes the entire locale directory to be ignored.

## Assets

| Path | Load when... |
|------|-------------|
| [references/setup.md](references/setup.md) | Creating resource directories and string files |
| [references/locale-manager.md](references/locale-manager.md) | Implementing runtime locale switching and RTL |
| [references/error-resolution.md](references/error-resolution.md) | Server-vs-local error message display |
| [references/integration.md](references/integration.md) | Wiring into Koin, App.kt, Accept-Language |
| [assets/snippets/localization-patterns.kt](assets/snippets/localization-patterns.kt) | UI patterns: dialogs, snackbars, onboarding with StringResource |
| [assets/templates/strings-feature.xml.template](assets/templates/strings-feature.xml.template) | Feature-scoped string XML template with naming conventions |

## Validation

### A. Localization correctness
- [ ] All user-facing strings externalized in `composeResources/values/strings.xml` (no hardcoded strings in UI code)
- [ ] Plural forms handled correctly with `pluralStringResource()` and `<plurals>` XML
- [ ] Arabic plurals define all 6 forms (zero, one, two, few, many, other)
- [ ] String formatting uses positional arguments (`%1$s`, `%1$d`) not sequential (`%s`, `%d`)
- [ ] RTL layout direction set explicitly via `CompositionLocalProvider(LocalLayoutDirection)`
- [ ] Locale-specific resources in correct `values-{tag}/` directories using BCP 47 subtags
- [ ] No `android.*` imports in commonMain for string access
- [ ] `stringResource()` only called from @Composable context; non-composable code uses `StringResource`
- [ ] Locale tag persistence uses DataStore, not in-memory-only state
- [ ] All locale directories contain translations for all keys (no silent fallback gaps)

### B. Security
- [ ] Server error messages sanitized before display (no stack traces or internal details shown to users)
- [ ] No secrets, tokens, or credentials in string resources or error messages
- [ ] `ResolvedError.ServerProvided` messages are treated as untrusted input

### C. Performance
- [ ] `LocaleManager.initialize()` called once at startup, not on every recomposition
- [ ] `supportedLocales()` does not allocate a new list on every call in a hot path
- [ ] No unnecessary recomposition from locale StateFlow collection

### D. Integration
- [ ] `kmp-resources-management` provides composeResources directory and Res class generation
- [ ] `kmp-compose-ui` provides MaterialTheme, CompositionLocalProvider, LocalLayoutDirection
- [ ] `kmp-dependency-injection` provides Koin module for LocaleManager with DataStore injection
- [ ] `kmp-networking` provides `ApiResult.Error` types consumed by `ErrorMessageResolver`
- [ ] `kmp-networking` consumes `Accept-Language` header from `LocaleManager.currentLocale`
- [ ] Android launcher name localized via separate `androidMain/res/values-{tag}/strings.xml`
