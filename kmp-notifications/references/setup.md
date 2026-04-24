# Setup: Notifications Dependencies

> **Note:** Version numbers are intentionally omitted. Always use the latest stable release from the official documentation.

## Version Catalog (libs.versions.toml)

Add Firebase Messaging for Android push notifications:

```toml
[versions]
# Always use latest stable versions — check official release pages
firebaseMessaging = "..."
googleServices = "..."

[libraries]
firebase-messaging = { module = "com.google.firebase:firebase-messaging-ktx", version.ref = "firebaseMessaging" }

[plugins]
google-services = { id = "com.google.gms.google-services", version.ref = "googleServices" }
```

## Shared Module build.gradle.kts

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            // No additional commonMain deps -- notification APIs are platform-native
            // kotlinx-serialization-json is already in the template for payload model
        }
        androidMain.dependencies {
            implementation(libs.firebase.messaging)
        }
    }
}
```

## Android: build.gradle.kts (app-level)

Apply the Google Services plugin:

```kotlin
plugins {
    // ... existing plugins
    alias(libs.plugins.google.services)
}
```

## Android: Root build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.google.services) apply false
}
```

## Android: google-services.json

Place `google-services.json` from Firebase Console into:

```
composeApp/google-services.json
```

## Android: AndroidManifest.xml Additions

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Notification permission (Android 13+) -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application>
        <!-- FCM Service -->
        <service
            android:name=".core.notifications.AppFirebaseMessagingService"
            android:exported="false">
            <intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT" />
            </intent-filter>
        </service>

        <!-- Default notification channel metadata -->
        <meta-data
            android:name="com.google.firebase.messaging.default_notification_channel_id"
            android:value="default_channel" />

        <!-- Deep link intent filter -->
        <activity android:name=".MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="myapp" android:host="screen" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

## iOS: Info.plist

Enable remote notifications background mode:

```xml
<key>UIBackgroundModes</key>
<array>
    <string>remote-notification</string>
</array>
```

## iOS: Xcode Capabilities

1. Open `iosApp.xcodeproj` in Xcode.
2. Select the target, go to **Signing & Capabilities**.
3. Add **Push Notifications** capability.
4. Add **Background Modes** and check **Remote notifications**.

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| `MissingPluginException: google-services` | Plugin not applied | Add `google-services` to root and app build.gradle.kts |
| `File google-services.json is missing` | File not in composeApp/ | Download from Firebase Console and place in `composeApp/` |
| `SecurityException: POST_NOTIFICATIONS` | Missing permission on Android 13+ | Add to manifest and request at runtime via `ActivityResultLauncher` |
| iOS push not arriving | Missing APNs key in Firebase | Upload APNs auth key (.p8) in Firebase Console > Cloud Messaging |
| iOS foreground notifications not showing | Delegate not set early enough | Set delegate before `didFinishLaunchingWithOptions` returns |
