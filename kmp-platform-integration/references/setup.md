# Android Build Configuration and Project Layout

> **Note:** Version numbers are intentionally omitted. Always use the latest stable release from the official documentation.

## Version Catalog (gradle/libs.versions.toml)

```toml
[libraries]
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "androidx-activity" }
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
work-runtime-ktx = { module = "androidx.work:work-runtime-ktx", version.ref = "work" }
sqlite-bundled = { module = "androidx.sqlite:sqlite-bundled", version.ref = "sqlite" }
```

## Module Gradle (composeApp/build.gradle.kts)

```kotlin
androidMain.dependencies {
    implementation(libs.compose.uiToolingPreview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.paging.runtime)
    implementation(libs.work.runtime.ktx)
}
```

### Android Block

```kotlin
extensions.configure<ApplicationExtension> {
    namespace = "{your.package}"
    compileSdk = 36

    defaultConfig {
        applicationId = "{your.package}"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}
```

## AndroidManifest.xml

Located at `composeApp/src/androidMain/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
            android:allowBackup="true"
            android:enableOnBackInvokedCallback="true"
            android:icon="@mipmap/ic_launcher"
            android:label="@string/app_name"
            android:roundIcon="@mipmap/ic_launcher_round"
            android:supportsRtl="true"
            android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity
                android:exported="true"
                android:name=".MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>
    </application>
</manifest>
```

Compose handles all theming via Material 3, so the manifest theme is a minimal no-action-bar fallback. The `enableOnBackInvokedCallback` flag enables predictive back gesture animations on Android 13+ and is mandatory on Android 16 (see [predictive-back.md](predictive-back.md)). INTERNET permission is added transitively by Ktor/OkHttp.

## ProGuard Rules

Located at `composeApp/proguard-rules.pro`:

```proguard
# Room KMP -- keep generated database implementations (mandatory)
-keep class * extends androidx.room.RoomDatabase { <init>(...); }

# Kotlinx Serialization (add when using @Serializable models in release)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
```

Without the Room rule, R8 strips generated constructors causing `RuntimeException: Cannot find implementation for AppDatabase` in release builds only.

## Directory Layout

```
composeApp/src/androidMain/
    AndroidManifest.xml
    kotlin/{your/package}/
        MainActivity.kt
        di/PlatformModule.android.kt
        core/
            platform/Platform.android.kt
            platform/PlatformContext.android.kt
            database/Database.android.kt
            datastore/DataStore.android.kt
            transfer/io/FileReader.kt
            transfer/io/FileWriter.kt
            transfer/io/AndroidFileReaderFactory.kt
            transfer/io/AndroidFileWriterFactory.kt
    res/
        values/strings.xml
        mipmap-anydpi-v26/ic_launcher.xml
        drawable-v24/ic_launcher_foreground.xml
```
