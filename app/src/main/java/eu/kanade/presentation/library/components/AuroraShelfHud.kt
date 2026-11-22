package eu.kanade.presentation.library.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.ui.library.LibraryItem
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.roundedfilled.PlayArrow
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

val ShelfHudHeight = 164.dp
val ShelfHudTitleHeight = 52.dp

/**
 * Shared tactile bottom shelf HUD for Aurora Coverflow, Carousel Ring, and Alphabetic Rail.
 *
 * Provides:
 * - 2-line manga title with crisp typography and subtle shadow, clickable to open manga details.
 * - Tabular numeral position counter (`${currentIndex + 1} / $totalItems`).
 * - Vertically stacked metadata stats on the left (unread, downloads, source language).
 * - Tactile Primary Resume button on the right with quick-resume action.
 * - Global search affordance when a search query is active.
 */
@Composable
fun AuroraShelfHud(
    item: LibraryItem?,
    totalItems: Int,
    currentIndex: Int,
    onClickManga: (LibraryManga) -> Unit,
    onClickContinueReading: ((LibraryManga) -> Unit)?,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    searchQuery: String? = null,
    onGlobalSearchClicked: (() -> Unit)? = null,
    hudAlpha: Float = 1f,
) {
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ShelfHudHeight + contentPadding.calculateBottomPadding())
            .graphicsLayer { alpha = hudAlpha }
            .padding(horizontal = 24.dp)
            .padding(bottom = contentPadding.calculateBottomPadding() + 10.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
        ) {
            // Manga Title: Fixed height so 1-line or 2-lines never shift the layout
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ShelfHudTitleHeight)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = item?.libraryManga?.manga?.title.orEmpty(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp,
                        color = if (isDark) Color.White else MaterialTheme.colorScheme.onBackground,
                        shadow = Shadow(
                            color = if (isDark) Color.Black.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.75f),
                            blurRadius = 5f,
                        ),
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(
                        interactionSource = null,
                        indication = null,
                        enabled = item != null,
                        onClick = { item?.let { onClickManga(it.libraryManga) } },
                    ),
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Position Counter: Tabular Numerals positioned right below title
            if (totalItems > 0 && currentIndex >= 0) {
                Text(
                    text = "${currentIndex + 1} / $totalItems",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = if (isDark) Color.White.copy(alpha = 0.55f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.60f),
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum",
                        letterSpacing = 0.5.sp,
                        shadow = Shadow(
                            color = if (isDark) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.5f),
                            blurRadius = 3f,
                        ),
                    ),
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Dynamic Metadata & Tactile Resume Action Row
            if (item != null) {
                val badges = item.badges
                val unreadCount = badges.unreadCount ?: 0L
                val canResume = onClickContinueReading != null && unreadCount > 0L

                val languageDisplayName = remember(badges.sourceLanguage, badges.isLocal) {
                    if (badges.isLocal) {
                        "Local"
                    } else if (badges.sourceLanguage.isNotEmpty()) {
                        try {
                            val loc = java.util.Locale.forLanguageTag(badges.sourceLanguage)
                            val name = loc.getDisplayLanguage(java.util.Locale.getDefault())
                            if (name.isNotBlank()) name.replaceFirstChar { it.uppercase() } else badges.sourceLanguage.uppercase()
                        } catch (_: Exception) {
                            badges.sourceLanguage.uppercase()
                        }
                    } else {
                        ""
                    }
                }

                val statTextStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    color = if (isDark) Color.White.copy(alpha = 0.88f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.88f),
                    shadow = Shadow(
                        color = if (isDark) Color.Black.copy(alpha = 0.70f) else Color.White.copy(alpha = 0.70f),
                        blurRadius = 4f,
                    ),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Stats placed directly on the background, stacked on top of each other (no boxes)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        if (badges.unreadCount != null) {
                            Text(
                                text = "$unreadCount unread",
                                style = statTextStyle,
                            )
                        }
                        if (badges.downloadCount != null) {
                            Text(
                                text = "${badges.downloadCount} downloads",
                                style = statTextStyle,
                            )
                        }
                        if (languageDisplayName.isNotEmpty()) {
                            Text(
                                text = languageDisplayName,
                                style = statTextStyle,
                            )
                        }
                    }

                    // Tactile Primary Resume Button on the side - larger & easy to click
                    if (canResume) {
                        Surface(
                            onClick = { onClickContinueReading(item.libraryManga) },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 4.dp,
                            modifier = Modifier.height(48.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            ) {
                                Icon(
                                    imageVector = MaterialSymbols.RoundedFilled.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(MR.strings.action_resume),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 15.sp,
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            // Global search affordance when filter query is active
            if (!searchQuery.isNullOrEmpty() && onGlobalSearchClicked != null) {
                Spacer(modifier = Modifier.height(4.dp))
                GlobalSearchItem(
                    searchQuery = searchQuery,
                    onClick = onGlobalSearchClicked,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
    }
}
