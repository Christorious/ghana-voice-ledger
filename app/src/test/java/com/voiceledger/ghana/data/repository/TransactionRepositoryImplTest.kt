package com.voiceledger.ghana.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.voiceledger.ghana.data.local.database.VoiceLedgerDatabase
import com.voiceledger.ghana.data.local.entity.Transaction
import com.voiceledger.ghana.util.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

/**
 * Unit tests for TransactionRepositoryImpl using in-memory database
 */
@RunWith(AndroidJUnit4::class)
class TransactionRepositoryImplTest {
    
    private lateinit var database: VoiceLedgerDatabase
    private lateinit var repository: TransactionRepositoryImpl
    
    @Before
    fun setUp() {
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            VoiceLedgerDatabase::class.java
        ).allowMainThreadQueries().build()
        
        repository = TransactionRepositoryImpl(database.transactionDao())
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun testInsertTransaction_shouldSaveSuccessfully() = runTest {
        // Given
        val transaction = createSampleTransaction()
        
        // When
        repository.insertTransaction(transaction)
        
        // Then
        val retrieved = repository.getTransactionById(transaction.id)
        assertNotNull("Should retrieve inserted transaction", retrieved)
        assertEquals("Should have same product", transaction.product, retrieved?.product)
        assertEquals("Should have same quantity", transaction.quantity, retrieved?.quantity)
        assertEquals("Should have same amount", transaction.amount, retrieved?.amount, 0.01)
    }
    
    @Test
    fun testGetAllTransactions_shouldReturnAllTransactions() = runTest {
        // Given
        val transactions = listOf(
            createSampleTransaction(product = "Tilapia"),
            createSampleTransaction(product = "Mackerel"),
            createSampleTransaction(product = "Sardines")
        )
        
        transactions.forEach { repository.insertTransaction(it) }
        
        // When
        val allTransactions = repository.getAllTransactions().first()
        
        // Then
        assertEquals("Should return all transactions", 3, allTransactions.size)
        assertTrue("Should contain Tilapia", allTransactions.any { it.product == "Tilapia" })
        assertTrue("Should contain Mackerel", allTransactions.any { it.product == "Mackerel" })
        assertTrue("Should contain Sardines", allTransactions.any { it.product == "Sardines" })
    }
    
    @Test
    fun testGetTransactionsByDateRange_shouldReturnCorrectTransactions() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val oneDayAgo = now - (24 * 60 * 60 * 1000)
        val twoDaysAgo = now - (2 * 24 * 60 * 60 * 1000)
        val threeDaysAgo = now - (3 * 24 * 60 * 60 * 1000)
        
        val transactions = listOf(
            createSampleTransaction(timestamp = threeDaysAgo, product = "Old Transaction"),
            createSampleTransaction(timestamp = oneDayAgo, product = "Recent Transaction 1"),
            createSampleTransaction(timestamp = now, product = "Recent Transaction 2")
        )
        
        transactions.forEach { repository.insertTransaction(it) }
        
        // When
        val recentTransactions = repository.getTransactionsByDateRange(twoDaysAgo, now).first()
        
