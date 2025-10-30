package com.voiceledger.ghana.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.voiceledger.ghana.data.local.entity.OfflineOperationEntity

@Dao
interface OfflineOperationDao {

    @Query("SELECT * FROM offline_operations ORDER BY priority DESC, timestamp ASC")
    suspend fun getAllOperations(): List<OfflineOperationEntity>

    @Query("SELECT * FROM offline_operations WHERE status = :status ORDER BY priority DESC, timestamp ASC")
    suspend fun getOperationsByStatus(status: String): List<OfflineOperationEntity>

    @Query("SELECT * FROM offline_operations WHERE status = 'PENDING' ORDER BY priority DESC, timestamp ASC")
    suspend fun getPendingOperations(): List<OfflineOperationEntity>

    @Query("SELECT * FROM offline_operations WHERE status = 'FAILED' ORDER BY priority DESC, timestamp ASC")
    suspend fun getFailedOperations(): List<OfflineOperationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(operation: OfflineOperationEntity)

    @Query("DELETE FROM offline_operations WHERE id = :id")
    suspend fun deleteOperationById(id: String)

    @Query("UPDATE offline_operations SET status = :status, errorMessage = :errorMessage, lastAttempt = :lastAttempt, retryCount = :retryCount WHERE id = :id")
    suspend fun updateOperationStatus(
        id: String,
        status: String,
        errorMessage: String?,
        lastAttempt: Long?,
        retryCount: Int
    )
import androidx.room.*
import com.voiceledger.ghana.data.local.entity.OfflineOperationEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for offline operations queue persistence
 */
@Dao
interface OfflineOperationDao {

    @Query("SELECT * FROM offline_operations ORDER BY timestamp ASC")
    fun getAllOperations(): Flow<List<OfflineOperationEntity>>

    @Query("SELECT * FROM offline_operations WHERE status = :status ORDER BY timestamp ASC")
    fun getOperationsByStatus(status: String): Flow<List<OfflineOperationEntity>>

    @Query("SELECT * FROM offline_operations WHERE status = :status ORDER BY timestamp ASC")
    suspend fun getOperationsByStatusSync(status: String): List<OfflineOperationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOperation(operation: OfflineOperationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOperations(operations: List<OfflineOperationEntity>)

    @Query("DELETE FROM offline_operations WHERE id = :operationId")
    suspend fun deleteOperation(operationId: String)

    @Query("DELETE FROM offline_operations WHERE status = :status")
    suspend fun deleteOperationsByStatus(status: String)

    @Query("DELETE FROM offline_operations")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM offline_operations WHERE status = :status")
    suspend fun countByStatus(status: String): Int

    @Query("SELECT * FROM offline_operations WHERE id = :operationId")
    suspend fun getOperationById(operationId: String): OfflineOperationEntity?

    @Query("SELECT * FROM offline_operations ORDER BY timestamp ASC")
    suspend fun getAllOperationsSync(): List<OfflineOperationEntity>
}
import com.voiceledger.ghana.data.local.entity.OfflineOperation
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for offline operations
 * Manages durable queue of operations that need to be synced
 */
@Dao
interface OfflineOperationDao {
    
    /**
     * Insert a new offline operation
     */
    @Insert
    suspend fun insertOperation(operation: OfflineOperation)
    
    /**
     * Insert multiple offline operations
     */
    @Insert
    suspend fun insertOperations(operations: List<OfflineOperation>)
    
    /**
     * Update an existing offline operation
     */
    @Update
    suspend fun updateOperation(operation: OfflineOperation)
    
    /**
     * Delete an offline operation
     */
    @Delete
    suspend fun deleteOperation(operation: OfflineOperation)
    
    /**
     * Get offline operation by ID
     */
    @Query("SELECT * FROM offline_operations WHERE id = :id")
    suspend fun getOperationById(id: String): OfflineOperation?
    
    /**
     * Get all unsynced operations
     */
    @Query("SELECT * FROM offline_operations WHERE synced = 0 ORDER BY priority ASC, timestamp ASC")
    suspend fun getUnsyncedOperations(): List<OfflineOperation>
    
