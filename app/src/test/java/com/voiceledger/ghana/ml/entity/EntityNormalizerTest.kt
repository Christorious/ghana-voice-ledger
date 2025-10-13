package com.voiceledger.ghana.ml.entity

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for EntityNormalizer
 * Tests normalization of Ghana-specific entities
 */
class EntityNormalizerTest {
    
    private lateinit var normalizer: EntityNormalizer
    
    @Before
    fun setUp() {
        normalizer = EntityNormalizer()
    }
    
    @Test
    fun testNormalizeProduct_withStandardNames_shouldReturnNormalized() {
        // Given
        val inputs = mapOf(
            "tilapia" to "tilapia",
            "TILAPIA" to "tilapia",
            "Tilapia" to "tilapia",
            "mackerel" to "mackerel",
            "MACKEREL" to "mackerel",
            "sardines" to "sardines",
            "red fish" to "red fish",
            "catfish" to "catfish"
        )
        
        inputs.forEach { (input, expected) ->
            // When
            val result = normalizer.normalizeProduct(input)
            
            // Then
            assertEquals("Should normalize $input to $expected", expected, result)
        }
    }
    
    @Test
    fun testNormalizeProduct_withLocalVariations_shouldReturnStandardNames() {
        // Given
        val inputs = mapOf(
            "tila" to "tilapia",
            "red-red" to "red fish",
            "kote" to "crab",
            "kɔtɔ" to "crab",
            "nam" to "fish",
            "sardine" to "sardines",
            "mackrel" to "mackerel", // Common misspelling
            "cat fish" to "catfish"
        )
        
        inputs.forEach { (input, expected) ->
            // When
            val result = normalizer.normalizeProduct(input)
            
            // Then
            assertEquals("Should normalize local variation $input to $expected", expected, result)
        }
    }
    
    @Test
    fun testNormalizeProduct_withTwiNames_shouldReturnEnglishEquivalents() {
        // Given
        val inputs = mapOf(
            "patuo" to "duck",
            "akoko" to "chicken",
            "aboa" to "animal",
            "aponkye" to "goat"
        )
        
        inputs.forEach { (input, expected) ->
            // When
            val result = normalizer.normalizeProduct(input)
            
            // Then
            assertEquals("Should normalize Twi name $input to $expected", expected, result)
        }
    }
    
    @Test
    fun testNormalizeQuantity_withNumbers_shouldReturnCorrectValues() {
        // Given
        val inputs = mapOf(
            "one" to 1,
            "two" to 2,
            "three" to 3,
            "four" to 4,
            "five" to 5,
            "ten" to 10,
            "twenty" to 20,
            "hundred" to 100
        )
        
        inputs.forEach { (input, expected) ->
            // When
            val result = normalizer.normalizeQuantity(input)
            
            // Then
            assertEquals("Should normalize $input to $expected", expected, result)
        }
    }
    
    @Test
    fun testNormalizeQuantity_withTwiNumbers_shouldReturnCorrectValues() {
        // Given
        val inputs = mapOf(
            "baako" to 1,
            "mmienu" to 2,
            "mmiɛnsa" to 3,
            "ɛnan" to 4,
            "enum" to 5,
            "nsia" to 6,
            "nson" to 7,
            "nwɔtwe" to 8,
            "nkron" to 9,
            "du" to 10
        )
        
        inputs.forEach { (input, expected) ->
            // When
            val result = normalizer.normalizeQuantity(input)
            
            // Then
            assertEquals("Should normalize Twi number $input to $expected", expected, result)
        }
    }
    
    @Test
    fun testNormalizeUnit_withStandardUnits_shouldReturnNormalized() {
        // Given
        val inputs = mapOf(
            "piece" to "pieces",
            "pieces" to "pieces",
            "PIECES" to "pieces",
            "bowl" to "bowls",
            "bowls" to "bowls",
            "bucket" to "buckets",
            "buckets" to "buckets",
            "tin" to "tins",
            "tins" to "tins"
        )
        
        inputs.forEach { (input, expected) ->
            // When
            val result = normalizer.normalizeUnit(input)
            
            // Then
            assertEquals("Should normalize unit $input to $expected", expected, result)
        }
    }
    
    @Test
    fun testNormalizeUnit_withLocalVariations_shouldReturnStandardUnits() {
        // Given
        val inputs = mapOf(
            "pc" to "pieces",
            "pcs" to "pieces",
            "pce" to "pieces",
            "bwl" to "bowls",
            "bkt" to "buckets",
            "container" to "buckets",
            "can" to "tins"
        )
        
        inputs.forEach { (input, expected) ->
            // When
            val result = normalizer.normalizeUnit(input)
            
            // Then
            assertEquals("Should normalize local unit $input to $expected", expected, result)
        }
    }
    
