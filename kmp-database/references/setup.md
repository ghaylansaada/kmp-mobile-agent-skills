# Room Database Setup

> Always use the latest stable versions. Check the official release pages for current versions.

## Version Catalog (libs.versions.toml)

```toml
[versions]
room = "..."
sqlite = "..."

[libraries]
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
room-paging = { module = "androidx.room:room-paging", version.ref = "room" }
sqlite-bundled = { module = "androidx.sqlite:sqlite-bundled", version.ref = "sqlite" }

[plugins]
ksp = { id = "com.google.devtools.ksp" }
```

## build.gradle.kts (composeApp)

```kotlin
plugins {
    alias(libs.plugins.ksp)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            linkerOpts.add("-lsqlite3")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.room.runtime)
            implementation(libs.room.paging)
            implementation(libs.sqlite.bundled)
        }
    }
}
```

## KSP Per-Target Configuration

Room requires per-target KSP. Do NOT use generic `ksp(...)`.

```kotlin
dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}
```

## Schema Export

```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/roomSchemas")
    arg("room.incremental", "true")
}
```

## ProGuard

```proguard
-keep class * extends androidx.room.RoomDatabase { <init>(...); }
```

## Verification

After adding dependencies, sync and build both Android and iOS targets to confirm Room KSP generates code successfully.
