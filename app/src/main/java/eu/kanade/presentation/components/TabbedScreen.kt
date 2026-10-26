package eu.kanade.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.icerock.moko.resources.StringResource
import kotlinx.coroutines.launch
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun TabbedScreen(
    titleRes: StringResource,
    tabs: List<TabContent>,
    state: PagerState = rememberPagerState { tabs.size },
    searchQuery: String? = null,
    onChangeSearchQuery: (String?) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                val tab = tabs[state.currentPage]
                val searchEnabled = tab.searchEnabled
                val isSearching = searchEnabled && searchQuery != null

                if (isSearching) {
                    SearchToolbar(
                        titleContent = { AppBarTitle(stringResource(titleRes)) },
                        searchEnabled = true,
                        searchQuery = searchQuery,
                        onChangeSearchQuery = onChangeSearchQuery,
                        actions = { AppBarActions(tab.actions) },
                    )
                } else {
                    SymmetricalHeader(
                        title = stringResource(titleRes),
                        leftContent = {
                            tab.leftActions.forEach { action ->
                                if (action is AppBar.Action) {
                                    KotoHeaderButton(
                                        icon = action.icon,
                                        contentDescription = action.title,
                                        tint = action.iconTint ?: MaterialTheme.colorScheme.onBackground,
                                        enabled = action.enabled,
                                        onClick = action.onClick,
                                    )
                                }
                            }
                        },
                        rightContent = {
                            val directActions = tab.actions.filterIsInstance<AppBar.Action>()
                            val overflowActions = tab.actions.filterIsInstance<AppBar.OverflowAction>()

                            directActions.forEach { action ->
                                KotoHeaderButton(
                                    icon = action.icon,
                                    contentDescription = action.title,
                                    tint = action.iconTint ?: MaterialTheme.colorScheme.onBackground,
                                    enabled = action.enabled,
                                    onClick = action.onClick,
                                )
                            }

                            if (overflowActions.isNotEmpty()) {
                                KotoHeaderOverflowButton(actions = overflowActions)
                            }
                        },
                    )
                }
            },
        ) { contentPadding ->
            Column(
                modifier = Modifier.padding(
                    top = contentPadding.calculateTopPadding(),
                    start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
                    end = contentPadding.calculateEndPadding(LocalLayoutDirection.current),
                ),
            ) {
                val foxTabs = tabs.map { tab ->
                    FoxTopSectionTab(
                        label = stringResource(tab.titleRes),
                        badgeCount = tab.badgeNumber,
                    )
                }

                FoxTopSectionBar(
                    tabs = foxTabs,
                    selectedIndex = state.currentPage,
                    onTabSelected = { index ->
                        scope.launch { state.animateScrollToPage(index) }
                    },
                    modifier = Modifier.padding(vertical = 4.dp),
                )

                HorizontalPager(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    verticalAlignment = Alignment.Top,
                ) { page ->
                    tabs[page].content(
                        PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                        snackbarHostState,
                    )
                }
            }
        }

        // Top-anchored floating snackbar/notification (Point 6)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                .zIndex(100f),
        )
    }
}

data class TabContent(
    val titleRes: StringResource,
    val badgeNumber: Int? = null,
    val searchEnabled: Boolean = false,
    val leftActions: List<AppBar.AppBarAction> = listOf(),
    val actions: List<AppBar.AppBarAction> = listOf(),
    val content: @Composable (contentPadding: PaddingValues, snackbarHostState: SnackbarHostState) -> Unit,
)
