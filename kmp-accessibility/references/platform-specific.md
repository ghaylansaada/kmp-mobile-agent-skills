# Platform-Specific Accessibility

## Android Accessibility

### AccessibilityNodeInfo

Compose Multiplatform maps semantics to `AccessibilityNodeInfo` on Android. Most properties are handled automatically, but some advanced scenarios require awareness of the underlying Android accessibility framework.

```kotlin
// androidMain
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.Composable

// Access the underlying Android View for advanced accessibility
@Composable
fun AndroidAccessibilityExample() {
    val view = LocalView.current
    // view.accessibilityDelegate can be set for advanced customization
    // but prefer Compose semantics whenever possible
}
```

### isImportantForAccessibility

On Android, the system determines which views are "important" for accessibility. Compose handles this automatically based on semantics, but when interoperating with Android Views:

```kotlin
// androidMain
import android.view.View

// When embedding Android Views in Compose
fun configureAccessibility(view: View) {
    // Mark as important (has meaningful accessibility info)
    view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES

    // Mark as not important (decorative, skip in accessibility traversal)
    view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO

    // Let the system decide (default)
    view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
}
```

### AccessibilityDelegate for Android View Interop

When you embed native Android views inside Compose (via `AndroidView`), you may need to set an accessibility delegate:

```kotlin
// androidMain
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun AccessibleAndroidViewInterop() {
    val description = stringResource(Res.string.cd_custom_view)

    AndroidView(
        factory = { context ->
            CustomNativeView(context).apply {
                ViewCompat.setAccessibilityDelegate(
                    this,
                    object : AccessibilityDelegateCompat() {
                        override fun onInitializeAccessibilityNodeInfo(
                            host: View,
                            info: AccessibilityNodeInfoCompat,
                        ) {
                            super.onInitializeAccessibilityNodeInfo(host, info)
                            info.contentDescription = description
                            info.roleDescription = "Custom control"
                            info.addAction(
                                AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                                    AccessibilityNodeInfoCompat.ACTION_CLICK,
                                    "Activate",
                                ),
                            )
                        }
                    },
                )
            }
        },
    )
}
```

### Modifier.testTag() for Testing and Accessibility Tools

`testTag` is primarily for UI testing but also appears in accessibility tool output (Layout Inspector, Accessibility Scanner):

```kotlin
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

// Apply testTag to key elements
Button(
    onClick = onSubmit,
    modifier = Modifier.testTag("submit_button"),
) {
    Text(stringResource(Res.string.action_submit))
}

// Test tags help with:
// 1. Compose UI tests (composeTestRule.onNodeWithTag("submit_button"))
// 2. Accessibility Scanner identification of elements
// 3. Layout Inspector debugging of accessibility tree
```

### Accessibility Service Detection (Android)

Detect whether an accessibility service is running to make non-intrusive UI adjustments:

```kotlin
// androidMain
import android.content.Context
import android.view.accessibility.AccessibilityManager

actual fun isAccessibilityServiceEnabled(): Boolean {
    val context: Context = /* from DI */
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    return am.isEnabled
}

actual fun isTouchExplorationEnabled(): Boolean {
    val context: Context = /* from DI */
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    return am.isTouchExplorationEnabled
}
```

**Important:** Never gate functionality behind accessibility service detection. Use it only for non-intrusive enhancements:
- Show additional text labels alongside icons when TalkBack is active
- Increase animation duration for better tracking
- Add visible focus indicators that are normally hidden

## iOS Accessibility

### UIAccessibility Labels in SwiftUI Interop

When embedding SwiftUI views in Compose (via `UIKitView`), configure accessibility through UIKit/SwiftUI APIs:

```kotlin
// iosMain
import androidx.compose.runtime.Composable
import androidx.compose.ui.interop.UIKitView
import platform.UIKit.UILabel
import platform.UIKit.UIAccessibilityTraitButton
import platform.UIKit.UIAccessibilityTraitHeader
import platform.UIKit.UIAccessibilityTraitImage
import platform.UIKit.UIAccessibilityTraitStaticText

@Composable
fun AccessibleUIKitViewInterop() {
    UIKitView(
        factory = {
            UILabel().apply {
                text = "Native Label"
                isAccessibilityElement = true
                accessibilityLabel = "Native accessible label"
                accessibilityHint = "Displays status information"
                accessibilityTraits = UIAccessibilityTraitStaticText
            }
        },
    )
}
```

### UIAccessibilityTraits Mapping from Compose Semantics

