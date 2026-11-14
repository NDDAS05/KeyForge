package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
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
        composeTestRule.onNodeWithTag("clipboard_history_button").assertIsDisplayed()
    }

    @Test
    fun `tapping syntax layout toggle switches syntax-specific character strip`() {
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("syntax_key_::").assertIsDisplayed()

        composeTestRule.onNodeWithTag("syntax_layout_toggle").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("syntax_key_#").assertIsDisplayed()

        composeTestRule.onNodeWithTag("syntax_layout_toggle").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("syntax_key_=>").assertIsDisplayed()
    }

    @Test
    fun `auto-closing bracket and cursor navigation buttons update sandbox editor`() {
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("editor_clear_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("symbol_key_lparen").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("editor_cursor_telemetry").assertTextContains("Ln 1, Col 2", substring = true)

        composeTestRule.onNodeWithTag("cursor_right_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("editor_cursor_telemetry").assertTextContains("Ln 1, Col 3", substring = true)

        composeTestRule.onNodeWithTag("cursor_left_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("editor_cursor_telemetry").assertTextContains("Ln 1, Col 2", substring = true)
    }

    @Test
    fun `clipboard history captures copied snippet and pastes it back`() {
        composeTestRule.waitForIdle()

        // Clear, insert a small token, copy it to history, clear again, then paste from history
        composeTestRule.onNodeWithTag("editor_clear_button").performClick()
        composeTestRule.onNodeWithTag("symbol_key_lbrace").performClick()
        composeTestRule.onNodeWithTag("editor_copy_button").performClick()
        composeTestRule.onNodeWithTag("editor_clear_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("editor_cursor_telemetry").assertTextContains("0 ch", substring = true)

        composeTestRule.onNodeWithTag("clipboard_history_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("clipboard_history_panel").assertIsDisplayed()

        composeTestRule.onNodeWithText("{}").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("editor_cursor_telemetry").assertTextContains("2 ch", substring = true)
    }

    @Test
    fun `settings exposes rgb highlight modes and interchangeable enter vs newline action`() {
        composeTestRule.waitForIdle()

        // Open settings sheet
        composeTestRule.onNodeWithTag("open_settings_button").performClick()
        composeTestRule.waitForIdle()

        // Verify RGB highlight options (Static, Dynamic, Gradient)
        composeTestRule.onNodeWithTag("settings_rgb_mode_static").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_rgb_mode_dynamic").performClick()
        composeTestRule.onNodeWithTag("settings_rgb_mode_gradient").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_gradient_cyber_prism")
            .performScrollTo()
            .assertIsDisplayed()

        // Interchange Enter / Newline: set Tap: Newline • Hold: Enter
        composeTestRule.onNodeWithTag("settings_enter_mode_press_newline_hold_enter")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        // Close settings via back press / dismiss
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()

        // Clear editor, type {}, then tap Enter key (should now insert raw newline -> 3 ch total)
        composeTestRule.onNodeWithTag("editor_clear_button").performClick()
        composeTestRule.onNodeWithTag("symbol_key_lbrace").performClick()
        composeTestRule.onNodeWithTag("key_enter").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("editor_cursor_telemetry").assertTextContains("Ln 2, Col 1  •  3 ch", substring = true)

        // Reset and hold Enter key (longPress should trigger Smart Enter -> expands block with 4-space indent -> 8 ch total)
        composeTestRule.onNodeWithTag("editor_clear_button").performClick()
        composeTestRule.onNodeWithTag("symbol_key_lbrace").performClick()
        composeTestRule.onNodeWithTag("key_enter").performTouchInput { longClick() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("editor_cursor_telemetry").assertTextContains("Ln 2, Col 5  •  8 ch", substring = true)
    }
}
