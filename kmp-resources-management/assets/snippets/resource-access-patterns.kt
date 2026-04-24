package mobile.composeapp.ui

// =============================================================================
// Resource Access Patterns
// =============================================================================

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mobile.composeapp.ui.theme.AppTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.compose_multiplatform

// ---------------------------------------------------------------------------
// Pattern 1: Shared vector drawable via painterResource
// ---------------------------------------------------------------------------
// Place XML in: composeApp/src/commonMain/composeResources/drawable/my_icon.xml

@Composable
fun SharedDrawableExample() {
    Image(
        painter = painterResource(Res.drawable.compose_multiplatform),
        contentDescription = stringResource(Res.string.cd_compose_multiplatform_logo),
        modifier = Modifier.size(AppTheme.sizing.iconXl),
    )
}

// ---------------------------------------------------------------------------
// Pattern 2: Shared string resource via stringResource
// ---------------------------------------------------------------------------
// Create: composeApp/src/commonMain/composeResources/values/strings.xml
// Format:
//   <resources>
//       <string name="greeting">Hello, World!</string>
//   </resources>
// After Gradle sync, import mobile.composeapp.generated.resources.greeting

@Composable
fun SharedStringExample() {
    Text(text = stringResource(Res.string.greeting))
}

// ---------------------------------------------------------------------------
// Pattern 3: Android-only resource access (androidMain only)
// ---------------------------------------------------------------------------
// In androidMain source set only:
//   import mobile.composeapp.R
//   val appName = context.getString(R.string.app_name)
//   val background = ContextCompat.getDrawable(context, R.drawable.ic_launcher_background)

// ---------------------------------------------------------------------------
// Pattern 4: Adding a new shared drawable
// ---------------------------------------------------------------------------
// Step 1: Create composeApp/src/commonMain/composeResources/drawable/my_new_icon.xml
// Step 2: Run Gradle sync in the IDE
// Step 3: Import and use:
//   import mobile.composeapp.generated.resources.my_new_icon
//   Image(painter = painterResource(Res.drawable.my_new_icon), ...)
//
// Note: "my-new-icon.xml" generates "my_new_icon" (hyphens -> underscores)

// ---------------------------------------------------------------------------
// Pattern 5: Composable with multiple resource types
// ---------------------------------------------------------------------------

@Composable
fun MultiResourceExample() {
    Column {
        Image(
            painter = painterResource(Res.drawable.compose_multiplatform),
            contentDescription = stringResource(Res.string.cd_logo),
        )
        Text(text = stringResource(Res.string.greeting))
    }
}

// ---------------------------------------------------------------------------
// Pattern 6: Passing resources to non-composable code
// ---------------------------------------------------------------------------
// Use DrawableResource / StringResource types to pass resource references
// outside @Composable context, then resolve inside a composable.

data class UiItem(
    val icon: DrawableResource,
    val label: StringResource,
)

@Composable
fun UiItemRow(item: UiItem) {
    Column {
        Image(
            painter = painterResource(item.icon),
            contentDescription = null,
            modifier = Modifier.size(AppTheme.sizing.minTouchTarget),
        )
        Text(text = stringResource(item.label))
    }
}
