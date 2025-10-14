package com.voiceledger.ghana.ml.transaction

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TransactionPatternMatcher
 * Tests pattern matching for Ghana-specific transaction phrases
 */
class TransactionPatternMatcherTest {
    
    private lateinit var patternMatcher: TransactionPatternMatcher
    
    @Before
    fun setUp() {
        patternMatcher = TransactionPatternMatcher()
    }
    
    @Test
    fun testDetectTransactionIntent_withProductMention_shouldReturnProductMention() {
        // Given
        val inputs = listOf(
            "I want to buy tilapia",
            "Do you have mackerel?",
            "Give me some sardines",
            "I need catfish",
            "Show me the red fish"
        )
        
        inputs.forEach { input ->
            // When
            val intent = patternMatcher.detectTransactionIntent(input)
            
            // Then
            assertEquals("Should detect product mention in: $input", 
                TransactionIntent.PRODUCT_MENTION, intent)
        }
    }
    
    @Test
    fun testDetectTransactionIntent_withQuantityMention_shouldReturnQuantityMention() {
        // Given
        val inputs = listOf(
            "Give me 3 pieces",
            "I want five tilapia",
            "Can I have 2 bowls?",
            "I need ten pieces",
            "Give me one bucket"
        )
        
        inputs.forEach { input ->
            // When
            val intent = patternMatcher.detectTransactionIntent(input)
            
            // Then
            assertEquals("Should detect quantity mention in: $input", 
                TransactionIntent.QUANTITY_MENTION, intent)
        }
    }
    
    @Test
    fun testDetectTransactionIntent_withPriceInquiry_shouldReturnPriceInquiry() {
        // Given
        val inputs = listOf(
            "How much is the tilapia?",
            "What's the price?",
            "How much does it cost?",
            "What be the price?", // Pidgin
            "How much you dey sell am?", // Pidgin
            "Ɛyɛ sɛn?" // Twi for "How much?"
        )
        
        inputs.forEach { input ->
            // When
            val intent = patternMatcher.detectTransactionIntent(input)
            
            // Then
            assertEquals("Should detect price inquiry in: $input", 
                TransactionIntent.PRICE_INQUIRY, intent)
        }
    }
    
    @Test
    fun testDetectTransactionIntent_withPriceOffer_shouldReturnPriceOffer() {
        // Given
        val inputs = listOf(
            "It's 15 cedis",
            "The price is 20 Ghana cedis",
            "That will be 25.50",
            "E be 10 cedis", // Pidgin
            "Na 12 cedis I dey sell am" // Pidgin
        )
        
        inputs.forEach { input ->
            // When
            val intent = patternMatcher.detectTransactionIntent(input)
            
            // Then
            assertEquals("Should detect price offer in: $input", 
                TransactionIntent.PRICE_OFFER, intent)
        }
    }
    
    @Test
    fun testDetectTransactionIntent_withPriceCounteroffer_shouldReturnPriceCounteroffer() {
        // Given
        val inputs = listOf(
            "Can you do 10 cedis?",
            "How about 8 cedis?",
            "Make it 12 cedis",
            "Too expensive, 15 cedis",
            "I will pay 20 cedis"
        )
        
        inputs.forEach { input ->
            // When
            val intent = patternMatcher.detectTransactionIntent(input)
            
            // Then
            assertEquals("Should detect price counteroffer in: $input", 
                TransactionIntent.PRICE_COUNTEROFFER, intent)
        }
    }
    
    @Test
    fun testDetectTransactionIntent_withPriceAgreement_shouldReturnPriceAgreement() {
        // Given
        val inputs = listOf(
            "OK, 15 cedis",
            "Agreed, 20 cedis",
            "Fine, I'll take it",
            "Deal",
            "Alright, that's good",
            "Ɛyɛ" // Twi for "OK/Good"
        )
        
        inputs.forEach { input ->
            // When
            val intent = patternMatcher.detectTransactionIntent(input)
            
            // Then
            assertEquals("Should detect price agreement in: $input", 
                TransactionIntent.PRICE_AGREEMENT, intent)
        }
    }
    
