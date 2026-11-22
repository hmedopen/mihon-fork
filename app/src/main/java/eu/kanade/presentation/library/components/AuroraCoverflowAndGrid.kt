package eu.kanade.presentation.library.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.library.LibraryItem
import tachiyomi.domain.library.model.LibraryManga

/**
 * Clean orchestrator for Coverflow <-> Grid library views.
 * Uses direct, smooth 200ms Crossfade for reliable, zero-glitch mode transitions.
 */
@Composable
fun AuroraCoverflowAndGrid(
    items: List<LibraryItem>,
    isGridMode: Boolean,
    columns: Int,
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

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { items.size },
    )
    val gridState = rememberLazyGridState()

    Crossfade(
        targetState = isGridMode,
        animationSpec = tween(durationMillis = 200),
        label = "LibraryModeSwitch",
        modifier = modifier.fillMaxSize(),
    ) { gridMode ->
        if (!gridMode) {
            AuroraCoverflow(
                items = items,
                contentPadding = contentPadding,
                selection = selection,
                onClickManga = onClickManga,
                onLongClickManga = onLongClickManga,
                onClickContinueReading = onClickContinueReading,
                searchQuery = searchQuery,
                onGlobalSearchClicked = onGlobalSearchClicked,
                pagerState = pagerState,
                hudAlpha = 1f,
            )
        } else {
            CleanLibraryGrid(
                items = items,
                columns = columns,
                contentPadding = contentPadding,
                selection = selection,
                onClick = onClickManga,
                onLongClick = onLongClickManga,
                searchQuery = searchQuery,
                onGlobalSearchClicked = onGlobalSearchClicked,
                gridState = gridState,
            )
        }
    }
}
