# Content Description Patterns

## Principles

Content descriptions are the text that screen readers (TalkBack, VoiceOver) announce when a user focuses an element. Getting them right is the single most impactful accessibility improvement. Every content description must come from a localized string resource -- never hardcoded.

## When to Use contentDescription

### Informative Images

Images that convey information the user needs.

```kotlin
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import {your.package}.ui.theme.AppTheme

// Status icon that conveys meaning
Icon(
    imageVector = Icons.Default.CheckCircle,
    contentDescription = stringResource(Res.string.cd_status_verified),
    modifier = Modifier.size(AppTheme.sizing.iconMd),
    tint = MaterialTheme.colorScheme.primary,
)

// Warning icon
Icon(
    imageVector = Icons.Default.Warning,
    contentDescription = stringResource(Res.string.cd_status_warning),
    modifier = Modifier.size(AppTheme.sizing.iconMd),
    tint = MaterialTheme.colorScheme.error,
)

// User profile photo
Image(
    painter = painterResource(Res.drawable.profile_photo),
    contentDescription = stringResource(Res.string.cd_profile_photo, userName),
    modifier = Modifier.size(AppTheme.sizing.iconXl),
    contentScale = ContentScale.Crop,
)
```

### Action Icons (Icon Buttons)

Icons that trigger an action when clicked.

```kotlin
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.IconButton

// Navigation back
IconButton(onClick = onNavigateBack) {
    Icon(
        imageVector = Icons.Default.ArrowBack,
        contentDescription = stringResource(Res.string.cd_navigate_back),
        modifier = Modifier.size(AppTheme.sizing.iconMd),
    )
}

// Delete action
IconButton(onClick = onDelete) {
    Icon(
        imageVector = Icons.Default.Delete,
        contentDescription = stringResource(Res.string.cd_delete_item, itemName),
        modifier = Modifier.size(AppTheme.sizing.iconMd),
    )
}

// Edit action
IconButton(onClick = onEdit) {
    Icon(
        imageVector = Icons.Default.Edit,
        contentDescription = stringResource(Res.string.cd_edit_item, itemName),
        modifier = Modifier.size(AppTheme.sizing.iconMd),
    )
}

// Share action
IconButton(onClick = onShare) {
    Icon(
        imageVector = Icons.Default.Share,
        contentDescription = stringResource(Res.string.cd_share_item, itemName),
        modifier = Modifier.size(AppTheme.sizing.iconMd),
    )
}
```

## When to Use null contentDescription

### Decorative Images

Images that add visual polish but convey no information. Setting `contentDescription = null` hides the element from the accessibility tree.

```kotlin
// Decorative background pattern
Image(
    painter = painterResource(Res.drawable.bg_pattern),
    contentDescription = null,
    modifier = Modifier.fillMaxWidth(),
    contentScale = ContentScale.Crop,
)

// Decorative divider icon
Icon(
    imageVector = Icons.Default.FiberManualRecord,
    contentDescription = null,
    modifier = Modifier.size(AppTheme.sizing.iconSm),
    tint = MaterialTheme.colorScheme.outline,
)
```

### Icons Adjacent to Text That Already Describes Them

When text provides the same information, the icon is redundant for screen readers.

```kotlin
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

// Icon + text label: icon is decorative
Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(
        imageVector = Icons.Default.Email,
        contentDescription = null, // "Email" text already describes this
        modifier = Modifier.size(AppTheme.sizing.iconMd),
    )
    Spacer(Modifier.width(AppTheme.spacing.sm))
    Text(
        text = stringResource(Res.string.label_email),
        style = MaterialTheme.typography.bodyMedium,
    )
}

// Phone icon next to phone number
Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(
        imageVector = Icons.Default.Phone,
        contentDescription = null, // phone number text is sufficient
        modifier = Modifier.size(AppTheme.sizing.iconMd),
    )
    Spacer(Modifier.width(AppTheme.spacing.sm))
    Text(
        text = phoneNumber,
        style = MaterialTheme.typography.bodyMedium,
    )
}
```

### Icons Inside Labeled Buttons

When the button already has a text label, the icon is decorative.

```kotlin
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults

Button(
    onClick = onAddItem,
    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
) {
    Icon(
        imageVector = Icons.Default.Add,
        contentDescription = null, // "Add Item" text already describes the action
        modifier = Modifier.size(AppTheme.sizing.iconSm),
    )
    Spacer(Modifier.width(AppTheme.spacing.sm))
    Text(stringResource(Res.string.action_add_item))
}
```

## Dynamic Content Descriptions

Content descriptions that change based on data or state.

### Descriptions from Data

