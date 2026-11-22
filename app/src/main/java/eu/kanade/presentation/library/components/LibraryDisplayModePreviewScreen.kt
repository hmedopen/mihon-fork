package eu.kanade.presentation.library.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import tachiyomi.presentation.core.util.collectAsState
import eu.kanade.presentation.components.AuroraLiveBackground
import eu.kanade.presentation.components.LiveBackgroundThumbnail
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.library.LibraryItem
import eu.kanade.tachiyomi.util.system.toast
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ArrowBack
import mihon.icons.materialsymbols.rounded.Check
import mihon.icons.materialsymbols.rounded.GridView
import mihon.icons.materialsymbols.rounded.KeyboardArrowDown
import mihon.icons.materialsymbols.rounded.KeyboardArrowUp
import mihon.icons.materialsymbols.rounded.MobileRotate
import mihon.icons.materialsymbols.rounded.SortByAlpha
import mihon.icons.materialsymbols.rounded.ViewCarousel
import tachiyomi.core.common.i18n.stringResource as contextStringResource
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.manga.model.MangaCover as DomainMangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Full-screen live shelf preview for Library Display Modes.
 *
 * Capabilities:
 * - Accurate full-screen rendering of the user's real shelf items, covers, titles, and stats.
 * - Live switching between Grid View, Coverflow, Carousel Ring, and Alphabetic Rail.
 * - Collapsible bottom drawer with an arrow-down button that slides the window down
 *   for a 100% unobstructed, crystal-clear view of the entire shelf.
 * - Compact expand pill when collapsed to easily restore the controls.
 * - Back and Apply actions with persistence via LibraryPreferences.
 */
