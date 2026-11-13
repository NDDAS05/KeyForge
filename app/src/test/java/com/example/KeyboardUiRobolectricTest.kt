package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.ui.components.symbolTagName
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KeyboardUiRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `dedicated top symbol row exposes all required one-tap symbols`() {
        composeTestRule.waitForIdle()
        val requiredSymbols = listOf("(", ")", "[", "]", "{", "}", ";", "<", ">", "=", "/")
        requiredSymbols.forEach { symbol ->
            composeTestRule
                .onNodeWithTag("symbol_key_${symbolTagName(symbol)}")
                .assertIsDisplayed()
        }
    }

    @Test
    fun `tapping syntax layout toggle switches syntax-specific character strip`() {
        composeTestRule.waitForIdle()

        // Default is KT / C
        composeTestRule.onNodeWithTag("syntax_key_::").assertIsDisplayed()

        // Toggle to Python
        composeTestRule.onNodeWithTag("syntax_layout_toggle").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("syntax_key_#").assertIsDisplayed()

        // Toggle to Web / JSX
        composeTestRule.onNodeWithTag("syntax_layout_toggle").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("syntax_key_=>").assertIsDisplayed()
    }

    @Test
    fun `auto-closing bracket and cursor navigation buttons update sandbox editor`() {
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("editor_clear_button").performClick()
        composeTestRule.waitForIdle()

        // Tap ( -> inserts () with cursor at Col 2 (Ln 1, Col 2)
        composeTestRule.onNodeWithTag("symbol_key_lparen").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("editor_cursor_telemetry").assertTextContains("Ln 1, Col 2", substring = true)

        // Tap cursor right -> moves past ) to Col 3
        composeTestRule.onNodeWithTag("cursor_right_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("editor_cursor_telemetry").assertTextContains("Ln 1, Col 3", substring = true)

        // Tap cursor left -> moves back between () to Col 2
        composeTestRule.onNodeWithTag("cursor_left_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("editor_cursor_telemetry").assertTextContains("Ln 1, Col 2", substring = true)
    }
}
