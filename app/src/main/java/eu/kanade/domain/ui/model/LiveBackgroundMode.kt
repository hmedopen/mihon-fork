package eu.kanade.domain.ui.model

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR

enum class LiveBackgroundMode(val titleRes: StringResource) {
    CLASSIC_PLACEHOLDER(MR.strings.pref_live_bg_classic),
    FLUX_LINES(MR.strings.pref_live_bg_flux_lines),
    AERO_SHARDS(MR.strings.pref_live_bg_aero_shards),
    INTERFERENCE_RIBBONS(MR.strings.pref_live_bg_interference_ribbons),
    PERSPECTIVE_GRID(MR.strings.pref_live_bg_perspective_grid),
    RING_FIELD(MR.strings.pref_live_bg_ring_field),
    GLIMMER_WRAP(MR.strings.pref_live_bg_glimmer_wrap),
    SILK_WAVES(MR.strings.pref_live_bg_silk_waves),
    ROLLING_BLINDS(MR.strings.pref_live_bg_rolling_blinds),
    MINIMAL_RIPPLE(MR.strings.pref_live_bg_minimal_ripple),
    NEURAL_FLASH(MR.strings.pref_live_bg_neural_flash),
    AURORA_BEAM(MR.strings.pref_live_bg_aurora_beam),
    PIXEL_RAIN(MR.strings.pref_live_bg_pixel_rain),
}
