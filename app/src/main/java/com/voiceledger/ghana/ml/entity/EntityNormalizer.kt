package com.voiceledger.ghana.ml.entity

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Normalizes extracted entities to standard formats
 * Handles Ghana-specific currency, product names, and measurements
 */
@Singleton
class EntityNormalizer @Inject constructor() {
    
    companion object {
        private const val TAG = "EntityNormalizer"
        
        // Currency normalization
        private const val GHANA_CEDIS = "GHS"
        private const val PESEWAS_TO_CEDIS = 100.0
        
        // Product name mappings (local to standard)
        private val PRODUCT_MAPPINGS = mapOf(
            "apateshi" to "Tilapia",
            "tuo" to "Tilapia",
            "kpanla" to "Mackerel",
            "titus" to "Mackerel",
            "herring" to "Sardines",
            "sardin" to "Sardines",
            "adwene" to "Red Fish",
            "sumbre" to "Catfish",
            "komi" to "Croaker"
        )
        
        // Unit normalization
        private val UNIT_MAPPINGS = mapOf(
            "kokoo" to "bowl",
            "rubber" to "bucket",
            "pieces" to "piece",
            "bowls" to "bowl",
            "buckets" to "bucket",
            "tins" to "tin",
            "cans" to "tin",
            "sizes" to "size"
        )
        
        // Quantity multipliers for common expressions
        private val QUANTITY_MULTIPLIERS = mapOf(
            "small" to 1,
            "medium" to 2,
            "large" to 3,
            "big" to 3,
            "plenty" to 5,
            "many" to 5,
            "few" to 2,
            "couple" to 2,
            "several" to 3
        )
    }
    
