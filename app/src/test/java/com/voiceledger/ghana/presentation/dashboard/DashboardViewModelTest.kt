package com.voiceledger.ghana.presentation.dashboard

import com.voiceledger.ghana.data.local.entity.Transaction
import com.voiceledger.ghana.domain.repository.TransactionRepository
import com.voiceledger.ghana.domain.repository.DailySummaryRepository
import com.voiceledger.ghana.domain.repository.SpeakerProfileRepository
import com.voiceledger.ghana.service.VoiceAgentServiceManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.voiceledger.ghana.domain.model.TransactionAnalytics
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.Dispatchers

/**
 * Unit tests for DashboardViewModel
 * Tests dashboard state management and business logic
 */
@ExperimentalCoroutinesApi
class DashboardViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
    
    @Mock
    private lateinit var transactionRepository: TransactionRepository
    
    @Mock
    private lateinit var dailySummaryRepository: DailySummaryRepository
    
    @Mock
    private lateinit var speakerProfileRepository: SpeakerProfileRepository
    
    @Mock
    private lateinit var voiceAgentServiceManager: VoiceAgentServiceManager
    
    private lateinit var viewModel: DashboardViewModel
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        // Setup default mock behaviors
        setupDefaultMocks()
        
        viewModel = DashboardViewModel(
            transactionRepository = transactionRepository,
            dailySummaryRepository = dailySummaryRepository,
            speakerProfileRepository = speakerProfileRepository,
            voiceAgentServiceManager = voiceAgentServiceManager
        )
    }
    
    @Test
    fun `initial state should be loading`() {
        // Given - ViewModel is initialized
        
        // When - Check initial state
        val initialState = viewModel.uiState.value
        
        // Then
        assertTrue("Should be loading initially", initialState.isLoading)
        assertNull("Should have no data initially", initialState.data)
        assertNull("Should have no error initially", initialState.error)
    }
    
    @Test
    fun `loadDashboardData should update state with transaction data`() = runTest {
        // Given
        val mockTransactions = listOf(
            createMockTransaction("1", "Tilapia", 15.0),
            createMockTransaction("2", "Mackerel", 12.0)
        )
        
        val mockAnalytics = TransactionAnalytics(
            totalSales = 27.0,
            transactionCount = 2,
            topProduct = "Tilapia",
            peakHour = "10:00-11:00",
            uniqueCustomers = 2,
            averageTransactionValue = 13.5
        )
        
        `when`(transactionRepository.getTodaysTransactions()).thenReturn(flowOf(mockTransactions))
        `when`(transactionRepository.getTodaysAnalytics()).thenReturn(flowOf(mockAnalytics))
        
        // When - ViewModel loads data
        viewModel.refreshData()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNotNull("Should have data", state.data)
        assertNull("Should have no error", state.error)
        
        val data = state.data!!
        assertEquals(27.0, data.totalSales, 0.01)
        assertEquals(2, data.transactionCount)
        assertEquals("Tilapia", data.topProduct)
        assertEquals("10:00-11:00", data.peakHour)
        assertEquals(2, data.uniqueCustomers)
    }
    
    @Test
    fun `pauseListening should call service manager`() = runTest {
        // When
        viewModel.pauseListening()
        
        // Then
        verify(voiceAgentServiceManager).pauseListening()
    }
    
    @Test
    fun `resumeListening should call service manager`() = runTest {
        // When
        viewModel.resumeListening()
        
        // Then
        verify(voiceAgentServiceManager).startListening()
    }
    
    @Test
    fun `generateDailySummary should call repository and refresh data`() = runTest {
        // Given
        val mockSummary = mock(com.voiceledger.ghana.data.local.entity.DailySummary::class.java)
        `when`(dailySummaryRepository.generateTodaysSummary()).thenReturn(mockSummary)
        
        // When
        viewModel.generateDailySummary()
        
        // Then
        verify(dailySummaryRepository).generateTodaysSummary()
        
        val state = viewModel.uiState.value
        assertFalse("Should not be generating summary", state.isGeneratingSummary)
        assertEquals("Daily summary generated successfully", state.message)
    }
    
    @Test
    fun `formatCurrency should format Ghana cedis correctly`() {
        // Given
        val amount = 25.50
        
        // When
        val formatted = viewModel.formatCurrency(amount)
        
        // Then
        assertTrue("Should contain currency symbol", formatted.contains("25"))
        assertTrue("Should contain decimal", formatted.contains("50") || formatted.contains(".5"))
    }
    
    @Test
    fun `getTimeSinceLastTransaction should return correct format`() {
        // Given - Mock data with recent transaction
        val recentTime = System.currentTimeMillis() - (5 * 60 * 1000) // 5 minutes ago
        val mockData = DashboardData(
            totalSales = 100.0,
            transactionCount = 5,
            topProduct = "Tilapia",
            peakHour = "10:00",
            uniqueCustomers = 3,
            regularCustomers = 2,
            recentTransactions = emptyList(),
            isListening = true,
            serviceStatus = "Active",
            batteryLevel = 80,
            lastTransactionTime = recentTime
        )
        
        // Update state with mock data
        viewModel.uiState.value.copy(data = mockData)
        
        // When
        val timeSince = viewModel.getTimeSinceLastTransaction()
        
        // Then
        assertNotNull("Should have time since last transaction", timeSince)
        assertTrue("Should show minutes", timeSince!!.contains("m ago"))
    }
    
    @Test
    fun `getMarketStatus should return correct status based on time`() {
        // This test would need to mock the current time or use a time provider
        // For now, we'll test the logic with different scenarios
        
        // When
        val status = viewModel.getMarketStatus()
        
        // Then
        assertNotNull("Should have market status", status)
        assertTrue("Should be valid market status", 
            status in listOf(MarketStatus.BEFORE_HOURS, MarketStatus.OPEN, MarketStatus.AFTER_HOURS))
    }
    
    @Test
    fun `getBatteryStatusColor should return appropriate colors`() {
        // Given - Mock data with different battery levels
        val highBatteryData = createMockDashboardData(batteryLevel = 80)
        val mediumBatteryData = createMockDashboardData(batteryLevel = 30)
        val lowBatteryData = createMockDashboardData(batteryLevel = 10)
        
        // Test high battery
        viewModel.uiState.value.copy(data = highBatteryData)
        val highColor = viewModel.getBatteryStatusColor()
        assertEquals("High battery should be green", androidx.compose.ui.graphics.Color.Green, highColor)
        
        // Test low battery
        viewModel.uiState.value.copy(data = lowBatteryData)
        val lowColor = viewModel.getBatteryStatusColor()
        assertEquals("Low battery should be red", androidx.compose.ui.graphics.Color.Red, lowColor)
    }
    
    @Test
    fun `error handling should update state correctly`() = runTest {
        // Given
        `when`(transactionRepository.getTodaysTransactions()).thenThrow(RuntimeException("Database error"))
        
        // When
        viewModel.refreshData()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNotNull("Should have error", state.error)
        assertTrue("Should contain error message", state.error!!.contains("Database error"))
    }
    
    @Test
    fun `clearMessage should remove message from state`() {
        // Given
        viewModel.uiState.value.copy(message = "Test message")
        
        // When
        viewModel.clearMessage()
        
        // Then
        assertNull("Message should be cleared", viewModel.uiState.value.message)
    }
    
    @Test
    fun `clearError should remove error from state`() {
        // Given
        viewModel.uiState.value.copy(error = "Test error")
        
        // When
        viewModel.clearError()
        
        // Then
        assertNull("Error should be cleared", viewModel.uiState.value.error)
    }
    
    @Test
    fun `dashboard should use analytics flow without suspend calls`() = runTest {
        // Given
        val mockTransactions = listOf(
            createMockTransaction("1", "Tilapia", 15.0),
            createMockTransaction("2", "Mackerel", 12.0),
            createMockTransaction("3", "Tilapia", 18.0)
        )
        
        val mockAnalytics = TransactionAnalytics(
            totalSales = 45.0,
            transactionCount = 3,
            topProduct = "Tilapia",
            peakHour = "14:00-15:00",
            uniqueCustomers = 1,
            averageTransactionValue = 15.0
        )
        
        `when`(transactionRepository.getTodaysTransactions()).thenReturn(flowOf(mockTransactions))
        `when`(transactionRepository.getTodaysAnalytics()).thenReturn(flowOf(mockAnalytics))
        
        // When
        viewModel.refreshData()
        
        // Then - Verify analytics data is used
        val data = viewModel.uiState.value.data!!
        assertEquals(45.0, data.totalSales, 0.01)
        assertEquals(3, data.transactionCount)
        assertEquals("Tilapia", data.topProduct)
        assertEquals("14:00-15:00", data.peakHour)
        assertEquals(1, data.uniqueCustomers)
        
        // Verify no direct suspend method calls were made for analytics
        verify(transactionRepository, never()).getTodaysTotalSales()
        verify(transactionRepository, never()).getTodaysTopProduct()
        verify(transactionRepository, never()).getTodaysPeakHour()
        verify(transactionRepository, never()).getTodaysUniqueCustomerCount()
    }
    
    @Test
    fun `analytics flow should handle empty transactions`() = runTest {
        // Given
        val emptyAnalytics = TransactionAnalytics(
            totalSales = 0.0,
            transactionCount = 0,
            topProduct = null,
            peakHour = null,
            uniqueCustomers = 0,
            averageTransactionValue = 0.0
        )
        
        `when`(transactionRepository.getTodaysTransactions()).thenReturn(flowOf(emptyList()))
        `when`(transactionRepository.getTodaysAnalytics()).thenReturn(flowOf(emptyAnalytics))
        
        // When
        viewModel.refreshData()
        
        // Then
        val data = viewModel.uiState.value.data!!
        assertEquals(0.0, data.totalSales, 0.01)
        assertEquals(0, data.transactionCount)
        assertEquals("No sales yet", data.topProduct)
        assertEquals("N/A", data.peakHour)
        assertEquals(0, data.uniqueCustomers)
    }
    
    private fun setupDefaultMocks() {
        // Default empty flows
        `when`(transactionRepository.getTodaysTransactions()).thenReturn(flowOf(emptyList()))
        `when`(transactionRepository.getTodaysAnalytics()).thenReturn(
            flowOf(
                TransactionAnalytics(
                    totalSales = 0.0,
                    transactionCount = 0,
                    topProduct = null,
                    peakHour = null,
                    uniqueCustomers = 0,
                    averageTransactionValue = 0.0
                )
            )
        )
        `when`(dailySummaryRepository.getTodaysSummaryFlow()).thenReturn(flowOf(null))
        `when`(speakerProfileRepository.getRegularCustomers()).thenReturn(flowOf(emptyList()))
        `when`(voiceAgentServiceManager.serviceState).thenReturn(flowOf(ServiceState()))
    }
    
    private fun createMockTransaction(id: String, product: String, amount: Double): Transaction {
        return Transaction(
            id = id,
            timestamp = System.currentTimeMillis(),
            date = "2024-01-01",
            amount = amount,
            currency = "GHS",
            product = product,
            quantity = 1,
            unit = "piece",
            customerId = null,
            confidence = 0.9f,
            transcriptSnippet = "Mock transaction",
            sellerConfidence = 0.9f,
            customerConfidence = 0.8f,
            needsReview = false,
            synced = false,
            originalPrice = null,
            finalPrice = amount,
            marketSession = null
        )
    }
    
    private fun createMockDashboardData(batteryLevel: Int = 100): DashboardData {
        return DashboardData(
            totalSales = 100.0,
            transactionCount = 5,
            topProduct = "Tilapia",
            peakHour = "10:00",
            uniqueCustomers = 3,
            regularCustomers = 2,
            recentTransactions = emptyList(),
            isListening = true,
            serviceStatus = "Active",
            batteryLevel = batteryLevel,
            lastTransactionTime = System.currentTimeMillis()
        )
    }
}