    /**
     * Get all unsynced operations as Flow
     */
    @Query("SELECT * FROM offline_operations WHERE synced = 0 ORDER BY priority ASC, timestamp ASC")
    fun getUnsyncedOperationsFlow(): Flow<List<OfflineOperation>>
    
    /**
     * Get unsynced operations by type
     */
    @Query("SELECT * FROM offline_operations WHERE synced = 0 AND operationType = :type ORDER BY priority ASC, timestamp ASC")
    suspend fun getUnsyncedOperationsByType(type: String): List<OfflineOperation>
    
    /**
     * Get operations that need retry (not exceeded max retries)
     */
    @Query("SELECT * FROM offline_operations WHERE synced = 0 AND retryCount < maxRetries ORDER BY priority ASC, timestamp ASC")
    suspend fun getRetryableOperations(): List<OfflineOperation>
    
    /**
     * Get operations that have exceeded max retries
     */
    @Query("SELECT * FROM offline_operations WHERE retryCount >= maxRetries AND synced = 0")
    suspend fun getFailedOperations(): List<OfflineOperation>
    
    /**
     * Get operations older than specified timestamp
     */
    @Query("SELECT * FROM offline_operations WHERE timestamp < :timestamp ORDER BY timestamp ASC")
    suspend fun getOperationsOlderThan(timestamp: Long): List<OfflineOperation>
    
    /**
     * Mark operation as synced
     */
    @Query("UPDATE offline_operations SET synced = 1, processing = 0 WHERE id = :id")
    suspend fun markAsSynced(id: String)
    
    /**
     * Mark operation as processing
     */
    @Query("UPDATE offline_operations SET processing = 1 WHERE id = :id")
    suspend fun markAsProcessing(id: String)
    
    /**
     * Mark operation as not processing
     */
    @Query("UPDATE offline_operations SET processing = 0 WHERE id = :id")
    suspend fun markAsNotProcessing(id: String)
    
    /**
     * Increment retry count for an operation
     */
    @Query("UPDATE offline_operations SET retryCount = retryCount + 1, processing = 0, lastError = :error WHERE id = :id")
    suspend fun incrementRetryCount(id: String, error: String? = null)
    
    /**
     * Delete operations older than specified timestamp
     */
    @Query("DELETE FROM offline_operations WHERE timestamp < :timestamp")
    suspend fun deleteOldOperations(timestamp: Long)
    
    /**
     * Delete synced operations older than specified timestamp
     */
    @Query("DELETE FROM offline_operations WHERE synced = 1 AND timestamp < :timestamp")
    suspend fun deleteOldSyncedOperations(timestamp: Long)
    
    /**
     * Get count of unsynced operations
     */
    @Query("SELECT COUNT(*) FROM offline_operations WHERE synced = 0")
    suspend fun getUnsyncedCount(): Int
    
    /**
     * Get count of unsynced operations by type
     */
    @Query("SELECT COUNT(*) FROM offline_operations WHERE synced = 0 AND operationType = :type")
    suspend fun getUnsyncedCountByType(type: String): Int
    
    /**
     * Get count of failed operations
     */
    @Query("SELECT COUNT(*) FROM offline_operations WHERE retryCount >= maxRetries AND synced = 0")
    suspend fun getFailedCount(): Int
    
    /**
     * Clear all operations
     */
    @Query("DELETE FROM offline_operations")
    suspend fun clearAllOperations()
    
    /**
     * Get operations by priority
     */
    @Query("SELECT * FROM offline_operations WHERE priority = :priority AND synced = 0 ORDER BY timestamp ASC")
    suspend fun getOperationsByPriority(priority: Int): List<OfflineOperation>
    
    /**
     * Get next operation to process (highest priority, oldest timestamp)
     */
    @Query("SELECT * FROM offline_operations WHERE synced = 0 AND processing = 0 ORDER BY priority ASC, timestamp ASC LIMIT 1")
    suspend fun getNextOperation(): OfflineOperation?
}
