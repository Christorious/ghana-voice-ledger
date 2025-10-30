package com.voiceledger.ghana.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

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
