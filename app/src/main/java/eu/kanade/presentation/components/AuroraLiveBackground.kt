package eu.kanade.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.model.LiveBackgroundMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Controller allowing child components (such as Coverflow) to dynamically provide
 * high-frequency scroll offsets evaluated strictly inside the draw phase.
 */
class LiveBackgroundController {
    var scrollOffsetProvider: (() -> Float)? = null
}

val LocalLiveBackgroundController = staticCompositionLocalOf<LiveBackgroundController?> { null }

private val LIGHT_MODE_BASE_GRADIENT = listOf(
    0.0f to Color(0xFFF9FAFD),
    0.5f to Color(0xFFEEF2F9),
    1.0f to Color(0xFFE6EBF5),
)

@Composable
fun AuroraLiveBackground(
    mode: LiveBackgroundMode,
    modifier: Modifier = Modifier,
    scrollOffset: Float = 0f,
    scrollOffsetProvider: (() -> Float)? = null,
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val controller = remember { LiveBackgroundController() }

    val infiniteTransition = rememberInfiniteTransition(label = "LiveBackgroundTransition")
    val timeState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 120f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 120_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "LiveBackgroundTime",
    )

    CompositionLocalProvider(LocalLiveBackgroundController provides controller) {
        Box(
            modifier = modifier.drawLiveBackground(
                mode = mode,
                timeState = timeState,
                isDark = isDark,
                scrollOffsetProvider = {
                    controller.scrollOffsetProvider?.invoke()
                        ?: scrollOffsetProvider?.invoke()
                        ?: scrollOffset
                },
            ),
        ) {
            content()
        }
    }
}

/**
 * High-performance, battery-friendly static snapshot thumbnail for live backgrounds.
 * Uses a fixed time coordinate to render instantaneously without allocating animation transitions.
 */
@Composable
fun LiveBackgroundThumbnail(
    mode: LiveBackgroundMode,
    modifier: Modifier = Modifier,
    snapshotTime: Float = 12.5f,
    isDark: Boolean = isSystemInDarkTheme(),
) {
    Box(
        modifier = modifier.drawBehind {
            val width = size.width
            val height = size.height
            if (width <= 0f || height <= 0f) return@drawBehind
            drawBackgroundForMode(
                mode = mode,
                time = snapshotTime,
                width = width,
                height = height,
                scrollOffset = 0f,
                isDark = isDark,
            )
        },
    )
}

internal fun DrawScope.drawBackgroundForMode(
    mode: LiveBackgroundMode,
    time: Float,
    width: Float,
    height: Float,
    scrollOffset: Float = 0f,
    isDark: Boolean = true,
) {
    when (mode) {
        LiveBackgroundMode.CLASSIC_PLACEHOLDER -> drawClassicPlaceholder(time, width, height, isDark)
        LiveBackgroundMode.FLUX_LINES -> drawFluxLines(time, width, height, scrollOffset, isDark)
        LiveBackgroundMode.AERO_SHARDS -> drawAeroShards(time, width, height, scrollOffset, isDark)
        LiveBackgroundMode.INTERFERENCE_RIBBONS -> drawInterferenceRibbons(time, width, height, scrollOffset, isDark)
        LiveBackgroundMode.PERSPECTIVE_GRID -> drawPerspectiveGrid(time, width, height, scrollOffset, isDark)
        LiveBackgroundMode.RING_FIELD -> drawRingField(time, width, height, scrollOffset, isDark)
        LiveBackgroundMode.GLIMMER_WRAP -> drawGlimmerWrap(time, width, height, scrollOffset, isDark)
        LiveBackgroundMode.SILK_WAVES -> drawSilkWaves(time, width, height, scrollOffset, isDark)
        LiveBackgroundMode.ROLLING_BLINDS -> drawRollingBlinds(time, width, height, scrollOffset, isDark)
        LiveBackgroundMode.MINIMAL_RIPPLE -> drawMinimalRipple(time, width, height, scrollOffset, isDark)
        LiveBackgroundMode.NEURAL_FLASH -> drawNeuralFlash(time, width, height, scrollOffset, isDark)
        LiveBackgroundMode.AURORA_BEAM -> drawAuroraBeam(time, width, height, scrollOffset, isDark)
        LiveBackgroundMode.PIXEL_RAIN -> drawPixelRain(time, width, height, scrollOffset, isDark)
    }
}

private fun Modifier.drawLiveBackground(
    mode: LiveBackgroundMode,
    timeState: State<Float>,
    isDark: Boolean,
    scrollOffsetProvider: () -> Float,
): Modifier = this.drawBehind {
    val width = size.width
    val height = size.height
    if (width <= 0f || height <= 0f) return@drawBehind

    val time = timeState.value
    val scrollOffset = scrollOffsetProvider()
    drawBackgroundForMode(mode, time, width, height, scrollOffset, isDark)
}

// =========================================================================
// 1. INTERFERENCE RIBBONS (React Bits Pro)
// A ribbon of fine glowing wires drifting and twisting with dynamic wave interference
// =========================================================================
private val INTERFERENCE_PATH = ThreadLocal.withInitial { Path() }
private val INTERFERENCE_PALETTES = listOf(
    listOf(Color(0xFFF43F5E), Color(0xFFA855F7), Color(0xFF00F5D4), Color(0xFFF43F5E)),
    listOf(Color(0xFFA855F7), Color(0xFF00F5D4), Color(0xFFE2E8F0), Color(0xFFA855F7)),
    listOf(Color(0xFF00F5D4), Color(0xFFF43F5E), Color(0xFFA855F7), Color(0xFF00F5D4)),
)
private val INTERFERENCE_LIGHT_PALETTES = listOf(
    listOf(Color(0xFFE11D48), Color(0xFF7C3AED), Color(0xFF0D9488), Color(0xFFE11D48)),
    listOf(Color(0xFF7C3AED), Color(0xFF2563EB), Color(0xFF059669), Color(0xFF7C3AED)),
    listOf(Color(0xFF0D9488), Color(0xFFE11D48), Color(0xFF7C3AED), Color(0xFF0D9488)),
)

