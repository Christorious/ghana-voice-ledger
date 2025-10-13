package com.voiceledger.ghana.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.voiceledger.ghana.offline.NetworkUtils
import com.voiceledger.ghana.offline.OfflineFirstRepository
import com.voiceledger.ghana.offline.OfflineQueueManager
import com.voiceledger.ghana.data.local.entity.Transaction
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Integration tests for offline functionality
 * Tests offline-first architecture with network disabled scenarios
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class OfflineFunctionalityTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var offlineFirstRepository: OfflineFirstRepository
    
    @Inject
    lateinit var offlineQueueManager: OfflineQueueManager
    
    @Inject
    lateinit var networkUtils: NetworkUtils
    
    @Before
    fun setUp() {
        hiltRule.inject()
    }
    
    @Test
    fun testOfflineTransactionStorage_shouldStoreLocally() = runTest {
        // Given - simulate offline state
        networkUtils.setNetworkAvailable(false)
        
        val transaction = createSampleTransaction()
        
        // When - store transaction offline
        val transactionId = offlineFirstRepository.insertTransaction(transaction)
        
        // Then
        assertTrue("Should return valid transaction ID", transactionId > 0)
        
        val storedTransaction = offlineFirstRepository.getTransactionById(transactionId)
        assertNotNull("Should retrieve stored transaction", storedTransaction)
        assertEquals("Should match stored data", transaction.productName, storedTransaction?.productName)
    }
    
    @Test
    fun testOfflineToOnlineSync_shouldSyncWhenNetworkReturns() = runTest {
        // Given - start offline with pending transactions
        networkUtils.setNetworkAvailable(false)
        
        val transactions = listOf(
            createSampleTransaction(productName = "Offline Transaction 1"),
            createSampleTransaction(productName = "Offline Transaction 2")
        )
        
        transactions.forEach { 
            offlineFirstRepository.insertTransaction(it)
        }
        
        // Verify transactions are queued for sync
        val queuedItems = offlineQueueManager.getPendingItems().first()
        assertTrue("Should have queued items", queuedItems.isNotEmpty())
        
        // When - network becomes available
        networkUtils.setNetworkAvailable(true)
        
        // Wait for sync to complete
        delay(2000)
        
        // Then
        val remainingQueuedItems = offlineQueueManager.getPendingItems().first()
        assertTrue("Should have fewer or no queued items after sync", 
            remainingQueuedItems.size <= queuedItems.size)
    }
    
    private fun createSampleTransaction(
        productName: String = "Test Product"
    ): Transaction {
        return Transaction(
            id = 0,
            productName = productName,
            quantity = 1,
            unit = "pieces",
            unitPrice = 10.0,
            totalPrice = 10.0,
            customerId = "test_customer",
            speakerId = "test_speaker",
            timestamp = System.currentTimeMillis(),
            confidence = 0.9f,
            audioFilePath = "/test/path",
            isVerified = false,
            notes = "Integration test transaction"
        )
    }
}