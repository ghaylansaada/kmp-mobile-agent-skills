# Implementation: Resources Management

## Shared Resources

### Compose Multiplatform Vector Drawable

File: `composeApp/src/commonMain/composeResources/drawable/compose-multiplatform.xml`

Uses Android vector format (`<vector>` with `android:pathData`, `android:fillColor`, gradient via `<aapt:attr>`). See [vector-drawable.xml.template](../assets/templates/vector-drawable.xml.template) for the structure.

Key details of the project's bundled vector:
- 450dp x 450dp, viewport 64x64
- Five `<path>` elements: solid fills (`#6075f2`, `#6b57ff`, `#000000`) and two gradients (radial, linear)
- Uses `<aapt:attr name="android:fillColor">` for inline gradient definitions

Note: `compose-multiplatform.xml` generates accessor `Res.drawable.compose_multiplatform` (hyphens become underscores).

### Accessing Shared Resources in Composables

See [resource-access-patterns.kt](../assets/snippets/resource-access-patterns.kt) for all access patterns (drawables, strings, Android-only, multi-resource).

Summary of patterns:
1. **Shared drawable** -- `painterResource(Res.drawable.name)` with import from `mobile.composeapp.generated.resources`
2. **Shared string** -- `stringResource(Res.string.name)` after creating `composeResources/values/strings.xml`
3. **Android-only** -- `R.string.app_name` / `R.drawable.*` in androidMain only
4. **Multi-resource composable** -- combine drawable and string accessors in a single composable

## Android Resources

### String Resources

File: `composeApp/src/androidMain/res/values/strings.xml`

```xml
<resources>
    <string name="app_name">mobile</string>
</resources>
```

### Adaptive Icon

File: `composeApp/src/androidMain/res/mipmap-anydpi-v26/ic_launcher.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>
```

### AndroidManifest References

```xml
<application
    android:icon="@mipmap/ic_launcher"
    android:roundIcon="@mipmap/ic_launcher_round"
    android:label="@string/app_name">
```

## Adding New Resources

### New Shared Drawables

1. Place the vector XML in `composeApp/src/commonMain/composeResources/drawable/`
2. Run Gradle sync to generate the accessor
3. Access via `Res.drawable.your_resource_name`

### New Shared Strings

1. Create `composeApp/src/commonMain/composeResources/values/strings.xml`
2. Add string entries in standard Android resource format
3. Run Gradle sync
4. Access via `Res.string.your_string_name` with `stringResource()`
