package com.voiceledger.ghana.domain.service

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.voiceledger.ghana.data.local.entity.DailySummary
import com.voiceledger.ghana.data.local.entity.SpeakerProfile
import com.voiceledger.ghana.data.local.entity.Transaction
import com.voiceledger.ghana.domain.repository.DailySummaryRepository
import com.voiceledger.ghana.domain.repository.SpeakerProfileRepository
import com.voiceledger.ghana.domain.repository.TransactionRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DailySummaryGeneratorTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var speakerProfileRepository: SpeakerProfileRepository
    private lateinit var dailySummaryRepository: DailySummaryRepository
    private lateinit var dailySummaryGenerator: DailySummaryGenerator

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val today = dateFormat.format(Date())

    private val sampleTransactions = listOf(
        Transaction(
            id = "1",
            product = "Tilapia",
            amount = 50.0,
            timestamp = System.currentTimeMillis(),
            transcriptSnippet = "Two tilapia please",
            confidence = 0.9f,
            needsReview = false,
            quantity = 2.0,
            unit = "pieces",
            customerId = "customer1"
        ),
        Transaction(
            id = "2",
            product = "Mackerel",
            amount = 30.0,
            timestamp = System.currentTimeMillis() - 3600000, // 1 hour ago
            transcriptSnippet = "Give me mackerel",
            confidence = 0.85f,
            needsReview = true,
            quantity = 3.0,
            unit = "pieces",
            customerId = "customer2"
        ),
        Transaction(
            id = "3",
            product = "Tilapia",
            amount = 25.0,
            timestamp = System.currentTimeMillis() - 7200000, // 2 hours ago
            transcriptSnippet = "One tilapia",
            confidence = 0.95f,
            needsReview = false,
            quantity = 1.0,
            unit = "pieces",
            customerId = "customer1" // Repeat customer
        )
    )

    private val sampleCustomerProfiles = listOf(
        SpeakerProfile(
            id = "customer1",
            name = "Regular Customer 1",
            voiceEmbedding = floatArrayOf(),
            isActive = true,
            visitCount = 5,
            totalSpent = 100.0,
            averageSpending = 20.0,
            lastVisit = System.currentTimeMillis(),
            customerType = "regular"
        ),
        SpeakerProfile(
            id = "customer2",
            name = "New Customer",
            voiceEmbedding = floatArrayOf(),
            isActive = true,
            visitCount = 1,
            totalSpent = 30.0,
            averageSpending = 30.0,
            lastVisit = System.currentTimeMillis(),
            customerType = "new"
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        transactionRepository = mockk()
        speakerProfileRepository = mockk()
        dailySummaryRepository = mockk()

        // Mock repository calls
        every { transactionRepository.getTransactionsByDate(any()) } returns flowOf(sampleTransactions)
        every { transactionRepository.getTransactionsByCustomer(any()) } returns flowOf(sampleTransactions.take(1))
        coEvery { speakerProfileRepository.getNewCustomerCountSince(any()) } returns 1
        coEvery { dailySummaryRepository.insertSummary(any()) } just Runs
        every { dailySummaryRepository.getSummariesByDateRange(any(), any()) } returns flowOf(emptyList())

        dailySummaryGenerator = DailySummaryGenerator(
            transactionRepository,
            speakerProfileRepository,
            dailySummaryRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `generateTodaysSummary should create comprehensive summary`() = runTest {
        // When
        val summary = dailySummaryGenerator.generateTodaysSummary()

        // Then
        assertEquals(today, summary.date)
        assertEquals(105.0, summary.totalSales) // 50 + 30 + 25
        assertEquals(3, summary.transactionCount)
        assertEquals(2, summary.uniqueCustomers)
        assertEquals("Tilapia", summary.topProduct) // Appears twice
        assertEquals(75.0, summary.topProductSales) // 50 + 25
        assertEquals(35.0, summary.averageTransactionValue) // 105 / 3
        assertEquals(1, summary.repeatCustomers) // customer1 appears twice
        assertEquals(6.0, summary.totalQuantitySold) // 2 + 3 + 1
        assertTrue(summary.confidenceScore > 0.8f)
        assertEquals(2, summary.reviewedTransactions) // 2 transactions don't need review
        
        // Verify repository interactions
        verify { transactionRepository.getTransactionsByDate(today) }
        coVerify { dailySummaryRepository.insertSummary(any()) }
    }

    @Test
    fun `generateDailySummary should handle empty transactions`() = runTest {
        // Given
        every { transactionRepository.getTransactionsByDate(any()) } returns flowOf(emptyList())

        // When
        val summary = dailySummaryGenerator.generateDailySummary(today)

        // Then
        assertEquals(today, summary.date)
        assertEquals(0.0, summary.totalSales)
        assertEquals(0, summary.transactionCount)
        assertEquals(0, summary.uniqueCustomers)
        assertEquals(null, summary.topProduct)
        assertEquals(0.0, summary.averageTransactionValue)
        assertEquals(0, summary.repeatCustomers)
        assertEquals(0.0, summary.totalQuantitySold)
        assertEquals(0.0f, summary.confidenceScore)
        assertTrue(summary.recommendations.isEmpty())
    }

    @Test
    fun `generateDailySummary should calculate product breakdown correctly`() = runTest {
        // When
        val summary = dailySummaryGenerator.generateDailySummary(today)

        // Then
        val productBreakdown = summary.productBreakdown
        assertEquals(2, productBreakdown.size)
        
        val tilapiaBreakdown = productBreakdown["Tilapia"]
        assertNotNull(tilapiaBreakdown)
        assertEquals(75.0, tilapiaBreakdown.totalSales)
        assertEquals(2, tilapiaBreakdown.transactionCount)
        assertEquals(3.0, tilapiaBreakdown.totalQuantity)
        
        val mackerelBreakdown = productBreakdown["Mackerel"]
        assertNotNull(mackerelBreakdown)
        assertEquals(30.0, mackerelBreakdown.totalSales)
        assertEquals(1, mackerelBreakdown.transactionCount)
        assertEquals(3.0, mackerelBreakdown.totalQuantity)
    }

    @Test
    fun `generateDailySummary should calculate hourly breakdown correctly`() = runTest {
        // When
        val summary = dailySummaryGenerator.generateDailySummary(today)

        // Then
        val hourlyBreakdown = summary.hourlyBreakdown
        assertTrue(hourlyBreakdown.isNotEmpty())
        
        // Each hour should have correct aggregations
        hourlyBreakdown.values.forEach { hourlySummary ->
            assertTrue(hourlySummary.totalSales > 0.0)
            assertTrue(hourlySummary.transactionCount > 0)
            assertTrue(hourlySummary.uniqueCustomers > 0)
            assertNotNull(hourlySummary.topProduct)
        }
    }

    @Test
    fun `generateDailySummary should calculate customer insights correctly`() = runTest {
        // When
        val summary = dailySummaryGenerator.generateDailySummary(today)

        // Then
        val customerInsights = summary.customerInsights
        assertEquals(2, customerInsights.size)
        
        val customer1Insight = customerInsights["customer1"]
        assertNotNull(customer1Insight)
        assertEquals(75.0, customer1Insight.totalSpent) // 50 + 25
        assertEquals(2, customer1Insight.transactionCount)
        assertEquals("Tilapia", customer1Insight.favoriteProduct)
        assertEquals(37.5, customer1Insight.averageTransactionValue)
        
        val customer2Insight = customerInsights["customer2"]
        assertNotNull(customer2Insight)
        assertEquals(30.0, customer2Insight.totalSpent)
        assertEquals(1, customer2Insight.transactionCount)
        assertEquals("Mackerel", customer2Insight.favoriteProduct)
    }

    @Test
    fun `generateDailySummary should provide relevant recommendations`() = runTest {
        // When
        val summary = dailySummaryGenerator.generateDailySummary(today)

        // Then
        val recommendations = summary.recommendations
        assertTrue(recommendations.isNotEmpty())
        
        // Should include peak hour recommendation since we have transactions
        assertTrue(recommendations.any { it.contains("peak sales hour") })
    }

    @Test
    fun `generateDailySummary should handle low sales with appropriate recommendations`() = runTest {
        // Given - transactions with very low amounts
        val lowSalesTransactions = listOf(
            Transaction(
                id = "1",
                product = "Small Fish",
                amount = 5.0,
                timestamp = System.currentTimeMillis(),
                transcriptSnippet = "Small fish",
                confidence = 0.7f,
                needsReview = false,
                quantity = 1.0,
                unit = "pieces",
                customerId = "customer1"
            )
        )
        every { transactionRepository.getTransactionsByDate(any()) } returns flowOf(lowSalesTransactions)

        // When
        val summary = dailySummaryGenerator.generateDailySummary(today)

        // Then
        assertEquals(5.0, summary.totalSales)
        assertTrue(summary.recommendations.any { it.contains("lower than usual") })
        assertTrue(summary.recommendations.any { it.contains("voice profile") }) // Low confidence
    }

    @Test
    fun `generatePeriodSummary should aggregate multiple days correctly`() = runTest {
        // Given
        val startDate = "2024-01-01"
        val endDate = "2024-01-07"
        val mockSummaries = listOf(
            DailySummary(
                date = "2024-01-01",
                totalSales = 100.0,
                transactionCount = 5,
                uniqueCustomers = 3,
                topProduct = "Tilapia",
                topProductSales = 60.0,
                peakHour = "10",
                peakHourSales = 40.0,
                averageTransactionValue = 20.0,
                repeatCustomers = 1,
                newCustomers = 2,
                totalQuantitySold = 10.0,
                mostProfitableHour = "10",
                leastActiveHour = "15",
                confidenceScore = 0.9f,
                reviewedTransactions = 4,
                comparisonWithYesterday = null,
                comparisonWithLastWeek = null,
                productBreakdown = emptyMap(),
                hourlyBreakdown = emptyMap(),
                customerInsights = emptyMap(),
                recommendations = emptyList()
            )
        )
        
        every { dailySummaryRepository.getSummariesByDateRange(startDate, endDate) } returns flowOf(mockSummaries)

        // When
        val periodSummary = dailySummaryGenerator.generatePeriodSummary(startDate, endDate)

        // Then
        assertEquals(startDate, periodSummary.startDate)
        assertEquals(endDate, periodSummary.endDate)
        assertEquals(105.0, periodSummary.totalSales) // From sample transactions
        assertEquals(3, periodSummary.totalTransactions)
        assertTrue(periodSummary.averageDailySales > 0.0)
        assertTrue(periodSummary.topProducts.isNotEmpty())
    }

    @Test
    fun `generateDailySummary should calculate new customers correctly`() = runTest {
        // Given - mock that customer2 is new (first transaction today)
        every { transactionRepository.getTransactionsByCustomer("customer1") } returns flowOf(
            listOf(
                sampleTransactions[0].copy(timestamp = System.currentTimeMillis() - 86400000) // Yesterday
            )
        )
        every { transactionRepository.getTransactionsByCustomer("customer2") } returns flowOf(
            listOf(sampleTransactions[1]) // Today only
        )

        // When
        val summary = dailySummaryGenerator.generateDailySummary(today)

        // Then
        assertEquals(1, summary.newCustomers) // Only customer2 is new
    }

    @Test
    fun `generateDailySummary should handle comparison data when previous data exists`() = runTest {
        // Given - mock yesterday's transactions
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }.let { dateFormat.format(it.time) }
        
        val yesterdayTransactions = listOf(
            Transaction(
                id = "y1",
                product = "Fish",
                amount = 80.0,
                timestamp = System.currentTimeMillis() - 86400000,
                transcriptSnippet = "Fish",
                confidence = 0.8f,
                needsReview = false,
                quantity = 2.0,
                unit = "pieces",
                customerId = "customer1"
            )
        )
        
        every { transactionRepository.getTransactionsByDate(yesterday) } returns flowOf(yesterdayTransactions)

        // When
        val summary = dailySummaryGenerator.generateDailySummary(today)

        // Then
        assertNotNull(summary.comparisonWithYesterday)
        val comparison = summary.comparisonWithYesterday!!
        assertEquals(31.25, comparison.salesChange, 0.01) // (105-80)/80 * 100 = 31.25%
        assertEquals(200.0, comparison.transactionCountChange, 0.01) // (3-1)/1 * 100 = 200%
    }
}