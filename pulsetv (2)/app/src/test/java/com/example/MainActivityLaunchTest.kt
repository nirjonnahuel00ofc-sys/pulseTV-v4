package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.config.FirebaseConfig
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MainActivityLaunchTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun mainActivity_launchesAndDisplaysBrandingNavAndContactMenu() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("brand_logo_admin_trigger").assertIsDisplayed()
        composeRule.onNodeWithTag("made_by_joy_label", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Made By JOY", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("channel_search_input").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_live_tv").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_live_events").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_countries").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_favorites").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_updates").assertIsDisplayed()

        // Switch to Live Events
        composeRule.onNodeWithTag("nav_live_events").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("event_search_input").assertIsDisplayed()
        composeRule.onNodeWithTag("sport_chip_Cricket").assertIsDisplayed()

        // Switch to Country Playlists
        composeRule.onNodeWithTag("nav_countries").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Country Playlists").assertIsDisplayed()

        // Three-dot menu -> Contact shows Email & Telegram
        composeRule.onNodeWithTag("more_options_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("menu_contact").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("pulsetv.bdofc65@gmail.com").assertIsDisplayed()
        composeRule.onNodeWithText("t.me/pulsetv_dl_ofc").assertIsDisplayed()
        composeRule.onNodeWithTag("close_contact_dialog_button").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun adminPin_isHiddenInUiAndUnlocksWith2110636243() {
        composeRule.waitForIdle()

        // Info dialog via three-dot menu must not leak the Admin PIN
        composeRule.onNodeWithTag("more_options_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("menu_info").performClick()
        composeRule.waitForIdle()
        assertEquals(
            0,
            composeRule.onAllNodesWithText(FirebaseConfig.DEFAULT_ADMIN_PIN, substring = true)
                .fetchSemanticsNodes().size
        )

        // Navigate to Admin Studio
        composeRule.onNodeWithTag("dialog_open_admin_button").performClick()
        composeRule.waitForIdle()

        // PIN gate must not display the PIN
        composeRule.onNodeWithTag("admin_pin_input").assertIsDisplayed()
        assertEquals(
            0,
            composeRule.onAllNodesWithText(FirebaseConfig.DEFAULT_ADMIN_PIN, substring = true)
                .fetchSemanticsNodes().size
        )

        // Entering 2110636243 unlocks to the Admin identity gate
        composeRule.onNodeWithTag("admin_pin_input").performTextInput("2110636243")
        composeRule.onNodeWithTag("admin_pin_unlock_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("admin_google_signin_button").assertIsDisplayed()
    }
}
