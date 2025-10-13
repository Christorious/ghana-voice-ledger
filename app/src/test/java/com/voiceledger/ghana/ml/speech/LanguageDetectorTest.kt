package com.voiceledger.ghana.ml.speech

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for LanguageDetector
 * Tests Ghana-specific language detection capabilities
 */
class LanguageDetectorTest {
    
    private lateinit var languageDetector: LanguageDetector
    
    @Before
    fun setUp() {
        languageDetector = LanguageDetector()
    }
    
    @Test
    fun `detectLanguage should identify Twi phrases correctly`() {
        // Given
        val twiTranscript = "sɛn na ɛyɛ apateshi no"
        
        // When
        val result = languageDetector.detectLanguage(twiTranscript)
        
        // Then
        assertEquals("tw", result.detectedLanguage)
        assertTrue("Confidence should be high for clear Twi", result.confidence > 0.6f)
    }
    
    @Test
    fun `detectLanguage should identify Ga phrases correctly`() {
        // Given
        val gaTranscript = "bawo naa adwene lɛ"
        
        // When
        val result = languageDetector.detectLanguage(gaTranscript)
        
        // Then
        assertEquals("ga", result.detectedLanguage)
        assertTrue("Confidence should be high for clear Ga", result.confidence > 0.6f)
    }
    
    @Test
    fun `detectLanguage should identify Ghana English correctly`() {
        // Given
        val ghanaEnglishTranscript = "how much be this fish reduce small"
        
        // When
        val result = languageDetector.detectLanguage(ghanaEnglishTranscript)
        
        // Then
        assertEquals("en-GH", result.detectedLanguage)
        assertTrue("Confidence should be high for Ghana English", result.confidence > 0.6f)
    }
    
    @Test
    fun `detectLanguage should identify standard English correctly`() {
        // Given
        val englishTranscript = "what is the price of this fish can you reduce it"
        
        // When
        val result = languageDetector.detectLanguage(englishTranscript)
        
        // Then
        assertEquals("en", result.detectedLanguage)
        assertTrue("Confidence should be reasonable for English", result.confidence > 0.4f)
    }
    
    @Test
    fun `detectLanguage should handle empty transcript`() {
        // Given
        val emptyTranscript = ""
        
        // When
        val result = languageDetector.detectLanguage(emptyTranscript)
        
        // Then
        assertEquals("en", result.detectedLanguage)
        assertEquals(0f, result.confidence)
    }
    
    @Test
    fun `detectLanguage should handle mixed language transcript`() {
        // Given
        val mixedTranscript = "how much sɛn na ɛyɛ this fish"
        
        // When
        val result = languageDetector.detectLanguage(mixedTranscript)
        
        // Then
        assertNotNull(result.detectedLanguage)
        assertTrue("Should detect some language", result.confidence > 0f)
        assertTrue("Should have alternatives for mixed language", result.alternatives.isNotEmpty())
    }
    
    @Test
    fun `detectCodeSwitching should detect single language`() {
        // Given
        val singleLanguageTranscript = "how much is this fish"
        
        // When
        val result = languageDetector.detectCodeSwitching(singleLanguageTranscript)
        
        // Then
        assertFalse("Should not detect code-switching", result.hasCodeSwitching)
        assertEquals(1, result.languages.size)
        assertEquals(1, result.segments.size)
    }
    
    @Test
    fun `detectCodeSwitching should detect language switching`() {
        // Given
        val codeSwitchingTranscript = "how much sɛn na ɛyɛ apateshi"
        
        // When
        val result = languageDetector.detectCodeSwitching(codeSwitchingTranscript)
        
        // Then
        assertTrue("Should detect code-switching", result.hasCodeSwitching)
        assertTrue("Should detect multiple languages", result.languages.size > 1)
        assertTrue("Should have multiple segments", result.segments.size > 1)
    }
    
    @Test
    fun `detectCodeSwitching should handle empty transcript`() {
        // Given
        val emptyTranscript = ""
        
        // When
        val result = languageDetector.detectCodeSwitching(emptyTranscript)
        
        // Then
        assertFalse("Should not detect code-switching for empty text", result.hasCodeSwitching)
        assertTrue("Should have no languages", result.languages.isEmpty())
        assertTrue("Should have no segments", result.segments.isEmpty())
    }
    
    @Test
    fun `getConfidenceLevel should return correct levels`() {
        // Test high confidence
        assertEquals("High", languageDetector.getConfidenceLevel(0.9f))
        
        // Test medium confidence
        assertEquals("Medium", languageDetector.getConfidenceLevel(0.7f))
        
        // Test low confidence
        assertEquals("Low", languageDetector.getConfidenceLevel(0.5f))
        
        // Test very low confidence
        assertEquals("Very Low", languageDetector.getConfidenceLevel(0.2f))
    }
    
    @Test
    fun `isGhanaLanguage should identify Ghana languages correctly`() {
        // Test Ghana languages
        assertTrue(languageDetector.isGhanaLanguage("en-GH"))
        assertTrue(languageDetector.isGhanaLanguage("tw"))
        assertTrue(languageDetector.isGhanaLanguage("ga"))
        
        // Test non-Ghana languages
        assertFalse(languageDetector.isGhanaLanguage("en"))
        assertFalse(languageDetector.isGhanaLanguage("fr"))
        assertFalse(languageDetector.isGhanaLanguage("es"))
    }
    
    @Test
    fun `getLanguageDisplayName should return correct names`() {
        assertEquals("English", languageDetector.getLanguageDisplayName("en"))
        assertEquals("Ghana English", languageDetector.getLanguageDisplayName("en-GH"))
        assertEquals("Twi", languageDetector.getLanguageDisplayName("tw"))
        assertEquals("Ga", languageDetector.getLanguageDisplayName("ga"))
        assertEquals("unknown", languageDetector.getLanguageDisplayName("unknown"))
    }
    
    @Test
    fun `detectLanguage should handle market-specific phrases`() {
        // Test common market phrases
        val marketPhrases = mapOf(
            "reduce small" to "en-GH",
            "customer price" to "en-GH",
            "my last price" to "en-GH",
            "I dey sell" to "en-GH",
            "apateshi" to "tw",
            "kpanla" to "tw",
            "adwene" to "ga",
            "sumbre" to "ga"
        )
        
        marketPhrases.forEach { (phrase, expectedLanguage) ->
            val result = languageDetector.detectLanguage(phrase)
            assertEquals("Failed for phrase: $phrase", expectedLanguage, result.detectedLanguage)
            assertTrue("Low confidence for phrase: $phrase", result.confidence > 0.5f)
        }
    }
    
    @Test
    fun `detectLanguage should provide alternatives for ambiguous text`() {
        // Given - text that could be multiple languages
        val ambiguousTranscript = "how much me pɛ"
        
        // When
        val result = languageDetector.detectLanguage(ambiguousTranscript)
        
        // Then
        assertNotNull(result.detectedLanguage)
        assertTrue("Should have alternatives for ambiguous text", result.alternatives.isNotEmpty())
        
        // Alternatives should be sorted by confidence
        if (result.alternatives.size > 1) {
            assertTrue("Alternatives should be sorted by confidence", 
                result.alternatives[0].confidence >= result.alternatives[1].confidence)
        }
    }
}