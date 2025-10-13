package com.voiceledger.ghana.ml.transaction

import android.util.Log
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pattern matcher for Ghana market transaction phrases
 * Recognizes transaction triggers in English, Twi, and Ga languages
 */
@Singleton
class TransactionPatternMatcher @Inject constructor() {
    
    companion object {
        private const val TAG = "TransactionPatternMatcher"
        
        // Confidence thresholds
        private const val HIGH_CONFIDENCE = 0.9f
        private const val MEDIUM_CONFIDENCE = 0.7f
        private const val LOW_CONFIDENCE = 0.5f
    }
    
    // Price inquiry patterns
    private val priceInquiryPatterns = mapOf(
        // English patterns
        "how much" to HIGH_CONFIDENCE,
        "what price" to HIGH_CONFIDENCE,
        "what's the price" to HIGH_CONFIDENCE,
        "how much is" to HIGH_CONFIDENCE,
        "how much does" to HIGH_CONFIDENCE,
        "what does it cost" to MEDIUM_CONFIDENCE,
        "price" to LOW_CONFIDENCE,
        
        // Ghana English patterns
        "how much be" to HIGH_CONFIDENCE,
        "how much be this" to HIGH_CONFIDENCE,
        "wetin be the price" to HIGH_CONFIDENCE,
        
        // Twi patterns
        "sɛn na ɛyɛ" to HIGH_CONFIDENCE,
        "ɛyɛ sɛn" to HIGH_CONFIDENCE,
        "sɛn" to MEDIUM_CONFIDENCE,
        
        // Ga patterns
        "bawo" to HIGH_CONFIDENCE,
        "naa kɛ" to HIGH_CONFIDENCE
    )
    
    // Price quote patterns (numbers + currency)
    private val priceQuotePatterns = listOf(
        Pattern.compile("\\b(\\d+(?:\\.\\d{1,2})?)\\s*(?:ghana\\s*)?cedis?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(\\d+(?:\\.\\d{1,2})?)\\s*(?:gh₵|ghc)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bgh₵\\s*(\\d+(?:\\.\\d{1,2})?)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(\\d+(?:\\.\\d{1,2})?)\\s*pesewas?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety|hundred)\\s*cedis?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(\\d+)\\s*(?:cedi|cedis)\\b", Pattern.CASE_INSENSITIVE)
    )
    
    // Negotiation patterns
    private val negotiationPatterns = mapOf(
        // English patterns
        "reduce" to HIGH_CONFIDENCE,
        "reduce small" to HIGH_CONFIDENCE,
        "reduce it" to HIGH_CONFIDENCE,
        "too much" to HIGH_CONFIDENCE,
        "too expensive" to HIGH_CONFIDENCE,
        "can you reduce" to HIGH_CONFIDENCE,
        "make it less" to MEDIUM_CONFIDENCE,
        "lower the price" to MEDIUM_CONFIDENCE,
        "my last price" to HIGH_CONFIDENCE,
        "final price" to MEDIUM_CONFIDENCE,
        "customer price" to HIGH_CONFIDENCE,
        "come down" to MEDIUM_CONFIDENCE,
        
        // Ghana English patterns
        "reduce small small" to HIGH_CONFIDENCE,
        "make am small" to HIGH_CONFIDENCE,
        "eii too much" to HIGH_CONFIDENCE,
        "chale reduce" to HIGH_CONFIDENCE,
        
        // Twi patterns
        "te so" to MEDIUM_CONFIDENCE,
        "yi bi fi" to HIGH_CONFIDENCE,
        
        // Ga patterns
        "kɛ te" to MEDIUM_CONFIDENCE
    )
    
    // Agreement patterns
    private val agreementPatterns = mapOf(
        // English patterns
        "okay" to MEDIUM_CONFIDENCE,
        "ok" to MEDIUM_CONFIDENCE,
        "fine" to MEDIUM_CONFIDENCE,
        "deal" to HIGH_CONFIDENCE,
        "agreed" to HIGH_CONFIDENCE,
        "i'll take it" to HIGH_CONFIDENCE,
        "i will take it" to HIGH_CONFIDENCE,
        "give me" to MEDIUM_CONFIDENCE,
        "i want" to MEDIUM_CONFIDENCE,
        "alright" to MEDIUM_CONFIDENCE,
        
        // Ghana English patterns
        "i go take am" to HIGH_CONFIDENCE,
        "give me that one" to HIGH_CONFIDENCE,
        "me paa" to MEDIUM_CONFIDENCE,
        
        // Twi patterns
        "ɛyɛ" to LOW_CONFIDENCE,
        "me pɛ" to MEDIUM_CONFIDENCE,
        "sɛ ɛyɛ a" to HIGH_CONFIDENCE,
        
        // Ga patterns
        "ɛyɛ" to LOW_CONFIDENCE,
        "mi lɛ" to MEDIUM_CONFIDENCE
    )
    
