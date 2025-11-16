package com.voiceledger.ghana.data.database.seed

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.voiceledger.ghana.data.local.database.seed.SeedDataLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SeedDataLoaderTest {

    private lateinit var context: Context
    private lateinit var loader: SeedDataLoader

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        loader = SeedDataLoader(context)
    }

    @Test
    fun loadProductSeeds_returnsAllSeedEntries() {
        val result = loader.loadProductSeeds()

        assertTrue(result.isSuccess)
        val seeds = result.getOrElse { emptyList() }
        assertEquals(13, seeds.size)
        assertTrue(seeds.any { it.id == "tilapia-001" && it.canonicalName == "Tilapia" })
        assertTrue(seeds.any { it.id == "bowl-001" && it.category == "measurement" })
        assertTrue(seeds.any { it.id == "tin-001" && it.canonicalName == "Tin" })
        assertTrue(seeds.any { it.id == "size-001" && it.canonicalName == "Size" })
    }

    @Test
    fun loadProductSeeds_containsExpectedFields() {
        val result = loader.loadProductSeeds()

        assertTrue(result.isSuccess)
        val seeds = result.getOrElse { emptyList() }
        val tilapia = seeds.find { it.id == "tilapia-001" }
        
        assertNotNull("Tilapia should be present", tilapia)
        tilapia?.let {
            assertEquals("Tilapia", it.canonicalName)
            assertEquals("fish", it.category)
            assertTrue("Should have variants", it.variants.isNotEmpty())
            assertTrue("Should have measurement units", it.measurementUnits.isNotEmpty())
            assertEquals("Should be active", true, it.isActive)
            assertEquals("Should not be learned by default", false, it.isLearned)
        }
    }

    @Test
    fun loadProductSeeds_handlesNullFields() {
        val result = loader.loadProductSeeds()

        assertTrue(result.isSuccess)
        val seeds = result.getOrElse { emptyList() }
        val tuna = seeds.find { it.id == "tuna-001" }
        
        assertNotNull("Tuna should be present", tuna)
        tuna?.let {
            assertEquals("Null twiNames should be preserved", null, it.twiNames)
            assertEquals("Null gaNames should be preserved", null, it.gaNames)
        }
    }
}

private fun assertNotNull(message: String, value: Any?) {
    if (value == null) {
        throw AssertionError(message)
    }
}
