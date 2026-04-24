# Setup: Navigation 3 Dependencies

> Always use the latest stable versions. Check the official release pages for current versions.

## Version Catalog (libs.versions.toml)

```toml
[versions]
navigation3 = "..."
lifecycle = "..."
kotlinxSerializationJson = "..."
composeMaterial3Adaptive = "..."

[libraries]
navigation3-ui = { module = "org.jetbrains.androidx.navigation3:navigation3-ui", version.ref = "navigation3" }
lifecycle-viewmodel-navigation3 = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-navigation3", version.ref = "lifecycle" }
compose-material3-adaptive-navigation3 = { module = "org.jetbrains.compose.material3.adaptive:adaptive-navigation3", version.ref = "composeMaterial3Adaptive" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerializationJson" }
```

The `org.jetbrains.androidx.navigation3` artifacts are the KMP-compatible Navigation 3 libraries from JetBrains. They work across Android, iOS, Desktop, and Web targets. Do NOT use the Android-only `androidx.navigation3` artifacts in shared code.

## Shared Module build.gradle.kts

Add to `commonMain.dependencies`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.navigation3.ui)
            implementation(libs.lifecycle.viewmodel.navigation3)
            implementation(libs.kotlinx.serialization.json)

            // For adaptive multi-destination layouts (list-detail on tablets):
            // implementation(libs.compose.material3.adaptive.navigation3)
        }
    }
}
```

## Required Plugins (already present)

```kotlin
plugins {
    alias(libs.plugins.kotlinSerialization)  // required for @Serializable NavKey
    alias(libs.plugins.composeCompiler)      // required for @Composable entries
}
```

## Polymorphic Serialization for KMP (non-JVM targets)

iOS, Web, and other non-JVM targets cannot use reflection-based serialization. You must register a `SerializersModule` with all NavKey subtypes:

```kotlin
package {your.package}.navigation

import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import {your.package}.navigation.routes.*

val navKeySerializersModule = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(LoginKey::class)
        subclass(RegisterKey::class)
        subclass(ForgotPasswordKey::class)
        subclass(HomeKey::class)
        subclass(DetailKey::class)
        subclass(SearchKey::class)
        subclass(ProfileKey::class)
        subclass(SettingsKey::class)
    }
}
```

Pass this module when creating the `SavedStateConfiguration`:

```kotlin
import androidx.navigation3.SavedStateConfiguration
import kotlinx.serialization.json.Json

val navJson = Json {
    serializersModule = navKeySerializersModule
}

val savedStateConfig = SavedStateConfiguration(json = navJson)
```

## Android Manifest for Deep Links

If using deep links, add intent filters to `AndroidManifest.xml`:

```xml
<activity
    android:name=".MainActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="myapp"
            android:host="template" />
    </intent-filter>
</activity>
```

## Verification

After syncing, verify the Navigation 3 artifacts are on the classpath. Expected dependencies:

```
+--- org.jetbrains.androidx.navigation3:navigation3-ui
+--- org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-navigation3
```

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| `Unresolved reference: NavDisplay` | Wrong artifact or missing Nav3 dependency | Use `org.jetbrains.androidx.navigation3:navigation3-ui` |
| `Unresolved reference: NavKey` | Missing Nav3 common dependency | Nav3-ui transitively includes navigation3-common |
| `SerializationException` on iOS | Missing polymorphic serialization | Register all NavKey subtypes in `SerializersModule` |
| `@Serializable route not found` | Missing serialization plugin | Ensure `kotlinSerialization` plugin is applied |
| `No activity to handle deep link` | Missing intent filter | Add `<intent-filter>` with matching scheme/host in AndroidManifest |
| `ViewModel not scoped to entry` | Missing viewmodel-navigation3 decorator | Add `rememberViewModelStoreNavEntryDecorator()` to NavDisplay decorators |
