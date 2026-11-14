package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import com.example.data.KeyHighlightMode
import com.example.data.KeyboardPreferences
import com.example.data.RgbGradientPreset
import kotlin.random.Random

@Stable
class RgbHighlightController(initialHue: Float = 190f) {
    private var currentHue by mutableFloatStateOf(initialHue)

    fun nextDynamicColor(): Color {
        currentHue = (currentHue + 137.508f) % 360f
        return Color.hsv(currentHue, 0.88f, 1.0f)
    }

    fun nextRandomVibrantColor(random: Random = Random.Default): Color {
        val hue = random.nextFloat() * 360f
        val saturation = 0.68f + random.nextFloat() * 0.32f
        val value = 0.86f + random.nextFloat() * 0.14f
        return Color.hsv(hue, saturation, value)
    }

    fun nextRandomGradientColors(random: Random = Random.Default): List<Color> {
        val baseHue = random.nextFloat() * 360f
        val secondHue = (baseHue + 65f + random.nextFloat() * 90f) % 360f
        val thirdHue = (baseHue + 160f + random.nextFloat() * 110f) % 360f
        return listOf(baseHue, secondHue, thirdHue).map { hue ->
            val saturation = 0.70f + random.nextFloat() * 0.30f
            val value = 0.86f + random.nextFloat() * 0.14f
            Color.hsv(hue, saturation, value)
        }
    }

    fun currentDynamicColor(): Color = Color.hsv(currentHue, 0.88f, 1.0f)
}

@Composable
fun rememberRgbHighlightController(): RgbHighlightController = remember {
    RgbHighlightController()
}

data class RgbPressAppearance(
    val fillBrush: Brush,
    val borderBrush: Brush,
    val contentColor: Color
)

fun resolveRgbPressAppearance(
    preferences: KeyboardPreferences,
    activeDynamicColor: Color,
    defaultPressedBg: Color,
    defaultPressedFg: Color,
    defaultBorder: Color,
    intensity: Float,
    activeGradientColors: List<Color> = preferences.gradientPreset.colorsArgb.map { Color(it) }
): RgbPressAppearance {
    val clamped = intensity.coerceIn(0f, 1f)
    return when (preferences.highlightMode) {
        KeyHighlightMode.OFF -> RgbPressAppearance(
            fillBrush = SolidColor(defaultPressedBg.copy(alpha = clamped * 0.9f)),
            borderBrush = SolidColor(defaultBorder.copy(alpha = 0.35f + clamped * 0.35f)),
            contentColor = defaultPressedFg
        )
        KeyHighlightMode.STATIC -> {
            val rgb = Color(preferences.staticHighlightColorArgb)
            RgbPressAppearance(
                fillBrush = SolidColor(rgb.copy(alpha = 0.32f * clamped)),
                borderBrush = SolidColor(rgb.copy(alpha = 0.95f * clamped)),
                contentColor = rgb
            )
        }
        KeyHighlightMode.DYNAMIC, KeyHighlightMode.RANDOM_RGB -> {
            RgbPressAppearance(
                fillBrush = SolidColor(activeDynamicColor.copy(alpha = 0.34f * clamped)),
                borderBrush = SolidColor(activeDynamicColor.copy(alpha = 0.95f * clamped)),
                contentColor = activeDynamicColor
            )
        }
        KeyHighlightMode.GRADIENT -> {
            val stops = if (
                preferences.gradientPreset == RgbGradientPreset.RANDOM &&
                activeGradientColors.size >= 2
            ) {
                activeGradientColors
            } else {
                preferences.gradientPreset.colorsArgb.map { Color(it) }
            }
            val fillStops = stops.map { it.copy(alpha = 0.36f * clamped) }
            val borderStops = stops.map { it.copy(alpha = 0.95f * clamped) }
            RgbPressAppearance(
                fillBrush = Brush.linearGradient(fillStops),
                borderBrush = Brush.linearGradient(borderStops),
                contentColor = stops.first()
            )
        }
    }
}