    // Payment patterns
    private val paymentPatterns = mapOf(
        // English patterns
        "here money" to HIGH_CONFIDENCE,
        "here is the money" to HIGH_CONFIDENCE,
        "take it" to MEDIUM_CONFIDENCE,
        "take the money" to HIGH_CONFIDENCE,
        "here you go" to MEDIUM_CONFIDENCE,
        "thank you" to LOW_CONFIDENCE,
        "thanks" to LOW_CONFIDENCE,
        "payment" to MEDIUM_CONFIDENCE,
        "pay" to LOW_CONFIDENCE,
        
        // Ghana English patterns
        "here be the money" to HIGH_CONFIDENCE,
        "take your money" to HIGH_CONFIDENCE,
        "collect your money" to HIGH_CONFIDENCE,
        
        // Twi patterns
        "gye wo sika" to HIGH_CONFIDENCE,
        "sika no ni" to HIGH_CONFIDENCE,
        "meda wo ase" to MEDIUM_CONFIDENCE,
        
        // Ga patterns
        "shi naa" to HIGH_CONFIDENCE,
        "oyiwala do" to MEDIUM_CONFIDENCE
    )
    
    // Cancellation patterns
    private val cancellationPatterns = mapOf(
        "no" to LOW_CONFIDENCE,
        "never mind" to HIGH_CONFIDENCE,
        "forget it" to HIGH_CONFIDENCE,
        "i don't want" to HIGH_CONFIDENCE,
        "cancel" to HIGH_CONFIDENCE,
        "not interested" to HIGH_CONFIDENCE,
        "maybe later" to MEDIUM_CONFIDENCE,
        "let me think" to MEDIUM_CONFIDENCE,
        
        // Ghana English
        "i no want" to HIGH_CONFIDENCE,
        "leave am" to HIGH_CONFIDENCE,
        
        // Twi
        "daabi" to HIGH_CONFIDENCE,
        "me mpɛ" to HIGH_CONFIDENCE
    )
    
    /**
     * Match price inquiry patterns
     */
    fun matchPriceInquiry(text: String): PatternMatchResult {
        return matchPatterns(text, priceInquiryPatterns, "PRICE_INQUIRY")
    }
    
    /**
     * Match price quote patterns
     */
    fun matchPriceQuote(text: String): PatternMatchResult {
        val normalizedText = text.lowercase()
        
        priceQuotePatterns.forEach { pattern ->
            val matcher = pattern.matcher(normalizedText)
            if (matcher.find()) {
                val amount = try {
                    matcher.group(1)?.toDoubleOrNull() ?: 0.0
                } catch (e: Exception) {
                    0.0
                }
                
                return PatternMatchResult(
                    matched = true,
                    confidence = HIGH_CONFIDENCE,
                    matchedText = matcher.group(0),
                    patternType = "PRICE_QUOTE",
                    extractedData = mapOf("amount" to amount)
                )
            }
        }
        
        return PatternMatchResult(false, 0f, "", "PRICE_QUOTE")
    }
    
    /**
     * Match negotiation patterns
     */
    fun matchNegotiation(text: String): PatternMatchResult {
        return matchPatterns(text, negotiationPatterns, "NEGOTIATION")
    }
    
    /**
     * Match agreement patterns
     */
    fun matchAgreement(text: String): PatternMatchResult {
        return matchPatterns(text, agreementPatterns, "AGREEMENT")
    }
    
    /**
     * Match payment patterns
     */
    fun matchPayment(text: String): PatternMatchResult {
        return matchPatterns(text, paymentPatterns, "PAYMENT")
    }
    
    /**
     * Match cancellation patterns
     */
    fun matchCancellation(text: String): PatternMatchResult {
        return matchPatterns(text, cancellationPatterns, "CANCELLATION")
    }
    