    /**
     * Normalize amount to standard Ghana cedis format
     */
    fun normalizeAmount(amount: AmountResult): NormalizedAmount {
        return try {
            val normalizedValue = when (amount.currency.uppercase()) {
                "PESEWAS", "PESEWA", "P" -> amount.amount / PESEWAS_TO_CEDIS
                "GHS", "CEDIS", "CEDI", "GHANA CEDIS" -> amount.amount
                else -> amount.amount // Assume cedis if unclear
            }
            
            // Round to 2 decimal places
            val roundedValue = (normalizedValue * 100).toLong() / 100.0
            
            NormalizedAmount(
                amount = roundedValue,
                currency = GHANA_CEDIS,
                originalAmount = amount.amount,
                originalCurrency = amount.currency,
                confidence = amount.confidence,
                conversionApplied = amount.currency.uppercase() == "PESEWAS"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to normalize amount: ${amount.amount} ${amount.currency}", e)
            NormalizedAmount(
                amount = 0.0,
                currency = GHANA_CEDIS,
                originalAmount = amount.amount,
                originalCurrency = amount.currency,
                confidence = 0f,
                conversionApplied = false,
                error = e.message
            )
        }
    }
    
    /**
     * Normalize product name to canonical form
     */
    fun normalizeProduct(product: ProductResult): NormalizedProduct {
        return try {
            val normalizedName = PRODUCT_MAPPINGS[product.productName.lowercase()] 
                ?: product.canonicalName
            
            // Capitalize properly
            val properName = normalizedName.split(" ").joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }
            
            NormalizedProduct(
                productName = properName,
                category = normalizeCategory(product.category),
                originalName = product.productName,
                variants = product.variants,
                confidence = product.confidence,
                mappingApplied = PRODUCT_MAPPINGS.containsKey(product.productName.lowercase())
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to normalize product: ${product.productName}", e)
            NormalizedProduct(
                productName = product.productName,
                category = product.category,
                originalName = product.productName,
                variants = product.variants,
                confidence = 0f,
                mappingApplied = false,
                error = e.message
            )
        }
    }
    
    /**
     * Normalize quantity and unit
     */
    fun normalizeQuantity(quantity: QuantityResult): NormalizedQuantity {
        return try {
            val normalizedUnit = UNIT_MAPPINGS[quantity.unit.lowercase()] 
                ?: quantity.unit.lowercase()
            
            // Apply quantity multipliers if needed
            val adjustedQuantity = applyQuantityAdjustments(quantity.quantity, quantity.originalText)
            
            NormalizedQuantity(
                quantity = adjustedQuantity,
                unit = normalizedUnit,
                originalQuantity = quantity.quantity,
                originalUnit = quantity.unit,
                confidence = quantity.confidence,
                adjustmentApplied = adjustedQuantity != quantity.quantity
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to normalize quantity: ${quantity.quantity} ${quantity.unit}", e)
            NormalizedQuantity(
                quantity = quantity.quantity,
                unit = quantity.unit,
                originalQuantity = quantity.quantity,
                originalUnit = quantity.unit,
                confidence = 0f,
                adjustmentApplied = false,
                error = e.message
            )
        }
    }
    
    /**
     * Normalize complete entity extraction result
     */
    fun normalizeEntities(entities: EntityExtractionResult): NormalizedEntityResult {
        val startTime = System.currentTimeMillis()
        
        return try {
            val normalizedAmount = entities.amount?.let { normalizeAmount(it) }
            val normalizedProduct = entities.product?.let { normalizeProduct(it) }
            val normalizedQuantity = entities.quantity?.let { normalizeQuantity(it) }
            
            // Calculate overall confidence
            val confidences = listOfNotNull(
                normalizedAmount?.confidence,
                normalizedProduct?.confidence,
                normalizedQuantity?.confidence
            )
            val overallConfidence = if (confidences.isNotEmpty()) {
                confidences.average().toFloat()
            } else {
                0f
            }
            
            NormalizedEntityResult(
                amount = normalizedAmount,
                product = normalizedProduct,
                quantity = normalizedQuantity,
                overallConfidence = overallConfidence,
                processingTimeMs = System.currentTimeMillis() - startTime,
                originalResult = entities
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to normalize entities", e)
            NormalizedEntityResult(
                overallConfidence = 0f,
                processingTimeMs = System.currentTimeMillis() - startTime,
                originalResult = entities,
                error = e.message
            )
        }
    }
    
    /**
     * Create transaction-ready data from normalized entities
     */
    fun createTransactionData(normalized: NormalizedEntityResult): TransactionData? {
        return try {
            val amount = normalized.amount ?: return null
            val product = normalized.product ?: return null
            
            if (amount.amount <= 0 || product.productName.isBlank()) {
                return null
            }
            
            TransactionData(
                amount = amount.amount,
                currency = amount.currency,
                product = product.productName,
                category = product.category,
                quantity = normalized.quantity?.quantity,
                unit = normalized.quantity?.unit,
                confidence = normalized.overallConfidence,
                metadata = TransactionMetadata(
                    amountNormalized = amount.conversionApplied,
                    productMapped = product.mappingApplied,
                    quantityAdjusted = normalized.quantity?.adjustmentApplied ?: false,
                    originalAmount = amount.originalAmount,
                    originalCurrency = amount.originalCurrency,
                    originalProduct = product.originalName,
                    originalQuantity = normalized.quantity?.originalQuantity,
                    originalUnit = normalized.quantity?.originalUnit
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create transaction data", e)
            null
        }
    }
    
    private fun normalizeCategory(category: String): String {
        return when (category.lowercase()) {
            "fish", "fishes" -> "Fish"
            "seafood" -> "Seafood"
            "meat" -> "Meat"
            "vegetable", "vegetables" -> "Vegetables"
            "fruit", "fruits" -> "Fruits"
            else -> category.replaceFirstChar { it.uppercase() }
        }
    }
    
    private fun applyQuantityAdjustments(quantity: Int, originalText: String): Int {
        val lowerText = originalText.lowercase()
        
        // Check for quantity multipliers
        QUANTITY_MULTIPLIERS.forEach { (keyword, multiplier) ->
            if (lowerText.contains(keyword)) {
                return quantity * multiplier
            }
        }
        
        return quantity
    }
}

/**
 * Normalized amount result
 */
data class NormalizedAmount(
    val amount: Double,
    val currency: String,
    val originalAmount: Double,
    val originalCurrency: String,
    val confidence: Float,
    val conversionApplied: Boolean,
    val error: String? = null
) {
    val isValid: Boolean get() = error == null && amount > 0
}

/**
 * Normalized product result
 */
data class NormalizedProduct(
    val productName: String,
    val category: String,
    val originalName: String,
    val variants: List<String>,
    val confidence: Float,
    val mappingApplied: Boolean,
    val error: String? = null
) {
    val isValid: Boolean get() = error == null && productName.isNotBlank()
}

/**
 * Normalized quantity result
 */
data class NormalizedQuantity(
    val quantity: Int,
    val unit: String,
    val originalQuantity: Int,
    val originalUnit: String,
    val confidence: Float,
    val adjustmentApplied: Boolean,
    val error: String? = null
) {
    val isValid: Boolean get() = error == null && quantity > 0
}

/**
 * Complete normalized entity result
 */
data class NormalizedEntityResult(
    val amount: NormalizedAmount? = null,
    val product: NormalizedProduct? = null,
    val quantity: NormalizedQuantity? = null,
    val overallConfidence: Float,
    val processingTimeMs: Long,
    val originalResult: EntityExtractionResult,
    val error: String? = null
) {
    val isValid: Boolean get() = error == null && amount?.isValid == true && product?.isValid == true
    val hasAmount: Boolean get() = amount?.isValid == true
    val hasProduct: Boolean get() = product?.isValid == true
    val hasQuantity: Boolean get() = quantity?.isValid == true
}

/**
 * Transaction-ready data
 */
data class TransactionData(
    val amount: Double,
    val currency: String,
    val product: String,
    val category: String,
    val quantity: Int?,
    val unit: String?,
    val confidence: Float,
    val metadata: TransactionMetadata
)

/**
 * Transaction metadata for tracking normalizations
 */
data class TransactionMetadata(
    val amountNormalized: Boolean,
    val productMapped: Boolean,
    val quantityAdjusted: Boolean,
    val originalAmount: Double,
    val originalCurrency: String,
    val originalProduct: String,
    val originalQuantity: Int?,
    val originalUnit: String?
)