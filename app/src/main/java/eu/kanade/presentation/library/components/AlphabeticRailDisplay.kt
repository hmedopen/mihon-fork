package eu.kanade.presentation.library.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.tachiyomi.ui.library.LibraryItem
import kotlinx.coroutines.launch
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.MangaCover as DomainMangaCover
import kotlin.math.abs
import kotlin.math.roundToInt

// Alphabet scrub rail order: 'A'..'Z' with '#' placed at the bottom
private val ALPHABET = ('A'..'Z').map { it.toString() } + listOf("#")

private data class LetterGroup(
    val letter: String,
    val items: List<LibraryItem>,
)

/**
 * Polished Alphabetic Index Rail with 5-Row Continuous Perspective Stage.
 *
 * Architecture:
 * - Groups manga catalog into alphabetical sections ('A'..'Z', ending with '#').
 * - 5-Row Perspective Stage: 2 preview rows above, 1 center active row, 2 preview rows below.
 * - All 4 preview rows share the exact same small size and clean letter chips.
 * - Continuous vertical translation and zoom interpolation during drag / scrub gestures.
 * - Center Active Row: Snapping horizontal carousel with prominent hero card, left/right cover peek.
 * - Bottom HUD: Shared AuroraShelfHud showing highlighted manga title, position, stacked stats, and tactile Resume button.
 * - Alphabet Scrub Rail: Wider, fluid wave thumb displacement with haptic feedback.
 */
