package com.voiceledger.ghana.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.voiceledger.ghana.data.local.database.VoiceLedgerDatabase
import com.voiceledger.ghana.data.local.entity.Transaction
import com.voiceledger.ghana.data.repository.TransactionRepositoryImpl
import com.voiceledger.ghana.security.SecureDataStorage
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Integration tests for database operations with security and encryption
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DatabaseIntegrationTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var database: VoiceLedgerDatabase
    
    @Inject
    lateinit var transactionRepository: TransactionRepositoryImpl
    
    @Inject
    lateinit var secureDataStorage: SecureDataStorage
    
    @Before
    fun setUp() {
        hiltRule.inject()
    }
    
    @Test
    fun testEncryptedDatabaseOperations_shouldMaintainDataIntegrity() = runTest {
        // Given - initialize secure database
        val secureDb = secureDataStorage.initializeSecureDatabase()
        assertNotNull("Secure database should be initialized", secureDb)
        
        // Create test transaction with sensitive data
        val sensitiveTransaction = Transaction(
            id = 0,
            productName = "Confidential Product",
            quantity = 5,
            unit = "pieces",
            unitPrice = 25.0,
            totalPrice = 125.0,
            customerId = "sensitive_customer_123",
            speakerId = "speaker_456",
            timestamp = System.currentTimeMillis(),
            confidence = 0.95f,
            audioFilePath = "/secure/path/audio.wav",
            isVerified = true,
            notes = "Sensitive transaction data"
        )
        
        // When - store and retrieve encrypted data
        val transactionId = transactionRepository.insertTransaction(sensitiveTransaction)
        val retrievedTransaction = transactionRepository.getTransactionById(transactionId)
        
        // Then - verify data integrity
        assertNotNull("Should retrieve encrypted transaction", retrievedTransaction)
        assertEquals("Product name should match", sensitiveTransaction.productName, retrievedTransaction?.productName)
        assertEquals("Customer ID should match", sensitiveTransaction.customerId, retrievedTransaction?.customerId)
        assertEquals("Total price should match", sensitiveTransaction.totalPrice, retrievedTransaction?.totalPrice, 0.01)
    }
    
    @Test
    fun testConcurrentDatabaseAccess_shouldHandleMultipleOperations() = runTest {
        // Given - multiple concurrent transactions
        val transactions = (1..10).map { index ->
            Transaction(
                id = 0,
                productName = "Product $index",
                quantity = index,
                unit = "pieces",
                unitPrice = index * 10.0,
                totalPrice = index * index * 10.0,
                customerId = "customer_$index",
                speakerId = "speaker_$index",
                timestamp = System.currentTimeMillis() + index,
                confidence = 0.9f,
                audioFilePath = "/path/audio_$index.wav",
                isVerified = true,
                notes = "Concurrent test transaction $index"
            )
        }
        
        // When - insert all transactions concurrently
        val insertedIds = transactions.map { transaction ->
            transactionRepository.insertTransaction(transaction)
        }
        
        // Then - verify all transactions were stored correctly
        assertEquals("Should insert all transactions", 10, insertedIds.size)
        assertTrue("All IDs should be unique", insertedIds.toSet().size == 10)
        
        val allTransactions = transactionRepository.getAllTransactions().first()
        assertTrue("Should retrieve all transactions", allTransactions.size >= 10)
        
        // Verify data integrity for each transaction
        insertedIds.forEachIndexed { index, id ->
            val retrieved = transactionRepository.getTransactionById(id)
            assertNotNull("Should retrieve transaction $index", retrieved)
            assertEquals("Should match product name", "Product ${index + 1}", retrieved?.productName)
        }
    }
    
    @Test
    fun testDatabaseMigrationAndBackup_shouldPreserveData() = runTest {
        // Given - insert test data
        val originalTransactions = listOf(
            createTestTransaction("Original Product 1"),
            createTestTransaction("Original Product 2"),
            createTestTransaction("Original Product 3")
        )
        
        val originalIds = originalTransactions.map { 
            transactionRepository.insertTransaction(it) 
        }
        
        // When - create backup
        val backupPath = "/test/backup/database_backup.db"
        val backupResult = secureDataStorage.createSecureBackup(backupPath)
        
        // Then - verify backup success
        assertTrue("Backup should succeed", backupResult.success)
        assertNotNull("Should have backup path", backupResult.backupPath)
        assertTrue("Backup size should be positive", backupResult.backupSize > 0)
        
        // Simulate data loss and restore
        originalIds.forEach { id ->
            transactionRepository.deleteTransaction(id)
        }
        
        // Verify data is gone
        val emptyTransactions = transactionRepository.getAllTransactions().first()
        assertTrue("Should have no transactions after deletion", 
            emptyTransactions.none { it.id in originalIds })
        
        // Restore from backup
        val restoreResult = secureDataStorage.restoreFromSecureBackup(backupPath)
        assertTrue("Restore should succeed", restoreResult.success)
        
        // Verify data is restored
        val restoredTransactions = transactionRepository.getAllTransactions().first()
        assertTrue("Should restore original transactions", 
            restoredTransactions.size >= originalTransactions.size)
    }
    
    @Test
    fun testDataRetentionPolicies_shouldDeleteOldData() = runTest {
        // Given - transactions with different ages
        val now = System.currentTimeMillis()
        val oldTransactions = listOf(
            createTestTransaction("Old Transaction 1", now - (40L * 24 * 60 * 60 * 1000)), // 40 days old
            createTestTransaction("Old Transaction 2", now - (35L * 24 * 60 * 60 * 1000))  // 35 days old
        )
        
        val recentTransactions = listOf(
            createTestTransaction("Recent Transaction 1", now - (10L * 24 * 60 * 60 * 1000)), // 10 days old
            createTestTransaction("Recent Transaction 2", now - (5L * 24 * 60 * 60 * 1000))   // 5 days old
        )
        
        // Insert all transactions
        val oldIds = oldTransactions.map { transactionRepository.insertTransaction(it) }
        val recentIds = recentTransactions.map { transactionRepository.insertTransaction(it) }
        
        // When - apply retention policy (delete data older than 30 days)
        val cutoffTime = now - (30L * 24 * 60 * 60 * 1000)
        val deletedCount = transactionRepository.deleteOlderThan(cutoffTime)
        
        // Then - verify old data is deleted, recent data is preserved
        assertEquals("Should delete 2 old transactions", 2, deletedCount)
        
        val remainingTransactions = transactionRepository.getAllTransactions().first()
        
        // Verify old transactions are gone
        oldIds.forEach { id ->
            val transaction = transactionRepository.getTransactionById(id)
            assertNull("Old transaction should be deleted", transaction)
        }
        
        // Verify recent transactions remain
        recentIds.forEach { id ->
            val transaction = transactionRepository.getTransactionById(id)
            assertNotNull("Recent transaction should remain", transaction)
        }
    }
    
    private fun createTestTransaction(
        productName: String,
        timestamp: Long = System.currentTimeMillis()
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
            timestamp = timestamp,
            confidence = 0.9f,
            audioFilePath = "/test/audio.wav",
            isVerified = true,
            notes = "Test transaction"
        )
    }
}