private fun DrawScope.drawInterferenceRibbons(
    time: Float,
    width: Float,
    height: Float,
    scrollOffset: Float,
    isDark: Boolean,
) {
    if (isDark) {
        drawRect(
            brush = Brush.verticalGradient(
                0.0f to Color(0xFF06040A),
                0.5f to Color(0xFF0B0716),
                1.0f to Color(0xFF050308),
                startY = 0f,
                endY = height,
            ),
        )
    } else {
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = LIGHT_MODE_BASE_GRADIENT.toTypedArray(),
                startY = 0f,
                endY = height,
            ),
        )
    }

    val maxDim = max(width, height)
    val glowBlend = if (isDark) BlendMode.Screen else BlendMode.SrcOver
    val glowColors = if (isDark) {
        listOf(
            Color(0xFF381552).copy(alpha = 0.40f),
            Color(0xFF19092B).copy(alpha = 0.20f),
            Color.Transparent,
        )
    } else {
        listOf(
            Color(0xFF8B5CF6).copy(alpha = 0.09f),
            Color(0xFFEC4899).copy(alpha = 0.05f),
            Color.Transparent,
        )
    }
    drawRect(
        brush = Brush.radialGradient(
            colors = glowColors,
            center = Offset(width * 0.5f, height * 0.52f),
            radius = maxDim * 0.65f,
        ),
        blendMode = glowBlend,
    )

    val path = INTERFERENCE_PATH.get() ?: Path()
    val wireCount = 14
    val stepCount = 40
    val totalWidth = width + 80f
    val startX = -40f
    val dx = totalWidth / stepCount
    val centerY = height * 0.52f
    val reactiveDrift = scrollOffset * 30f

    val paletteList = if (isDark) INTERFERENCE_PALETTES else INTERFERENCE_LIGHT_PALETTES
    val wireBlend = if (isDark) BlendMode.Screen else BlendMode.SrcOver

    for (k in 0 until wireCount) {
        path.reset()
        val kNorm = k.toFloat() / (wireCount - 1).toFloat()
        val freq1 = 2f * PI.toFloat() * (1.6f + k * 0.08f)
        val freq2 = 2f * PI.toFloat() * (2.8f + k * 0.12f)
        val phase1 = time * (0.45f + k * 0.04f) + k * 0.42f + reactiveDrift * 0.05f
        val phase2 = -time * (0.32f + k * 0.03f) + k * 0.65f

        val amp1 = height * (0.055f + sin(time * 0.2f + k * 0.3f) * 0.015f)
        val amp2 = height * 0.025f
        val wireBaseY = centerY + (kNorm - 0.5f) * (height * 0.16f) + sin(time * 0.3f + k * 0.5f) * (height * 0.03f)

        fun computeY(x: Float): Float {
            val u = x / width
            return wireBaseY + sin(u * freq1 + phase1) * amp1 + cos(u * freq2 + phase2) * amp2
        }

        var prevX = startX
        var prevY = computeY(prevX)
        path.moveTo(prevX, prevY)

        for (s in 1..stepCount) {
            val currX = startX + s * dx
            val currY = computeY(currX)
            val midX = (prevX + currX) * 0.5f
            val midY = (prevY + currY) * 0.5f
            path.quadraticTo(prevX, prevY, midX, midY)
            prevX = currX
            prevY = currY
        }
        path.lineTo(prevX, prevY)

        val brush = Brush.horizontalGradient(
            colors = paletteList[k % paletteList.size],
            startX = 0f,
            endX = width,
        )

        // Outer glow
        drawPath(
            path = path,
            brush = brush,
            alpha = if (isDark) 0.12f else 0.18f,
            style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            blendMode = wireBlend,
        )

        // Core sharp filament
        drawPath(
            path = path,
            brush = brush,
            alpha = if (isDark) 0.75f else 0.85f,
            style = Stroke(width = if (isDark) 1.2.dp.toPx() else 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            blendMode = wireBlend,
        )
    }
}

// =========================================================================
// 2. PERSPECTIVE GRID (React Bits Pro)
// An endless grid scrolling toward the horizon with glowing perspective lines
// =========================================================================
private fun DrawScope.drawPerspectiveGrid(
    time: Float,
    width: Float,
    height: Float,
    scrollOffset: Float,
    isDark: Boolean,
) {
    val horizonY = height * 0.58f

    if (isDark) {
        drawRect(
            brush = Brush.verticalGradient(
                0.0f to Color(0xFF030408),
                0.58f to Color(0xFF0A0C18),
                1.0f to Color(0xFF04050A),
                startY = 0f,
                endY = height,
            ),
        )
    } else {
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = LIGHT_MODE_BASE_GRADIENT.toTypedArray(),
                startY = 0f,
                endY = height,
            ),
        )
    }

    val gridBlend = if (isDark) BlendMode.Screen else BlendMode.SrcOver
    val horizonGlowColor = if (isDark) Color(0xFF00F5D4).copy(alpha = 0.32f) else Color(0xFF4F46E5).copy(alpha = 0.12f)
    val horizonCenterColor = if (isDark) Color(0xFF6366F1).copy(alpha = 0.18f) else Color(0xFF06B6D4).copy(alpha = 0.08f)

    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                horizonGlowColor,
                horizonCenterColor,
                Color.Transparent,
            ),
            center = Offset(width * 0.5f, horizonY),
            radius = width * 0.65f,
        ),
        blendMode = gridBlend,
    )

    val horizonLineColor = if (isDark) Color(0xFF00F5D4) else Color(0xFF4F46E5)
    drawLine(
        brush = Brush.horizontalGradient(
            0.0f to Color.Transparent,
            0.2f to horizonLineColor.copy(alpha = if (isDark) 0.3f else 0.4f),
            0.5f to horizonLineColor.copy(alpha = if (isDark) 0.85f else 0.75f),
            0.8f to horizonLineColor.copy(alpha = if (isDark) 0.3f else 0.4f),
            1.0f to Color.Transparent,
        ),
        start = Offset(0f, horizonY),
        end = Offset(width, horizonY),
        strokeWidth = 1.5.dp.toPx(),
        blendMode = gridBlend,
    )

    val lineCount = 9
    val scrollPhase = (time * 0.35f) % 1.0f
    val transColor1 = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val transColor2 = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)

    for (i in 0 until lineCount) {
        val t = ((i.toFloat() + scrollPhase) / lineCount.toFloat())
        val curveT = t * t // Quadratic perspective distribution
        val lineY = horizonY + (height - horizonY) * curveT
        val alpha = (0.05f + 0.60f * curveT).coerceIn(0f, 0.75f)

        drawLine(
            brush = Brush.horizontalGradient(
                0.0f to Color.Transparent,
                0.3f to transColor1.copy(alpha = alpha),
                0.7f to transColor2.copy(alpha = alpha),
                1.0f to Color.Transparent,
            ),
            start = Offset(0f, lineY),
            end = Offset(width, lineY),
            strokeWidth = (0.8.dp.toPx() + 1.2.dp.toPx() * curveT),
            blendMode = gridBlend,
        )
    }

    val rayCount = 8
    val vanishingX = width * 0.5f + (scrollOffset * 35f)
    val vanishingPoint = Offset(vanishingX, horizonY)
    val rayColor1 = if (isDark) Color(0xFF00F5D4) else Color(0xFF0284C7)
    val rayColor2 = if (isDark) Color(0xFF6366F1) else Color(0xFF4F46E5)

    for (i in -rayCount..rayCount) {
        val spread = i.toFloat() / rayCount.toFloat()
        val bottomX = vanishingX + spread * (width * 1.15f)

        drawLine(
            brush = Brush.verticalGradient(
                0.0f to rayColor1.copy(alpha = 0.05f),
                1.0f to rayColor2.copy(alpha = if (isDark) 0.35f else 0.40f),
                startY = horizonY,
                endY = height,
            ),
            start = vanishingPoint,
            end = Offset(bottomX, height),
            strokeWidth = 1.dp.toPx(),
            blendMode = gridBlend,
        )
    }
}

// =========================================================================
// 3. RING FIELD (React Bits Pro)
// Wavy rings of fine dust drifting outward from a dark center
// =========================================================================
private data class RingDustMote(
    val baseAngle: Float,
    val ringIndex: Int,
    val phase: Float,
    val sizeDp: Float,
    val speed: Float,
)

private val RING_DUST_MOTES: List<RingDustMote> = run {
    val list = ArrayList<RingDustMote>(140)
    val random = kotlin.random.Random(2026)
    for (i in 0 until 140) {
        list.add(
            RingDustMote(
                baseAngle = random.nextFloat() * 2f * PI.toFloat(),
                ringIndex = i % 6,
                phase = random.nextFloat() * 2f * PI.toFloat(),
                sizeDp = 1.0f + random.nextFloat() * 2.2f,
                speed = 0.8f + random.nextFloat() * 0.4f,
            ),
        )
    }
    list
}

