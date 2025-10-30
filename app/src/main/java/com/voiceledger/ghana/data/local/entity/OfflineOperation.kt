package com.voiceledger.ghana.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Entity representing an offline operation that needs to be synced
 * Ensures durability of operations when network connectivity is unavailable
 */
@Entity(
    tableName = "offline_operations",
    indices = [
        Index(value = ["operationType"]),
        Index(value = ["timestamp"]),
        Index(value = ["synced"]),
        Index(value = ["retryCount"])
    ]
)
data class OfflineOperation(
    @PrimaryKey
    val id: String,
    
    /** Type of operation (CREATE, UPDATE, DELETE) */
    val operationType: String,
    
    /** Target entity type (TRANSACTION, DAILY_SUMMARY, etc.) */
    val entityType: String,
    
    /** ID of the target entity */
    val entityId: String,
    
    /** Serialized data for the operation (JSON) */
    val data: String,
    
    /** Unix timestamp when operation was created */
    val timestamp: Long,
    
    /** Whether this operation has been synced to the server */
    val synced: Boolean = false,
    
    /** Number of retry attempts */
    val retryCount: Int = 0,
    
    /** Maximum retry attempts allowed */
    val maxRetries: Int = 3,
    
    /** Last error message (if any) */
    val lastError: String? = null,
    
    /** Priority of this operation (1=highest, 5=lowest) */
    val priority: Int = 3,
    
    /** Whether this operation is currently being processed */
    val processing: Boolean = false
)