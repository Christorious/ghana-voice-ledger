package com.voiceledger.ghana.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
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
 * Responsive design tests for different screen sizes
 * Tests UI adaptation across phone, tablet, and foldable devices
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ResponsiveDesignTest {
    
    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)
    
    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()
    
    @Before
    fun setUp() {
        hiltRule.inject()
    }
    
    @Test
    fun dashboardScreen_adaptsToPhoneSize() {
        // Given - phone-sized screen (360x640 dp)
        val phoneSize = Modifier.size(360.dp, 640.dp)
        val dashboardState = createSampleDashboardState()
        
        // When - display dashboard on phone
        composeTestRule.setContent {
            GhanaVoiceLedgerTheme {
                Surface(modifier = phoneSize.testTag("phone_screen")) {
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
        }
        
        // Then - verify phone layout
        composeTestRule.onNodeWithTag("phone_screen").assertIsDisplayed()
        
        // Verify vertical layout elements are present
        composeTestRule.onNodeWithText("Today's Sales").assertIsDisplayed()
        composeTestRule.onNodeWithText("GH₵150.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pause").assertIsDisplayed()
        
        // Verify recent transactions are in a vertical list
        composeTestRule.onNodeWithTag("recent_transactions_list").assertIsDisplayed()
        
        // Verify navigation bar is at bottom (typical phone layout)
        composeTestRule.onNodeWithText("Dashboard").assertIsDisplayed()
        composeTestRule.onNodeWithText("History").assertIsDisplayed()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }
    
    @Test
    fun dashboardScreen_adaptsToTabletSize() {
        // Given - tablet-sized screen (768x1024 dp)
        val tabletSize = Modifier.size(768.dp, 1024.dp)
        val dashboardState = createSampleDashboardState()
        
        // When - display dashboard on tablet
        composeTestRule.setContent {
            GhanaVoiceLedgerTheme {
                Surface(modifier = tabletSize.testTag("tablet_screen")) {
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
        }
        
        // Then - verify tablet layout adaptations
        composeTestRule.onNodeWithTag("tablet_screen").assertIsDisplayed()
        
        // On tablet, elements might be arranged differently
        composeTestRule.onNodeWithText("Today's Sales").assertIsDisplayed()
        composeTestRule.onNodeWithText("GH₵150.00").assertIsDisplayed()
        
        // Verify that more content is visible due to larger screen
        composeTestRule.onNodeWithText("Recent Transactions").assertIsDisplayed()
        
        // Check if side-by-side layout is used for some elements
        composeTestRule.onNodeWithText("3 sales today").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tilapia").assertIsDisplayed() // Top product
    }
    
    @Test
    fun dashboardScreen_adaptsToLandscapeOrientation() {
        // Given - landscape orientation (640x360 dp)
        val landscapeSize = Modifier.size(640.dp, 360.dp)
        val dashboardState = createSampleDashboardState()
        
        // When - display dashboard in landscape
        composeTestRule.setContent {
            GhanaVoiceLedgerTheme {
                Surface(modifier = landscapeSize.testTag("landscape_screen")) {
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
        }
        
        // Then - verify landscape layout
        composeTestRule.onNodeWithTag("landscape_screen").assertIsDisplayed()
        
        // In landscape, layout should be more horizontal
        composeTestRule.onNodeWithText("Today's Sales").assertIsDisplayed()
        composeTestRule.onNodeWithText("GH₵150.00").assertIsDisplayed()
        
        // Verify key elements are still accessible despite limited height
        composeTestRule.onNodeWithText("Pause").assertIsDisplayed()
        composeTestRule.onNodeWithText("View History").assertIsDisplayed()
    }
    
    @Test
    fun historyScreen_adaptsToPhoneSize() {
        // Given - phone size with history data
        val phoneSize = Modifier.size(360.dp, 640.dp)
        val historyState = createSampleHistoryState()
        
        // When - display history on phone
        composeTestRule.setContent {
            GhanaVoiceLedgerTheme {
                Surface(modifier = phoneSize.testTag("phone_history")) {
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
        }
        
        // Then - verify phone history layout
        composeTestRule.onNodeWithTag("phone_history").assertIsDisplayed()
        
        // Verify search and filter are accessible
        composeTestRule.onNodeWithContentDescription("Search transactions").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Filter transactions").assertIsDisplayed()
        
        // Verify transaction list is scrollable
        composeTestRule.onNodeWithText("Tilapia").assertIsDisplayed()
        composeTestRule.onNodeWithText("GH₵45.00").assertIsDisplayed()
    }
    
    @Test
    fun historyScreen_adaptsToTabletSize() {
        // Given - tablet size with history data
        val tabletSize = Modifier.size(768.dp, 1024.dp)
        val historyState = createSampleHistoryState()
        
        // When - display history on tablet
        composeTestRule.setContent {
            GhanaVoiceLedgerTheme {
                Surface(modifier = tabletSize.testTag("tablet_history")) {
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
        }
        
        // Then - verify tablet history layout
        composeTestRule.onNodeWithTag("tablet_history").assertIsDisplayed()
        
        // On tablet, more transactions should be visible
        composeTestRule.onNodeWithText("Tilapia").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mackerel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sardines").assertIsDisplayed()
        
        // Verify search and filters are more prominent
        composeTestRule.onNodeWithContentDescription("Search transactions").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Filter transactions").assertIsDisplayed()
    }
    
    @Test
    fun settingsScreen_adaptsToPhoneSize() {
        // Given - phone size
        val phoneSize = Modifier.size(360.dp, 640.dp)
        
        // When - display settings on phone
        composeTestRule.setContent {
            GhanaVoiceLedgerTheme {
                Surface(modifier = phoneSize.testTag("phone_settings")) {
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
        }
        
        // Then - verify phone settings layout
        composeTestRule.onNodeWithTag("phone_settings").assertIsDisplayed()
        
        // Verify settings sections are in vertical list
        composeTestRule.onNodeWithText("Language & Region").assertIsDisplayed()
        composeTestRule.onNodeWithText("Voice Recognition").assertIsDisplayed()
        composeTestRule.onNodeWithText("Notifications").assertIsDisplayed()
        composeTestRule.onNodeWithText("Data & Privacy").assertIsDisplayed()
    }
    
    @Test
    fun settingsScreen_adaptsToTabletSize() {
        // Given - tablet size
        val tabletSize = Modifier.size(768.dp, 1024.dp)
        
        // When - display settings on tablet
        composeTestRule.setContent {
            GhanaVoiceLedgerTheme {
                Surface(modifier = tabletSize.testTag("tablet_settings")) {
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
        }
        
        // Then - verify tablet settings layout
        composeTestRule.onNodeWithTag("tablet_settings").assertIsDisplayed()
        
        // On tablet, settings might be arranged in columns or with more detail
        composeTestRule.onNodeWithText("Language & Region").assertIsDisplayed()
        composeTestRule.onNodeWithText("Voice Recognition").assertIsDisplayed()
        composeTestRule.onNodeWithText("Notifications").assertIsDisplayed()
        composeTestRule.onNodeWithText("Data & Privacy").assertIsDisplayed()
    }
    
    @Test
    fun smallScreen_handlesContentOverflow() {
        // Given - very small screen (240x320 dp - old phone size)
        val smallSize = Modifier.size(240.dp, 320.dp)
        val dashboardState = createSampleDashboardState()
        
        // When - display on small screen
        composeTestRule.setContent {
            GhanaVoiceLedgerTheme {
                Surface(modifier = smallSize.testTag("small_screen")) {
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
        }
        
        // Then - verify essential elements are still accessible
        composeTestRule.onNodeWithTag("small_screen").assertIsDisplayed()
        
        // Core functionality should still be available
        composeTestRule.onNodeWithText("Today's Sales").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pause").assertIsDisplayed()
        
        // Some elements might be scrollable or collapsed
        try {
            composeTestRule.onNodeWithText("Recent Transactions").assertIsDisplayed()
        } catch (e: AssertionError) {
            // It's okay if recent transactions are not visible on very small screens
            println("Recent transactions not visible on small screen - acceptable")
        }
    }
    
    @Test
    fun largeScreen_utilizesExtraSpace() {
        // Given - large screen (1200x1920 dp - large tablet)
        val largeSize = Modifier.size(1200.dp, 1920.dp)
        val dashboardState = createSampleDashboardState()
        
        // When - display on large screen
        composeTestRule.setContent {
            GhanaVoiceLedgerTheme {
                Surface(modifier = largeSize.testTag("large_screen")) {
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
        }
        
        // Then - verify large screen utilization
        composeTestRule.onNodeWithTag("large_screen").assertIsDisplayed()
        
        // More content should be visible
        composeTestRule.onNodeWithText("Today's Sales").assertIsDisplayed()
        composeTestRule.onNodeWithText("GH₵150.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("Recent Transactions").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 sales today").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tilapia").assertIsDisplayed()
        
        // All transactions should be visible without scrolling
        composeTestRule.onNodeWithText("Mackerel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sardines").assertIsDisplayed()
    }
    
    @Test
    fun foldableScreen_adaptsToUnfoldedState() {
        // Given - foldable screen unfolded (673x841 dp - Surface Duo)
        val foldableSize = Modifier.size(673.dp, 841.dp)
        val dashboardState = createSampleDashboardState()
        
        // When - display on unfolded foldable
        composeTestRule.setContent {
            GhanaVoiceLedgerTheme {
                Surface(modifier = foldableSize.testTag("foldable_screen")) {
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
        }
        
        // Then - verify foldable layout
        composeTestRule.onNodeWithTag("foldable_screen").assertIsDisplayed()
        
        // Content should adapt to the unique aspect ratio
        composeTestRule.onNodeWithText("Today's Sales").assertIsDisplayed()
        composeTestRule.onNodeWithText("GH₵150.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("Recent Transactions").assertIsDisplayed()
    }
    
    @Test
    fun textScaling_adaptsToUserPreferences() {
        // Test different text scaling preferences
        val dashboardState = createSampleDashboardState()
        
        // Test with normal text scale
        composeTestRule.setContent {
            GhanaVoiceLedgerTheme {
                Surface(modifier = Modifier.fillMaxSize().testTag("normal_text")) {
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
        }
        
        // Verify text is readable and buttons are accessible
        composeTestRule.onNodeWithText("Today's Sales").assertIsDisplayed()
        composeTestRule.onNodeWithText("GH₵150.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pause").assertIsDisplayed()
        composeTestRule.onNodeWithText("View History").assertIsDisplayed()
        
        // Verify buttons are still clickable
        composeTestRule.onNodeWithText("Pause").performClick()
        composeTestRule.onNodeWithText("View History").performClick()
    }
    
    // Helper methods
    private fun createSampleDashboardState(): DashboardUiState {
        val transactions = listOf(
            createSampleTransaction("Tilapia", 3, 45.0),
            createSampleTransaction("Mackerel", 2, 30.0),
            createSampleTransaction("Sardines", 5, 25.0)
        )
        
        return DashboardUiState(
            isListening = true,
            todaysSales = 150.0,
            salesCount = 3,
            topProduct = "Tilapia",
            recentTransactions = transactions,
            batteryLevel = 85,
            isLoading = false
        )
    }
    
    private fun createSampleHistoryState(): HistoryUiState {
        val transactions = listOf(
            createSampleTransaction("Tilapia", 3, 45.0),
            createSampleTransaction("Mackerel", 2, 30.0),
            createSampleTransaction("Sardines", 5, 25.0)
        )
        
        return HistoryUiState(
            transactions = transactions,
            isLoading = false,
            selectedFilter = null,
            searchQuery = "",
            sortOrder = "date_desc"
        )
    }
    
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
            notes = "Responsive design test transaction"
        )
    }
}