private fun DrawScope.drawRingField(
    time: Float,
    width: Float,
    height: Float,
    scrollOffset: Float,
    isDark: Boolean,
) {
    val maxDim = max(width, height)
    val center = Offset(width * 0.5f + (scrollOffset * 25f), height * 0.50f)
    val maxRadius = maxDim * 0.65f

    if (isDark) {
        drawRect(
            brush = Brush.radialGradient(
                0.0f to Color(0xFF030206),
                0.5f to Color(0xFF07050E),
                1.0f to Color(0xFF030205),
                center = center,
                radius = maxDim * 0.70f,
            ),
        )
    } else {
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = LIGHT_MODE_BASE_GRADIENT.toTypedArray(),
                startY = 0f,
                endY = height,
            ),
        )
    }

    val ringBlend = if (isDark) BlendMode.Screen else BlendMode.SrcOver
    val ringColor = if (isDark) Color(0xFF8B5CF6) else Color(0xFF7C3AED)

    // 2. 6 Concentric wavy ring guides
    val ringCount = 6
    for (r in 0 until ringCount) {
        val expandTime = (time * 0.12f * (1f + r * 0.05f) + r.toFloat() / ringCount.toFloat()) % 1.0f
        val radius = expandTime * maxRadius
        if (radius < 20f) continue
        val ringAlpha = ((1.0f - expandTime) * (if (isDark) 0.16f else 0.22f)).coerceIn(0f, 0.25f)

        drawCircle(
            color = ringColor.copy(alpha = ringAlpha),
            radius = radius,
            center = center,
            style = Stroke(width = 1.dp.toPx()),
            blendMode = ringBlend,
        )
    }

    val moteColor1 = if (isDark) Color(0xFFC084FC) else Color(0xFF7C3AED)
    val moteColor2 = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

    // 3. Wavy dust motes drifting along expanding waves
    for (mote in RING_DUST_MOTES) {
        val ringExpand = (time * 0.10f * mote.speed + mote.ringIndex.toFloat() / 6f) % 1.0f
        val baseR = ringExpand * maxRadius
        if (baseR < 15f) continue

        val wave = sin(mote.baseAngle * 5f + time * 1.5f + mote.phase) * (14.dp.toPx())
        val finalR = baseR + wave
        val angle = mote.baseAngle + time * 0.05f * (if (mote.ringIndex % 2 == 0) 1f else -1f)

        val px = center.x + cos(angle) * finalR
        val py = center.y + sin(angle) * finalR

        val lifeFade = (1.0f - ringExpand) * (if (ringExpand < 0.15f) ringExpand / 0.15f else 1.0f)
        val twinkle = sin(time * 2.5f + mote.phase) * 0.35f + 0.65f
        val alpha = ((if (isDark) 0.15f else 0.25f) + 0.60f * twinkle) * lifeFade

        val color = if (mote.ringIndex % 2 == 0) {
            moteColor1.copy(alpha = alpha.coerceIn(0f, 1f))
        } else {
            moteColor2.copy(alpha = alpha.coerceIn(0f, 1f))
        }

        drawCircle(
            color = color,
            radius = mote.sizeDp.dp.toPx() * lifeFade,
            center = Offset(px, py),
            blendMode = ringBlend,
        )
    }
}

// =========================================================================
// 4. GLIMMER WRAP (React Bits Pro)
// A field of fine glints drifting past, flashing as they catch the light
// =========================================================================
private data class GlimmerStar(
    val seedX: Float,
    val seedY: Float,
    val depth: Float,
    val flashFreq: Float,
    val flashPhase: Float,
    val baseSizeDp: Float,
)

private val GLIMMER_STARS: List<GlimmerStar> = run {
    val list = ArrayList<GlimmerStar>(130)
    val random = kotlin.random.Random(1997)
    for (i in 0 until 130) {
        list.add(
            GlimmerStar(
                seedX = random.nextFloat(),
                seedY = random.nextFloat(),
                depth = 0.2f + random.nextFloat() * 0.8f,
                flashFreq = 0.8f + random.nextFloat() * 1.5f,
                flashPhase = random.nextFloat() * 2f * PI.toFloat(),
                baseSizeDp = 1.0f + random.nextFloat() * 2.5f,
            ),
        )
    }
    list
}

private fun DrawScope.drawGlimmerWrap(
    time: Float,
    width: Float,
    height: Float,
    scrollOffset: Float,
    isDark: Boolean,
) {
    if (isDark) {
        drawRect(
            brush = Brush.verticalGradient(
                0.0f to Color(0xFF04060E),
                0.5f to Color(0xFF070B18),
                1.0f to Color(0xFF03050B),
                startY = 0f,
                endY = height,
            ),
        )
    } else {
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = LIGHT_MODE_BASE_GRADIENT.toTypedArray(),
                startY = 0f,
                endY = height,
            ),
        )
    }

    val maxDim = max(width, height)
    val glimmerBlend = if (isDark) BlendMode.Screen else BlendMode.SrcOver
    val ambientGlow = if (isDark) Color(0xFF172554).copy(alpha = 0.35f) else Color(0xFF38BDF8).copy(alpha = 0.08f)

    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                ambientGlow,
                Color.Transparent,
            ),
            center = Offset(width * 0.45f, height * 0.40f),
            radius = maxDim * 0.60f,
        ),
        blendMode = glimmerBlend,
    )

    val starColor = if (isDark) Color(0xFFBAE6FD) else Color(0xFF0284C7)
    val flareColor = if (isDark) Color.White else Color(0xFF2563EB)
    val flareHaloColor = if (isDark) Color(0xFF38BDF8) else Color(0xFFD97706)

    // 3. Glint stars & diamond 4-point flashes
    for (star in GLIMMER_STARS) {
        val curY = ((star.seedY - time * 0.015f * star.depth) % 1.0f + 1.0f) % 1.0f * height
        val sway = sin(time * 0.5f + star.seedX * 8f) * (width * 0.02f)
        val curX = (star.seedX * width + sway + scrollOffset * 30f * star.depth) % width

        val flashRaw = (sin(time * star.flashFreq + star.flashPhase) * 0.5f + 0.5f)
        val flashPeak = flashRaw * flashRaw * flashRaw * flashRaw // Sharp peak on high power
        val baseAlpha = (if (isDark) 0.08f else 0.16f) + (if (isDark) 0.20f else 0.30f) * star.depth

        // Draw ambient dot
        drawCircle(
            color = starColor.copy(alpha = baseAlpha),
            radius = star.baseSizeDp.dp.toPx() * (0.6f + star.depth * 0.5f),
            center = Offset(curX, curY),
            blendMode = glimmerBlend,
        )

        // When flashing peak occurs: draw diamond 4-point glint flare
        if (flashPeak > 0.4f) {
            val flareIntensity = (flashPeak - 0.4f) / 0.6f
            val flareLen = (10.dp.toPx() + 16.dp.toPx() * star.depth) * flareIntensity
            val flareAlpha = ((if (isDark) 0.35f else 0.45f) + 0.55f * flareIntensity) * star.depth

            // Horizontal spike
            drawLine(
                color = flareColor.copy(alpha = flareAlpha),
                start = Offset(curX - flareLen, curY),
                end = Offset(curX + flareLen, curY),
                strokeWidth = 1.dp.toPx(),
                blendMode = glimmerBlend,
            )
            // Vertical spike
            drawLine(
                color = flareColor.copy(alpha = flareAlpha),
                start = Offset(curX, curY - flareLen),
                end = Offset(curX, curY + flareLen),
                strokeWidth = 1.dp.toPx(),
                blendMode = glimmerBlend,
            )
            // Soft center halo
            drawCircle(
                color = flareHaloColor.copy(alpha = flareAlpha * 0.4f),
                radius = 6.dp.toPx() * flareIntensity,
                center = Offset(curX, curY),
                blendMode = glimmerBlend,
            )
        }
    }
}

// =========================================================================
// 5. SILK WAVES (React Bits Pro)
// Flowing waves like draped silk with rich layered sheen
// =========================================================================
private val SILK_PATH = ThreadLocal.withInitial { Path() }
private val SILK_PALETTES = listOf(
    listOf(Color(0xFF818CF8), Color(0xFFC084FC), Color(0xFFF472B6)),
    listOf(Color(0xFF38BDF8), Color(0xFF818CF8), Color(0xFFC084FC)),
    listOf(Color(0xFFC084FC), Color(0xFFF472B6), Color(0xFFFB7185)),
    listOf(Color(0xFF6366F1), Color(0xFF38BDF8), Color(0xFF818CF8)),
)

private val SILK_LIGHT_PALETTES = listOf(
    listOf(Color(0xFF6366F1), Color(0xFFA855F7), Color(0xFFEC4899)),
    listOf(Color(0xFF0284C7), Color(0xFF6366F1), Color(0xFFA855F7)),
    listOf(Color(0xFFA855F7), Color(0xFFEC4899), Color(0xFFF43F5E)),
    listOf(Color(0xFF4F46E5), Color(0xFF0284C7), Color(0xFF6366F1)),
)

