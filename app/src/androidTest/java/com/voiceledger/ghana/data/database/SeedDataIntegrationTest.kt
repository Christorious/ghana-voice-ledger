package com.voiceledger.ghana.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.voiceledger.ghana.data.local.database.VoiceLedgerDatabase
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SeedDataIntegrationTest {

    private var database: VoiceLedgerDatabase? = null

    @After
    fun tearDown() {
        database?.close()
    }

    @Test
    fun databaseCreation_populatesProductVocabulary() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        database = Room.inMemoryDatabaseBuilder(
            context,
            VoiceLedgerDatabase::class.java
        )
            .addCallback(VoiceLedgerDatabase.createSeedDataCallback(context, dispatcher))
            .build()

        testScheduler.advanceUntilIdle()

        val productDao = database!!.productVocabularyDao()
        val products = productDao.getActiveProductCount()

        assertEquals("Should have 11 seed products", 11, products)
    }

    @Test
    fun seedData_containsExpectedFishProducts() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        database = Room.inMemoryDatabaseBuilder(
            context,
            VoiceLedgerDatabase::class.java
        )
            .addCallback(VoiceLedgerDatabase.createSeedDataCallback(context, dispatcher))
            .build()

        testScheduler.advanceUntilIdle()

        val productDao = database!!.productVocabularyDao()
        
        val tilapia = productDao.getProductById("tilapia-001")
        assertNotNull("Should contain Tilapia", tilapia)
        assertEquals("Tilapia", tilapia?.canonicalName)
        assertEquals("fish", tilapia?.category)
        assertTrue(tilapia?.variants?.contains("tilapia") == true)
        
        val mackerel = productDao.getProductById("mackerel-001")
        assertNotNull("Should contain Mackerel", mackerel)
        assertEquals("Mackerel", mackerel?.canonicalName)
        assertTrue(mackerel?.variants?.contains("kpanla") == true)
    }

    @Test
    fun seedData_containsMeasurementUnits() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        database = Room.inMemoryDatabaseBuilder(
            context,
            VoiceLedgerDatabase::class.java
        )
            .addCallback(VoiceLedgerDatabase.createSeedDataCallback(context, dispatcher))
            .build()

        testScheduler.advanceUntilIdle()

        val productDao = database!!.productVocabularyDao()
        
        val bowl = productDao.getProductById("bowl-001")
        assertNotNull("Should contain Bowl", bowl)
        assertEquals("Bowl", bowl?.canonicalName)
        assertEquals("measurement", bowl?.category)
        
        val bucket = productDao.getProductById("bucket-001")
        assertNotNull("Should contain Bucket", bucket)
        
        val piece = productDao.getProductById("piece-001")
        assertNotNull("Should contain Piece", piece)
    }

    @Test
    fun seedData_noDuplicatesOnMultipleInits() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        database = Room.inMemoryDatabaseBuilder(
            context,
            VoiceLedgerDatabase::class.java
        )
            .addCallback(VoiceLedgerDatabase.createSeedDataCallback(context, dispatcher))
            .build()

        testScheduler.advanceUntilIdle()

        val productDao = database!!.productVocabularyDao()
        val initialCount = productDao.getActiveProductCount()

        val tilapia = productDao.getProductById("tilapia-001")
        assertNotNull(tilapia)
        
        productDao.insertProduct(tilapia!!)
        
        val countAfterReinsert = productDao.getActiveProductCount()
        assertEquals("Should not create duplicates (REPLACE strategy)", initialCount, countAfterReinsert)
    }
}
