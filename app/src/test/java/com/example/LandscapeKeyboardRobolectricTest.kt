package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w850dp-h412dp-land")
class LandscapeKeyboardRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `landscape keyboard stays compact and toolbar pills do not stretch`() {
        composeTestRule.waitForIdle()

        val syntaxToggleBounds = composeTestRule
            .onNodeWithTag("syntax_layout_toggle")
            .getUnclippedBoundsInRoot()
        val autoCloseBounds = composeTestRule
            .onNodeWithTag("auto_close_toggle")
            .getUnclippedBoundsInRoot()

        assertTrue(
            "Expected compact KT/C pill width <= 105dp in landscape, was ${syntaxToggleBounds.width}",
            syntaxToggleBounds.width.value <= 105f
        )
        assertTrue(
            "Expected compact {|} pill width <= 95dp in landscape, was ${autoCloseBounds.width}",
            autoCloseBounds.width.value <= 95f
        )

        val topBarTop = syntaxToggleBounds.top
        val bottomKeyBottom = composeTestRule
            .onNodeWithTag("key_space")
            .getUnclippedBoundsInRoot()
            .bottom
        val keyboardHeightDp = (bottomKeyBottom - topBarTop).value

        assertTrue(
            "Expected 7-row landscape keyboard height <= 218dp (~53% of 412dp), was $keyboardHeightDp",
            keyboardHeightDp <= 218f
        )

        composeTestRule.onNodeWithTag("key_comma").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sandbox_code_editor").assertIsDisplayed()
    }
}
