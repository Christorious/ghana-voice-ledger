package com.voiceledger.ghana.offline

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Base class for offline-first repositories
 * Provides common patterns for local-first data access with cloud sync
 */
abstract class OfflineFirstRepository<T, ID>(
    protected val context: Context,
    protected val offlineQueueManager: OfflineQueueManager
) {
    
    /**
     * Get item by ID - tries local first, then remote if needed
     */
    abstract suspend fun getById(id: ID): T?
    
    /**
     * Get all items - local first approach
     */
    abstract suspend fun getAll(): List<T>
    
    /**
     * Save item - saves locally immediately, queues for sync
     */
    abstract suspend fun save(item: T): T
    
    /**
     * Delete item - deletes locally immediately, queues for sync
     */
    abstract suspend fun delete(id: ID): Boolean
    
    /**
     * Sync with remote - called when network is available
     */
    abstract suspend fun syncWithRemote(): SyncResult
    
    /**
     * Get items as flow - reactive local data
     */
    abstract fun getAsFlow(): Flow<List<T>>
    
    /**
     * Check if item exists locally
     */
    abstract suspend fun existsLocally(id: ID): Boolean
    
    /**
     * Get local cache status
     */
    abstract suspend fun getCacheStatus(): CacheStatus
    
    /**
     * Force refresh from remote (when network available)
     */
    suspend fun forceRefresh(): SyncResult {
        return if (NetworkUtils.isNetworkAvailable(context)) {
            syncWithRemote()
        } else {
            SyncResult.NetworkUnavailable
        }
    }
    
    /**
     * Get recommended sync strategy
     */
    protected fun getRecommendedSyncStrategy(): SyncStrategy {
        return NetworkUtils.getRecommendedSyncStrategy(context)
    }
    
    /**
     * Queue operation for offline sync
     */
    protected suspend fun queueForSync(
        operationType: OperationType,
        data: String,
        priority: OperationPriority = OperationPriority.NORMAL
    ) {
        val operation = OfflineOperation(
            id = generateOperationId(),
            type = operationType,
            data = data,
            timestamp = System.currentTimeMillis(),
            priority = priority
        )
        
        offlineQueueManager.enqueueOperation(operation)
    }
    
    /**
     * Generate unique operation ID
     */
    private fun generateOperationId(): String {
        return "${System.currentTimeMillis()}_${(0..999).random()}"
    }
}

/**
 * Sync result types
 */
sealed class SyncResult {
    object Success : SyncResult()
    object NetworkUnavailable : SyncResult()
    data class PartialSuccess(val syncedCount: Int, val failedCount: Int) : SyncResult()
    data class Error(val message: String, val cause: Throwable? = null) : SyncResult()
}

/**
 * Cache status information
 */
data class CacheStatus(
    val totalItems: Int,
    val lastSyncTime: Long,
    val pendingSyncItems: Int,
    val isStale: Boolean
)

/**
 * Offline-first transaction repository implementation
 */
