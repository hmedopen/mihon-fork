package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.model.LiveBackgroundMode
import eu.kanade.presentation.components.LiveBackgroundThumbnail
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.roundedfilled.CheckCircle
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.secondaryItemAlpha

@Composable
internal fun LiveBackgroundPreferenceWidget(
    currentMode: LiveBackgroundMode,
    onItemClick: (LiveBackgroundMode) -> Unit,
) {
    BasePreferenceWidget(
        title = stringResource(MR.strings.pref_live_background),
        subcomponent = {
            LiveBackgroundList(
                currentMode = currentMode,
                onItemClick = onItemClick,
            )
        },
    )
}

@Composable
private fun LiveBackgroundList(
    currentMode: LiveBackgroundMode,
    onItemClick: (LiveBackgroundMode) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = PrefsHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        items(
            items = LiveBackgroundMode.entries,
            key = { it.name },
        ) { mode ->
            val isSelected = currentMode == mode
            Column(
                modifier = Modifier
                    .width(114.dp)
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LiveBackgroundPreviewItem(
                    mode = mode,
                    selected = isSelected,
                    onClick = { onItemClick(mode) },
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(mode.titleRes),
                    modifier = Modifier
                        .fillMaxWidth()
                        .secondaryItemAlpha(),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    minLines = 2,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun LiveBackgroundPreviewItem(
    mode: LiveBackgroundMode,
    selected: Boolean,
    onClick: () -> Unit,
) {
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
            .clickable(onClick = onClick),
    ) {
        // Pure, clean static background thumbnail
        LiveBackgroundThumbnail(
            mode = mode,
            modifier = Modifier.fillMaxSize(),
        )

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