private fun DrawScope.drawSilkWaves(
    time: Float,
    width: Float,
    height: Float,
    scrollOffset: Float,
    isDark: Boolean,
) {
    if (isDark) {
        drawRect(
            brush = Brush.verticalGradient(
                0.0f to Color(0xFF070712),
                0.5f to Color(0xFF0D0A1C),
                1.0f to Color(0xFF06050E),
                startY = 0f,
                endY = height,
            ),
        )
    } else {
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = LIGHT_MODE_BASE_GRADIENT.toTypedArray(),
                startY = 0f,
                endY = height,
            ),
        )
    }

    val path = SILK_PATH.get() ?: Path()
    val waveCount = 4
    val stepCount = 36
    val totalWidth = width + 80f
    val startX = -40f
    val dx = totalWidth / stepCount
    val baseY = height * 0.48f

    val paletteList = if (isDark) SILK_PALETTES else SILK_LIGHT_PALETTES
    val silkBlend = if (isDark) BlendMode.Screen else BlendMode.SrcOver

    for (i in 0 until waveCount) {
        path.reset()
        val iNorm = i.toFloat() / waveCount.toFloat()
        val freq1 = 2f * PI.toFloat() * (1.2f + i * 0.18f)
        val phase1 = time * (0.35f + i * 0.08f) + i * 1.6f + scrollOffset * 0.2f
        val amp1 = height * (0.065f + (i % 2) * 0.02f)
        val waveBaseY = baseY + (i - 1.5f) * (height * 0.12f) + sin(time * 0.25f + i * 1.2f) * (height * 0.04f)

        fun computeY(x: Float): Float {
            val u = x / width
            return waveBaseY + sin(u * freq1 + phase1) * amp1
        }

        var prevX = startX
        var prevY = computeY(prevX)
        path.moveTo(prevX, prevY)

        for (s in 1..stepCount) {
            val currX = startX + s * dx
            val currY = computeY(currX)
            val midX = (prevX + currX) * 0.5f
            val midY = (prevY + currY) * 0.5f
            path.quadraticTo(prevX, prevY, midX, midY)
            prevX = currX
            prevY = currY
        }
        path.lineTo(prevX, prevY)
        path.lineTo(width + 40f, height + 40f)
        path.lineTo(-40f, height + 40f)
        path.close()

        val brush = Brush.horizontalGradient(
            colors = paletteList[i % paletteList.size],
            startX = 0f,
            endX = width,
        )

        // Fill with translucent draped gradient
        drawPath(
            path = path,
            brush = brush,
            alpha = (if (isDark) 0.15f else 0.12f) + iNorm * (if (isDark) 0.08f else 0.06f),
            blendMode = silkBlend,
        )

        // Top silk sheen rim
        drawPath(
            path = path,
            brush = brush,
            alpha = if (isDark) 0.65f else 0.70f,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            blendMode = silkBlend,
        )
    }
}

// =========================================================================
// 6. ROLLING BLINDS (React Bits Pro)
// Vertical blinds of light rolling across the frame
// =========================================================================
private fun DrawScope.drawRollingBlinds(
    time: Float,
    width: Float,
    height: Float,
    scrollOffset: Float,
    isDark: Boolean,
) {
    if (isDark) {
        drawRect(Color(0xFF040407))
    } else {
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = LIGHT_MODE_BASE_GRADIENT.toTypedArray(),
                startY = 0f,
                endY = height,
            ),
        )
    }

    val blindCount = 8
    val blindWidth = width * 0.18f
    val totalCycleWidth = width + blindWidth * 2f
    val speed = width * 0.08f
    val offset = (time * speed + scrollOffset * 40f) % totalCycleWidth
    val blindBlend = if (isDark) BlendMode.Screen else BlendMode.SrcOver

    for (b in 0 until blindCount) {
        val basePosX = ((b.toFloat() / blindCount.toFloat()) * totalCycleWidth + offset) % totalCycleWidth - blindWidth
        val centerNorm = (basePosX / width).coerceIn(0f, 1f)
        val alphaMultiplier = sin(centerNorm * PI.toFloat()).coerceAtLeast(0f)

        val col1 = if (isDark) Color(0xFFF59E0B) else Color(0xFFD97706)
        val col2 = if (isDark) Color(0xFFFDE047) else Color(0xFFEAB308)

        drawRect(
            brush = Brush.horizontalGradient(
                0.0f to Color.Transparent,
                0.3f to col1.copy(alpha = 0.04f * alphaMultiplier),
                0.5f to col2.copy(alpha = (if (isDark) 0.22f else 0.20f) * alphaMultiplier),
                0.7f to col1.copy(alpha = 0.04f * alphaMultiplier),
                1.0f to Color.Transparent,
                startX = basePosX,
                endX = basePosX + blindWidth,
            ),
            topLeft = Offset(basePosX, 0f),
            size = Size(blindWidth, height),
            blendMode = blindBlend,
        )
    }
}

// =========================================================================
// 7. MINIMAL RIPPLE (React Bits Pro)
// Concentric ripples spreading from the center
// =========================================================================
private fun DrawScope.drawMinimalRipple(
    time: Float,
    width: Float,
    height: Float,
    scrollOffset: Float,
    isDark: Boolean,
) {
    val maxDim = max(width, height)
    val center = Offset(width * 0.5f + scrollOffset * 20f, height * 0.50f)
    val maxRadius = maxDim * 0.65f

    if (isDark) {
        drawRect(
            brush = Brush.radialGradient(
                0.0f to Color(0xFF020914),
                0.6f to Color(0xFF04060C),
                1.0f to Color(0xFF020307),
                center = center,
                radius = maxDim * 0.70f,
            ),
        )
    } else {
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = LIGHT_MODE_BASE_GRADIENT.toTypedArray(),
                startY = 0f,
                endY = height,
            ),
        )
    }

    val rippleBlend = if (isDark) BlendMode.Screen else BlendMode.SrcOver
    val mainColor = if (isDark) Color(0xFF00F5D4) else Color(0xFF0284C7)
    val secondColor = if (isDark) Color(0xFF00B4D8) else Color(0xFF6366F1)

    // 2. 6 concentric expanding ripple wavefronts
    val rippleCount = 6
    for (i in 0 until rippleCount) {
        val t = ((time * 0.18f + i.toFloat() / rippleCount.toFloat()) % 1.0f)
        val radius = t * maxRadius
        if (radius < 10f) continue

        val waveAlpha = (sin(t * PI.toFloat()) * (if (isDark) 0.35f else 0.40f)).coerceIn(0f, 0.45f)
        val strokeW = 1.dp.toPx() + 2.5.dp.toPx() * (1.0f - t)

        // Main wavefront
        drawCircle(
            color = mainColor.copy(alpha = waveAlpha),
            radius = radius,
            center = center,
            style = Stroke(width = strokeW),
            blendMode = rippleBlend,
        )

        // Secondary harmonic inner ring
        if (radius > 25f) {
            drawCircle(
                color = secondColor.copy(alpha = waveAlpha * 0.45f),
                radius = radius - 8.dp.toPx(),
                center = center,
                style = Stroke(width = 0.8.dp.toPx()),
                blendMode = rippleBlend,
            )
        }
    }
}

// =========================================================================
// 8. NEURAL FLASH (React Bits Pro)
// A storm of glowing filaments, like a thought firing
// =========================================================================
private data class NeuralNode(
    val seedX: Float,
    val seedY: Float,
    val connectIndex1: Int,
    val connectIndex2: Int,
    val pulseFreq: Float,
    val pulsePhase: Float,
)