class OfflineTransactionRepository @Inject constructor(
    context: Context,
    offlineQueueManager: OfflineQueueManager,
    private val localTransactionDao: com.voiceledger.ghana.data.local.dao.TransactionDao,
    private val remoteTransactionApi: RemoteTransactionApi? = null
) : OfflineFirstRepository<com.voiceledger.ghana.data.local.entity.Transaction, String>(
    context, offlineQueueManager
) {
    
    override suspend fun getById(id: String): com.voiceledger.ghana.data.local.entity.Transaction? {
        // Always try local first
        val localTransaction = localTransactionDao.getTransactionById(id)
        
        if (localTransaction != null) {
            return localTransaction
        }
        
        // If not found locally and network is available, try remote
        if (NetworkUtils.isNetworkAvailable(context) && remoteTransactionApi != null) {
            try {
                val remoteTransaction = remoteTransactionApi.getTransaction(id)
                remoteTransaction?.let {
                    // Cache locally
                    localTransactionDao.insertTransaction(it)
                    return it
                }
            } catch (e: Exception) {
                // Remote fetch failed, continue with local-only approach
            }
        }
        
        return null
    }
    
    override suspend fun getAll(): List<com.voiceledger.ghana.data.local.entity.Transaction> {
        return localTransactionDao.getAllTransactions()
    }
    
    override suspend fun save(item: com.voiceledger.ghana.data.local.entity.Transaction): com.voiceledger.ghana.data.local.entity.Transaction {
        // Save locally immediately
        localTransactionDao.insertTransaction(item)
        
        // Queue for remote sync
        queueForSync(
            operationType = OperationType.TRANSACTION_SYNC,
            data = kotlinx.serialization.json.Json.encodeToString(item),
            priority = OperationPriority.NORMAL
        )
        
        return item
    }
    
    override suspend fun delete(id: String): Boolean {
        // Delete locally immediately
        val deleted = localTransactionDao.deleteTransaction(id) > 0
        
        if (deleted) {
            // Queue for remote deletion
            queueForSync(
                operationType = OperationType.DELETE_DATA,
                data = kotlinx.serialization.json.Json.encodeToString(mapOf("type" to "transaction", "id" to id)),
                priority = OperationPriority.HIGH
            )
        }
        
        return deleted
    }
    
    override suspend fun syncWithRemote(): SyncResult {
        if (!NetworkUtils.isNetworkAvailable(context) || remoteTransactionApi == null) {
            return SyncResult.NetworkUnavailable
        }
        
        return try {
            // Get local transactions that need sync
            val localTransactions = localTransactionDao.getUnsyncedTransactions()
            var syncedCount = 0
            var failedCount = 0
            
            // Sync each transaction
            localTransactions.forEach { transaction ->
                try {
                    remoteTransactionApi.syncTransaction(transaction)
                    localTransactionDao.markAsSynced(transaction.id)
                    syncedCount++
                } catch (e: Exception) {
                    failedCount++
                }
            }
            
            // Fetch updates from remote
            try {
                val remoteTransactions = remoteTransactionApi.getUpdatedTransactions(getLastSyncTime())
                remoteTransactions.forEach { transaction ->
                    localTransactionDao.insertOrUpdateTransaction(transaction)
                }
            } catch (e: Exception) {
                // Continue even if remote fetch fails
            }
            
            updateLastSyncTime()
            
            if (failedCount == 0) {
                SyncResult.Success
            } else {
                SyncResult.PartialSuccess(syncedCount, failedCount)
            }
            
        } catch (e: Exception) {
            SyncResult.Error("Sync failed: ${e.message}", e)
        }
    }
    
    override fun getAsFlow(): Flow<List<com.voiceledger.ghana.data.local.entity.Transaction>> {
        return localTransactionDao.getAllTransactionsFlow()
    }
    
    override suspend fun existsLocally(id: String): Boolean {
        return localTransactionDao.getTransactionById(id) != null
    }
    
    override suspend fun getCacheStatus(): CacheStatus {
        val totalItems = localTransactionDao.getTransactionCount()
        val pendingSyncItems = localTransactionDao.getUnsyncedTransactionCount()
        val lastSyncTime = getLastSyncTime()
        val isStale = (System.currentTimeMillis() - lastSyncTime) > (24 * 60 * 60 * 1000) // 24 hours
        
        return CacheStatus(
            totalItems = totalItems,
            lastSyncTime = lastSyncTime,
            pendingSyncItems = pendingSyncItems,
            isStale = isStale
        )
    }
    
    /**
     * Get transactions for date range (local first)
     */
    suspend fun getTransactionsForDateRange(startDate: String, endDate: String): List<com.voiceledger.ghana.data.local.entity.Transaction> {
        return localTransactionDao.getTransactionsByDateRange(startDate, endDate)
    }
    
    /**
     * Get today's transactions (local only for performance)
     */
    fun getTodaysTransactionsFlow(): Flow<List<com.voiceledger.ghana.data.local.entity.Transaction>> {
        return localTransactionDao.getTodaysTransactionsFlow()
    }
    
    private fun getLastSyncTime(): Long {
        // Implementation would get from SharedPreferences or database
        return 0L
    }
    
    private fun updateLastSyncTime() {
        // Implementation would save to SharedPreferences or database
    }
}

/**
 * Remote API interface for transactions
 */
interface RemoteTransactionApi {
    suspend fun getTransaction(id: String): com.voiceledger.ghana.data.local.entity.Transaction?
    suspend fun syncTransaction(transaction: com.voiceledger.ghana.data.local.entity.Transaction)
    suspend fun getUpdatedTransactions(since: Long): List<com.voiceledger.ghana.data.local.entity.Transaction>
    suspend fun deleteTransaction(id: String)
}