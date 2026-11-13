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
    val darkMode: Boolean = true
)
