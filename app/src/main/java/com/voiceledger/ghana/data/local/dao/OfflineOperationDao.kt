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
}
