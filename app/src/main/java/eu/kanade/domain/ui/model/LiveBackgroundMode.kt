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

    // Gradient ASCII
    ASCII_HORIZON(MR.strings.pref_live_bg_ascii_horizon),
    ASCII_PINCH(MR.strings.pref_live_bg_ascii_pinch),
    ASCII_RISE(MR.strings.pref_live_bg_ascii_rise),
    ASCII_ECLIPSE(MR.strings.pref_live_bg_ascii_eclipse),

    // CRT Warp
    CRT_PURPLE(MR.strings.pref_live_bg_crt_purple),
    CRT_AMBER(MR.strings.pref_live_bg_crt_amber),
    CRT_GREEN(MR.strings.pref_live_bg_crt_green),
    CRT_VINTAGE(MR.strings.pref_live_bg_crt_vintage),

    // Light Tunnel
    LIGHT_TUNNEL_INWARD(MR.strings.pref_live_bg_light_tunnel_inward),
    LIGHT_TUNNEL_OUTWARD(MR.strings.pref_live_bg_light_tunnel_outward),
    LIGHT_TUNNEL_COSMOS(MR.strings.pref_live_bg_light_tunnel_cosmos),

    // Lightfall
    LIGHTFALL_SILVER(MR.strings.pref_live_bg_lightfall_silver),
    LIGHTFALL_VIOLET(MR.strings.pref_live_bg_lightfall_violet),
    LIGHTFALL_SYNTHWAVE(MR.strings.pref_live_bg_lightfall_synthwave),
    LIGHTFALL_EMERALD(MR.strings.pref_live_bg_lightfall_emerald),
    LIGHTFALL_EMBER(MR.strings.pref_live_bg_lightfall_ember),
    LIGHTFALL_HYPER(MR.strings.pref_live_bg_lightfall_hyper),

    // Antigravity
    ANTIGRAVITY(MR.strings.pref_live_bg_antigravity),

    // Light Rays
    LIGHT_RAYS_CYAN(MR.strings.pref_live_bg_light_rays_cyan),
    LIGHT_RAYS_ANGLED(MR.strings.pref_live_bg_light_rays_angled),

    // Plasma Wave
    PLASMA_WAVE_NEON(MR.strings.pref_live_bg_plasma_wave_neon),
    PLASMA_WAVE_SUNSET(MR.strings.pref_live_bg_plasma_wave_sunset),

    // Pixel Snow
    PIXEL_SNOW(MR.strings.pref_live_bg_pixel_snow),
    PIXEL_SNOW_ACCUMULATE(MR.strings.pref_live_bg_pixel_snow_accumulate),

    // Faulty Terminal
    FAULTY_TERMINAL_MATRIX(MR.strings.pref_live_bg_faulty_terminal_matrix),
    FAULTY_TERMINAL_BLOCKS(MR.strings.pref_live_bg_faulty_terminal_blocks),
}
