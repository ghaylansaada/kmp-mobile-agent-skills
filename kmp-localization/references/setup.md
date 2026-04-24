# Setup

> **Note:** Version numbers are intentionally omitted. Always use the latest stable release from the official documentation.

## Prerequisites

The template already includes `compose.components.resources` in `commonMain.dependencies`. No additional Gradle dependencies needed.

## Resource Directory Structure

```
composeApp/src/commonMain/composeResources/
    values/
        strings.xml              (default English)
        plurals.xml              (default English plurals)
    values-ar/
        strings.xml              (Arabic)
    values-fr/
        strings.xml              (French)
```

## Default Strings (values/strings.xml)

```xml
<resources>
    <string name="app_name">Mobile App</string>

    <string name="action_ok">OK</string>
    <string name="action_cancel">Cancel</string>
    <string name="action_retry">Retry</string>
    <string name="action_save">Save</string>
    <string name="action_delete">Delete</string>

    <string name="error_generic">Something went wrong. Please try again.</string>
    <string name="error_network">No internet connection. Please check your network.</string>
    <string name="error_timeout">Request timed out. Please try again.</string>
    <string name="error_unauthorized">Your session has expired. Please log in again.</string>
    <string name="error_not_found">The requested resource was not found.</string>
    <string name="error_server">Server error. Please try again later.</string>

    <string name="greeting_user">Hello, %1$s!</string>
    <string name="item_count_label">%1$d items selected</string>
</resources>
```

## Plurals (values/plurals.xml)

```xml
<resources>
    <plurals name="items_count">
        <item quantity="one">%1$d item</item>
        <item quantity="other">%1$d items</item>
    </plurals>
    <plurals name="minutes_ago">
        <item quantity="one">%1$d minute ago</item>
        <item quantity="other">%1$d minutes ago</item>
    </plurals>
</resources>
```

## Locale-Specific Example (values-ar/strings.xml)

```xml
<resources>
    <string name="app_name">تطبيق الهاتف</string>
    <string name="action_ok">موافق</string>
    <string name="action_cancel">إلغاء</string>
    <string name="error_generic">حدث خطأ ما. يرجى المحاولة مرة أخرى.</string>
    <string name="greeting_user">مرحبًا، %1$s!</string>
</resources>
```

Translate all keys from the default file. Keys missing in a locale directory fall back to the default `values/` strings.

## Android strings.xml Coexistence

`androidMain/res/values/strings.xml` provides `app_name` for Android system surfaces (launcher, settings). Compose Multiplatform `composeResources/values/strings.xml` is separate for in-app UI. Both coexist without conflict.

## Generated Code

After adding strings, sync the project to trigger Compose resource class generation. Access via:

```kotlin
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.app_name
```

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| `Unresolved reference: Res` | Sync project to trigger Compose resource class generation |
| Locale not applied | Use `values-ar` not `values-ar-rSA`; Compose resources use BCP 47 subtags |
| Arabic text not RTL | Set `LocalLayoutDirection` — see locale-manager.md |
