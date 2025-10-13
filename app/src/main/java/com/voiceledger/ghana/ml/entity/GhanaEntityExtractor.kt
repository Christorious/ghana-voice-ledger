package com.voiceledger.ghana.ml.entity

import android.util.Log
import com.voiceledger.ghana.domain.repository.ProductVocabularyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ghana-specific entity extractor for market transactions
 * Handles Ghana cedis, local fish names, and market-specific quantities
 */
@Singleton
class GhanaEntityExtractor @Inject constructor(
    private val productVocabularyRepository: ProductVocabularyRepository
) : EntityExtractor {
    
    companion object {
        private const val TAG = "GhanaEntityExtractor"
        
        // Ghana currency patterns
        private val CEDIS_PATTERNS = listOf(
            Pattern.compile("(\\d+(?:\\.\\d{1,2})?)\\s*(?:ghana\\s*)?cedis?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("gh₵\\s*(\\d+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d+(?:\\.\\d{1,2})?)\\s*gh₵", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d+(?:\\.\\d{1,2})?)\\s*pesewas?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("((?:twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety|hundred)(?:\\s+(?:one|two|three|four|five|six|seven|eight|nine))?)", Pattern.CASE_INSENSITIVE)
        )
        
        // Quantity patterns
        private val QUANTITY_PATTERNS = listOf(
            Pattern.compile("(\\d+)\\s*(pieces?|pcs?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d+)\\s*(bowls?|kokoo)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d+)\\s*(buckets?|rubber)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d+)\\s*(tins?|cans?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d+)\\s*(sizes?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(one|two|three|four|five|six|seven|eight|nine|ten)\\s*(pieces?|bowls?|buckets?)", Pattern.CASE_INSENSITIVE)
        )
        
        // Number word mappings
        private val NUMBER_WORDS = mapOf(
            "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
            "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
            "eleven" to 11, "twelve" to 12, "thirteen" to 13, "fourteen" to 14, "fifteen" to 15,
            "sixteen" to 16, "seventeen" to 17, "eighteen" to 18, "nineteen" to 19, "twenty" to 20,
            "thirty" to 30, "forty" to 40, "fifty" to 50, "sixty" to 60, "seventy" to 70,
            "eighty" to 80, "ninety" to 90, "hundred" to 100
        )
        
        // Ghana-specific measurement units
        private val GHANA_UNITS = mapOf(
            "kokoo" to "bowl",
            "rubber" to "bucket",
            "piece" to "piece",
            "pieces" to "piece",
            "bowl" to "bowl",
            "bowls" to "bowl",
            "bucket" to "bucket",
            "buckets" to "bucket",
            "tin" to "tin",
            "tins" to "tin",
            "size" to "size",
            "sizes" to "size"
        )
    }
    
    override suspend fun extractAmount(text: String): AmountResult? = withContext(Dispatchers.Default) {
        val normalizedText = text.lowercase().trim()
        val alternatives = mutableListOf<AmountAlternative>()
        
        // Try each currency pattern
        for (pattern in CEDIS_PATTERNS) {
            val matcher = pattern.matcher(normalizedText)
            if (matcher.find()) {
                val amountStr = matcher.group(1)
                val amount = parseAmount(amountStr)
                
                if (amount > 0) {
                    val confidence = calculateAmountConfidence(matcher.group(), normalizedText)
                    
                    return@withContext AmountResult(
                        amount = amount,
                        currency = "GHS",
                        confidence = confidence,
                        originalText = matcher.group(),
                        normalizedText = normalizedText,
                        startIndex = matcher.start(),
                        endIndex = matcher.end(),
                        alternatives = alternatives
                    )
                }
            }
        }
        
        // Try to extract numeric amounts without explicit currency
        val numericPattern = Pattern.compile("(\\d+(?:\\.\\d{1,2})?)")
        val matcher = numericPattern.matcher(normalizedText)
        
        while (matcher.find()) {
            val amount = matcher.group(1).toDoubleOrNull()
            if (amount != null && amount > 0 && amount < 10000) { // Reasonable range
                alternatives.add(
                    AmountAlternative(
                        amount = amount,
                        currency = "GHS",
                        confidence = 0.6f, // Lower confidence without explicit currency
                        interpretation = "Inferred Ghana cedis"
                    )
                )
            }
        }
        
        // Return best alternative if any found
        alternatives.maxByOrNull { it.confidence }?.let { best ->
            AmountResult(
                amount = best.amount,
                currency = best.currency,
                confidence = best.confidence,
                originalText = best.amount.toString(),
                normalizedText = normalizedText,
                startIndex = 0,
                endIndex = normalizedText.length,
                alternatives = alternatives
            )
        }
    }
    
    override suspend fun extractProduct(text: String): ProductResult? = withContext(Dispatchers.Default) {
        val normalizedText = text.lowercase().trim()
        
        // Search for products using the vocabulary repository
        val searchResults = productVocabularyRepository.searchProducts(normalizedText)
        
        if (searchResults.isNotEmpty()) {
            val bestMatch = searchResults.first()
            val confidence = calculateProductConfidence(bestMatch.canonicalName, normalizedText)
            
            val alternatives = searchResults.drop(1).take(3).map { product ->
                ProductAlternative(
                    productName = product.canonicalName,
                    canonicalName = product.canonicalName,
                    confidence = calculateProductConfidence(product.canonicalName, normalizedText),
                    matchType = "search"
                )
            }
            
            return@withContext ProductResult(
                productName = bestMatch.canonicalName,
                canonicalName = bestMatch.canonicalName,
                category = bestMatch.category,
                confidence = confidence,
                originalText = normalizedText,
                startIndex = 0,
                endIndex = normalizedText.length,
                variants = bestMatch.getVariantsList(),
                alternatives = alternatives
            )
        }
        
        // Try fuzzy matching if no direct search results
        val fuzzyMatches = productVocabularyRepository.fuzzyMatch(normalizedText, 2)
        if (fuzzyMatches.isNotEmpty()) {
            val bestMatch = fuzzyMatches.first()
            val confidence = 0.7f // Lower confidence for fuzzy matches
            
            return@withContext ProductResult(
                productName = bestMatch.canonicalName,
                canonicalName = bestMatch.canonicalName,
                category = bestMatch.category,
                confidence = confidence,
                originalText = normalizedText,
                startIndex = 0,
                endIndex = normalizedText.length,
                variants = bestMatch.getVariantsList(),
                alternatives = emptyList()
            )
        }
        
        null
    }
    
    override suspend fun extractQuantity(text: String): QuantityResult? = withContext(Dispatchers.Default) {
        val normalizedText = text.lowercase().trim()
        val alternatives = mutableListOf<QuantityAlternative>()
        
        // Try each quantity pattern
        for (pattern in QUANTITY_PATTERNS) {
            val matcher = pattern.matcher(normalizedText)
            if (matcher.find()) {
                val quantityStr = matcher.group(1)
                val unitStr = matcher.group(2)
                
                val quantity = parseQuantity(quantityStr)
                val unit = GHANA_UNITS[unitStr.lowercase()] ?: unitStr.lowercase()
                
                if (quantity > 0) {
                    val confidence = calculateQuantityConfidence(matcher.group(), normalizedText)
                    
                    return@withContext QuantityResult(
                        quantity = quantity,
                        unit = unit,
                        confidence = confidence,
                        originalText = matcher.group(),
                        startIndex = matcher.start(),
                        endIndex = matcher.end(),
                        alternatives = alternatives
                    )
                }
            }
        }
        
        // Try to infer quantity from context
        if (normalizedText.contains("small") || normalizedText.contains("little")) {
            alternatives.add(
                QuantityAlternative(
                    quantity = 1,
                    unit = "piece",
                    confidence = 0.5f,
                    interpretation = "Inferred from 'small'"
                )
            )
        }
        
        if (normalizedText.contains("plenty") || normalizedText.contains("many")) {
            alternatives.add(
                QuantityAlternative(
                    quantity = 5,
                    unit = "piece",
                    confidence = 0.4f,
                    interpretation = "Inferred from 'plenty'"
                )
            )
        }
        
        // Return best alternative if any found
        alternatives.maxByOrNull { it.confidence }?.let { best ->
            QuantityResult(
                quantity = best.quantity,
                unit = best.unit,
                confidence = best.confidence,
                originalText = best.interpretation,
                startIndex = 0,
                endIndex = normalizedText.length,
                alternatives = alternatives
            )
        }
    }
    
    override suspend fun extractAllEntities(text: String): EntityExtractionResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val issues = mutableListOf<String>()
        
        try {
            val amount = extractAmount(text)
            val product = extractProduct(text)
            val quantity = extractQuantity(text)
            
            val overallConfidence = calculateOverallConfidence(amount, product, quantity)
            val processingTime = System.currentTimeMillis() - startTime
            
            // Add issues for missing critical entities
            if (amount == null) {
                issues.add("No amount detected in transcript")
            }
            if (product == null) {
                issues.add("No product detected in transcript")
            }
            
            EntityExtractionResult(
                amount = amount,
                product = product,
                quantity = quantity,
                overallConfidence = overallConfidence,
                processingTimeMs = processingTime,
                extractedText = text,
                issues = issues
            )
        } catch (e: Exception) {
            Log.e(TAG, "Entity extraction failed", e)
            EntityExtractionResult(
                overallConfidence = 0f,
                processingTimeMs = System.currentTimeMillis() - startTime,
                extractedText = text,
                issues = listOf("Entity extraction failed: ${e.message}")
            )
        }
    }
    
    override suspend fun validateEntities(entities: EntityExtractionResult): ValidationResult = withContext(Dispatchers.Default) {
        val issues = mutableListOf<ValidationIssue>()
        var confidence = entities.overallConfidence
        
        // Validate amount
        entities.amount?.let { amount ->
            if (amount.amount < 0.5) {
                issues.add(ValidationIssue(
                    type = IssueType.INVALID_AMOUNT,
                    message = "Amount too small (${amount.amount} cedis)",
                    severity = IssueSeverity.WARNING,
                    field = "amount"
                ))
            }
            
            if (amount.amount > 1000) {
                issues.add(ValidationIssue(
                    type = IssueType.PRICE_OUT_OF_RANGE,
                    message = "Amount unusually high (${amount.amount} cedis)",
                    severity = IssueSeverity.WARNING,
                    field = "amount"
                ))
            }
            
            if (amount.confidence < 0.7f) {
                issues.add(ValidationIssue(
                    type = IssueType.LOW_CONFIDENCE,
                    message = "Low confidence in amount detection (${amount.confidence})",
                    severity = IssueSeverity.INFO,
                    field = "amount"
                ))
            }
        } ?: run {
            issues.add(ValidationIssue(
                type = IssueType.MISSING_AMOUNT,
                message = "No amount detected",
                severity = IssueSeverity.ERROR,
                field = "amount"
            ))
            confidence *= 0.5f
        }
        
        // Validate product
        entities.product?.let { product ->
            if (product.confidence < 0.6f) {
                issues.add(ValidationIssue(
                    type = IssueType.LOW_CONFIDENCE,
                    message = "Low confidence in product detection (${product.confidence})",
                    severity = IssueSeverity.INFO,
                    field = "product"
                ))
            }
        } ?: run {
            issues.add(ValidationIssue(
                type = IssueType.MISSING_PRODUCT,
                message = "No product detected",
                severity = IssueSeverity.ERROR,
                field = "product"
            ))
            confidence *= 0.5f
        }
        
        // Validate quantity
        entities.quantity?.let { quantity ->
            if (quantity.quantity > 100) {
                issues.add(ValidationIssue(
                    type = IssueType.UNREALISTIC_QUANTITY,
                    message = "Quantity seems unrealistic (${quantity.quantity})",
                    severity = IssueSeverity.WARNING,
                    field = "quantity"
                ))
            }
        }
        
        val isValid = issues.none { it.severity == IssueSeverity.ERROR }
        val suggestions = generateSuggestions(entities, issues)
        
        ValidationResult(
            isValid = isValid,
            confidence = confidence,
            issues = issues,
            suggestions = suggestions
        )
    }
    
    override suspend fun getExtractionConfidence(text: String): Float = withContext(Dispatchers.Default) {
        val entities = extractAllEntities(text)
        entities.overallConfidence
    }
    
    private fun parseAmount(amountStr: String): Double {
        return try {
            // Handle number words
            if (NUMBER_WORDS.containsKey(amountStr.lowercase())) {
                NUMBER_WORDS[amountStr.lowercase()]!!.toDouble()
            } else {
                amountStr.toDouble()
            }
        } catch (e: NumberFormatException) {
            0.0
        }
    }
    
    private fun parseQuantity(quantityStr: String): Int {
        return try {
            // Handle number words
            if (NUMBER_WORDS.containsKey(quantityStr.lowercase())) {
                NUMBER_WORDS[quantityStr.lowercase()]!!
            } else {
                quantityStr.toInt()
            }
        } catch (e: NumberFormatException) {
            0
        }
    }
    
    private fun calculateAmountConfidence(matchedText: String, fullText: String): Float {
        var confidence = 0.8f
        
        // Higher confidence for explicit currency mentions
        if (matchedText.contains("cedis", ignoreCase = true) || matchedText.contains("gh₵", ignoreCase = true)) {
            confidence += 0.1f
        }
        
        // Lower confidence for very short matches
        if (matchedText.length < 3) {
            confidence -= 0.2f
        }
        
        // Context-based adjustments
        if (fullText.contains("price") || fullText.contains("cost") || fullText.contains("how much")) {
            confidence += 0.1f
        }
        
        return confidence.coerceIn(0f, 1f)
    }
    
    private fun calculateProductConfidence(productName: String, text: String): Float {
        var confidence = 0.7f
        
        // Exact match gets higher confidence
        if (text.contains(productName, ignoreCase = true)) {
            confidence += 0.2f
        }
        
        // Context-based adjustments
        if (text.contains("fish") || text.contains("sell")) {
            confidence += 0.1f
        }
        
        return confidence.coerceIn(0f, 1f)
    }
    
    private fun calculateQuantityConfidence(matchedText: String, fullText: String): Float {
        var confidence = 0.7f
        
        // Higher confidence for explicit unit mentions
        if (matchedText.contains("piece") || matchedText.contains("bowl") || matchedText.contains("bucket")) {
            confidence += 0.1f
        }
        
        // Ghana-specific units get bonus
        if (matchedText.contains("kokoo") || matchedText.contains("rubber")) {
            confidence += 0.15f
        }
        
        return confidence.coerceIn(0f, 1f)
    }
    
    private fun calculateOverallConfidence(
        amount: AmountResult?,
        product: ProductResult?,
        quantity: QuantityResult?
    ): Float {
        val confidences = listOfNotNull(
            amount?.confidence,
            product?.confidence,
            quantity?.confidence
        )
        
        return if (confidences.isNotEmpty()) {
            confidences.average().toFloat()
        } else {
            0f
        }
    }
    
    private fun generateSuggestions(entities: EntityExtractionResult, issues: List<ValidationIssue>): List<String> {
        val suggestions = mutableListOf<String>()
        
        if (entities.amount == null) {
            suggestions.add("Try asking 'How much is this?' to get price information")
        }
        
        if (entities.product == null) {
            suggestions.add("Mention the specific fish name (e.g., tilapia, mackerel)")
        }
        
        if (issues.any { it.type == IssueType.LOW_CONFIDENCE }) {
            suggestions.add("Speak more clearly or repeat the transaction details")
        }
        
        return suggestions
    }
}