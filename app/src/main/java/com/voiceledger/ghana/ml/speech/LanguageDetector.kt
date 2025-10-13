package com.voiceledger.ghana.ml.speech

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Language detection utility for Ghana Voice Ledger
 * Detects English, Twi, and Ga languages in speech transcripts
 */
@Singleton
class LanguageDetector @Inject constructor() {
    
    companion object {
        private const val TAG = "LanguageDetector"
        
        // Language codes
        private const val ENGLISH = "en"
        private const val GHANA_ENGLISH = "en-GH"
        private const val TWI = "tw"
        private const val GA = "ga"
        
        // Confidence thresholds
        private const val HIGH_CONFIDENCE = 0.8f
        private const val MEDIUM_CONFIDENCE = 0.6f
        private const val LOW_CONFIDENCE = 0.4f
    }
    
    // Language pattern dictionaries
    private val twiPatterns = mapOf(
        // Common Twi words and phrases
        "sɛn na ɛyɛ" to 1.0f,
        "ɛyɛ sɛn" to 1.0f,
        "dɛn" to 0.8f,
        "wo ho te sɛn" to 1.0f,
        "me pɛ" to 0.9f,
        "ɛyɛ" to 0.7f,
        "sɛn" to 0.6f,
        "wo" to 0.3f,
        "me" to 0.3f,
        "ɛ" to 0.2f,
        "na" to 0.4f,
        "te" to 0.3f,
        "ho" to 0.3f,
        "pɛ" to 0.5f,
        "apateshi" to 0.9f,
        "tuo" to 0.8f,
        "kpanla" to 0.9f
    )
    
    private val gaPatterns = mapOf(
        // Common Ga words and phrases
        "bawo" to 1.0f,
        "naa" to 0.8f,
        "kɛ" to 0.7f,
        "mi" to 0.4f,
        "ni" to 0.3f,
        "lɛ" to 0.5f,
        "adwene" to 0.9f,
        "komi" to 0.8f,
        "sumbre" to 0.9f
    )
    
    private val ghanaEnglishPatterns = mapOf(
        // Ghana English specific phrases
        "how much" to 0.9f,
        "what price" to 0.9f,
        "reduce small" to 1.0f,
        "too much" to 0.7f,
        "my last price" to 1.0f,
        "customer price" to 1.0f,
        "I dey sell" to 1.0f,
        "how much be this" to 1.0f,
        "eii" to 0.8f,
        "chale" to 0.9f,
        "small small" to 0.8f,
        "plenty" to 0.6f,
        "dey" to 0.7f,
        "be" to 0.3f
    )
    
    private val englishPatterns = mapOf(
        // Standard English patterns
        "how much is" to 0.8f,
        "what is the price" to 0.9f,
        "can you reduce" to 0.8f,
        "too expensive" to 0.8f,
        "final price" to 0.8f,
        "thank you" to 0.6f,
        "okay" to 0.4f,
        "fine" to 0.4f,
        "deal" to 0.5f,
        "here is the money" to 0.9f,
        "take it" to 0.6f
    )
    
    /**
     * Detect the primary language in a transcript
     */
    fun detectLanguage(transcript: String): LanguageDetectionResult {
        if (transcript.isBlank()) {
            return LanguageDetectionResult(
                detectedLanguage = ENGLISH,
                confidence = 0f
            )
        }
        
        val normalizedText = transcript.lowercase().trim()
        
        // Calculate scores for each language
        val twiScore = calculateLanguageScore(normalizedText, twiPatterns)
        val gaScore = calculateLanguageScore(normalizedText, gaPatterns)
        val ghanaEnglishScore = calculateLanguageScore(normalizedText, ghanaEnglishPatterns)
        val englishScore = calculateLanguageScore(normalizedText, englishPatterns)
        
        // Determine the language with highest score
        val scores = mapOf(
            TWI to twiScore,
            GA to gaScore,
            GHANA_ENGLISH to ghanaEnglishScore,
            ENGLISH to englishScore
        )
        
        val (detectedLanguage, confidence) = scores.maxByOrNull { it.value } ?: (ENGLISH to 0f)
        
        // Create alternatives list
        val alternatives = scores
            .filter { it.key != detectedLanguage && it.value > LOW_CONFIDENCE }
            .map { (lang, score) ->
                LanguageAlternative(language = lang, confidence = score)
            }
            .sortedByDescending { it.confidence }
        
        Log.d(TAG, "Language detection - Text: '$transcript', Detected: $detectedLanguage, Confidence: $confidence")
        
        return LanguageDetectionResult(
            detectedLanguage = detectedLanguage,
            confidence = confidence,
            alternatives = alternatives
        )
    }
    
