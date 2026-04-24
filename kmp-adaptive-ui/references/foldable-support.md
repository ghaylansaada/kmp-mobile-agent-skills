# Foldable Device Support

Foldable devices introduce posture-aware layouts where the screen hinge position and fold state affect content arrangement. This reference covers detecting fold postures, splitting content at the hinge, and providing a cross-platform abstraction.

## Foldable Postures

| Posture | Description | Layout Strategy |
|---|---|---|
| **Flat** | Device fully open, single plane | Standard adaptive layout (window size classes) |
| **Half-opened (tabletop)** | Folded along horizontal axis, bottom half flat on table | Split content at hinge: video/image on top, controls on bottom |
| **Half-opened (book)** | Folded along vertical axis, like an open book | Dual-pane layout: content on left, detail/controls on right |
| **Folded** | Device closed, using outer display | Compact layout, typically phone-sized window |

## Android: WindowInfoTracker

Jetpack Window Manager provides `WindowInfoTracker` for fold detection on Android.

### Dependency

```toml
[libraries]
window-manager = { module = "androidx.window:window", version.ref = "windowManager" }
```

```kotlin
kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.window.manager)
        }
    }
}
```

### Reading Fold State

```kotlin
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo

// In Android-specific code (androidMain)
class AndroidFoldStateProvider(private val activity: Activity) {
    fun windowLayoutInfoFlow(): Flow<WindowLayoutInfo> =
        WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity)
}
```

### FoldingFeature Properties

```kotlin
val foldingFeature: FoldingFeature = // from WindowLayoutInfo.displayFeatures

// State: FLAT or HALF_OPENED
val state: FoldingFeature.State = foldingFeature.state

// Orientation: HORIZONTAL or VERTICAL
val orientation: FoldingFeature.Orientation = foldingFeature.orientation

// Bounds: the rectangle occupied by the fold/hinge
val bounds: Rect = foldingFeature.bounds

// Is the fold separating the display into two regions?
val isSeparating: Boolean = foldingFeature.isSeparating
```

### Posture Detection

```kotlin
enum class DevicePosture {
    Normal,         // Flat, no fold, or fold is flat
    TabletopTop,    // Half-opened, horizontal fold, content split top/bottom
    Book,           // Half-opened, vertical fold, content split left/right
}

fun computePosture(foldingFeature: FoldingFeature?): DevicePosture {
    if (foldingFeature == null) return DevicePosture.Normal
    if (foldingFeature.state != FoldingFeature.State.HALF_OPENED) return DevicePosture.Normal

    return when (foldingFeature.orientation) {
        FoldingFeature.Orientation.HORIZONTAL -> DevicePosture.TabletopTop
        FoldingFeature.Orientation.VERTICAL -> DevicePosture.Book
    }
}
```

## Cross-Platform Abstraction (expect/actual)

Since `WindowInfoTracker` is Android-only, use `expect`/`actual` to provide fold state across platforms.

### Common (commonMain)

```kotlin
package {your.package}.platform

import kotlinx.coroutines.flow.Flow

enum class DevicePosture {
    Normal,
    TabletopTop,
    Book,
}

data class FoldInfo(
    val posture: DevicePosture = DevicePosture.Normal,
    val hingeBoundsTop: Int = 0,
    val hingeBoundsBottom: Int = 0,
    val hingeBoundsLeft: Int = 0,
    val hingeBoundsRight: Int = 0,
)

expect class FoldStateProvider {
    fun foldInfoFlow(): Flow<FoldInfo>
}
```

### Android (androidMain)

```kotlin
package {your.package}.platform

import android.app.Activity
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

actual class FoldStateProvider(private val activity: Activity) {
    actual fun foldInfoFlow(): Flow<FoldInfo> =
        WindowInfoTracker.getOrCreate(activity)
            .windowLayoutInfo(activity)
            .map { layoutInfo ->
                val foldingFeature = layoutInfo.displayFeatures
                    .filterIsInstance<FoldingFeature>()
                    .firstOrNull()

                if (foldingFeature == null || foldingFeature.state != FoldingFeature.State.HALF_OPENED) {
                    FoldInfo()
                } else {
                    FoldInfo(
                        posture = when (foldingFeature.orientation) {
                            FoldingFeature.Orientation.HORIZONTAL -> DevicePosture.TabletopTop
                            FoldingFeature.Orientation.VERTICAL -> DevicePosture.Book
                        },
                        hingeBoundsTop = foldingFeature.bounds.top,
                        hingeBoundsBottom = foldingFeature.bounds.bottom,
                        hingeBoundsLeft = foldingFeature.bounds.left,
                        hingeBoundsRight = foldingFeature.bounds.right,
                    )
                }
            }
}
```

