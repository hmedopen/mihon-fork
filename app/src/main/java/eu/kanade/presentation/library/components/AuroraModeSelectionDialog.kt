package eu.kanade.presentation.library.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.domain.ui.model.LiveBackgroundMode
import eu.kanade.presentation.components.LiveBackgroundThumbnail
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.tachiyomi.ui.library.LibraryItem
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Check
import mihon.icons.materialsymbols.rounded.Close
import mihon.icons.materialsymbols.rounded.GridView
import mihon.icons.materialsymbols.rounded.MobileRotate
import mihon.icons.materialsymbols.rounded.SortByAlpha
import mihon.icons.materialsymbols.rounded.ViewCarousel
import mihon.icons.materialsymbols.roundedfilled.Visibility
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.manga.model.MangaCover as DomainMangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Display Mode Selection Dialog.
 *
 * Designed as a horizontal slider of vertical static preview cards:
 * 1. Grid View (CompactGrid)
 * 2. Cover Flow (Coverflow)
 * 3. Carousel Ring (CarouselRing)
 * 4. Alphabetic Rail (AlphabeticRail)
 *
 * Each card features a battery-friendly static snapshot of the active wallpaper
 * with an accurate miniature rendering of user shelf items.
 * Tapping a card opens the full-screen live shelf preview screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuroraModeSelectionDialog(
    currentMode: LibraryDisplayMode,
    onSelectMode: (LibraryDisplayMode) -> Unit,
    onOpenPreview: (LibraryDisplayMode) -> Unit,
    onDismissRequest: () -> Unit,
    items: List<LibraryItem>,
    liveBackgroundMode: LiveBackgroundMode,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    var selectedOption by remember { mutableStateOf(currentMode) }

    // Sample manga covers for accurate miniature previews
    val sampleCovers = remember(items) {
        if (items.isNotEmpty()) {
            items.take(4).map { item ->
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

    val displayOptions = listOf(
        LibraryDisplayMode.CompactGrid,
        LibraryDisplayMode.Coverflow,
        LibraryDisplayMode.CarouselRing,
        LibraryDisplayMode.AlphabeticRail,
    )

    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier.fillMaxWidth(0.96f),
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = if (isDark) Color(0xFF111420).copy(alpha = 0.98f) else Color(0xFFF9FAFD).copy(alpha = 0.98f),
            border = BorderStroke(
                width = 1.dp,
                color = if (isDark) Color.White.copy(alpha = 0.14f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
            ),
            shadowElevation = 20.dp,
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

                // Horizontal Slider of Vertical Options
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(
                        items = displayOptions,
                        key = { it::class.simpleName ?: it.toString() },
                    ) { mode ->
                        val isSelected = mode == selectedOption
                        val title = when (mode) {
                            LibraryDisplayMode.Coverflow -> "Cover Flow"
                            LibraryDisplayMode.CarouselRing -> "Carousel Ring"
                            LibraryDisplayMode.AlphabeticRail -> "Alphabetic Rail"
                            else -> "Grid View"
                        }
                        val subtitle = when (mode) {
                            LibraryDisplayMode.Coverflow -> "3D Xbox 360"
                            LibraryDisplayMode.CarouselRing -> "3D Cylinder"
                            LibraryDisplayMode.AlphabeticRail -> "Wave Scrub"
                            else -> "Multi-column"
                        }

                        Column(
                            modifier = Modifier
                                .width(118.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    selectedOption = mode
                                    onOpenPreview(mode)
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            // Vertical Static Preview Card
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = if (isDark) Color(0xFF181C2A) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        if (isDark) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                    },
                                ),
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    // Static snapshot thumbnail
                                    LiveBackgroundThumbnail(
                                        mode = liveBackgroundMode,
                                        snapshotTime = 12.5f,
                                        modifier = Modifier.fillMaxSize(),
                                    )

                                    // Accurate Miniature Rendering
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(6.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        when (mode) {
                                            LibraryDisplayMode.Coverflow -> {
                                                MiniCoverflowGlimpse(sampleCovers = sampleCovers)
                                            }
                                            LibraryDisplayMode.CarouselRing -> {
                                                MiniCarouselRingGlimpse(sampleCovers = sampleCovers)
                                            }
                                            LibraryDisplayMode.AlphabeticRail -> {
                                                MiniAlphabeticRailGlimpse(sampleCovers = sampleCovers)
                                            }
                                            else -> {
                                                MiniGridGlimpse(sampleCovers = sampleCovers)
                                            }
                                        }
                                    }

                                    // Fullscreen preview hint icon
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.Black.copy(alpha = 0.50f),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(22.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = MaterialSymbols.RoundedFilled.Visibility,
                                                contentDescription = "Preview",
                                                tint = Color.White.copy(alpha = 0.85f),
                                                modifier = Modifier.size(14.dp),
                                            )
                                        }
                                    }

                                    // Selected Checkmark Pill
                                    if (isSelected) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(6.dp)
                                                .size(20.dp),
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = MaterialSymbols.Rounded.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(14.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else (if (isDark) Color.White else MaterialTheme.colorScheme.onSurface),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )

                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    color = if (isDark) Color.White.copy(alpha = 0.50f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Row: Preview Full-screen & Apply Directly
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = {
                            onOpenPreview(selectedOption)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.30f) else MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Icon(
                            imageVector = MaterialSymbols.RoundedFilled.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Preview",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }

                    Button(
                        onClick = {
                            onSelectMode(selectedOption)
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(MR.strings.action_apply),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniGridGlimpse(sampleCovers: List<DomainMangaCover>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            MiniThumb(sampleCovers.getOrNull(0))
            MiniThumb(sampleCovers.getOrNull(1) ?: sampleCovers.getOrNull(0))
        }
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            MiniThumb(sampleCovers.getOrNull(2) ?: sampleCovers.getOrNull(0))
            MiniThumb(sampleCovers.getOrNull(3) ?: sampleCovers.getOrNull(1) ?: sampleCovers.getOrNull(0))
        }
    }
}

@Composable
private fun MiniCoverflowGlimpse(sampleCovers: List<DomainMangaCover>) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val c0 = sampleCovers.getOrNull(0)
        val c1 = sampleCovers.getOrNull(1) ?: c0
        val c2 = sampleCovers.getOrNull(2) ?: c0

        // Left angled cover
        Box(
            modifier = Modifier
                .size(34.dp, 51.dp)
                .graphicsLayer {
                    translationX = -22.dp.toPx()
                    rotationY = 32f
                    scaleX = 0.80f
                    scaleY = 0.80f
                    alpha = 0.70f
                },
        ) {
            MiniThumb(c1)
        }

        // Right angled cover
        Box(
            modifier = Modifier
                .size(34.dp, 51.dp)
                .graphicsLayer {
                    translationX = 22.dp.toPx()
                    rotationY = -32f
                    scaleX = 0.80f
                    scaleY = 0.80f
                    alpha = 0.70f
                },
        ) {
            MiniThumb(c2)
        }

        // Center Hero cover
        Box(
            modifier = Modifier
                .size(42.dp, 63.dp)
                .graphicsLayer {
                    shadowElevation = 8.dp.toPx()
                },
        ) {
            MiniThumb(c0)
        }
    }
}

@Composable
private fun MiniCarouselRingGlimpse(sampleCovers: List<DomainMangaCover>) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val c0 = sampleCovers.getOrNull(0)
        val c1 = sampleCovers.getOrNull(1) ?: c0
        val c2 = sampleCovers.getOrNull(2) ?: c0

        // Rear ring elements
        Box(
            modifier = Modifier
                .size(24.dp, 36.dp)
                .graphicsLayer {
                    translationY = -24.dp.toPx()
                    scaleX = 0.50f
                    scaleY = 0.50f
                    alpha = 0.35f
                },
        ) {
            MiniThumb(c2)
        }

        // Flanking left
        Box(
            modifier = Modifier
                .size(32.dp, 48.dp)
                .graphicsLayer {
                    translationX = -24.dp.toPx()
                    rotationY = 28f
                    scaleX = 0.75f
                    scaleY = 0.75f
                    alpha = 0.75f
                },
        ) {
            MiniThumb(c1)
        }

        // Flanking right
        Box(
            modifier = Modifier
                .size(32.dp, 48.dp)
                .graphicsLayer {
                    translationX = 24.dp.toPx()
                    rotationY = -28f
                    scaleX = 0.75f
                    scaleY = 0.75f
                    alpha = 0.75f
                },
        ) {
            MiniThumb(c2)
        }

        // Front Center Hero
        Box(
            modifier = Modifier
                .size(38.dp, 57.dp)
                .graphicsLayer {
                    shadowElevation = 8.dp.toPx()
                },
        ) {
            MiniThumb(c0)
        }
    }
}

@Composable
private fun MiniAlphabeticRailGlimpse(sampleCovers: List<DomainMangaCover>) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Top Context
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(20.dp, 30.dp).graphicsLayer { alpha = 0.40f }) {
                    MiniThumb(sampleCovers.getOrNull(1))
                }
                Box(modifier = Modifier.size(20.dp, 30.dp).graphicsLayer { alpha = 0.40f }) {
                    MiniThumb(sampleCovers.getOrNull(2))
                }
            }

            // Center Active Row
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(28.dp, 42.dp)) {
                    MiniThumb(sampleCovers.getOrNull(0))
                }
                Box(modifier = Modifier.size(28.dp, 42.dp)) {
                    MiniThumb(sampleCovers.getOrNull(1) ?: sampleCovers.getOrNull(0))
                }
            }

            // Bottom Context
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(20.dp, 30.dp).graphicsLayer { alpha = 0.40f }) {
                    MiniThumb(sampleCovers.getOrNull(2))
                }
                Box(modifier = Modifier.size(20.dp, 30.dp).graphicsLayer { alpha = 0.40f }) {
                    MiniThumb(sampleCovers.getOrNull(3))
                }
            }
        }

        // Mini vertical rail dots
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(4.dp)) {}
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(7.dp)) {}
            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(4.dp)) {}
            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(4.dp)) {}
        }
    }
}

@Composable
private fun MiniThumb(cover: DomainMangaCover?) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(6.dp),
        color = Color.Black.copy(alpha = 0.45f),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.20f)),
    ) {
        if (cover != null) {
            MangaCover.Book(
                data = cover,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
