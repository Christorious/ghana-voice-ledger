package com.voiceledger.ghana.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.voiceledger.ghana.data.local.entity.OfflineOperationEntity

/**
 * DAO for the offline sync queue.
 *
 * Backed by the `offline_operations` table created in MIGRATION_1_2, whose
 * columns are id, type, data, timestamp, priority, status, errorMessage,
 * lastAttempt and retryCount. Queue order is FIFO by timestamp.
 */
@Dao
interface OfflineOperationDao {

    @Query("SELECT * FROM offline_operations ORDER BY timestamp ASC")
    suspend fun getAllOperations(): List<OfflineOperationEntity>

    /** Alias kept for call sites that spell out the non-Flow intent. */
    @Query("SELECT * FROM offline_operations ORDER BY timestamp ASC")
    suspend fun getAllOperationsSync(): List<OfflineOperationEntity>

    @Query("SELECT * FROM offline_operations WHERE id = :operationId")
    suspend fun getOperationById(operationId: String): OfflineOperationEntity?

    @Query("SELECT * FROM offline_operations WHERE status = :status ORDER BY timestamp ASC")
    suspend fun getOperationsByStatus(status: String): List<OfflineOperationEntity>

    @Query("SELECT * FROM offline_operations WHERE status = 'PENDING' ORDER BY timestamp ASC")
    suspend fun getPendingOperations(): List<OfflineOperationEntity>

    @Query("SELECT * FROM offline_operations WHERE status = 'FAILED' ORDER BY timestamp ASC")
    suspend fun getFailedOperations(): List<OfflineOperationEntity>

    @Query("SELECT COUNT(*) FROM offline_operations WHERE status = :status")
    suspend fun countByStatus(status: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOperation(operation: OfflineOperationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOperations(operations: List<OfflineOperationEntity>)

    /** Same contract as [upsertOperation]; kept for existing call sites. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(operation: OfflineOperationEntity)

    @Query(
        "UPDATE offline_operations SET status = :status, errorMessage = :errorMessage, " +
            "lastAttempt = :lastAttempt, retryCount = :retryCount WHERE id = :id"
    )
    suspend fun updateOperationStatus(
        id: String,
        status: String,
        errorMessage: String?,
        lastAttempt: Long?,
        retryCount: Int
    )

    @Query("DELETE FROM offline_operations WHERE id = :operationId")
    suspend fun deleteOperation(operationId: String)

    /** Same contract as [deleteOperation]; kept for existing call sites. */
    @Query("DELETE FROM offline_operations WHERE id = :id")
    suspend fun deleteOperationById(id: String)

    @Query("DELETE FROM offline_operations WHERE status = :status")
    suspend fun deleteOperationsByStatus(status: String)

    /** Housekeeping: drop anything older than [timestamp]. */
    @Query("DELETE FROM offline_operations WHERE timestamp < :timestamp")
    suspend fun deleteOldOperations(timestamp: Long)

    @Query("DELETE FROM offline_operations")
    suspend fun clearAll()
}
