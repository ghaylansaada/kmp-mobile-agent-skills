---
name: kmp-resources-management
description: >
  Manage compose resources in KMP -- shared drawables, strings, fonts in composeResources/,
  Android platform resources (launcher icons, manifest strings), generated Res class for
  type-safe access, and resource directory conventions. Activate when adding images, strings,
  fonts, or other resources to a KMP project, organizing composeResources/ directories,
  accessing resources via the generated Res class, or setting up density-qualified drawables.
compatibility: >
  KMP with Compose Multiplatform resources. Requires compose.resources Gradle plugin.
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---

# KMP Resources Management

## When to use

- Adding new shared drawable resources accessible from all platforms
- Adding Android-specific resources (launcher icons, strings, themes)
- Referencing shared resources in composables via the generated Res class
- Configuring adaptive icons for Android
- Understanding the resource directory structure and naming conventions
- Adding shared string or font resources for cross-platform use

## Depends on

- Compose Multiplatform plugin (`org.jetbrains.compose`) for resource processing
- compose-components-resources library in commonMain
- **kmp-compose-ui** -- for composables consuming resources via painterResource/stringResource
- **kmp-localization** -- for localized string resources in value-qualified directories

## Workflow

1. Verify compose-components-resources dependency is in commonMain --> [setup.md](references/setup.md)
2. Place shared drawables in `commonMain/composeResources/drawable/` --> [implementation.md](references/implementation.md)
3. Configure Android platform resources (launcher icons, strings) --> [implementation.md](references/implementation.md)
4. Access resources in composables via generated Res class --> [implementation.md](references/implementation.md)
5. Connect resources to Compose UI and platform entry points --> [integration.md](references/integration.md)

## Gotchas

1. **`Res.*` accessors are generated at compile time and require a Gradle sync.** After adding or renaming any file in `composeResources/`, you must sync Gradle in the IDE. Until then, `Unresolved reference: Res` errors will appear. This is not a code error -- it is a build pipeline timing issue.

2. **Resource qualifiers are resolved differently on iOS.** On Android, resource qualifiers (e.g., `drawable-night`, `values-ar`) are resolved by the system based on configuration. On iOS, the Compose resource system resolves qualifiers using its own logic, which may not match the system dark mode or locale settings. Test qualifier-based resource resolution on iOS explicitly.

3. **Font loading is async on iOS.** `Font(Res.font.my_font)` loads synchronously on Android but asynchronously on iOS. This means text rendered with a custom font may briefly flash with the fallback system font on iOS. Use `remember` with a loading state or preload fonts during app startup to avoid the flash.

4. **Large assets in commonMain double the app size.** Resources in `commonMain/composeResources/` are bundled into both the Android APK and the iOS framework. A 10MB image in commonMain adds 10MB to both platforms. For platform-specific large assets (video, high-res images), use platform-specific resource directories (`androidMain/res/`, iOS asset catalogs) instead.

5. **Resource file names must follow strict naming rules.** Names must be lowercase with underscores only. Hyphens are technically allowed in `composeResources/` but are converted to underscores in the generated Res accessor (e.g., `compose-multiplatform.xml` becomes `Res.drawable.compose_multiplatform`). Spaces, uppercase letters, and special characters will cause build failures.

6. **Vector drawable XML in composeResources must use Android vector format, not SVG.** The Compose resource compiler only understands `<vector>` XML with `android:pathData`, `android:fillColor`, etc. Raw SVG files will be silently ignored or cause build errors. Convert SVGs to Android vector format using Android Studio's Vector Asset tool before placing them in composeResources.

7. **Android mipmap resources are not accessible from shared code.** Launcher icons in `androidMain/res/mipmap-anydpi-v26/` are only accessible from the Android manifest and Android-specific code via the `R` class. They cannot be referenced from commonMain composables. For shared icons, place them in `commonMain/composeResources/drawable/`.

8. **`painterResource()` is `@Composable`-only.** You cannot call `painterResource()` or `stringResource()` outside a `@Composable` context. For ViewModels or data layer code, pass `Res.drawable.name` as a `DrawableResource` or `Res.string.name` as a `StringResource` and resolve it later in a composable. Calling these functions outside composition throws `IllegalStateException`.

9. **Density-qualified drawables require correct directory suffixes.** For raster images (PNG, WebP), use `drawable-mdpi/`, `drawable-hdpi/`, `drawable-xhdpi/`, etc. Placing all densities in the flat `drawable/` directory means every platform uses the same asset, which is either too large for low-density screens or too blurry for high-density screens.

## Assets

| Path | Load when... |
|------|-------------|
| [references/setup.md](references/setup.md) | Configuring compose-components-resources dependency and directory structure |
| [references/implementation.md](references/implementation.md) | Adding shared drawables, Android resources, or new resource types |
| [references/integration.md](references/integration.md) | Connecting resources to Compose UI and platform entry points |
| [assets/templates/vector-drawable.xml.template](assets/templates/vector-drawable.xml.template) | Creating a new shared vector drawable XML |
| [assets/snippets/resource-access-patterns.kt](assets/snippets/resource-access-patterns.kt) | Accessing shared drawables, strings, and Android-only resources |

## Validation

### A. Build and Compilation

- [ ] All imports in code blocks are valid and resolve against declared dependencies
- [ ] No deprecated API usage (`compose.desktop.common` resources, `ResourceItem`)

### B. Resources Correctness

- [ ] Resource files placed in correct directories (`composeResources/drawable/`, `composeResources/values/`, etc.)
- [ ] Generated `Res` class used for all resource access (not raw paths or hardcoded strings)
- [ ] Density-qualifier directories used for raster images (`drawable-mdpi/`, `drawable-hdpi/`, etc.)
- [ ] Locale-qualifier directories use BCP 47 subtags (`values-ar/`, not `values-ar-rSA/`)
- [ ] No hardcoded resource paths in Kotlin code (e.g., no `"drawable/icon.xml"` strings)
- [ ] Vector drawables use Android vector format (`<vector>` with `android:pathData`), not SVG
- [ ] File names are lowercase with underscores or hyphens only (no spaces, no uppercase)
- [ ] Android-only resources (mipmaps, manifest strings) in `androidMain/res/`, not `commonMain/composeResources/`
- [ ] No `android.*` imports in commonMain for resource access
- [ ] `painterResource()` and `stringResource()` called only from `@Composable` context

### C. Security

- [ ] No secrets, tokens, or credentials embedded in resource files or string values
- [ ] No PII or real user data in sample/test resource files

### D. Performance

- [ ] Large platform-specific assets (video, high-res images) in platform source sets, not commonMain
- [ ] Vector drawables preferred over raster images where possible to reduce bundle size
- [ ] No duplicate assets across commonMain and platform source sets

### E. Integration

- [ ] **kmp-compose-ui** consumes shared resources via `painterResource()` and `stringResource()`
- [ ] **kmp-localization** depends on composeResources directory and Res class for localized strings
- [ ] **kmp-image-loader** uses `painterResource()` for local resources, Coil for remote images
- [ ] Compose Multiplatform plugin applied in build.gradle.kts for resource processing
- [ ] Android namespace configured for R class generation
