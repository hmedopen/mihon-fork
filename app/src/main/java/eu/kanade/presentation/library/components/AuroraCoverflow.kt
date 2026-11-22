package eu.kanade.presentation.library.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.tachiyomi.ui.library.LibraryItem
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.roundedfilled.PlayArrow
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.MangaCover as DomainMangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.absoluteValue

val CoverflowHudHeight = 164.dp
val CoverflowTitleHeight = 52.dp

/**
 * High-framerate 3D Xbox 360 Aurora-style Coverflow carousel in Jetpack Compose.
 *
 * Performance Architecture:
 * - Continuous 3D transforms (Y-rotation, scale, translation, alpha) and specular lighting
 *   are strictly computed inside Render/Draw phase blocks (`Modifier.graphicsLayer` and
 *   `Modifier.drawWithContent`) by reading `pagerState.currentPageOffsetFraction` without
 *   triggering Composable recompositions during 60/120fps swipes.
 */
@Composable
fun AuroraCoverflow(
    items: List<LibraryItem>,
    contentPadding: PaddingValues,
    selection: Set<Long>,
    onClickManga: (LibraryManga) -> Unit,
    onLongClickManga: (LibraryManga) -> Unit,
    onClickContinueReading: ((LibraryManga) -> Unit)?,
    searchQuery: String?,
    onGlobalSearchClicked: () -> Unit,
    modifier: Modifier = Modifier,
    pagerState: PagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { items.size },
    ),
    hudAlpha: Float = 1f,
) {
    if (items.isEmpty()) return

    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val coverCache = remember { context.appGraph.coverCache }

    // Aurora zoom gesture state (-1f for full zoom out with 5 covers, +1f for zoom in / 3D back cover flip)
    val zoomAnimatable = remember { Animatable(0f) }

    // Background remains fixed across swipes and shelf changes (Point 1)

    val flingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        pagerSnapDistance = PagerSnapDistance.atMost(30),
        decayAnimationSpec = rememberSplineBasedDecay(),
        snapAnimationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 75f,
        ),
    )

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val bottomBarPadding = contentPadding.calculateBottomPadding()

        // Dedicated fixed Coverflow carousel area height: 100% decoupled from bottom HUD content
        val coverflowAreaHeight = (screenHeight - CoverflowHudHeight - bottomBarPadding).coerceAtLeast(200.dp)

        // Aspect ratio 2f / 3f (height = width * 1.5f).
        // Total cover height with reflection = cardHeight + 6.dp + (cardHeight * 0.42f) = cardHeight * 1.42f + 6.dp.
        val maxCardHeightFromHeight = (coverflowAreaHeight - 6.dp) / 1.42f
        val maxCardWidthFromHeight = maxCardHeightFromHeight * (2f / 3f)
        val cardWidth = minOf(screenWidth * 0.50f, maxCardWidthFromHeight, 230.dp).coerceIn(140.dp, 280.dp)
        val cardHeight = cardWidth * 1.5f
        val horizontalPadding = ((screenWidth - cardWidth) / 2).coerceAtLeast(0.dp)

        val density = LocalDensity.current
        val cardWidthPx = with(density) { cardWidth.toPx() }
        val cardHeightPx = with(density) { cardHeight.toPx() }
        val primaryColor = MaterialTheme.colorScheme.primary
        val cardCornerRadius = 12.dp
        val cardShape = RoundedCornerShape(cardCornerRadius)

        // 1. HorizontalPager Carousel in its own dedicated, fixed-height upper region
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(coverflowAreaHeight)
                .align(Alignment.TopCenter)
                // Aurora Gestures (Point 8):
                // Pull down to zoom in / 3D flip; Pull up to zoom out (showing 5 covers);
                // clicking covers while zoomed out centers them smoothly without resetting zoom until release.
                .pointerInput(Unit) {
                    val touchSlop = viewConfiguration.touchSlop
                    val dragThreshold = 240.dp.toPx()

                    awaitEachGesture {
                        val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                        var totalDy = 0f
                        var totalDx = 0f
                        var isVerticalDrag = false
                        val activePointerId = down.id

                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            val pointer = event.changes.firstOrNull { it.id == activePointerId } ?: break
                            if (!pointer.pressed) break

                            val dy = pointer.position.y - pointer.previousPosition.y
                            val dx = pointer.position.x - pointer.previousPosition.x
                            totalDy += dy
                            totalDx += dx

                            if (!isVerticalDrag) {
                                if (totalDy.absoluteValue > touchSlop && totalDy.absoluteValue > totalDx.absoluteValue * 1.2f) {
                                    isVerticalDrag = true
                                    pointer.consume()
                                }
                            }

                            if (isVerticalDrag) {
                                pointer.consume()
                                val targetZoom = (totalDy / dragThreshold).coerceIn(-1f, 1f)
                                coroutineScope.launch {
                                    zoomAnimatable.snapTo(targetZoom)
                                }
                            }
                        }

                        // Released: spring back to normal!
                        coroutineScope.launch {
                            zoomAnimatable.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = 320f,
                                ),
                            )
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = horizontalPadding),
                beyondViewportPageCount = 5,
                flingBehavior = flingBehavior,
                pageSpacing = 0.dp,
                verticalAlignment = Alignment.CenterVertically,
            ) { page ->
                val item = items.getOrNull(page) ?: return@HorizontalPager
                val manga = item.libraryManga.manga
                val isSelected = manga.id in selection

                // Domain MangaCover model for Coil image loading
                val coverData = DomainMangaCover(
                    mangaId = manga.id,
                    sourceId = manga.source,
                    isMangaFavorite = manga.favorite,
                    url = manga.thumbnailUrl,
                    lastModified = manga.coverLastModified,
                )

                val hasBackCover = remember(manga.id) { coverCache.hasBackCover(manga.id) }
                val backCoverFile = remember(manga.id, hasBackCover) {
                    if (hasBackCover) coverCache.getBackCoverFile(manga.id) else null
                }

                // Distance from center page integer for stable z-index layering
                val pageDelta = (pagerState.currentPage - page).absoluteValue.coerceIn(0, 3)

                Column(
                    modifier = Modifier
                        .zIndex(10f - pageDelta.toFloat())
                        .graphicsLayer {
                            // Render-phase continuous 3D transforms: Zero recomposition overhead
                            val liveOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                            val liveAbsOffset = liveOffset.absoluteValue.coerceIn(0f, 3f)
                            val zoom = zoomAnimatable.value
                            val zoomIn = zoom.coerceIn(0f, 1f)
                            val zoomOut = (-zoom).coerceIn(0f, 1f)

                            this.cameraDistance = 18f * density.density

                            // 3D Y-Rotation: tilt inward toward center spine; completely flat (0 deg) when zoomed out
                            val clampedOffset = liveOffset.coerceIn(-1f, 1f)
                            val easeFraction = FastOutSlowInEasing.transform(clampedOffset.absoluteValue)
                            val normalRotationY = (if (clampedOffset >= 0f) 1f else -1f) * easeFraction * 38f * (1f - zoomOut)

                            if (liveAbsOffset < 0.5f && zoomIn > 0f) {
                                if (hasBackCover) {
                                    // 3D Card Flip to show back cover (Point 8.5)
                                    this.rotationY = normalRotationY * (1f - zoomIn) + (180f * zoomIn)
                                } else {
                                    // Standard zoom in: keep facing camera
                                    this.rotationY = normalRotationY * (1f - zoomIn * 0.5f)
                                }
                            } else {
                                this.rotationY = normalRotationY
                            }

                            // Scale:
                            // Normal mode: center is 1.0f, down to 0.78f, then 0.68f
                            // Zoom-out mode: all 5 covers become the exact same size (0.58f)
                            val normalScale = when {
                                liveAbsOffset <= 1f -> 1.0f - 0.22f * liveAbsOffset
                                liveAbsOffset <= 2f -> 0.78f - 0.10f * (liveAbsOffset - 1f)
                                else -> 0.68f
                            }
                            val zoomedOutScale = 0.58f
                            var scale = normalScale * (1f - zoomOut) + zoomedOutScale * zoomOut

                            if (zoomIn > 0f) {
                                if (liveAbsOffset < 0.5f) {
                                    scale = if (hasBackCover) {
                                        normalScale * (1.0f + 0.35f * zoomIn)
                                    } else {
                                        normalScale * (1.0f + 0.48f * zoomIn)
                                    }
                                } else {
                                    scale = normalScale * (1.0f - 0.35f * zoomIn)
                                }
                            }
                            this.scaleX = scale
                            this.scaleY = scale

                            // Horizontal compression: pull flanking cards into view for 5 covers
                            val compression = 0.22f + 0.36f * zoomOut - 0.08f * zoomIn
                            this.translationX = liveOffset * cardWidthPx * compression

                            // Vertical descent when zoomed in:
                            // Center-scaling expands the card upwards by (scale - normalScale) * cardHeightPx / 2.
                            // We offset this upward expansion plus add downward descent clearance so the top edge
                            // of the card descends safely below any top bars / clipping boundaries, showing the full cover.
                            if (zoomIn > 0f && liveAbsOffset < 0.5f) {
                                val upwardScaleExpansion = (scale - normalScale) * cardHeightPx / 2f
                                this.translationY = upwardScaleExpansion + with(density) { 36.dp.toPx() } * zoomIn
                            } else {
                                this.translationY = 0f
                            }

                            // Depth dimming & visibility: all 5 covers are bright and visible when zoomed out
                            val normalAlpha = 1.0f - (1.0f - 0.55f) * (liveAbsOffset / 2f).coerceIn(0f, 1f)
                            val zoomedOutAlpha = 1.0f - 0.15f * (liveAbsOffset / 2f).coerceIn(0f, 1f)
                            var cardAlpha = normalAlpha * (1f - zoomOut) + zoomedOutAlpha * zoomOut
                            if (zoomIn > 0f && liveAbsOffset >= 0.5f) {
                                cardAlpha *= (1f - 0.65f * zoomIn)
                            }
                            this.alpha = cardAlpha
                        }
                        .width(cardWidth),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Main Cover Box
                    Box(
                        modifier = Modifier
                            .size(cardWidth, cardHeight)
                            .clip(cardShape)
                            .drawWithContent {
                                drawContent()
                                // Specular border computed inside Draw phase
                                val liveOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                val liveAbs = liveOffset.absoluteValue.coerceIn(0f, 3f)
                                val specularAlpha = if (liveAbs < 0.5f) {
                                    0.25f - (0.25f - 0.08f) * (liveAbs * 2f)
                                } else {
                                    0.08f
                                }
                                val strokeWidth = if (isSelected) 2.dp.toPx() else 1.dp.toPx()
                                val cornerRadius = cardCornerRadius.toPx()
                                val halfStroke = strokeWidth / 2f
                                drawRoundRect(
                                    color = if (isSelected) primaryColor else Color.White.copy(alpha = specularAlpha),
                                    topLeft = Offset(halfStroke, halfStroke),
                                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                                    cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                                    style = Stroke(width = strokeWidth),
                                )
                            }
                            // Responsive, instant tap handling (Point 7 & Point 8)
                            .pointerInput(page, pagerState.currentPage) {
                                detectTapGestures(
                                    onTap = {
                                        val liveOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                        val isCenter = liveOffset.absoluteValue < 0.45f
                                        val isZoomedOut = zoomAnimatable.value < -0.15f

                                        if (isZoomedOut) {
                                            // Point 8: While zoomed out, clicking any cover moves it to center smoothly
                                            // without breaking the zoom out while held!
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(
                                                    page = page,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                                        stiffness = 380f,
                                                    ),
                                                )
                                            }
                                        } else if (isCenter) {
                                            // Point 7: Center cover opens immediately!
                                            onClickManga(item.libraryManga)
                                        } else {
                                            // Point 7: Right/left flanking cover brings to middle right away with no waiting and smoothly!
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(
                                                    page = page,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                                        stiffness = 380f,
                                                    ),
                                                )
                                            }
                                        }
                                    },
                                    onLongPress = {
                                        val liveOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                        if (liveOffset.absoluteValue < 0.45f) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onLongClickManga(item.libraryManga)
                                        }
                                    },
                                )
                            },
                    ) {
                        val liveOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        val zoomIn = zoomAnimatable.value.coerceIn(0f, 1f)
                        val isFlippedToBack = hasBackCover && liveOffset.absoluteValue < 0.5f && (180f * zoomIn) >= 90f

                        if (isFlippedToBack && backCoverFile != null) {
                            // Back cover view (Point 8.5)
                            AsyncImage(
                                model = backCoverFile,
                                contentDescription = "${manga.title} back cover",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(cardShape)
                                    .graphicsLayer {
                                        // Counteract parent Y-flip so image is right-side up
                                        rotationY = 180f
                                    },
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            // Front cover view
                            MangaCover.Book(
                                data = coverData,
                                modifier = Modifier.fillMaxSize(),
                                contentDescription = manga.title,
                                shape = cardShape,
                            )
                        }
                    }

                    // Spacer between cover and reflection
                    Spacer(modifier = Modifier.height(6.dp))

                    // Locked 3D Floor Reflection
                    Box(
                        modifier = Modifier
                            .width(cardWidth)
                            .height(cardHeight * 0.42f)
                            .clip(cardShape)
                            .graphicsLayer {
                                shape = cardShape
                                clip = true
                                compositingStrategy = CompositingStrategy.Offscreen
                                val liveOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                val curAbs = liveOffset.absoluteValue.coerceIn(0f, 3f)
                                val zoomIn = zoomAnimatable.value.coerceIn(0f, 1f)
                                alpha = 0.35f * (1f - (curAbs * 0.4f).coerceIn(0f, 1f)) * hudAlpha * (1f - zoomIn)
                            }
                            .drawWithContent {
                                drawContent()
                                val cornerRadiusPx = cardCornerRadius.toPx()
                                // BlendMode.DstIn dissolves reflection into dark floor at 75% height
                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        0.0f to Color.Black,
                                        0.75f to Color.Transparent,
                                        1.0f to Color.Transparent,
                                    ),
                                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                                    blendMode = BlendMode.DstIn,
                                )
                            },
                    ) {
                        AsyncImage(
                            model = coverData,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(cardShape)
                                .graphicsLayer { scaleY = -1f },
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.BottomCenter,
                        )
                    }
                }
            }
        }

        // 2. Fixed Bottom Aurora HUD (Completely decoupled from Carousel height)
        val activeIndex = pagerState.currentPage.coerceIn(0, items.lastIndex)
        val currentItem = items.getOrNull(activeIndex)
        val zoomIn = zoomAnimatable.value.coerceIn(0f, 1f)

        AuroraShelfHud(
            item = currentItem,
            totalItems = items.size,
            currentIndex = activeIndex,
            onClickManga = onClickManga,
            onClickContinueReading = onClickContinueReading,
            contentPadding = contentPadding,
            searchQuery = searchQuery,
            onGlobalSearchClicked = onGlobalSearchClicked,
            hudAlpha = hudAlpha * (1f - zoomIn * 0.65f),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
