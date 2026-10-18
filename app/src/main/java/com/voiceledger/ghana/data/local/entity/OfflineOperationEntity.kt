package com.voiceledger.ghana.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import com.voiceledger.ghana.offline.OfflineOperation
import com.voiceledger.ghana.offline.OperationType
import com.voiceledger.ghana.offline.OperationPriority
import com.voiceledger.ghana.offline.OperationStatus

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
