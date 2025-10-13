package com.voiceledger.ghana.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.voiceledger.ghana.data.local.entity.Transaction
import com.voiceledger.ghana.presentation.dashboard.DashboardScreen
import com.voiceledger.ghana.presentation.dashboard.DashboardUiState
import com.voiceledger.ghana.presentation.theme.GhanaVoiceLedgerTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for Dashboard screen with Compose testing
 * Tests dashboard updates with real-time transaction data
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DashboardScreenTest {
    
    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)
    
    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()
    
    @Before
    fun setUp() {
        hiltRule.inject()
    }
    
    @Test
    fun dashboardScreen_withNoTransactions_showsEmptyState() {
        // Given - empty dashboard state
        val emptyState = DashboardUiState(
            isListening = false,
            todaysSales = 0.0,
            salesCount = 0,
            topProduct = null,
            recentTransactions = emptyList(),
            batteryLevel = 85,
            isLoading = false
        )
        
        // When - display dashboard
        composeTestRule.setContent {
            GhanaVoiceLedgerTheme {
                DashboardScreen(
                    uiState = emptyState,
                    onStartListening = {},
                    onStopListening = {},
                    onNavigateToHistory = {},
                    onNavigateToSettings = {},
                    onEndDaySummary = {}
                )
            }
        }
        
        // Then - verify empty state elements
        composeTestRule.onNodeWithText("No transactions yet today").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start selling and transactions will appear here automatically").assertIsDisplayed()
        composeTestRule.onNodeWithText("GH₵0.00").assertIsDisplayed() // Today's sales
        composeTestRule.onNodeWithText("0 sales today").assertIsDisplayed()
        composeTestRule.onNodeWithText("85%").assertIsDisplayed() // Battery level
    }
    
    @Test
    fun dashboardScreen_withTransactions_showsCorrectData() {
        // Given - dashboard with transactions
        val transactions = listOf(
            createSampleTransaction("Tilapia", 3, 45.0),
            createSampleTransaction("Mackerel", 2, 30.0),
            createSampleTransaction("Sardines", 5, 25.0)
        )
        
        val stateWithTransactions = DashboardUiState(
            isListening = true,
            todaysSales = 100.0,
            salesCount = 3,
            topProduct = "Tilapia",
            recentTransactions = transactions,
            batteryLevel = 75,
            isLoading = false
        )
        
        // When - display dashboard
        composeTestRule.setContent {
            GhanaVoiceLedgerTheme {
                DashboardScreen(
                    uiState = stateWithTransactions,
                    onStartListening = {},
                    onStopListening = {},
                    onNavigateToHistory = {},
                    onNavigateToSettings = {},
                    onEndDaySummary = {}
                )
            }
        }
        
        // Then - verify transaction data is displayed
        composeTestRule.onNodeWithText("GH₵100.00").assertIsDisplayed() // Today's sales
        composeTestRule.onNodeWithText("3 sales today").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tilapia").assertIsDisplayed() // Top product
        composeTestRule.onNodeWithText("75%").assertIsDisplayed() // Battery level
        
        // Verify recent transactions are shown
        composeTestRule.onNodeWithText("Tilapia").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mackerel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sardines").assertIsDisplayed()
        composeTestRule.onNodeWithText("GH₵45.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("GH₵30.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("GH₵25.00").assertIsDisplayed()
    }
    
    @Test
    fun dashboardScreen_listeningToggle_worksCorrectly() {
        var isListening = false
        var startListeningCalled = false
        var stopListeningCalled = false
        
        val initialState = DashboardUiState(
            isListening = isListening,
            todaysSales = 0.0,
            salesCount = 0,
            topProduct = null,
            recentTransactions = emptyList(),
            batteryLevel = 80,
            isLoading = false
        )
        
        // When - display dashboard
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
        
        // Then - verify initial state shows "Resume" button
        composeTestRule.onNodeWithText("Resume").assertIsDisplayed()
        
        // When - click resume button
        composeTestRule.onNodeWithText("Resume").performClick()
        
        // Then - verify start listening was called
        assert(startListeningCalled)
        
        // Update state to listening
        isListening = true
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
        
        // Verify "Pause" button is now shown
        composeTestRule.onNodeWithText("Pause").assertIsDisplayed()
        
        // When - click pause button
        composeTestRule.onNodeWithText("Pause").performClick()
        
        // Then - verify stop listening was called
        assert(stopListeningCalled)
    }
    
    @Test
    fun dashboardScreen_navigationButtons_workCorrectly() {
        var historyNavigationCalled = false
        var settingsNavigationCalled = false
        var endDaySummaryCalled = false
        
        val state = DashboardUiState(
            isListening = false,
            todaysSales = 50.0,
            salesCount = 2,
            topProduct = "Fish",
            recentTransactions = emptyList(),
            batteryLevel = 90,
            isLoading = false
        )
        
        // When - display dashboard
        composeTestRule.setContent {
            GhanaVoiceLedgerTheme {
                DashboardScreen(
                    uiState = state,
                    onStartListening = {},
                    onStopListening = {},
                    onNavigateToHistory = { historyNavigationCalled = true },
                    onNavigateToSettings = { settingsNavigationCalled = true },
                    onEndDaySummary = { endDaySummaryCalled = true }
                )
            }
        }
        
        // Then - test navigation buttons
        composeTestRule.onNodeWithText("View History").performClick()
        assert(historyNavigationCalled)
        
        composeTestRule.onNodeWithText("End Day Summary").performClick()
        assert(endDaySummaryCalled)
        
        // Test settings button (using content description)
        composeTestRule.onNodeWithContentDescription("Settings button").performClick()
        assert(settingsNavigationCalled)
    }
    
    @Test
    fun dashboardScreen_loadingState_showsProgressIndicator() {
        // Given - loading state
        val loadingState = DashboardUiState(
            isListening = false,
            todaysSales = 0.0,
            salesCount = 0,
            topProduct = null,
            recentTransactions = emptyList(),
            batteryLevel = 80,
            isLoading = true
        )
        
        // When - display dashboard
        composeTestRule.setContent {
            GhanaVoiceLedgerTheme {
                DashboardScreen(
                    uiState = loadingState,
                    onStartListening = {},
                    onStopListening = {},
                    onNavigateToHistory = {},
                    onNavigateToSettings = {},
                    onEndDaySummary = {}
                )
            }
        }
        
        // Then - verify loading indicator is shown
        composeTestRule.onNode(hasTestTag("loading_indicator")).assertIsDisplayed()
    }
    
    @Test
    fun dashboardScreen_batteryLevelIndicator_showsCorrectColor() {
        // Test different battery levels and their corresponding colors
        val batteryLevels = listOf(95, 50, 20, 10) // High, Medium, Low, Critical
        
        batteryLevels.forEach { batteryLevel ->
            val state = DashboardUiState(
                isListening = false,
                todaysSales = 0.0,
                salesCount = 0,
                topProduct = null,
                recentTransactions = emptyList(),
                batteryLevel = batteryLevel,
                isLoading = false
            )
            
            composeTestRule.setContent {
                GhanaVoiceLedgerTheme {
                    DashboardScreen(
                        uiState = state,
                        onStartListening = {},
                        onStopListening = {},
                        onNavigateToHistory = {},
                        onNavigateToSettings = {},
                        onEndDaySummary = {}
                    )
                }
            }
            
            // Verify battery percentage is displayed
            composeTestRule.onNodeWithText("$batteryLevel%").assertIsDisplayed()
            
            // Verify battery indicator has appropriate test tag based on level
            val expectedTag = when (batteryLevel) {
                in 80..100 -> "battery_high"
                in 50..79 -> "battery_medium"
                in 20..49 -> "battery_low"
                else -> "battery_critical"
            }
            composeTestRule.onNodeWithTag(expectedTag).assertIsDisplayed()
        }
    }
    
    @Test
    fun dashboardScreen_recentTransactionsList_scrollsCorrectly() {
        // Given - many transactions to test scrolling
        val manyTransactions = (1..10).map { index ->
            createSampleTransaction("Product $index", index, index * 10.0)
        }
        
        val state = DashboardUiState(
            isListening = false,
            todaysSales = 550.0,
            salesCount = 10,
            topProduct = "Product 1",
            recentTransactions = manyTransactions,
            batteryLevel = 85,
            isLoading = false
        )
        
        // When - display dashboard
        composeTestRule.setContent {
            GhanaVoiceLedgerTheme {
                DashboardScreen(
                    uiState = state,
                    onStartListening = {},
                    onStopListening = {},
                    onNavigateToHistory = {},
                    onNavigateToSettings = {},
                    onEndDaySummary = {}
                )
            }
        }
        
        // Then - verify first few transactions are visible
        composeTestRule.onNodeWithText("Product 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Product 2").assertIsDisplayed()
        
        // Scroll down to see more transactions
        composeTestRule.onNodeWithTag("recent_transactions_list").performScrollToIndex(8)
        
        // Verify later transactions become visible
        composeTestRule.onNodeWithText("Product 9").assertIsDisplayed()
        composeTestRule.onNodeWithText("Product 10").assertIsDisplayed()
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
            customerId = "customer_${productName.lowercase()}",
            speakerId = "speaker_test",
            timestamp = System.currentTimeMillis(),
            confidence = 0.95f,
            audioFilePath = "/test/audio.wav",
            isVerified = true,
            notes = "UI test transaction"
        )
    }
}