package eu.kanade.presentation.library.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.category.visualName
import kotlin.math.abs
import kotlinx.coroutines.launch
import tachiyomi.domain.category.model.Category

/**
 * Aurora-Style Top Shelf Selector Bar.
 *
 * Architecture:
 * - Renders directly on the live ambient background (no pill box/container).
 * - Continuous horizontal slide-and-scale motion: as the active center title moves
 *   to the side it shrinks and dims, while the incoming flank title grows and brightens.
 * - Center: Active shelf name in bold console typography (15.sp), squircle count badge, micro-indicator horizon line.
 * - Flanks: Dimmed adjacent previews (78% scale, 35% opacity), clickable to transition.
 * - Horizontal Drag Gestures directly on the bar with spring physics.
 */
@Composable
fun AuroraShelfSelectorBar(
    categories: List<Category>,
    pagerState: PagerState,
    getItemCountForCategory: (Category) -> Int?,
    onSelectCategory: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (categories.isEmpty()) return

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val dragOffsetAnimatable = remember { Animatable(0f) }
    val dragThresholdPx = with(density) { 36.dp.toPx() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .pointerInput(pagerState.currentPage, categories.size) {
                detectHorizontalDragGestures(
                    onDragStart = {},
                    onDragEnd = {
                        val dragVal = dragOffsetAnimatable.value
                        val curPage = pagerState.currentPage
                        if (dragVal < -dragThresholdPx && curPage < categories.lastIndex) {
                            onSelectCategory(curPage + 1)
                        } else if (dragVal > dragThresholdPx && curPage > 0) {
                            onSelectCategory(curPage - 1)
                        }
                        coroutineScope.launch {
                            dragOffsetAnimatable.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = 400f,
                                ),
                            )
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            dragOffsetAnimatable.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = 400f,
                                ),
                            )
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            dragOffsetAnimatable.snapTo(dragOffsetAnimatable.value + dragAmount)
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        val totalWidth = maxWidth
        val flankOffset = minOf(totalWidth * 0.36f, 140.dp)
        val flankOffsetPx = with(density) { flankOffset.toPx() }

        val pagerContinuous = pagerState.currentPage + pagerState.currentPageOffsetFraction
        val dragFraction = if (flankOffsetPx > 0f) dragOffsetAnimatable.value / flankOffsetPx else 0f
        val visualPosition = pagerContinuous - dragFraction

        val minIdx = (visualPosition.toInt() - 2).coerceAtLeast(0)
        val maxIdx = (visualPosition.toInt() + 2).coerceAtMost(categories.lastIndex)

        for (index in minIdx..maxIdx) {
            val category = categories[index]
            val offset = index.toFloat() - visualPosition
            val absOffset = abs(offset)
            if (absOffset > 1.8f) continue

            val translationXPx = offset * flankOffsetPx
            val scale = (1.0f - 0.22f * absOffset.coerceIn(0f, 1f)).coerceIn(0.75f, 1.0f)
            val alpha = when {
                absOffset <= 1.0f -> 1.0f - 0.65f * absOffset
                absOffset <= 1.8f -> (0.35f * (1.8f - absOffset) / 0.8f).coerceAtLeast(0f)
                else -> 0f
            }
            val centerDecorAlpha = (1.0f - absOffset * 2.2f).coerceIn(0f, 1f)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        this.translationX = translationXPx
                        this.scaleX = scale
                        this.scaleY = scale
                        this.alpha = alpha
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        if (absOffset > 0.35f) {
                            onSelectCategory(index)
                        }
                    },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = category.visualName,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 15.sp,
                            fontWeight = if (absOffset < 0.5f) FontWeight.SemiBold else FontWeight.Medium,
                            color = Color.White,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 140.dp),
                    )

                    val count = getItemCountForCategory(category)
                    if (count != null && count > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f * centerDecorAlpha),
                            modifier = Modifier.graphicsLayer { this.alpha = centerDecorAlpha },
                        ) {
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }

                // Micro-indicator horizon underglow bar beneath active shelf
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(width = 20.dp, height = 2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .graphicsLayer { this.alpha = centerDecorAlpha }
                        .background(Color.White.copy(alpha = 0.70f)),
                )
            }
        }
    }
}

@Composable
fun AuroraShelfSelectorBar(
    categories: List<Category>,
    currentPage: Int,
    getItemCountForCategory: (Category) -> Int?,
    onSelectCategory: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (categories.isEmpty()) return

    val animatablePosition = remember { Animatable(currentPage.toFloat()) }

    LaunchedEffect(currentPage) {
        animatablePosition.animateTo(
            targetValue = currentPage.toFloat(),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = 380f,
            ),
        )
    }

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val dragOffsetAnimatable = remember { Animatable(0f) }
    val dragThresholdPx = with(density) { 36.dp.toPx() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .pointerInput(currentPage, categories.size) {
                detectHorizontalDragGestures(
                    onDragStart = {},
                    onDragEnd = {
                        val dragVal = dragOffsetAnimatable.value
                        if (dragVal < -dragThresholdPx && currentPage < categories.lastIndex) {
                            onSelectCategory(currentPage + 1)
                        } else if (dragVal > dragThresholdPx && currentPage > 0) {
                            onSelectCategory(currentPage - 1)
                        }
                        coroutineScope.launch {
                            dragOffsetAnimatable.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = 400f,
                                ),
                            )
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            dragOffsetAnimatable.animateTo(0f)
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            dragOffsetAnimatable.snapTo(dragOffsetAnimatable.value + dragAmount)
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        val totalWidth = maxWidth
        val flankOffset = minOf(totalWidth * 0.36f, 140.dp)
        val flankOffsetPx = with(density) { flankOffset.toPx() }

        val dragFraction = if (flankOffsetPx > 0f) dragOffsetAnimatable.value / flankOffsetPx else 0f
        val visualPosition = animatablePosition.value - dragFraction

        val minIdx = (visualPosition.toInt() - 2).coerceAtLeast(0)
        val maxIdx = (visualPosition.toInt() + 2).coerceAtMost(categories.lastIndex)

        for (index in minIdx..maxIdx) {
            val category = categories[index]
            val offset = index.toFloat() - visualPosition
            val absOffset = abs(offset)
            if (absOffset > 1.8f) continue

            val translationXPx = offset * flankOffsetPx
            val scale = (1.0f - 0.22f * absOffset.coerceIn(0f, 1f)).coerceIn(0.75f, 1.0f)
            val alpha = when {
                absOffset <= 1.0f -> 1.0f - 0.65f * absOffset
                absOffset <= 1.8f -> (0.35f * (1.8f - absOffset) / 0.8f).coerceAtLeast(0f)
                else -> 0f
            }
            val centerDecorAlpha = (1.0f - absOffset * 2.2f).coerceIn(0f, 1f)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        this.translationX = translationXPx
                        this.scaleX = scale
                        this.scaleY = scale
                        this.alpha = alpha
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        if (absOffset > 0.35f) {
                            onSelectCategory(index)
                        }
                    },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = category.visualName,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 15.sp,
                            fontWeight = if (absOffset < 0.5f) FontWeight.SemiBold else FontWeight.Medium,
                            color = Color.White,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 140.dp),
                    )

                    val count = getItemCountForCategory(category)
                    if (count != null && count > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f * centerDecorAlpha),
                            modifier = Modifier.graphicsLayer { this.alpha = centerDecorAlpha },
                        ) {
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(width = 20.dp, height = 2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .graphicsLayer { this.alpha = centerDecorAlpha }
                        .background(Color.White.copy(alpha = 0.70f)),
                )
            }
        }
    }
}