private val NEURAL_NODES: List<NeuralNode> = run {
    val list = ArrayList<NeuralNode>(28)
    val random = kotlin.random.Random(101)
    for (i in 0 until 28) {
        list.add(
            NeuralNode(
                seedX = 0.08f + random.nextFloat() * 0.84f,
                seedY = 0.08f + random.nextFloat() * 0.84f,
                connectIndex1 = (i + 1) % 28,
                connectIndex2 = (i + 3) % 28,
                pulseFreq = 0.6f + random.nextFloat() * 1.4f,
                pulsePhase = random.nextFloat() * 2f * PI.toFloat(),
            ),
        )
    }
    list
}

private fun DrawScope.drawNeuralFlash(
    time: Float,
    width: Float,
    height: Float,
    scrollOffset: Float,
    isDark: Boolean,
) {
    if (isDark) {
        drawRect(Color(0xFF04060C))
    } else {
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = LIGHT_MODE_BASE_GRADIENT.toTypedArray(),
                startY = 0f,
                endY = height,
            ),
        )
    }

    val neuralBlend = if (isDark) BlendMode.Screen else BlendMode.SrcOver
    val axonColor = if (isDark) Color(0xFF38BDF8).copy(alpha = 0.08f) else Color(0xFF4F46E5).copy(alpha = 0.22f)
    val pulseColor = if (isDark) Color(0xFFC084FC).copy(alpha = 0.75f) else Color(0xFF7C3AED).copy(alpha = 0.85f)
    val nodeBaseColor = if (isDark) Color(0xFF818CF8) else Color(0xFF2563EB)

    val reactiveX = scrollOffset * 25f

    for (node in NEURAL_NODES) {
        val n1X = node.seedX * width + reactiveX
        val n1Y = node.seedY * height

        val target1 = NEURAL_NODES[node.connectIndex1]
        val n2X = target1.seedX * width + reactiveX
        val n2Y = target1.seedY * height

        // Synaptic axon line
        drawLine(
            color = axonColor,
            start = Offset(n1X, n1Y),
            end = Offset(n2X, n2Y),
            strokeWidth = 1.dp.toPx(),
            blendMode = neuralBlend,
        )

        // Firing electrical pulse traveling along axon
        val pulseTime = (time * node.pulseFreq + node.pulsePhase) % 1.0f
        val pulseX = n1X + (n2X - n1X) * pulseTime
        val pulseY = n1Y + (n2Y - n1Y) * pulseTime

        drawCircle(
            color = pulseColor,
            radius = 2.dp.toPx(),
            center = Offset(pulseX, pulseY),
            blendMode = neuralBlend,
        )

        // Node flash
        val flash = sin(time * node.pulseFreq * 2f + node.pulsePhase) * 0.5f + 0.5f
        val nodeAlpha = (if (isDark) 0.15f else 0.30f) + 0.65f * (flash * flash)

        drawCircle(
            color = nodeBaseColor.copy(alpha = nodeAlpha),
            radius = 3.dp.toPx() + 2.dp.toPx() * flash,
            center = Offset(n1X, n1Y),
            blendMode = neuralBlend,
        )
    }
}

// =========================================================================
// 9. AURORA BEAM (React Bits Pro)
// A sweeping beam of aurora light
// =========================================================================
private val AURORA_BEAM_PATH = ThreadLocal.withInitial { Path() }

private fun DrawScope.drawAuroraBeam(
    time: Float,
    width: Float,
    height: Float,
    scrollOffset: Float,
    isDark: Boolean,
) {
    if (isDark) {
        drawRect(
            brush = Brush.verticalGradient(
                0.0f to Color(0xFF020409),
                0.5f to Color(0xFF050E18),
                1.0f to Color(0xFF03050B),
                startY = 0f,
                endY = height,
            ),
        )
    } else {
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = LIGHT_MODE_BASE_GRADIENT.toTypedArray(),
                startY = 0f,
                endY = height,
            ),
        )
    }

    val beamBlend = if (isDark) BlendMode.Screen else BlendMode.SrcOver
    val path = AURORA_BEAM_PATH.get() ?: Path()
    val stepCount = 30
    val reactive = scrollOffset * 30f

    for (c in 0 until 3) {
        path.reset()
        val cNorm = c.toFloat() / 2f
        val baseBeamX = width * (0.35f + cNorm * 0.30f) + sin(time * 0.35f + c * 1.2f) * (width * 0.12f) + reactive

        for (s in 0..stepCount) {
            val currY = (s.toFloat() / stepCount.toFloat()) * height
            val wave = sin(currY * 0.008f + time * 0.8f + c * 1.5f) * (35.dp.toPx()) +
                cos(currY * 0.015f - time * 0.5f) * (18.dp.toPx())
            val currX = baseBeamX + (currY / height) * (width * 0.25f) + wave

            if (s == 0) path.moveTo(currX, currY) else path.lineTo(currX, currY)
        }

        val colors = if (isDark) {
            when (c) {
                0 -> listOf(Color(0xFF10B981), Color(0xFF06B6D4), Color.Transparent)
                1 -> listOf(Color(0xFF06B6D4), Color(0xFF8B5CF6), Color.Transparent)
                else -> listOf(Color(0xFFD946EF), Color(0xFF10B981), Color.Transparent)
            }
        } else {
            when (c) {
                0 -> listOf(Color(0xFF059669), Color(0xFF0284C7), Color.Transparent)
                1 -> listOf(Color(0xFF0284C7), Color(0xFF7C3AED), Color.Transparent)
                else -> listOf(Color(0xFFC026D3), Color(0xFF059669), Color.Transparent)
            }
        }

        drawPath(
            path = path,
            brush = Brush.horizontalGradient(colors = colors, startX = 0f, endX = width),
            alpha = if (isDark) 0.35f else 0.30f,
            style = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            blendMode = beamBlend,
        )
        drawPath(
            path = path,
            color = (if (isDark) Color.White else Color(0xFF4F46E5)).copy(alpha = if (isDark) 0.50f else 0.40f),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            blendMode = beamBlend,
        )
    }
}

// =========================================================================
// 10. PIXEL RAIN (React Bits Pro)
// Pixelated streaks falling in a quiet digital rain
// =========================================================================
private data class PixelRainColumn(
    val xNorm: Float,
    val speed: Float,
    val phase: Float,
    val length: Int,
    val pixelSizeDp: Float,
)

private val PIXEL_RAIN_COLUMNS: List<PixelRainColumn> = run {
    val list = ArrayList<PixelRainColumn>(24)
    val random = kotlin.random.Random(1337)
    for (i in 0 until 24) {
        list.add(
            PixelRainColumn(
                xNorm = (i.toFloat() + 0.5f) / 24f,
                speed = 0.15f + random.nextFloat() * 0.25f,
                phase = random.nextFloat() * 2f * PI.toFloat(),
                length = 7 + random.nextInt(9),
                pixelSizeDp = 4.5f + random.nextFloat() * 2.0f,
            ),
        )
    }
    list
}

private fun DrawScope.drawPixelRain(
    time: Float,
    width: Float,
    height: Float,
    scrollOffset: Float,
    isDark: Boolean,
) {
    if (isDark) {
        drawRect(Color(0xFF030508))
    } else {
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = LIGHT_MODE_BASE_GRADIENT.toTypedArray(),
                startY = 0f,
                endY = height,
            ),
        )
    }

    val rainBlend = if (isDark) BlendMode.Screen else BlendMode.SrcOver
    val stepH = 8.dp.toPx()

    for (col in PIXEL_RAIN_COLUMNS) {
        val colX = col.xNorm * width + (scrollOffset * 15f)
        val headYNorm = ((time * col.speed + col.phase) % 1.2f) - 0.1f
        val headY = headYNorm * height
        val pixelPx = col.pixelSizeDp.dp.toPx()

        for (p in 0 until col.length) {
            val py = headY - p * stepH
            if (py < 0f || py > height) continue

            val tailFactor = 1.0f - (p.toFloat() / col.length.toFloat())
            val alpha = (tailFactor * tailFactor * (if (isDark) 0.85f else 0.75f)).coerceIn(0f, 1f)

            val color = if (p == 0) {
                if (isDark) Color.White.copy(alpha = 0.95f) else Color(0xFF047857).copy(alpha = 0.95f)
            } else if (p < 3) {
                if (isDark) Color(0xFF6EE7B7).copy(alpha = alpha) else Color(0xFF059669).copy(alpha = alpha)
            } else {
                if (isDark) Color(0xFF059669).copy(alpha = alpha) else Color(0xFF10B981).copy(alpha = alpha)
            }

            drawRect(
                color = color,
                topLeft = Offset(colX - pixelPx * 0.5f, py),
                size = Size(pixelPx, pixelPx),
                blendMode = rainBlend,
            )
        }
    }
}

