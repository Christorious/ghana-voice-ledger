package com.voiceledger.ghana.data.local.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.voiceledger.ghana.data.local.entity.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests to verify query performance and index usage
 * These tests ensure that common queries utilize the created indices
 */
@RunWith(AndroidJUnit4::class)
class QueryPerformanceTest {

    private lateinit var database: VoiceLedgerDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(
            context,
            VoiceLedgerDatabase::class.java
        ).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun verifyTransactionQueriesUseIndices() = runBlocking {
        // Insert test data
        val transactionDao = database.transactionDao()
        val testTransactions = listOf(
            createTestTransaction("trans-1", needsReview = true, synced = false),
            createTestTransaction("trans-2", needsReview = false, synced = true),
            createTestTransaction("trans-3", needsReview = true, synced = false)
        )
        testTransactions.forEach { transactionDao.insertTransaction(it) }

        // Verify queries work correctly
        val needsReview = transactionDao.getTransactionsNeedingReview()
        assertNotNull("Needs review query should work", needsReview)

        val unsynced = transactionDao.getUnsyncedTransactions()
        assertTrue("Unsynced query should find transactions", unsynced.isNotEmpty())

        // Verify query plan uses indices
        val db = database.openHelper.writableDatabase
        
        // Test needsReview index
        verifyIndexUsage(
            db,
            "SELECT * FROM transactions WHERE needsReview = 1",
            "index_transactions_needsReview"
        )

        // Test synced index
        verifyIndexUsage(
            db,
            "SELECT * FROM transactions WHERE synced = 0",
            "index_transactions_synced"
        )

        // Test composite index for date + customerId
        verifyIndexUsage(
            db,
            "SELECT * FROM transactions WHERE date = '2024-01-15' AND customerId = 'customer-1'",
            "index_transactions_date_customerId"
        )
    }

    @Test
    fun verifyDailySummaryQueriesUseIndices() = runBlocking {
        val dailySummaryDao = database.dailySummaryDao()
        val testSummary = createTestDailySummary(synced = false)
        dailySummaryDao.insertSummary(testSummary)

        // Verify queries work
        val unsynced = dailySummaryDao.getUnsyncedSummaries()
        assertTrue("Unsynced query should find summaries", unsynced.isNotEmpty())

        // Verify query plan uses indices
        val db = database.openHelper.writableDatabase
        verifyIndexUsage(
            db,
            "SELECT * FROM daily_summaries WHERE synced = 0",
            "index_daily_summaries_synced"
        )
    }

    @Test
    fun verifyAudioMetadataQueriesUseIndices() = runBlocking {
        val audioMetadataDao = database.audioMetadataDao()
        val testMetadata = listOf(
            createTestAudioMetadata("audio-1", speechDetected = true),
            createTestAudioMetadata("audio-2", speechDetected = false)
        )
        testMetadata.forEach { audioMetadataDao.insertMetadata(it) }

        // Verify query plan uses indices
        val db = database.openHelper.writableDatabase
        verifyIndexUsage(
            db,
            "SELECT * FROM audio_metadata WHERE speechDetected = 1",
            "index_audio_metadata_speechDetected"
        )
    }

    @Test
    fun verifySpeakerProfileQueriesUseIndices() = runBlocking {
        val speakerProfileDao = database.speakerProfileDao()
        val testProfile = createTestSpeakerProfile(isActive = true, synced = false)
        speakerProfileDao.insertProfile(testProfile)

        // Verify queries work
        val unsynced = speakerProfileDao.getUnsyncedProfiles()
        assertTrue("Unsynced query should find profiles", unsynced.isNotEmpty())

        // Verify query plan uses composite index
        val db = database.openHelper.writableDatabase
        verifyIndexUsage(
            db,
            "SELECT * FROM speaker_profiles WHERE isSeller = 0 AND isActive = 1",
            "index_speaker_profiles_isSeller_isActive"
        )
    }

    @Test
    fun verifyProductVocabularyQueriesUseIndices() = runBlocking {
        val productVocabularyDao = database.productVocabularyDao()
        val testProduct = createTestProductVocabulary(frequency = 10)
        productVocabularyDao.insertProduct(testProduct)

        // Verify query plan uses frequency index
        val db = database.openHelper.writableDatabase
        verifyIndexUsage(
            db,
            "SELECT * FROM product_vocabulary WHERE isActive = 1 ORDER BY frequency DESC",
            "index_product_vocabulary_frequency"
        )
    }

