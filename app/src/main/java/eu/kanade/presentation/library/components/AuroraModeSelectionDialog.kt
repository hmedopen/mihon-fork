package eu.kanade.presentation.library.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import eu.kanade.domain.ui.model.LiveBackgroundMode
import eu.kanade.presentation.components.AuroraLiveBackground
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.tachiyomi.ui.library.LibraryItem
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Check
import mihon.icons.materialsymbols.rounded.Close
import mihon.icons.materialsymbols.rounded.GridView
import mihon.icons.materialsymbols.rounded.Photo
import mihon.icons.materialsymbols.rounded.ViewCarousel
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaCover as DomainMangaCover

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuroraModeSelectionDialog(
    currentMode: LibraryDisplayMode,
    onSelectMode: (LibraryDisplayMode) -> Unit,
    onDismissRequest: () -> Unit,
    items: List<LibraryItem>,
    liveBackgroundMode: LiveBackgroundMode,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val isCoverflow = currentMode == LibraryDisplayMode.Coverflow

    // Sample manga covers from library for live glimpse emulation (fallback to placeholders if library is empty)
    val sampleCovers = remember(items) {
        if (items.isNotEmpty()) {
            items.shuffled().take(4).map { item ->
                val manga = item.libraryManga.manga
                DomainMangaCover(
                    mangaId = manga.id,
                    sourceId = manga.source,
                    isMangaFavorite = manga.favorite,
                    url = manga.thumbnailUrl,
                    lastModified = manga.coverLastModified,
                )
            }
        } else {
            emptyList()
        }
    }

    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier.fillMaxWidth(0.92f),
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = if (isDark) Color(0xFF131622).copy(alpha = 0.96f) else Color(0xFFF9FAFD).copy(alpha = 0.98f),
            border = BorderStroke(
                width = 1.dp,
                color = if (isDark) Color.White.copy(alpha = 0.14f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
            ),
            shadowElevation = 18.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Display Mode",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp,
                                color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                            ),
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Select your library viewing style",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isDark) Color.White.copy(alpha = 0.60f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }

                    Surface(
                        onClick = onDismissRequest,
                        shape = CircleShape,
                        color = if (isDark) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(34.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Close,
                                contentDescription = "Close",
                                tint = if (isDark) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Option 1: Grid Mode Glimpse Emulation
                ModeCard(
                    title = "Grid View",
                    subtitle = "Classic multi-column library view",
                    icon = MaterialSymbols.Rounded.GridView,
                    isSelected = !isCoverflow,
                    onClick = {
                        onSelectMode(LibraryDisplayMode.CompactGrid)
                        onDismissRequest()
                    },
                    isDark = isDark,
                ) {
                    // Live glimpse emulation of a little 2x2 grid over the live background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        AuroraLiveBackground(
                            mode = liveBackgroundMode,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        MiniCoverGlimpse(cover = sampleCovers.getOrNull(0), isDark = isDark)
                                        MiniCoverGlimpse(cover = sampleCovers.getOrNull(1) ?: sampleCovers.getOrNull(0), isDark = isDark)
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        MiniCoverGlimpse(cover = sampleCovers.getOrNull(2) ?: sampleCovers.getOrNull(0), isDark = isDark)
                                        MiniCoverGlimpse(cover = sampleCovers.getOrNull(3) ?: sampleCovers.getOrNull(1) ?: sampleCovers.getOrNull(0), isDark = isDark)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Option 2: Cover Flow Mode Glimpse Emulation
                ModeCard(
                    title = "Cover Flow",
                    subtitle = "3D Xbox 360 Aurora carousel with reflections",
                    icon = MaterialSymbols.Rounded.ViewCarousel,
                    isSelected = isCoverflow,
                    onClick = {
                        onSelectMode(LibraryDisplayMode.Coverflow)
                        onDismissRequest()
                    },
                    isDark = isDark,
                ) {
                    // Live glimpse emulation of a little 3D Cover Flow (3 covers) over the live background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        AuroraLiveBackground(
                            mode = liveBackgroundMode,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                val cover0 = sampleCovers.getOrNull(0)
                                val cover1 = sampleCovers.getOrNull(1) ?: cover0
                                val cover2 = sampleCovers.getOrNull(2) ?: cover0

                                // Left 3D cover
                                MiniCoverflowCard(
                                    cover = cover1,
                                    rotationY = 32f,
                                    scale = 0.78f,
                                    translationX = (-30).dp,
                                    zIndex = 1f,
                                    isDark = isDark,
                                )

                                // Right 3D cover
                                MiniCoverflowCard(
                                    cover = cover2,
                                    rotationY = -32f,
                                    scale = 0.78f,
                                    translationX = 30.dp,
                                    zIndex = 1f,
                                    isDark = isDark,
                                )

                                // Center hero cover
                                MiniCoverflowCard(
                                    cover = cover0,
                                    rotationY = 0f,
                                    scale = 1.0f,
                                    translationX = 0.dp,
                                    zIndex = 3f,
                                    isDark = isDark,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDark: Boolean,
    previewContent: @Composable () -> Unit,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val borderColor = if (isSelected) {
        primaryColor
    } else if (isDark) {
        Color.White.copy(alpha = 0.10f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (isDark) Color(0xFF1B1E2D).copy(alpha = 0.75f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            // Live Preview Glimpse Window
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.25f)),
            ) {
                previewContent()
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mode Label and Checkmark
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) primaryColor else (if (isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                            ),
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = if (isDark) Color.White.copy(alpha = 0.55f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(primaryColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Check,
                            contentDescription = "Active",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniCoverGlimpse(
    cover: DomainMangaCover?,
    isDark: Boolean,
) {
    val cardShape = RoundedCornerShape(4.dp)
    Box(
        modifier = Modifier
            .size(width = 30.dp, height = 40.dp)
            .clip(cardShape)
            .background(if (isDark) Color(0xFF232738) else Color(0xFFD6DBE5)),
        contentAlignment = Alignment.Center,
    ) {
        if (cover != null) {
            MangaCover.Book(
                data = cover,
                modifier = Modifier.fillMaxSize(),
                contentDescription = "",
                shape = cardShape,
            )
        } else {
            Icon(
                imageVector = MaterialSymbols.Rounded.Photo,
                contentDescription = null,
                tint = if (isDark) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.25f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun MiniCoverflowCard(
    cover: DomainMangaCover?,
    rotationY: Float,
    scale: Float,
    translationX: androidx.compose.ui.unit.Dp,
    zIndex: Float,
    isDark: Boolean,
) {
    val cardShape = RoundedCornerShape(4.dp)
    val cardWidth = 32.dp
    val cardHeight = 44.dp

    Column(
        modifier = Modifier
            .zIndex(zIndex)
            .graphicsLayer {
                this.rotationY = rotationY
                this.scaleX = scale
                this.scaleY = scale
                this.translationX = translationX.toPx()
                this.cameraDistance = 16f * density
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Main Cover
        Box(
            modifier = Modifier
                .size(cardWidth, cardHeight)
                .clip(cardShape)
                .background(if (isDark) Color(0xFF232738) else Color(0xFFD6DBE5)),
            contentAlignment = Alignment.Center,
        ) {
            if (cover != null) {
                MangaCover.Book(
                    data = cover,
                    modifier = Modifier.fillMaxSize(),
                    contentDescription = "",
                    shape = cardShape,
                )
            } else {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Photo,
                    contentDescription = null,
                    tint = if (isDark) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.25f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        // Mini 3D reflection
        Box(
            modifier = Modifier
                .size(cardWidth, cardHeight * 0.35f)
                .clip(cardShape)
                .graphicsLayer {
                    alpha = 0.30f
                }
                .drawWithContent {
                    drawContent()
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            0.0f to Color.Black,
                            1.0f to Color.Transparent,
                        ),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    )
                },
        ) {
            if (cover != null) {
                AsyncImage(
                    model = cover,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(cardShape)
                        .graphicsLayer { scaleY = -1f },
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.BottomCenter,
                )
            }
        }
    }
}
