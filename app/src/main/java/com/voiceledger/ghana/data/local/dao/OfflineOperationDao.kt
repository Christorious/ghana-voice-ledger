package com.voiceledger.ghana.data.local.dao

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
