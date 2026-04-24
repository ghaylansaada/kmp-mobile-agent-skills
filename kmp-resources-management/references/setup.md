# Setup: Resources Management Dependencies

> **Note:** Version numbers are intentionally omitted. Always use the latest stable release from the official documentation.

## Version Catalog

```toml
[libraries]
compose-components-resources = { module = "org.jetbrains.compose.components:components-resources", version.ref = "composeMultiplatform" }
```

## build.gradle.kts Dependency

```kotlin
sourceSets {
    commonMain.dependencies {
        implementation(libs.compose.components.resources)
    }
}
```

The Compose Multiplatform plugin must be applied for resource processing:

```kotlin
plugins {
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}
```

## Directory Structure

### Shared Compose Resources

```
composeApp/src/commonMain/
  composeResources/
    drawable/
      compose-multiplatform.xml    # Shared vector drawable
```

The plugin processes these at build time and generates a type-safe `Res` class.

### Android Platform Resources

```
composeApp/src/androidMain/
  res/
    drawable/
      ic_launcher_background.xml
    drawable-v24/
      ic_launcher_foreground.xml
    mipmap-anydpi-v26/
      ic_launcher.xml
      ic_launcher_round.xml
    values/
      strings.xml
```

### Naming Rules

- Lowercase letters, digits, underscores
- Hyphens allowed in composeResources (converted to underscores in generated code)
- No spaces or uppercase
- Subdirectories must match resource types: `drawable`, `string`, `font`, etc.

## Generated Res Class

After adding resources and running a Gradle sync, the plugin generates:

- `Res` object in `mobile.composeapp.generated.resources` package
- Type-safe accessors like `Res.drawable.compose_multiplatform`
- Resource collectors per target (androidMain, iosArm64Main, iosSimulatorArm64Main)

Generated code lives in:
```
composeApp/build/generated/compose/resourceGenerator/kotlin/
  commonResClass/                 # Res.kt
  commonMainResourceAccessors/    # Shared accessors
  androidMainResourceCollectors/  # Android resource collector
  iosArm64MainResourceCollectors/ # iOS ARM64 resource collector
```

## Android Namespace Configuration

```kotlin
extensions.configure<ApplicationExtension> {
    namespace = "{your.package}"
    compileSdk = 36
}
```