/**
 * CLASSIC_PLACEHOLDER:
 * Original Xbox 360 Aurora-inspired animated plasma caustics & cyan bloom.
 */
private fun DrawScope.drawClassicPlaceholder(
    time: Float,
    width: Float,
    height: Float,
    isDark: Boolean,
) {
    val maxDim = max(width, height)

    if (isDark) {
        drawRect(
            brush = Brush.verticalGradient(
                0.0f to Color(0xFF04070D),
                0.5f to Color(0xFF060A12),
                1.0f to Color(0xFF080E18),
                startY = 0f,
                endY = height,
            ),
        )
    } else {
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = LIGHT_MODE_BASE_GRADIENT.toTypedArray(),
                startY = 0f,
                endY = height,
            ),
        )
    }

    val classicBlend = if (isDark) BlendMode.Screen else BlendMode.SrcOver
    val horizonBreathe = (sin(time * 0.628f) * 0.5f + 0.5f) * 0.06f
    val horizonCenter = Offset(width * 0.5f, height * 0.70f)
    val horizonRadius = maxDim * (0.65f + horizonBreathe * 0.5f)

    val horizonColors = if (isDark) {
        listOf(
            Color(0xFF00E5FF).copy(alpha = 0.24f + horizonBreathe),
            Color(0xFF0077B6).copy(alpha = 0.14f + horizonBreathe * 0.5f),
            Color(0xFF023E8A).copy(alpha = 0.05f),
            Color.Transparent,
        )
    } else {
        listOf(
            Color(0xFF0284C7).copy(alpha = 0.12f + horizonBreathe * 0.5f),
            Color(0xFF0D9488).copy(alpha = 0.07f + horizonBreathe * 0.3f),
            Color.Transparent,
        )
    }

    drawRect(
        brush = Brush.radialGradient(
            colors = horizonColors,
            center = horizonCenter,
            radius = horizonRadius,
        ),
        blendMode = classicBlend,
    )

    val p1X = width * (0.36f + 0.26f * sin(time * 0.22f))
    val p1Y = height * (0.34f + 0.16f * cos(time * 0.18f))
    val p1Radius = maxDim * (0.58f + 0.05f * sin(time * 0.31f))

    val p1Colors = if (isDark) {
        listOf(
            Color(0xFF00F5D4).copy(alpha = 0.26f),
            Color(0xFF00B4D8).copy(alpha = 0.13f),
            Color(0xFF0077B6).copy(alpha = 0.04f),
            Color.Transparent,
        )
    } else {
        listOf(
            Color(0xFF0D9488).copy(alpha = 0.10f),
            Color(0xFF0284C7).copy(alpha = 0.05f),
            Color.Transparent,
        )
    }

    drawRect(
        brush = Brush.radialGradient(
            colors = p1Colors,
            center = Offset(p1X, p1Y),
            radius = p1Radius,
        ),
        blendMode = classicBlend,
    )

    val p2X = width * (0.64f + 0.22f * cos(time * 0.15f + 1.4f))
    val p2Y = height * (0.50f + 0.18f * sin(time * 0.24f + 0.9f))
    val p2Radius = maxDim * (0.64f + 0.06f * cos(time * 0.28f))

    val p2Colors = if (isDark) {
        listOf(
            Color(0xFF2563EB).copy(alpha = 0.24f),
            Color(0xFF1D4ED8).copy(alpha = 0.14f),
            Color(0xFF0F172A).copy(alpha = 0.05f),
            Color.Transparent,
        )
    } else {
        listOf(
            Color(0xFF4F46E5).copy(alpha = 0.09f),
            Color(0xFF6366F1).copy(alpha = 0.04f),
            Color.Transparent,
        )
    }

    drawRect(
        brush = Brush.radialGradient(
            colors = p2Colors,
            center = Offset(p2X, p2Y),
            radius = p2Radius,
        ),
        blendMode = classicBlend,
    )

    val p3X = width * (0.50f + 0.30f * sin(time * 0.12f + 2.8f))
    val p3Y = height * (0.20f + 0.12f * cos(time * 0.19f + 1.5f))
    val p3Radius = maxDim * 0.46f

    val p3Colors = if (isDark) {
        listOf(
            Color(0xFF38BDF8).copy(alpha = 0.18f),
            Color(0xFF0284C7).copy(alpha = 0.09f),
            Color(0xFF0369A1).copy(alpha = 0.03f),
            Color.Transparent,
        )
    } else {
        listOf(
            Color(0xFF38BDF8).copy(alpha = 0.08f),
            Color(0xFF0284C7).copy(alpha = 0.03f),
            Color.Transparent,
        )
    }

    drawRect(
        brush = Brush.radialGradient(
            colors = p3Colors,
            center = Offset(p3X, p3Y),
            radius = p3Radius,
        ),
        blendMode = classicBlend,
    )
}

private val FLUX_PATH_THREAD_LOCAL = ThreadLocal.withInitial { Path() }

private val FLUX_PALETTES = listOf(
    listOf(Color.White, Color(0xFF8AB4F8), Color(0xFFC58AF9), Color.White),
    listOf(Color(0xFF8AB4F8), Color(0xFFC58AF9), Color(0xFFE8EAED), Color(0xFF8AB4F8)),
    listOf(Color(0xFFC58AF9), Color.White, Color(0xFF8AB4F8), Color(0xFFC58AF9)),
    listOf(Color(0xFF8AB4F8), Color(0xFF80DEEA), Color(0xFFC58AF9), Color(0xFF8AB4F8)),
    listOf(Color.White, Color(0xFFC58AF9), Color(0xFF8AB4F8), Color(0xFFE8EAED)),
)
private val FLUX_LIGHT_PALETTES = listOf(
    listOf(Color(0xFF4F46E5), Color(0xFF0284C7), Color(0xFF7C3AED), Color(0xFF4F46E5)),
    listOf(Color(0xFF0284C7), Color(0xFF7C3AED), Color(0xFF059669), Color(0xFF0284C7)),
    listOf(Color(0xFF7C3AED), Color(0xFFE11D48), Color(0xFF0284C7), Color(0xFF7C3AED)),
    listOf(Color(0xFF0284C7), Color(0xFF059669), Color(0xFF7C3AED), Color(0xFF0284C7)),
    listOf(Color(0xFF4F46E5), Color(0xFF7C3AED), Color(0xFF0284C7), Color(0xFF4F46E5)),
)

private val CORE_FILAMENT_COLORS = listOf(
    Color.White,
    Color(0xFFE8EAED),
    Color.White,
)
private val CORE_FILAMENT_LIGHT_COLORS = listOf(
    Color(0xFF312E81),
    Color(0xFF1E1B4B),
    Color(0xFF312E81),
)

/**
 * FLUX_LINES:
 * Inspired by React Bits' "Flux Lines" shader aesthetic (reactbits.dev/backgrounds/flux-lines).
 */