    /**
     * Detect code-switching in a transcript
     */
    fun detectCodeSwitching(transcript: String): CodeSwitchingResult {
        if (transcript.isBlank()) {
            return CodeSwitchingResult(
                hasCodeSwitching = false,
                languages = emptyList(),
                segments = emptyList()
            )
        }
        
        val words = transcript.lowercase().split("\\s+".toRegex())
        val segments = mutableListOf<LanguageSegment>()
        val detectedLanguages = mutableSetOf<String>()
        
        var currentLanguage = ENGLISH
        var currentSegmentStart = 0
        var currentSegmentWords = mutableListOf<String>()
        
        words.forEachIndexed { index, word ->
            val wordLanguage = detectWordLanguage(word)
            
            if (wordLanguage != currentLanguage && currentSegmentWords.isNotEmpty()) {
                // Language switch detected
                segments.add(
                    LanguageSegment(
                        text = currentSegmentWords.joinToString(" "),
                        language = currentLanguage,
                        startIndex = currentSegmentStart,
                        endIndex = index - 1,
                        confidence = 0.8f // Simplified confidence
                    )
                )
                
                detectedLanguages.add(currentLanguage)
                currentLanguage = wordLanguage
                currentSegmentStart = index
                currentSegmentWords.clear()
            }
            
            currentSegmentWords.add(word)
        }
        
        // Add final segment
        if (currentSegmentWords.isNotEmpty()) {
            segments.add(
                LanguageSegment(
                    text = currentSegmentWords.joinToString(" "),
                    language = currentLanguage,
                    startIndex = currentSegmentStart,
                    endIndex = words.size - 1,
                    confidence = 0.8f
                )
            )
            detectedLanguages.add(currentLanguage)
        }
        
        val hasCodeSwitching = detectedLanguages.size > 1
        
        Log.d(TAG, "Code-switching detection - Has switching: $hasCodeSwitching, Languages: $detectedLanguages")
        
        return CodeSwitchingResult(
            hasCodeSwitching = hasCodeSwitching,
            languages = detectedLanguages.toList(),
            segments = segments
        )
    }
    
    /**
     * Get language confidence level description
     */
    fun getConfidenceLevel(confidence: Float): String {
        return when {
            confidence >= HIGH_CONFIDENCE -> "High"
            confidence >= MEDIUM_CONFIDENCE -> "Medium"
            confidence >= LOW_CONFIDENCE -> "Low"
            else -> "Very Low"
        }
    }
    
    /**
     * Check if detected language is a Ghana language
     */
    fun isGhanaLanguage(languageCode: String): Boolean {
        return languageCode in listOf(GHANA_ENGLISH, TWI, GA)
    }
    
    /**
     * Get language display name
     */
    fun getLanguageDisplayName(languageCode: String): String {
        return when (languageCode) {
            ENGLISH -> "English"
            GHANA_ENGLISH -> "Ghana English"
            TWI -> "Twi"
            GA -> "Ga"
            else -> languageCode
        }
    }
    
    private fun calculateLanguageScore(text: String, patterns: Map<String, Float>): Float {
        var totalScore = 0f
        var matchCount = 0
        
        patterns.forEach { (pattern, weight) ->
            if (text.contains(pattern)) {
                totalScore += weight
                matchCount++
            }
        }
        
        // Normalize score based on text length and pattern matches
        val normalizedScore = if (matchCount > 0) {
            totalScore / (text.split("\\s+".toRegex()).size.coerceAtLeast(1))
        } else {
            0f
        }
        
        return normalizedScore.coerceAtMost(1f)
    }
    
    private fun detectWordLanguage(word: String): String {
        // Check each language pattern for the word
        val twiScore = twiPatterns[word] ?: 0f
        val gaScore = gaPatterns[word] ?: 0f
        val ghanaEnglishScore = ghanaEnglishPatterns[word] ?: 0f
        val englishScore = englishPatterns[word] ?: 0f
        
        return when {
            twiScore > 0.5f -> TWI
            gaScore > 0.5f -> GA
            ghanaEnglishScore > 0.5f -> GHANA_ENGLISH
            englishScore > 0.5f -> ENGLISH
            else -> ENGLISH // Default to English
        }
    }
}

/**
 * Code-switching detection result
 */
data class CodeSwitchingResult(
    val hasCodeSwitching: Boolean,
    val languages: List<String>,
    val segments: List<LanguageSegment>
)

/**
 * Language segment in code-switching
 */
data class LanguageSegment(
    val text: String,
    val language: String,
    val startIndex: Int,
    val endIndex: Int,
    val confidence: Float
)