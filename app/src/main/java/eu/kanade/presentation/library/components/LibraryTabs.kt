package eu.kanade.presentation.library.components

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import tachiyomi.domain.category.model.Category

@Composable
internal fun LibraryTabs(
    categories: List<Category>,
    pagerState: PagerState,
    getItemCountForCategory: (Category) -> Int?,
    onTabItemClick: (Int) -> Unit,
) {
    if (categories.isEmpty()) return

    AuroraShelfSelectorBar(
        categories = categories,
        pagerState = pagerState,
        getItemCountForCategory = getItemCountForCategory,
        onSelectCategory = onTabItemClick,
    )
}
