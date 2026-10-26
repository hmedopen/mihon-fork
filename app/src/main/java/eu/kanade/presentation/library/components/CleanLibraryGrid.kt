package eu.kanade.presentation.library.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.tachiyomi.ui.library.LibraryItem
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.MangaCover as DomainMangaCover
import tachiyomi.presentation.core.util.plus

@Composable
fun CleanLibraryGrid(
    items: List<LibraryItem>,
    columns: Int,
    contentPadding: PaddingValues,
    selection: Set<Long>,
    onClick: (LibraryManga) -> Unit,
    onLongClick: (LibraryManga) -> Unit,
    modifier: Modifier = Modifier,
    searchQuery: String? = null,
    onGlobalSearchClicked: () -> Unit = {},
    gridState: LazyGridState = rememberLazyGridState(),
) {
    val colCount = if (columns > 0) columns else 3
    val cardShape = RoundedCornerShape(10.dp)

    LazyVerticalGrid(
        columns = GridCells.Fixed(colCount),
        modifier = modifier.fillMaxSize(),
        state = gridState,
        contentPadding = contentPadding + PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!searchQuery.isNullOrEmpty()) {
            globalSearchItem(searchQuery, onGlobalSearchClicked)
        }

        items(
            items = items,
            key = { it.libraryManga.id },
            contentType = { "library_item" },
        ) { item ->
            val manga = item.libraryManga.manga
            val isSelected = manga.id in selection

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onClick(item.libraryManga) },
                        onLongClick = { onLongClick(item.libraryManga) },
                    ),
            ) {
                // 2f / 3f Aspect Ratio Cover Box
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f),
                    shape = cardShape,
                    color = Color.Transparent,
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f),
                    ),
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val coverData = DomainMangaCover(
                            mangaId = manga.id,
                            sourceId = manga.source,
                            isMangaFavorite = manga.favorite,
                            url = manga.thumbnailUrl,
                            lastModified = manga.coverLastModified,
                        )

                        MangaCover.Book(
                            data = coverData,
                            modifier = Modifier.fillMaxSize(),
                            contentDescription = manga.title,
                            shape = cardShape,
                        )

                        // Selection dimming overlay
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(cardShape),
                            )
                        }

                        // Compact top-right badges
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(5.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            DownloadsBadge(count = item.badges.downloadCount)
                            UnreadBadge(count = item.badges.unreadCount)
                            LanguageBadge(
                                isLocal = item.badges.isLocal,
                                sourceLanguage = item.badges.sourceLanguage,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Clean 2-line title underneath cover
                Text(
                    text = manga.title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.88f),
                        fontWeight = FontWeight.Medium,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