private fun DrawScope.drawFluxLines(
    time: Float,
    width: Float,
    height: Float,
    scrollOffset: Float,
    isDark: Boolean,
) {
    if (isDark) {
        drawRect(Color(0xFF030406))
    } else {
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = LIGHT_MODE_BASE_GRADIENT.toTypedArray(),
                startY = 0f,
                endY = height,
            ),
        )
    }

    val maxDim = max(width, height)
    val fluxBlend = if (isDark) BlendMode.Screen else BlendMode.SrcOver
    val glowColor = if (isDark) Color(0xFF0F1524).copy(alpha = 0.50f) else Color(0xFF4F46E5).copy(alpha = 0.08f)

    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                glowColor,
                Color.Transparent,
            ),
            center = Offset(width * 0.5f, height * 0.52f),
            radius = maxDim * 0.70f,
        ),
        blendMode = fluxBlend,
    )

    val path: Path = FLUX_PATH_THREAD_LOCAL.get() ?: Path()
    val baseCenterY = height * 0.52f
    val idleDrift = sin(time * 0.35f) * 0.25f
    val reactiveScroll = (scrollOffset * 0.18f) + idleDrift

    val stepCount = 36
    val totalWidth = width + 80f
    val startX = -40f
    val dx = totalWidth / stepCount
    val paletteList = if (isDark) FLUX_PALETTES else FLUX_LIGHT_PALETTES

    for (i in 0 until 5) {
        path.reset()

        val waveBaseY = baseCenterY + (i - 2) * (height * 0.075f)
        val verticalDrift = sin(time * 0.42f + i * 1.45f) * (height * 0.045f)

        val freq1 = (1.35f + i * 0.22f) * 2f * PI.toFloat()
        val freq2 = (2.50f + i * 0.28f) * 2f * PI.toFloat()
        val phase1 = (time * (0.50f + i * 0.12f)) + (reactiveScroll * 2.2f) + (i * 1.5f)
        val phase2 = (-time * (0.35f + i * 0.09f)) + (reactiveScroll * 1.6f) + (i * 2.2f)

        val amp1 = height * (0.042f + (i % 2) * 0.016f)
        val amp2 = height * 0.016f

        var prevX = startX
        var prevY = computeFluxY(
            x = prevX,
            width = width,
            waveBaseY = waveBaseY,
            verticalDrift = verticalDrift,
            freq1 = freq1,
            freq2 = freq2,
            phase1 = phase1,
            phase2 = phase2,
            amp1 = amp1,
            amp2 = amp2,
        )
        path.moveTo(prevX, prevY)

        val firstNextX = prevX + dx
        val firstNextY = computeFluxY(
            x = firstNextX,
            width = width,
            waveBaseY = waveBaseY,
            verticalDrift = verticalDrift,
            freq1 = freq1,
            freq2 = freq2,
            phase1 = phase1,
            phase2 = phase2,
            amp1 = amp1,
            amp2 = amp2,
        )
        path.lineTo((prevX + firstNextX) * 0.5f, (prevY + firstNextY) * 0.5f)
        prevX = firstNextX
        prevY = firstNextY

        for (s in 2..stepCount) {
            val currX = startX + s * dx
            val currY = computeFluxY(
                x = currX,
                width = width,
                waveBaseY = waveBaseY,
                verticalDrift = verticalDrift,
                freq1 = freq1,
                freq2 = freq2,
                phase1 = phase1,
                phase2 = phase2,
                amp1 = amp1,
                amp2 = amp2,
            )
            val midX = (prevX + currX) * 0.5f
            val midY = (prevY + currY) * 0.5f
            path.quadraticTo(prevX, prevY, midX, midY)
            prevX = currX
            prevY = currY
        }
        path.lineTo(prevX, prevY)

        val brush = Brush.horizontalGradient(
            colors = paletteList[i],
            startX = -reactiveScroll * 50f,
            endX = width + 80f - reactiveScroll * 50f,
        )

        // Pass 1: Outer diffuse glow
        drawPath(
            path = path,
            brush = brush,
            alpha = if (isDark) 0.06f else 0.12f,
            style = Stroke(width = 22.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            blendMode = fluxBlend,
        )

        // Pass 2: Medium bloom halo
        drawPath(
            path = path,
            brush = brush,
            alpha = if (isDark) 0.14f else 0.22f,
            style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            blendMode = fluxBlend,
        )

        // Pass 3: Inner luminous ribbon
        drawPath(
            path = path,
            brush = brush,
            alpha = if (isDark) 0.25f else 0.35f,
            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            blendMode = fluxBlend,
        )

        // Pass 4: Bright core filament
        drawPath(
            path = path,
            brush = Brush.horizontalGradient(
                colors = if (isDark) CORE_FILAMENT_COLORS else CORE_FILAMENT_LIGHT_COLORS,
                startX = 0f,
                endX = width,
            ),
            alpha = if (isDark) 0.85f else 0.80f,
            style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            blendMode = fluxBlend,
        )
    }
}

private fun computeFluxY(
    x: Float,
    width: Float,
    waveBaseY: Float,
    verticalDrift: Float,
    freq1: Float,
    freq2: Float,
    phase1: Float,
    phase2: Float,
    amp1: Float,
    amp2: Float,
): Float {
    val u = x / width
    return waveBaseY + verticalDrift +
        sin(u * freq1 + phase1) * amp1 +
        cos(u * freq2 + phase2) * amp2
}

private data class AeroShardDescriptor(
    val seedX: Float,
    val seedY: Float,
    val depth: Float,
    val speedY: Float,
    val swayAmp: Float,
    val swayFreq: Float,
    val lengthDp: Float,
    val widthDp: Float,
    val baseAngleRad: Float,
    val spinFreq: Float,
    val tiltAmp: Float,
    val rollFreq: Float,
    val facetDarkFactor: Float,
)

private data class SurveyMarkerDescriptor(
    val seedX: Float,
    val seedY: Float,
    val depth: Float,
    val blinkFreq: Float,
    val blinkPhase: Float,
    val sizeDp: Float,
)

private val AERO_SHARD_DESCRIPTORS: List<AeroShardDescriptor> = run {
    val list = ArrayList<AeroShardDescriptor>(180)
    val random = kotlin.random.Random(1337)
    for (i in 0 until 180) {
        val sx = random.nextFloat()
        val sy = random.nextFloat()
        val depth = 0.20f + random.nextFloat() * 0.75f
        val speedY = (0.012f + random.nextFloat() * 0.028f) * (0.6f + depth * 0.5f)
        val swayAmp = 0.04f + random.nextFloat() * 0.07f
        val swayFreq = 0.35f + random.nextFloat() * 0.45f
        val lengthDp = (12f + random.nextFloat() * 18f) * (0.6f + depth * 0.6f)
        val widthDp = (4f + random.nextFloat() * 6.5f) * (0.6f + depth * 0.6f)
        val baseAngle = (random.nextFloat() * 2f - 1f) * (PI.toFloat() * 0.35f) - (PI.toFloat() * 0.22f)
        val spinFreq = 0.3f + random.nextFloat() * 0.7f
        val tiltAmp = 0.2f + random.nextFloat() * 0.35f
        val rollFreq = 0.4f + random.nextFloat() * 0.7f
        val facetDark = 0.55f + random.nextFloat() * 0.30f

        list.add(
            AeroShardDescriptor(
                seedX = sx,
                seedY = sy,
                depth = depth,
                speedY = speedY,
                swayAmp = swayAmp,
                swayFreq = swayFreq,
                lengthDp = lengthDp,
                widthDp = widthDp,
                baseAngleRad = baseAngle,
                spinFreq = spinFreq,
                tiltAmp = tiltAmp,
                rollFreq = rollFreq,
                facetDarkFactor = facetDark,
            ),
        )
    }
    list
}

private val SURVEY_MARKERS: List<SurveyMarkerDescriptor> = run {
    val list = ArrayList<SurveyMarkerDescriptor>(18)
    val random = kotlin.random.Random(42)
    for (i in 0 until 18) {
        list.add(
            SurveyMarkerDescriptor(
                seedX = 0.08f + random.nextFloat() * 0.84f,
                seedY = 0.08f + random.nextFloat() * 0.84f,
                depth = 0.25f + random.nextFloat() * 0.7f,
                blinkFreq = 0.5f + random.nextFloat() * 1.0f,
                blinkPhase = random.nextFloat() * 2f * PI.toFloat(),
                sizeDp = 4f + random.nextFloat() * 3f,
            ),
        )
    }
    list
}

