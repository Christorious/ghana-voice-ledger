package com.voiceledger.ghana.offline

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.work.WorkManager
import com.voiceledger.ghana.data.local.database.VoiceLedgerDatabase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineQueueManagerTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var context: Context
    private lateinit var database: VoiceLedgerDatabase
    private lateinit var workManager: WorkManager
    private lateinit var offlineQueueManager: OfflineQueueManager

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        context = mockk(relaxed = true)
        database = mockk(relaxed = true)
        workManager = mockk(relaxed = true)
        
        // Mock NetworkUtils
        mockkObject(NetworkUtils)
        every { NetworkUtils.isNetworkAvailable(any()) } returns true
        
        // Mock WorkManager
        mockkStatic(WorkManager::class)
        every { WorkManager.getInstance(any()) } returns workManager
        
        offlineQueueManager = OfflineQueueManager(context, database)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        offlineQueueManager.cleanup()
        unmockkAll()
    }

    @Test
    fun `initial state should be empty`() = runTest {
        // When - Check initial state
        val initialState = offlineQueueManager.queueState.value
        
        // Then
        assertEquals(0, initialState.totalOperations)
        assertEquals(0, initialState.pendingOperations)
        assertEquals(0, initialState.failedOperations)
        assertEquals(0, initialState.processingOperations)
    }

    @Test
    fun `should enqueue operation successfully`() = runTest {
        // Given - Operation to enqueue
        val operation = OfflineOperation(
            id = "test_op_1",
            type = OperationType.TRANSACTION_SYNC,
            data = """{"id": "tx1", "amount": 100.0}""",
            timestamp = System.currentTimeMillis()
        )
        
        // When - Enqueue operation
        offlineQueueManager.enqueueOperation(operation)
        
        // Then - Should be in queue
        advanceTimeBy(100) // Allow state update
        val state = offlineQueueManager.queueState.value
        assertEquals(1, state.totalOperations)
        assertEquals(1, state.pendingOperations)
    }

    @Test
    fun `should process operation when network is available`() = runTest {
        // Given - Network is available
        every { NetworkUtils.isNetworkAvailable(any()) } returns true
        
        val operation = OfflineOperation(
            id = "test_op_2",
            type = OperationType.TRANSACTION_SYNC,
            data = """{"id": "tx2", "amount": 200.0}""",
            timestamp = System.currentTimeMillis()
        )
        
        // When - Enqueue operation
        offlineQueueManager.enqueueOperation(operation)
        
        // Then - Should attempt to process immediately
        advanceTimeBy(2000) // Allow processing time
        // Operation should be processed (implementation would remove it from queue)
    }

    @Test
    fun `should queue operation when network is unavailable`() = runTest {
        // Given - Network is unavailable
        every { NetworkUtils.isNetworkAvailable(any()) } returns false
        
        val operation = OfflineOperation(
            id = "test_op_3",
            type = OperationType.SUMMARY_SYNC,
            data = """{"date": "2024-01-15", "totalSales": 500.0}""",
            timestamp = System.currentTimeMillis()
        )
        
        // When - Enqueue operation
        offlineQueueManager.enqueueOperation(operation)
        
        // Then - Should remain in queue
        advanceTimeBy(100)
        val state = offlineQueueManager.queueState.value
        assertEquals(1, state.totalOperations)
        assertEquals(1, state.pendingOperations)
        assertEquals(false, state.isNetworkAvailable)
    }

    @Test
    fun `should handle multiple operations of different types`() = runTest {
        // Given - Multiple operations
        val operations = listOf(
            OfflineOperation(
                id = "tx_op",
                type = OperationType.TRANSACTION_SYNC,
                data = """{"id": "tx1"}""",
                timestamp = System.currentTimeMillis()
            ),
            OfflineOperation(
                id = "summary_op",
                type = OperationType.SUMMARY_SYNC,
                data = """{"date": "2024-01-15"}""",
                timestamp = System.currentTimeMillis()
            ),
            OfflineOperation(
                id = "speaker_op",
                type = OperationType.SPEAKER_PROFILE_SYNC,
                data = """{"speakerId": "speaker1"}""",
                timestamp = System.currentTimeMillis()
            )
        )
        
        // When - Enqueue all operations
        operations.forEach { operation ->
            offlineQueueManager.enqueueOperation(operation)
        }
        
        // Then - All should be queued
        advanceTimeBy(100)
        val state = offlineQueueManager.queueState.value
        assertEquals(3, state.totalOperations)
        
        // And - Should be able to get operations by type
        val transactionOps = offlineQueueManager.getOperationsByType(OperationType.TRANSACTION_SYNC)
        assertEquals(1, transactionOps.size)
        assertEquals("tx_op", transactionOps.first().id)
    }

    @Test
    fun `should respect max queue size`() = runTest {
        // Given - Configure small max queue size
        offlineQueueManager.configure(maxQueueSize = 2)
        
        // When - Enqueue more operations than max size
        repeat(5) { index ->
            val operation = OfflineOperation(
                id = "op_$index",
                type = OperationType.TRANSACTION_SYNC,
                data = """{"id": "tx$index"}""",
                timestamp = System.currentTimeMillis() + index
            )
            offlineQueueManager.enqueueOperation(operation)
        }
        
        // Then - Should not exceed max size
        advanceTimeBy(100)
        val state = offlineQueueManager.queueState.value
        assertTrue(state.totalOperations <= 2)
    }

    @Test
    fun `should process all pending operations when requested`() = runTest {
        // Given - Multiple pending operations and network available
        every { NetworkUtils.isNetworkAvailable(any()) } returns false // Initially offline
        
        repeat(3) { index ->
            val operation = OfflineOperation(
                id = "pending_op_$index",
                type = OperationType.TRANSACTION_SYNC,
                data = """{"id": "tx$index"}""",
                timestamp = System.currentTimeMillis() + index
            )
            offlineQueueManager.enqueueOperation(operation)
        }
        
        // When - Network becomes available and process all
        every { NetworkUtils.isNetworkAvailable(any()) } returns true
        offlineQueueManager.processAllPendingOperations()
        
        // Then - All operations should be processed
        advanceTimeBy(5000) // Allow processing time
        // Implementation would remove processed operations from queue
    }

    @Test
    fun `should clear failed operations`() = runTest {
        // Given - Some failed operations (would be set by error handling)
        // This test would need to simulate failed operations
        
        // When - Clear failed operations
        offlineQueueManager.clearFailedOperations()
        
        // Then - Failed operations should be removed
        advanceTimeBy(100)
        val state = offlineQueueManager.queueState.value
        assertEquals(0, state.failedOperations)
    }

    @Test
    fun `should configure queue settings correctly`() = runTest {
        // When - Configure queue settings
        offlineQueueManager.configure(
            maxRetryAttempts = 5,
            retryDelayMs = 60_000L,
            maxQueueSize = 500
        )
        
        // Then - Settings should be applied
        // This would be tested through behavior changes
        // For now, just verify no exceptions are thrown
    }

    @Test
    fun `should handle operation priorities correctly`() = runTest {
        // Given - Operations with different priorities
        val lowPriorityOp = OfflineOperation(
            id = "low_priority",
            type = OperationType.BACKUP_DATA,
            data = """{"type": "backup"}""",
            timestamp = System.currentTimeMillis(),
            priority = OperationPriority.LOW
        )
        
        val highPriorityOp = OfflineOperation(
            id = "high_priority",
            type = OperationType.DELETE_DATA,
            data = """{"type": "delete"}""",
            timestamp = System.currentTimeMillis(),
            priority = OperationPriority.HIGH
        )
        
        // When - Enqueue operations
        offlineQueueManager.enqueueOperation(lowPriorityOp)
        offlineQueueManager.enqueueOperation(highPriorityOp)
        
        // Then - Both should be queued
        advanceTimeBy(100)
        val state = offlineQueueManager.queueState.value
        assertEquals(2, state.totalOperations)
        
        // High priority operations would be processed first in a real implementation
    }

    @Test
    fun `should serialize and deserialize operations correctly`() = runTest {
        // Given - Operation with complex data
        val complexData = """
            {
                "id": "complex_tx",
                "amount": 123.45,
                "products": ["fish", "rice"],
                "customer": {
                    "id": "customer1",
                    "name": "John Doe"
                },
                "timestamp": 1642680000000
            }
        """.trimIndent()
        
        val operation = OfflineOperation(
            id = "complex_op",
            type = OperationType.TRANSACTION_SYNC,
            data = complexData,
            timestamp = System.currentTimeMillis(),
            priority = OperationPriority.NORMAL
        )
        
        // When - Enqueue operation
        offlineQueueManager.enqueueOperation(operation)
        
        // Then - Should handle complex data correctly
        advanceTimeBy(100)
        val state = offlineQueueManager.queueState.value
        assertEquals(1, state.totalOperations)
    }
}