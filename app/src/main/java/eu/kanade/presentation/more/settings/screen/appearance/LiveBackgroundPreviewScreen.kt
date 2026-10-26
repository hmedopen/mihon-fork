package eu.kanade.presentation.more.settings.screen.appearance

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
import androidx.compose.foundation.lazy.items
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
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.ui.model.LiveBackgroundMode
import eu.kanade.presentation.components.AuroraLiveBackground
import eu.kanade.presentation.components.LiveBackgroundThumbnail
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.util.system.toast
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ArrowBack
import mihon.icons.materialsymbols.rounded.Check
import tachiyomi.core.common.i18n.stringResource as contextStringResource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

class LiveBackgroundPreviewScreen(
    private val initialMode: LiveBackgroundMode,
) : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val uiPreferences = remember { context.appGraph.uiPreferences }

        var selectedMode by remember { mutableStateOf(initialMode) }
        val isDark = isSystemInDarkTheme()

        val topBarText = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
        val topBarSub = if (isDark) Color.White.copy(alpha = 0.70f) else MaterialTheme.colorScheme.onSurfaceVariant
        val topBarBg = if (isDark) Color.Black.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        val bottomSurfaceBg = if (isDark) Color.Black.copy(alpha = 0.70f) else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f)
        val bottomBorder = if (isDark) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

        AuroraLiveBackground(
            mode = selectedMode,
            modifier = Modifier.fillMaxSize(),
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = stringResource(selectedMode.titleRes),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = topBarText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = stringResource(MR.strings.pref_live_bg_preview_subtitle),
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
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = topBarBg,
                        ),
                    )
                },
                bottomBar = {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = bottomSurfaceBg,
                        border = BorderStroke(1.dp, bottomBorder),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                        ) {
                            // Seamless interactive thumbnail slider
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(
                                    items = LiveBackgroundMode.entries,
                                    key = { it.name },
                                ) { mode ->
                                    val isSelected = mode == selectedMode
                                    Column(
                                        modifier = Modifier
                                            .width(62.dp)
                                            .clickable { selectedMode = mode },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(9f / 15f)
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
                                            LiveBackgroundThumbnail(
                                                mode = mode,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = stringResource(mode.titleRes),
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

                            Spacer(modifier = Modifier.height(14.dp))

                            // 2 Prominent Action Buttons: Back and Apply
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
                                        .height(48.dp),
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
                                    Spacer(modifier = Modifier.width(8.dp))
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
                                        uiPreferences.liveBackgroundMode.set(selectedMode)
                                        context.toast(context.contextStringResource(MR.strings.pref_live_bg_preview_applied))
                                        navigator.pop()
                                    },
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(48.dp),
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
                                    Spacer(modifier = Modifier.width(8.dp))
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
                },
            ) { paddingValues ->
                // Clean full-screen live view without any obstructive cards
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            }
        }
    }
}