### iOS (iosMain)

```kotlin
package {your.package}.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

actual class FoldStateProvider {
    actual fun foldInfoFlow(): Flow<FoldInfo> = flowOf(FoldInfo())
}
```

iOS does not have foldable devices. The no-op implementation returns `DevicePosture.Normal` permanently.

## Composable Integration

### Collecting Fold State

```kotlin
@Composable
fun rememberFoldInfo(foldStateProvider: FoldStateProvider): State<FoldInfo> {
    return foldStateProvider.foldInfoFlow()
        .collectAsState(initial = FoldInfo())
}
```

### Tabletop Layout (Horizontal Fold)

When the device is in tabletop posture (folded on a horizontal hinge), split content with the upper half showing media/content and the lower half showing controls:

```kotlin
@Composable
fun TabletopAwareContent(
    foldInfo: FoldInfo,
    modifier: Modifier = Modifier,
) {
    if (foldInfo.posture == DevicePosture.TabletopTop) {
        Column(modifier = modifier.fillMaxSize()) {
            // Upper half: content above the hinge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(AppTheme.spacing.lg),
            ) {
                // Video player, image, or primary content
                Text(
                    text = stringResource(Res.string.media_content),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Hinge gap
            Spacer(modifier = Modifier.height(AppTheme.spacing.sm))

            // Lower half: controls below the hinge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(AppTheme.spacing.lg),
            ) {
                // Playback controls, action buttons
                Text(
                    text = stringResource(Res.string.controls_content),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        // Normal layout (no fold or flat)
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(AppTheme.spacing.lg),
        ) {
            // Single-pane layout
            Text(
                text = stringResource(Res.string.media_content),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(AppTheme.spacing.lg))
            Text(
                text = stringResource(Res.string.controls_content),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
```

### Book Layout (Vertical Fold)

When the device is in book posture (folded on a vertical hinge), use a dual-pane layout:

```kotlin
@Composable
fun BookAwareContent(
    foldInfo: FoldInfo,
    modifier: Modifier = Modifier,
) {
    if (foldInfo.posture == DevicePosture.Book) {
        Row(modifier = modifier.fillMaxSize()) {
            // Left pane
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(AppTheme.spacing.lg),
            ) {
                Text(
                    text = stringResource(Res.string.primary_content),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Hinge gap
            Spacer(modifier = Modifier.width(AppTheme.spacing.sm))

            // Right pane
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(AppTheme.spacing.lg),
            ) {
                Text(
                    text = stringResource(Res.string.secondary_content),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        // Normal single-pane layout
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(AppTheme.spacing.lg),
        ) {
            Text(
                text = stringResource(Res.string.primary_content),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(AppTheme.spacing.lg))
            Text(
                text = stringResource(Res.string.secondary_content),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
```

## Testing with Android Emulator

Test foldable layouts using the Android Emulator's foldable device profiles:

1. Create an AVD with a foldable hardware profile (7.6" Foldable, 8" Foldable with outer display)
2. Use the emulator's posture controls to switch between flat, half-opened, and folded states
3. Test transitions: fold and unfold while the app is running to verify layout adapts in real time
4. Test both horizontal and vertical fold orientations

Emulator posture controls are in the extended controls panel (three-dot menu) under "Posture" or "Virtual sensors".

## File Placement

```
commonMain/
  kotlin/{your.package}/
    platform/
      DevicePosture.kt       <- DevicePosture enum, FoldInfo data class
      FoldStateProvider.kt    <- expect declaration

androidMain/
  kotlin/{your.package}/
    platform/
      FoldStateProvider.kt    <- actual with WindowInfoTracker

iosMain/
  kotlin/{your.package}/
    platform/
      FoldStateProvider.kt    <- actual with no-op Flow
```

## Rules

1. **Always provide a normal/fallback layout.** Never gate UI features exclusively on fold detection. Non-foldable devices and iOS must have a complete experience.
2. **Use `expect`/`actual` for the `FoldStateProvider`.** Do not import `androidx.window.*` in `commonMain`.
3. **Fold state can change at runtime.** Collect it as a `Flow` and react to changes. Do not cache the initial value.
4. **Combine fold detection with window size classes.** A half-opened foldable may have medium or expanded width. Use window size class for primary layout decisions and fold posture for hinge-aware refinements.
5. **All spacing must use `AppTheme.spacing.*`.** Hinge gaps, content padding, and pane margins must use design tokens.
6. **All strings must use `stringResource()`.** Labels and content descriptions must come from Compose resources.
