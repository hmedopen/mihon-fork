package eu.kanade.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.abs
import kotlin.math.roundToInt

@Immutable
data class FoxTopSectionTab(
    val label: String,
    val badgeCount: Int? = null,
    val contentDescription: String = label,
)

@Immutable
data class FoxTopSectionBarColors(
    val inactiveContainer: Color,
    val selectedContainer: Color,
    val inactiveText: Color,
    val selectedText: Color,
    val inactiveBorder: Color,
    val selectedBorder: Color,
    val pressedContainer: Color,
    val badgeContainer: Color,
    val badgeText: Color,
)

object FoxTopSectionBarDefaults {
    @Composable
    fun colors(
        isDark: Boolean = isSystemInDarkTheme(),
    ): FoxTopSectionBarColors {
        val colorScheme = MaterialTheme.colorScheme
        return FoxTopSectionBarColors(
            inactiveContainer = if (isDark) {
                colorScheme.surfaceContainerHigh.copy(alpha = 0.55f)
            } else {
                colorScheme.surfaceContainer.copy(alpha = 0.65f)
            },
            selectedContainer = colorScheme.primary.copy(alpha = if (isDark) 0.32f else 0.22f),
            inactiveText = colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
            selectedText = colorScheme.primary,
            inactiveBorder = colorScheme.outlineVariant.copy(alpha = if (isDark) 0.28f else 0.40f),
            selectedBorder = colorScheme.primary.copy(alpha = 0.85f),
            pressedContainer = colorScheme.primary.copy(alpha = 0.45f),
            badgeContainer = colorScheme.primary,
            badgeText = colorScheme.onPrimary,
        )
    }
}

/**
 * FoxDex-style category selection bar for fixed width rows (e.g. Browse tab with 3 sections).
 */
