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
        assertEquals(11, seeds.size)
        assertTrue(seeds.any { it.id == "tilapia-001" && it.canonicalName == "Tilapia" })
        assertTrue(seeds.any { it.id == "bowl-001" && it.category == "measurement" })
    }
}
