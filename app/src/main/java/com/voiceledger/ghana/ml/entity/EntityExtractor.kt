package com.voiceledger.ghana.ml.entity

/**
 * Interface for extracting transaction entities from speech transcripts
 * Handles Ghana-specific currency, products, and quantities
 */
interface EntityExtractor {
    
    /**
     * Extract amount information from text
     * @param text The transcript text to analyze
     * @return Amount extraction result with confidence
     */
    suspend fun extractAmount(text: String): AmountResult?
    
    /**
     * Extract product information from text
     * @param text The transcript text to analyze
     * @return Product extraction result with confidence
     */
    suspend fun extractProduct(text: String): ProductResult?
    
    /**
     * Extract quantity information from text
     * @param text The transcript text to analyze
     * @return Quantity extraction result with confidence
     */
    suspend fun extractQuantity(text: String): QuantityResult?
    
    /**
     * Extract all entities from text in one pass
     * @param text The transcript text to analyze
     * @return Complete entity extraction result
     */
    suspend fun extractAllEntities(text: String): EntityExtractionResult
    
    /**
     * Validate extracted transaction entities
     * @param entities The extracted entities to validate
     * @return Validation result with issues and suggestions
     */
    suspend fun validateEntities(entities: EntityExtractionResult): ValidationResult
    
    /**
     * Get extraction confidence for the given text
     * @param text The transcript text to analyze
     * @return Overall confidence score for entity extraction
     */
    suspend fun getExtractionConfidence(text: String): Float
}

/**
 * Result of amount extraction
 */
data class AmountResult(
    val amount: Double,
    val currency: String,
    val confidence: Float,
    val originalText: String,
    val normalizedText: String,
    val startIndex: Int,
    val endIndex: Int,
    val alternatives: List<AmountAlternative> = emptyList()
) {
    val isValid: Boolean get() = amount > 0 && confidence > 0.5f
}

/**
 * Alternative amount interpretation
 */
data class AmountAlternative(
    val amount: Double,
    val currency: String,
    val confidence: Float,
    val interpretation: String
)

/**
 * Result of product extraction
 */
data class ProductResult(
    val productName: String,
    val canonicalName: String,
    val category: String,
    val confidence: Float,
    val originalText: String,
    val startIndex: Int,
    val endIndex: Int,
    val variants: List<String> = emptyList(),
    val alternatives: List<ProductAlternative> = emptyList()
) {
    val isValid: Boolean get() = productName.isNotBlank() && confidence > 0.6f
}

/**
 * Alternative product interpretation
 */
data class ProductAlternative(
    val productName: String,
    val canonicalName: String,
    val confidence: Float,
    val matchType: String // "exact", "variant", "fuzzy"
)

/**
 * Result of quantity extraction
 */
data class QuantityResult(
    val quantity: Int,
    val unit: String,
    val confidence: Float,
    val originalText: String,
    val startIndex: Int,
    val endIndex: Int,
    val alternatives: List<QuantityAlternative> = emptyList()
) {
    val isValid: Boolean get() = quantity > 0 && confidence > 0.5f
}

/**
 * Alternative quantity interpretation
 */
data class QuantityAlternative(
    val quantity: Int,
    val unit: String,
    val confidence: Float,
    val interpretation: String
)

/**
 * Complete entity extraction result
 */
data class EntityExtractionResult(
    val amount: AmountResult? = null,
    val product: ProductResult? = null,
    val quantity: QuantityResult? = null,
    val overallConfidence: Float,
    val processingTimeMs: Long,
    val extractedText: String,
    val language: String? = null,
    val issues: List<String> = emptyList()
) {
    val hasAmount: Boolean get() = amount?.isValid == true
    val hasProduct: Boolean get() = product?.isValid == true
    val hasQuantity: Boolean get() = quantity?.isValid == true
    val isComplete: Boolean get() = hasAmount && hasProduct
    val isEmpty: Boolean get() = !hasAmount && !hasProduct && !hasQuantity
}

/**
 * Validation result for extracted entities
 */
data class ValidationResult(
    val isValid: Boolean,
    val confidence: Float,
    val issues: List<ValidationIssue> = emptyList(),
    val suggestions: List<String> = emptyList()
)

/**
 * Validation issue details
 */
data class ValidationIssue(
    val type: IssueType,
    val message: String,
    val severity: IssueSeverity,
    val field: String? = null
)

/**
 * Types of validation issues
 */
enum class IssueType {
    MISSING_AMOUNT,
    MISSING_PRODUCT,
    INVALID_AMOUNT,
    UNKNOWN_PRODUCT,
    UNREALISTIC_QUANTITY,
    PRICE_OUT_OF_RANGE,
    AMBIGUOUS_ENTITY,
    LOW_CONFIDENCE
}

/**
 * Severity levels for validation issues
 */
enum class IssueSeverity {
    ERROR,    // Prevents transaction logging
    WARNING,  // Flags for review
    INFO      // Informational only
}