    /**
     * Extract product information from text
     */
    fun extractProduct(text: String): ProductExtractionResult {
        val normalizedText = text.lowercase()
        
        // Ghana fish names and their variants
        val fishPatterns = mapOf(
            "tilapia" to listOf("tilapia", "apateshi", "tuo"),
            "mackerel" to listOf("mackerel", "kpanla", "titus"),
            "sardines" to listOf("sardines", "sardin", "herring"),
            "red fish" to listOf("red fish", "adwene", "red snapper"),
            "catfish" to listOf("catfish", "sumbre", "mudfish"),
            "croaker" to listOf("croaker", "komi", "yellow croaker"),
            "salmon" to listOf("salmon", "pink salmon"),
            "tuna" to listOf("tuna", "light meat", "chunk light")
        )
        
        fishPatterns.forEach { (canonicalName, variants) ->
            variants.forEach { variant ->
                if (normalizedText.contains(variant)) {
                    return ProductExtractionResult(
                        found = true,
                        productName = canonicalName,
                        confidence = HIGH_CONFIDENCE,
                        matchedVariant = variant
                    )
                }
            }
        }
        
        return ProductExtractionResult(false, "", 0f, "")
    }
    
    /**
     * Extract quantity information from text
     */
    fun extractQuantity(text: String): QuantityExtractionResult {
        val quantityPatterns = listOf(
            Pattern.compile("\\b(\\d+)\\s*(?:pieces?|pcs?)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(\\d+)\\s*(?:bowls?|kokoo)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(\\d+)\\s*(?:buckets?|rubber)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(\\d+)\\s*(?:tins?|cans?)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(one|two|three|four|five|six|seven|eight|nine|ten)\\b", Pattern.CASE_INSENSITIVE)
        )
        
        quantityPatterns.forEach { pattern ->
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val quantityStr = matcher.group(1)
                val quantity = when (quantityStr?.lowercase()) {
                    "one" -> 1
                    "two" -> 2
                    "three" -> 3
                    "four" -> 4
                    "five" -> 5
                    "six" -> 6
                    "seven" -> 7
                    "eight" -> 8
                    "nine" -> 9
                    "ten" -> 10
                    else -> quantityStr?.toIntOrNull() ?: 1
                }
                
                return QuantityExtractionResult(
                    found = true,
                    quantity = quantity,
                    unit = extractUnit(matcher.group(0)),
                    confidence = HIGH_CONFIDENCE
                )
            }
        }
        
        return QuantityExtractionResult(false, 1, "", 0f)
    }
    
    private fun matchPatterns(text: String, patterns: Map<String, Float>, patternType: String): PatternMatchResult {
        val normalizedText = text.lowercase()
        var bestMatch: PatternMatchResult? = null
        
        patterns.forEach { (pattern, confidence) ->
            if (normalizedText.contains(pattern)) {
                val currentMatch = PatternMatchResult(
                    matched = true,
                    confidence = confidence,
                    matchedText = pattern,
                    patternType = patternType
                )
                
                if (bestMatch == null || currentMatch.confidence > bestMatch.confidence) {
                    bestMatch = currentMatch
                }
            }
        }
        
        return bestMatch ?: PatternMatchResult(false, 0f, "", patternType)
    }
    
    private fun extractUnit(matchedText: String): String {
        return when {
            matchedText.contains("piece", true) || matchedText.contains("pcs", true) -> "piece"
            matchedText.contains("bowl", true) || matchedText.contains("kokoo", true) -> "bowl"
            matchedText.contains("bucket", true) || matchedText.contains("rubber", true) -> "bucket"
            matchedText.contains("tin", true) || matchedText.contains("can", true) -> "tin"
            else -> "piece"
        }
    }
}

/**
 * Pattern matching result
 */
data class PatternMatchResult(
    val matched: Boolean,
    val confidence: Float,
    val matchedText: String,
    val patternType: String,
    val extractedData: Map<String, Any> = emptyMap()
)

/**
 * Product extraction result
 */
data class ProductExtractionResult(
    val found: Boolean,
    val productName: String,
    val confidence: Float,
    val matchedVariant: String
)

/**
 * Quantity extraction result
 */
data class QuantityExtractionResult(
    val found: Boolean,
    val quantity: Int,
    val unit: String,
    val confidence: Float
)