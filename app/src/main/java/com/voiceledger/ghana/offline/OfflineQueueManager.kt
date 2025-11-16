package com.voiceledger.ghana.offline

import android.content.Context
import androidx.work.*
import com.voiceledger.ghana.data.local.dao.OfflineOperationDao
import com.voiceledger.ghana.data.local.entity.OfflineOperationEntity
import com.voiceledger.ghana.data.local.entity.toDomain
import com.voiceledger.ghana.data.local.entity.toEntity
import com.voiceledger.ghana.data.repository.TransactionRepository
import com.voiceledger.ghana.data.repository.DailySummaryRepository
import com.voiceledger.ghana.domain.service.NetworkMonitorService
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for offline operations queue
 * Handles durable storage and retry logic for offline operations
 */
@Singleton
class OfflineQueueManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val offlineOperationDao: OfflineOperationDao,
    private val transactionRepository: TransactionRepository,
    private val dailySummaryRepository: DailySummaryRepository,
    private val networkMonitorService: NetworkMonitorService,
    private val gson: Gson,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    
    companion object {
        private const val SYNC_WORK_NAME = "OfflineQueueSyncWork"
        private const val RETRY_DELAY_MINUTES = 15L
        private const val MAX_AGE_DAYS = 30L
        private const val MAX_RETRY_ATTEMPTS = 3
    }
    
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    
    private val _queueState = MutableStateFlow(OfflineQueueState())
    val queueState: StateFlow<OfflineQueueState> = _queueState.asStateFlow()
    
    // In-memory cache for fast access
    private val pendingOperations = ConcurrentHashMap<String, OfflineOperation>()
    
    init {
        loadPersistedOperations()
        schedulePeriodicSync()
    }
    
    /**
     * Queue a transaction creation operation
     */
    suspend fun queueTransactionCreate(transactionData: String) {
        val operation = OfflineOperation(
            id = generateOperationId(),
            type = OperationType.TRANSACTION_SYNC,
            data = transactionData,
            timestamp = System.currentTimeMillis(),
            priority = OperationPriority.HIGH
        )
        
        addOperation(operation)
    }
    
    /**
     * Queue a transaction update operation
     */
    suspend fun queueTransactionUpdate(transactionId: String, transactionData: String) {
        val operation = OfflineOperation(
            id = generateOperationId(),
            type = OperationType.TRANSACTION_SYNC,
            data = transactionData,
            timestamp = System.currentTimeMillis(),
            priority = OperationPriority.NORMAL
        )
        
        addOperation(operation)
    }
    
    /**
     * Queue a daily summary operation
     */
    suspend fun queueDailySummaryCreate(summaryData: String) {
        val operation = OfflineOperation(
            id = generateOperationId(),
            type = OperationType.SUMMARY_SYNC,
            data = summaryData,
            timestamp = System.currentTimeMillis(),
            priority = OperationPriority.LOW
        )
        
        addOperation(operation)
    }
    
    /**
     * Add operation to queue and persist
     */
    private suspend fun addOperation(operation: OfflineOperation) {
        pendingOperations[operation.id] = operation
        persistOperation(operation)
        updateQueueState()
        
        // Try immediate sync if network is available
        if (NetworkUtils.isNetworkAvailable(context)) {
            processOperation(operation)
        }
    }
    
    /**
     * Process all pending operations
     */
    suspend fun processAllPendingOperations() {
        if (!NetworkUtils.isNetworkAvailable(context)) return
        
        val operations = pendingOperations.values.filter { 
            it.status == OperationStatus.PENDING || 
            (it.status == OperationStatus.FAILED && it.retryCount < MAX_RETRY_ATTEMPTS)
        }
        
        for (operation in operations) {
            processOperation(operation)
        }
    }
    
    /**
     * Process a single operation
     */
    private suspend fun processOperation(operation: OfflineOperation): Boolean {
        val processingOperation = operation.copy(
            status = OperationStatus.PROCESSING,
            lastAttempt = System.currentTimeMillis()
        )
        pendingOperations[operation.id] = processingOperation
        persistOperation(processingOperation)
        updateQueueState()
        
        return try {
            val result = when (operation.type) {
                OperationType.TRANSACTION_SYNC -> processTransactionSync(processingOperation)
                OperationType.SUMMARY_SYNC -> processSummarySync(processingOperation)
                OperationType.SPEAKER_PROFILE_SYNC -> processSpeakerProfileSync(processingOperation)
                OperationType.BACKUP_DATA -> processBackupData(processingOperation)
                OperationType.DELETE_DATA -> processDeleteData(processingOperation)
            }
            
            if (result) {
                markOperationCompleted(processingOperation)
            } else {
                handleOperationError(processingOperation, Exception("Operation failed"))
            }
            result
        } catch (e: Exception) {
            handleOperationError(processingOperation, e)
            false
        }
    }
    
    /**
     * Process transaction sync operation
     */
    private suspend fun processTransactionSync(operation: OfflineOperation): Boolean {
        return withContext(ioDispatcher) {
            try {
                // Simulate network call - replace with actual implementation
                delay(1000)
                true // Success
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Process summary sync operation
     */
    private suspend fun processSummarySync(operation: OfflineOperation): Boolean {
        return withContext(ioDispatcher) {
            try {
                // Simulate network call - replace with actual implementation
                delay(800)
                true // Success
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Process speaker profile sync operation
     */
    private suspend fun processSpeakerProfileSync(operation: OfflineOperation): Boolean {
        return withContext(ioDispatcher) {
            try {
                // Simulate network call - replace with actual implementation
                delay(1200)
                true // Success
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Process backup data operation
     */
    private suspend fun processBackupData(operation: OfflineOperation): Boolean {
        return withContext(ioDispatcher) {
            try {
                // Simulate backup - replace with actual implementation
                delay(2000)
                true // Success
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Process delete data operation
     */
    private suspend fun processDeleteData(operation: OfflineOperation): Boolean {
        return withContext(ioDispatcher) {
            try {
                // Simulate delete - replace with actual implementation
                delay(500)
                true // Success
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Mark operation as completed
     */
    private suspend fun markOperationCompleted(operation: OfflineOperation) {
        val completedOperation = operation.copy(
            status = OperationStatus.COMPLETED
        )
        pendingOperations[operation.id] = completedOperation
        persistOperation(completedOperation)
        
        // Remove from pending queue after a delay
        scope.launch {
            delay(5000) // Keep for 5 seconds for UI updates
            pendingOperations.remove(operation.id)
            removePersistedOperation(operation.id)
            updateQueueState()
        }
    }
    
    /**
     * Handle operation error
     */
    private suspend fun handleOperationError(operation: OfflineOperation, error: Exception) {
        val retryCount = operation.retryCount + 1
        
        if (retryCount < MAX_RETRY_ATTEMPTS) {
            val failedOperation = operation.copy(
                status = OperationStatus.FAILED,
                errorMessage = error.message,
                lastAttempt = System.currentTimeMillis(),
                retryCount = retryCount
            )
            pendingOperations[operation.id] = failedOperation
            persistOperation(failedOperation)
            
            // Schedule retry with exponential backoff
            scheduleRetry(failedOperation, retryCount)
        } else {
            // Max retries reached, mark as permanently failed
            val permanentlyFailedOperation = operation.copy(
                status = OperationStatus.FAILED,
                errorMessage = "Max retries reached: ${error.message}",
                retryCount = retryCount
            )
            pendingOperations[operation.id] = permanentlyFailedOperation
            persistOperation(permanentlyFailedOperation)
        }
        
        updateQueueState()
    }
    
    /**
     * Schedule retry for failed operation
     */
    private fun scheduleRetry(operation: OfflineOperation, retryCount: Int) {
        val delayMs = RETRY_DELAY_MINUTES * 60_000L * retryCount // Exponential backoff
        
        scope.launch {
            delay(delayMs)
            if (pendingOperations.containsKey(operation.id)) {
                processOperation(operation)
            }
        }
    }
    
    /**
     * Get queue statistics
     */
    suspend fun getQueueStatistics(): QueueStatistics {
        val operations = pendingOperations.values.toList()
        
        return QueueStatistics(
            pendingCount = operations.count { it.status == OperationStatus.PENDING },
            processingCount = operations.count { it.status == OperationStatus.PROCESSING },
            failedCount = operations.count { it.status == OperationStatus.FAILED },
            completedCount = operations.count { it.status == OperationStatus.COMPLETED },
            totalOperations = operations.size,
            oldestOperation = operations.minOfOrNull { it.timestamp }
        )
    }
    
    /**
     * Retry failed operations
     */
    suspend fun retryFailedOperations() {
        val failedOperations = pendingOperations.values.filter { 
            it.status == OperationStatus.FAILED && it.retryCount < MAX_RETRY_ATTEMPTS 
        }
        
        for (operation in failedOperations) {
            val resetOperation = operation.copy(
                status = OperationStatus.PENDING,
                errorMessage = null,
                retryCount = 0
            )
            pendingOperations[operation.id] = resetOperation
            persistOperation(resetOperation)
        }
        
        updateQueueState()
        
        // Process if network is available
        if (NetworkUtils.isNetworkAvailable(context)) {
            processAllPendingOperations()
        }
    }
    
    /**
     * Clean up old operations
     */
    suspend fun cleanupOldOperations() {
        val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(MAX_AGE_DAYS)
        
        // Remove old completed operations
        val oldOperations = pendingOperations.values.filter { 
            it.timestamp < cutoffTime && it.status == OperationStatus.COMPLETED 
        }
        
        for (operation in oldOperations) {
            pendingOperations.remove(operation.id)
            removePersistedOperation(operation.id)
        }
        
        // Also clean up database
        withContext(ioDispatcher) {
            offlineOperationDao.deleteOldOperations(cutoffTime)
        }
        
        updateQueueState()
    }
    
    /**
     * Load persisted operations on startup
     */
    private fun loadPersistedOperations() {
        scope.launch {
            try {
                val persisted = withContext(ioDispatcher) {
                    offlineOperationDao.getAllOperationsSync()
                }
                
                persisted.forEach { entity ->
                    val operation = entity.toDomain()
                    pendingOperations[operation.id] = operation
                }
                
                updateQueueState()
            } catch (e: Exception) {
                // Handle error loading operations
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Persist operation to database
     */
    private suspend fun persistOperation(operation: OfflineOperation) {
        withContext(ioDispatcher) {
            offlineOperationDao.upsertOperation(operation.toEntity())
        }
    }
    
    /**
     * Remove operation from database
     */
    private suspend fun removePersistedOperation(operationId: String) {
        withContext(ioDispatcher) {
            offlineOperationDao.deleteOperation(operationId)
        }
    }
    
    /**
     * Update queue state
     */
    private fun updateQueueState() {
        val operations = pendingOperations.values.toList()
        
        _queueState.value = OfflineQueueState(
            pendingOperations = operations.count { it.status == OperationStatus.PENDING },
            processingOperations = operations.count { it.status == OperationStatus.PROCESSING },
            failedOperations = operations.count { it.status == OperationStatus.FAILED },
            completedOperations = operations.count { it.status == OperationStatus.COMPLETED },
            lastSyncTime = System.currentTimeMillis()
        )
    }
    
    /**
     * Schedule periodic sync work
     */
    private fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        
        val syncRequest = PeriodicWorkRequestBuilder<OfflineSyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                RETRY_DELAY_MINUTES,
                TimeUnit.MINUTES
            )
            .addTag(SYNC_WORK_NAME)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
    
    /**
     * Generate unique operation ID
     */
    private fun generateOperationId(): String {
        return "offline_op_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    /**
     * Get all pending items as Flow
     */
    fun getPendingItems(): Flow<List<OfflineOperation>> {
        return kotlinx.coroutines.flow.flow {
            emit(pendingOperations.values.toList())
        }
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        scope.cancel()
        WorkManager.getInstance(context).cancelAllWorkByTag(SYNC_WORK_NAME)
    }
}

/**
 * Offline operation data class
 */
@Serializable
data class OfflineOperation(
    val id: String,
    val type: OperationType,
    val data: String, // JSON serialized data
    val timestamp: Long,
    val priority: OperationPriority = OperationPriority.NORMAL,
    val status: OperationStatus = OperationStatus.PENDING,
    val errorMessage: String? = null,
    val lastAttempt: Long? = null,
    val retryCount: Int = 0
)

/**
 * Operation types
 */
@Serializable
enum class OperationType {
    TRANSACTION_SYNC,
    SUMMARY_SYNC,
    SPEAKER_PROFILE_SYNC,
    BACKUP_DATA,
    DELETE_DATA
}

/**
 * Operation priority levels
 */
@Serializable
enum class OperationPriority {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL
}

/**
 * Operation status
 */
@Serializable
enum class OperationStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}

/**
 * Queue state
 */
data class OfflineQueueState(
    val pendingOperations: Int = 0,
    val processingOperations: Int = 0,
    val failedOperations: Int = 0,
    val completedOperations: Int = 0,
    val lastSyncTime: Long = 0L
)

/**
 * Queue statistics
 */
data class QueueStatistics(
    val pendingCount: Int,
    val processingCount: Int,
    val failedCount: Int,
    val completedCount: Int,
    val totalOperations: Int,
    val oldestOperation: Long?
)