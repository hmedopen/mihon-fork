package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.domain.ui.model.LiveBackgroundMode
import eu.kanade.presentation.components.LiveBackgroundThumbnail
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.GridView
import mihon.icons.materialsymbols.rounded.MobileRotate
import mihon.icons.materialsymbols.rounded.SortByAlpha
import mihon.icons.materialsymbols.rounded.ViewCarousel
import mihon.icons.materialsymbols.roundedfilled.CheckCircle
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.secondaryItemAlpha

@Composable
internal fun DisplayModePreferenceWidget(
    currentMode: LibraryDisplayMode,
    liveBackgroundMode: LiveBackgroundMode,
    onItemClick: (LibraryDisplayMode) -> Unit,
) {
    BasePreferenceWidget(
        title = stringResource(MR.strings.action_display_mode),
        subcomponent = {
            DisplayModeList(
                currentMode = currentMode,
                liveBackgroundMode = liveBackgroundMode,
                onItemClick = onItemClick,
            )
        },
    )
}

@Composable
private fun DisplayModeList(
    currentMode: LibraryDisplayMode,
    liveBackgroundMode: LiveBackgroundMode,
    onItemClick: (LibraryDisplayMode) -> Unit,
) {
    val modes = listOf(
        LibraryDisplayMode.CompactGrid,
        LibraryDisplayMode.Coverflow,
        LibraryDisplayMode.CarouselRing,
        LibraryDisplayMode.AlphabeticRail,
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = PrefsHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        items(
            items = modes,
            key = { it::class.simpleName ?: it.toString() },
        ) { mode ->
            val isSelected = currentMode == mode
            val title = when (mode) {
                LibraryDisplayMode.Coverflow -> stringResource(MR.strings.action_display_coverflow)
                LibraryDisplayMode.CarouselRing -> stringResource(MR.strings.action_display_carousel_ring)
                LibraryDisplayMode.AlphabeticRail -> stringResource(MR.strings.action_display_alphabetic_rail)
                else -> stringResource(MR.strings.action_display_grid)
            }

            Column(
                modifier = Modifier
                    .width(114.dp)
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DisplayModePreviewItem(
                    mode = mode,
                    liveBackgroundMode = liveBackgroundMode,
                    selected = isSelected,
                    onClick = { onItemClick(mode) },
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .secondaryItemAlpha(),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    minLines = 2,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Unspecified,
                )
            }
        }
    }
}

@Composable
private fun DisplayModePreviewItem(
    mode: LibraryDisplayMode,
    liveBackgroundMode: LiveBackgroundMode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val icon = when (mode) {
        LibraryDisplayMode.Coverflow -> MaterialSymbols.Rounded.ViewCarousel
        LibraryDisplayMode.CarouselRing -> MaterialSymbols.Rounded.MobileRotate
        LibraryDisplayMode.AlphabeticRail -> MaterialSymbols.Rounded.SortByAlpha
        else -> MaterialSymbols.Rounded.GridView
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .border(
                width = if (selected) 3.dp else 1.5.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    DividerDefaults.color
                },
                shape = RoundedCornerShape(16.dp),
            )
            .padding(3.dp)
            .clip(RoundedCornerShape(13.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        // Battery-friendly static live background snapshot
        LiveBackgroundThumbnail(
            mode = liveBackgroundMode,
            snapshotTime = 12.5f,
            modifier = Modifier.fillMaxSize(),
        )

        // Center stylized icon badge representing the display mode
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = CircleShape,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.60f),
                modifier = Modifier.size(38.dp),
                shadowElevation = 4.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.onPrimary else Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        // Selection checkmark indicator
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
            ) {
                Icon(
                    imageVector = MaterialSymbols.RoundedFilled.CheckCircle,
                    contentDescription = stringResource(MR.strings.selected),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
