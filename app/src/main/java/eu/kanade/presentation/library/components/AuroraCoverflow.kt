package eu.kanade.presentation.library.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import eu.kanade.presentation.components.LocalLiveBackgroundController
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.tachiyomi.ui.library.LibraryItem
import kotlinx.coroutines.launch
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.roundedfilled.PlayArrow
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.MangaCover as DomainMangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.absoluteValue

val CoverflowHudHeight = 148.dp
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

    val bgController = LocalLiveBackgroundController.current
    DisposableEffect(pagerState, bgController) {
        bgController?.scrollOffsetProvider = {
            pagerState.currentPage + pagerState.currentPageOffsetFraction
        }
        onDispose {
            if (bgController?.scrollOffsetProvider != null) {
                bgController.scrollOffsetProvider = null
            }
        }
    }

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
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // Dedicated fixed Coverflow carousel area height: 100% decoupled from bottom HUD content
        val coverflowAreaHeight = (screenHeight - CoverflowHudHeight).coerceAtLeast(200.dp)

        // Aspect ratio 2f / 3f (height = width * 1.5f).
        // Total cover height with reflection = cardHeight + 6.dp + (cardHeight * 0.42f) = cardHeight * 1.42f + 6.dp.
        val maxCardHeightFromHeight = (coverflowAreaHeight - 6.dp) / 1.42f
        val maxCardWidthFromHeight = maxCardHeightFromHeight * (2f / 3f)
        val cardWidth = minOf(screenWidth * 0.50f, maxCardWidthFromHeight, 230.dp).coerceIn(140.dp, 280.dp)
        val cardHeight = cardWidth * 1.5f
        val horizontalPadding = ((screenWidth - cardWidth) / 2).coerceAtLeast(0.dp)

        val density = LocalDensity.current
        val cardWidthPx = with(density) { cardWidth.toPx() }
        val primaryColor = MaterialTheme.colorScheme.primary
        val cardCornerRadius = 12.dp
        val cardShape = RoundedCornerShape(cardCornerRadius)

        // 1. HorizontalPager Carousel in its own dedicated, fixed-height upper region
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(coverflowAreaHeight)
                .align(Alignment.TopCenter),
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

                // Distance from center page integer for stable z-index layering
                val pageDelta = (pagerState.currentPage - page).absoluteValue.coerceIn(0, 3)

                Column(
                    modifier = Modifier
                        .zIndex(10f - pageDelta.toFloat())
                        .graphicsLayer {
                            // Render-phase continuous 3D transforms: Zero recomposition overhead
                            val liveOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                            val liveAbsOffset = liveOffset.absoluteValue.coerceIn(0f, 3f)

                            this.cameraDistance = 16f * density.density

                            // 3D Y-Rotation: tilt inward toward center spine
                            val clampedOffset = liveOffset.coerceIn(-1f, 1f)
                            val easeFraction = FastOutSlowInEasing.transform(clampedOffset.absoluteValue)
                            this.rotationY = (if (clampedOffset >= 0f) 1f else -1f) * easeFraction * 38f

                            // Scale: center hero is 1.0f, down to 0.78f at abs==1, 0.68f at abs>=2
                            val scale = when {
                                liveAbsOffset <= 1f -> 1.0f - 0.22f * liveAbsOffset
                                liveAbsOffset <= 2f -> 0.78f - 0.10f * (liveAbsOffset - 1f)
                                else -> 0.68f
                            }
                            this.scaleX = scale
                            this.scaleY = scale

                            // Horizontal compression: pull flanking cards slightly closer toward center
                            this.translationX = liveOffset * cardWidthPx * 0.22f

                            // Depth dimming: center at 1.0f, dimming down to 0.55f at outer edges
                            this.alpha = 1.0f - (1.0f - 0.55f) * (liveAbsOffset / 2f).coerceIn(0f, 1f)
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
                            .pointerInput(page, pagerState.currentPage) {
                                detectTapGestures(
                                    onTap = {
                                        if (pagerState.isScrollInProgress) return@detectTapGestures

                                        if (page == pagerState.currentPage) {
                                            onClickManga(item.libraryManga)
                                        } else {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(
                                                    page = page,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                                        stiffness = Spring.StiffnessLow,
                                                    ),
                                                )
                                            }
                                        }
                                    },
                                    onLongPress = {
                                        if (pagerState.isScrollInProgress) return@detectTapGestures
                                        if (page == pagerState.currentPage) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onLongClickManga(item.libraryManga)
                                        }
                                    },
                                )
                            },
                    ) {
                        MangaCover.Book(
                            data = coverData,
                            modifier = Modifier.fillMaxSize(),
                            contentDescription = manga.title,
                            shape = cardShape,
                        )
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
                                alpha = 0.35f * (1f - (curAbs * 0.4f).coerceIn(0f, 1f)) * hudAlpha
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CoverflowHudHeight)
                .align(Alignment.BottomCenter)
                .graphicsLayer { alpha = hudAlpha }
                .padding(horizontal = 24.dp)
                .padding(bottom = 10.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            val isDark = isSystemInDarkTheme()

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                // Manga Title: Fixed height so 1-line or 2-lines never shift the layout
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(CoverflowTitleHeight)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = currentItem?.libraryManga?.manga?.title.orEmpty(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 24.sp,
                            color = if (isDark) Color.White else MaterialTheme.colorScheme.onBackground,
                            shadow = Shadow(
                                color = if (isDark) Color.Black.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.75f),
                                blurRadius = 5f,
                            ),
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(
                            interactionSource = null,
                            indication = null,
                            enabled = currentItem != null,
                            onClick = { currentItem?.let { onClickManga(it.libraryManga) } },
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Position Counter: Tabular Numerals positioned lower, right below title
                Text(
                    text = "${activeIndex + 1} / ${items.size}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = if (isDark) Color.White.copy(alpha = 0.55f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.60f),
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum",
                        letterSpacing = 0.5.sp,
                        shadow = Shadow(
                            color = if (isDark) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.5f),
                            blurRadius = 3f,
                        ),
                    ),
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Dynamic Metadata & Tactile Resume Action Row
                if (currentItem != null) {
                    val badges = currentItem.badges
                    val unreadCount = badges.unreadCount ?: 0L
                    val canResume = onClickContinueReading != null && unreadCount > 0L

                    val languageDisplayName = remember(badges.sourceLanguage, badges.isLocal) {
                        if (badges.isLocal) {
                            "Local"
                        } else if (badges.sourceLanguage.isNotEmpty()) {
                            try {
                                val loc = java.util.Locale.forLanguageTag(badges.sourceLanguage)
                                val name = loc.getDisplayLanguage(java.util.Locale.getDefault())
                                if (name.isNotBlank()) name.replaceFirstChar { it.uppercase() } else badges.sourceLanguage.uppercase()
                            } catch (_: Exception) {
                                badges.sourceLanguage.uppercase()
                            }
                        } else {
                            ""
                        }
                    }

                    val statTextStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = if (isDark) Color.White.copy(alpha = 0.88f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.88f),
                        shadow = Shadow(
                            color = if (isDark) Color.Black.copy(alpha = 0.70f) else Color.White.copy(alpha = 0.70f),
                            blurRadius = 4f,
                        ),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Stats placed directly on the background, stacked on top of each other (no boxes)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            horizontalAlignment = Alignment.Start,
                        ) {
                            if (badges.unreadCount != null) {
                                Text(
                                    text = "$unreadCount unread",
                                    style = statTextStyle,
                                )
                            }
                            if (badges.downloadCount != null) {
                                Text(
                                    text = "${badges.downloadCount} downloads",
                                    style = statTextStyle,
                                )
                            }
                            if (languageDisplayName.isNotEmpty()) {
                                Text(
                                    text = languageDisplayName,
                                    style = statTextStyle,
                                )
                            }
                        }

                        // Tactile Primary Resume Button on the side - larger & easy to click
                        if (canResume) {
                            Surface(
                                onClick = { onClickContinueReading(currentItem.libraryManga) },
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primary,
                                shadowElevation = 4.dp,
                                modifier = Modifier.height(48.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                ) {
                                    Icon(
                                        imageVector = MaterialSymbols.RoundedFilled.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(24.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(MR.strings.action_resume),
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 15.sp,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }

                // Global search affordance when filter query is active
                if (!searchQuery.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    GlobalSearchItem(
                        searchQuery = searchQuery,
                        onClick = onGlobalSearchClicked,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }
        }
    }
}
