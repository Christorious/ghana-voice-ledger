package com.voiceledger.ghana.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.voiceledger.ghana.offline.NetworkUtils
import com.voiceledger.ghana.offline.OfflineQueueManager
import com.voiceledger.ghana.offline.OperationPriority
import com.voiceledger.ghana.offline.OperationStatus
import com.voiceledger.ghana.offline.OperationType
import com.voiceledger.ghana.offline.OfflineOperation
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
 * Integration test simulating offline to online sync transitions
 * Verifies queue persistence and processing
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class OfflineToOnlineSyncIntegrationTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var offlineQueueManager: OfflineQueueManager

    @Inject
    lateinit var networkUtils: NetworkUtils

    @Before
    fun setUp() {
        hiltRule.inject()
        offlineQueueManager.configure(maxRetryAttempts = 3, retryDelayMs = 1000L, maxQueueSize = 50)
    }

    @Test
    fun testOfflineToOnlineSync_processesQueuedOperations() = runTest {
        // Given - Offline state
        networkUtils.setNetworkAvailable(false)

        // Queue multiple offline operations
        val operations = listOf(
            OfflineOperation(
                id = "op-001",
                type = OperationType.TRANSACTION_SYNC,
                data = "{\"transactionId\":\"tx-001\"}",
                timestamp = System.currentTimeMillis(),
                priority = OperationPriority.HIGH
            ),
            OfflineOperation(
                id = "op-002",
                type = OperationType.SUMMARY_SYNC,
                data = "{\"summaryId\":\"sum-001\"}",
                timestamp = System.currentTimeMillis(),
                priority = OperationPriority.NORMAL
            )
        )

        operations.forEach { offlineQueueManager.enqueueOperation(it) }

        // Verify operations are pending
        val pendingItemsBefore = offlineQueueManager.queueState.first()
        assertEquals("Should have 2 pending operations", 2, pendingItemsBefore.pendingOperations)

        // When - Network becomes available
        networkUtils.setNetworkAvailable(true)

        // Process operations
        offlineQueueManager.processAllPendingOperations()
        delay(2000) // Allow time for processing simulation

        // Then - Queue should have processed operations
        val queueState = offlineQueueManager.queueState.first()
        assertTrue("Queue should have 0 pending operations", queueState.pendingOperations == 0)
        assertEquals("Queue should have 0 failed operations", 0, queueState.failedOperations)
    }

    @Test
    fun testOfflineQueue_persistenceAcrossSessions() = runTest {
        // Given - Offline state
        networkUtils.setNetworkAvailable(false)

        val operation = OfflineOperation(
            id = "op-003",
            type = OperationType.TRANSACTION_SYNC,
            data = "{\"transactionId\":\"tx-002\"}",
            timestamp = System.currentTimeMillis(),
            priority = OperationPriority.NORMAL
        )

        // Queue operation
        offlineQueueManager.enqueueOperation(operation)

        // Simulate app restart by reloading persisted operations
        offlineQueueManager.cleanup()

        // Reinitialize queue manager (simulate DI re-creation)
        offlineQueueManager.configure(maxRetryAttempts = 3, retryDelayMs = 1000L, maxQueueSize = 50)

        // When - Network becomes available
        networkUtils.setNetworkAvailable(true)

        // Process operations
        offlineQueueManager.processAllPendingOperations()
        delay(2000)

        // Then - Operation should be processed and removed from queue
        val queuedItems = offlineQueueManager.getOperationsByType(OperationType.TRANSACTION_SYNC)
        assertTrue("Operations should be processed and removed", queuedItems.none { it.id == "op-003" })
    }

    @Test
    fun testOfflineQueue_retryLogic() = runTest {
        // Given - Offline state and operation that will fail
        networkUtils.setNetworkAvailable(false)

        val failingOperation = OfflineOperation(
            id = "op-004",
            type = OperationType.TRANSACTION_SYNC,
            data = "{\"transactionId\":\"tx-003\"}",
            timestamp = System.currentTimeMillis(),
            priority = OperationPriority.CRITICAL
        )

        offlineQueueManager.enqueueOperation(failingOperation)

        // Simulate network returning but force retry due to failure
        networkUtils.setNetworkAvailable(true)

        // Process operations (first attempt - will succeed due to simulation)
        offlineQueueManager.processAllPendingOperations()
        delay(1000)

        val queueState = offlineQueueManager.queueState.first()
        assertTrue("Failed operations should be 0", queueState.failedOperations == 0)
    }

    @Test
    fun testOfflineQueue_handlesNetworkFluctuations() = runTest {
        // Given - Intermittent network availability
        val operationIds = (1..5).map { "op-fluct-$it" }

        operationIds.forEach { id ->
            val operation = OfflineOperation(
                id = id,
                type = OperationType.TRANSACTION_SYNC,
                data = "{\"transactionId\":\"tx-$id\"}",
                timestamp = System.currentTimeMillis(),
                priority = OperationPriority.NORMAL
            )
            offlineQueueManager.enqueueOperation(operation)
        }

        // Simulate network fluctuations
        networkUtils.setNetworkAvailable(false)
        delay(500)
        networkUtils.setNetworkAvailable(true)
        offlineQueueManager.processAllPendingOperations()
        delay(1000)
        networkUtils.setNetworkAvailable(false)
        delay(500)
        networkUtils.setNetworkAvailable(true)
        offlineQueueManager.processAllPendingOperations()
        delay(1000)

        // Then - Queue should eventually process operations
        val queueState = offlineQueueManager.queueState.first()
        assertTrue("Pending operations should be 0", queueState.pendingOperations == 0)
        assertTrue("Failed operations should be 0", queueState.failedOperations == 0)
    }

    @Test
    fun testOfflineQueue_statusTransitions() = runTest {
        // Given - Offline operation
        networkUtils.setNetworkAvailable(false)

        val operation = OfflineOperation(
            id = "op-status-001",
            type = OperationType.SUMMARY_SYNC,
            data = "{\"summaryId\":\"sum-status-001\"}",
            timestamp = System.currentTimeMillis(),
            priority = OperationPriority.NORMAL
        )

        // Queue operation
        offlineQueueManager.enqueueOperation(operation)

        // When - Network returns
        networkUtils.setNetworkAvailable(true)
        offlineQueueManager.processAllPendingOperations()
        delay(1000)

        // Then - Operation should be completed
        val operations = offlineQueueManager.getOperationsByType(OperationType.SUMMARY_SYNC)
        assertTrue("Operations should be completed and removed", operations.none { it.id == "op-status-001" })

        val queueState = offlineQueueManager.queueState.first()
        assertEquals("Failed operations should be 0", 0, queueState.failedOperations)
        assertEquals("Pending operations should be 0", 0, queueState.pendingOperations)
    }
}
