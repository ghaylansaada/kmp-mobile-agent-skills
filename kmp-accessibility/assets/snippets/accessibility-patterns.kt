package {your.package}.ui.snippets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import {your.package}.ui.theme.AppTheme

// ─── Pattern 1: Accessible Icon Button ──────────────────────────────────────

/**
 * Icon button with proper content description from string resource.
 * Touch target is handled by Material 3 IconButton (48dp default).
 */
@Composable
fun AccessibleDeleteButton(
    itemName: String,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onDelete,
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = stringResource(Res.string.cd_delete_item, itemName),
            modifier = Modifier.size(AppTheme.sizing.iconMd),
        )
    }
}

// ─── Pattern 2: Decorative Image with null contentDescription ───────────────

/**
 * Decorative icon that adds visual context but no informational value.
 * contentDescription = null removes it from the accessibility tree.
 */
@Composable
fun DecorativeRatingStars(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs),
    ) {
        repeat(5) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null, // decorative only
                modifier = Modifier.size(AppTheme.sizing.iconSm),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// ─── Pattern 3: Toggle with Custom stateDescription ─────────────────────────

/**
 * Toggle row where the entire row is the tap target.
 * stateDescription tells the screen reader the current toggle state.
 */
@Composable
fun AccessibleToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                role = Role.Switch
                stateDescription = if (checked) {
                    stringResource(Res.string.cd_state_on)
                } else {
                    stringResource(Res.string.cd_state_off)
                }
            }
            .clickable(
                onClickLabel = stringResource(Res.string.cd_toggle),
                onClick = { onCheckedChange(!checked) },
            )
            .padding(
                horizontal = AppTheme.spacing.lg,
                vertical = AppTheme.spacing.md,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
        Switch(
            checked = checked,
            onCheckedChange = null, // handled by Row clickable
        )
    }
}

// ─── Pattern 4: List Item with mergeDescendants and Custom Actions ──────────

/**
 * List item that merges title + subtitle into one announcement
 * and provides swipe-to-dismiss alternative via custom actions.
 */
@Composable
fun AccessibleListItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val deleteLabel = stringResource(Res.string.cd_action_delete)
    val favoriteLabel = stringResource(Res.string.cd_action_favorite)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                customActions = listOf(
                    CustomAccessibilityAction(deleteLabel) {
                        onDelete()
                        true
                    },
                    CustomAccessibilityAction(favoriteLabel) {
                        onFavorite()
                        true
                    },
                )
            }
            .clickable(
                onClickLabel = stringResource(Res.string.cd_action_open),
                onClick = onClick,
            )
            .padding(AppTheme.spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─── Pattern 5: Error Field with semantics { error() } ─────────────────────

/**
 * Text field error annotation for screen reader announcement.
 * When focused, TalkBack/VoiceOver announces "Error: [message]".
 */
@Composable
fun AccessibleErrorField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = errorMessage != null,
        supportingText = errorMessage?.let {
            {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(AppTheme.sizing.iconSm),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text(it)
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                if (errorMessage != null) {
                    error(errorMessage)
                }
            },
    )
}

// ─── Pattern 6: Progress Indicator with semantics ───────────────────────────

/**
 * Determinate progress bar with semantic progress info.
 * Screen reader announces "X percent" as progress changes.
 */
@Composable
fun AccessibleProgressBar(
    progress: Float,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(AppTheme.spacing.sm))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = progress,
                        range = 0f..1f,
                        steps = 0,
                    )
                    contentDescription = stringResource(
                        Res.string.cd_progress,
                        (progress * 100).toInt(),
                    )
                },
        )
    }
}

/**
 * Indeterminate progress with loading announcement.
 */
@Composable
fun AccessibleLoadingIndicator(
    modifier: Modifier = Modifier,
) {
    CircularProgressIndicator(
        modifier = modifier
            .size(AppTheme.sizing.iconLg)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                contentDescription = stringResource(Res.string.cd_loading)
            },
    )
}

// ─── Pattern 7: Heading Text with semantics { heading() } ──────────────────

/**
 * Section heading for screen reader navigation.
 * Users can jump between headings using heading navigation gestures.
 */
@Composable
fun AccessibleSectionHeading(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier.semantics { heading() },
    )
}

// ─── Pattern 8: LiveRegion for Dynamic Updates ──────────────────────────────

/**
 * Status text that announces changes to screen readers automatically.
 * Uses Polite mode so it does not interrupt current speech.
 */
@Composable
fun AccessibleStatusMessage(
    message: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.semantics {
            liveRegion = LiveRegionMode.Polite
        },
    )
}

/**
 * Error banner that interrupts screen reader with urgent announcement.
 * Use sparingly -- Assertive mode interrupts current speech.
 */
@Composable
fun AccessibleErrorBanner(
    errorMessage: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                liveRegion = LiveRegionMode.Assertive
            }
            .padding(AppTheme.spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null, // error text provides the info
            modifier = Modifier.size(AppTheme.sizing.iconMd),
            tint = MaterialTheme.colorScheme.error,
        )
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}
