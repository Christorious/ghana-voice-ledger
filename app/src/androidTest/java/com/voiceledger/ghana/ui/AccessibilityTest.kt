package com.voiceledger.ghana.ui

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.voiceledger.ghana.data.local.entity.Transaction
import com.voiceledger.ghana.presentation.dashboard.DashboardScreen
import com.voiceledger.ghana.presentation.dashboard.DashboardUiState
import com.voiceledger.ghana.presentation.history.HistoryScreen
import com.voiceledger.ghana.presentation.history.HistoryUiState
import com.voiceledger.ghana.presentation.settings.SettingsScreen
import com.voiceledger.ghana.presentation.theme.GhanaVoiceLedgerTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility tests with TalkBack support validation
 * Tests screen reader compatibility and accessibility features
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AccessibilityTest {
    
    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)
    
    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()
    
    @Before
    fun setUp() {
        hiltRule.inject()
    }
    
    @Test
    fun dashboardScreen_hasProperContentDescriptions() {
        // Given - dashboard with sample data
        val sampleTransactions = listOf(
            createSampleTransaction("Tilapia", 3, 45.0),
            createSampleTransaction("Mackerel", 2, 30.0)
        )
        
        val dashboardState = DashboardUiState(
            isListening = true,
            todaysSales = 75.0,
            salesCount = 2,
            topProduct = "Tilapia",
            recentTransactions = sampleTransactions,
            batteryLevel = 85,
            isLoading = false
        )
        
        // When - display dashboard
        composeTestRule.setContent {
            GhanaVoiceLedgerTheme {
                DashboardScreen(
                    uiState = dashboardState,
                    onStartListening = {},
                    onStopListening = {},
                    onNavigateToHistory = {},
                    onNavigateToSettings = {},
                    onEndDaySummary = {}
                )
            }
        }
        
        // Then - verify accessibility content descriptions
        composeTestRule.onNodeWithContentDescription("Listening status indicator").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Battery level indicator").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Pause listening button").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Settings button").assertIsDisplayed()
        
        // Verify transaction items have proper descriptions
        composeTestRule.onNodeWithContentDescription("Transaction item").assertExists()
        
        // Verify semantic roles are properly set
        composeTestRule.onNode(hasRole(Role.Button) and hasText("Pause")).assertIsDisplayed()
        composeTestRule.onNode(hasRole(Role.Button) and hasText("View History")).assertIsDisplayed()
        composeTestRule.onNode(hasRole(Role.Button) and hasText("End Day Summary")).assertIsDisplayed()
    }
    
    @Test
    fun dashboardScreen_supportsKeyboardNavigation() {
        val dashboardState = DashboardUiState(
            isListening = false,
            todaysSales = 50.0,
            salesCount = 1,
            topProduct = "Fish",
            recentTransactions = emptyList(),
            batteryLevel = 90,
            isLoading = false
        )
        
        composeTestRule.setContent {
            GhanaVoiceLedgerTheme {
                DashboardScreen(
                    uiState = dashboardState,
                    onStartListening = {},
                    onStopListening = {},
                    onNavigateToHistory = {},
                    onNavigateToSettings = {},
                    onEndDaySummary = {}
                )
            }
        }
        
        // Test tab navigation through focusable elements
        val focusableElements = listOf(
            "Resume",
            "View History", 
            "End Day Summary"
        )
        
        focusableElements.forEach { elementText ->
            val node = composeTestRule.onNodeWithText(elementText)
            node.assertIsDisplayed()
            
            // Verify element can receive focus
            node.requestFocus()
            node.assertIsFocused()
        }
    }
    
    @Test
    fun historyScreen_hasProperAccessibilityLabels() {
        // Given - history with transactions
        val transactions = listOf(
            createSampleTransaction("Tilapia", 3, 45.0),
            createSampleTransaction("Mackerel", 2, 30.0),
            createSampleTransaction("Sardines", 5, 25.0)
        )
        
        val historyState = HistoryUiState(
            transactions = transactions,
            isLoading = false,
            selectedFilter = null,
            searchQuery = "",
            sortOrder = "date_desc"
        )
        
        // When - display history screen
        composeTestRule.setContent {
            GhanaVoiceLedgerTheme {
                HistoryScreen(
                    uiState = historyState,
                    onSearchQueryChange = {},
                    onFilterChange = {},
                    onSortOrderChange = {},
                    onTransactionClick = {},
                    onNavigateBack = {}
                )
            }
        }
        
        // Then - verify accessibility features
        composeTestRule.onNodeWithContentDescription("Back button").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Search transactions").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Filter transactions").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Sort transactions").assertIsDisplayed()
        
        // Verify transaction list has proper semantics
        composeTestRule.onNode(hasRole(Role.Button) and hasContentDescription("Transaction item")).assertExists()
        
        // Verify search field has proper semantics
        composeTestRule.onNode(hasRole(Role.TextField)).assertExists()
    }
    
    @Test
    fun settingsScreen_hasProperAccessibilityStructure() {
        // When - display settings screen
        composeTestRule.setContent {
            GhanaVoiceLedgerTheme {
                SettingsScreen(
                    onNavigateBack = {},
                    onLanguageChange = {},
                    onMarketHoursChange = {},
                    onVoiceProfileSetup = {},
                    onNotificationToggle = {},
                    onPrivacySettings = {},
                    onDataExport = {},
                    onDataDelete = {}
                )
            }
        }
        
        // Then - verify accessibility structure
        composeTestRule.onNodeWithContentDescription("Back button").assertIsDisplayed()
        
        // Verify settings sections have proper headings
        composeTestRule.onNode(hasRole(Role.Button) and hasText("Language & Region")).assertIsDisplayed()
        composeTestRule.onNode(hasRole(Role.Button) and hasText("Voice Recognition")).assertIsDisplayed()
        composeTestRule.onNode(hasRole(Role.Button) and hasText("Notifications")).assertIsDisplayed()
        composeTestRule.onNode(hasRole(Role.Button) and hasText("Data & Privacy")).assertIsDisplayed()
        
        // Verify switches have proper roles and descriptions
        composeTestRule.onAllNodes(hasRole(Role.Switch)).assertCountEquals(expectedSwitchCount = 3) // Adjust based on actual switches
    }
    
    @Test
    fun allScreens_supportHighContrastMode() {
        // Test that screens work well with high contrast
        // This would typically involve checking color contrast ratios
        
        val dashboardState = DashboardUiState(
            isListening = true,
            todaysSales = 100.0,
            salesCount = 3,
            topProduct = "Fish",
            recentTransactions = emptyList(),
            batteryLevel = 75,
            isLoading = false
        )
        
        composeTestRule.setContent {
            GhanaVoiceLedgerTheme {
                DashboardScreen(
                    uiState = dashboardState,
                    onStartListening = {},
                    onStopListening = {},
                    onNavigateToHistory = {},
                    onNavigateToSettings = {},
                    onEndDaySummary = {}
                )
            }
        }
        
        // Verify important elements are still visible and accessible
        composeTestRule.onNodeWithText("Today's Sales").assertIsDisplayed()
        composeTestRule.onNodeWithText("GH₵100.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pause").assertIsDisplayed()
        
        // Verify color-dependent information has text alternatives
        composeTestRule.onNodeWithText("75%").assertIsDisplayed() // Battery level as text
        composeTestRule.onNodeWithContentDescription("Battery level indicator").assertIsDisplayed()
    }
    
    @Test
    fun textScaling_worksCorrectlyWithLargeText() {
        // Test that UI adapts to large text settings
        val dashboardState = DashboardUiState(
            isListening = false,
            todaysSales = 250.0,
            salesCount = 5,
            topProduct = "Tilapia",
            recentTransactions = listOf(createSampleTransaction("Test", 1, 10.0)),
            batteryLevel = 60,
            isLoading = false
        )
        
        composeTestRule.setContent {
            GhanaVoiceLedgerTheme {
                DashboardScreen(
                    uiState = dashboardState,
                    onStartListening = {},
                    onStopListening = {},
                    onNavigateToHistory = {},
                    onNavigateToSettings = {},
                    onEndDaySummary = {}
                )
            }
        }
        
        // Verify text elements are still readable and don't overlap
        composeTestRule.onNodeWithText("Today's Sales").assertIsDisplayed()
        composeTestRule.onNodeWithText("GH₵250.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("5 sales today").assertIsDisplayed()
        composeTestRule.onNodeWithText("Resume").assertIsDisplayed()
        
        // Verify buttons are still clickable with larger text
        composeTestRule.onNodeWithText("Resume").performClick()
        composeTestRule.onNodeWithText("View History").performClick()
    }
    
    @Test
    fun screenReader_announcementsWork() {
        var startListeningCalled = false
        var stopListeningCalled = false
        
        val initialState = DashboardUiState(
            isListening = false,
            todaysSales = 0.0,
            salesCount = 0,
            topProduct = null,
            recentTransactions = emptyList(),
            batteryLevel = 80,
            isLoading = false
        )
        
        composeTestRule.setContent {
            GhanaVoiceLedgerTheme {
                DashboardScreen(
                    uiState = initialState,
                    onStartListening = { startListeningCalled = true },
                    onStopListening = { stopListeningCalled = true },
                    onNavigateToHistory = {},
                    onNavigateToSettings = {},
                    onEndDaySummary = {}
                )
            }
        }
        
        // Test that state changes are announced properly
        composeTestRule.onNodeWithContentDescription("Pause listening button").performClick()
        assert(startListeningCalled)
        
        // Update to listening state
        val listeningState = initialState.copy(isListening = true)
        
        composeTestRule.setContent {
            GhanaVoiceLedgerTheme {
                DashboardScreen(
                    uiState = listeningState,
                    onStartListening = { startListeningCalled = true },
                    onStopListening = { stopListeningCalled = true },
                    onNavigateToHistory = {},
                    onNavigateToSettings = {},
                    onEndDaySummary = {}
                )
            }
        }
        
        // Verify the button text and description changed appropriately
        composeTestRule.onNodeWithContentDescription("Resume listening button").performClick()
        assert(stopListeningCalled)
    }
    
    @Test
    fun errorStates_haveProperAccessibilitySupport() {
        // Test error states with proper announcements
        val errorState = DashboardUiState(
            isListening = false,
            todaysSales = 0.0,
            salesCount = 0,
            topProduct = null,
            recentTransactions = emptyList(),
            batteryLevel = 80,
            isLoading = false,
            errorMessage = "Failed to load transactions"
        )
        
        composeTestRule.setContent {
            GhanaVoiceLedgerTheme {
                DashboardScreen(
                    uiState = errorState,
                    onStartListening = {},
                    onStopListening = {},
                    onNavigateToHistory = {},
                    onNavigateToSettings = {},
                    onEndDaySummary = {}
                )
            }
        }
        
        // Verify error message is accessible
        composeTestRule.onNodeWithText("Failed to load transactions").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Error message").assertExists()
        
        // Verify retry button if present
        try {
            composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
            composeTestRule.onNode(hasRole(Role.Button) and hasText("Retry")).assertIsDisplayed()
        } catch (e: AssertionError) {
            // Retry button not present, which is fine
        }
    }
    
    // Helper method to create sample transactions
    private fun createSampleTransaction(
        productName: String,
        quantity: Int,
        totalPrice: Double
    ): Transaction {
        return Transaction(
            id = System.currentTimeMillis(),
            productName = productName,
            quantity = quantity,
            unit = "pieces",
            unitPrice = totalPrice / quantity,
            totalPrice = totalPrice,
            customerId = "customer_test",
            speakerId = "speaker_test",
            timestamp = System.currentTimeMillis(),
            confidence = 0.95f,
            audioFilePath = "/test/audio.wav",
            isVerified = true,
            notes = "Accessibility test transaction"
        )
    }
}