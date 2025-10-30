package com.voiceledger.ghana.offline

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.voiceledger.ghana.data.local.dao.OfflineOperationDao
import com.voiceledger.ghana.data.local.entity.toEntity
import com.voiceledger.ghana.data.local.entity.toOfflineOperation
import com.voiceledger.ghana.data.local.entity.OfflineOperationEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap
import com.voiceledger.ghana.data.local.dao.OfflineOperationDao
import com.voiceledger.ghana.data.local.entity.OfflineOperation
import com.voiceledger.ghana.data.repository.TransactionRepository
import com.voiceledger.ghana.data.repository.DailySummaryRepository
import com.voiceledger.ghana.domain.service.NetworkMonitorService
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Manager for offline operations queue
 * Handles durable storage and retry logic for offline operations
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
class OfflineQueueManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val offlineOperationDao: com.voiceledger.ghana.data.local.dao.OfflineOperationDao
    private val offlineOperationDao: OfflineOperationDao,
    private val transactionRepository: TransactionRepository,
    private val dailySummaryRepository: DailySummaryRepository,
    private val networkMonitorService: NetworkMonitorService,
    private val gson: Gson,
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val SYNC_WORK_NAME = "OfflineQueueSyncWork"
        private const val RETRY_DELAY_MINUTES = 15L
        private const val MAX_AGE_DAYS = 30L
    }
    
    /**
     * Queue a transaction creation operation
     */
    suspend fun queueTransactionCreate(transactionData: String) {
        val operation = OfflineOperation(
            id = generateOperationId(),
            operationType = "CREATE",
            entityType = "TRANSACTION",
            entityId = extractTransactionId(transactionData),
            data = transactionData,
            timestamp = System.currentTimeMillis(),
            priority = 1 // High priority for transactions
        )
        
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
        offlineOperationDao.insertOperation(operation)
        scheduleSyncWork()
    }
    
    /**
     * Queue a transaction update operation
     */
    suspend fun queueTransactionUpdate(transactionId: String, transactionData: String) {
        val operation = OfflineOperation(
            id = generateOperationId(),
            operationType = "UPDATE",
            entityType = "TRANSACTION",
            entityId = transactionId,
            data = transactionData,
            timestamp = System.currentTimeMillis(),
            priority = 2
        )
        
        offlineOperationDao.insertOperation(operation)
        scheduleSyncWork()
    }
    
    /**
     * Queue a daily summary operation
     */
    suspend fun queueDailySummaryCreate(summaryData: String) {
        val operation = OfflineOperation(
            id = generateOperationId(),
            operationType = "CREATE",
            entityType = "DAILY_SUMMARY",
            entityId = extractSummaryId(summaryData),
            data = summaryData,
            timestamp = System.currentTimeMillis(),
            priority = 3 // Lower priority for summaries
        )
        
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
        offlineOperationDao.insertOperation(operation)
        scheduleSyncWork()
    }
    
    /**
     * Process all unsynced operations
     */
    suspend fun processUnsyncedOperations(): SyncResult {
        val operations = offlineOperationDao.getUnsyncedOperations()
        val results = mutableListOf<OperationResult>()
        var successCount = 0
        var failureCount = 0
        
        for (operation in operations) {
            try {
                offlineOperationDao.markAsProcessing(operation.id)
                
                val success = when (operation.entityType) {
                    "TRANSACTION" -> processTransactionOperation(operation)
                    "DAILY_SUMMARY" -> processSummaryOperation(operation)
                    else -> false
                }
                
                if (success) {
                    offlineOperationDao.markAsSynced(operation.id)
                    results.add(OperationResult(operation.id, true, null))
                    successCount++
                } else {
                    offlineOperationDao.incrementRetryCount(operation.id, "Sync failed")
                    results.add(OperationResult(operation.id, false, "Sync failed"))
                    failureCount++
                }
                
            } catch (e: Exception) {
                offlineOperationDao.incrementRetryCount(operation.id, e.message)
                results.add(OperationResult(operation.id, false, e.message))
                failureCount++
            }
        }
        
        return SyncResult(
            totalOperations = operations.size,
            successCount = successCount,
            failureCount = failureCount,
            results = results
        )
        
        pendingOperations[operation.id] = failedOperation
        retryAttempts.remove(operation.id)
        persistOperation(failedOperation)
        updateQueueState()
    }
    
    /**
     * Get offline queue statistics
     */
    suspend fun getQueueStatistics(): QueueStatistics {
        return QueueStatistics(
            unsyncedCount = offlineOperationDao.getUnsyncedCount(),
            transactionCount = offlineOperationDao.getUnsyncedCountByType("TRANSACTION"),
            summaryCount = offlineOperationDao.getUnsyncedCountByType("DAILY_SUMMARY"),
            failedCount = offlineOperationDao.getFailedCount(),
            oldestOperation = getOldestOperationTimestamp()
        )
    }
    
    /**
     * Clean up old operations
     */
    suspend fun cleanupOldOperations() {
        val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(MAX_AGE_DAYS)
        
        // Delete old synced operations
        offlineOperationDao.deleteOldSyncedOperations(cutoffTime)
        
        // Delete very old unsynced operations (they're likely stale)
        val veryOldCutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(MAX_AGE_DAYS * 2)
        offlineOperationDao.deleteOldOperations(veryOldCutoff)
    }
    
    /**
     * Retry failed operations
     */
    suspend fun retryFailedOperations() {
        val retryableOperations = offlineOperationDao.getRetryableOperations()
        
        for (operation in retryableOperations) {
            // Reset retry count for manual retry
            val updatedOperation = operation.copy(
                retryCount = 0,
                lastError = null,
                processing = false
            )
            offlineOperationDao.updateOperation(updatedOperation)
        }
        
        if (retryableOperations.isNotEmpty()) {
            scheduleSyncWork()
        }
    }
    
    /**
     * Process a transaction operation
     */
    private suspend fun processTransactionOperation(operation: OfflineOperation): Boolean {
        return try {
            when (operation.operationType) {
                "CREATE" -> {
                    // Sync transaction to server
                    syncTransactionToServer(operation.data)
                }
                "UPDATE" -> {
                    // Update transaction on server
                    updateTransactionOnServer(operation.entityId, operation.data)
                }
                else -> false
            }
        } catch (e: Exception) {
            false
        }
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
     * Update queue state
     * Process a summary operation
     */
    private suspend fun processSummaryOperation(operation: OfflineOperation): Boolean {
        return try {
            when (operation.operationType) {
                "CREATE" -> {
                    // Sync summary to server
                    syncSummaryToServer(operation.data)
                }
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Sync transaction to remote server
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
            val persisted = offlineOperationDao.getAllOperationsSync()
            persisted.forEach { entity ->
                pendingOperations[entity.id] = entity.toDomain()
            }
            updateQueueState()
        }
    private suspend fun syncTransactionToServer(transactionData: String): Boolean {
        // Implementation would depend on your API client
        // For now, return true to simulate success
        return true
    }
    
    /**
     * Update transaction on remote server
     */
    private suspend fun persistOperation(operation: OfflineOperation) {
        withContext(ioDispatcher) {
            val retryCount = retryAttempts[operation.id] ?: 0
            operationDao.insertOrReplace(operation.toEntity(retryCount))
        }
        offlineOperationDao.upsertOperation(operation.toEntity())
    private suspend fun updateTransactionOnServer(transactionId: String, transactionData: String): Boolean {
        // Implementation would depend on your API client
        // For now, return true to simulate success
        return true
    }
    
    /**
     * Sync summary to remote server
     */
    private suspend fun removePersistedOperation(operationId: String) {
        withContext(ioDispatcher) {
            operationDao.deleteOperationById(operationId)
        }
        offlineOperationDao.deleteOperation(operationId)
    private suspend fun syncSummaryToServer(summaryData: String): Boolean {
        // Implementation would depend on your API client
        // For now, return true to simulate success
        return true
    }
    
    /**
     * Schedule periodic sync work
     */
    private fun scheduleSyncWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncRequest = PeriodicWorkRequestBuilder<OfflineSyncWorker>(15, TimeUnit.MINUTES)
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
     * Extract transaction ID from JSON data
     */
    private fun extractTransactionId(transactionData: String): String {
        return try {
            val json = gson.fromJson(transactionData, Map::class.java)
            json["id"]?.toString() ?: generateOperationId()
        } catch (e: Exception) {
            generateOperationId()
        }
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
    
    /**
     * Extract summary ID from JSON data
     */
    private fun extractSummaryId(summaryData: String): String {
        return try {
            val json = gson.fromJson(summaryData, Map::class.java)
            json["date"]?.toString() ?: generateOperationId()
        } catch (e: Exception) {
            generateOperationId()
        }
    }
    
    /**
     * Get oldest operation timestamp
     */
    private suspend fun getOldestOperationTimestamp(): Long? {
        val operations = offlineOperationDao.getUnsyncedOperations()
        return operations.minOfOrNull { it.timestamp }
    }
}

/**
 * Worker for processing offline operations
 */
@HiltWorker
class OfflineSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val offlineQueueManager: OfflineQueueManager
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            val syncResult = offlineQueueManager.processUnsyncedOperations()
            
            if (syncResult.failureCount == 0) {
                Result.success()
            } else if (syncResult.successCount > 0) {
                Result.success() // Partial success
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }
}

/**
 * Extension functions for converting between domain and entity models
 */
fun OfflineOperation.toEntity(): OfflineOperationEntity {
    return OfflineOperationEntity(
        id = id,
        type = type.name,
        data = data,
        timestamp = timestamp,
        priority = priority.name,
        status = status.name,
        errorMessage = errorMessage,
        lastAttempt = lastAttempt,
        retryCount = retryCount
    )
}

fun OfflineOperationEntity.toDomain(): OfflineOperation {
    val typeEnum = runCatching { OperationType.valueOf(type) }.getOrElse { OperationType.TRANSACTION_SYNC }
    val priorityEnum = runCatching { OperationPriority.valueOf(priority) }.getOrElse { OperationPriority.NORMAL }
    val statusEnum = runCatching { OperationStatus.valueOf(status) }.getOrElse { OperationStatus.PENDING }

    return OfflineOperation(
        id = id,
        type = typeEnum,
        data = data,
        timestamp = timestamp,
        priority = priorityEnum,
        status = statusEnum,
        errorMessage = errorMessage,
        lastAttempt = lastAttempt,
        retryCount = retryCount
    )
}
 * Result of sync operation
 */
data class SyncResult(
    val totalOperations: Int,
    val successCount: Int,
    val failureCount: Int,
    val results: List<OperationResult>
)

/**
 * Result of individual operation
 */
data class OperationResult(
    val operationId: String,
    val success: Boolean,
    val error: String?
)

/**
 * Queue statistics
 */
data class QueueStatistics(
    val unsyncedCount: Int,
    val transactionCount: Int,
    val summaryCount: Int,
    val failedCount: Int,
    val oldestOperation: Long?
)
