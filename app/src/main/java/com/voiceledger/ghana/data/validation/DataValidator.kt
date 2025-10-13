package com.voiceledger.ghana.data.validation

import com.voiceledger.ghana.data.local.entity.Transaction
import com.voiceledger.ghana.data.local.entity.SpeakerProfile
import com.voiceledger.ghana.data.local.entity.ProductVocabulary
import com.voiceledger.ghana.data.local.entity.DailySummary
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data validation utility for Ghana Voice Ledger
 * Ensures data integrity and business rule compliance
 */
@Singleton
class DataValidator @Inject constructor() {
    
    companion object {
        // Ghana-specific validation patterns
        private val GHANA_CEDIS_PATTERN = Pattern.compile("^\\d+(\\.\\d{1,2})?$")
        private val PRODUCT_NAME_PATTERN = Pattern.compile("^[a-zA-Z\\s\\-']{2,50}$")
        private val SPEAKER_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-]{3,50}$")
        private val DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$")
        
        // Business constraints
        private const val MIN_TRANSACTION_AMOUNT = 0.50 // 50 pesewas minimum
        private const val MAX_TRANSACTION_AMOUNT = 10000.0 // 10,000 cedis maximum
        private const val MIN_CONFIDENCE_SCORE = 0.0f
        private const val MAX_CONFIDENCE_SCORE = 1.0f
        private const val MAX_TRANSCRIPT_LENGTH = 500
        private const val MAX_PRODUCT_NAME_LENGTH = 50
        private const val MAX_SPEAKER_NAME_LENGTH = 100
    }
    
    /**
     * Validate transaction data
     */
    fun validateTransaction(transaction: Transaction): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Validate ID
        if (transaction.id.isBlank()) {
            errors.add("Transaction ID cannot be empty")
        }
        
        // Validate timestamp
        if (transaction.timestamp <= 0) {
            errors.add("Transaction timestamp must be positive")
        }
        
        // Validate date format
        if (!DATE_PATTERN.matcher(transaction.date).matches()) {
            errors.add("Transaction date must be in YYYY-MM-DD format")
        }
        
        // Validate amount
        if (transaction.amount < MIN_TRANSACTION_AMOUNT) {
            errors.add("Transaction amount must be at least GH₵$MIN_TRANSACTION_AMOUNT")
        }
        
        if (transaction.amount > MAX_TRANSACTION_AMOUNT) {
            errors.add("Transaction amount cannot exceed GH₵$MAX_TRANSACTION_AMOUNT")
        }
        
        if (!GHANA_CEDIS_PATTERN.matcher(transaction.amount.toString()).matches()) {
            errors.add("Transaction amount must be a valid Ghana cedis value")
        }
        
        // Validate currency
        if (transaction.currency != "GHS") {
            errors.add("Currency must be GHS (Ghana cedis)")
        }
        
        // Validate product
        if (transaction.product.isBlank()) {
            errors.add("Product name cannot be empty")
        }
        
        if (transaction.product.length > MAX_PRODUCT_NAME_LENGTH) {
            errors.add("Product name cannot exceed $MAX_PRODUCT_NAME_LENGTH characters")
        }
        
        // Validate quantity
        transaction.quantity?.let { quantity ->
            if (quantity <= 0) {
                errors.add("Quantity must be positive")
            }
        }
        
        // Validate confidence scores
        if (transaction.confidence < MIN_CONFIDENCE_SCORE || transaction.confidence > MAX_CONFIDENCE_SCORE) {
            errors.add("Confidence score must be between $MIN_CONFIDENCE_SCORE and $MAX_CONFIDENCE_SCORE")
        }
        
        if (transaction.sellerConfidence < MIN_CONFIDENCE_SCORE || transaction.sellerConfidence > MAX_CONFIDENCE_SCORE) {
            errors.add("Seller confidence score must be between $MIN_CONFIDENCE_SCORE and $MAX_CONFIDENCE_SCORE")
        }
        
        if (transaction.customerConfidence < MIN_CONFIDENCE_SCORE || transaction.customerConfidence > MAX_CONFIDENCE_SCORE) {
            errors.add("Customer confidence score must be between $MIN_CONFIDENCE_SCORE and $MAX_CONFIDENCE_SCORE")
        }
        
        // Validate transcript snippet
        if (transaction.transcriptSnippet.length > MAX_TRANSCRIPT_LENGTH) {
            errors.add("Transcript snippet cannot exceed $MAX_TRANSCRIPT_LENGTH characters")
        }
        
        // Validate price consistency
        transaction.originalPrice?.let { originalPrice ->
            if (originalPrice < MIN_TRANSACTION_AMOUNT || originalPrice > MAX_TRANSACTION_AMOUNT) {
                errors.add("Original price must be within valid range")
            }
            
            if (transaction.finalPrice != transaction.amount) {
                errors.add("Final price must match transaction amount")
            }
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
    
    /**
     * Validate speaker profile data
     */
    fun validateSpeakerProfile(profile: SpeakerProfile): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Validate ID
        if (!SPEAKER_ID_PATTERN.matcher(profile.id).matches()) {
            errors.add("Speaker ID must be 3-50 characters, alphanumeric with underscores and hyphens only")
        }
        
        // Validate voice embedding
        if (profile.voiceEmbedding.isBlank()) {
            errors.add("Voice embedding cannot be empty")
        } else {
            try {
                val embedding = profile.getEmbeddingArray()
                if (embedding.isEmpty()) {
                    errors.add("Voice embedding must contain at least one value")
                }
                
                if (embedding.size != 128) { // Expected embedding size
                    errors.add("Voice embedding must be 128 dimensions")
                }
            } catch (e: Exception) {
                errors.add("Voice embedding format is invalid")
            }
        }
        
        // Validate name
        profile.name?.let { name ->
            if (name.length > MAX_SPEAKER_NAME_LENGTH) {
                errors.add("Speaker name cannot exceed $MAX_SPEAKER_NAME_LENGTH characters")
            }
        }
        
        // Validate visit count
        if (profile.visitCount < 0) {
            errors.add("Visit count cannot be negative")
        }
        
        // Validate timestamps
        if (profile.lastVisit <= 0) {
            errors.add("Last visit timestamp must be positive")
        }
        
        if (profile.createdAt <= 0) {
            errors.add("Created timestamp must be positive")
        }
        
        if (profile.updatedAt <= 0) {
            errors.add("Updated timestamp must be positive")
        }
        
        if (profile.updatedAt < profile.createdAt) {
            errors.add("Updated timestamp cannot be before created timestamp")
        }
        
        // Validate spending data for customers
        if (!profile.isSeller) {
            profile.averageSpending?.let { avgSpending ->
                if (avgSpending < 0) {
                    errors.add("Average spending cannot be negative")
                }
            }
            
            profile.totalSpent?.let { totalSpent ->
                if (totalSpent < 0) {
                    errors.add("Total spent cannot be negative")
                }
            }
        }
        
        // Validate confidence threshold
        if (profile.confidenceThreshold < MIN_CONFIDENCE_SCORE || profile.confidenceThreshold > MAX_CONFIDENCE_SCORE) {
            errors.add("Confidence threshold must be between $MIN_CONFIDENCE_SCORE and $MAX_CONFIDENCE_SCORE")
        }
        
        // Validate language code
        profile.preferredLanguage?.let { language ->
            if (language !in listOf("en", "tw", "ga")) {
                errors.add("Preferred language must be 'en', 'tw', or 'ga'")
            }
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
    
    /**
     * Validate product vocabulary data
     */
    fun validateProductVocabulary(product: ProductVocabulary): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Validate ID
        if (product.id.isBlank()) {
            errors.add("Product ID cannot be empty")
        }
        
        // Validate canonical name
        if (product.canonicalName.isBlank()) {
            errors.add("Canonical name cannot be empty")
        }
        
        if (product.canonicalName.length > MAX_PRODUCT_NAME_LENGTH) {
            errors.add("Canonical name cannot exceed $MAX_PRODUCT_NAME_LENGTH characters")
        }
        
        // Validate category
        if (product.category.isBlank()) {
            errors.add("Category cannot be empty")
        }
        
        // Validate price range
        if (product.minPrice < 0) {
            errors.add("Minimum price cannot be negative")
        }
        
        if (product.maxPrice < 0) {
            errors.add("Maximum price cannot be negative")
        }
        
        if (product.maxPrice < product.minPrice) {
            errors.add("Maximum price cannot be less than minimum price")
        }
        
        // Validate frequency
        if (product.frequency < 0) {
            errors.add("Frequency cannot be negative")
        }
        
        // Validate timestamps
        if (product.createdAt <= 0) {
            errors.add("Created timestamp must be positive")
        }
        
        if (product.updatedAt <= 0) {
            errors.add("Updated timestamp must be positive")
        }
        
        if (product.updatedAt < product.createdAt) {
            errors.add("Updated timestamp cannot be before created timestamp")
        }
        
        // Validate learning confidence
        if (product.learningConfidence < MIN_CONFIDENCE_SCORE || product.learningConfidence > MAX_CONFIDENCE_SCORE) {
            errors.add("Learning confidence must be between $MIN_CONFIDENCE_SCORE and $MAX_CONFIDENCE_SCORE")
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
    
    /**
     * Validate daily summary data
     */
    fun validateDailySummary(summary: DailySummary): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Validate date format
        if (!DATE_PATTERN.matcher(summary.date).matches()) {
            errors.add("Date must be in YYYY-MM-DD format")
        }
        
        // Validate sales data
        if (summary.totalSales < 0) {
            errors.add("Total sales cannot be negative")
        }
        
        if (summary.transactionCount < 0) {
            errors.add("Transaction count cannot be negative")
        }
        
        if (summary.averageTransactionValue < 0) {
            errors.add("Average transaction value cannot be negative")
        }
        
        // Validate customer data
        if (summary.repeatCustomers < 0) {
            errors.add("Repeat customers count cannot be negative")
        }
        
        if (summary.uniqueCustomers < 0) {
            errors.add("Unique customers count cannot be negative")
        }
        
        if (summary.reviewedTransactions < 0) {
            errors.add("Reviewed transactions count cannot be negative")
        }
        
        // Validate timestamps
        if (summary.generatedAt <= 0) {
            errors.add("Generated timestamp must be positive")
        }
        
        // Validate profitable hour
        summary.mostProfitableHour?.let { hour ->
            if (hour < 0 || hour > 23) {
                errors.add("Most profitable hour must be between 0 and 23")
            }
        }
        
        // Validate day-over-day change
        summary.dayOverDayChange?.let { change ->
            if (change < -1.0 || change > 10.0) { // Allow up to 1000% increase, 100% decrease
                errors.add("Day-over-day change seems unrealistic")
            }
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
    
    /**
     * Validate Ghana cedis amount format
     */
    fun validateGhanaCedisAmount(amount: String): Boolean {
        return GHANA_CEDIS_PATTERN.matcher(amount).matches()
    }
    
    /**
     * Validate product name format
     */
    fun validateProductName(name: String): Boolean {
        return PRODUCT_NAME_PATTERN.matcher(name).matches()
    }
    
    /**
     * Check if amount is within reasonable transaction range
     */
    fun isReasonableTransactionAmount(amount: Double): Boolean {
        return amount >= MIN_TRANSACTION_AMOUNT && amount <= MAX_TRANSACTION_AMOUNT
    }
    
    /**
     * Check if confidence score is valid
     */
    fun isValidConfidenceScore(confidence: Float): Boolean {
        return confidence >= MIN_CONFIDENCE_SCORE && confidence <= MAX_CONFIDENCE_SCORE
    }
}

/**
 * Data class representing validation result
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>
) {
    fun getErrorMessage(): String {
        return errors.joinToString("; ")
    }
    
    fun hasErrors(): Boolean = !isValid
    
    fun throwIfInvalid() {
        if (!isValid) {
            throw ValidationException(getErrorMessage())
        }
    }
}

/**
 * Exception thrown when validation fails
 */
class ValidationException(message: String) : Exception(message)