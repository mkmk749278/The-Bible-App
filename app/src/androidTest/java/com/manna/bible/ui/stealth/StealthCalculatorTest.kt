package com.manna.bible.ui.stealth

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI test for the disguised Stealth-Mode calculator lock ([StealthCalculator]).
 *
 * Drives the stateless keypad directly so no Hilt graph is needed: entering the
 * expected digits and pressing "=" must unlock, while a wrong entry must not — and
 * must reveal nothing (the display resets to 0).
 */
@RunWith(AndroidJUnit4::class)
class StealthCalculatorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun correctPinUnlocks() {
        var unlocked = false
        composeRule.setContent {
            StealthCalculator(verify = { it == "4821" }, onUnlock = { unlocked = true })
        }

        composeRule.onNodeWithText("4").performClick()
        composeRule.onNodeWithText("8").performClick()
        composeRule.onNodeWithText("2").performClick()
        composeRule.onNodeWithText("1").performClick()
        composeRule.onNodeWithText("=").performClick()

        assertTrue("entering the correct PIN should unlock", unlocked)
    }

    @Test
    fun wrongPinDoesNotUnlock() {
        var unlocked = false
        composeRule.setContent {
            StealthCalculator(verify = { it == "4821" }, onUnlock = { unlocked = true })
        }

        composeRule.onNodeWithText("9").performClick()
        composeRule.onNodeWithText("9").performClick()
        composeRule.onNodeWithText("9").performClick()
        composeRule.onNodeWithText("9").performClick()
        composeRule.onNodeWithText("=").performClick()

        assertFalse("a wrong PIN must not unlock", unlocked)
        // The display reset to 0, revealing nothing about a hidden app.
        composeRule.onNodeWithText("0").assertExists()
    }
}