class LibraryDisplayModePreviewScreen(
    private val initialMode: LibraryDisplayMode,
    private val items: List<LibraryItem> = emptyList(),
) : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val uiPreferences = remember { context.appGraph.uiPreferences }
        val libraryPreferences = remember { context.appGraph.libraryPreferences }

        val liveBackgroundMode by uiPreferences.liveBackgroundMode().collectAsState()
        var selectedMode by remember { mutableStateOf(initialMode) }
        var isDrawerCollapsed by remember { mutableStateOf(false) }

        val displayedItems by androidx.compose.runtime.produceState(initialValue = items) {
            if (items.isEmpty()) {
                val favorites = context.appGraph.getFavorites.await()
                value = favorites.map { manga ->
                    LibraryItem(
                        libraryManga = tachiyomi.domain.library.model.LibraryManga(
                            manga = manga,
                            categories = emptyList(),
                            totalChapters = 0,
                            readCount = 0,
                            bookmarkCount = 0,
                            latestUpload = null,
                            chapterFetchedAt = null,
                            lastRead = null,
                        ),
                        downloadCount = 0,
                        unreadCount = 0,
                        isLocal = false,
                        sourceName = "",
                        sourceLanguage = "",
                        badges = LibraryItem.Badges(
                            downloadCount = null,
                            unreadCount = null,
                            isLocal = false,
                            sourceLanguage = "",
                        ),
                    )
                }
            }
        }

        val isDark = isSystemInDarkTheme()
        val topBarText = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
        val topBarSub = if (isDark) Color.White.copy(alpha = 0.70f) else MaterialTheme.colorScheme.onSurfaceVariant
        val topBarBg = if (isDark) Color.Black.copy(alpha = 0.40f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        val bottomSurfaceBg = if (isDark) Color(0xFF0F121C).copy(alpha = 0.92f) else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f)
        val bottomBorder = if (isDark) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

        val modeTitle = when (selectedMode) {
            LibraryDisplayMode.Coverflow -> stringResource(MR.strings.action_display_coverflow)
            LibraryDisplayMode.CarouselRing -> stringResource(MR.strings.action_display_carousel_ring)
            LibraryDisplayMode.AlphabeticRail -> stringResource(MR.strings.action_display_alphabetic_rail)
            else -> stringResource(MR.strings.action_display_grid)
        }

        AuroraLiveBackground(
            mode = liveBackgroundMode,
            modifier = Modifier.fillMaxSize(),
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = modeTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = topBarText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "Full-screen live shelf preview",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = topBarSub,
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { navigator.pop() }) {
                                Icon(
                                    imageVector = MaterialSymbols.AutoMirroredRounded.ArrowBack,
                                    contentDescription = stringResource(MR.strings.action_back),
                                    tint = topBarText,
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = topBarBg),
                    )
                },
                bottomBar = {
                    // Collapsible Bottom Controls Drawer
                    Box(modifier = Modifier.fillMaxWidth()) {
                        AnimatedVisibility(
                            visible = !isDrawerCollapsed,
                            enter = slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = tween(280, easing = FastOutSlowInEasing),
                            ) + fadeIn(),
                            exit = slideOutVertically(
                                targetOffsetY = { it },
                                animationSpec = tween(240, easing = FastOutSlowInEasing),
                            ) + fadeOut(),
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = bottomSurfaceBg,
                                border = BorderStroke(1.dp, bottomBorder),
                                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    // Section Header with Arrow Down button to drop drawer down
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = "Select Viewing Style",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isDark) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            ),
                                        )

                                        // Arrow Down Button to drop selection window down
                                        Surface(
                                            onClick = { isDrawerCollapsed = true },
                                            shape = CircleShape,
                                            color = if (isDark) Color.White.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.size(30.dp),
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = MaterialSymbols.Rounded.KeyboardArrowDown,
                                                    contentDescription = "Collapse Preview Controls",
                                                    tint = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            }
                                        }
                                    }

                                    // Mode Slider with 4 choices
                                    val availableModes = listOf(
                                        LibraryDisplayMode.CompactGrid,
                                        LibraryDisplayMode.Coverflow,
                                        LibraryDisplayMode.CarouselRing,
                                        LibraryDisplayMode.AlphabeticRail,
                                    )

                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        items(
                                            items = availableModes,
                                            key = { it::class.simpleName ?: it.toString() },
                                        ) { mode ->
                                            val isSelected = mode == selectedMode
                                            val title = when (mode) {
                                                LibraryDisplayMode.Coverflow -> "Cover Flow"
                                                LibraryDisplayMode.CarouselRing -> "Carousel Ring"
                                                LibraryDisplayMode.AlphabeticRail -> "Alphabetic Rail"
                                                else -> "Grid View"
                                            }
                                            val icon = when (mode) {
                                                LibraryDisplayMode.Coverflow -> MaterialSymbols.Rounded.ViewCarousel
                                                LibraryDisplayMode.CarouselRing -> MaterialSymbols.Rounded.MobileRotate
                                                LibraryDisplayMode.AlphabeticRail -> MaterialSymbols.Rounded.SortByAlpha
                                                else -> MaterialSymbols.Rounded.GridView
                                            }

                                            Column(
                                                modifier = Modifier
                                                    .width(72.dp)
                                                    .clickable { selectedMode = mode },
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .aspectRatio(9f / 13f)
                                                        .border(
                                                            width = if (isSelected) 2.5.dp else 1.dp,
                                                            color = if (isSelected) {
                                                                MaterialTheme.colorScheme.primary
                                                            } else {
                                                                if (isDark) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.50f)
                                                            },
                                                            shape = RoundedCornerShape(10.dp),
                                                        )
                                                        .padding(2.dp)
                                                        .clip(RoundedCornerShape(8.dp)),
                                                ) {
                                                    // Static live background thumbnail
                                                    LiveBackgroundThumbnail(
                                                        mode = liveBackgroundMode,
                                                        snapshotTime = 12.5f,
                                                        modifier = Modifier.fillMaxSize(),
                                                    )

                                                    // Centered mode icon
                                                    Box(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentAlignment = Alignment.Center,
                                                    ) {
                                                        Surface(
                                                            shape = CircleShape,
                                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.60f),
                                                            modifier = Modifier.size(28.dp),
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Icon(
                                                                    imageVector = icon,
                                                                    contentDescription = title,
                                                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White,
                                                                    modifier = Modifier.size(16.dp),
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))

                                                Text(
                                                    text = title,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 10.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    ),
                                                    color = if (isSelected) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        if (isDark) Color.White.copy(alpha = 0.80f) else MaterialTheme.colorScheme.onSurfaceVariant
                                                    },
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center,
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Action Buttons: Back & Apply
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        // 1. Back Button
                                        OutlinedButton(
                                            onClick = { navigator.pop() },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(46.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                                            ),
                                        ) {
                                            Icon(
                                                imageVector = MaterialSymbols.AutoMirroredRounded.ArrowBack,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = stringResource(MR.strings.action_back),
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                ),
                                            )
                                        }

                                        // 2. Apply Button
                                        Button(
                                            onClick = {
                                                libraryPreferences.displayMode.set(selectedMode)
                                                context.toast(context.contextStringResource(MR.strings.action_display_mode_preview_applied))
                                                navigator.pop()
                                            },
                                            modifier = Modifier
                                                .weight(1.3f)
                                                .height(46.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                            ),
                                        ) {
                                            Icon(
                                                imageVector = MaterialSymbols.Rounded.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = stringResource(MR.strings.action_apply),
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                ),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Collapsed Floating Pill with Arrow Up to restore controls
                        if (isDrawerCollapsed) {
                            Surface(
                                onClick = { isDrawerCollapsed = false },
                                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                                color = bottomSurfaceBg,
                                border = BorderStroke(1.dp, bottomBorder),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 0.dp),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = MaterialSymbols.Rounded.KeyboardArrowUp,
                                        contentDescription = "Expand Display Modes",
                                        tint = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Text(
                                        text = "Modes",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                },
            ) { paddingValues ->
                // Full-screen interactive shelf preview with seamless mode crossfade
                Crossfade(
                    targetState = selectedMode,
                    animationSpec = tween(200),
                    label = "PreviewModeCrossfade",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                ) { mode ->
                    when (mode) {
                        LibraryDisplayMode.Coverflow -> {
                            AuroraCoverflow(
                                items = displayedItems,
                                contentPadding = PaddingValues(0.dp),
                                selection = emptySet(),
                                onClickManga = {},
                                onLongClickManga = {},
                                onClickContinueReading = null,
                                searchQuery = null,
                                onGlobalSearchClicked = {},
                            )
                        }
                        LibraryDisplayMode.CarouselRing -> {
                            CarouselRingDisplay(
                                items = displayedItems,
                                contentPadding = PaddingValues(0.dp),
                                selection = emptySet(),
                                onClickManga = {},
                                onLongClickManga = {},
                                onClickContinueReading = null,
                                searchQuery = null,
                                onGlobalSearchClicked = {},
                            )
                        }
                        LibraryDisplayMode.AlphabeticRail -> {
                            AlphabeticRailDisplay(
                                items = displayedItems,
                                contentPadding = PaddingValues(0.dp),
                                selection = emptySet(),
                                onClickManga = {},
                                onLongClickManga = {},
                                onClickContinueReading = null,
                                searchQuery = null,
                                onGlobalSearchClicked = {},
                            )
                        }
                        else -> {
                            CleanLibraryGrid(
                                items = displayedItems,
                                columns = 3,
                                contentPadding = PaddingValues(8.dp),
                                selection = emptySet(),
                                onClick = {},
                                onLongClick = {},
                                searchQuery = null,
                                onGlobalSearchClicked = {},
                                gridState = rememberLazyGridState(),
                            )
                        }
                    }
                }
            }
        }
    }
}