```kotlin
// User avatar with dynamic name
Image(
    painter = rememberAsyncImagePainter(user.avatarUrl),
    contentDescription = stringResource(Res.string.cd_user_avatar, user.displayName),
    modifier = Modifier.size(AppTheme.sizing.iconXl),
    contentScale = ContentScale.Crop,
)

// Notification badge
if (unreadCount > 0) {
    Icon(
        imageVector = Icons.Default.Notifications,
        contentDescription = stringResource(
            Res.string.cd_notifications_unread,
            unreadCount,
        ),
        modifier = Modifier.size(AppTheme.sizing.iconMd),
    )
} else {
    Icon(
        imageVector = Icons.Default.Notifications,
        contentDescription = stringResource(Res.string.cd_notifications_none),
        modifier = Modifier.size(AppTheme.sizing.iconMd),
    )
}
```

### Toggle Buttons with State-Dependent Descriptions

```kotlin
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

IconButton(
    onClick = onToggleFavorite,
    modifier = Modifier.semantics {
        stateDescription = if (isFavorite) {
            stringResource(Res.string.cd_state_favorited)
        } else {
            stringResource(Res.string.cd_state_not_favorited)
        }
    },
) {
    Icon(
        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
        contentDescription = stringResource(Res.string.cd_toggle_favorite, itemName),
        modifier = Modifier.size(AppTheme.sizing.iconMd),
        tint = if (isFavorite) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}
```

### Expandable Sections

```kotlin
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore

Row(
    modifier = Modifier
        .semantics(mergeDescendants = true) {
            stateDescription = if (isExpanded) {
                stringResource(Res.string.cd_state_expanded)
            } else {
                stringResource(Res.string.cd_state_collapsed)
            }
        }
        .clickable(
            onClickLabel = if (isExpanded) {
                stringResource(Res.string.cd_action_collapse)
            } else {
                stringResource(Res.string.cd_action_expand)
            },
            onClick = onToggleExpand,
        )
        .padding(AppTheme.spacing.lg),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
) {
    Text(
        text = sectionTitle,
        style = MaterialTheme.typography.titleMedium,
    )
    Icon(
        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
        contentDescription = null, // state is on the parent
        modifier = Modifier.size(AppTheme.sizing.iconMd),
    )
}
```

## Image Accessibility Patterns

### Informative Image with Fallback

```kotlin
@Composable
fun AccessibleNetworkImage(
    imageUrl: String?,
    contentDescriptionText: String,
    modifier: Modifier = Modifier,
) {
    if (imageUrl != null) {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescriptionText,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = modifier
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .semantics {
                    contentDescription = contentDescriptionText
                    role = Role.Image
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null, // parent has description
                modifier = Modifier.size(AppTheme.sizing.iconLg),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
```

### Image Gallery Item

```kotlin
@Composable
fun GalleryItem(
    imageUrl: String,
    imageIndex: Int,
    totalImages: Int,
    altText: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = altText ?: stringResource(
        Res.string.cd_gallery_image,
        imageIndex + 1,
        totalImages,
    )

    AsyncImage(
        model = imageUrl,
        contentDescription = description,
        modifier = modifier
            .clickable(
                onClickLabel = stringResource(Res.string.cd_action_view_full_size),
                onClick = onClick,
            ),
        contentScale = ContentScale.Crop,
    )
}
```

## String Resource Naming Convention

All content description strings follow the `cd_` prefix convention:

```
cd_navigate_back           -> "Navigate back"
cd_search                  -> "Search"
cd_delete_item             -> "Delete %1$s"         (parameterized)
cd_edit_item               -> "Edit %1$s"           (parameterized)
cd_share_item              -> "Share %1$s"           (parameterized)
cd_toggle_favorite         -> "Toggle favorite for %1$s"
cd_status_verified         -> "Verified"
cd_status_warning          -> "Warning"
cd_profile_photo           -> "%1$s's profile photo"
cd_user_avatar             -> "%1$s's avatar"
cd_notifications_unread    -> "%1$d unread notifications"
cd_notifications_none      -> "No notifications"
cd_state_enabled           -> "Enabled"
cd_state_disabled          -> "Disabled"
cd_state_favorited         -> "Favorited"
cd_state_not_favorited     -> "Not favorited"
cd_state_expanded          -> "Expanded"
cd_state_collapsed         -> "Collapsed"
cd_action_collapse         -> "Collapse"
cd_action_expand           -> "Expand"
cd_action_view_full_size   -> "View full size"
cd_gallery_image           -> "Image %1$d of %2$d"
cd_loading                 -> "Loading"
cd_download_progress       -> "Download progress: %1$d percent"
```

## Complete Imports

```kotlin
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import {your.package}.ui.theme.AppTheme
```
