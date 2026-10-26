package eu.kanade.presentation.library.components

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.components.FoxScrollableTopSectionBar
import eu.kanade.presentation.components.FoxTopSectionTab
import tachiyomi.domain.category.model.Category

@Composable
internal fun LibraryTabs(
    categories: List<Category>,
    pagerState: PagerState,
    getItemCountForCategory: (Category) -> Int?,
    onTabItemClick: (Int) -> Unit,
) {
    if (categories.isEmpty()) return
    val currentPageIndex = pagerState.currentPage.coerceIn(0, categories.lastIndex)

    val tabs = categories.map { category ->
        FoxTopSectionTab(
            label = category.visualName,
            badgeCount = getItemCountForCategory(category),
        )
    }

    FoxScrollableTopSectionBar(
        tabs = tabs,
        selectedIndex = currentPageIndex,
        onTabSelected = onTabItemClick,
        visibleSlots = 4,
    )
}