@Composable
fun FoxTopSectionBar(
    tabs: List<FoxTopSectionTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    colors: FoxTopSectionBarColors = FoxTopSectionBarDefaults.colors(),
    enabled: Boolean = true,
) {
    if (tabs.isEmpty()) return

    val targetPosition = selectedIndex.toFloat()
    val animatable = remember { Animatable(targetPosition) }
    var isAnimating by remember { mutableStateOf(false) }

    LaunchedEffect(targetPosition) {
        val diff = abs(targetPosition - animatable.value)
        if (diff > 0.05f) {
            isAnimating = true
            animatable.animateTo(
                targetValue = targetPosition,
                animationSpec = tween(
                    durationMillis = 180,
                    easing = FastOutSlowInEasing,
                ),
            )
            isAnimating = false
        } else {
            animatable.snapTo(targetPosition)
            isAnimating = false
        }
    }

    val animatedPosition = if (isAnimating) animatable.value else targetPosition

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clipToBounds()
            .padding(horizontal = 12.dp, vertical = 3.dp),
    ) {
        tabs.forEachIndexed { index, tab ->
            val activeProgress = sectionSelectionIntensity(
                selectedPosition = animatedPosition,
                index = index,
            )

            val leftDeflection = if (index > 0) {
                val pLeft = sectionSelectionIntensity(animatedPosition, index - 1)
                8.dp * (pLeft - activeProgress)
            } else {
                0.dp
            }

            val rightDeflection = if (index < tabs.size - 1) {
                val pRight = sectionSelectionIntensity(animatedPosition, index + 1)
                8.dp * (activeProgress - pRight)
            } else {
                0.dp
            }

            FoxTopSectionBarItem(
                tab = tab,
                selected = index == selectedIndex,
                activeProgress = activeProgress,
                leftDeflection = leftDeflection,
                rightDeflection = rightDeflection,
                slotIndex = index,
                slotCount = tabs.size,
                colors = colors,
                enabled = enabled,
                onClick = { onTabSelected(index) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * FoxDex-style category selection bar for scrollable shelves (e.g. Library shelves).
 * Displays exactly 4 shelves at a time on screen. Buttons are fixed size, text truncates with ellipsis.
 * Automatically scrolls to keep active shelf centered/visible when clicked or swiped.
 */
@Composable
fun FoxScrollableTopSectionBar(
    tabs: List<FoxTopSectionTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    colors: FoxTopSectionBarColors = FoxTopSectionBarDefaults.colors(),
    visibleSlots: Int = 4,
    enabled: Boolean = true,
) {
    if (tabs.isEmpty()) return

    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .padding(vertical = 3.dp),
    ) {
        val totalWidth = maxWidth
        val slotWidth = totalWidth / visibleSlots.coerceAtLeast(1)
        val slotWidthPx = with(density) { slotWidth.toPx() }

        // Auto-scroll when selectedIndex changes so active shelf is visible in the 4-slot window
        LaunchedEffect(selectedIndex, slotWidthPx) {
            val targetScrollPx = ((selectedIndex - 1.5f) * slotWidthPx).coerceAtLeast(0f)
            scrollState.animateScrollTo(
                value = targetScrollPx.roundToInt(),
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
            )
        }

        val targetPosition = selectedIndex.toFloat()
        val animatable = remember { Animatable(targetPosition) }
        var isAnimating by remember { mutableStateOf(false) }

        LaunchedEffect(targetPosition) {
            val diff = abs(targetPosition - animatable.value)
            if (diff > 0.05f) {
                isAnimating = true
                animatable.animateTo(
                    targetValue = targetPosition,
                    animationSpec = tween(
                        durationMillis = 200,
                        easing = FastOutSlowInEasing,
                    ),
                )
                isAnimating = false
            } else {
                animatable.snapTo(targetPosition)
                isAnimating = false
            }
        }

        val animatedPosition = if (isAnimating) animatable.value else targetPosition

        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
                .clipToBounds(),
        ) {
            tabs.forEachIndexed { index, tab ->
                val activeProgress = sectionSelectionIntensity(
                    selectedPosition = animatedPosition,
                    index = index,
                )

                val leftDeflection = if (index > 0) {
                    val pLeft = sectionSelectionIntensity(animatedPosition, index - 1)
                    6.dp * (pLeft - activeProgress)
                } else {
                    0.dp
                }

                val rightDeflection = if (index < tabs.size - 1) {
                    val pRight = sectionSelectionIntensity(animatedPosition, index + 1)
                    6.dp * (activeProgress - pRight)
                } else {
                    0.dp
                }

                FoxTopSectionBarItem(
                    tab = tab,
                    selected = index == selectedIndex,
                    activeProgress = activeProgress,
                    leftDeflection = leftDeflection,
                    rightDeflection = rightDeflection,
                    slotIndex = index,
                    slotCount = tabs.size,
                    colors = colors,
                    enabled = enabled,
                    onClick = { onTabSelected(index) },
                    modifier = Modifier.width(slotWidth),
                )
            }
        }
    }
}

@Composable
private fun FoxTopSectionBarItem(
    tab: FoxTopSectionTab,
    selected: Boolean,
    activeProgress: Float,
    leftDeflection: Dp,
    rightDeflection: Dp,
    slotIndex: Int,
    slotCount: Int,
    colors: FoxTopSectionBarColors,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val baseContainer = lerp(colors.inactiveContainer, colors.selectedContainer, activeProgress)
    val containerColor = if (pressed) {
        lerp(baseContainer, colors.pressedContainer, 0.35f)
    } else {
        baseContainer
    }
    val textColor = lerp(colors.inactiveText, colors.selectedText, activeProgress)
    val borderColor = lerp(colors.inactiveBorder, colors.selectedBorder, activeProgress)

    Box(
        modifier = modifier
            .fillMaxSize()
            .defaultMinSize(minHeight = 40.dp)
            .zIndex(activeProgress)
            .selectable(
                selected = selected,
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .then(
                if (enabled) {
                    Modifier.semantics {
                        contentDescription = tab.contentDescription
                    }
                } else {
                    Modifier.clearAndSetSemantics {}
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val corner = 3.dp.toPx()
            val path = sectionCellPath(
                width = size.width,
                height = size.height,
                slotIndex = slotIndex,
                slotCount = slotCount,
                leftDeflection = leftDeflection.toPx(),
                rightDeflection = rightDeflection.toPx(),
            )
            drawPath(path = path, color = containerColor)
            drawPath(
                path = path,
                color = borderColor,
                style = Stroke(
                    width = if (activeProgress > 0.5f) 1.5.dp.toPx() else 1.dp.toPx(),
                    join = StrokeJoin.Round,
                    pathEffect = PathEffect.cornerPathEffect(corner),
                ),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 6.dp),
        ) {
            Text(
                text = tab.label,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = if (activeProgress > 0.5f) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )

            if (tab.badgeCount != null && tab.badgeCount > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                    shape = CircleShape,
                    color = if (activeProgress > 0.5f) colors.badgeContainer else colors.inactiveContainer,
                    modifier = Modifier.defaultMinSize(minWidth = 18.dp, minHeight = 18.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = if (tab.badgeCount > 99) "99+" else "${tab.badgeCount}",
                            color = if (activeProgress > 0.5f) colors.badgeText else colors.inactiveText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 12.sp,
                        )
                    }
                }
            }
        }
    }
}

private fun sectionSelectionIntensity(
    selectedPosition: Float,
    index: Int,
): Float {
    return (1f - abs(selectedPosition - index)).coerceIn(0f, 1f)
}

private fun sectionCellPath(
    width: Float,
    height: Float,
    slotIndex: Int,
    slotCount: Int,
    leftDeflection: Float,
    rightDeflection: Float,
): Path {
    return Path().apply {
        moveTo(0f, 0f)
        lineTo(width, 0f)
        if (slotIndex < slotCount - 1) {
            lineTo(width + rightDeflection, height / 2f)
        }
        lineTo(width, height)
        lineTo(0f, height)
        if (slotIndex > 0) {
            lineTo(leftDeflection, height / 2f)
        }
        close()
    }
}
