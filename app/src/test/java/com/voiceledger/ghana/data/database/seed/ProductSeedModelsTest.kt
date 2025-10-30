package com.voiceledger.ghana.data.database.seed

import com.voiceledger.ghana.data.local.database.seed.ProductSeed
import org.junit.Assert.*
import org.junit.Test

class ProductSeedModelsTest {

    @Test
    fun testToEntity_convertsBasicFields() {
        val seed = ProductSeed(
            id = "test-001",
            canonicalName = "Test Product",
            category = "test",
            variants = listOf("variant1", "variant2"),
            minPrice = 10.0,
            maxPrice = 20.0,
            measurementUnits = listOf("piece", "bowl")
        )

        val entity = seed.toEntity { 12345L }

        assertEquals("test-001", entity.id)
        assertEquals("Test Product", entity.canonicalName)
        assertEquals("test", entity.category)
        assertEquals("variant1,variant2", entity.variants)
        assertEquals(10.0, entity.minPrice, 0.01)
        assertEquals(20.0, entity.maxPrice, 0.01)
        assertEquals("piece,bowl", entity.measurementUnits)
        assertEquals(0, entity.frequency)
        assertTrue(entity.isActive)
        assertEquals(12345L, entity.createdAt)
        assertEquals(12345L, entity.updatedAt)
        assertFalse(entity.isLearned)
        assertEquals(1.0f, entity.learningConfidence, 0.01f)
    }

    @Test
    fun testToEntity_handlesNullableFields() {
        val seed = ProductSeed(
            id = "test-001",
            canonicalName = "Test Product",
            category = "test",
            variants = emptyList(),
            minPrice = 0.0,
            maxPrice = 0.0,
            measurementUnits = emptyList(),
            seasonality = "summer",
            twiNames = listOf("twi1", "twi2"),
            gaNames = null
        )

        val entity = seed.toEntity()

        assertNull(entity.gaNames)
        assertEquals("twi1,twi2", entity.twiNames)
        assertEquals("summer", entity.seasonality)
    }

    @Test
    fun testToEntity_normalizesVariants() {
        val seed = ProductSeed(
            id = "test-001",
            canonicalName = "Test Product",
            category = "test",
            variants = listOf("  variant1  ", "variant2", "", "  ", "variant1"),
            minPrice = 0.0,
            maxPrice = 0.0,
            measurementUnits = listOf("piece")
        )

        val entity = seed.toEntity()

        assertEquals("variant1,variant2", entity.variants)
    }

    @Test
    fun testToEntity_handlesEmptyLocalNames() {
        val seed = ProductSeed(
            id = "test-001",
            canonicalName = "Test Product",
            category = "test",
            variants = emptyList(),
            minPrice = 0.0,
            maxPrice = 0.0,
            measurementUnits = emptyList(),
            twiNames = emptyList(),
            gaNames = listOf()
        )

        val entity = seed.toEntity()

        assertNull(entity.twiNames)
        assertNull(entity.gaNames)
    }

    @Test
    fun testToEntity_handlesBlankSeasonality() {
        val seed = ProductSeed(
            id = "test-001",
            canonicalName = "Test Product",
            category = "test",
            variants = emptyList(),
            minPrice = 0.0,
            maxPrice = 0.0,
            measurementUnits = emptyList(),
            seasonality = "  "
        )

        val entity = seed.toEntity()

        assertNull(entity.seasonality)
    }
}
