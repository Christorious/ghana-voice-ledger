package com.voiceledger.ghana.data.database.seed

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.voiceledger.ghana.data.local.database.VoiceLedgerDatabase
import com.voiceledger.ghana.data.local.database.seed.ProductVocabularySeeder
import com.voiceledger.ghana.data.local.database.seed.SeedDataLoader
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductVocabularySeederTest {

    private lateinit var context: Context
    private lateinit var database: VoiceLedgerDatabase
    private lateinit var seeder: ProductVocabularySeeder

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(
            context,
            VoiceLedgerDatabase::class.java
        ).allowMainThreadQueries().build()

        seeder = ProductVocabularySeeder(SeedDataLoader(context))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun seed_insertsSeedDataWhenEmpty() = runTest {
        val result = seeder.seed(database)

        assertTrue(result.isSuccess)
        assertEquals(13, result.getOrThrow())
        assertEquals(13, database.productVocabularyDao().getActiveProductCount())
    }

    @Test
    fun seed_doesNotInsertDuplicatesOnSubsequentRuns() = runTest {
        seeder.seed(database)
        val initialCount = database.productVocabularyDao().getActiveProductCount()

        val secondResult = seeder.seed(database)

        assertTrue(secondResult.isSuccess)
        assertEquals(0, secondResult.getOrThrow())
        assertEquals(initialCount, database.productVocabularyDao().getActiveProductCount())
    }
}
