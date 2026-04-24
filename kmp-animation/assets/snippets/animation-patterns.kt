package {your.package}.ui.animations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import {your.package}.theme.AppTheme
import {your.package}.Res

// =============================================================================
// 1. Loading Spinner with Pulsing Dots
// =============================================================================

/**
 * Three dots that pulse in sequence, commonly used as a loading indicator.
 *
 * Usage:
 *   PulsingDotsLoader()
 *   PulsingDotsLoader(dotSize = 10.dp, color = MaterialTheme.colorScheme.primary)
 */
@Composable
fun PulsingDotsLoader(
    modifier: Modifier = Modifier,
    dotCount: Int = 3,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dotsLoader")

    Row(
        modifier = modifier.semantics { contentDescription = stringResource(Res.string.cd_loading) },
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(dotCount) { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = AppTheme.motion.durationLong, delayMillis = index * AppTheme.motion.durationShort),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot$index",
            )
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.8f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = AppTheme.motion.durationLong, delayMillis = index * AppTheme.motion.durationShort),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dotScale$index",
            )

            Box(
                modifier = Modifier
                    .size(AppTheme.sizing.iconSm) // dot size
                    .graphicsLayer {
                        this.alpha = alpha
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

// =============================================================================
// 2. Rotating Loading Spinner
// =============================================================================

/**
 * A continuously rotating circular indicator.
 *
 * Usage:
 *   RotatingSpinner()
 */
@Composable
fun RotatingSpinner(
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spinner")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = AppTheme.motion.durationExtraLong, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )

    Box(
        modifier = modifier
            .size(AppTheme.sizing.iconMd)
            .graphicsLayer { rotationZ = rotation }
            .semantics { contentDescription = stringResource(Res.string.cd_loading) },
    ) {
        // Replace with your custom spinner drawable or composable
        androidx.compose.material3.CircularProgressIndicator(
            modifier = Modifier.size(AppTheme.sizing.iconMd),
            strokeWidth = AppTheme.sizing.strokeMedium,
        )
    }
}

// =============================================================================
// 3. Fade-In Staggered List
// =============================================================================

/**
 * A LazyColumn where each item fades and slides in with a staggered delay.
 * Items animate only on first appearance.
 *
 * Usage:
 *   FadeInStaggeredList(
 *       items = myList,
 *       itemContent = { item -> Text(item.name) },
 *   )
 */
@Composable
fun <T> FadeInStaggeredList(
    items: List<T>,
    modifier: Modifier = Modifier,
    staggerDelayMs: Long = 50L,
    itemContent: @Composable (T) -> Unit,
) {
    LazyColumn(modifier = modifier) {
        itemsIndexed(items, key = { index, _ -> index }) { index, item ->
            var isVisible by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                delay(index * staggerDelayMs)
                isVisible = true
            }

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(AppTheme.motion.durationMedium)) + slideInVertically(
                    initialOffsetY = { it / 4 },
                    animationSpec = tween(AppTheme.motion.durationMedium),
                ),
            ) {
                itemContent(item)
            }
        }
    }
}

// =============================================================================
// 4. Expandable Card
// =============================================================================

/**
 * A card that expands to reveal additional content with smooth animation.
 *
 * Usage:
 *   ExpandableCard(
 *       title = "Section Title",
 *       content = { Text("Hidden details here") },
 *   )
 */
@Composable
fun ExpandableCard(
    title: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit,
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            ),
        onClick = { isExpanded = !isExpanded },
    ) {
        Column(modifier = Modifier.padding(AppTheme.spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess
                                  else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) stringResource(Res.string.cd_collapse, title)
                                         else stringResource(Res.string.cd_expand, title),
                )
            }
            if (isExpanded) {
                Spacer(Modifier.height(AppTheme.spacing.md))
                content()
            }
        }
    }
}

// =============================================================================
// 5. Pulse Effect
// =============================================================================

/**
 * A composable that continuously pulses (scales up and down) to draw attention.
 * Commonly used for notification badges, call-to-action buttons, or live indicators.
 *
 * Usage:
 *   PulseEffect {
 *       Badge { Text("3") }
 *   }
 */
@Composable
fun PulseEffect(
    modifier: Modifier = Modifier,
    minScale: Float = 0.95f,
    maxScale: Float = 1.05f,
    durationMs: Int = 1000,
    content: @Composable () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    Box(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
    ) {
        content()
    }
}

// =============================================================================
// 6. Shimmer Loading Placeholder
// =============================================================================

/**
 * A shimmer effect for loading placeholders. Apply to boxes that represent
 * loading content.
 *
 * Usage:
 *   ShimmerBox(modifier = Modifier.fillMaxWidth().height(20.dp))
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = AppTheme.motion.durationExtraLong),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmerAlpha",
    )

    Box(
        modifier = modifier
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(AppTheme.corners.sm))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
    )
}

/**
 * A loading placeholder card with shimmer boxes mimicking content layout.
 *
 * Usage:
 *   ShimmerLoadingCard()
 */
@Composable
fun ShimmerLoadingCard(
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = AppTheme.sizing.strokeMedium),
    ) {
        Row(modifier = Modifier.padding(AppTheme.spacing.lg)) {
            // Avatar placeholder
            ShimmerBox(
                modifier = Modifier
                    .size(AppTheme.sizing.iconLg)
                    .clip(CircleShape),
            )
            Spacer(Modifier.width(AppTheme.spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                // Title placeholder
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(AppTheme.spacing.lg),
                )
                Spacer(Modifier.height(AppTheme.spacing.sm))
                // Subtitle placeholder
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(AppTheme.spacing.md),
                )
            }
        }
    }
}

// =============================================================================
// 7. Appear-on-Scroll Item Wrapper
// =============================================================================

/**
 * Wraps content with a fade+slide animation that triggers when the item
 * first becomes visible in a scrollable container.
 *
 * Usage:
 *   LazyColumn {
 *       items(list) { item ->
 *           AppearOnScroll {
 *               ItemCard(item)
 *           }
 *       }
 *   }
 */
@Composable
fun AppearOnScroll(
    modifier: Modifier = Modifier,
    delayMs: Long = 0L,
    content: @Composable () -> Unit,
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (delayMs > 0) delay(delayMs)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier,
        enter = fadeIn(tween(AppTheme.motion.durationMedium)) + slideInVertically(
            initialOffsetY = { it / 5 },
            animationSpec = tween(AppTheme.motion.durationMedium),
        ),
        exit = fadeOut(tween(AppTheme.motion.durationShort)),
    ) {
        content()
    }
}
