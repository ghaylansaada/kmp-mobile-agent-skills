# BuildKonfig Plugin Setup

> Always use the latest stable versions. Check the official release pages for current versions.

## 1. Version Catalog (libs.versions.toml)

```toml
[versions]
buildkonfig = "..."

[plugins]
buildkonfig = { id = "com.codingfeline.buildkonfig", version.ref = "buildkonfig" }
```

Verify compatibility with your Kotlin version at
[BuildKonfig releases](https://github.com/yshrsmz/BuildKonfig/releases).

## 2. Apply Plugin

In `composeApp/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.buildkonfig)
}
```

## 3. Flavor Property

In `gradle.properties`:

```properties
flavor=dev
```

Override from CLI with `-Pflavor=staging`.

Precedence: CLI `-P` > system properties > `~/.gradle/gradle.properties` > project `gradle.properties`.

## 4. BuildKonfig Block

Place at top level in `composeApp/build.gradle.kts` (not inside `android {}` or `kotlin {}`). Use the template at `assets/templates/buildkonfig-block.gradle.kts` for a complete copy-paste block.

Minimal example:

```kotlin
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.INT
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING

buildkonfig {
    packageName = "{your.package}"
    objectName = "BuildKonfig"
    val flavor = project.findProperty("flavor")?.toString() ?: "dev"

    defaultConfigs {
        buildConfigField(STRING, "BASE_URL", "https://dev-api.example.com/")
        buildConfigField(STRING, "ENVIRONMENT", "dev")
        buildConfigField(BOOLEAN, "ENABLE_LOGGING", "true")
        buildConfigField(INT, "API_TIMEOUT_SECONDS", "60")
    }
    defaultConfigs(flavor = "staging") {
        buildConfigField(STRING, "BASE_URL", "https://staging-api.example.com/")
        buildConfigField(STRING, "ENVIRONMENT", "staging")
        buildConfigField(BOOLEAN, "ENABLE_LOGGING", "true")
        buildConfigField(INT, "API_TIMEOUT_SECONDS", "30")
    }
    defaultConfigs(flavor = "prod") {
        buildConfigField(STRING, "BASE_URL", "https://api.example.com/")
        buildConfigField(STRING, "ENVIRONMENT", "prod")
        buildConfigField(BOOLEAN, "ENABLE_LOGGING", "false")
        buildConfigField(INT, "API_TIMEOUT_SECONDS", "15")
    }
}
```

## 5. Verify Generation

Generated file: `composeApp/build/buildkonfig/commonMain/{your/package/path}/BuildKonfig.kt`

## BuildKonfig vs Android BuildConfig

| Feature | Android BuildConfig | BuildKonfig |
|---------|-------------------|-------------|
| Available in commonMain | No | Yes |
| Field type | `const val` | `val` |
| Flavor control | `productFlavors` | `-Pflavor=` |
| Target scope | Android only | All KMP targets |
