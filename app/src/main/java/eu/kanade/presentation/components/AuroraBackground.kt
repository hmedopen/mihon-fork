package eu.kanade.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.domain.ui.model.LiveBackgroundMode

/**
 * Backward-compatible wrapper delegating to [AuroraLiveBackground] in [LiveBackgroundMode.CLASSIC_PLACEHOLDER].
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AuroraLiveBackground(
        mode = LiveBackgroundMode.CLASSIC_PLACEHOLDER,
        modifier = modifier,
        content = content,
    )
}
