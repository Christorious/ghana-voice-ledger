package com.voiceledger.ghana.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import com.voiceledger.ghana.offline.OfflineOperation
import com.voiceledger.ghana.offline.OperationType
import com.voiceledger.ghana.offline.OperationPriority
import com.voiceledger.ghana.offline.OperationStatus

@Entity(
    tableName = "offline_operations",
    indices = [
        Index(value = ["status"]),
        Index(value = ["priority"]),
        Index(value = ["timestamp"])

/**
 * Entity representing an offline operation to be synced when network becomes available
 * Used for offline-first architecture to queue operations
 */
@Entity(
    tableName = "offline_operations",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["type"]),
        Index(value = ["status"]),
        Index(value = ["priority"])
    ]
)
data class OfflineOperationEntity(
    @PrimaryKey
    val id: String,
    
    val type: String,
    
    val data: String,
    
    val timestamp: Long,
    
    val priority: String,
    
    val status: String,
    
    val errorMessage: String?,
    
    val lastAttempt: Long?,
    
    val retryCount: Int = 0
)

fun OfflineOperationEntity.toOfflineOperation(): OfflineOperation {
    return OfflineOperation(
        id = id,
        type = OperationType.valueOf(type),
        data = data,
        timestamp = timestamp,
        priority = OperationPriority.valueOf(priority),
        status = OperationStatus.valueOf(status),
        errorMessage = errorMessage,
        lastAttempt = lastAttempt
    )
}

fun OfflineOperation.toEntity(retryCount: Int = 0): OfflineOperationEntity {
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
    /** Type of operation (TRANSACTION_SYNC, SUMMARY_SYNC, etc.) */
    val type: String,
    
    /** Serialized data for the operation (JSON) */
    val data: String,
    
    /** Timestamp when operation was created */
    val timestamp: Long,
    
    /** Priority level (LOW, NORMAL, HIGH, CRITICAL) */
    val priority: String = "NORMAL",
    
    /** Current status (PENDING, PROCESSING, COMPLETED, FAILED) */
    val status: String = "PENDING",
    
    /** Error message if operation failed */
    val errorMessage: String? = null,
    
    /** Timestamp of last attempt */
    val lastAttempt: Long? = null,
    
    /** Number of retry attempts */
    val retryCount: Int = 0
)
