package com.voiceledger.ghana.offline

import android.content.Context
import androidx.work.*
import com.voiceledger.ghana.data.local.dao.OfflineOperationDao
import com.voiceledger.ghana.data.local.entity.toEntity
import com.voiceledger.ghana.data.local.entity.toOfflineOperation
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Manages offline operations queue for when network is unavailable
 * Handles queuing, persistence, and retry logic for offline operations
 */
@Singleton
class OfflineQueueManager(
    @ApplicationContext private val context: Context,
    private val operationDao: OfflineOperationDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    
    private val _queueState = MutableStateFlow(OfflineQueueState())
    val queueState: StateFlow<OfflineQueueState> = _queueState.asStateFlow()
    
    // In-memory queue for fast access
    private val pendingOperations = ConcurrentHashMap<String, OfflineOperation>()
    private val retryAttempts = ConcurrentHashMap<String, Int>()
    
    // Configuration
    private var maxRetryAttempts = 3
    private var retryDelayMs = 30_000L // 30 seconds
    private var maxQueueSize = 1000
    
    companion object {
        private const val SYNC_WORK_TAG = "offline_sync"
        private const val RETRY_WORK_TAG = "offline_retry"
    }
    
    init {
        loadPersistedOperations()
        schedulePeriodicSync()
    }
    
    /**
     * Add operation to offline queue
     */
    suspend fun enqueueOperation(operation: OfflineOperation) {
        if (pendingOperations.size >= maxQueueSize) {
            // Remove oldest operations if queue is full
            val oldestKey = pendingOperations.keys.minByOrNull { 
                pendingOperations[it]?.timestamp ?: Long.MAX_VALUE 
            }
            oldestKey?.let { pendingOperations.remove(it) }
        }
        
        pendingOperations[operation.id] = operation
        persistOperation(operation)
        updateQueueState()
        
        // Try immediate sync if network is available
        if (NetworkUtils.isNetworkAvailable(context)) {
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
        // Implementation would sync transaction data to cloud
        // For now, simulate success
        delay(1000) // Simulate network call
        return true
    }
    
    /**
     * Process summary sync operation
     */
    private suspend fun processSummarySync(operation: OfflineOperation): Boolean {
        // Implementation would sync summary data to cloud
        delay(800)
        return true
    }
    
    /**
     * Process speaker profile sync operation
     */
    private suspend fun processSpeakerProfileSync(operation: OfflineOperation): Boolean {
        // Implementation would sync speaker profile data
        delay(1200)
        return true
    }
    
    /**
     * Process backup data operation
     */
    private suspend fun processBackupData(operation: OfflineOperation): Boolean {
        // Implementation would backup data to cloud storage
        delay(2000)
        return true
    }
    
    /**
     * Process delete data operation
     */
    private suspend fun processDeleteData(operation: OfflineOperation): Boolean {
        // Implementation would delete data from cloud
        delay(500)
        return true
    }
    
    /**
     * Handle operation error and retry logic
     */
    private suspend fun handleOperationError(operation: OfflineOperation, error: Exception) {
        val currentAttempts = retryAttempts.getOrDefault(operation.id, 0)
        
        if (currentAttempts < maxRetryAttempts) {
            val newRetryCount = currentAttempts + 1
            retryAttempts[operation.id] = newRetryCount
            
            val updatedOperation = operation.copy(
                status = OperationStatus.PENDING,
                lastAttempt = System.currentTimeMillis()
            )
            pendingOperations[operation.id] = updatedOperation
            persistOperation(updatedOperation)
            
            scheduleRetry(operation, newRetryCount)
            updateQueueState()
        } else {
            markOperationFailed(operation, error)
        }
    }
    
    /**
     * Schedule retry for failed operation
     */
    private fun scheduleRetry(operation: OfflineOperation, attemptNumber: Int) {
        val delay = retryDelayMs * attemptNumber // Exponential backoff
        
        val retryWork = OneTimeWorkRequestBuilder<OfflineRetryWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("operation_id" to operation.id))
            .addTag(RETRY_WORK_TAG)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        
        WorkManager.getInstance(context).enqueue(retryWork)
    }
    
    /**
     * Mark operation as failed
     */
    private suspend fun markOperationFailed(operation: OfflineOperation, error: Exception) {
        val failedOperation = operation.copy(
            status = OperationStatus.FAILED,
            errorMessage = error.message,
            lastAttempt = System.currentTimeMillis()
        )
        
        pendingOperations[operation.id] = failedOperation
        retryAttempts.remove(operation.id)
        persistOperation(failedOperation)
        updateQueueState()
    }
    
    /**
     * Mark operation as completed
     */
    private suspend fun markOperationCompleted(operation: OfflineOperation) {
        pendingOperations.remove(operation.id)
        retryAttempts.remove(operation.id)
        removePersistedOperation(operation.id)
        updateQueueState()
    }
    
    /**
     * Process all pending operations
     */
    suspend fun processAllPendingOperations() {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            return
        }
        
        val operations = pendingOperations.values.filter { 
            it.status == OperationStatus.PENDING 
        }.sortedBy { it.timestamp }
        
        operations.forEach { operation ->
            if (processOperation(operation)) {
                markOperationCompleted(operation)
            }
        }
    }
    
    /**
     * Clear all failed operations
     */
    suspend fun clearFailedOperations() {
        val failedOperations = pendingOperations.values.filter { 
            it.status == OperationStatus.FAILED 
        }
        
        failedOperations.forEach { operation ->
            pendingOperations.remove(operation.id)
            retryAttempts.remove(operation.id)
            removePersistedOperation(operation.id)
        }
        
        updateQueueState()
    }
    
    /**
     * Get operations by type
     */
    fun getOperationsByType(type: OperationType): List<OfflineOperation> {
        return pendingOperations.values.filter { it.type == type }
    }
    
    /**
     * Get failed operations
     */
    fun getFailedOperations(): List<OfflineOperation> {
        return pendingOperations.values.filter { it.status == OperationStatus.FAILED }
    }
    
    /**
     * Update queue state
     */
    private fun updateQueueState() {
        val operations = pendingOperations.values.toList()
        val pending = operations.count { it.status == OperationStatus.PENDING }
        val failed = operations.count { it.status == OperationStatus.FAILED }
        val processing = operations.count { it.status == OperationStatus.PROCESSING }
        
        _queueState.value = OfflineQueueState(
            totalOperations = operations.size,
            pendingOperations = pending,
            failedOperations = failed,
            processingOperations = processing,
            lastSyncAttempt = System.currentTimeMillis(),
            isNetworkAvailable = NetworkUtils.isNetworkAvailable(context)
        )
    }
    
    /**
     * Load persisted operations from database
     */
    private fun loadPersistedOperations() {
        scope.launch {
            try {
                val persistedOperations = operationDao.getAllOperations()
                persistedOperations.forEach { entity ->
                    val operation = entity.toOfflineOperation()
                    pendingOperations[operation.id] = operation
                    retryAttempts[operation.id] = entity.retryCount
                }
                updateQueueState()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Persist operation to database
     */
    private suspend fun persistOperation(operation: OfflineOperation) {
        withContext(ioDispatcher) {
            val retryCount = retryAttempts[operation.id] ?: 0
            operationDao.insertOrReplace(operation.toEntity(retryCount))
        }
    }
    
    /**
     * Remove persisted operation from database
     */
    private suspend fun removePersistedOperation(operationId: String) {
        withContext(ioDispatcher) {
            operationDao.deleteOperationById(operationId)
        }
    }
    
    /**
     * Schedule periodic sync
     */
    private fun schedulePeriodicSync() {
        val syncWork = PeriodicWorkRequestBuilder<OfflineSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .addTag(SYNC_WORK_TAG)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "offline_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncWork
        )
    }
    
    /**
     * Configure queue settings
     */
    fun configure(
        maxRetryAttempts: Int = this.maxRetryAttempts,
        retryDelayMs: Long = this.retryDelayMs,
        maxQueueSize: Int = this.maxQueueSize
    ) {
        this.maxRetryAttempts = maxRetryAttempts.coerceIn(1, 10)
        this.retryDelayMs = retryDelayMs.coerceIn(5_000L, 300_000L)
        this.maxQueueSize = maxQueueSize.coerceIn(100, 5000)
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        scope.cancel()
        WorkManager.getInstance(context).cancelAllWorkByTag(SYNC_WORK_TAG)
        WorkManager.getInstance(context).cancelAllWorkByTag(RETRY_WORK_TAG)
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
    val lastAttempt: Long? = null
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
 * Offline queue state
 */
data class OfflineQueueState(
    val totalOperations: Int = 0,
    val pendingOperations: Int = 0,
    val failedOperations: Int = 0,
    val processingOperations: Int = 0,
    val lastSyncAttempt: Long = 0L,
    val isNetworkAvailable: Boolean = false
)

/**
 * Worker for periodic offline sync
 */
class OfflineSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            // Get OfflineQueueManager instance and process operations
            // This would be injected in a real implementation
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

/**
 * Worker for retrying failed operations
 */
class OfflineRetryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val operationId = inputData.getString("operation_id") ?: return Result.failure()
            // Retry the specific operation
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}