# Setup: Networking Dependencies

> Always use the latest stable versions. Check the official release pages for current versions.

## Version Catalog (libs.versions.toml)

```toml
[libraries]
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json" }

ktor-client-core = { module = "io.ktor:ktor-client-core" }
ktor-client-contentNegotiation = { module = "io.ktor:ktor-client-content-negotiation" }
ktor-client-auth = { module = "io.ktor:ktor-client-auth" }
ktor-client-logging = { module = "io.ktor:ktor-client-logging" }
ktor-serialization-kotlinxJson = { module = "io.ktor:ktor-serialization-kotlinx-json" }
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp" }
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin" }
ktor-client-mock = { module = "io.ktor:ktor-client-mock" }

ktorfit-ksp = { module = "de.jensklingenberg.ktorfit:ktorfit-ksp" }
ktorfit-lib = { module = "de.jensklingenberg.ktorfit:ktorfit-lib" }
ktorfit-converters-response = { module = "de.jensklingenberg.ktorfit:ktorfit-converters-response" }

[plugins]
kotlinSerialization = { id = "org.jetbrains.kotlin.plugin.serialization" }
ksp = { id = "com.google.devtools.ksp" }
ktorfitGradle = { id = "de.jensklingenberg.ktorfit" }
```

## build.gradle.kts (composeApp)

```kotlin
plugins {
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktorfitGradle)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinxJson)
            implementation(libs.ktorfit.lib)
            implementation(libs.ktorfit.converters.response)
        }
        androidMain.dependencies { implementation(libs.ktor.client.okhttp) }
        iosMain.dependencies { implementation(libs.ktor.client.darwin) }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

ktorfit { compilerPluginVersion.set("LATEST_STABLE") }
```

## KSP Processor Registration

Per-target registration is required -- global `ksp()` does not work:

```kotlin
dependencies {
    add("kspCommonMainMetadata", libs.ktorfit.ksp)
    add("kspAndroid", libs.ktorfit.ksp)
    add("kspIosArm64", libs.ktorfit.ksp)
    add("kspIosSimulatorArm64", libs.ktorfit.ksp)
}
```

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| `Unresolved reference: create` on Ktorfit service | KSP not registered for target | Add `add("kspAndroid", ...)` etc. per target |
| `No instance for HttpClientEngineFactory` | Platform module not loaded | Ensure platform module loads before ktorfitModule |
| Conflicting Ktor versions | Multiple Ktor BOMs or mixed versions | Pin single Ktor version across all artifacts in version catalog |
| `ClassNotFoundException` for Ktorfit generated class | KSP processor missing for target | Verify all KSP target entries match active Kotlin targets |
