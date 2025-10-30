package com.voiceledger.ghana.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Transaction entity representing a completed sales transaction
 * Optimized with indexes for common query patterns
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["product"]),
        Index(value = ["customerId"]),
        Index(value = ["date"]),
        Index(value = ["needsReview"]),
        Index(value = ["synced"]),
        Index(value = ["date", "customerId"]),
        Index(value = ["date", "needsReview"]),
        Index(value = ["synced", "timestamp"]),
        Index(value = ["customerId", "timestamp"])
    ]
)
data class Transaction(
    @PrimaryKey
    val id: String,
    
    /** Unix timestamp when transaction occurred */
    val timestamp: Long,
    
    /** Date in YYYY-MM-DD format for efficient daily queries */
    val date: String,
    
    /** Transaction amount in Ghana cedis */
    val amount: Double,
    
    /** Currency code (always GHS for Ghana cedis) */
    val currency: String = "GHS",
    
    /** Product name (standardized) */
    val product: String,
    
    /** Quantity sold (nullable for cases where quantity wasn't detected) */
    val quantity: Int?,
    
    /** Unit of measurement (pieces, bowls, buckets, etc.) */
    val unit: String?,
    
    /** Customer ID if repeat customer recognized */
    val customerId: String?,
    
    /** Overall confidence score (0.0 to 1.0) */
    val confidence: Float,
    
    /** Snippet of conversation that led to this transaction */
    val transcriptSnippet: String,
    
    /** Confidence that seller was correctly identified */
    val sellerConfidence: Float,
    
    /** Confidence that customer was correctly identified */
    val customerConfidence: Float,
    
    /** Whether this transaction needs manual review */
    val needsReview: Boolean = false,
    
    /** Whether this transaction has been synced to cloud */
    val synced: Boolean = false,
    
    /** Original price quoted before negotiation (if any) */
    val originalPrice: Double?,
    
    /** Final agreed price */
    val finalPrice: Double = amount,
    
    /** Market session identifier (AM/PM) */
    val marketSession: String? = null
)