package com.voiceledger.ghana.data.local.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Tests for database migrations
 * Ensures that migrations preserve data and create proper indices
 */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        VoiceLedgerDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Before
    fun setUp() {
        // Clean up any existing test database
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(TEST_DB)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(TEST_DB)
    }

    @Test
    @Throws(IOException::class)
    fun migrate1To2_preservesData() {
        // Create the database with version 1
        var db = helper.createDatabase(TEST_DB, 1)

        // Insert test data into version 1 database
        insertTransactionV1(db, "trans-001", 1234567890000L, "2024-01-15", 50.0, "Tilapia", 0, 0)
        insertTransactionV1(db, "trans-002", 1234567890100L, "2024-01-15", 75.0, "Mackerel", 1, 0)
        insertDailySummaryV1(db, "2024-01-15", 125.0, 2, 0)
        insertAudioMetadataV1(db, "audio-001", 1234567890000L, 0.85f, 1, 1)
        insertSpeakerProfileV1(db, "speaker-001", "0.1,0.2,0.3", 1, 5, 1234567890000L, 1, 0)
        insertProductVocabularyV1(db, "prod-001", "Tilapia", "fish", 1, 10)

        db.close()

        // Migrate to version 2
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, DatabaseMigrations.MIGRATION_1_2)

        // Verify data is preserved
        verifyTransactionData(db, "trans-001", 50.0, "Tilapia")
        verifyTransactionData(db, "trans-002", 75.0, "Mackerel")
        verifyDailySummaryData(db, "2024-01-15", 125.0)
        verifyAudioMetadataData(db, "audio-001", 0.85f)
        verifySpeakerProfileData(db, "speaker-001", 5)
        verifyProductVocabularyData(db, "prod-001", "Tilapia")

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate1To2_createsIndices() {
        // Create the database with version 1
        var db = helper.createDatabase(TEST_DB, 1)
        db.close()

        // Migrate to version 2
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, DatabaseMigrations.MIGRATION_1_2)

        // Verify indices exist
        verifyIndicesExist(db)

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate1To2_indicesImproveQueryPerformance() {
        // Create the database with version 1
        var db = helper.createDatabase(TEST_DB, 1)

        // Insert substantial test data for performance testing
        for (i in 1..100) {
            insertTransactionV1(
                db,
                "trans-$i",
                1234567890000L + i,
                "2024-01-15",
                50.0 + i,
                "Product-$i",
                if (i % 2 == 0) 1 else 0,
                if (i % 3 == 0) 1 else 0
            )
        }

        db.close()

        // Migrate to version 2
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, DatabaseMigrations.MIGRATION_1_2)

        // Test index usage for common queries
        assertQueryUsesIndex(
            db,
            "SELECT * FROM transactions WHERE needsReview = 1",
            "index_transactions_needsReview"
        )

        assertQueryUsesIndex(
            db,
            "SELECT * FROM transactions WHERE synced = 0 ORDER BY timestamp ASC",
            "index_transactions_synced_timestamp"
        )

        assertQueryUsesIndex(
            db,
            "SELECT * FROM transactions WHERE date = '2024-01-15' AND needsReview = 1",
            "index_transactions_date_needsReview"
        )

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate1To2_withEmptyDatabase_succeeds() {
        // Create empty version 1 database
        var db = helper.createDatabase(TEST_DB, 1)
        db.close()

        // Migrate to version 2
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, DatabaseMigrations.MIGRATION_1_2)

        // Verify tables exist and are empty
        val cursor1 = db.query("SELECT COUNT(*) FROM transactions")
        cursor1.moveToFirst()
        assertEquals("Transactions table should be empty", 0, cursor1.getInt(0))
        cursor1.close()

        val cursor2 = db.query("SELECT COUNT(*) FROM daily_summaries")
        cursor2.moveToFirst()
        assertEquals("Daily summaries table should be empty", 0, cursor2.getInt(0))
        cursor2.close()

        // Verify indices exist
        verifyIndicesExist(db)

        db.close()
    }

    // Helper methods to insert data into version 1 database

    private fun insertTransactionV1(
        db: SupportSQLiteDatabase,
        id: String,
        timestamp: Long,
        date: String,
        amount: Double,
        product: String,
        needsReview: Int,
        synced: Int
    ) {
        db.execSQL(
            """
            INSERT INTO transactions 
            (id, timestamp, date, amount, currency, product, quantity, unit, customerId, 
             confidence, transcriptSnippet, sellerConfidence, customerConfidence, 
             needsReview, synced, originalPrice, finalPrice, marketSession) 
            VALUES (?, ?, ?, ?, 'GHS', ?, 1, 'piece', NULL, 0.9, 'test snippet', 0.9, 0.9, ?, ?, NULL, ?, NULL)
            """,
            arrayOf(id, timestamp, date, amount, product, needsReview, synced, amount)
        )
    }

    private fun insertDailySummaryV1(
        db: SupportSQLiteDatabase,
        date: String,
        totalSales: Double,
        transactionCount: Int,
        synced: Int
    ) {
        db.execSQL(
            """
            INSERT INTO daily_summaries 
            (date, totalSales, transactionCount, uniqueCustomers, topProduct, topProductSales, 
             peakHour, peakHourSales, averageTransactionValue, repeatCustomers, newCustomers, 
             totalQuantitySold, mostProfitableHour, leastActiveHour, confidenceScore, 
             reviewedTransactions, comparisonWithYesterday, comparisonWithLastWeek, 
             productBreakdown, hourlyBreakdown, customerInsights, recommendations, timestamp, synced) 
            VALUES (?, ?, ?, 1, 'Tilapia', 50.0, '10', 50.0, 50.0, 0, 1, 10.0, '10', '14', 0.9, 
                    0, NULL, NULL, '{}', '{}', '{}', '[]', ${System.currentTimeMillis()}, ?)
            """,
            arrayOf(date, totalSales, transactionCount, synced)
        )
    }

    private fun insertAudioMetadataV1(
        db: SupportSQLiteDatabase,
        chunkId: String,
        timestamp: Long,
        vadScore: Float,
        speechDetected: Int,
        speakerDetected: Int
    ) {
        db.execSQL(
            """
            INSERT INTO audio_metadata 
            (chunkId, timestamp, vadScore, speechDetected, speakerDetected, speakerId, 
             speakerConfidence, audioQuality, durationMs, processingTimeMs, 
             contributedToTransaction, transactionId, errorMessage, batteryLevel, powerSavingMode) 
            VALUES (?, ?, ?, ?, ?, NULL, NULL, 0.8, 1000, 100, 0, NULL, NULL, 80, 0)
            """,
            arrayOf(chunkId, timestamp, vadScore, speechDetected, speakerDetected)
        )
    }

    private fun insertSpeakerProfileV1(
        db: SupportSQLiteDatabase,
        id: String,
        voiceEmbedding: String,
        isSeller: Int,
        visitCount: Int,
        lastVisit: Long,
        isActive: Int,
        synced: Int
    ) {
        val now = System.currentTimeMillis()
        db.execSQL(
            """
            INSERT INTO speaker_profiles 
            (id, voiceEmbedding, isSeller, name, visitCount, lastVisit, averageSpending, 
             totalSpent, preferredLanguage, customerType, confidenceThreshold, isActive, 
             createdAt, updatedAt, synced) 
            VALUES (?, ?, ?, 'Test Speaker', ?, ?, 50.0, 250.0, 'en', 'regular', 0.75, ?, ?, ?, ?)
            """,
            arrayOf(id, voiceEmbedding, isSeller, visitCount, lastVisit, isActive, now, now, synced)
        )
    }

    private fun insertProductVocabularyV1(
        db: SupportSQLiteDatabase,
        id: String,
        canonicalName: String,
        category: String,
        isActive: Int,
        frequency: Int
    ) {
        val now = System.currentTimeMillis()
        db.execSQL(
            """
            INSERT INTO product_vocabulary 
            (id, canonicalName, category, variants, minPrice, maxPrice, measurementUnits, 
             frequency, isActive, seasonality, twiNames, gaNames, createdAt, updatedAt, 
             isLearned, learningConfidence) 
            VALUES (?, ?, ?, 'variant1,variant2', 10.0, 50.0, 'piece,bowl', ?, ?, NULL, NULL, NULL, ?, ?, 0, 1.0)
            """,
            arrayOf(id, canonicalName, category, frequency, isActive, now, now)
        )
    }

    // Helper methods to verify data after migration

    private fun verifyTransactionData(db: SupportSQLiteDatabase, id: String, amount: Double, product: String) {
        val cursor = db.query("SELECT amount, product FROM transactions WHERE id = ?", arrayOf(id))
        assertTrue("Transaction $id should exist", cursor.moveToFirst())
        assertEquals("Amount should match", amount, cursor.getDouble(0), 0.01)
        assertEquals("Product should match", product, cursor.getString(1))
        cursor.close()
    }

    private fun verifyDailySummaryData(db: SupportSQLiteDatabase, date: String, totalSales: Double) {
        val cursor = db.query("SELECT totalSales FROM daily_summaries WHERE date = ?", arrayOf(date))
        assertTrue("Daily summary for $date should exist", cursor.moveToFirst())
        assertEquals("Total sales should match", totalSales, cursor.getDouble(0), 0.01)
        cursor.close()
    }

    private fun verifyAudioMetadataData(db: SupportSQLiteDatabase, chunkId: String, vadScore: Float) {
        val cursor = db.query("SELECT vadScore FROM audio_metadata WHERE chunkId = ?", arrayOf(chunkId))
        assertTrue("Audio metadata $chunkId should exist", cursor.moveToFirst())
        assertEquals("VAD score should match", vadScore, cursor.getFloat(0), 0.01f)
        cursor.close()
    }

    private fun verifySpeakerProfileData(db: SupportSQLiteDatabase, id: String, visitCount: Int) {
        val cursor = db.query("SELECT visitCount FROM speaker_profiles WHERE id = ?", arrayOf(id))
        assertTrue("Speaker profile $id should exist", cursor.moveToFirst())
        assertEquals("Visit count should match", visitCount, cursor.getInt(0))
        cursor.close()
    }

    private fun verifyProductVocabularyData(db: SupportSQLiteDatabase, id: String, canonicalName: String) {
        val cursor = db.query("SELECT canonicalName FROM product_vocabulary WHERE id = ?", arrayOf(id))
        assertTrue("Product vocabulary $id should exist", cursor.moveToFirst())
        assertEquals("Canonical name should match", canonicalName, cursor.getString(0))
        cursor.close()
    }

    private fun verifyIndicesExist(db: SupportSQLiteDatabase) {
        // Query sqlite_master to verify indices exist
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type = 'index' AND name LIKE 'index_%'")
        
        val indices = mutableListOf<String>()
        while (cursor.moveToNext()) {
            indices.add(cursor.getString(0))
        }
        cursor.close()

        // Verify critical indices exist
        assertTrue("Should have needsReview index", indices.any { it.contains("needsReview") })
        assertTrue("Should have synced index for transactions", indices.any { it.contains("transactions_synced") })
        assertTrue("Should have synced index for daily_summaries", indices.any { it.contains("daily_summaries_synced") })
        assertTrue("Should have isActive index", indices.any { it.contains("isActive") })
        assertTrue("Should have frequency index", indices.any { it.contains("frequency") })
    }

    private fun assertQueryUsesIndex(db: SupportSQLiteDatabase, query: String, expectedIndex: String) {
        // Use EXPLAIN QUERY PLAN to verify index usage
        val explainCursor = db.query("EXPLAIN QUERY PLAN $query")
        val explanation = StringBuilder()
        var usesIndex = false
        while (explainCursor.moveToNext()) {
            val detail = explainCursor.getString(3) ?: ""
            explanation.append(detail).append('\n')
            if (detail.contains(expectedIndex, ignoreCase = true) || detail.contains("USING INDEX", ignoreCase = true)) {
                usesIndex = true
            }
        }
        explainCursor.close()

        // Verify the index is mentioned in the query plan
        assertTrue(
            "Query should use index $expectedIndex. Plan: $explanation",
            usesIndex
        )
    }
}
