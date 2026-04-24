# Integration: Resources Management

## Upstream Dependencies

### Project Structure

Resource directories follow the KMP source set convention:
- `commonMain/composeResources/` for shared resources
- `androidMain/res/` for Android-specific resources

### Gradle Configuration

The Compose Multiplatform plugin (`org.jetbrains.compose`) processes the `composeResources/` directory and generates the `Res` class. The plugin must be applied in `build.gradle.kts`.

## Connected Skills

### Compose Multiplatform

Screen composables consume shared resources via the generated `Res` class using `painterResource()` and `stringResource()`. See [resource-access-patterns.kt](../assets/snippets/resource-access-patterns.kt) for complete examples.

### Image Loading (Coil)

Local resources use `painterResource()` (synchronous, no caching). Remote images use `AppAsyncImage` (async, Coil caching). The two systems are complementary.

## Build Pipeline

### Compose Resource Processing

```
1. Developer adds file to commonMain/composeResources/drawable/
2. Gradle sync triggers compose resource generator
3. Plugin generates Res.kt, accessors, per-platform collectors
4. Generated code placed in build/generated/compose/resourceGenerator/kotlin/
5. Composables import from mobile.composeapp.generated.resources
```

### Android Resource Processing

```
1. Developer adds file to androidMain/res/drawable/ (or values/, mipmap/)
2. AGP processes resources during Android build
3. R class generated in {your.package} package
4. Android-only code imports {your.package}.R
```

## AndroidManifest Integration

The manifest references (see [implementation.md](implementation.md) for the XML) resolve to files in `androidMain/res/`:
- `@mipmap/ic_launcher` --> `res/mipmap-anydpi-v26/ic_launcher.xml`
- `@string/app_name` --> `res/values/strings.xml`
- `@android:style/Theme.Material.Light.NoActionBar` --> system theme (no local file)

## Resource Naming Conventions

### Shared Resources (composeResources)

| Type | Directory | Naming | Example |
|---|---|---|---|
| Drawables | `composeResources/drawable/` | lowercase, underscores or hyphens | `compose-multiplatform.xml` |
| Strings | `composeResources/values/` | standard Android XML format | `strings.xml` |
| Fonts | `composeResources/font/` | lowercase, underscores | `roboto_regular.ttf` |

### Android Resources (res)

| Type | Directory | Naming | Example |
|---|---|---|---|
| Drawables | `res/drawable/` | lowercase, underscores only | `ic_launcher_background.xml` |
| Mipmaps | `res/mipmap-anydpi-v26/` | launcher icon convention | `ic_launcher.xml` |
| Strings | `res/values/` | standard Android XML | `strings.xml` |
