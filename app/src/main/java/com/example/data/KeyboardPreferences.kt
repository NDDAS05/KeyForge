package com.example.data

import com.example.model.SyntaxLayout

enum class HapticStrength(val id: String, val label: String, val durationMs: Long, val amplitude: Int) {
    SOFT("soft", "Soft Tap", 10L, 60),
    CRISP("crisp", "Crisp Click", 18L, 140),
    MECHANICAL("mechanical", "Mechanical Tactile", 28L, 220);

    companion object {
        fun fromId(id: String?): HapticStrength =
            entries.firstOrNull { it.id == id } ?: CRISP
    }
}

enum class KeyHighlightMode(val id: String, val label: String) {
    OFF("off", "Off"),
    STATIC("static", "Static"),
    DYNAMIC("dynamic", "Dynamic"),
    RANDOM_RGB("random_rgb", "Random RGB"),
    GRADIENT("gradient", "Gradient");

    companion object {
        fun fromId(id: String?): KeyHighlightMode =
            entries.firstOrNull { it.id == id } ?: STATIC
    }
}

enum class RgbGradientPreset(
    val id: String,
    val label: String,
    val colorsArgb: List<Long>
) {
    CYBER_PRISM(
        id = "cyber_prism",
        label = "Cyber Prism",
        colorsArgb = listOf(0xFF22D3EE, 0xFFA855F7, 0xFFF43F5E)
    ),
    AURORA(
        id = "aurora",
        label = "Aurora",
        colorsArgb = listOf(0xFF10B981, 0xFF22D3EE, 0xFF3B82F6)
    ),
    SOLAR_FLARE(
        id = "solar_flare",
        label = "Solar Flare",
        colorsArgb = listOf(0xFFF59E0B, 0xFFFB7185, 0xFFD946EF)
    ),
    NEON_PULSE(
        id = "neon_pulse",
        label = "Neon Pulse",
        colorsArgb = listOf(0xFF84CC16, 0xFF06B6D4, 0xFFEC4899)
    ),
    RANDOM(
        id = "random",
        label = "Random",
        colorsArgb = listOf(0xFF22D3EE, 0xFFF59E0B, 0xFFEC4899, 0xFF10B981)
    );

    companion object {
        fun fromId(id: String?): RgbGradientPreset =
            entries.firstOrNull { it.id == id } ?: CYBER_PRISM
    }
}

enum class EnterKeyBehavior(
    val id: String,
    val title: String,
    val subtitle: String
) {
    PRESS_ENTER_HOLD_NEWLINE(
        id = "press_enter_hold_newline",
        title = "Tap: Enter  •  Hold: Newline",
        subtitle = "Tap triggers smart block indent or IME action; hold inserts raw \\n"
    ),
    PRESS_NEWLINE_HOLD_ENTER(
        id = "press_newline_hold_enter",
        title = "Tap: Newline  •  Hold: Enter",
        subtitle = "Tap inserts raw \\n; hold triggers smart block indent or IME action"
    );

    companion object {
        fun fromId(id: String?): EnterKeyBehavior =
            entries.firstOrNull { it.id == id } ?: PRESS_ENTER_HOLD_NEWLINE
    }
}

data class ClipboardItem(
    val id: String,
    val text: String,
    val timestampMs: Long,
    val isPinned: Boolean = false
)

data class KeyboardPreferences(
    val syntaxLayout: SyntaxLayout = SyntaxLayout.KOTLIN_C,
    val autoCloseBrackets: Boolean = true,
    val autoCloseAngleBrackets: Boolean = true,
    val smartPairBackspace: Boolean = true,
    val smartIndentOnEnter: Boolean = true,
    val hapticEnabled: Boolean = true,
    val hapticStrength: HapticStrength = HapticStrength.CRISP,
    val showSyntaxStrip: Boolean = true,
    val showKeyHints: Boolean = true,
    val darkMode: Boolean = true,
    val highlightMode: KeyHighlightMode = KeyHighlightMode.STATIC,
    val staticHighlightColorArgb: Long = 0xFF22D3EE,
    val gradientPreset: RgbGradientPreset = RgbGradientPreset.CYBER_PRISM,
    val enterKeyBehavior: EnterKeyBehavior = EnterKeyBehavior.PRESS_ENTER_HOLD_NEWLINE
)
