package com.voiceledger.ghana.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.voiceledger.ghana.ml.entity.EntityExtractionService
import com.voiceledger.ghana.ml.speech.LanguageDetector
import com.voiceledger.ghana.ml.speech.SpeechRecognitionManager
import com.voiceledger.ghana.ml.transaction.TransactionProcessor
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Integration tests for multi-language support and code-switching scenarios
 * Tests the system's ability to handle English, Twi, and Pidgin mixed conversations
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MultiLanguageIntegrationTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var languageDetector: LanguageDetector
    
    @Inject
    lateinit var speechRecognitionManager: SpeechRecognitionManager
    
    @Inject
    lateinit var entityExtractionService: EntityExtractionService
    
    @Inject
    lateinit var transactionProcessor: TransactionProcessor
    
    @Before
    fun setUp() {
        hiltRule.inject()
    }
    
    @Test
    fun testEnglishToTwiCodeSwitching_shouldExtractEntitiesCorrectly() = runTest {
        // Given - mixed English and Twi phrase
        val codeSwitchedText = "I want tilapia, me pɛ mmiɛnsa" // "I want tilapia, I want three"
        
        // When
        val detectedLanguages = languageDetector.detectLanguages(codeSwitchedText)
        val entities = entityExtractionService.extractEntities(codeSwitchedText)
        val transaction = transactionProcessor.processTransaction(codeSwitchedText, "test_speaker")
        
        // Then
        assertTrue("Should detect multiple languages", detectedLanguages.size > 1)
        assertTrue("Should detect English", detectedLanguages.any { it.language == "en" })
        assertTrue("Should detect Twi", detectedLanguages.any { it.language == "tw" })
        
        assertNotNull("Should extract entities", entities)
        assertEquals("Should extract product", "tilapia", entities?.product?.name)
        assertEquals("Should extract quantity", 3, entities?.quantity?.quantity)
        
        assertNotNull("Should process transaction", transaction)
        assertEquals("Should have correct product", "tilapia", transaction?.productName)
        assertEquals("Should have correct quantity", 3, transaction?.quantity)
    }
    
    @Test
    fun testPidginEnglishMix_shouldHandleCorrectly() = runTest {
        // Given - Pidgin English mixed with standard English
        val pidginText = "How much be the fish? I want am for 10 cedis"
        
        // When
        val detectedLanguages = languageDetector.detectLanguages(pidginText)
        val entities = entityExtractionService.extractEntities(pidginText)
        val transaction = transactionProcessor.processTransaction(pidginText, "test_speaker")
        
        // Then
        assertTrue("Should detect language variations", detectedLanguages.isNotEmpty())
        
        assertNotNull("Should extract entities from pidgin", entities)
        assertEquals("Should extract product", "fish", entities?.product?.name)
        assertEquals("Should extract price", 10.0, entities?.price?.amount, 0.01)
        
        assertNotNull("Should process pidgin transaction", transaction)
        assertEquals("Should have correct price", 10.0, transaction?.totalPrice, 0.01)
    }
    
    @Test
    fun testTrilingualConversation_shouldMaintainContext() = runTest {
        // Given - conversation mixing all three languages
        val phrases = listOf(
            "Hello, me pɛ fish", // English + Twi
            "How much you dey sell am?", // Pidgin
            "Give me mmiɛnsa pieces", // English + Twi + English
            "That be 15 cedis" // Pidgin + English
        )
        
        var cumulativeContext = ""
        val transactions = mutableListOf<com.voiceledger.ghana.data.local.entity.Transaction?>()
        
        // When - process each phrase in sequence
        phrases.forEach { phrase ->
            cumulativeContext += " $phrase"
            val transaction = transactionProcessor.processTransaction(cumulativeContext.trim(), "test_speaker")
            transactions.add(transaction)
        }
        
        // Then
        val finalTransaction = transactions.lastOrNull()
        assertNotNull("Should create final transaction", finalTransaction)
        assertEquals("Should extract product from multilingual context", "fish", finalTransaction?.productName)
        assertEquals("Should extract quantity from multilingual context", 3, finalTransaction?.quantity)
        assertEquals("Should extract price from multilingual context", 15.0, finalTransaction?.totalPrice, 0.01)
    }
    
    @Test
    fun testLanguageDetectionAccuracy_withVariousInputs() = runTest {
        // Given - various language inputs
        val testCases = mapOf(
            "I want to buy fish" to "en",
            "Me pɛ nam" to "tw", // "I want fish" in Twi
            "How much you dey sell am?" to "pcm", // Pidgin
            "Ɛyɛ sɛn?" to "tw", // "How much?" in Twi
            "Give me some tilapia" to "en"
        )
        
        testCases.forEach { (text, expectedLanguage) ->
            // When
            val detectedLanguages = languageDetector.detectLanguages(text)
            
            // Then
            assertTrue("Should detect language for: $text", detectedLanguages.isNotEmpty())
            val primaryLanguage = detectedLanguages.maxByOrNull { it.confidence }
            assertEquals("Should detect correct primary language for: $text", 
                expectedLanguage, primaryLanguage?.language)
        }
    }
    
    @Test
    fun testEntityExtractionAcrossLanguages_shouldNormalizeCorrectly() = runTest {
        // Given - same entities expressed in different languages
        val testCases = listOf(
            "I want tilapia" to "tilapia",
            "Me pɛ tilapia" to "tilapia", // Twi + English
            "Give me kɔtɔ" to "crab", // Twi
            "I need nam" to "fish", // English + Twi
            "How much be the red-red?" to "red fish" // Pidgin + local term
        )
        
        testCases.forEach { (text, expectedProduct) ->
            // When
            val entities = entityExtractionService.extractEntities(text)
            
            // Then
            assertNotNull("Should extract entities from: $text", entities)
            assertEquals("Should normalize product correctly for: $text", 
                expectedProduct, entities?.product?.name)
        }
    }
    
    @Test
    fun testNumberExtractionAcrossLanguages_shouldHandleAllFormats() = runTest {
        // Given - numbers in different languages
        val testCases = listOf(
            "Give me three pieces" to 3,
            "Ma me mmiɛnsa" to 3, // "Give me three" in Twi
            "I want 5 bowls" to 5,
            "Me pɛ enum" to 5, // "I want five" in Twi
            "Give me ten pieces" to 10,
            "Ma me du" to 10 // "Give me ten" in Twi
        )
        
        testCases.forEach { (text, expectedQuantity) ->
            // When
            val entities = entityExtractionService.extractEntities(text)
            
            // Then
            assertNotNull("Should extract quantity from: $text", entities)
            assertEquals("Should extract correct quantity for: $text", 
                expectedQuantity, entities?.quantity?.quantity)
        }
    }
    
    @Test
    fun testPriceExtractionWithCurrencyVariations_shouldNormalizeToGHS() = runTest {
        // Given - price expressions in different formats
        val testCases = listOf(
            "That's 15 cedis" to 15.0,
            "E be 20 Ghana cedis" to 20.0, // Pidgin
            "Pay 25.50 GHS" to 25.50,
            "It cost 10 pesewas" to 0.10,
            "Na 30 cedis I dey sell am" to 30.0 // Pidgin
        )
        
        testCases.forEach { (text, expectedPrice) ->
            // When
            val entities = entityExtractionService.extractEntities(text)
            
            // Then
            assertNotNull("Should extract price from: $text", entities)
            assertEquals("Should extract correct price for: $text", 
                expectedPrice, entities?.price?.amount, 0.01)
            assertEquals("Should normalize currency to GHS", "GHS", entities?.price?.currency)
        }
    }
}