    @Test
    fun testDetectTransactionIntent_withTransactionCancellation_shouldReturnCancellation() {
        // Given
        val inputs = listOf(
            "Never mind",
            "I don't want it anymore",
            "Cancel that",
            "Forget it",
            "I changed my mind",
            "No, thank you"
        )
        
        inputs.forEach { input ->
            // When
            val intent = patternMatcher.detectTransactionIntent(input)
            
            // Then
            assertEquals("Should detect transaction cancellation in: $input", 
                TransactionIntent.TRANSACTION_CANCELLATION, intent)
        }
    }
    
    @Test
    fun testDetectTransactionIntent_withUnrelatedText_shouldReturnUnknown() {
        // Given
        val inputs = listOf(
            "Hello, how are you?",
            "Nice weather today",
            "What time is it?",
            "Random conversation",
            "Goodbye"
        )
        
        inputs.forEach { input ->
            // When
            val intent = patternMatcher.detectTransactionIntent(input)
            
            // Then
            assertEquals("Should return unknown for unrelated text: $input", 
                TransactionIntent.UNKNOWN, intent)
        }
    }
    
    @Test
    fun testExtractProduct_withValidProductMentions_shouldExtractCorrectly() {
        // Given
        val inputs = mapOf(
            "I want to buy tilapia" to "tilapia",
            "Do you have fresh mackerel?" to "mackerel",
            "Give me some sardines" to "sardines",
            "How much is the red fish?" to "red fish",
            "I need catfish today" to "catfish"
        )
        
        inputs.forEach { (input, expected) ->
            // When
            val product = patternMatcher.extractProduct(input)
            
            // Then
            assertEquals("Should extract product from: $input", expected, product)
        }
    }
    
    @Test
    fun testExtractProduct_withNoProductMention_shouldReturnNull() {
        // Given
        val input = "Hello, how are you today?"
        
        // When
        val product = patternMatcher.extractProduct(input)
        
        // Then
        assertNull("Should return null when no product mentioned", product)
    }
    
    @Test
    fun testExtractQuantity_withValidQuantityMentions_shouldExtractCorrectly() {
        // Given
        val inputs = mapOf(
            "Give me 3 pieces" to Pair(3, "pieces"),
            "I want five tilapia" to Pair(5, "pieces"),
            "Can I have 2 bowls?" to Pair(2, "bowls"),
            "I need ten pieces of fish" to Pair(10, "pieces"),
            "Give me one bucket" to Pair(1, "bucket")
        )
        
        inputs.forEach { (input, expected) ->
            // When
            val quantity = patternMatcher.extractQuantity(input)
            
            // Then
            assertNotNull("Should extract quantity from: $input", quantity)
            assertEquals("Should extract correct quantity", expected.first, quantity?.first)
            assertEquals("Should extract correct unit", expected.second, quantity?.second)
        }
    }
    
    @Test
    fun testExtractQuantity_withTwiNumbers_shouldExtractCorrectly() {
        // Given
        val inputs = mapOf(
            "Ma me baako" to Pair(1, "pieces"), // Give me one
            "Me pɛ mmienu" to Pair(2, "pieces"), // I want two
            "Ma me mmiɛnsa" to Pair(3, "pieces"), // Give me three
            "Me pɛ ɛnan" to Pair(4, "pieces"), // I want four
            "Ma me enum" to Pair(5, "pieces") // Give me five
        )
        
        inputs.forEach { (input, expected) ->
            // When
            val quantity = patternMatcher.extractQuantity(input)
            
            // Then
            assertNotNull("Should extract Twi quantity from: $input", quantity)
            assertEquals("Should extract correct Twi quantity", expected.first, quantity?.first)
        }
    }
    
    @Test
    fun testExtractQuantity_withNoQuantityMention_shouldReturnNull() {
        // Given
        val input = "I want to buy fish"
        
        // When
        val quantity = patternMatcher.extractQuantity(input)
        
        // Then
        assertNull("Should return null when no quantity mentioned", quantity)
    }
    
    @Test
    fun testExtractPrice_withValidPriceMentions_shouldExtractCorrectly() {
        // Given
        val inputs = mapOf(
            "That's 15 cedis" to 15.0,
            "It costs 25.50 cedis" to 25.50,
            "The price is 10 Ghana cedis" to 10.0,
            "Pay 5.75 GHS" to 5.75,
            "It's 20 pesewas" to 0.20,
            "50 pesewas only" to 0.50
        )
        
        inputs.forEach { (input, expected) ->
            // When
            val price = patternMatcher.extractPrice(input)
            
            // Then
            assertNotNull("Should extract price from: $input", price)
            assertEquals("Should extract correct price", expected, price, 0.01)
        }
    }
    