| Compose Semantics | iOS UIAccessibilityTrait |
|---|---|
| `role = Role.Button` | `UIAccessibilityTraitButton` |
| `role = Role.Image` | `UIAccessibilityTraitImage` |
| `heading()` | `UIAccessibilityTraitHeader` |
| `role = Role.Checkbox` (checked) | `UIAccessibilityTraitButton` + `UIAccessibilityTraitSelected` |
| `role = Role.Switch` | `UIAccessibilityTraitButton` (toggle announced via value) |
| `role = Role.Tab` (selected) | `UIAccessibilityTraitButton` + `UIAccessibilityTraitSelected` |
| `stateDescription` | Mapped to `accessibilityValue` |
| `contentDescription` | Mapped to `accessibilityLabel` |
| `onClickLabel` | Mapped to `accessibilityHint` |
| `liveRegion` | Triggers `UIAccessibility.post(notification:)` |

### VoiceOver Rotor Custom Actions

Custom accessibility actions defined in Compose appear in VoiceOver's Actions rotor. The user rotates the rotor to "Actions", then swipes up/down to cycle through available actions:

```kotlin
// commonMain -- custom actions work on both platforms
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics

@Composable
fun EmailListItem(
    email: EmailItem,
    onOpen: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onFlag: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val archiveLabel = stringResource(Res.string.cd_action_archive)
    val deleteLabel = stringResource(Res.string.cd_action_delete)
    val flagLabel = stringResource(Res.string.cd_action_flag)

    Row(
        modifier = modifier
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(archiveLabel) {
                        onArchive()
                        true
                    },
                    CustomAccessibilityAction(deleteLabel) {
                        onDelete()
                        true
                    },
                    CustomAccessibilityAction(flagLabel) {
                        onFlag()
                        true
                    },
                )
            }
            .clickable(
                onClickLabel = stringResource(Res.string.cd_action_open_email),
                onClick = onOpen,
            )
            .fillMaxWidth()
            .padding(AppTheme.spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = email.sender,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = email.subject,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = email.timestamp,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
```

### VoiceOver Escape Gesture Handling

The two-finger Z-scrub escape gesture must be explicitly connected to navigation in Compose Multiplatform on iOS:

```kotlin
// iosMain
import platform.UIKit.UIAccessibility
import platform.UIKit.UIViewController

// In your ComposeUIViewController wrapper
class ComposeHostViewController : UIViewController() {

    override fun accessibilityPerformEscape(): Boolean {
        // Connect to Compose navigation back stack
        // This requires a bridge between UIKit and Compose navigation
        val handled = navigationBridge.popBackStack()
        return handled
    }
}
```

### Moving VoiceOver Focus Programmatically (iOS)

`FocusRequester.requestFocus()` only moves keyboard focus on iOS, not VoiceOver focus. To move VoiceOver focus:

```kotlin
// iosMain
import platform.UIKit.UIAccessibility
import platform.UIKit.UIAccessibilityScreenChangedNotification
import platform.UIKit.UIAccessibilityLayoutChangedNotification

// Post a screen changed notification to reset VoiceOver focus
fun announceScreenChange(message: String? = null) {
    UIAccessibility.postNotification(
        UIAccessibilityScreenChangedNotification,
        message,
    )
}

// Post a layout changed notification to move focus to a specific element
fun moveFocusToElement(element: Any?) {
    UIAccessibility.postNotification(
        UIAccessibilityLayoutChangedNotification,
        element,
    )
}
```

### Accessibility Service Detection (iOS)

```kotlin
// iosMain
import platform.UIKit.UIAccessibility

actual fun isAccessibilityServiceEnabled(): Boolean {
    return UIAccessibility.isVoiceOverRunning
}

actual fun isTouchExplorationEnabled(): Boolean {
    return UIAccessibility.isVoiceOverRunning
}

// Additional iOS-specific checks
fun isSwitchControlRunning(): Boolean {
    return UIAccessibility.isSwitchControlRunning
}

fun isBoldTextEnabled(): Boolean {
    return UIAccessibility.isBoldTextEnabled
}

fun isGrayscaleEnabled(): Boolean {
    return UIAccessibility.isGrayscaleEnabled
}
```

## Cross-Platform expect/actual Declarations

```kotlin
// commonMain
expect fun isAccessibilityServiceEnabled(): Boolean
expect fun isTouchExplorationEnabled(): Boolean
expect fun isReducedMotionEnabled(): Boolean
```

### Conditional UI Adjustments

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
fun AdaptiveIconLabel(
    icon: @Composable () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val showLabel = remember { isAccessibilityServiceEnabled() }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        if (showLabel) {
            // Show text label alongside icon when accessibility service is active
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
```

**Rule:** Never use accessibility service detection to:
- Hide features from accessibility users
- Provide a degraded experience
- Gate premium functionality
- Track users for analytics

Only use it to **enhance** the experience (show additional labels, increase timing, add visual cues).

## Complete Imports

```kotlin
// commonMain
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import {your.package}.ui.theme.AppTheme
```
