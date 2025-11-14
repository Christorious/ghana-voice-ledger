package com.voiceledger.ghana.domain.model

import com.voiceledger.ghana.data.local.entity.Transaction
import com.voiceledger.ghana.data.repository.TransactionRepositoryImpl
import com.voiceledger.ghana.security.SecurityManager
import com.voiceledger.ghana.util.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.util.*

/**
 * Unit tests for TransactionAnalytics computation
 * Tests the analytics flow and calculations
 */
class TransactionAnalyticsTest {
    
    @Test
    fun `analytics computation with empty list should return zeros`() {
        // Given
        val transactions = emptyList<Transaction>()
        
        // When
        val analytics = computeAnalytics(transactions)
        
        // Then
        assertEquals(0.0, analytics.totalSales, 0.01)
        assertEquals(0, analytics.transactionCount)
        assertNull(analytics.topProduct)
        assertNull(analytics.peakHour)
        assertEquals(0, analytics.uniqueCustomers)
        assertEquals(0.0, analytics.averageTransactionValue, 0.01)
    }
    
    @Test
    fun `analytics computation with single transaction`() {
        // Given
        val transaction = createSampleTransaction(product = "Tilapia", amount = 25.0)
        val transactions = listOf(transaction)
        
        // When
        val analytics = computeAnalytics(transactions)
        
        // Then
        assertEquals(25.0, analytics.totalSales, 0.01)
        assertEquals(1, analytics.transactionCount)
        assertEquals("Tilapia", analytics.topProduct)
        assertEquals(1, analytics.uniqueCustomers)
        assertEquals(25.0, analytics.averageTransactionValue, 0.01)
    }
    
    @Test
    fun `analytics computation should calculate top product correctly`() {
        // Given
        val transactions = listOf(
            createSampleTransaction(product = "Tilapia", amount = 15.0),
            createSampleTransaction(product = "Tilapia", amount = 15.0),
            createSampleTransaction(product = "Mackerel", amount = 12.0)
        )
        
        // When
        val analytics = computeAnalytics(transactions)
        
        // Then
        assertEquals("Tilapia", analytics.topProduct)
        assertEquals(42.0, analytics.totalSales, 0.01)
        assertEquals(3, analytics.transactionCount)
    }
    
    @Test
    fun `analytics computation should calculate average correctly`() {
        // Given
        val transactions = listOf(
            createSampleTransaction(amount = 10.0),
            createSampleTransaction(amount = 20.0),
            createSampleTransaction(amount = 30.0)
        )
        
        // When
        val analytics = computeAnalytics(transactions)
        
        // Then
        assertEquals(60.0, analytics.totalSales, 0.01)
        assertEquals(3, analytics.transactionCount)
        assertEquals(20.0, analytics.averageTransactionValue, 0.01)
    }
    
    @Test
    fun `analytics computation should count unique customers`() {
        // Given
        val transactions = listOf(
            createSampleTransaction(customerId = "customer1"),
            createSampleTransaction(customerId = "customer1"),
            createSampleTransaction(customerId = "customer2"),
            createSampleTransaction(customerId = null),
            createSampleTransaction(customerId = "customer3")
        )
        
        // When
        val analytics = computeAnalytics(transactions)
        
        // Then
        assertEquals(3, analytics.uniqueCustomers) // Only counts non-null customer IDs
    }
    
    @Test
    fun `analytics computation should format peak hour correctly`() {
        // Given
        val calendar = Calendar.getInstance()
        val baseTime = calendar.timeInMillis
        
        // Create transactions at hour 10
        calendar.set(Calendar.HOUR_OF_DAY, 10)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val time10 = calendar.timeInMillis
        
        val transactions = listOf(
            createSampleTransaction(timestamp = time10),
            createSampleTransaction(timestamp = time10 + 60000)
        )
        
        // When
        val analytics = computeAnalytics(transactions)
        
        // Then
        assertNotNull(analytics.peakHour)
        assertTrue(analytics.peakHour!!.contains("10:"))
    }
    
    @Test
    fun `analytics model should have all required fields`() {
        // Given
        val analytics = TransactionAnalytics(
            totalSales = 100.0,
            transactionCount = 5,
            topProduct = "Tilapia",
            peakHour = "10:00-11:00",
            uniqueCustomers = 3,
            averageTransactionValue = 20.0
        )
        
        // Then
        assertEquals(100.0, analytics.totalSales, 0.01)
        assertEquals(5, analytics.transactionCount)
        assertEquals("Tilapia", analytics.topProduct)
        assertEquals("10:00-11:00", analytics.peakHour)
        assertEquals(3, analytics.uniqueCustomers)
        assertEquals(20.0, analytics.averageTransactionValue, 0.01)
    }
    
    // Helper functions
    
    private fun computeAnalytics(transactions: List<Transaction>): TransactionAnalytics {
        if (transactions.isEmpty()) {
            return TransactionAnalytics(
                totalSales = 0.0,
                transactionCount = 0,
                topProduct = null,
                peakHour = null,
                uniqueCustomers = 0,
                averageTransactionValue = 0.0
            )
        }
        
        val totalSales = transactions.sumOf { it.amount }
        val transactionCount = transactions.size
        
        val topProduct = transactions
            .groupingBy { it.product }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
        
        val peakHour = transactions
            .groupingBy { extractHourOfDay(it.timestamp) }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            ?.let { hour -> formatHourRange(hour) }
        
        val uniqueCustomers = transactions
            .mapNotNull { it.customerId }
            .toSet()
            .size
        
        val averageTransactionValue = totalSales / transactionCount
        
        return TransactionAnalytics(
            totalSales = totalSales,
            transactionCount = transactionCount,
            topProduct = topProduct,
            peakHour = peakHour,
            uniqueCustomers = uniqueCustomers,
            averageTransactionValue = averageTransactionValue
        )
    }
    
    private fun extractHourOfDay(timestamp: Long): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return calendar.get(Calendar.HOUR_OF_DAY)
    }
    
    private fun formatHourRange(hour: Int): String {
        val nextHour = (hour + 1) % 24
        return String.format(Locale.getDefault(), "%02d:00-%02d:00", hour, nextHour)
    }
    
    private fun createSampleTransaction(
        product: String = "Tilapia",
        amount: Double = 25.50,
        customerId: String? = "customer123",
        timestamp: Long = System.currentTimeMillis()
    ): Transaction {
        return Transaction(
            id = UUID.randomUUID().toString(),
            timestamp = timestamp,
            date = DateUtils.formatDate(timestamp),
            amount = amount,
            product = product,
            quantity = 3,
            unit = "pieces",
            customerId = customerId,
            confidence = 0.95f,
            transcriptSnippet = "Test transaction for $product",
            sellerConfidence = 0.95f,
            customerConfidence = 0.90f,
            needsReview = false,
            synced = false,
            originalPrice = null,
            finalPrice = amount,
            marketSession = null
        )
    }
}
