package eu.kanade.presentation.library.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.components.AdaptiveSheet
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Close
import mihon.icons.materialsymbols.rounded.Refresh
import mihon.icons.materialsymbols.rounded.Tune
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ShelfCustomizationSheet(
    onDismissRequest: () -> Unit,
    libraryPreferences: LibraryPreferences,
    onClickManageCategories: () -> Unit,
    onClickRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AdaptiveSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // Header Row with Title and [X] Dismiss Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(MR.strings.shelf_customization_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        letterSpacing = (-0.5).sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.Close,
                        contentDescription = stringResource(MR.strings.action_cancel),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 1. Highlight & Cover Badges
            HeadingItem(MR.strings.shelf_badges_header)
            CheckboxItem(
                label = stringResource(MR.strings.action_display_unread_badge),
                pref = libraryPreferences.unreadBadge,
            )
            CheckboxItem(
                label = stringResource(MR.strings.action_display_download_badge),
                pref = libraryPreferences.downloadBadge,
            )
            CheckboxItem(
                label = stringResource(MR.strings.action_display_language_badge),
                pref = libraryPreferences.languageBadge,
            )
            CheckboxItem(
                label = stringResource(MR.strings.action_display_local_badge),
                pref = libraryPreferences.localBadge,
            )
            CheckboxItem(
                label = stringResource(MR.strings.action_display_show_continue_reading_button),
                pref = libraryPreferences.showContinueReadingButton,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Shelf Header Options
            HeadingItem(MR.strings.shelf_header_options)
            CheckboxItem(
                label = stringResource(MR.strings.action_display_show_tabs),
                pref = libraryPreferences.categoryTabs,
            )
            CheckboxItem(
                label = stringResource(MR.strings.action_display_show_number_of_items),
                pref = libraryPreferences.categoryNumberOfItems,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Shelf Actions
            HeadingItem(MR.strings.shelf_actions_header)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ShelfActionButton(
                    icon = MaterialSymbols.Rounded.Tune,
                    label = stringResource(MR.strings.action_manage_shelves),
                    onClick = onClickManageCategories,
                    modifier = Modifier.weight(1f),
                )
                ShelfActionButton(
                    icon = MaterialSymbols.Rounded.Refresh,
                    label = stringResource(MR.strings.action_refresh_shelf),
                    onClick = onClickRefresh,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ShelfActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}
