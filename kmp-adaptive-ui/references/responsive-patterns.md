# Responsive Layout Patterns

This reference covers practical responsive patterns that adapt content based on window size: adaptive padding, grid columns, dialog presentation, form layouts, image sizing, and RTL support. All patterns use `AppTheme` design tokens and `stringResource()` for compliance.

## Responsive Padding and Margins

Scale horizontal padding based on window width class to prevent overly wide content on large screens:

```kotlin
@Composable
fun responsiveHorizontalPadding(widthSizeClass: WindowWidthSizeClass): Dp =
    when (widthSizeClass) {
        WindowWidthSizeClass.COMPACT -> AppTheme.spacing.lg
        WindowWidthSizeClass.MEDIUM -> AppTheme.spacing.xl
        else -> AppTheme.spacing.xxl
    }

@Composable
fun ResponsiveScreen(
    widthSizeClass: WindowWidthSizeClass,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = responsiveHorizontalPadding(widthSizeClass))
            .padding(vertical = AppTheme.spacing.lg),
        content = content,
    )
}
```

### Content Max Width

On expanded screens, constrain content to a readable maximum width instead of stretching to fill:

```kotlin
@Composable
fun MaxWidthContent(
    widthSizeClass: WindowWidthSizeClass,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = if (widthSizeClass == WindowWidthSizeClass.COMPACT) Dp.Unspecified else 840.dp)
                .fillMaxWidth()
                .padding(horizontal = responsiveHorizontalPadding(widthSizeClass)),
            content = content,
        )
    }
}
```

Note: The `840.dp` max width is a layout constraint breakpoint from the Material Design specification, not a design token.

## Adaptive Grid Columns

Adjust grid column count based on available width:

```kotlin
@Composable
fun adaptiveGridColumns(widthSizeClass: WindowWidthSizeClass): Int =
    when (widthSizeClass) {
        WindowWidthSizeClass.COMPACT -> 2
        WindowWidthSizeClass.MEDIUM -> 3
        else -> 4
    }

@Composable
fun AdaptiveGrid(
    items: List<GridItem>,
    widthSizeClass: WindowWidthSizeClass,
    modifier: Modifier = Modifier,
) {
    val columns = adaptiveGridColumns(widthSizeClass)

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppTheme.spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        items(
            items = items,
            key = { it.id },
        ) { item ->
            GridItemCard(item = item)
        }
    }
}
```

### BoxWithConstraints Grid

For component-level column decisions independent of window size class:

```kotlin
@Composable
fun AutoColumnGrid(
    items: List<GridItem>,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val columns = when {
            maxWidth >= 1200.dp -> 6
            maxWidth >= 840.dp -> 4
            maxWidth >= 600.dp -> 3
            else -> 2
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(AppTheme.spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            items(
                items = items,
                key = { it.id },
            ) { item ->
                GridItemCard(item = item)
            }
        }
    }
}
```

Note: The width breakpoints (600, 840, 1200 dp) are Material Design window class boundaries used for layout decisions, not visual design tokens.

## Image Sizing Based on Available Width

Scale images relative to available width rather than using fixed sizes:

```kotlin
@Composable
fun ResponsiveHeroImage(
    imageUrl: String,
    widthSizeClass: WindowWidthSizeClass,
    modifier: Modifier = Modifier,
) {
    val aspectRatio = when (widthSizeClass) {
        WindowWidthSizeClass.COMPACT -> 16f / 9f
        WindowWidthSizeClass.MEDIUM -> 21f / 9f
        else -> 32f / 9f
    }

    AsyncImage(
        model = imageUrl,
        contentDescription = stringResource(Res.string.cd_hero_image),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio),
        contentScale = ContentScale.Crop,
    )
}
```

## Form Layouts: Single Column vs Multi-Column

On compact screens, stack form fields vertically. On larger screens, place related fields side by side:

```kotlin
@Composable
fun AdaptiveForm(
    widthSizeClass: WindowWidthSizeClass,
    modifier: Modifier = Modifier,
) {
    val isCompact = widthSizeClass == WindowWidthSizeClass.COMPACT

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
    ) {
        Text(
            text = stringResource(Res.string.form_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (isCompact) {
            // Single column: fields stacked vertically
            OutlinedTextField(
                value = firstName,
                onValueChange = onFirstNameChange,
                label = { Text(stringResource(Res.string.first_name_label)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = lastName,
                onValueChange = onLastNameChange,
                label = { Text(stringResource(Res.string.last_name_label)) },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            // Multi-column: related fields side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
            ) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = onFirstNameChange,
                    label = { Text(stringResource(Res.string.first_name_label)) },
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = onLastNameChange,
                    label = { Text(stringResource(Res.string.last_name_label)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text(stringResource(Res.string.email_label)) },
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .then(
                    if (isCompact) Modifier.fillMaxWidth()
                    else Modifier.widthIn(min = 200.dp)
                )
                .heightIn(min = AppTheme.sizing.minTouchTarget),
        ) {
            Text(stringResource(Res.string.action_submit))
        }
    }
}
```

Note: The `200.dp` minimum button width on non-compact layouts is a layout constraint ensuring the button doesn't shrink to unreadable size, not a design token.

## Dialog vs Full-Screen Based on Window Size

Show a dialog on larger screens and a full-screen overlay on compact screens:

