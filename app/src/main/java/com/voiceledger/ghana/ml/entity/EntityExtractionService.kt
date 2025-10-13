package com.voiceledger.ghana.ml.entity

import android.util.Log
import com.voiceledger.ghana.domain.repository.ProductVocabularyRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level service for entity extraction and processing
 * Coordinates extraction, normalization, and validation
 */
@Singleton
class EntityExtractionService @Inject constructor(
    private val entityExtractor: EntityExtractor,
    private val entityNormalizer: EntityNormalizer,
    private val productVocabularyRepository: ProductVocabularyRepository
) {
    
    companion object {
        private const val TAG = "EntityExtractionService"
        private const val CACHE_SIZE = 100
        private const val CACHE_EXPIRY_MS = 5 * 60 * 1000L // 5 minutes
    }
    
    // Cache for recent extractions
    private val extractionCache = mutableMapOf<String, CachedExtraction>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    /**
     * Process transcript and extract transaction entities
     */
    suspend fun processTranscript(
        transcript: String,
        speakerId: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ): ProcessingResult = withContext(Dispatchers.Default) {
        
        val startTime = System.currentTimeMillis()
        
        try {
            // Check cache first
            val cacheKey = generateCacheKey(transcript)
            val cached = getCachedExtraction(cacheKey)
            if (cached != null) {
                Log.d(TAG, "Using cached extraction for transcript")
                return@withContext ProcessingResult(
                    entities = cached.normalizedResult,
                    validation = cached.validation,
                    transactionData = cached.transactionData,
                    processingTimeMs = System.currentTimeMillis() - startTime,
                    fromCache = true
                )
            }
            
            // Extract entities
            val rawEntities = entityExtractor.extractAllEntities(transcript)
            
            // Normalize entities
            val normalizedEntities = entityNormalizer.normalizeEntities(rawEntities)
            
            // Validate entities
            val validation = entityExtractor.validateEntities(rawEntities)
            
            // Create transaction data if valid
            val transactionData = if (validation.isValid) {
                entityNormalizer.createTransactionData(normalizedEntities)
            } else null
            
            // Update product vocabulary if new product detected
            normalizedEntities.product?.let { product ->
                if (product.mappingApplied) {
                    updateProductVocabulary(product, transcript)
                }
            }
            
            val result = ProcessingResult(
                entities = normalizedEntities,
                validation = validation,
                transactionData = transactionData,
                processingTimeMs = System.currentTimeMillis() - startTime,
                fromCache = false
            )
            
            // Cache the result
            cacheExtraction(cacheKey, result)
            
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process transcript: $transcript", e)
            ProcessingResult(
                entities = NormalizedEntityResult(
                    overallConfidence = 0f,
                    processingTimeMs = System.currentTimeMillis() - startTime,
                    originalResult = EntityExtractionResult(
                        overallConfidence = 0f,
                        processingTimeMs = 0,
                        extractedText = transcript,
                        issues = listOf("Processing failed: ${e.message}")
                    ),
                    error = e.message
                ),
                validation = ValidationResult(
                    isValid = false,
                    confidence = 0f,
                    issues = listOf(ValidationIssue(
                        type = IssueType.LOW_CONFIDENCE,
                        message = "Processing failed: ${e.message}",
                        severity = IssueSeverity.ERROR
                    ))
                ),
                transactionData = null,
                processingTimeMs = System.currentTimeMillis() - startTime,
                fromCache = false,
                error = e.message
            )
        }
    }
    
    /**
     * Process multiple transcripts in batch
     */
    suspend fun processBatch(
        transcripts: List<TranscriptInput>
    ): List<ProcessingResult> = withContext(Dispatchers.Default) {
        
        transcripts.map { input ->
            async {
                processTranscript(
                    transcript = input.transcript,
                    speakerId = input.speakerId,
                    timestamp = input.timestamp
                )
            }
        }.awaitAll()
    }
    
    /**
     * Get processing statistics
     */
    suspend fun getProcessingStats(): ProcessingStats = withContext(Dispatchers.Default) {
        ProcessingStats(
            cacheSize = extractionCache.size,
            cacheHitRate = calculateCacheHitRate(),
            averageProcessingTime = calculateAverageProcessingTime(),
            totalProcessed = getTotalProcessedCount()
        )
    }
    
    /**
     * Clear processing cache
     */
    fun clearCache() {
        synchronized(extractionCache) {
            extractionCache.clear()
        }
        Log.d(TAG, "Extraction cache cleared")
    }
    
    /**
     * Validate transaction data against business rules
     */
    suspend fun validateTransaction(transactionData: TransactionData): BusinessValidationResult = withContext(Dispatchers.Default) {
        val issues = mutableListOf<BusinessIssue>()
        
        // Check amount ranges
        if (transactionData.amount < 0.5) {
            issues.add(BusinessIssue(
                type = "AMOUNT_TOO_LOW",
                message = "Amount below minimum transaction value (GH₵0.50)",
                severity = "WARNING"
            ))
        }
        
        if (transactionData.amount > 1000) {
            issues.add(BusinessIssue(
                type = "AMOUNT_HIGH",
                message = "Amount unusually high for market transaction",
                severity = "WARNING"
            ))
        }
        
        // Validate product against vocabulary
        val productExists = productVocabularyRepository.getProductByName(transactionData.product) != null
        if (!productExists) {
            issues.add(BusinessIssue(
                type = "UNKNOWN_PRODUCT",
                message = "Product not found in vocabulary: ${transactionData.product}",
                severity = "INFO"
            ))
        }
        
        // Check quantity reasonableness
        transactionData.quantity?.let { quantity ->
            if (quantity > 50) {
                issues.add(BusinessIssue(
                    type = "QUANTITY_HIGH",
                    message = "Quantity seems unusually high: $quantity",
                    severity = "WARNING"
                ))
            }
        }
        
        BusinessValidationResult(
            isValid = issues.none { it.severity == "ERROR" },
            confidence = transactionData.confidence,
            issues = issues,
            recommendedAction = determineRecommendedAction(issues)
        )
    }
    
    /**
     * Learn from user corrections
     */
    suspend fun learnFromCorrection(
        originalTranscript: String,
        correctedData: TransactionData,
        correctionType: CorrectionType
    ) = withContext(Dispatchers.Default) {
        
        try {
            when (correctionType) {
                CorrectionType.PRODUCT_NAME -> {
                    // Update product vocabulary
                    productVocabularyRepository.correctProductName(
                        wrongName = extractProductFromTranscript(originalTranscript),
                        correctName = correctedData.product
                    )
                }
                CorrectionType.AMOUNT -> {
                    // Could implement amount pattern learning here
                    Log.d(TAG, "Amount correction noted: ${correctedData.amount}")
                }
                CorrectionType.QUANTITY -> {
                    // Could implement quantity pattern learning here
                    Log.d(TAG, "Quantity correction noted: ${correctedData.quantity}")
                }
            }
            
            // Invalidate cache for this transcript
            val cacheKey = generateCacheKey(originalTranscript)
            synchronized(extractionCache) {
                extractionCache.remove(cacheKey)
            }
            
            Log.d(TAG, "Learned from correction: $correctionType")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to learn from correction", e)
        }
    }
    
    private fun getCachedExtraction(cacheKey: String): CachedExtraction? {
        synchronized(extractionCache) {
            val cached = extractionCache[cacheKey]
            return if (cached != null && !cached.isExpired()) {
                cached
            } else {
                extractionCache.remove(cacheKey)
                null
            }
        }
    }
    
    private fun cacheExtraction(cacheKey: String, result: ProcessingResult) {
        synchronized(extractionCache) {
            // Remove oldest entries if cache is full
            if (extractionCache.size >= CACHE_SIZE) {
                val oldestKey = extractionCache.keys.first()
                extractionCache.remove(oldestKey)
            }
            
            extractionCache[cacheKey] = CachedExtraction(
                normalizedResult = result.entities,
                validation = result.validation,
                transactionData = result.transactionData,
                timestamp = System.currentTimeMillis()
            )
        }
    }
    
    private fun generateCacheKey(transcript: String): String {
        return transcript.lowercase().trim().hashCode().toString()
    }
    
    private suspend fun updateProductVocabulary(product: NormalizedProduct, transcript: String) {
        try {
            // Increment frequency for recognized products
            val existingProduct = productVocabularyRepository.getProductByName(product.productName)
            existingProduct?.let {
                productVocabularyRepository.incrementFrequency(it.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update product vocabulary", e)
        }
    }
    
    private fun calculateCacheHitRate(): Float {
        // This would be tracked with actual metrics in production
        return 0.75f // Placeholder
    }
    
    private fun calculateAverageProcessingTime(): Long {
        // This would be tracked with actual metrics in production
        return 150L // Placeholder
    }
    
    private fun getTotalProcessedCount(): Long {
        // This would be tracked with actual metrics in production
        return 1000L // Placeholder
    }
    
    private fun determineRecommendedAction(issues: List<BusinessIssue>): String {
        return when {
            issues.any { it.severity == "ERROR" } -> "REJECT"
            issues.any { it.severity == "WARNING" } -> "REVIEW"
            else -> "ACCEPT"
        }
    }
    
    private fun extractProductFromTranscript(transcript: String): String {
        // Simple extraction for learning - in production this would be more sophisticated
        return transcript.split(" ").find { word ->
            word.length > 3 && !word.matches("\\d+".toRegex())
        } ?: "unknown"
    }
}

/**
 * Input for transcript processing
 */
data class TranscriptInput(
    val transcript: String,
    val speakerId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Complete processing result
 */
data class ProcessingResult(
    val entities: NormalizedEntityResult,
    val validation: ValidationResult,
    val transactionData: TransactionData?,
    val processingTimeMs: Long,
    val fromCache: Boolean,
    val error: String? = null
) {
    val isSuccess: Boolean get() = error == null
    val isValid: Boolean get() = validation.isValid
    val hasTransactionData: Boolean get() = transactionData != null
}

/**
 * Processing statistics
 */
data class ProcessingStats(
    val cacheSize: Int,
    val cacheHitRate: Float,
    val averageProcessingTime: Long,
    val totalProcessed: Long
)

/**
 * Business validation result
 */
data class BusinessValidationResult(
    val isValid: Boolean,
    val confidence: Float,
    val issues: List<BusinessIssue>,
    val recommendedAction: String
)

/**
 * Business validation issue
 */
data class BusinessIssue(
    val type: String,
    val message: String,
    val severity: String // ERROR, WARNING, INFO
)

/**
 * Types of corrections for learning
 */
enum class CorrectionType {
    PRODUCT_NAME,
    AMOUNT,
    QUANTITY
}

/**
 * Cached extraction result
 */
private data class CachedExtraction(
    val normalizedResult: NormalizedEntityResult,
    val validation: ValidationResult,
    val transactionData: TransactionData?,
    val timestamp: Long
) {
    fun isExpired(): Boolean {
        return System.currentTimeMillis() - timestamp > EntityExtractionService.CACHE_EXPIRY_MS
    }
}