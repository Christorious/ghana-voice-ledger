package com.voiceledger.ghana.ml.entity

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

/**
 * Unit tests for GhanaEntityExtractor
 * Tests Ghana-specific phrases and variations
 */
class GhanaEntityExtractorTest {
    
    @Mock
    private lateinit var mockNormalizer: EntityNormalizer
    
    private lateinit var extractor: GhanaEntityExtractor
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        extractor = GhanaEntityExtractor(mockNormalizer)
    }
    
    @Test
    fun testExtractProduct_withEnglishFishNames_shouldExtractCorrectly() = runTest {
        // Given
        val inputs = listOf(
            "I want to buy tilapia",
            "Do you have mackerel?",
            "How much is the sardines?",
            "Give me some red fish",
            "I need catfish today"
        )
        
        val expectedProducts = listOf("tilapia", "mackerel", "sardines", "red fish", "catfish")
        
        inputs.forEachIndexed { index, input ->
            whenever(mockNormalizer.normalizeProduct(expectedProducts[index]))
                .thenReturn(expectedProducts[index])
            
            // When
            val result = extractor.extractProduct(input)
            
            // Then
            assertNotNull("Should extract product from: $input", result)
            assertEquals("Should extract correct product", expectedProducts[index], result?.name)
        }
    }
    
    @Test
    fun testExtractProduct_withTwiNames_shouldExtractCorrectly() = runTest {
        // Given - Twi names for fish
        val inputs = listOf(
            "Me pɛ tilapia", // I want tilapia
            "Wɔ wɔ kɔtɔ anaa?", // Do you have crab?
            "Patuo no yɛ sɛn?", // How much is the duck?
        )
        
        whenever(mockNormalizer.normalizeProduct("tilapia")).thenReturn("tilapia")
        whenever(mockNormalizer.normalizeProduct("kɔtɔ")).thenReturn("crab")
        whenever(mockNormalizer.normalizeProduct("patuo")).thenReturn("duck")
        
        // When & Then
        val result1 = extractor.extractProduct(inputs[0])
        assertNotNull("Should extract Twi tilapia", result1)
        assertEquals("Should normalize Twi product", "tilapia", result1?.name)
        
        val result2 = extractor.extractProduct(inputs[1])
        assertNotNull("Should extract Twi crab", result2)
        assertEquals("Should normalize Twi product", "crab", result2?.name)
    }
    
    @Test
    fun testExtractProduct_withLocalVariations_shouldExtractCorrectly() = runTest {
        // Given - Local variations and pronunciations
        val inputs = listOf(
            "I want some tila", // Shortened tilapia
            "Give me red-red", // Local term for red fish
            "How much be the kote?", // Pidgin for crab
            "I dey find sardine", // Pidgin for sardines
        )
        
        whenever(mockNormalizer.normalizeProduct("tila")).thenReturn("tilapia")
        whenever(mockNormalizer.normalizeProduct("red-red")).thenReturn("red fish")
        whenever(mockNormalizer.normalizeProduct("kote")).thenReturn("crab")
        whenever(mockNormalizer.normalizeProduct("sardine")).thenReturn("sardines")
        
        inputs.forEach { input ->
            // When
            val result = extractor.extractProduct(input)
            
            // Then
            assertNotNull("Should extract product from local variation: $input", result)
        }
    }
    
    @Test
    fun testExtractQuantity_withEnglishNumbers_shouldExtractCorrectly() = runTest {
        // Given
        val inputs = listOf(
            "Give me 3 pieces",
            "I want five tilapia",
            "Can I have 2 bowls?",
            "I need ten pieces of fish",
            "Give me one bucket"
        )
        
        val expectedQuantities = listOf(3, 5, 2, 10, 1)
        val expectedUnits = listOf("pieces", "pieces", "bowls", "pieces", "bucket")
        
        inputs.forEachIndexed { index, input ->
            // When
            val result = extractor.extractQuantity(input)
            
            // Then
            assertNotNull("Should extract quantity from: $input", result)
            assertEquals("Should extract correct quantity", expectedQuantities[index], result?.quantity)
            assertEquals("Should extract correct unit", expectedUnits[index], result?.unit)
        }
    }
    
    @Test
    fun testExtractQuantity_withTwiNumbers_shouldExtractCorrectly() = runTest {
        // Given - Twi numbers
        val inputs = listOf(
            "Ma me baako", // Give me one
            "Me pɛ mmienu", // I want two
            "Ma me mmiɛnsa", // Give me three
            "Me pɛ ɛnan", // I want four
            "Ma me enum" // Give me five
        )
        
        val expectedQuantities = listOf(1, 2, 3, 4, 5)
        
        inputs.forEachIndexed { index, input ->
            // When
            val result = extractor.extractQuantity(input)
            
            // Then
            assertNotNull("Should extract Twi quantity from: $input", result)
            assertEquals("Should extract correct Twi quantity", expectedQuantities[index], result?.quantity)
        }
    }
    
    @Test
    fun testExtractPrice_withCedisAndPesewas_shouldExtractCorrectly() = runTest {
        // Given
        val inputs = listOf(
            "That's 15 cedis",
            "It costs 25.50 cedis",
            "The price is 10 Ghana cedis",
            "Pay 5.75 GHS",
            "It's 20 pesewas",
            "50 pesewas only"
        )
        
        val expectedPrices = listOf(15.0, 25.50, 10.0, 5.75, 0.20, 0.50)
        
        inputs.forEachIndexed { index, input ->
            // When
            val result = extractor.extractPrice(input)
            
            // Then
            assertNotNull("Should extract price from: $input", result)
            assertEquals("Should extract correct price", expectedPrices[index], result?.amount, 0.01)
            assertEquals("Should use GHS currency", "GHS", result?.currency)
        }
    }
    
    @Test
    fun testExtractPrice_withLocalPhrases_shouldExtractCorrectly() = runTest {
        // Given - Local pricing phrases
        val inputs = listOf(
            "E be 10 cedis", // Pidgin
            "The thing cost 5 cedis",
            "Na 15 cedis I dey sell am", // Pidgin
            "Make you pay 20 cedis",
            "The price na 12.50"
        )
        
        inputs.forEach { input ->
            // When
            val result = extractor.extractPrice(input)
            
            // Then
            assertNotNull("Should extract price from local phrase: $input", result)
            assertTrue("Should extract positive price", result!!.amount > 0)
        }
    }
    
    @Test
    fun testExtractCustomer_withGhanaianNames_shouldExtractCorrectly() = runTest {
        // Given - Common Ghanaian names
        val inputs = listOf(
            "Kwame wants to buy fish",
            "Ama is here for tilapia",
            "Tell Kofi the price",
            "Akosua, how much you want?",
            "Yaw, come and buy"
        )
        
        val expectedNames = listOf("Kwame", "Ama", "Kofi", "Akosua", "Yaw")
        
        inputs.forEachIndexed { index, input ->
            // When
            val result = extractor.extractCustomer(input)
            
            // Then
            assertNotNull("Should extract customer name from: $input", result)
            assertEquals("Should extract correct name", expectedNames[index], result?.name)
        }
    }
    
    @Test
    fun testExtractLocation_withGhanaianPlaces_shouldExtractCorrectly() = runTest {
        // Given - Ghanaian locations
        val inputs = listOf(
            "Deliver to Accra",
            "I'm from Kumasi",
            "Send it to Tema",
            "I live in Cape Coast",
            "Bring it to Tamale"
        )
        
        val expectedLocations = listOf("Accra", "Kumasi", "Tema", "Cape Coast", "Tamale")
        
        inputs.forEachIndexed { index, input ->
            // When
            val result = extractor.extractLocation(input)
            
            // Then
            assertNotNull("Should extract location from: $input", result)
            assertEquals("Should extract correct location", expectedLocations[index], result?.name)
        }
    }
    
    @Test
    fun testExtractTime_withLocalTimeExpressions_shouldExtractCorrectly() = runTest {
        // Given - Local time expressions
        val inputs = listOf(
            "Come back tomorrow",
            "I will pay you later",
            "Bring it this evening",
            "See you next week",
            "Come in the morning"
        )
        
        inputs.forEach { input ->
            // When
            val result = extractor.extractTime(input)
            
            // Then
            assertNotNull("Should extract time reference from: $input", result)
        }
    }
    
    @Test
    fun testExtractProduct_withNoProduct_shouldReturnNull() = runTest {
        // Given
        val input = "Hello, how are you today?"
        
        // When
        val result = extractor.extractProduct(input)
        
        // Then
        assertNull("Should return null when no product mentioned", result)
    }
    
    @Test
    fun testExtractQuantity_withNoQuantity_shouldReturnNull() = runTest {
        // Given
        val input = "I want to buy fish"
        
        // When
        val result = extractor.extractQuantity(input)
        
        // Then
        assertNull("Should return null when no quantity mentioned", result)
    }
    
    @Test
    fun testExtractPrice_withNoPrice_shouldReturnNull() = runTest {
        // Given
        val input = "Do you have fish?"
        
        // When
        val result = extractor.extractPrice(input)
        
        // Then
        assertNull("Should return null when no price mentioned", result)
    }
    
    @Test
    fun testExtractMultipleEntities_fromComplexSentence_shouldExtractAll() = runTest {
        // Given
        val input = "Kwame wants 3 pieces of tilapia for 15 cedis"
        whenever(mockNormalizer.normalizeProduct("tilapia")).thenReturn("tilapia")
        
        // When
        val product = extractor.extractProduct(input)
        val quantity = extractor.extractQuantity(input)
        val price = extractor.extractPrice(input)
        val customer = extractor.extractCustomer(input)
        
        // Then
        assertNotNull("Should extract product", product)
        assertEquals("Should extract tilapia", "tilapia", product?.name)
        
        assertNotNull("Should extract quantity", quantity)
        assertEquals("Should extract 3 pieces", 3, quantity?.quantity)
        assertEquals("Should extract pieces unit", "pieces", quantity?.unit)
        
        assertNotNull("Should extract price", price)
        assertEquals("Should extract 15 cedis", 15.0, price?.amount, 0.01)
        
        assertNotNull("Should extract customer", customer)
        assertEquals("Should extract Kwame", "Kwame", customer?.name)
    }
    
    @Test
    fun testExtractProduct_withCodeSwitching_shouldHandleMixedLanguages() = runTest {
        // Given - Code-switching between English and Twi
        val inputs = listOf(
            "I want tilapia, me pɛ tilapia", // English-Twi mix
            "Give me fish, ma me nam", // English-Twi mix
            "How much be the kɔtɔ?", // Pidgin-Twi mix
        )
        
        whenever(mockNormalizer.normalizeProduct("tilapia")).thenReturn("tilapia")
        whenever(mockNormalizer.normalizeProduct("nam")).thenReturn("fish")
        whenever(mockNormalizer.normalizeProduct("kɔtɔ")).thenReturn("crab")
        
        inputs.forEach { input ->
            // When
            val result = extractor.extractProduct(input)
            
            // Then
            assertNotNull("Should handle code-switching in: $input", result)
        }
    }
    
    @Test
    fun testExtractQuantity_withApproximateNumbers_shouldExtractCorrectly() = runTest {
        // Given - Approximate quantities common in markets
        val inputs = listOf(
            "Give me about 5 pieces",
            "I want around 3 bowls",
            "Maybe 2 buckets",
            "Roughly 10 pieces"
        )
        
        inputs.forEach { input ->
            // When
            val result = extractor.extractQuantity(input)
            
            // Then
            assertNotNull("Should extract approximate quantity from: $input", result)
            assertTrue("Should extract positive quantity", result!!.quantity > 0)
        }
    }
}