package eu.kanade.presentation.library.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.KotoHeaderButton
import eu.kanade.presentation.components.SearchToolbar
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.FilterList
import mihon.icons.materialsymbols.rounded.FlipToBack
import mihon.icons.materialsymbols.rounded.GridView
import mihon.icons.materialsymbols.rounded.Search
import mihon.icons.materialsymbols.rounded.SelectAll
import mihon.icons.materialsymbols.rounded.Tune
import mihon.icons.materialsymbols.rounded.ViewCarousel
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.Pill
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.theme.active

@Composable
fun LibraryToolbar(
    hasActiveFilters: Boolean,
    selectedCount: Int,
    title: LibraryToolbarTitle,
    displayMode: LibraryDisplayMode,
    onToggleDisplayMode: () -> Unit,
    onClickShelfEdit: () -> Unit,
    onClickUnselectAll: () -> Unit,
    onClickSelectAll: () -> Unit,
    onClickInvertSelection: () -> Unit,
    onClickFilter: () -> Unit,
    searchQuery: String?,
    onSearchQueryChange: (String?) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior?,
) = when {
    selectedCount > 0 -> LibrarySelectionToolbar(
        selectedCount = selectedCount,
        onClickUnselectAll = onClickUnselectAll,
        onClickSelectAll = onClickSelectAll,
        onClickInvertSelection = onClickInvertSelection,
    )
    searchQuery != null -> SearchToolbar(
        backgroundColor = Color.Transparent,
        searchQuery = searchQuery,
        onChangeSearchQuery = onSearchQueryChange,
        actions = {
            val filterTint = if (hasActiveFilters) MaterialTheme.colorScheme.active else LocalContentColor.current
            AppBarActions(
                listOf(
                    AppBar.Action(
                        title = stringResource(MR.strings.action_filter),
                        icon = MaterialSymbols.Rounded.FilterList,
                        iconTint = filterTint,
                        onClick = onClickFilter,
                    ),
                ),
            )
        },
        scrollBehavior = scrollBehavior,
    )
    else -> LibrarySymmetricalToolbar(
        title = title,
        displayMode = displayMode,
        hasFilters = hasActiveFilters,
        onToggleDisplayMode = onToggleDisplayMode,
        onClickShelfEdit = onClickShelfEdit,
        onClickFilter = onClickFilter,
        onSearchQueryChange = onSearchQueryChange,
    )
}

@Composable
private fun LibrarySymmetricalToolbar(
    title: LibraryToolbarTitle,
    displayMode: LibraryDisplayMode,
    hasFilters: Boolean,
    onToggleDisplayMode: () -> Unit,
    onClickShelfEdit: () -> Unit,
    onClickFilter: () -> Unit,
    onSearchQueryChange: (String?) -> Unit,
) {
    val pillAlpha = if (isSystemInDarkTheme()) 0.12f else 0.08f

    Box(
        modifier = Modifier
            .windowInsetsPadding(TopAppBarDefaults.windowInsets)
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
    ) {
        // Left 2 buttons: Shelf Edit & Mode Switch
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Button 1: Shelf Edit Button (Far Left)
            KotoHeaderButton(
                icon = MaterialSymbols.Rounded.Tune,
                contentDescription = stringResource(MR.strings.shelf_customization_title),
                onClick = onClickShelfEdit,
            )

            // Button 2: Mode Switch Button (Inner Left - Coverflow vs Grid)
            val isCoverflow = displayMode == LibraryDisplayMode.Coverflow
            KotoHeaderButton(
                icon = if (isCoverflow) MaterialSymbols.Rounded.ViewCarousel else MaterialSymbols.Rounded.GridView,
                contentDescription = stringResource(MR.strings.action_display_mode),
                onClick = onToggleDisplayMode,
            )
        }

        // Center Title: Dead-Center with bold, clean typography
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 96.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    letterSpacing = (-0.5).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // Right 2 buttons: Filter & Search
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Button 3: Filter Button (Inner Right)
            val filterTint = if (hasFilters) MaterialTheme.colorScheme.active else MaterialTheme.colorScheme.onBackground
            KotoHeaderButton(
                icon = MaterialSymbols.Rounded.FilterList,
                contentDescription = stringResource(MR.strings.action_filter),
                tint = filterTint,
                onClick = onClickFilter,
            )

            // Button 4: Search Button (Far Right)
            KotoHeaderButton(
                icon = MaterialSymbols.Rounded.Search,
                contentDescription = stringResource(MR.strings.action_search),
                onClick = { onSearchQueryChange("") },
            )
        }
    }
}


@Composable
private fun LibrarySelectionToolbar(
    selectedCount: Int,
    onClickUnselectAll: () -> Unit,
    onClickSelectAll: () -> Unit,
    onClickInvertSelection: () -> Unit,
) {
    AppBar(
        titleContent = { Text(text = "$selectedCount") },
        actions = {
            AppBarActions(
                listOf(
                    AppBar.Action(
                        title = stringResource(MR.strings.action_select_all),
                        icon = MaterialSymbols.Rounded.SelectAll,
                        onClick = onClickSelectAll,
                    ),
                    AppBar.Action(
                        title = stringResource(MR.strings.action_select_inverse),
                        icon = MaterialSymbols.Rounded.FlipToBack,
                        onClick = onClickInvertSelection,
                    ),
                ),
            )
        },
        isActionMode = true,
        onCancelActionMode = onClickUnselectAll,
    )
}

@Immutable
data class LibraryToolbarTitle(
    val text: String,
    val numberOfManga: Int? = null,
)
