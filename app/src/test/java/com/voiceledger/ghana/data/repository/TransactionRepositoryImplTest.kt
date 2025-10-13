package com.voiceledger.ghana.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.voiceledger.ghana.data.local.database.VoiceLedgerDatabase
import com.voiceledger.ghana.data.local.entity.Transaction
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
        val insertedId = repository.insertTransaction(transaction)
        
        // Then
        assertTrue("Should return valid ID", insertedId > 0)
        
        val retrieved = repository.getTransactionById(insertedId)
        assertNotNull("Should retrieve inserted transaction", retrieved)
        assertEquals("Should have same product", transaction.productName, retrieved?.productName)
        assertEquals("Should have same quantity", transaction.quantity, retrieved?.quantity)
        assertEquals("Should have same price", transaction.totalPrice, retrieved?.totalPrice, 0.01)
    }
    
    @Test
    fun testGetAllTransactions_shouldReturnAllTransactions() = runTest {
        // Given
        val transactions = listOf(
            createSampleTransaction(productName = "Tilapia"),
            createSampleTransaction(productName = "Mackerel"),
            createSampleTransaction(productName = "Sardines")
        )
        
        transactions.forEach { repository.insertTransaction(it) }
        
        // When
        val allTransactions = repository.getAllTransactions().first()
        
        // Then
        assertEquals("Should return all transactions", 3, allTransactions.size)
        assertTrue("Should contain Tilapia", allTransactions.any { it.productName == "Tilapia" })
        assertTrue("Should contain Mackerel", allTransactions.any { it.productName == "Mackerel" })
        assertTrue("Should contain Sardines", allTransactions.any { it.productName == "Sardines" })
    }
    
    @Test
    fun testGetTransactionsByDateRange_shouldReturnCorrectTransactions() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val oneDayAgo = now - (24 * 60 * 60 * 1000)
        val twoDaysAgo = now - (2 * 24 * 60 * 60 * 1000)
        val threeDaysAgo = now - (3 * 24 * 60 * 60 * 1000)
        
        val transactions = listOf(
            createSampleTransaction(timestamp = threeDaysAgo, productName = "Old Transaction"),
            createSampleTransaction(timestamp = oneDayAgo, productName = "Recent Transaction 1"),
            createSampleTransaction(timestamp = now, productName = "Recent Transaction 2")
        )
        
        transactions.forEach { repository.insertTransaction(it) }
        
        // When
        val recentTransactions = repository.getTransactionsByDateRange(twoDaysAgo, now).first()
        
        // Then
        assertEquals("Should return 2 recent transactions", 2, recentTransactions.size)
        assertTrue("Should contain Recent Transaction 1", 
            recentTransactions.any { it.productName == "Recent Transaction 1" })
        assertTrue("Should contain Recent Transaction 2", 
            recentTransactions.any { it.productName == "Recent Transaction 2" })
        assertFalse("Should not contain old transaction", 
            recentTransactions.any { it.productName == "Old Transaction" })
    }
    
    @Test
    fun testGetTransactionsByProduct_shouldReturnCorrectTransactions() = runTest {
        // Given
        val transactions = listOf(
            createSampleTransaction(productName = "Tilapia", quantity = 2),
            createSampleTransaction(productName = "Tilapia", quantity = 5),
            createSampleTransaction(productName = "Mackerel", quantity = 3)
        )
        
        transactions.forEach { repository.insertTransaction(it) }
        
        // When
        val tilapiaTransactions = repository.getTransactionsByProduct("Tilapia").first()
        
        // Then
        assertEquals("Should return 2 tilapia transactions", 2, tilapiaTransactions.size)
        assertTrue("All should be tilapia transactions", 
            tilapiaTransactions.all { it.productName == "Tilapia" })
        assertEquals("Should have correct quantities", 
            setOf(2, 5), tilapiaTransactions.map { it.quantity }.toSet())
    }
    
    @Test
    fun testGetTransactionsByCustomer_shouldReturnCorrectTransactions() = runTest {
        // Given
        val transactions = listOf(
            createSampleTransaction(customerId = "customer1", productName = "Tilapia"),
            createSampleTransaction(customerId = "customer1", productName = "Mackerel"),
            createSampleTransaction(customerId = "customer2", productName = "Sardines")
        )
        
        transactions.forEach { repository.insertTransaction(it) }
        
        // When
        val customer1Transactions = repository.getTransactionsByCustomer("customer1").first()
        
        // Then
        assertEquals("Should return 2 transactions for customer1", 2, customer1Transactions.size)
        assertTrue("All should be for customer1", 
            customer1Transactions.all { it.customerId == "customer1" })
        assertEquals("Should have correct products", 
            setOf("Tilapia", "Mackerel"), customer1Transactions.map { it.productName }.toSet())
    }
    
    @Test
    fun testUpdateTransaction_shouldUpdateSuccessfully() = runTest {
        // Given
        val transaction = createSampleTransaction()
        val insertedId = repository.insertTransaction(transaction)
        
        val updatedTransaction = transaction.copy(
            id = insertedId,
            productName = "Updated Product",
            totalPrice = 99.99
        )
        
        // When
        repository.updateTransaction(updatedTransaction)
        
        // Then
        val retrieved = repository.getTransactionById(insertedId)
        assertNotNull("Should retrieve updated transaction", retrieved)
        assertEquals("Should have updated product name", "Updated Product", retrieved?.productName)
        assertEquals("Should have updated price", 99.99, retrieved?.totalPrice, 0.01)
    }
    
    @Test
    fun testDeleteTransaction_shouldRemoveSuccessfully() = runTest {
        // Given
        val transaction = createSampleTransaction()
        val insertedId = repository.insertTransaction(transaction)
        
        // Verify it exists
        assertNotNull("Transaction should exist before deletion", 
            repository.getTransactionById(insertedId))
        
        // When
        repository.deleteTransaction(insertedId)
        
        // Then
        assertNull("Transaction should not exist after deletion", 
            repository.getTransactionById(insertedId))
    }
    
    @Test
    fun testGetDailySummary_shouldCalculateCorrectTotals() = runTest {
        // Given
        val today = System.currentTimeMillis()
        val startOfDay = today - (today % (24 * 60 * 60 * 1000))
        
        val transactions = listOf(
            createSampleTransaction(timestamp = startOfDay + 1000, totalPrice = 10.0),
            createSampleTransaction(timestamp = startOfDay + 2000, totalPrice = 15.0),
            createSampleTransaction(timestamp = startOfDay + 3000, totalPrice = 20.0),
            // Transaction from previous day (should not be included)
            createSampleTransaction(timestamp = startOfDay - 1000, totalPrice = 100.0)
        )
        
        transactions.forEach { repository.insertTransaction(it) }
        
        // When
        val summary = repository.getDailySummary(startOfDay, startOfDay + (24 * 60 * 60 * 1000))
        
        // Then
        assertNotNull("Should return summary", summary)
        assertEquals("Should have 3 transactions", 3, summary?.totalTransactions)
        assertEquals("Should have correct total revenue", 45.0, summary?.totalRevenue, 0.01)
    }
    
    @Test
    fun testGetTopProducts_shouldReturnMostSoldProducts() = runTest {
        // Given
        val transactions = listOf(
            createSampleTransaction(productName = "Tilapia", quantity = 5),
            createSampleTransaction(productName = "Tilapia", quantity = 3),
            createSampleTransaction(productName = "Mackerel", quantity = 2),
            createSampleTransaction(productName = "Sardines", quantity = 1)
        )
        
        transactions.forEach { repository.insertTransaction(it) }
        
        // When
        val topProducts = repository.getTopProducts(2)
        
        // Then
        assertEquals("Should return top 2 products", 2, topProducts.size)
        assertEquals("First should be Tilapia", "Tilapia", topProducts[0].productName)
        assertEquals("Tilapia should have 8 total quantity", 8, topProducts[0].totalQuantity)
        assertEquals("Second should be Mackerel", "Mackerel", topProducts[1].productName)
        assertEquals("Mackerel should have 2 total quantity", 2, topProducts[1].totalQuantity)
    }
    
    @Test
    fun testSearchTransactions_shouldReturnMatchingTransactions() = runTest {
        // Given
        val transactions = listOf(
            createSampleTransaction(productName = "Fresh Tilapia", customerId = "john_doe"),
            createSampleTransaction(productName = "Smoked Mackerel", customerId = "jane_smith"),
            createSampleTransaction(productName = "Dried Sardines", customerId = "john_doe")
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
    fun testGetTransactionCount_shouldReturnCorrectCount() = runTest {
        // Given
        val transactions = listOf(
            createSampleTransaction(),
            createSampleTransaction(),
            createSampleTransaction()
        )
        
        transactions.forEach { repository.insertTransaction(it) }
        
        // When
        val count = repository.getTransactionCount()
        
        // Then
        assertEquals("Should return correct count", 3, count)
    }
    
    @Test
    fun testDeleteOlderThan_shouldDeleteOldTransactions() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val oldTimestamp = now - (10 * 24 * 60 * 60 * 1000) // 10 days ago
        val recentTimestamp = now - (1 * 24 * 60 * 60 * 1000) // 1 day ago
        val cutoffTimestamp = now - (5 * 24 * 60 * 60 * 1000) // 5 days ago
        
        val transactions = listOf(
            createSampleTransaction(timestamp = oldTimestamp, productName = "Old Transaction"),
            createSampleTransaction(timestamp = recentTimestamp, productName = "Recent Transaction")
        )
        
        transactions.forEach { repository.insertTransaction(it) }
        
        // When
        val deletedCount = repository.deleteOlderThan(cutoffTimestamp)
        
        // Then
        assertEquals("Should delete 1 old transaction", 1, deletedCount)
        
        val remainingTransactions = repository.getAllTransactions().first()
        assertEquals("Should have 1 remaining transaction", 1, remainingTransactions.size)
        assertEquals("Remaining should be recent transaction", 
            "Recent Transaction", remainingTransactions[0].productName)
    }
    
    @Test
    fun testConcurrentInserts_shouldHandleCorrectly() = runTest {
        // Given
        val transactions = (1..10).map { 
            createSampleTransaction(productName = "Product $it") 
        }
        
        // When - simulate concurrent inserts
        val insertedIds = transactions.map { transaction ->
            repository.insertTransaction(transaction)
        }
        
        // Then
        assertEquals("Should insert all transactions", 10, insertedIds.size)
        assertTrue("All IDs should be unique", insertedIds.toSet().size == 10)
        
        val allTransactions = repository.getAllTransactions().first()
        assertEquals("Should retrieve all transactions", 10, allTransactions.size)
    }
    
    // Helper method to create sample transactions
    private fun createSampleTransaction(
        productName: String = "Tilapia",
        quantity: Int = 3,
        unit: String = "pieces",
        totalPrice: Double = 25.50,
        customerId: String = "customer123",
        timestamp: Long = System.currentTimeMillis()
    ): Transaction {
        return Transaction(
            id = 0, // Will be auto-generated
            productName = productName,
            quantity = quantity,
            unit = unit,
            unitPrice = totalPrice / quantity,
            totalPrice = totalPrice,
            customerId = customerId,
            speakerId = "speaker123",
            timestamp = timestamp,
            confidence = 0.95f,
            audioFilePath = "/path/to/audio.wav",
            isVerified = true,
            notes = "Test transaction"
        )
    }
}