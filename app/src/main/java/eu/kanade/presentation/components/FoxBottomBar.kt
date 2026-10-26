package eu.kanade.presentation.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import eu.kanade.presentation.util.Tab

val LocalBottomBarPadding = compositionLocalOf { 0.dp }

private val BottomBarHeight = 74.dp
private val InactiveCellHeight = 56.dp
private val ActiveCellHeight = 68.dp
private val CenterActiveCellHeight = 72.dp
private val ActiveShoulderDrop = 16.dp
private val ActiveBottomInset = 12.dp
private val CellCornerRadius = 3.dp

@Composable
fun FoxBottomBar(
    tabs: List<Tab>,
    currentTab: cafe.adriel.voyager.navigator.tab.Tab,
    onTabSelected: (Tab) -> Unit,
    tabBadge: @Composable (Tab) -> (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BottomBarHeight)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val selected = currentTab::class == tab::class
                        val isCenter = index == 2 // Slot #3 is the Center Hero slot (Library)

                        FoxBottomBarItem(
                            tab = tab,
                            selected = selected,
                            isCenter = isCenter,
                            slotIndex = index,
                            slotCount = tabs.size,
                            tabBadge = tabBadge,
                            onClick = { onTabSelected(tab) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FoxBottomBarItem(
    tab: Tab,
    selected: Boolean,
    isCenter: Boolean,
    slotIndex: Int,
    slotCount: Int,
    tabBadge: @Composable (Tab) -> (@Composable () -> Unit)?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val colorScheme = MaterialTheme.colorScheme

    val inactiveCellFill = if (isDark) {
        colorScheme.surfaceContainer.copy(alpha = 0.82f)
    } else {
        colorScheme.surfaceContainerLow
    }
    val activeCellFill = if (isDark) {
        colorScheme.surfaceContainerHighest
    } else {
        colorScheme.primaryContainer.copy(alpha = 0.85f)
    }

    val inactiveBorder = if (isDark) {
        colorScheme.outlineVariant.copy(alpha = 0.30f)
    } else {
        colorScheme.outlineVariant.copy(alpha = 0.45f)
    }
    val activeBorder = if (isDark) {
        colorScheme.primary.copy(alpha = 0.75f)
    } else {
        colorScheme.primary.copy(alpha = 0.65f)
    }

    val inactiveContent = colorScheme.onSurfaceVariant
    val activeContent = if (isDark) {
        colorScheme.primary
    } else {
        colorScheme.onPrimaryContainer
    }

    val interactionSource = remember { MutableInteractionSource() }
    val transition = updateTransition(targetState = selected, label = "Fox nav selection")

    val activeProgress = transition.animateFloat(
        transitionSpec = { tween(durationMillis = 200, easing = FastOutSlowInEasing) },
        label = "polygon cap",
    ) { isSelected ->
        if (isSelected) 1f else 0f
    }

    val cellHeight = transition.animateDp(
        transitionSpec = { tween(durationMillis = 200, easing = FastOutSlowInEasing) },
        label = "cell height",
    ) { isSelected ->
        if (isSelected && isCenter) {
            CenterActiveCellHeight
        } else if (isSelected) {
            ActiveCellHeight
        } else {
            InactiveCellHeight
        }
    }

    val cellColor = transition.animateColor(
        transitionSpec = { tween(durationMillis = 140) },
        label = "cell color",
    ) { isSelected ->
        if (isSelected) activeCellFill else inactiveCellFill
    }

    val borderColor = transition.animateColor(
        transitionSpec = { tween(durationMillis = 140) },
        label = "border color",
    ) { isSelected ->
        if (isSelected) activeBorder else inactiveBorder
    }

    val contentColor = transition.animateColor(
        transitionSpec = { tween(durationMillis = 120) },
        label = "content color",
    ) { isSelected ->
        if (isSelected) activeContent else inactiveContent
    }

    val iconScale = transition.animateFloat(
        transitionSpec = { tween(durationMillis = 140, easing = FastOutSlowInEasing) },
        label = "icon scale",
    ) { isSelected ->
        if (isSelected) (if (isCenter) 1.10f else 1.08f) else 1f
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .zIndex(activeProgress.value)
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(cellHeight.value)
                .drawBehind {
                    val cellPath = polygonCellPath(
                        width = size.width,
                        height = size.height,
                        slotIndex = slotIndex,
                        slotCount = slotCount,
                        activeProgress = activeProgress.value,
                        shoulderDrop = ActiveShoulderDrop.toPx(),
                        activeBottomInset = ActiveBottomInset.toPx(),
                    )

                    drawPath(
                        path = cellPath,
                        color = cellColor.value,
                    )
                    drawPath(
                        path = cellPath,
                        color = borderColor.value,
                        style = Stroke(
                            width = 1.dp.toPx(),
                            join = StrokeJoin.Round,
                            pathEffect = PathEffect.cornerPathEffect(CellCornerRadius.toPx()),
                        ),
                    )
                }
                .padding(horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Icon + Badge Box
            Box(
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = tab.options.icon!!,
                    contentDescription = tab.options.title,
                    tint = contentColor.value,
                    modifier = Modifier
                        .size(if (isCenter) 26.dp else 22.dp)
                        .graphicsLayer(
                            scaleX = iconScale.value,
                            scaleY = iconScale.value,
                        ),
                )

                val badge = tabBadge(tab)
                if (badge != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 10.dp, y = (-6).dp),
                    ) {
                        badge()
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = tab.options.title,
                color = contentColor.value,
                fontSize = 11.sp,
                lineHeight = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun polygonCellPath(
    width: Float,
    height: Float,
    slotIndex: Int,
    slotCount: Int,
    activeProgress: Float,
    shoulderDrop: Float,
    activeBottomInset: Float,
): Path {
    val isFirst = slotIndex == 0
    val isLast = slotIndex == slotCount - 1

    val inactiveLeftTop = 0f
    val inactiveRightTop = width
    val inactiveTopCenter = width / 2f
    val inactiveLeftBottom = 0f
    val inactiveRightBottom = width

    val activeLeftShoulder = if (isFirst) 0f else activeBottomInset * 0.42f
    val activeRightShoulder = if (isLast) width else width - (activeBottomInset * 0.42f)
    val activeLeftBottom = if (isFirst) 0f else activeBottomInset
    val activeRightBottom = if (isLast) width else width - activeBottomInset

    return Path().apply {
        moveTo(
            x = lerp(inactiveLeftTop, activeLeftShoulder, activeProgress),
            y = lerp(0f, shoulderDrop, activeProgress),
        )
        lineTo(
            x = lerp(inactiveTopCenter, width / 2f, activeProgress),
            y = 0f,
        )
        lineTo(
            x = lerp(inactiveRightTop, activeRightShoulder, activeProgress),
            y = lerp(0f, shoulderDrop, activeProgress),
        )
        lineTo(
            x = lerp(inactiveRightBottom, activeRightBottom, activeProgress),
            y = height,
        )
        lineTo(
            x = lerp(inactiveLeftBottom, activeLeftBottom, activeProgress),
            y = height,
        )
        close()
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + ((stop - start) * fraction)
}
