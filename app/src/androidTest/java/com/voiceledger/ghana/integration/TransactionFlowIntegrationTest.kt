package com.voiceledger.ghana.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.voiceledger.ghana.data.local.database.VoiceLedgerDatabase
import com.voiceledger.ghana.data.local.entity.Transaction
import com.voiceledger.ghana.data.repository.TransactionRepositoryImpl
import com.voiceledger.ghana.offline.NetworkUtils
import com.voiceledger.ghana.offline.OfflineOperation
import com.voiceledger.ghana.offline.OfflineQueueManager
import com.voiceledger.ghana.offline.OperationPriority
import com.voiceledger.ghana.offline.OperationType
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * Integration test for end-to-end transaction flow
 * Tests transaction insertion, retrieval, summaries, and offline queue interactions
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TransactionFlowIntegrationTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var database: VoiceLedgerDatabase

    @Inject
    lateinit var offlineQueueManager: OfflineQueueManager

    private lateinit var repository: TransactionRepositoryImpl
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    @Before
    fun setUp() {
        NetworkUtils.resetTestMode()
        hiltRule.inject()
        repository = TransactionRepositoryImpl(database.transactionDao())
    }

    @After
    fun tearDown() {
        NetworkUtils.resetTestMode()
        database.close()
    }

    @Test
    fun testEndToEndTransactionFlow_insertRetrieveAndVerify() = runTest {
        // Given - Create a test transaction
        val transaction = createTestTransaction(
            id = "test-tx-001",
            product = "Tilapia",
            amount = 50.0,
            quantity = 5
        )

        // When - Insert transaction through repository
        repository.insertTransaction(transaction)

        // Then - Verify retrieval by ID
        val retrieved = repository.getTransactionById(transaction.id)
        assertNotNull("Transaction should be retrieved", retrieved)
        assertEquals("Product name should match", transaction.product, retrieved?.product)
        assertEquals("Amount should match", transaction.amount, retrieved?.amount, 0.01)
        assertEquals("Quantity should match", transaction.quantity, retrieved?.quantity)
    }

    @Test
    fun testTransactionFlow_multipleInsertionsAndRetrieval() = runTest {
        // Given - Multiple transactions
        val transactions = listOf(
            createTestTransaction("tx-001", "Tilapia", 50.0, 5),
            createTestTransaction("tx-002", "Mackerel", 30.0, 3),
            createTestTransaction("tx-003", "Catfish", 75.0, 7)
        )

        // When - Insert all transactions
        repository.insertTransactions(transactions)

        // Then - Verify all are retrievable
        val allTransactions = repository.getAllTransactions().first()
        assertTrue("Should have at least 3 transactions", allTransactions.size >= 3)

        // Verify each transaction can be retrieved by ID
        transactions.forEach { original ->
            val retrieved = repository.getTransactionById(original.id)
            assertNotNull("Transaction ${original.id} should exist", retrieved)
            assertEquals("Product should match", original.product, retrieved?.product)
        }
    }

    @Test
    fun testTransactionFlow_dailySummaryCalculations() = runTest {
        // Given - Transactions for today
        val today = dateFormat.format(Date())
        val todayTransactions = listOf(
            createTestTransaction("daily-001", "Tilapia", 100.0, 10),
            createTestTransaction("daily-002", "Mackerel", 50.0, 5),
            createTestTransaction("daily-003", "Catfish", 75.0, 7)
        )

        // When - Insert transactions
        repository.insertTransactions(todayTransactions)

        // Then - Verify daily summary calculations
        val totalSales = repository.getTodaysTotalSales()
        assertTrue("Total sales should be at least 225.0", totalSales >= 225.0)

        val transactionCount = repository.getTodaysTransactionCount()
        assertTrue("Should have at least 3 transactions today", transactionCount >= 3)

        val todaysTransactions = repository.getTodaysTransactions().first()
        assertTrue("Should retrieve today's transactions", todaysTransactions.size >= 3)
    }

    @Test
    fun testTransactionFlow_queryByProduct() = runTest {
        // Given - Multiple transactions with same product
        val transactions = listOf(
            createTestTransaction("prod-001", "Tilapia", 50.0, 5),
            createTestTransaction("prod-002", "Tilapia", 60.0, 6),
            createTestTransaction("prod-003", "Mackerel", 30.0, 3)
        )

        // When - Insert and query by product
        repository.insertTransactions(transactions)
        val tilapiaTransactions = repository.getTransactionsByProduct("Tilapia").first()

        // Then - Verify correct filtering
        assertTrue("Should have at least 2 Tilapia transactions", tilapiaTransactions.size >= 2)
        tilapiaTransactions.forEach { tx ->
            assertTrue("Product should contain Tilapia", tx.product.contains("Tilapia"))
        }
    }

    @Test
    fun testTransactionFlow_queryByTimeRange() = runTest {
        // Given - Transactions at different times
        val now = System.currentTimeMillis()
        val oneHourAgo = now - (60 * 60 * 1000)
        val twoHoursAgo = now - (2 * 60 * 60 * 1000)

        val transactions = listOf(
            createTestTransaction("time-001", "Tilapia", 50.0, timestamp = twoHoursAgo),
            createTestTransaction("time-002", "Mackerel", 30.0, timestamp = oneHourAgo),
            createTestTransaction("time-003", "Catfish", 75.0, timestamp = now)
        )

        // When - Insert and query by time range
        repository.insertTransactions(transactions)
        val recentTransactions = repository.getTransactionsByTimeRange(oneHourAgo, now).first()

        // Then - Verify time filtering
        assertTrue("Should have at least 2 recent transactions", recentTransactions.size >= 2)
        recentTransactions.forEach { tx ->
            assertTrue("Transaction should be within time range", tx.timestamp >= oneHourAgo)
        }
    }

    @Test
    fun testTransactionFlow_updateAndDelete() = runTest {
        // Given - Insert a transaction
        val transaction = createTestTransaction("update-001", "Tilapia", 50.0, 5)
        repository.insertTransaction(transaction)

        // When - Update the transaction
        val updated = transaction.copy(
            product = "Red Snapper",
            amount = 100.0,
            quantity = 10
        )
        repository.updateTransaction(updated)

        // Then - Verify update
        val retrieved = repository.getTransactionById(transaction.id)
        assertEquals("Product should be updated", "Red Snapper", retrieved?.product)
        assertEquals("Amount should be updated", 100.0, retrieved?.amount, 0.01)

        // When - Delete the transaction
        repository.deleteTransactionById(transaction.id)

        // Then - Verify deletion
        val afterDelete = repository.getTransactionById(transaction.id)
        assertNull("Transaction should be deleted", afterDelete)
    }

    @Test
    fun testTransactionFlow_markAsReviewed() = runTest {
        // Given - Transaction needing review
        val transaction = createTestTransaction(
            id = "review-001",
            product = "Tilapia",
            amount = 50.0,
            needsReview = true
        )
        repository.insertTransaction(transaction)

        // Verify it needs review
        val needsReview = repository.getTransactionsNeedingReview().first()
        assertTrue("Should have transactions needing review", needsReview.any { it.id == transaction.id })

        // When - Mark as reviewed
        repository.markTransactionAsReviewed(transaction.id)

        // Then - Verify it no longer needs review
        val afterReview = repository.getTransactionsNeedingReview().first()
        assertFalse("Should not need review anymore", afterReview.any { it.id == transaction.id })
    }

    @Test
    fun testTransactionFlow_unsyncedTransactions() = runTest {
        // Given - Unsynced transactions
        val transactions = listOf(
            createTestTransaction("sync-001", "Tilapia", 50.0, synced = false),
            createTestTransaction("sync-002", "Mackerel", 30.0, synced = false),
            createTestTransaction("sync-003", "Catfish", 75.0, synced = true)
        )

        // When - Insert transactions
        repository.insertTransactions(transactions)

        // Then - Verify unsynced filtering
        val unsynced = repository.getUnsyncedTransactions()
        assertTrue("Should have at least 2 unsynced transactions", unsynced.size >= 2)
        assertTrue("All should be unsynced", unsynced.all { !it.synced })

        // When - Mark as synced
        repository.markTransactionsAsSynced(listOf("sync-001", "sync-002"))

        // Then - Verify sync status updated
        val afterSync = repository.getUnsyncedTransactions()
        assertFalse("sync-001 should be synced", afterSync.any { it.id == "sync-001" })
        assertFalse("sync-002 should be synced", afterSync.any { it.id == "sync-002" })
    }

    @Test
    fun testTransactionFlow_searchTransactions() = runTest {
        // Given - Various transactions
        val transactions = listOf(
            createTestTransaction("search-001", "Tilapia", 50.0),
            createTestTransaction("search-002", "Mackerel", 30.0),
            createTestTransaction("search-003", "Catfish", 75.0)
        )

        // When - Insert and search
        repository.insertTransactions(transactions)
        val searchResults = repository.searchTransactions("Tilapia").first()

        // Then - Verify search results
        assertTrue("Should find Tilapia transactions", searchResults.any { it.product.contains("Tilapia") })
    }

    @Test
    fun testTransactionFlow_offlineQueueInteractions() = runTest {
        // Given - Create offline operations through queue manager
        val operations = listOf(
            OfflineOperation(
                id = "queue-op-001",
                type = OperationType.TRANSACTION_SYNC,
                data = "{\"transactionId\":\"tx-queue-001\"}",
                timestamp = System.currentTimeMillis(),
                priority = OperationPriority.HIGH
            ),
            OfflineOperation(
                id = "queue-op-002",
                type = OperationType.SUMMARY_SYNC,
                data = "{\"summaryId\":\"summary-001\"}",
                timestamp = System.currentTimeMillis(),
                priority = OperationPriority.NORMAL
            )
        )

        // When - Enqueue operations while offline
        NetworkUtils.setNetworkAvailable(false)
        operations.forEach { offlineQueueManager.enqueueOperation(it) }

        // Then - Verify operations are persisted and retrievable
        val queuedItems = offlineQueueManager.getOperationsByType(OperationType.TRANSACTION_SYNC)
        assertTrue("Transaction sync operations should be queued", queuedItems.any { it.id == "queue-op-001" })

        // When - Network becomes available and operations processed
        NetworkUtils.setNetworkAvailable(true)
        offlineQueueManager.processAllPendingOperations()
        delay(1500)

        // Then - Operations should be removed from queue
        val remaining = offlineQueueManager.getOperationsByType(OperationType.TRANSACTION_SYNC)
        assertTrue("Operations should be processed and removed", remaining.none { it.id == "queue-op-001" })
    }

    @Test
    fun testTransactionFlow_analyticsCalculations() = runTest {
        // Given - Multiple transactions for analytics
        val today = dateFormat.format(Date())
        val transactions = listOf(
            createTestTransaction("analytics-001", "Tilapia", 100.0, 10),
            createTestTransaction("analytics-002", "Tilapia", 50.0, 5),
            createTestTransaction("analytics-003", "Mackerel", 30.0, 3)
        )

        // When - Insert transactions
        repository.insertTransactions(transactions)

        // Then - Verify analytics
        val topProduct = repository.getTodaysTopProduct()
        assertEquals("Top product should be Tilapia", "Tilapia", topProduct)

        val avgValue = repository.getTodaysAverageTransactionValue()
        assertTrue("Average should be positive", avgValue > 0)
    }

    private fun createTestTransaction(
        id: String,
        product: String,
        amount: Double,
        quantity: Int? = 1,
        timestamp: Long = System.currentTimeMillis(),
        needsReview: Boolean = false,
        synced: Boolean = false
    ): Transaction {
        return Transaction(
            id = id,
            timestamp = timestamp,
            date = dateFormat.format(Date(timestamp)),
            amount = amount,
            currency = "GHS",
            product = product,
            quantity = quantity,
            unit = "pieces",
            customerId = "test-customer-${System.currentTimeMillis()}",
            confidence = 0.95f,
            transcriptSnippet = "Test transaction for $product",
            sellerConfidence = 0.9f,
            customerConfidence = 0.85f,
            needsReview = needsReview,
            synced = synced,
            originalPrice = amount,
            finalPrice = amount,
            marketSession = "AM"
        )
    }
}