private val AERO_PATH_FACET_A = ThreadLocal.withInitial { Path() }
private val AERO_PATH_FACET_B = ThreadLocal.withInitial { Path() }

/**
 * AERO_SHARDS:
 * Inspired by React Bits' "AeroShards" WebGPU aesthetic (reactbits.dev).
 * - Translucent 3D faceted glass shards (#896ABD) streaming in layered depths.
 * - Razor-sharp neon violet rim lighting (#A855F7) and specular glints.
 * - Depth-of-field focal attenuation (depthOfField = 0.75).
 * - Floating survey markers ("plus" crosshairs) blinking subtly in space.
 * - Deep cinematic vignette (0.30).
 * - Organic flowing silk-stream movement reacting to scroll gestures.
 */
private fun DrawScope.drawAeroShards(
    time: Float,
    width: Float,
    height: Float,
    scrollOffset: Float,
    isDark: Boolean,
) {
    val maxDim = max(width, height)

    if (isDark) {
        drawRect(
            brush = Brush.verticalGradient(
                0.0f to Color(0xFF050409),
                0.5f to Color(0xFF080611),
                1.0f to Color(0xFF040307),
                startY = 0f,
                endY = height,
            ),
        )
    } else {
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = LIGHT_MODE_BASE_GRADIENT.toTypedArray(),
                startY = 0f,
                endY = height,
            ),
        )
    }

    val aeroBlend = if (isDark) BlendMode.Screen else BlendMode.SrcOver
    val bloomCenter = Offset(width * 0.5f, height * 0.48f)
    val bloomRadius = maxDim * 0.68f
    val bloomColors = if (isDark) {
        listOf(
            Color(0xFF281845).copy(alpha = 0.42f),
            Color(0xFF140B26).copy(alpha = 0.20f),
            Color.Transparent,
        )
    } else {
        listOf(
            Color(0xFF8B5CF6).copy(alpha = 0.09f),
            Color(0xFFA855F7).copy(alpha = 0.04f),
            Color.Transparent,
        )
    }
    drawRect(
        brush = Brush.radialGradient(
            colors = bloomColors,
            center = bloomCenter,
            radius = bloomRadius,
        ),
        blendMode = aeroBlend,
    )

    val markerColorBase = if (isDark) Color(0xFFA855F7) else Color(0xFF7C3AED)
    for (marker in SURVEY_MARKERS) {
        val driftY = ((marker.seedY - (time * 0.008f * marker.depth)) % 1.0f + 1.0f) % 1.0f
        val my = driftY * height
        val mx = (marker.seedX * width) + (scrollOffset * 25f * marker.depth)
        val blink = (sin(time * marker.blinkFreq + marker.blinkPhase) * 0.5f + 0.5f)
        val markerAlpha = ((if (isDark) 0.06f else 0.15f) + 0.22f * blink) * marker.depth
        val sizePx = marker.sizeDp.dp.toPx()
        val markerColor = markerColorBase.copy(alpha = markerAlpha)

        drawLine(
            color = markerColor,
            start = Offset(mx - sizePx, my),
            end = Offset(mx + sizePx, my),
            strokeWidth = 1.dp.toPx(),
            blendMode = aeroBlend,
        )
        drawLine(
            color = markerColor,
            start = Offset(mx, my - sizePx),
            end = Offset(mx, my + sizePx),
            strokeWidth = 1.dp.toPx(),
            blendMode = aeroBlend,
        )
    }

    val pathA = AERO_PATH_FACET_A.get() ?: Path()
    val pathB = AERO_PATH_FACET_B.get() ?: Path()

    val shardColor = if (isDark) Color(0xFF896ABD) else Color(0xFF7C3AED)
    val accentColor = if (isDark) Color(0xFFA855F7) else Color(0xFF4F46E5)

    for (shard in AERO_SHARD_DESCRIPTORS) {
        val normY = ((shard.seedY - (time * shard.speedY)) % 1.0f + 1.0f) % 1.0f
        val cy = normY * (height + 120.dp.toPx()) - 60.dp.toPx()

        val sway = sin(normY * 3.4f * PI.toFloat() + time * shard.swayFreq + shard.seedX * 6f) * (width * shard.swayAmp)
        val cx = (shard.seedX * width) + sway + (scrollOffset * 40f * shard.depth)

        val angle = shard.baseAngleRad + sin(time * shard.spinFreq + shard.seedY * 8f) * shard.tiltAmp
        val roll = cos(time * shard.rollFreq + shard.seedX * 5f)
        val absRoll = abs(roll).coerceAtLeast(0.12f)

        val halfLen = shard.lengthDp.dp.toPx() * 0.5f
        val halfWid = (shard.widthDp.dp.toPx() * 0.5f) * absRoll

        val cosA = cos(angle)
        val sinA = sin(angle)

        val p0X = cx - cosA * halfLen
        val p0Y = cy - sinA * halfLen
        val p2X = cx + cosA * halfLen
        val p2Y = cy + sinA * halfLen

        val perpX = -sinA * halfWid
        val perpY = cosA * halfWid
        val p1X = cx + perpX
        val p1Y = cy + perpY
        val p3X = cx - perpX
        val p3Y = cy - perpY

        val dofFactor = 1.0f - (abs(shard.depth - 0.65f) * 0.75f).coerceIn(0f, 0.6f)
        val baseAlpha = ((if (isDark) 0.10f else 0.12f) + 0.55f * shard.depth) * dofFactor

        val edgeFade = when {
            normY < 0.08f -> normY / 0.08f
            normY > 0.92f -> (1.0f - normY) / 0.08f
            else -> 1.0f
        }
        val finalAlpha = (baseAlpha * edgeFade).coerceIn(0f, 1f)

        pathA.reset()
        pathA.moveTo(p0X, p0Y)
        pathA.lineTo(p1X, p1Y)
        pathA.lineTo(p2X, p2Y)
        pathA.close()

        pathB.reset()
        pathB.moveTo(p0X, p0Y)
        pathB.lineTo(p2X, p2Y)
        pathB.lineTo(p3X, p3Y)
        pathB.close()

        val alphaA = finalAlpha * (if (isDark) 1f else 0.45f)
        val alphaB = finalAlpha * shard.facetDarkFactor * (if (isDark) 0.85f else 0.35f)

        drawPath(
            path = pathA,
            color = shardColor.copy(alpha = alphaA),
            blendMode = aeroBlend,
        )
        drawPath(
            path = pathB,
            color = shardColor.copy(alpha = alphaB),
            blendMode = aeroBlend,
        )

        val rimGlint = (sin(time * 1.5f + shard.seedX * 12f) * 0.5f + 0.5f)
        val rimAlpha = (0.25f + 0.65f * rimGlint) * shard.depth * edgeFade
        val rimColor = if (isDark) {
            if (rimGlint > 0.7f) Color.White.copy(alpha = rimAlpha) else accentColor.copy(alpha = rimAlpha)
        } else {
            accentColor.copy(alpha = (rimAlpha * 0.8f).coerceIn(0f, 1f))
        }

        drawLine(
            color = rimColor,
            start = Offset(p0X, p0Y),
            end = Offset(p1X, p1Y),
            strokeWidth = 0.9.dp.toPx() * (0.6f + shard.depth * 0.6f),
            blendMode = aeroBlend,
        )

        val creaseAlpha = (0.15f + 0.25f * rimGlint) * shard.depth * edgeFade
        drawLine(
            color = (if (isDark) Color.White else Color(0xFF6366F1)).copy(alpha = creaseAlpha),
            start = Offset(p0X, p0Y),
            end = Offset(p2X, p2Y),
            strokeWidth = 0.75.dp.toPx(),
            blendMode = aeroBlend,
        )
    }

    if (isDark) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFF030206).copy(alpha = 0.30f),
                ),
                center = Offset(width * 0.5f, height * 0.5f),
                radius = maxDim * 0.72f,
            ),
        )
    }
}
