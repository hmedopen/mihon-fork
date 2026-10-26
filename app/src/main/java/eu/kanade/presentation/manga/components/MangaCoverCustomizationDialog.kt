package eu.kanade.presentation.manga.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Delete
import mihon.icons.materialsymbols.rounded.FlipToBack
import mihon.icons.materialsymbols.rounded.Photo
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.domain.manga.model.MangaCover as DomainMangaCover
import kotlin.time.Instant
import java.io.File

@Composable
fun MangaCoverCustomizationDialog(
    manga: Manga,
    onDismissRequest: () -> Unit,
    onCoverUpdated: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val coverCache = remember { context.appGraph.coverCache }

    var hasCustomCover by remember { mutableStateOf(coverCache.hasCustomCover(manga.id)) }
    var hasBackCover by remember { mutableStateOf(coverCache.hasBackCover(manga.id)) }
    var backCoverTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    var frontCoverTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }

    // Front Cover Picker
    val frontCoverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        coverCache.setCustomCoverToCache(manga, stream)
                    }
                }
                hasCustomCover = true
                frontCoverTimestamp = System.currentTimeMillis()
                onCoverUpdated()
            }
        }
    }

    // Back Cover Picker
    val backCoverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        coverCache.setBackCoverToCache(manga.id, stream)
                    }
                }
                hasBackCover = true
                backCoverTimestamp = System.currentTimeMillis()
                onCoverUpdated()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Column {
                Text(
                    text = "Cover Settings",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    ),
                )
                Text(
                    text = manga.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Front Cover Section
                CoverSection(
                    title = "Main Front Cover",
                    hasCustom = hasCustomCover,
                    model = manga.asMangaCover().copy(
                        lastModified = Instant.fromEpochMilliseconds(frontCoverTimestamp),
                    ),
                    onPickNew = { frontCoverPicker.launch("image/*") },
                    onReset = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                coverCache.deleteCustomCover(manga.id)
                            }
                            hasCustomCover = false
                            frontCoverTimestamp = System.currentTimeMillis()
                            onCoverUpdated()
                        }
                    },
                )

                // Back Cover Section
                val backCoverFile = remember(backCoverTimestamp) {
                    coverCache.getBackCoverFile(manga.id)
                }

                CoverSection(
                    title = "Back Cover (Aurora 3D Flip)",
                    hasCustom = hasBackCover,
                    model = if (hasBackCover) backCoverFile else null,
                    emptyPlaceholder = "No back cover set.\nAdd one to enable 3D flip when zooming!",
                    onPickNew = { backCoverPicker.launch("image/*") },
                    onReset = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                coverCache.deleteBackCover(manga.id)
                            }
                            hasBackCover = false
                            backCoverTimestamp = System.currentTimeMillis()
                            onCoverUpdated()
                        }
                    },
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismissRequest) {
                Text("Done")
            }
        },
    )
}

@Composable
private fun CoverSection(
    title: String,
    hasCustom: Boolean,
    model: Any?,
    emptyPlaceholder: String = "Default cover active",
    onPickNew: () -> Unit,
    onReset: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                ),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Thumbnail
                Box(
                    modifier = Modifier
                        .size(width = 54.dp, height = 76.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .border(
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            RoundedCornerShape(8.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (model != null) {
                        AsyncImage(
                            model = model,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                        )
                    } else {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.FlipToBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Action buttons
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Button(
                        onClick = onPickNew,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                    ) {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Photo,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (hasCustom) "Change Cover" else "Add Cover",
                            fontSize = 12.sp,
                        )
                    }

                    if (hasCustom) {
                        OutlinedButton(
                            onClick = onReset,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                        ) {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Remove",
                                fontSize = 12.sp,
                            )
                        }
                    } else {
                        Text(
                            text = emptyPlaceholder,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                fontSize = 11.sp,
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
