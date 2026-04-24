# Build Files Reference -- KMP Project Setup

## Root build.gradle.kts

File: `build.gradle.kts` (project root)

The root build script declares every plugin used anywhere in the project with `apply false`.
This loads the plugin classes into the build classpath exactly once.

```kotlin
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
}
```

Plugins that are only applied at the module level (e.g., `kotlinSerialization`, `ksp`,
`ktorfitGradle`) do not need `apply false` in the root script -- Gradle resolves them
from the version catalog when the module applies them via `alias(libs.plugins.*)`.

---

## settings.gradle.kts

File: `settings.gradle.kts` (project root)

```kotlin
rootProject.name = "mobile"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "..."
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":composeApp")
```

Always use the latest stable version for the foojay-resolver-convention plugin.

---

## composeApp/build.gradle.kts

The full module build script is in [project-skeleton.gradle.kts.template](../assets/templates/project-skeleton.gradle.kts.template).
Key configuration points:

- **Kotlin targets** -- `androidTarget` with JVM 25, `iosArm64()` and `iosSimulatorArm64()` with static framework
- **iOS framework** -- `baseName = "ComposeApp"`, `isStatic = true`, `linkerOpts("-lsqlite3")` for Room/SQLite
- **Source sets** -- `commonMain`, `commonTest`, `androidMain`, `iosMain` with platform-specific dependencies
- **Compiler flag** -- `-Xexpect-actual-classes` enables expect/actual class syntax
- **Android config** -- `ApplicationExtension` with namespace, SDK versions, ProGuard, Java 25 compatibility
- **KSP** -- Room schema location, incremental processing, per-target processor registration
- **Ktorfit** -- `ktorfit { compilerPluginVersion.set("...") }` pins the compiler plugin version

---

## ProGuard Rules

File: `composeApp/proguard-rules.pro`

```proguard
-keep class * extends androidx.room.RoomDatabase { <init>(...); }
```

Room KMP generates database implementation classes instantiated via reflection.
Without this keep rule, R8 strips the constructor, causing a runtime crash.
