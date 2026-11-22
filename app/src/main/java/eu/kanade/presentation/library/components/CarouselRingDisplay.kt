package eu.kanade.presentation.library.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.roundedfilled.PlayArrow
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.MangaCover as DomainMangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 3D Infinite Cylinder / Carousel Ring Display Mode.
 *
 * Architecture & Physics:
 * - Arranges all manga covers along an infinite cylindrical ring in physical 3D space.
 * - Angular coordinates: X = R * sin(theta), Z = R * cos(theta).
 * - Outward facing normal: rotationY = -theta in degrees.
 * - Render-phase isolation: all rotations, offsets, scales, and alphas computed in graphicsLayer.
 * - Depth hierarchy: foreground Z > 0, background Z <= 0 with scale & alpha dimming.
 * - Scroll Interception: tapping while active inertia is in flight stops momentum without opening manga.
 * - Pinch-to-zoom density: scales cylinder radius and viewing density smoothly.
 */
@Composable
fun CarouselRingDisplay(
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

    val totalItems = items.size
    // Angular step between items on the ring
    val ringSlots = if (totalItems >= 8) totalItems else 8
    val anglePerItem = (2.0 * PI / ringSlots).toFloat()

    // Continuous scroll angle in radians
    val currentAngle = remember { Animatable(0f) }

    // Aurora vertical pull zoom (-1f for zoom out / circle expansion, +1f for zoom in to hero)
    val zoomAnimatable = remember { Animatable(0f) }

    // Velocity tracker for momentum fling
    val velocityTracker = remember { VelocityTracker() }

    // Scroll interception flag: tracks if carousel was actively moving when touch started
    var isFlinging by remember { mutableStateOf(false) }
    var touchDownTime by remember { mutableLongStateOf(0L) }

    // Pinch-to-zoom density scale factor [0.75f .. 1.35f]
    var zoomDensity by remember { mutableFloatStateOf(1.0f) }
    val transformState = rememberTransformableState { zoomChange, _, _ ->
        zoomDensity = (zoomDensity * zoomChange).coerceIn(0.75f, 1.35f)
    }

    // Active center index derived from currentAngle
    val centerIndex by remember {
        derivedStateOf {
            val normalized = (currentAngle.value / anglePerItem).roundToInt()
            val rem = normalized % totalItems
            if (rem < 0) rem + totalItems else rem
        }
    }

    // Haptic feedback on hero index change
    var lastHapticIndex by remember { mutableIntStateOf(centerIndex) }
    LaunchedEffect(centerIndex) {
        if (centerIndex != lastHapticIndex) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            lastHapticIndex = centerIndex
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .transformable(state = transformState),
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val screenWidthPx = with(density) { screenWidth.toPx() }

        // Cylinder radius derived from screen dimensions and zoom density
        val baseRadius = min(screenWidthPx * 0.46f, with(density) { 260.dp.toPx() }) * zoomDensity
        val cardWidthDp = (145.dp * zoomDensity).coerceIn(110.dp, 210.dp)
        val cardHeightDp = cardWidthDp * 1.5f

        val centerYOffset = with(density) {
            contentPadding.calculateTopPadding() + (screenHeight - ShelfHudHeight - cardHeightDp) * 0.44f
        }

        // Tap & Drag Gesture Handling with Momentum Interception + Vertical Pull Zoom
        val dragModifier = Modifier
            .fillMaxSize()
            .pointerInput(totalItems, baseRadius) {
                val touchSlop = viewConfiguration.touchSlop
                val verticalDragThreshold = 240.dp.toPx()

                awaitEachGesture {
                    val down = awaitFirstDown(pass = PointerEventPass.Initial)
                    touchDownTime = System.currentTimeMillis()
                    val wasMoving = isFlinging || currentAngle.isRunning
                    if (wasMoving) {
                        // Immediately intercept momentum on touch down
                        scope.launch { currentAngle.stop() }
                        isFlinging = false
                    }

                    velocityTracker.resetTracking()
                    velocityTracker.addPosition(down.uptimeMillis, down.position)

                    var totalDy = 0f
                    var totalDx = 0f
                    var isVerticalDrag = false
                    var isHorizontalDrag = false
                    val activePointerId = down.id

                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        val pointer = event.changes.firstOrNull { it.id == activePointerId } ?: break
                        if (!pointer.pressed) break

                        val dy = pointer.position.y - pointer.previousPosition.y
                        val dx = pointer.position.x - pointer.previousPosition.x
                        totalDy += dy
                        totalDx += dx

                        if (!isVerticalDrag && !isHorizontalDrag) {
                            if (Math.abs(totalDy) > touchSlop && Math.abs(totalDy) > Math.abs(totalDx) * 1.2f) {
                                isVerticalDrag = true
                            } else if (Math.abs(totalDx) > touchSlop) {
                                isHorizontalDrag = true
                                isFlinging = true
                            }
                        }

                        if (isVerticalDrag) {
                            pointer.consume()
                            val targetZoom = (totalDy / verticalDragThreshold).coerceIn(-1f, 1f)
                            scope.launch {
                                zoomAnimatable.snapTo(targetZoom)
                            }
                        } else if (isHorizontalDrag) {
                            pointer.consume()
                            velocityTracker.addPosition(pointer.uptimeMillis, pointer.position)
                            val angleDelta = -(dx / baseRadius)
                            scope.launch {
                                currentAngle.snapTo(currentAngle.value + angleDelta)
                            }
                        }
                    }

                    if (isVerticalDrag) {
                        scope.launch {
                            zoomAnimatable.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = 320f,
                                ),
                            )
                        }
                    } else if (isHorizontalDrag) {
                        val velocity = velocityTracker.calculateVelocity().x
                        val angularVelocity = -velocity / baseRadius

                        scope.launch {
                            isFlinging = true
                            val targetAngleRaw = currentAngle.value + (angularVelocity * 0.22f)
                            val targetSlot = (targetAngleRaw / anglePerItem).roundToInt()
                            val targetAngle = targetSlot * anglePerItem

                            currentAngle.animateTo(
                                targetValue = targetAngle,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessLow,
                                ),
                            )
                            isFlinging = false
                        }
                    }
                }
            }

        Box(
            modifier = dragModifier,
            contentAlignment = Alignment.Center,
        ) {
            // Render visible cards along 3D cylinder
            for (index in 0 until totalItems) {
                val item = items[index]
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

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = centerYOffset)
                        .size(width = cardWidthDp, height = cardHeightDp)
                        .graphicsLayer {
                            // 1. Angular coordinate calculation inside graphicsLayer (Render Phase Isolation)
                            val itemBaseAngle = index * anglePerItem
                            val relAngle = itemBaseAngle - currentAngle.value
                            // Normalize angle to [-PI, PI]
                            var theta = (relAngle % (2f * PI.toFloat()))
                            if (theta > PI.toFloat()) theta -= 2f * PI.toFloat()
                            if (theta < -PI.toFloat()) theta += 2f * PI.toFloat()

                            val sinT = sin(theta)
                            val cosT = cos(theta)

                            val zoom = zoomAnimatable.value
                            val zoomIn = zoom.coerceIn(0f, 1f)
                            val zoomOut = (-zoom).coerceIn(0f, 1f)

                            // Cylinder radius: expands when zooming out so the circle is bigger and displays more!
                            val effectiveRadius = baseRadius * (1f + 0.45f * zoomOut)

                            val x = effectiveRadius * sinT
                            val z = effectiveRadius * cosT

                            // Normal outward facing
                            rotationY = -Math.toDegrees(theta.toDouble()).toFloat()
                            translationX = x
                            cameraDistance = 16f * density.density

                            // 2. Depth hierarchy (Front vs Back)
                            val normZ = z / effectiveRadius // [-1.0 .. 1.0]
                            if (z > 0f) {
                                // Foreground: hero highlighted manga is larger (1.18x default), side covers 0.70x
                                val progress = normZ.coerceIn(0f, 1f)
                                val normalScale = 0.70f + 0.48f * progress
                                val uniformScale = 0.70f // When zooming out, all front covers become the exact same size!
                                var cardScale = normalScale * (1f - zoomOut) + uniformScale * zoomOut

                                if (zoomIn > 0f) {
                                    if (progress > 0.85f) {
                                        // Active hero zooms in prominently
                                        cardScale = normalScale * (1.0f + 0.45f * zoomIn)
                                    } else {
                                        cardScale = normalScale * (1.0f - 0.25f * zoomIn)
                                    }
                                }
                                scaleX = cardScale
                                scaleY = cardScale

                                var cardAlpha = 0.80f + 0.20f * progress
                                if (zoomIn > 0f && progress <= 0.85f) {
                                    cardAlpha *= (1f - 0.60f * zoomIn)
                                }
                                alpha = cardAlpha
                                shadowElevation = (16.dp.toPx() * progress).coerceAtLeast(0f)

                                // Vertical descent when zoomed in
                                if (zoomIn > 0f && progress > 0.85f) {
                                    translationY = with(density) { 20.dp.toPx() } * zoomIn
                                } else {
                                    translationY = 0f
                                }
                            } else {
                                // Background rear arc
                                val progress = ((z + effectiveRadius) / effectiveRadius).coerceIn(0f, 1f)
                                scaleX = (0.40f + 0.30f * progress) * (1f - zoomIn * 0.5f)
                                scaleY = scaleX
                                alpha = (0.25f + 0.25f * progress) * (1f - zoomIn * 0.7f)
                                shadowElevation = 0f
                                translationY = 0f
                            }
                        }
                        .zIndex(
                            // Dynamic Z-Ordering: positive depth sorting in front, lower in back
                            run {
                                val itemBaseAngle = index * anglePerItem
                                val relAngle = itemBaseAngle - currentAngle.value
                                var theta = (relAngle % (2f * PI.toFloat()))
                                if (theta > PI.toFloat()) theta -= 2f * PI.toFloat()
                                if (theta < -PI.toFloat()) theta += 2f * PI.toFloat()
                                val zoomOut = (-zoomAnimatable.value).coerceIn(0f, 1f)
                                val effectiveRadius = baseRadius * (1f + 0.45f * zoomOut)
                                val z = effectiveRadius * cos(theta)
                                if (z > 0f) 100f + z else 10f + z
                            },
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (isFlinging) {
                                // Momentum interception: tap stops flight
                                scope.launch { currentAngle.stop() }
                                isFlinging = false
                                return@clickable
                            }

                            if (index == centerIndex) {
                                // Front hero cover clicked: open manga details
                                onClickManga(item.libraryManga)
                            } else {
                                // Background or flanking cover clicked: rotate to front hero position
                                scope.launch {
                                    val itemBaseAngle = index * anglePerItem
                                    val relAngle = itemBaseAngle - currentAngle.value
                                    var theta = (relAngle % (2f * PI.toFloat()))
                                    if (theta > PI.toFloat()) theta -= 2f * PI.toFloat()
                                    if (theta < -PI.toFloat()) theta += 2f * PI.toFloat()

                                    currentAngle.animateTo(
                                        targetValue = currentAngle.value + theta,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessLow,
                                        ),
                                    )
                                }
                            }
                        },
                ) {
                    val isCenter = index == centerIndex

                    // Card surface with subtle specular border on center hero
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.40f),
                        border = BorderStroke(
                            width = if (isCenter) 2.dp else 1.dp,
                            color = if (isCenter) {
                                Color.White.copy(alpha = 0.35f)
                            } else {
                                Color.White.copy(alpha = 0.10f)
                            },
                        ),
                    ) {
                        MangaCover.Book(
                            data = domainCover,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    // Selection check badge if multiselecting
                    if (selection.contains(manga.id)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.TopEnd,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = MaterialSymbols.RoundedFilled.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Hero Manga Info / HUD
            val activeItem = items.getOrNull(centerIndex)
            val zoomIn = zoomAnimatable.value.coerceIn(0f, 1f)
            AuroraShelfHud(
                item = activeItem,
                totalItems = totalItems,
                currentIndex = centerIndex,
                onClickManga = onClickManga,
                onClickContinueReading = onClickContinueReading,
                contentPadding = contentPadding,
                searchQuery = searchQuery,
                onGlobalSearchClicked = onGlobalSearchClicked,
                hudAlpha = 1f - zoomIn * 0.65f,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