    // Helper methods

    private fun verifyIndexUsage(
        db: androidx.sqlite.db.SupportSQLiteDatabase,
        query: String,
        expectedIndex: String
    ) {
        val cursor = db.query("EXPLAIN QUERY PLAN $query")
        val queryPlan = StringBuilder()
        var foundIndex = false

        while (cursor.moveToNext()) {
            val detail = cursor.getString(3) ?: ""
            queryPlan.append(detail).append('\n')
            if (detail.contains(expectedIndex, ignoreCase = true) || 
                detail.contains("USING INDEX", ignoreCase = true)) {
                foundIndex = true
            }
        }
        cursor.close()

        assertTrue(
            "Query should use index $expectedIndex.\nQuery: $query\nPlan:\n$queryPlan",
            foundIndex
        )
    }

    private fun createTestTransaction(
        id: String,
        needsReview: Boolean = false,
        synced: Boolean = false
    ): Transaction {
        return Transaction(
            id = id,
            timestamp = System.currentTimeMillis(),
            date = "2024-01-15",
            amount = 50.0,
            currency = "GHS",
            product = "Test Product",
            quantity = 1,
            unit = "piece",
            customerId = "customer-1",
            confidence = 0.9f,
            transcriptSnippet = "test snippet",
            sellerConfidence = 0.9f,
            customerConfidence = 0.9f,
            needsReview = needsReview,
            synced = synced,
            originalPrice = null,
            finalPrice = 50.0,
            marketSession = null
        )
    }

    private fun createTestDailySummary(synced: Boolean = false): DailySummary {
        return DailySummary(
            date = "2024-01-15",
            totalSales = 100.0,
            transactionCount = 2,
            uniqueCustomers = 1,
            topProduct = "Test Product",
            topProductSales = 100.0,
            peakHour = "10",
            peakHourSales = 50.0,
            averageTransactionValue = 50.0,
            repeatCustomers = 0,
            newCustomers = 1,
            totalQuantitySold = 2.0,
            mostProfitableHour = "10",
            leastActiveHour = "14",
            confidenceScore = 0.9f,
            reviewedTransactions = 0,
            comparisonWithYesterday = null,
            comparisonWithLastWeek = null,
            productBreakdown = emptyMap(),
            hourlyBreakdown = emptyMap(),
            customerInsights = emptyMap(),
            recommendations = emptyList(),
            timestamp = System.currentTimeMillis(),
            synced = synced
        )
    }

    private fun createTestAudioMetadata(
        chunkId: String,
        speechDetected: Boolean = false
    ): AudioMetadata {
        return AudioMetadata(
            chunkId = chunkId,
            timestamp = System.currentTimeMillis(),
            vadScore = 0.8f,
            speechDetected = speechDetected,
            speakerDetected = false,
            speakerId = null,
            speakerConfidence = null,
            audioQuality = 0.8f,
            durationMs = 1000,
            processingTimeMs = 100,
            contributedToTransaction = false,
            transactionId = null,
            errorMessage = null,
            batteryLevel = 80,
            powerSavingMode = false
        )
    }

    private fun createTestSpeakerProfile(
        isActive: Boolean = true,
        synced: Boolean = false
    ): SpeakerProfile {
        val now = System.currentTimeMillis()
        return SpeakerProfile(
            id = "speaker-1",
            voiceEmbedding = "0.1,0.2,0.3",
            isSeller = false,
            name = "Test Speaker",
            visitCount = 5,
            lastVisit = now,
            averageSpending = 50.0,
            totalSpent = 250.0,
            preferredLanguage = "en",
            customerType = "regular",
            confidenceThreshold = 0.75f,
            isActive = isActive,
            createdAt = now,
            updatedAt = now,
            synced = synced
        )
    }

    private fun createTestProductVocabulary(frequency: Int = 10): ProductVocabulary {
        val now = System.currentTimeMillis()
        return ProductVocabulary(
            id = "prod-1",
            canonicalName = "Test Product",
            category = "fish",
            variants = "variant1,variant2",
            minPrice = 10.0,
            maxPrice = 50.0,
            measurementUnits = "piece,bowl",
            frequency = frequency,
            isActive = true,
            seasonality = null,
            twiNames = null,
            gaNames = null,
            createdAt = now,
            updatedAt = now,
            isLearned = false,
            learningConfidence = 1.0f
        )
    }
}