    @Test
    fun testNormalizeCurrency_withVariousCurrencyFormats_shouldReturnGHS() {
        // Given
        val inputs = listOf(
            "cedis", "cedi", "ghana cedis", "ghana cedi",
            "GHS", "ghs", "Ghs", "GH₵", "₵"
        )
        
        inputs.forEach { input ->
            // When
            val result = normalizer.normalizeCurrency(input)
            
            // Then
            assertEquals("Should normalize currency $input to GHS", "GHS", result)
        }
    }
    
    @Test
    fun testNormalizeCustomerName_withGhanaianNames_shouldReturnCapitalized() {
        // Given
        val inputs = mapOf(
            "kwame" to "Kwame",
            "KWAME" to "Kwame",
            "ama" to "Ama",
            "kofi" to "Kofi",
            "akosua" to "Akosua",
            "yaw" to "Yaw",
            "adjoa" to "Adjoa",
            "kweku" to "Kweku"
        )
        
        inputs.forEach { (input, expected) ->
            // When
            val result = normalizer.normalizeCustomerName(input)
            
            // Then
            assertEquals("Should normalize name $input to $expected", expected, result)
        }
    }
    
    @Test
    fun testNormalizeLocation_withGhanaianCities_shouldReturnProperCase() {
        // Given
        val inputs = mapOf(
            "accra" to "Accra",
            "ACCRA" to "Accra",
            "kumasi" to "Kumasi",
            "tema" to "Tema",
            "cape coast" to "Cape Coast",
            "tamale" to "Tamale",
            "ho" to "Ho",
            "sunyani" to "Sunyani"
        )
        
        inputs.forEach { (input, expected) ->
            // When
            val result = normalizer.normalizeLocation(input)
            
            // Then
            assertEquals("Should normalize location $input to $expected", expected, result)
        }
    }
    
    @Test
    fun testNormalizeProduct_withUnknownProduct_shouldReturnOriginal() {
        // Given
        val unknownProduct = "unknown_product_xyz"
        
        // When
        val result = normalizer.normalizeProduct(unknownProduct)
        
        // Then
        assertEquals("Should return original for unknown product", 
            unknownProduct.lowercase(), result)
    }
    
    @Test
    fun testNormalizeQuantity_withInvalidNumber_shouldReturnNull() {
        // Given
        val invalidInputs = listOf("abc", "xyz", "invalid", "")
        
        invalidInputs.forEach { input ->
            // When
            val result = normalizer.normalizeQuantity(input)
            
            // Then
            assertNull("Should return null for invalid quantity: $input", result)
        }
    }
    
    @Test
    fun testNormalizeUnit_withUnknownUnit_shouldReturnOriginal() {
        // Given
        val unknownUnit = "unknown_unit"
        
        // When
        val result = normalizer.normalizeUnit(unknownUnit)
        
        // Then
        assertEquals("Should return original for unknown unit", 
            unknownUnit.lowercase(), result)
    }
    
    @Test
    fun testNormalizeCurrency_withUnknownCurrency_shouldReturnOriginal() {
        // Given
        val unknownCurrency = "USD"
        
        // When
        val result = normalizer.normalizeCurrency(unknownCurrency)
        
        // Then
        assertEquals("Should return original for unknown currency", unknownCurrency, result)
    }
    
    @Test
    fun testNormalizeWithWhitespace_shouldTrimAndNormalize() {
        // Given
        val inputs = mapOf(
            "  tilapia  " to "tilapia",
            "\tthree\t" to 3,
            "  pieces  " to "pieces",
            "  Kwame  " to "Kwame"
        )
        
        // When & Then
        assertEquals("Should trim and normalize product", 
            inputs["  tilapia  "], normalizer.normalizeProduct("  tilapia  "))
        assertEquals("Should trim and normalize quantity", 
            inputs["\tthree\t"], normalizer.normalizeQuantity("\tthree\t"))
        assertEquals("Should trim and normalize unit", 
            inputs["  pieces  "], normalizer.normalizeUnit("  pieces  "))
        assertEquals("Should trim and normalize name", 
            inputs["  Kwame  "], normalizer.normalizeCustomerName("  Kwame  "))
    }
    
    @Test
    fun testNormalizeProduct_withPluralForms_shouldHandleCorrectly() {
        // Given
        val inputs = mapOf(
            "tilapias" to "tilapia",
            "mackerels" to "mackerel",
            "catfishes" to "catfish",
            "fishes" to "fish"
        )
        
        inputs.forEach { (input, expected) ->
            // When
            val result = normalizer.normalizeProduct(input)
            
            // Then
            assertEquals("Should handle plural form $input", expected, result)
        }
    }
    
    @Test
    fun testNormalizeQuantity_withApproximateNumbers_shouldReturnExactNumber() {
        // Given
        val inputs = mapOf(
            "about five" to 5,
            "around three" to 3,
            "roughly ten" to 10,
            "approximately two" to 2
        )
        
        inputs.forEach { (input, expected) ->
            // When
            val result = normalizer.normalizeQuantity(input)
            
            // Then
            assertEquals("Should extract number from approximate phrase $input", expected, result)
        }
    }
}