        // Then
        assertEquals("Should return 2 recent transactions", 2, recentTransactions.size)
        assertTrue("Should contain Recent Transaction 1", 
            recentTransactions.any { it.product == "Recent Transaction 1" })
        assertTrue("Should contain Recent Transaction 2", 
            recentTransactions.any { it.product == "Recent Transaction 2" })
        assertFalse("Should not contain old transaction", 
            recentTransactions.any { it.product == "Old Transaction" })
    }
    
    @Test
    fun testGetTransactionsByProduct_shouldReturnCorrectTransactions() = runTest {
        // Given
        val transactions = listOf(
            createSampleTransaction(product = "Tilapia", amount = 10.0),
            createSampleTransaction(product = "Tilapia", amount = 15.0),
            createSampleTransaction(product = "Mackerel", amount = 12.0)
        )
        
        transactions.forEach { repository.insertTransaction(it) }
        
        // When
        val tilapiaTransactions = repository.getTransactionsByProduct("Tilapia").first()
        
        // Then
        assertEquals("Should return 2 tilapia transactions", 2, tilapiaTransactions.size)
        assertTrue("All should be tilapia transactions", 
            tilapiaTransactions.all { it.product == "Tilapia" })
        assertEquals("Should have correct amounts", 
            setOf(10.0, 15.0), tilapiaTransactions.map { it.amount }.toSet())
    }
    
    @Test
    fun testGetTransactionsByCustomer_shouldReturnCorrectTransactions() = runTest {
        // Given
        val transactions = listOf(
            createSampleTransaction(customerId = "customer1", product = "Tilapia"),
            createSampleTransaction(customerId = "customer1", product = "Mackerel"),
            createSampleTransaction(customerId = "customer2", product = "Sardines")
        )
        
        transactions.forEach { repository.insertTransaction(it) }
        
        // When
        val customer1Transactions = repository.getTransactionsByCustomer("customer1").first()
        
        // Then
        assertEquals("Should return 2 transactions for customer1", 2, customer1Transactions.size)
        assertTrue("All should be for customer1", 
            customer1Transactions.all { it.customerId == "customer1" })
        assertEquals("Should have correct products", 
            setOf("Tilapia", "Mackerel"), customer1Transactions.map { it.product }.toSet())
    }
    
    @Test
    fun testUpdateTransaction_shouldUpdateSuccessfully() = runTest {
        // Given
        val transaction = createSampleTransaction()
        repository.insertTransaction(transaction)
        
        val updatedTransaction = transaction.copy(
            product = "Updated Product",
            amount = 99.99
        )
        
        // When
        repository.updateTransaction(updatedTransaction)
        
        // Then
        val retrieved = repository.getTransactionById(transaction.id)
        assertNotNull("Should retrieve updated transaction", retrieved)
        assertEquals("Should have updated product name", "Updated Product", retrieved?.product)
        assertEquals("Should have updated amount", 99.99, retrieved?.amount, 0.01)
    }
    
    @Test
    fun testDeleteTransaction_shouldRemoveSuccessfully() = runTest {
        // Given
        val transaction = createSampleTransaction()
        repository.insertTransaction(transaction)
        
        // Verify it exists
        assertNotNull("Transaction should exist before deletion", 
            repository.getTransactionById(transaction.id))
        
        // When
        repository.deleteTransaction(transaction)
        
        // Then
        assertNull("Transaction should not exist after deletion", 
            repository.getTransactionById(transaction.id))
    }
    
    @Test
    fun testGetTransactionStats_shouldCalculateCorrectly() = runTest {
        // Given
        val testDate = "2024-01-15"
        val transactions = listOf(
            createSampleTransaction(amount = 10.0),
            createSampleTransaction(amount = 15.0),
            createSampleTransaction(amount = 20.0)
        )
        val transactionsWithDate = transactions.map { it.copy(date = testDate) }
        
        transactionsWithDate.forEach { repository.insertTransaction(it) }
        
        // When
        val stats = repository.getTransactionStats(testDate, testDate)
        
        // Then
        assertEquals("Should have 3 transactions", 3, stats.totalTransactions)
        assertEquals("Should have correct total amount", 45.0, stats.totalAmount, 0.01)
        assertEquals("Should have correct average amount", 15.0, stats.averageAmount, 0.01)
        assertEquals("Should have correct highest amount", 20.0, stats.highestAmount, 0.01)
        assertEquals("Should have correct lowest amount", 10.0, stats.lowestAmount, 0.01)
        assertEquals("Should have 1 unique customer", 1, stats.uniqueCustomers)
        assertEquals("Should have 1 unique product", 1, stats.uniqueProducts)
        assertEquals("Should have 0 reviewed transactions", 0, stats.reviewedTransactions)
    }
    
    @Test
    fun testSearchTransactions_shouldReturnMatchingTransactions() = runTest {
        // Given
        val transactions = listOf(
            createSampleTransaction(product = "Fresh Tilapia", customerId = "john_doe"),
            createSampleTransaction(product = "Smoked Mackerel", customerId = "jane_smith"),
            createSampleTransaction(product = "Dried Sardines", customerId = "john_doe")
        )
        
        transactions.forEach { repository.insertTransaction(it) }
        
        // When
        val searchResults = repository.searchTransactions("john").first()
        
        // Then
        assertEquals("Should return 2 transactions for john", 2, searchResults.size)
        assertTrue("All should be for john_doe", 
            searchResults.all { it.customerId == "john_doe" })
    }
    
    @Test
    fun testDeleteOldTransactions_shouldDeleteOldTransactions() = runTest {
        // Given
        val oldDate = DateUtils.getDateDaysAgo(10)
        val recentDate = DateUtils.getDateDaysAgo(1)
        
        val oldTransaction = createSampleTransaction(date = oldDate, product = "Old Transaction")
        val recentTransaction = createSampleTransaction(date = recentDate, product = "Recent Transaction")
        
        repository.insertTransaction(oldTransaction)
        repository.insertTransaction(recentTransaction)
        
        // When
        repository.deleteOldTransactions(5) // Delete transactions older than 5 days
        
        // Then
        val allTransactions = repository.getAllTransactions().first()
        assertEquals("Should have 1 remaining transaction", 1, allTransactions.size)
        assertEquals("Remaining should be recent transaction", 
            "Recent Transaction", allTransactions[0].product)
    }
    
    @Test
    fun testConcurrentInserts_shouldHandleCorrectly() = runTest {
        // Given
        val transactions = (1..10).map { 
            createSampleTransaction(product = "Product $it") 
        }
        
        // When - simulate concurrent inserts
        transactions.forEach { repository.insertTransaction(it) }
        
        // Then
        val allTransactions = repository.getAllTransactions().first()
        assertEquals("Should insert all transactions", 10, allTransactions.size)
        assertTrue("All products should be unique", 
            allTransactions.map { it.product }.toSet().size == 10)
    }
    
    @Test
    fun testDateFormatting_consistency() = runTest {
        // Given
        val timestamp = System.currentTimeMillis()
        val transaction = createSampleTransaction(timestamp = timestamp)
        
        // When
        repository.insertTransaction(transaction)
        
        // Then
        val retrieved = repository.getTransactionById(transaction.id)
        assertNotNull("Should retrieve transaction", retrieved)
        assertEquals("Date should be correctly formatted", 
            DateUtils.formatDate(timestamp), retrieved?.date)
    }
    
    @Test
    fun testTodaysAnalytics_methods() = runTest {
        // Given
        val today = DateUtils.getTodayDateString()
        val transactions = listOf(
            createSampleTransaction(product = "Tilapia", amount = 30.0),
            createSampleTransaction(product = "Mackerel", amount = 20.0),
            createSampleTransaction(product = "Sardines", amount = 15.0)
        )
        
        // When
        transactions.forEach { repository.insertTransaction(it) }
        
        // Then
        val todayTransactions = repository.getTodaysTransactions().first()
        assertEquals("Should get today's transactions", 3, todayTransactions.size)
        
        val todayTotalSales = repository.getTodaysTotalSales()
        assertEquals("Should calculate today's total sales", 65.0, todayTotalSales, 0.01)
        
        val todayCount = repository.getTodaysTransactionCount()
        assertEquals("Should get today's transaction count", 3, todayCount)
        
        val topProduct = repository.getTodaysTopProduct()
        assertNotNull("Should get today's top product", topProduct)
        
        val uniqueCustomers = repository.getTodaysUniqueCustomerCount()
        assertEquals("Should count unique customers", 1, uniqueCustomers) // All use same customer by default
        
        val avgValue = repository.getTodaysAverageTransactionValue()
        assertEquals("Should calculate average transaction value", 65.0/3.0, avgValue, 0.01)
    }
    
    @Test
    fun testDateBasedAnalytics_methods() = runTest {
        // Given
        val testDate = "2024-01-15"
        val testTimestamp = DateUtils.getStartOfDayTimestamp(testDate) + 3600000 // 1 hour after start of day
        val transactions = listOf(
            createSampleTransaction(timestamp = testTimestamp, product = "Tilapia", amount = 50.0),
            createSampleTransaction(timestamp = testTimestamp + 3600000, product = "Mackerel", amount = 25.0)
        )
        
        // Manually set the date for these transactions
        val transactionsWithDate = transactions.map { it.copy(date = testDate) }
        
        // When
        transactionsWithDate.forEach { repository.insertTransaction(it) }
        
        // Then
        val dateTransactions = repository.getTransactionsByDate(testDate).first()
        assertEquals("Should get transactions for specific date", 2, dateTransactions.size)
        
        val totalSales = repository.getTotalSalesForDate(testDate)
        assertEquals("Should calculate total sales for date", 75.0, totalSales, 0.01)
        
        val count = repository.getTransactionCountForDate(testDate)
        assertEquals("Should get transaction count for date", 2, count)
        
        val topProduct = repository.getTopProductForDate(testDate)
        assertEquals("Should get top product for date", "Tilapia", topProduct)
    }
    
    // Helper method to create sample transactions
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