package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import com.example.data.KeyHighlightMode
import com.example.data.KeyboardPreferences
import com.example.data.RgbGradientPreset
import com.example.ui.components.RgbHighlightController
import com.example.ui.components.resolveRgbPressAppearance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class RgbHighlightTest {

    @Test
    fun `nextRandomVibrantColor generates distinct bright saturated colors`() {
        val controller = RgbHighlightController()
        val seededRandom = Random(42)
        val generated = (1..24).map { controller.nextRandomVibrantColor(seededRandom) }

        assertTrue("Expected many distinct random colors, got ${generated.toSet().size}", generated.toSet().size >= 20)
        generated.forEach { color ->
            val maxChannel = max(color.red, max(color.green, color.blue))
            val minChannel = min(color.red, min(color.green, color.blue))
            val saturation = if (maxChannel > 0f) (maxChannel - minChannel) / maxChannel else 0f

            assertTrue("Expected bright value >= 0.85, was $maxChannel", maxChannel >= 0.85f)
            assertTrue("Expected vivid saturation >= 0.67, was $saturation", saturation >= 0.67f)
        }
    }

    @Test
    fun `nextRandomGradientColors produces three distinct vibrant stops`() {
        val controller = RgbHighlightController()
        val seededRandom = Random(99)

        val firstPressStops = controller.nextRandomGradientColors(seededRandom)
        val secondPressStops = controller.nextRandomGradientColors(seededRandom)

        assertEquals(3, firstPressStops.size)
        assertEquals(3, firstPressStops.toSet().size)
        assertNotEquals(firstPressStops, secondPressStops)

        firstPressStops.forEach { color ->
            val maxChannel = max(color.red, max(color.green, color.blue))
            val minChannel = min(color.red, min(color.green, color.blue))
            val saturation = if (maxChannel > 0f) (maxChannel - minChannel) / maxChannel else 0f
            assertTrue("Expected bright stop >= 0.85, was $maxChannel", maxChannel >= 0.85f)
            assertTrue("Expected vivid stop saturation >= 0.69, was $saturation", saturation >= 0.69f)
        }
    }

    @Test
    fun `resolveRgbPressAppearance applies RANDOM_RGB and RANDOM gradient properly`() {
        val sampledColor = Color(0xFFFF3B9A)
        val randomPrefs = KeyboardPreferences(highlightMode = KeyHighlightMode.RANDOM_RGB)
        val randomAppearance = resolveRgbPressAppearance(
            preferences = randomPrefs,
            activeDynamicColor = sampledColor,
            defaultPressedBg = Color.DarkGray,
            defaultPressedFg = Color.White,
            defaultBorder = Color.Gray,
            intensity = 1f
        )
        assertTrue(randomAppearance.fillBrush is SolidColor)
        assertEquals(sampledColor, randomAppearance.contentColor)

        val randomGradientStops = listOf(Color(0xFF00F5A0), Color(0xFF00D9F5), Color(0xFFFF4D8D))
        val randomGradientPrefs = KeyboardPreferences(
            highlightMode = KeyHighlightMode.GRADIENT,
            gradientPreset = RgbGradientPreset.RANDOM
        )
        val gradientAppearance = resolveRgbPressAppearance(
            preferences = randomGradientPrefs,
            activeDynamicColor = sampledColor,
            defaultPressedBg = Color.DarkGray,
            defaultPressedFg = Color.White,
            defaultBorder = Color.Gray,
            intensity = 1f,
            activeGradientColors = randomGradientStops
        )
        assertFalse(gradientAppearance.fillBrush is SolidColor)
        assertEquals(randomGradientStops.first(), gradientAppearance.contentColor)
    }

    @Test
    fun `enum serialization round trips random_rgb and random gradient preset`() {
        assertEquals(KeyHighlightMode.RANDOM_RGB, KeyHighlightMode.fromId("random_rgb"))
        assertEquals(RgbGradientPreset.RANDOM, RgbGradientPreset.fromId("random"))
    }
}
