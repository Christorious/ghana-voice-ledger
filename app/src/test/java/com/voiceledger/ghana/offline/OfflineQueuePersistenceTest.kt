package com.voiceledger.ghana.offline

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class OfflineQueuePersistenceTest {

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
        
        context = ApplicationProvider.getApplicationContext()
        
        database = Room.inMemoryDatabaseBuilder(
            context,
            VoiceLedgerDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        
        workManager = mockk(relaxed = true)
        
        mockkObject(NetworkUtils)
        every { NetworkUtils.isNetworkAvailable(any()) } returns false
        
        mockkStatic(WorkManager::class)
        every { WorkManager.getInstance(any()) } returns workManager
        
        offlineQueueManager = OfflineQueueManager(
            context = context,
            operationDao = database.offlineOperationDao(),
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        offlineQueueManager.cleanup()
        database.close()
        unmockkAll()
    }

    @Test
    fun `should persist operation to database when enqueued`() = runTest {
        val operation = OfflineOperation(
            id = "persist_test_1",
            type = OperationType.TRANSACTION_SYNC,
            data = """{"id": "tx1", "amount": 100.0}""",
            timestamp = System.currentTimeMillis()
        )
        
        offlineQueueManager.enqueueOperation(operation)
        advanceUntilIdle()
        
        val persistedOperations = database.offlineOperationDao().getAllOperations()
        assertEquals(1, persistedOperations.size)
        assertEquals(operation.id, persistedOperations[0].id)
        assertEquals(operation.type.name, persistedOperations[0].type)
        assertEquals(operation.data, persistedOperations[0].data)
    }

    @Test
    fun `should restore operations after manager restart`() = runTest {
        val operations = listOf(
            OfflineOperation(
                id = "restore_test_1",
                type = OperationType.TRANSACTION_SYNC,
                data = """{"id": "tx1", "amount": 100.0}""",
                timestamp = System.currentTimeMillis()
            ),
            OfflineOperation(
                id = "restore_test_2",
                type = OperationType.SUMMARY_SYNC,
                data = """{"date": "2024-01-15", "total": 500.0}""",
                timestamp = System.currentTimeMillis() + 1000
            ),
            OfflineOperation(
                id = "restore_test_3",
                type = OperationType.SPEAKER_PROFILE_SYNC,
                data = """{"speakerId": "speaker1"}""",
                timestamp = System.currentTimeMillis() + 2000,
                priority = OperationPriority.HIGH
            )
        )
        
        operations.forEach { operation ->
            offlineQueueManager.enqueueOperation(operation)
        }
        advanceUntilIdle()
        
        val firstState = offlineQueueManager.queueState.value
        assertEquals(3, firstState.totalOperations)
        
        offlineQueueManager.cleanup()
        
        val newManager = OfflineQueueManager(
            context = context,
            operationDao = database.offlineOperationDao(),
            ioDispatcher = testDispatcher
        )
        advanceUntilIdle()
        
        val restoredState = newManager.queueState.value
        assertEquals(3, restoredState.totalOperations)
        assertEquals(3, restoredState.pendingOperations)
        
        newManager.cleanup()
    }

    @Test
    fun `should persist retry count across restarts`() = runTest {
        val operation = OfflineOperation(
            id = "retry_test_1",
            type = OperationType.TRANSACTION_SYNC,
            data = """{"id": "tx1", "amount": 100.0}""",
            timestamp = System.currentTimeMillis()
        )
        
        offlineQueueManager.enqueueOperation(operation)
        advanceUntilIdle()
        
        val dao = database.offlineOperationDao()
        dao.updateOperationStatus(
            id = operation.id,
            status = OperationStatus.PENDING.name,
            errorMessage = "Network error",
            lastAttempt = System.currentTimeMillis(),
            retryCount = 2
        )
        
        offlineQueueManager.cleanup()
        
        val newManager = OfflineQueueManager(
            context = context,
            operationDao = database.offlineOperationDao(),
            ioDispatcher = testDispatcher
        )
        advanceUntilIdle()
        
        val persistedOperations = dao.getAllOperations()
        assertEquals(1, persistedOperations.size)
        assertEquals(2, persistedOperations[0].retryCount)
        
        newManager.cleanup()
    }

    @Test
    fun `should update operation status in database`() = runTest {
        val operation = OfflineOperation(
            id = "status_test_1",
            type = OperationType.TRANSACTION_SYNC,
            data = """{"id": "tx1", "amount": 100.0}""",
            timestamp = System.currentTimeMillis()
        )
        
        offlineQueueManager.enqueueOperation(operation)
        advanceUntilIdle()
        
        val dao = database.offlineOperationDao()
        var persistedOperations = dao.getAllOperations()
        assertEquals(OperationStatus.PENDING.name, persistedOperations[0].status)
        
        dao.updateOperationStatus(
            id = operation.id,
            status = OperationStatus.PROCESSING.name,
            errorMessage = null,
            lastAttempt = System.currentTimeMillis(),
            retryCount = 0
        )
        
        persistedOperations = dao.getAllOperations()
        assertEquals(OperationStatus.PROCESSING.name, persistedOperations[0].status)
        
        dao.updateOperationStatus(
            id = operation.id,
            status = OperationStatus.FAILED.name,
            errorMessage = "Test error",
            lastAttempt = System.currentTimeMillis(),
            retryCount = 3
        )
        
        persistedOperations = dao.getAllOperations()
        assertEquals(OperationStatus.FAILED.name, persistedOperations[0].status)
        assertEquals("Test error", persistedOperations[0].errorMessage)
        assertEquals(3, persistedOperations[0].retryCount)
    }

    @Test
    fun `should remove completed operations from database`() = runTest {
        val operations = listOf(
            OfflineOperation(
                id = "remove_test_1",
                type = OperationType.TRANSACTION_SYNC,
                data = """{"id": "tx1"}""",
                timestamp = System.currentTimeMillis()
            ),
            OfflineOperation(
                id = "remove_test_2",
                type = OperationType.SUMMARY_SYNC,
                data = """{"date": "2024-01-15"}""",
                timestamp = System.currentTimeMillis() + 1000
            )
        )
        
        operations.forEach { operation ->
            offlineQueueManager.enqueueOperation(operation)
        }
        advanceUntilIdle()
        
        val dao = database.offlineOperationDao()
        assertEquals(2, dao.getAllOperations().size)
        
        dao.deleteOperationById(operations[0].id)
        
        val remaining = dao.getAllOperations()
        assertEquals(1, remaining.size)
        assertEquals(operations[1].id, remaining[0].id)
    }

    @Test
    fun `should query operations by status`() = runTest {
        val operations = listOf(
            OfflineOperation(
                id = "query_test_1",
                type = OperationType.TRANSACTION_SYNC,
                data = """{"id": "tx1"}""",
                timestamp = System.currentTimeMillis(),
                status = OperationStatus.PENDING
            ),
            OfflineOperation(
                id = "query_test_2",
                type = OperationType.SUMMARY_SYNC,
                data = """{"date": "2024-01-15"}""",
                timestamp = System.currentTimeMillis() + 1000,
                status = OperationStatus.PENDING
            ),
            OfflineOperation(
                id = "query_test_3",
                type = OperationType.SPEAKER_PROFILE_SYNC,
                data = """{"speakerId": "speaker1"}""",
                timestamp = System.currentTimeMillis() + 2000,
                status = OperationStatus.FAILED
            )
        )
        
        operations.forEach { operation ->
            offlineQueueManager.enqueueOperation(operation)
        }
        advanceUntilIdle()
        
        val dao = database.offlineOperationDao()
        dao.updateOperationStatus(
            id = "query_test_3",
            status = OperationStatus.FAILED.name,
            errorMessage = "Test failure",
            lastAttempt = System.currentTimeMillis(),
            retryCount = 3
        )
        
        val pendingOps = dao.getPendingOperations()
        assertEquals(2, pendingOps.size)
        assertTrue(pendingOps.all { it.status == OperationStatus.PENDING.name })
        
        val failedOps = dao.getFailedOperations()
        assertEquals(1, failedOps.size)
        assertEquals(OperationStatus.FAILED.name, failedOps[0].status)
    }

    @Test
    fun `should respect operation priority ordering`() = runTest {
        val operations = listOf(
            OfflineOperation(
                id = "priority_test_1",
                type = OperationType.TRANSACTION_SYNC,
                data = """{"id": "tx1"}""",
                timestamp = System.currentTimeMillis(),
                priority = OperationPriority.LOW
            ),
            OfflineOperation(
                id = "priority_test_2",
                type = OperationType.TRANSACTION_SYNC,
                data = """{"id": "tx2"}""",
                timestamp = System.currentTimeMillis() + 1000,
                priority = OperationPriority.CRITICAL
            ),
            OfflineOperation(
                id = "priority_test_3",
                type = OperationType.TRANSACTION_SYNC,
                data = """{"id": "tx3"}""",
                timestamp = System.currentTimeMillis() + 2000,
                priority = OperationPriority.HIGH
            )
        )
        
        operations.forEach { operation ->
            offlineQueueManager.enqueueOperation(operation)
        }
        advanceUntilIdle()
        
        val dao = database.offlineOperationDao()
        val allOps = dao.getPendingOperations()
        
        assertEquals(3, allOps.size)
        assertEquals("priority_test_2", allOps[0].id)
        assertEquals("priority_test_3", allOps[1].id)
        assertEquals("priority_test_1", allOps[2].id)
    }

    @Test
    fun `should handle multiple failed operations correctly`() = runTest {
        val operation = OfflineOperation(
            id = "multi_fail_test_1",
            type = OperationType.TRANSACTION_SYNC,
            data = """{"id": "tx1"}""",
            timestamp = System.currentTimeMillis()
        )
        
        offlineQueueManager.enqueueOperation(operation)
        advanceUntilIdle()
        
        val dao = database.offlineOperationDao()
        
        repeat(3) { attempt ->
            dao.updateOperationStatus(
                id = operation.id,
                status = OperationStatus.PENDING.name,
                errorMessage = "Attempt ${attempt + 1} failed",
                lastAttempt = System.currentTimeMillis(),
                retryCount = attempt + 1
            )
        }
        
        val finalOp = dao.getAllOperations().first()
        assertEquals(3, finalOp.retryCount)
        assertEquals("Attempt 3 failed", finalOp.errorMessage)
    }

    @Test
    fun `should clear failed operations from database`() = runTest {
        val operations = listOf(
            OfflineOperation(
                id = "clear_test_1",
                type = OperationType.TRANSACTION_SYNC,
                data = """{"id": "tx1"}""",
                timestamp = System.currentTimeMillis(),
                status = OperationStatus.FAILED
            ),
            OfflineOperation(
                id = "clear_test_2",
                type = OperationType.SUMMARY_SYNC,
                data = """{"date": "2024-01-15"}""",
                timestamp = System.currentTimeMillis() + 1000,
                status = OperationStatus.PENDING
            )
        )
        
        val dao = database.offlineOperationDao()
        operations.forEach { operation ->
            offlineQueueManager.enqueueOperation(operation)
        }
        advanceUntilIdle()
        
        dao.updateOperationStatus(
            id = "clear_test_1",
            status = OperationStatus.FAILED.name,
            errorMessage = "Failed",
            lastAttempt = System.currentTimeMillis(),
            retryCount = 3
        )
        
        offlineQueueManager.clearFailedOperations()
        advanceUntilIdle()
        
        val remaining = dao.getAllOperations()
        assertEquals(1, remaining.size)
        assertEquals("clear_test_2", remaining[0].id)
    }
}
