package eu.kanade.presentation.library.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.tachiyomi.ui.library.LibraryItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.MangaCover as DomainMangaCover
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private data class MorphParams(
    val x: Float,
    val y: Float,
    val scale: Float,
    val rotY: Float,
    val alpha: Float,
)

/**
 * High-performance orchestrator for Coverflow <-> Grid library views.
 *
 * Mode Switching Animation Architecture:
 * - The 3 visible hero cards the user is currently looking at rearrange themselves immediately.
 * - The flanking cards smoothly untilt their 3D perspective and glide into their exact grid slots.
 * - Subsequent rows follow with a physical staggered wave (motion graphics style).
 * - Exact viewport metrics ensure zero teleportation or spatial discontinuity.
 */
@Composable
fun AuroraCoverflowAndGrid(
    items: List<LibraryItem>,
    isGridMode: Boolean,
    columns: Int,
    contentPadding: PaddingValues,
    selection: Set<Long>,
    onClickManga: (LibraryManga) -> Unit,
    onLongClickManga: (LibraryManga) -> Unit,
    onClickContinueReading: ((LibraryManga) -> Unit)?,
    searchQuery: String?,
    onGlobalSearchClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { items.size },
    )
    val gridState = rememberLazyGridState()

    var previousGridMode by remember { mutableStateOf(isGridMode) }
    var isTransitioning by remember { mutableStateOf(false) }

    // Map of animatable progress per item index (0f = Coverflow, 1f = Grid)
    val itemProgressMap = remember { mutableStateMapOf<Int, Animatable<Float, *>>() }

    // Synchronize Coverflow HUD / reflection fade
    val hudAlpha by animateFloatAsState(
        targetValue = if (isGridMode) 0f else 1f,
        animationSpec = tween(
            durationMillis = if (isGridMode) 100 else 220,
            delayMillis = if (isGridMode) 0 else 120,
            easing = FastOutSlowInEasing,
        ),
        label = "hudAlpha",
    )

    // Detect mode switches and execute the motion-graphics choreographed transition
    LaunchedEffect(isGridMode) {
        if (previousGridMode != isGridMode) {
            isTransitioning = true
            val target = if (isGridMode) 1f else 0f
            val colCount = if (columns > 0) columns else 3

            val centerIndex = if (isGridMode) {
                pagerState.currentPage.coerceIn(0, items.lastIndex)
            } else {
                gridState.firstVisibleItemIndex.coerceIn(0, items.lastIndex)
            }

            // The 3 hero cards visible in Coverflow
            val heroIndices = listOf(centerIndex - 1, centerIndex, centerIndex + 1)
                .filter { it in 0..items.lastIndex }
                .toSet()

            // Align grid scroll position with the hero row
            val firstVisibleRow = (centerIndex / colCount).coerceAtLeast(0)
            val firstVisibleItem = (firstVisibleRow * colCount).coerceIn(0, items.lastIndex)

            if (isGridMode) {
                gridState.scrollToItem(firstVisibleItem)
            } else {
                pagerState.scrollToPage(centerIndex)
            }

            // Visible range: hero row + up to 3 follower rows below (and 1 above if applicable)
            val minRow = (firstVisibleRow - 1).coerceAtLeast(0)
            val maxRow = (firstVisibleRow + 3).coerceAtMost(items.lastIndex / colCount)
            val minIdx = (minRow * colCount).coerceAtLeast(0)
            val maxIdx = ((maxRow + 1) * colCount - 1).coerceAtMost(items.lastIndex)

            // Motion-graphics deceleration easing (Android Emphasized Decelerate)
            val easeCurve = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

            for (idx in minIdx..maxIdx) {
                val isHero = idx in heroIndices
                val itemRow = idx / colCount
                val rowDistance = (itemRow - firstVisibleRow).absoluteValue

                // Stagger: Hero cards start immediately (0ms). Other rows follow smoothly (60ms, 120ms).
                val staggerDelay = if (isHero) {
                    0L
                } else {
                    (rowDistance * 60L).coerceIn(50L, 180L)
                }

                val animatable = itemProgressMap.getOrPut(idx) {
                    Animatable(if (isGridMode) 0f else 1f)
                }

                launch {
                    delay(staggerDelay)
                    animatable.animateTo(
                        targetValue = target,
                        animationSpec = tween(
                            durationMillis = if (isHero) 360 else 320,
                            easing = easeCurve,
                        ),
                    )
                }
            }

            // Allow the longest staggered animation to fully settle
            delay(460)
            previousGridMode = isGridMode
            isTransitioning = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (!isTransitioning) {
            // Settled State: Direct Interactive View
            if (!isGridMode) {
                AuroraCoverflow(
                    items = items,
                    contentPadding = contentPadding,
                    selection = selection,
                    onClickManga = onClickManga,
                    onLongClickManga = onLongClickManga,
                    onClickContinueReading = onClickContinueReading,
                    searchQuery = searchQuery,
                    onGlobalSearchClicked = onGlobalSearchClicked,
                    pagerState = pagerState,
                    hudAlpha = hudAlpha,
                )
            } else {
                CleanLibraryGrid(
                    items = items,
                    columns = columns,
                    contentPadding = contentPadding,
                    selection = selection,
                    onClick = onClickManga,
                    onLongClick = onLongClickManga,
                    searchQuery = searchQuery,
                    onGlobalSearchClicked = onGlobalSearchClicked,
                    gridState = gridState,
                )
            }
        } else {
            // Active Transitioning State: Choreographed Morphing Stage
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val screenWidth = maxWidth
                val screenHeight = maxHeight
                val density = LocalDensity.current
                val isDark = isSystemInDarkTheme()

                val screenWidthPx = with(density) { screenWidth.toPx() }

                // Coverflow metrics (strictly matching AuroraCoverflow decoupled geometry)
                val coverflowAreaHeight = (screenHeight - CoverflowHudHeight).coerceAtLeast(200.dp)
                val maxCardHeightFromHeight = (coverflowAreaHeight - 6.dp) / 1.42f
                val maxCardWidthFromHeight = maxCardHeightFromHeight * (2f / 3f)
                val cfCardWidth = minOf(screenWidth * 0.50f, maxCardWidthFromHeight, 230.dp).coerceIn(140.dp, 280.dp)
                val cfCardHeight = cfCardWidth * 1.5f
                val cfCardWidthPx = with(density) { cfCardWidth.toPx() }
                val cfCardHeightPx = with(density) { cfCardHeight.toPx() }
                val cfCenterXPx = screenWidthPx / 2f
                val cfCenterYPx = with(density) { (contentPadding.calculateTopPadding() + coverflowAreaHeight / 2f).toPx() }

                // Grid metrics (matching CleanLibraryGrid)
                val colCount = if (columns > 0) columns else 3
                val gridSpacingPx = with(density) { 12.dp.toPx() }
                val gridPaddingHorizontalPx = with(density) { 16.dp.toPx() }
                val gridPaddingTopPx = with(density) { (contentPadding.calculateTopPadding() + 12.dp).toPx() }
                val gridCardWidthPx = (screenWidthPx - 2 * gridPaddingHorizontalPx - (colCount - 1) * gridSpacingPx) / colCount
                val gridCardHeightPx = gridCardWidthPx * 1.5f
                val gridTitleHeightPx = with(density) { 38.dp.toPx() }
                val gridCellTotalHeightPx = gridCardHeightPx + gridTitleHeightPx

                val centerIndex = if (isGridMode) pagerState.currentPage else gridState.firstVisibleItemIndex
                val firstVisibleRow = (centerIndex / colCount).coerceAtLeast(0)
                val minRow = (firstVisibleRow - 1).coerceAtLeast(0)
                val maxRow = (firstVisibleRow + 3).coerceAtMost(items.lastIndex / colCount)
                val minIdx = (minRow * colCount).coerceAtLeast(0)
                val maxIdx = ((maxRow + 1) * colCount - 1).coerceAtMost(items.lastIndex)

                val heroIndices = listOf(centerIndex - 1, centerIndex, centerIndex + 1)
                    .filter { it in 0..items.lastIndex }
                    .toSet()

                for (i in minIdx..maxIdx) {
                    val item = items[i]
                    val manga = item.libraryManga.manga
                    val isSelected = manga.id in selection
                    val p = itemProgressMap[i]?.value ?: if (isGridMode) 0f else 1f
                    val isHero = i in heroIndices

                    val pageDelta = i - centerIndex
                    val itemRow = i / colCount
                    val itemCol = i % colCount
                    val visibleRow = itemRow - firstVisibleRow

                    // Grid target coordinates
                    val gridX = gridPaddingHorizontalPx + itemCol * (gridCardWidthPx + gridSpacingPx)
                    val gridY = gridPaddingTopPx + visibleRow * (gridCellTotalHeightPx + gridSpacingPx)
                    val gridScale = 1.0f
                    val gridRotY = 0f
                    val gridAlpha = 1.0f

                    // Coverflow source coordinates
                    val cfParams = if (isHero) {
                        when (pageDelta) {
                            0 -> {
                                val x = cfCenterXPx - cfCardWidthPx / 2f
                                val y = cfCenterYPx - cfCardHeightPx / 2f
                                MorphParams(x = x, y = y, scale = 1.0f, rotY = 0f, alpha = 1.0f)
                            }
                            -1 -> {
                                val x = cfCenterXPx - cfCardWidthPx / 2f - (cfCardWidthPx * 0.78f)
                                val y = cfCenterYPx - cfCardHeightPx / 2f
                                MorphParams(x = x, y = y, scale = 0.78f, rotY = 38f, alpha = 0.90f)
                            }
                            1 -> {
                                val x = cfCenterXPx - cfCardWidthPx / 2f + (cfCardWidthPx * 0.78f)
                                val y = cfCenterYPx - cfCardHeightPx / 2f
                                MorphParams(x = x, y = y, scale = 0.78f, rotY = -38f, alpha = 0.90f)
                            }
                            else -> {
                                val x = cfCenterXPx - cfCardWidthPx / 2f
                                val y = cfCenterYPx - cfCardHeightPx / 2f
                                MorphParams(x = x, y = y, scale = 0.68f, rotY = 0f, alpha = 0f)
                            }
                        }
                    } else {
                        // Follower items start slightly offset vertically and fade in
                        val startY = gridY + (if (itemRow > firstVisibleRow) with(density) { 45.dp.toPx() } else with(density) { (-45).dp.toPx() })
                        MorphParams(x = gridX, y = startY, scale = 0.92f, rotY = 0f, alpha = 0f)
                    }

                    // Interpolate at progress p (0f = Coverflow, 1f = Grid)
                    val currentX = (1f - p) * cfParams.x + p * gridX
                    val currentY = (1f - p) * cfParams.y + p * gridY
                    val currentWidthPx = (1f - p) * cfCardWidthPx + p * gridCardWidthPx
                    val currentHeightPx = (1f - p) * cfCardHeightPx + p * gridCardHeightPx
                    val currentRotY = (1f - p) * cfParams.rotY + p * gridRotY
                    val currentScale = (1f - p) * cfParams.scale + p * gridScale
                    val currentAlpha = (1f - p) * cfParams.alpha + p * gridAlpha
                    val currentCornerRadius = ((1f - p) * 12f + p * 10f).dp

                    // Z-index layering: hero center card on top, then flanking cards, then follower rows
                    val z = if (pageDelta == 0) 10f else if (isHero) 8f else (5f - visibleRow.toFloat()).coerceAtLeast(1f)

                    val coverData = DomainMangaCover(
                        mangaId = manga.id,
                        sourceId = manga.source,
                        isMangaFavorite = manga.favorite,
                        url = manga.thumbnailUrl,
                        lastModified = manga.coverLastModified,
                    )

                    Column(
                        modifier = Modifier
                            .offset { IntOffset(currentX.roundToInt(), currentY.roundToInt()) }
                            .width(with(density) { currentWidthPx.toDp() })
                            .zIndex(z)
                            .graphicsLayer {
                                this.cameraDistance = 16f * density.density
                                this.rotationY = currentRotY
                                this.scaleX = currentScale
                                this.scaleY = currentScale
                                this.alpha = currentAlpha
                            },
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(with(density) { currentHeightPx.toDp() }),
                            shape = RoundedCornerShape(currentCornerRadius),
                            color = Color.Transparent,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f),
                            ),
                        ) {
                            MangaCover.Book(
                                data = coverData,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(currentCornerRadius)),
                                contentDescription = manga.title,
                                shape = RoundedCornerShape(currentCornerRadius),
                            )
                        }

                        // Grid Title fades in as cards settle into grid positions (p > 0.4)
                        if (p > 0.1f) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = manga.title,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = (if (isDark) Color.White else MaterialTheme.colorScheme.onBackground)
                                        .copy(alpha = ((p - 0.1f) / 0.9f).coerceIn(0f, 0.88f)),
                                    fontWeight = FontWeight.Medium,
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}
