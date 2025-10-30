package com.voiceledger.ghana.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Product vocabulary entity for Ghana market products
 * Supports fuzzy matching and learning from user corrections
 */
@Entity(
    tableName = "product_vocabulary",
    indices = [
        Index(value = ["canonicalName"]),
        Index(value = ["category"]),
        Index(value = ["isActive"]),
        Index(value = ["frequency"]),
        Index(value = ["isLearned"]),
        Index(value = ["updatedAt"])
    ]
)
data class ProductVocabulary(
    @PrimaryKey
    val id: String,
    
    /** Standardized product name */
    val canonicalName: String,
    
    /** Product category (fish, vegetables, etc.) */
    val category: String,
    
    /** Alternative names/variants as comma-separated string */
    val variants: String, // e.g., "tilapia,apateshi,tuo"
    
    /** Typical price range minimum in Ghana cedis */
    val minPrice: Double,
    
    /** Typical price range maximum in Ghana cedis */
    val maxPrice: Double,
    
    /** Common measurement units as comma-separated string */
    val measurementUnits: String, // e.g., "piece,bowl,bucket"
    
    /** How often this product appears in transactions */
    val frequency: Int = 0,
    
    /** Whether this product is currently active/available */
    val isActive: Boolean = true,
    
    /** Seasonal availability (if applicable) */
    val seasonality: String?,
    
    /** Local language names in Twi */
    val twiNames: String?,
    
    /** Local language names in Ga */
    val gaNames: String?,
    
    /** Timestamp when this entry was created */
    val createdAt: Long,
    
    /** Timestamp when this entry was last updated */
    val updatedAt: Long,
    
    /** Whether this entry was learned from user corrections */
    val isLearned: Boolean = false,
    
    /** Confidence score for learned entries */
    val learningConfidence: Float = 1.0f
) {
    /**
     * Get all variants as a list
     */
    fun getVariantsList(): List<String> {
        return variants.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
    
    /**
     * Get all measurement units as a list
     */
    fun getMeasurementUnitsList(): List<String> {
        return measurementUnits.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
    
    /**
     * Check if price is within typical range
     */
    fun isPriceInRange(price: Double): Boolean {
        return price >= minPrice && price <= maxPrice
    }
    
    companion object {
        /**
         * Convert list of strings to comma-separated string
         */
        fun listToString(items: List<String>): String {
            return items.joinToString(",")
        }
    }
}