```kotlin
@Composable
fun AdaptiveDetailView(
    isVisible: Boolean,
    widthSizeClass: WindowWidthSizeClass,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (!isVisible) return

    if (widthSizeClass == WindowWidthSizeClass.COMPACT) {
        // Full-screen on compact
        FullScreenDialog(
            onDismiss = onDismiss,
            content = content,
        )
    } else {
        // Dialog on medium+
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(Res.string.detail_title)) },
            text = { content() },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.action_close))
                }
            },
        )
    }
}

@Composable
fun FullScreenDialog(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(AppTheme.spacing.lg),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(AppTheme.sizing.minTouchTarget),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(Res.string.cd_close_dialog),
                            modifier = Modifier.size(AppTheme.sizing.iconMd),
                        )
                    }
                }
                content()
            }
        }
    }
}
```

## BoxWithConstraints for Component-Level Decisions

Use `BoxWithConstraints` when a component needs to adapt based on its own available space rather than the global window size class:

```kotlin
@Composable
fun AdaptiveProfileHeader(
    userName: String,
    avatarUrl: String,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxWidth > 400.dp) {
            // Wide: horizontal layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = stringResource(Res.string.cd_user_avatar),
                    modifier = Modifier
                        .size(AppTheme.sizing.iconXl)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Text(
                    text = userName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        } else {
            // Narrow: vertical layout
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            ) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = stringResource(Res.string.cd_user_avatar),
                    modifier = Modifier
                        .size(AppTheme.sizing.iconXl)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Text(
                    text = userName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
```

Note: The `400.dp` threshold is a component-level layout breakpoint, not a design token.

## RTL (Right-to-Left) Support

### Use start/end, Never left/right

```kotlin
// WRONG -- does not flip in RTL
Modifier.padding(left = AppTheme.spacing.lg, right = AppTheme.spacing.sm)
Row(horizontalArrangement = Arrangement.Absolute.Left)

// RIGHT -- flips correctly in RTL
Modifier.padding(start = AppTheme.spacing.lg, end = AppTheme.spacing.sm)
Row(horizontalArrangement = Arrangement.Start)
```

### Reading Layout Direction

```kotlin
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

@Composable
fun DirectionAwareContent(modifier: Modifier = Modifier) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = AppTheme.spacing.lg,
                end = AppTheme.spacing.sm,
            ),
    ) {
        // Content respects RTL automatically through start/end
        Icon(
            imageVector = if (isRtl) Icons.Default.ArrowBack else Icons.Default.ArrowForward,
            contentDescription = stringResource(Res.string.cd_direction_arrow),
            modifier = Modifier.size(AppTheme.sizing.iconMd),
        )
        Spacer(Modifier.width(AppTheme.spacing.sm))
        Text(
            text = stringResource(Res.string.content_label),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
```

### Testing RTL Layout

Force RTL layout direction in previews and tests:

```kotlin
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

@Preview
@Composable
private fun ContentRtlPreview() {
    AppTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            DirectionAwareContent()
        }
    }
}
```

### RTL Checklist

- All horizontal padding uses `start`/`end`, never `left`/`right`
- All horizontal arrangement uses `Arrangement.Start`/`End`, never `Arrangement.Absolute.Left`/`Right`
- Directional icons (arrows, chevrons) are mirrored for RTL using `LocalLayoutDirection` check
- Text alignment uses `TextAlign.Start`/`End`, never `TextAlign.Left`/`Right`
- `LazyRow` uses `contentPadding` with `PaddingValues(start = ..., end = ...)`, not `PaddingValues(left = ..., right = ...)`

## Responsive Item Row

An item card that adapts its internal layout based on available width:

```kotlin
@Composable
fun ResponsiveItemRow(
    title: String,
    description: String,
    imageUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AppTheme.sizing.minTouchTarget),
        shape = RoundedCornerShape(AppTheme.corners.lg),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        BoxWithConstraints {
            if (maxWidth > 400.dp) {
                Row(
                    modifier = Modifier.padding(AppTheme.spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null, // Decorative
                        modifier = Modifier
                            .size(AppTheme.sizing.iconXl)
                            .clip(RoundedCornerShape(AppTheme.corners.md)),
                        contentScale = ContentScale.Crop,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(AppTheme.spacing.xs))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.padding(AppTheme.spacing.md)) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null, // Decorative
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(AppTheme.corners.md)),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(Modifier.height(AppTheme.spacing.sm))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(AppTheme.spacing.xs))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
```

## Rules

1. **All spacing uses `AppTheme.spacing.*`.** No hardcoded dp values for padding, margins, or arrangement spacing.
2. **All sizing uses `AppTheme.sizing.*`.** Icon sizes, touch targets, and component heights use tokens.
3. **All corner radii use `AppTheme.corners.*`.** Shape definitions use corner tokens.
4. **All colors use `MaterialTheme.colorScheme.*`.** No hardcoded hex or Color() values.
5. **All text uses `stringResource()`.** Labels, titles, descriptions, and accessibility labels come from Compose resources.
6. **Use `start`/`end` for horizontal layout.** Never `left`/`right`. This ensures RTL compatibility.
7. **Prefer window size class for layout decisions.** Use `BoxWithConstraints` only for component-level sizing that cannot be determined from the window size class.
8. **Layout breakpoints (600dp, 840dp, 1200dp, 400dp) are not design tokens.** They are Material Design specification values or component-level thresholds that determine structural layout changes, not visual styling.

## Imports

```kotlin
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import org.jetbrains.compose.resources.stringResource
import {your.package}.ui.theme.AppTheme
```