@Composable
fun AlphabeticRailDisplay(
    items: List<LibraryItem>,
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

    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    // 1. Group items alphabetically ('A'..'Z' then '#')
    val groupedSections = remember(items) {
        val map = LinkedHashMap<String, MutableList<LibraryItem>>()
        for (item in items) {
            val title = item.libraryManga.manga.title.trim()
            val firstChar = title.firstOrNull()?.uppercaseChar() ?: '#'
            val key = if (firstChar in 'A'..'Z') firstChar.toString() else "#"
            map.getOrPut(key) { mutableListOf() }.add(item)
        }

        val list = mutableListOf<LetterGroup>()
        for (letter in ALPHABET) {
            val sectionItems = map[letter]
            if (!sectionItems.isNullOrEmpty()) {
                list.add(LetterGroup(letter, sectionItems))
            }
        }
        if (list.isEmpty()) {
            list.add(LetterGroup("#", items))
        }
        list
    }

    val sectionCount = groupedSections.size

    // Continuous vertical section index as an Animatable float
    val activeSectionFloat = remember { Animatable(0f) }
    var isFlinging by remember { mutableStateOf(false) }

    val activeSectionIndex by remember {
        derivedStateOf {
            activeSectionFloat.value.roundToInt().coerceIn(0, sectionCount - 1)
        }
    }

    // Velocity tracker for momentum vertical flings
    val velocityTracker = remember { VelocityTracker() }

    // Pinch-to-zoom density scale factor [0.85f .. 1.25f]
    var zoomDensity by remember { mutableFloatStateOf(1.0f) }
    val transformState = rememberTransformableState { zoomChange, _, _ ->
        zoomDensity = (zoomDensity * zoomChange).coerceIn(0.85f, 1.25f)
    }

    // Haptic feedback tick when scrub crosses a letter boundary
    var lastHapticLetter by remember { mutableStateOf("") }
    LaunchedEffect(activeSectionIndex) {
        val letter = groupedSections.getOrNull(activeSectionIndex)?.letter ?: ""
        if (letter.isNotEmpty() && letter != lastHapticLetter) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            lastHapticLetter = letter
        }
    }

    // Currently focused hero manga inside the active row
    var currentHeroItem by remember {
        mutableStateOf(groupedSections.getOrNull(0)?.items?.firstOrNull())
    }
    var currentHeroIndex by remember { mutableIntStateOf(0) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .transformable(state = transformState),
    ) {
        val screenHeight = maxHeight
        val screenWidth = maxWidth

        val centerCardWidth = (120.dp * zoomDensity).coerceIn(100.dp, 160.dp)
        val centerCardHeight = centerCardWidth * 1.5f

        val previewCardWidth = 32.dp * zoomDensity
        val previewCardHeight = previewCardWidth * 1.5f

        // Available height dedicated to the 5-row perspective stage
        val bottomHudTotalHeight = ShelfHudHeight + contentPadding.calculateBottomPadding()
        val stageHeight = (screenHeight - bottomHudTotalHeight - contentPadding.calculateTopPadding() - 16.dp).coerceAtLeast(300.dp)

        val verticalStepPx = with(density) { 110.dp.toPx() }

        // Vertical Drag Gestures (swiping up/down to slide rows smoothly with spring settling)
        val stageDragModifier = Modifier
            .fillMaxWidth()
            .height(stageHeight)
            .padding(end = 44.dp) // clearance for alphabet scrub rail
            .pointerInput(sectionCount) {
                awaitEachGesture {
                    val down = awaitFirstDown(pass = PointerEventPass.Initial)
                    if (isFlinging || activeSectionFloat.isRunning) {
                        scope.launch { activeSectionFloat.stop() }
                        isFlinging = false
                    }
                    velocityTracker.resetTracking()
                    velocityTracker.addPosition(down.uptimeMillis, down.position)
                }
            }
            .pointerInput(sectionCount, verticalStepPx) {
                detectVerticalDragGestures(
                    onDragStart = { isFlinging = true },
                    onDragEnd = {
                        val velocity = velocityTracker.calculateVelocity().y
                        val flingSlots = -(velocity / verticalStepPx) * 0.22f
                        val targetIndex = (activeSectionFloat.value + flingSlots).roundToInt().coerceIn(0, sectionCount - 1)
                        scope.launch {
                            isFlinging = true
                            activeSectionFloat.animateTo(
                                targetValue = targetIndex.toFloat(),
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessLow,
                                ),
                            )
                            isFlinging = false
                        }
                    },
                    onDragCancel = {
                        val targetIndex = activeSectionFloat.value.roundToInt().coerceIn(0, sectionCount - 1)
                        scope.launch {
                            isFlinging = true
                            activeSectionFloat.animateTo(
                                targetValue = targetIndex.toFloat(),
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessLow,
                                ),
                            )
                            isFlinging = false
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        val delta = -(dragAmount / verticalStepPx)
                        scope.launch {
                            val next = (activeSectionFloat.value + delta).coerceIn(0f, (sectionCount - 1).toFloat())
                            activeSectionFloat.snapTo(next)
                        }
                    },
                )
            }

        // =========================================================================
        // 5-ROW CONTINUOUS PERSPECTIVE STAGE
        // =========================================================================
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = contentPadding.calculateTopPadding() + 8.dp)
                .then(stageDragModifier),
            contentAlignment = Alignment.Center,
        ) {
            val currCenter = activeSectionIndex

            // Evaluate 5 visible rows around center: [curr - 2 .. curr + 2]
            val visibleIndices = (currCenter - 2..currCenter + 2).filter { it in 0 until sectionCount }

            for (idx in visibleIndices) {
                val group = groupedSections[idx]
                val d = idx - activeSectionFloat.value // Continuous distance from center [-2.5 .. 2.5]
                val absD = abs(d)

                if (absD <= 2.6f) {
                    val isCenterRow = idx == currCenter

                    // Continuous vertical positioning:
                    // Step 1: 115dp from center; Step 2: 64dp beyond step 1
                    val step1Dp = 115.dp
                    val step2Dp = 64.dp
                    val yOffsetDp = when {
                        d >= 0f && d <= 1f -> step1Dp * d
                        d > 1f -> step1Dp + step2Dp * (d - 1f)
                        d < 0f && d >= -1f -> step1Dp * d
                        else -> -step1Dp + step2Dp * (d + 1f)
                    }

                    // Continuous zoom scale:
                    // Center is 1.0f. Preview rows are uniformly 0.62f.
                    val scale = if (absD <= 1f) {
                        1.0f - (1.0f - 0.62f) * absD
                    } else {
                        0.62f
                    }

                    // Continuous alpha:
                    val rowAlpha = when {
                        absD <= 0.5f -> 1.0f
                        absD <= 1f -> 1.0f - 0.55f * ((absD - 0.5f) / 0.5f)
                        absD <= 2f -> 0.45f - 0.15f * (absD - 1f)
                        else -> (0.30f * (2.6f - absD) / 0.6f).coerceAtLeast(0f)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(10f - absD)
                            .graphicsLayer {
                                translationY = with(density) { yOffsetDp.toPx() }
                                scaleX = scale
                                scaleY = scale
                                alpha = rowAlpha
                            },
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (isCenterRow) {
                            CenterActiveRow(
                                group = group,
                                cardWidth = centerCardWidth,
                                cardHeight = centerCardHeight,
                                selection = selection,
                                onHeroMangaChanged = { heroManga, heroIdx ->
                                    currentHeroItem = heroManga
                                    currentHeroIndex = heroIdx
                                },
                                onClickManga = {
                                    if (!isFlinging) onClickManga(it)
                                },
                                onLongClickManga = onLongClickManga,
                                isDark = isDark,
                            )
                        } else {
                            PerspectiveContextRow(
                                group = group,
                                cardWidth = previewCardWidth,
                                cardHeight = previewCardHeight,
                                onClickSection = {
                                    scope.launch {
                                        activeSectionFloat.animateTo(
                                            targetValue = idx.toFloat(),
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessLow,
                                            ),
                                        )
                                    }
                                },
                                isDark = isDark,
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // VERTICAL ALPHABET SCRUB RAIL (WIDER, REORDERED: '#' AT BOTTOM)
        // =========================================================================
        AlphabetScrubRail(
            activeLetter = groupedSections.getOrNull(activeSectionIndex)?.letter ?: "A",
            onSelectLetter = { letter ->
                val foundIdx = groupedSections.indexOfFirst { it.letter == letter }
                if (foundIdx >= 0) {
                    scope.launch {
                        activeSectionFloat.animateTo(
                            targetValue = foundIdx.toFloat(),
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessLow,
                            ),
                        )
                    }
                }
            },
            contentPadding = contentPadding,
            modifier = Modifier.align(Alignment.CenterEnd),
        )

        // =========================================================================
        // SHARED TACTILE BOTTOM SHELF HUD
        // =========================================================================
        val activeGroup = groupedSections.getOrNull(activeSectionIndex)
        val activeGroupSize = activeGroup?.items?.size ?: 0
        val displayedHero = currentHeroItem ?: activeGroup?.items?.firstOrNull()

        AuroraShelfHud(
            item = displayedHero,
            totalItems = activeGroupSize,
            currentIndex = currentHeroIndex.coerceIn(0, (activeGroupSize - 1).coerceAtLeast(0)),
            onClickManga = onClickManga,
            onClickContinueReading = onClickContinueReading,
            contentPadding = contentPadding,
            searchQuery = searchQuery,
            onGlobalSearchClicked = onGlobalSearchClicked,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/**
 * Center Active Stage Row:
 * - Snapping HorizontalPager carousel.
 * - Center highlighted manga is larger (1.0x scale, elevation shadow, subtle specular border).
 * - Previous manga cover peeks on the left side (~32dp visible) to signal prior items.
 * - Flanking items scale down to 0.84x.
 * - Clean letter bubble with no title count text.
 */
@Composable
private fun CenterActiveRow(
    group: LetterGroup,
    cardWidth: androidx.compose.ui.unit.Dp,
    cardHeight: androidx.compose.ui.unit.Dp,
    selection: Set<Long>,
    onHeroMangaChanged: (LibraryItem, Int) -> Unit,
    onClickManga: (LibraryManga) -> Unit,
    onLongClickManga: (LibraryManga) -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val totalItems = group.items.size

    key(group.letter) {
        val pagerState = rememberPagerState(
            initialPage = 0,
            pageCount = { totalItems },
        )

        LaunchedEffect(pagerState.currentPage, totalItems) {
            val item = group.items.getOrNull(pagerState.currentPage)
            if (item != null) {
                onHeroMangaChanged(item, pagerState.currentPage)
            }
        }

        Column(modifier = modifier.fillMaxWidth()) {
            // Section Header: letter chip only (no "X titles" text)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp),
                    shadowElevation = 2.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = group.letter,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 15.sp,
                            ),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Snapping HorizontalPager:
            // contentPadding provides ~32dp peek of the previous manga on the left edge
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight + 14.dp),
                contentPadding = PaddingValues(start = 44.dp, end = 44.dp),
                pageSpacing = 12.dp,
                flingBehavior = PagerDefaults.flingBehavior(
                    state = pagerState,
                    pagerSnapDistance = PagerSnapDistance.atMost(10),
                    snapAnimationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) { page ->
                val item = group.items.getOrNull(page) ?: return@HorizontalPager
                val manga = item.libraryManga.manga
                val isSelected = selection.contains(manga.id)

                val domainCover = remember(manga.id, manga.thumbnailUrl, manga.favorite) {
                    DomainMangaCover(
                        mangaId = manga.id,
                        sourceId = manga.source,
                        isMangaFavorite = manga.favorite,
                        url = manga.thumbnailUrl,
                        lastModified = manga.coverLastModified,
                    )
                }

                // Render-phase page offset calculation: center item is larger
                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).coerceIn(-2f, 2f)
                val absOffset = abs(pageOffset).coerceIn(0f, 1f)
                val isCenter = absOffset < 0.25f

                val cardScale = 1.0f - 0.16f * absOffset
                val cardAlpha = 1.0f - 0.25f * absOffset

                Box(
                    modifier = Modifier
                        .size(width = cardWidth, height = cardHeight)
                        .graphicsLayer {
                            scaleX = cardScale
                            scaleY = cardScale
                            alpha = cardAlpha
                            shadowElevation = if (isCenter) 10.dp.toPx() else 2.dp.toPx()
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) {
                            if (page == pagerState.currentPage) {
                                onClickManga(item.libraryManga)
                            } else {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(page)
                                }
                            }
                        },
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.35f),
                        border = BorderStroke(
                            width = if (isSelected) 2.5.dp else if (isCenter) 1.5.dp else 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else if (isCenter) {
                                Color.White.copy(alpha = 0.35f)
                            } else {
                                Color.White.copy(alpha = 0.12f)
                            },
                        ),
                    ) {
                        MangaCover.Book(
                            data = domainCover,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    // Subtle unread badge in top-start corner
                    if (item.libraryManga.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            contentAlignment = Alignment.TopStart,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary,
                            ) {
                                Text(
                                    text = "${item.libraryManga.unreadCount}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Perspective Context Row (preview rows):
 * - Uniform small card dimensions (same exact size for top 2 and bottom 2).
 * - Clean letter bubble (no title count text).
 * - Clickable to smoothly animate this section into center.
 */
@Composable
private fun PerspectiveContextRow(
    group: LetterGroup?,
    cardWidth: androidx.compose.ui.unit.Dp,
    cardHeight: androidx.compose.ui.unit.Dp,
    onClickSection: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    if (group == null) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { onClickSection() }
            .padding(vertical = 2.dp),
    ) {
        // Section Header: letter chip only (no "• X" title count)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = if (isDark) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(22.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = group.letter,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                        ),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Horizontal Row of small uniform preview covers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (item in group.items.take(7)) {
                val manga = item.libraryManga.manga
                val domainCover = remember(manga.id, manga.thumbnailUrl, manga.favorite) {
                    DomainMangaCover(
                        mangaId = manga.id,
                        sourceId = manga.source,
                        isMangaFavorite = manga.favorite,
                        url = manga.thumbnailUrl,
                        lastModified = manga.coverLastModified,
                    )
                }

                Surface(
                    modifier = Modifier.size(width = cardWidth, height = cardHeight),
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.30f),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.12f)),
                ) {
                    MangaCover.Book(
                        data = domainCover,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

/**
 * Vertical Alphabet Scrub Rail:
 * - Wider touch area (44.dp) and comfortable letter sizing (28.dp).
 * - Order: 'A'..'Z' followed by '#' at the bottom.
 * - Sinusoidal / Gaussian wave thumb displacement curves outward smoothly.
 * - Haptic feedback ticks on crossing letter boundaries.
 */
@Composable
private fun AlphabetScrubRail(
    activeLetter: String,
    onSelectLetter: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var touchY by remember { mutableFloatStateOf(-1000f) }
    var isTouching by remember { mutableStateOf(false) }

    // Map letter to its vertical center Y in parent coordinates
    val letterCenterYMap = remember { mutableMapOf<String, Float>() }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(44.dp)
            .padding(
                top = contentPadding.calculateTopPadding() + 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 12.dp,
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(pass = PointerEventPass.Initial)
                    touchY = down.position.y
                    isTouching = true

                    val closest = letterCenterYMap.minByOrNull { abs(it.value - touchY) }
                    if (closest != null) {
                        onSelectLetter(closest.key)
                    }

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val drag = event.changes.firstOrNull { it.pressed }
                        if (drag != null) {
                            touchY = drag.position.y
                            val nearest = letterCenterYMap.minByOrNull { abs(it.value - touchY) }
                            if (nearest != null && nearest.key != activeLetter) {
                                onSelectLetter(nearest.key)
                            }
                        } else {
                            isTouching = false
                            touchY = -1000f
                            break
                        }
                    }
                }
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val radiusPx = with(LocalDensity.current) { 68.dp.toPx() }

        for (letter in ALPHABET) {
            val isCurrent = letter == activeLetter

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .onGloballyPositioned { coords ->
                        val centerY = coords.boundsInParent().center.y
                        letterCenterYMap[letter] = centerY
                    }
                    .graphicsLayer {
                        // Render-phase wave calculation
                        if (isTouching && touchY > -500f) {
                            val centerY = letterCenterYMap[letter] ?: 0f
                            val dist = abs(centerY - touchY)
                            if (dist < radiusPx) {
                                val factor = (1f - (dist / radiusPx)).coerceIn(0f, 1f)
                                val wave = factor * factor
                                // Curves outward toward thumb by up to 26dp
                                translationX = -26.dp.toPx() * wave
                                scaleX = 0.88f + 0.57f * wave // up to 1.45x
                                scaleY = scaleX
                                alpha = 0.45f + 0.55f * wave
                            } else {
                                translationX = 0f
                                scaleX = 0.88f
                                scaleY = 0.88f
                                alpha = 0.45f
                            }
                        } else {
                            translationX = 0f
                            scaleX = if (isCurrent) 1.25f else 0.88f
                            scaleY = scaleX
                            alpha = if (isCurrent) 1.0f else 0.45f
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = letter,
                    fontSize = 12.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
