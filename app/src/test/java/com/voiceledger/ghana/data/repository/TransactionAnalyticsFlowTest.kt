package com.voiceledger.ghana.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.voiceledger.ghana.data.local.database.VoiceLedgerDatabase
import com.voiceledger.ghana.data.local.entity.Transaction
import com.voiceledger.ghana.domain.model.TransactionAnalytics
import com.voiceledger.ghana.security.SecurityManager
import com.voiceledger.ghana.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.util.*

/**
 * Unit tests for TransactionAnalytics reactive flow
 * Tests that analytics are computed correctly from transactions flow
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class TransactionAnalyticsFlowTest {
    
    private lateinit var database: VoiceLedgerDatabase
    private lateinit var repository: TransactionRepositoryImpl
    
    @Mock
    private lateinit var securityManager: SecurityManager
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            VoiceLedgerDatabase::class.java
        ).allowMainThreadQueries().build()
        
        repository = TransactionRepositoryImpl(database.transactionDao(), securityManager)
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun `getTodaysAnalytics should emit empty analytics when no transactions`() = runTest {
        // When
        val analytics = repository.getTodaysAnalytics().first()
        
        // Then
        assertEquals(0.0, analytics.totalSales, 0.01)
        assertEquals(0, analytics.transactionCount)
        assertNull(analytics.topProduct)
        assertNull(analytics.peakHour)
        assertEquals(0, analytics.uniqueCustomers)
        assertEquals(0.0, analytics.averageTransactionValue, 0.01)
    }
    
    @Test
    fun `getTodaysAnalytics should compute correct total sales`() = runTest {
        // Given
        val today = DateUtils.getTodayDateString()
        val transactions = listOf(
            createSampleTransaction(date = today, amount = 10.0),
            createSampleTransaction(date = today, amount = 15.0),
            createSampleTransaction(date = today, amount = 20.0)
        )
        
        transactions.forEach { repository.insertTransaction(it) }
        
        // When
        val analytics = repository.getTodaysAnalytics().first()
        
        // Then
        assertEquals(45.0, analytics.totalSales, 0.01)
        assertEquals(3, analytics.transactionCount)
        assertEquals(15.0, analytics.averageTransactionValue, 0.01)
    }
    
    @Test
    fun `getTodaysAnalytics should identify top product`() = runTest {
        // Given
        val today = DateUtils.getTodayDateString()
        val transactions = listOf(
            createSampleTransaction(date = today, product = "Tilapia", amount = 15.0),
            createSampleTransaction(date = today, product = "Tilapia", amount = 15.0),
            createSampleTransaction(date = today, product = "Mackerel", amount = 12.0)
        )
        
        transactions.forEach { repository.insertTransaction(it) }
        
        // When
        val analytics = repository.getTodaysAnalytics().first()
        
        // Then
        assertEquals("Tilapia", analytics.topProduct)
    }
    
    @Test
    fun `getTodaysAnalytics should count unique customers`() = runTest {
        // Given
        val today = DateUtils.getTodayDateString()
        val transactions = listOf(
            createSampleTransaction(date = today, customerId = "customer1"),
            createSampleTransaction(date = today, customerId = "customer1"),
            createSampleTransaction(date = today, customerId = "customer2"),
            createSampleTransaction(date = today, customerId = "customer3")
        )
        
        transactions.forEach { repository.insertTransaction(it) }
        
        // When
        val analytics = repository.getTodaysAnalytics().first()
        
        // Then
        assertEquals(3, analytics.uniqueCustomers)
    }
    
    @Test
    fun `getTodaysAnalytics should identify peak hour`() = runTest {
        // Given
        val today = DateUtils.getTodayDateString()
        val calendar = Calendar.getInstance()
        
        // Create transactions at hour 14
        calendar.set(Calendar.HOUR_OF_DAY, 14)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val time14 = calendar.timeInMillis
        
        val transactions = listOf(
            createSampleTransaction(date = today, timestamp = time14),
            createSampleTransaction(date = today, timestamp = time14 + 60000),
            createSampleTransaction(date = today, timestamp = time14 + 120000)
        )
        
        transactions.forEach { repository.insertTransaction(it) }
        
        // When
        val analytics = repository.getTodaysAnalytics().first()
        
        // Then
        assertNotNull(analytics.peakHour)
        assertTrue(analytics.peakHour!!.contains("14:"))
    }
    
    @Test
    fun `getTodaysAnalytics should update when new transactions added`() = runTest {
        // Given - insert first transaction
        val today = DateUtils.getTodayDateString()
        val transaction1 = createSampleTransaction(date = today, product = "Tilapia", amount = 10.0)
        repository.insertTransaction(transaction1)
        
        // When - get analytics after first transaction
        val analytics1 = repository.getTodaysAnalytics().first()
        
        // Then - verify first transaction counted
        assertEquals(10.0, analytics1.totalSales, 0.01)
        assertEquals(1, analytics1.transactionCount)
        
        // Given - add another transaction
        val transaction2 = createSampleTransaction(date = today, product = "Mackerel", amount = 15.0)
        repository.insertTransaction(transaction2)
        
        // When - get updated analytics
        val analytics2 = repository.getTodaysAnalytics().first()
        
        // Then - verify both transactions counted
        assertEquals(25.0, analytics2.totalSales, 0.01)
        assertEquals(2, analytics2.transactionCount)
    }
    
    @Test
    fun `getTodaysAnalytics should not include previous day transactions`() = runTest {
        // Given - insert transaction from yesterday
        val yesterday = DateUtils.getDateDaysAgo(1)
        val yesterdayTransaction = createSampleTransaction(date = yesterday, amount = 100.0)
        repository.insertTransaction(yesterdayTransaction)
        
        // Given - insert transaction for today
        val today = DateUtils.getTodayDateString()
        val todayTransaction = createSampleTransaction(date = today, amount = 25.0)
        repository.insertTransaction(todayTransaction)
        
        // When
        val analytics = repository.getTodaysAnalytics().first()
        
        // Then
        assertEquals(25.0, analytics.totalSales, 0.01)
        assertEquals(1, analytics.transactionCount)
    }
    
    @Test
    fun `analytics flow emits reactively without suspend calls`() = runTest {
        // Given
        val today = DateUtils.getTodayDateString()
        val expectedTransactions = listOf(
            createSampleTransaction(date = today, product = "Tilapia", amount = 20.0),
            createSampleTransaction(date = today, product = "Mackerel", amount = 15.0)
        )
        
        // When - insert transactions (flow should react)
        expectedTransactions.forEach { repository.insertTransaction(it) }
        
        // Then - get analytics without calling suspend methods
        val analytics = repository.getTodaysAnalytics().first()
        
        // Verify analytics are correct
        assertEquals(35.0, analytics.totalSales, 0.01)
        assertEquals(2, analytics.transactionCount)
        assertEquals(17.5, analytics.averageTransactionValue, 0.01)
        assertTrue(listOf("Tilapia", "Mackerel").contains(analytics.topProduct))
    }
    
    // Helper functions
    
    private fun createSampleTransaction(
        product: String = "Tilapia",
        amount: Double = 25.50,
        customerId: String? = "customer123",
        timestamp: Long = System.currentTimeMillis(),
        date: String = DateUtils.formatDate(timestamp)
    ): Transaction {
        return Transaction(
            id = UUID.randomUUID().toString(),
            timestamp = timestamp,
            date = date,
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
