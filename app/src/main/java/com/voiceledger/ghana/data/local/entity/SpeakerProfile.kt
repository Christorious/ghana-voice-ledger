package com.voiceledger.ghana.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Speaker profile entity for voice identification
 * Stores voice embeddings and customer information
 */
@Entity(
    tableName = "speaker_profiles",
    indices = [
        Index(value = ["isSeller"]),
        Index(value = ["lastVisit"]),
        Index(value = ["visitCount"]),
        Index(value = ["isActive"]),
        Index(value = ["synced"]),
        Index(value = ["createdAt"]),
        Index(value = ["isSeller", "isActive"]),
        Index(value = ["lastVisit", "isActive"])
    ]
)
data class SpeakerProfile(
    @PrimaryKey
    val id: String,
    
    /** 128-dimensional voice embedding as comma-separated string */
    val voiceEmbedding: String, // Stored as comma-separated floats for Room compatibility
    
    /** Whether this profile belongs to the seller */
    val isSeller: Boolean,
    
    /** Display name for the speaker (optional) */
    val name: String?,
    
    /** Number of times this customer has visited */
    val visitCount: Int,
    
    /** Timestamp of last visit */
    val lastVisit: Long,
    
    /** Average spending per visit (nullable for seller) */
    val averageSpending: Double?,
    
    /** Total amount spent by this customer */
    val totalSpent: Double?,
    
    /** Preferred language (en, tw, ga) */
    val preferredLanguage: String?,
    
    /** Customer type classification (regular, occasional, new) */
    val customerType: String?,
    
    /** Confidence threshold for this speaker (learned over time) */
    val confidenceThreshold: Float = 0.75f,
    
    /** Whether this profile is active (not deleted) */
    val isActive: Boolean = true,
    
    /** Timestamp when profile was created */
    val createdAt: Long,
    
    /** Timestamp when profile was last updated */
    val updatedAt: Long,
    
    /** Whether this profile has been synced to cloud */
    val synced: Boolean = false
) {
    /**
     * Convert comma-separated string back to FloatArray
     */
    fun getEmbeddingArray(): FloatArray {
        return voiceEmbedding.split(",").map { it.toFloat() }.toFloatArray()
    }
    
    companion object {
        /**
         * Convert FloatArray to comma-separated string for storage
         */
        fun embeddingToString(embedding: FloatArray): String {
            return embedding.joinToString(",")
        }
    }
}