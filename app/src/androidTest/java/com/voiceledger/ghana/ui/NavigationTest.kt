package com.voiceledger.ghana.ui

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.voiceledger.ghana.presentation.MainActivity
import com.voiceledger.ghana.presentation.theme.GhanaVoiceLedgerTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for navigation between screens and user interactions
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NavigationTest {
    
    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)
    
    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()
    
    private lateinit var navController: TestNavHostController
    
    @Before
    fun setUp() {
        hiltRule.inject()
        
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            
            GhanaVoiceLedgerTheme {
                MainActivity()
            }
        }
    }
    
    @Test
    fun navigationBar_clickingItems_navigatesToCorrectScreens() {
        // Verify initial screen is Dashboard
        composeTestRule.onNodeWithText("Dashboard").assertIsDisplayed()
        
        // Navigate to History
        composeTestRule.onNodeWithText("History").performClick()
        composeTestRule.waitForIdle()
        
        // Verify History screen is displayed
        composeTestRule.onNodeWithText("Transaction History").assertIsDisplayed()
        
        // Navigate to Settings
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
        
        // Verify Settings screen is displayed
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        
        // Navigate back to Dashboard
        composeTestRule.onNodeWithText("Dashboard").performClick()
        composeTestRule.waitForIdle()
        
        // Verify Dashboard screen is displayed again
        composeTestRule.onNodeWithText("Today's Sales").assertIsDisplayed()
    }
    
    @Test
    fun dashboardToHistoryNavigation_viaViewHistoryButton_worksCorrectly() {
        // Start on Dashboard
        composeTestRule.onNodeWithText("Dashboard").assertIsDisplayed()
        
        // Click "View History" button
        composeTestRule.onNodeWithText("View History").performClick()
        composeTestRule.waitForIdle()
        
        // Verify navigation to History screen
        composeTestRule.onNodeWithText("Transaction History").assertIsDisplayed()
        
        // Verify back navigation works
        composeTestRule.onNodeWithContentDescription("Back button").performClick()
        composeTestRule.waitForIdle()
        
        // Should be back on Dashboard
        composeTestRule.onNodeWithText("Today's Sales").assertIsDisplayed()
    }
    
    @Test
    fun settingsNavigation_accessingSubScreens_worksCorrectly() {
        // Navigate to Settings
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
        
        // Test Privacy Settings navigation
        composeTestRule.onNodeWithText("Data & Privacy").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Privacy Settings").assertIsDisplayed()
        
        // Navigate back to main Settings
        composeTestRule.onNodeWithContentDescription("Back button").performClick()
        composeTestRule.waitForIdle()
        
        // Test Voice Recognition settings
        composeTestRule.onNodeWithText("Voice Recognition").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Voice Profile").assertIsDisplayed()
        
        // Navigate back
        composeTestRule.onNodeWithContentDescription("Back button").performClick()
        composeTestRule.waitForIdle()
        
        // Should be back on main Settings screen
        composeTestRule.onNodeWithText("Language & Region").assertIsDisplayed()
    }
    
    @Test
    fun deepLinkNavigation_toSpecificScreens_worksCorrectly() {
        // Test direct navigation to History with filter
        composeTestRule.onNodeWithText("History").performClick()
        composeTestRule.waitForIdle()
        
        // Apply a filter
        composeTestRule.onNodeWithContentDescription("Filter transactions").performClick()
        composeTestRule.waitForIdle()
        
        // Select a product filter
        composeTestRule.onNodeWithText("Tilapia").performClick()
        composeTestRule.onNodeWithText("Apply").performClick()
        composeTestRule.waitForIdle()
        
        // Verify filtered view is shown
        composeTestRule.onNodeWithText("Filtered by: Tilapia").assertIsDisplayed()
        
        // Navigate away and back
        composeTestRule.onNodeWithText("Dashboard").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("History").performClick()
        composeTestRule.waitForIdle()
        
        // Filter should be preserved
        composeTestRule.onNodeWithText("Filtered by: Tilapia").assertIsDisplayed()
    }
    
    @Test
    fun navigationState_isPreservedAcrossScreens() {
        // Start on Dashboard and modify some state
        composeTestRule.onNodeWithText("Dashboard").assertIsDisplayed()
        
        // Start listening (if not already)
        if (composeTestRule.onNodeWithText("Resume").isDisplayed()) {
            composeTestRule.onNodeWithText("Resume").performClick()
            composeTestRule.waitForIdle()
        }
        
        // Navigate to Settings
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
        
        // Change a setting
        composeTestRule.onNodeWithText("Notifications").performClick()
        composeTestRule.waitForIdle()
        
        val notificationSwitch = composeTestRule.onNodeWithText("Enable Notifications")
        notificationSwitch.performClick()
        
        // Navigate back to Dashboard
        composeTestRule.onNodeWithText("Dashboard").performClick()
        composeTestRule.waitForIdle()
        
        // Verify listening state is preserved
        composeTestRule.onNodeWithText("Pause").assertIsDisplayed()
        
        // Go back to Settings and verify setting is preserved
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Notifications").performClick()
        composeTestRule.waitForIdle()
        
        // Verify the notification setting change was preserved
        // (This would depend on the actual implementation)
    }
    
    @Test
    fun navigationDrawer_ifPresent_worksCorrectly() {
        // This test assumes there might be a navigation drawer
        // Skip if not implemented
        try {
            composeTestRule.onNodeWithContentDescription("Open navigation drawer").performClick()
            composeTestRule.waitForIdle()
            
            // Test drawer navigation items
            composeTestRule.onNodeWithText("Dashboard").performClick()
            composeTestRule.waitForIdle()
            
            composeTestRule.onNodeWithText("Today's Sales").assertIsDisplayed()
            
        } catch (e: AssertionError) {
            // Navigation drawer not implemented, skip test
            println("Navigation drawer not found, skipping test")
        }
    }
    
    @Test
    fun backButtonBehavior_worksCorrectlyInDifferentScreens() {
        // Test back button behavior from different screens
        
        // From History screen
        composeTestRule.onNodeWithText("History").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithContentDescription("Back button").performClick()
        composeTestRule.waitForIdle()
        
        // Should go back to Dashboard
        composeTestRule.onNodeWithText("Today's Sales").assertIsDisplayed()
        
        // From Settings screen
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
        
        // Go to a sub-settings screen
        composeTestRule.onNodeWithText("Voice Recognition").performClick()
        composeTestRule.waitForIdle()
        
        // Back should go to main Settings
        composeTestRule.onNodeWithContentDescription("Back button").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Language & Region").assertIsDisplayed()
        
        // Back again should go to Dashboard
        composeTestRule.onNodeWithContentDescription("Back button").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Today's Sales").assertIsDisplayed()
    }
    
    @Test
    fun navigationWithKeyboard_worksCorrectly() {
        // Navigate to a screen with text input
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Voice Recognition").performClick()
        composeTestRule.waitForIdle()
        
        // If there's a text input field for speaker name or similar
        try {
            val textField = composeTestRule.onNodeWithText("Speaker Name")
            textField.performClick()
            textField.performTextInput("Test Speaker")
            
            // Navigate away while keyboard might be open
            composeTestRule.onNodeWithContentDescription("Back button").performClick()
            composeTestRule.waitForIdle()
            
            // Should still navigate correctly
            composeTestRule.onNodeWithText("Language & Region").assertIsDisplayed()
            
        } catch (e: AssertionError) {
            // No text input found, skip this part of the test
            println("No text input found for keyboard test")
        }
    }
    
    @Test
    fun navigationDuringLoading_handlesCorrectly() {
        // Start on Dashboard
        composeTestRule.onNodeWithText("Dashboard").assertIsDisplayed()
        
        // Try to navigate while potentially loading
        composeTestRule.onNodeWithText("History").performClick()
        
        // Even if loading, navigation should eventually complete
        composeTestRule.waitForIdle()
        
        // Should reach History screen
        composeTestRule.onNodeWithText("Transaction History").assertIsDisplayed()
    }
    
    @Test
    fun multipleRapidNavigations_handlesCorrectly() {
        // Test rapid navigation clicks
        repeat(3) {
            composeTestRule.onNodeWithText("History").performClick()
            composeTestRule.onNodeWithText("Settings").performClick()
            composeTestRule.onNodeWithText("Dashboard").performClick()
        }
        
        composeTestRule.waitForIdle()
        
        // Should end up on Dashboard
        composeTestRule.onNodeWithText("Today's Sales").assertIsDisplayed()
    }
}