    @Test
    fun testExtractPrice_withLocalPhrases_shouldExtractCorrectly() {
        // Given
        val inputs = listOf(
            "E be 10 cedis", // Pidgin
            "The thing cost 5 cedis",
            "Na 15 cedis I dey sell am", // Pidgin
            "Make you pay 20 cedis"
        )
        
        inputs.forEach { input ->
            // When
            val price = patternMatcher.extractPrice(input)
            
            // Then
            assertNotNull("Should extract price from local phrase: $input", price)
            assertTrue("Should extract positive price", price!! > 0)
        }
    }
    
    @Test
    fun testExtractPrice_withNoPriceMention_shouldReturnNull() {
        // Given
        val input = "Do you have fish?"
        
        // When
        val price = patternMatcher.extractPrice(input)
        
        // Then
        assertNull("Should return null when no price mentioned", price)
    }
    
    @Test
    fun testExtractCustomer_withCustomerMentions_shouldExtractCorrectly() {
        // Given
        val inputs = mapOf(
            "Kwame wants to buy fish" to "Kwame",
            "Tell Ama the price" to "Ama",
            "Kofi is here" to "Kofi",
            "For Akosua" to "Akosua",
            "Yaw, come here" to "Yaw"
        )
        
        inputs.forEach { (input, expected) ->
            // When
            val customer = patternMatcher.extractCustomer(input)
            
            // Then
            assertEquals("Should extract customer from: $input", expected, customer)
        }
    }
    
    @Test
    fun testExtractCustomer_withNoCustomerMention_shouldReturnNull() {
        // Given
        val input = "I want to buy fish"
        
        // When
        val customer = patternMatcher.extractCustomer(input)
        
        // Then
        assertNull("Should return null when no customer mentioned", customer)
    }
    
    @Test
    fun testIsConfirmationPhrase_withConfirmationPhrases_shouldReturnTrue() {
        // Given
        val confirmationPhrases = listOf(
            "yes", "yeah", "ok", "okay", "alright", "sure",
            "agreed", "deal", "fine", "good", "ɛyɛ"
        )
        
        confirmationPhrases.forEach { phrase ->
            // When
            val isConfirmation = patternMatcher.isConfirmationPhrase(phrase)
            
            // Then
            assertTrue("Should recognize confirmation phrase: $phrase", isConfirmation)
        }
    }
    
    @Test
    fun testIsConfirmationPhrase_withNegationPhrases_shouldReturnFalse() {
        // Given
        val negationPhrases = listOf(
            "no", "nope", "never", "not", "daabi"
        )
        
        negationPhrases.forEach { phrase ->
            // When
            val isConfirmation = patternMatcher.isConfirmationPhrase(phrase)
            
            // Then
            assertFalse("Should not recognize negation as confirmation: $phrase", isConfirmation)
        }
    }
    
    @Test
    fun testIsNegationPhrase_withNegationPhrases_shouldReturnTrue() {
        // Given
        val negationPhrases = listOf(
            "no", "nope", "never", "not", "cancel", "stop", "daabi"
        )
        
        negationPhrases.forEach { phrase ->
            // When
            val isNegation = patternMatcher.isNegationPhrase(phrase)
            
            // Then
            assertTrue("Should recognize negation phrase: $phrase", isNegation)
        }
    }
    
    @Test
    fun testGetConfidenceScore_shouldReturnReasonableScores() {
        // Given
        val highConfidenceInputs = listOf(
            "I want to buy 3 pieces of tilapia for 15 cedis",
            "Give me 5 bowls of mackerel"
        )
        
        val lowConfidenceInputs = listOf(
            "Maybe fish?",
            "Hmm, not sure"
        )
        
        // When & Then
        highConfidenceInputs.forEach { input ->
            val confidence = patternMatcher.getConfidenceScore(input)
            assertTrue("Should have high confidence for: $input", confidence > 0.7f)
        }
        
        lowConfidenceInputs.forEach { input ->
            val confidence = patternMatcher.getConfidenceScore(input)
            assertTrue("Should have low confidence for: $input", confidence < 0.5f)
        }
    }
}