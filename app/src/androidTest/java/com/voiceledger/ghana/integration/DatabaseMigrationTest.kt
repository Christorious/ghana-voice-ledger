package com.voiceledger.ghana.integration

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.voiceledger.ghana.data.local.database.DatabaseMigrations
import com.voiceledger.ghana.data.local.database.VoiceLedgerDatabase
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Integration test for database migrations
 * Tests schema version upgrades and data preservation
 */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    private val TEST_DB_NAME = "migration_test_db"

    @get:Rule
    val migrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        VoiceLedgerDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun testMigration1To2_addsOfflineOperationsTable() {
        // Create database with version 1 schema
        var db = migrationTestHelper.createDatabase(TEST_DB_NAME, 1)

        // Insert test data into version 1 database
        db.execSQL("""
            INSERT INTO transactions (id, timestamp, date, amount, currency, product, quantity, unit, 
                customerId, confidence, transcriptSnippet, sellerConfidence, customerConfidence, 
                needsReview, synced, originalPrice, finalPrice, marketSession)
            VALUES ('test-001', ${System.currentTimeMillis()}, '2024-01-01', 50.0, 'GHS', 'Tilapia', 
                5, 'pieces', 'customer-001', 0.95, 'Test transaction', 0.9, 0.85, 0, 0, 50.0, 50.0, 'AM')
        """)

        // Verify data was inserted
        val cursor = db.query("SELECT COUNT(*) FROM transactions")
        cursor.moveToFirst()
        val countBefore = cursor.getInt(0)
        cursor.close()
        assertEquals("Should have 1 transaction before migration", 1, countBefore)

        db.close()

        // Migrate to version 2
        db = migrationTestHelper.runMigrationsAndValidate(
            TEST_DB_NAME,
            2,
            true,
            DatabaseMigrations.MIGRATION_1_2
        )

        // Verify offline_operations table was created
        val tableCheckCursor = db.query("""
            SELECT name FROM sqlite_master 
            WHERE type='table' AND name='offline_operations'
        """)
        assertTrue("offline_operations table should exist", tableCheckCursor.moveToFirst())
        tableCheckCursor.close()

        // Verify table structure
        val columnsCursor = db.query("PRAGMA table_info(offline_operations)")
        val columnNames = mutableListOf<String>()
        while (columnsCursor.moveToNext()) {
            columnNames.add(columnsCursor.getString(columnsCursor.getColumnIndexOrThrow("name")))
        }
        columnsCursor.close()

        assertTrue("Should have id column", columnNames.contains("id"))
        assertTrue("Should have type column", columnNames.contains("type"))
        assertTrue("Should have data column", columnNames.contains("data"))
        assertTrue("Should have timestamp column", columnNames.contains("timestamp"))
        assertTrue("Should have priority column", columnNames.contains("priority"))
        assertTrue("Should have status column", columnNames.contains("status"))
        assertTrue("Should have errorMessage column", columnNames.contains("errorMessage"))
        assertTrue("Should have lastAttempt column", columnNames.contains("lastAttempt"))
        assertTrue("Should have retryCount column", columnNames.contains("retryCount"))

        // Verify indexes were created
        val indexCursor = db.query("""
            SELECT name FROM sqlite_master 
            WHERE type='index' AND tbl_name='offline_operations'
        """)
        val indexNames = mutableListOf<String>()
        while (indexCursor.moveToNext()) {
            indexNames.add(indexCursor.getString(0))
        }
        indexCursor.close()

        assertTrue("Should have timestamp index", 
            indexNames.any { it.contains("timestamp") })
        assertTrue("Should have type index", 
            indexNames.any { it.contains("type") })
        assertTrue("Should have status index", 
            indexNames.any { it.contains("status") })
        assertTrue("Should have priority index", 
            indexNames.any { it.contains("priority") })

        // Verify original data is preserved
        val dataCursor = db.query("SELECT COUNT(*) FROM transactions")
        dataCursor.moveToFirst()
        val countAfter = dataCursor.getInt(0)
        dataCursor.close()
        assertEquals("Original data should be preserved", countBefore, countAfter)

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun testMigration1To2_preservesTransactionData() {
        // Create database with version 1 and insert multiple transactions
        var db = migrationTestHelper.createDatabase(TEST_DB_NAME, 1)

        val testTransactions = listOf(
            Triple("tx-001", "Tilapia", 50.0),
            Triple("tx-002", "Mackerel", 30.0),
            Triple("tx-003", "Catfish", 75.0)
        )

        testTransactions.forEach { (id, product, amount) ->
            db.execSQL("""
                INSERT INTO transactions (id, timestamp, date, amount, currency, product, quantity, unit, 
                    customerId, confidence, transcriptSnippet, sellerConfidence, customerConfidence, 
                    needsReview, synced, originalPrice, finalPrice, marketSession)
                VALUES ('$id', ${System.currentTimeMillis()}, '2024-01-01', $amount, 'GHS', '$product', 
                    5, 'pieces', 'customer-001', 0.95, 'Test transaction', 0.9, 0.85, 0, 0, $amount, $amount, 'AM')
            """)
        }

        db.close()

        // Migrate to version 2
        db = migrationTestHelper.runMigrationsAndValidate(
            TEST_DB_NAME,
            2,
            true,
            DatabaseMigrations.MIGRATION_1_2
        )

        // Verify all transactions are preserved
        val cursor = db.query("SELECT id, product, amount FROM transactions ORDER BY id")
        val retrievedTransactions = mutableListOf<Triple<String, String, Double>>()
        while (cursor.moveToNext()) {
            retrievedTransactions.add(
                Triple(
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getDouble(2)
                )
            )
        }
        cursor.close()

        assertEquals("Should have same number of transactions", 
            testTransactions.size, retrievedTransactions.size)

        testTransactions.forEachIndexed { index, expected ->
            val actual = retrievedTransactions[index]
            assertEquals("Transaction ID should match", expected.first, actual.first)
            assertEquals("Product should match", expected.second, actual.second)
            assertEquals("Amount should match", expected.third, actual.third, 0.01)
        }

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun testMigration1To2_canInsertOfflineOperations() {
        // Create database with version 1
        var db = migrationTestHelper.createDatabase(TEST_DB_NAME, 1)
        db.close()

        // Migrate to version 2
        db = migrationTestHelper.runMigrationsAndValidate(
            TEST_DB_NAME,
            2,
            true,
            DatabaseMigrations.MIGRATION_1_2
        )

        // Insert test offline operation
        db.execSQL("""
            INSERT INTO offline_operations (id, type, data, timestamp, priority, status, retryCount)
            VALUES ('op-001', 'TRANSACTION_SYNC', '{"test":"data"}', ${System.currentTimeMillis()}, 'NORMAL', 'PENDING', 0)
        """)

        // Verify insertion
        val cursor = db.query("SELECT * FROM offline_operations WHERE id = 'op-001'")
        assertTrue("Should be able to query inserted operation", cursor.moveToFirst())
        assertEquals("Operation ID should match", "op-001", cursor.getString(cursor.getColumnIndexOrThrow("id")))
        assertEquals("Type should match", "TRANSACTION_SYNC", cursor.getString(cursor.getColumnIndexOrThrow("type")))
        assertEquals("Status should match", "PENDING", cursor.getString(cursor.getColumnIndexOrThrow("status")))
        cursor.close()

        db.close()
    }

    @Test
    fun testDatabaseCreation_withCurrentVersion() {
        // Test that database can be created with current version (version 2)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.databaseBuilder(
            context,
            VoiceLedgerDatabase::class.java,
            "test_current_version_db"
        )
            .addMigrations(*DatabaseMigrations.getAllMigrations())
            .build()

        // Verify database is accessible
        val transactionDao = database.transactionDao()
        assertNotNull("Transaction DAO should be accessible", transactionDao)

        val offlineOperationDao = database.offlineOperationDao()
        assertNotNull("Offline Operation DAO should be accessible", offlineOperationDao)

        database.close()
    }

    @Test
    @Throws(IOException::class)
    fun testMigration1To2_handlesLargeDataset() {
        // Create database with version 1 and insert many transactions
        var db = migrationTestHelper.createDatabase(TEST_DB_NAME, 1)

        // Insert 100 transactions
        for (i in 1..100) {
            db.execSQL("""
                INSERT INTO transactions (id, timestamp, date, amount, currency, product, quantity, unit, 
                    customerId, confidence, transcriptSnippet, sellerConfidence, customerConfidence, 
                    needsReview, synced, originalPrice, finalPrice, marketSession)
                VALUES ('tx-$i', ${System.currentTimeMillis()}, '2024-01-01', ${i * 10.0}, 'GHS', 'Product$i', 
                    $i, 'pieces', 'customer-$i', 0.95, 'Test transaction $i', 0.9, 0.85, 0, 0, ${i * 10.0}, ${i * 10.0}, 'AM')
            """)
        }

        val cursor = db.query("SELECT COUNT(*) FROM transactions")
        cursor.moveToFirst()
        val countBefore = cursor.getInt(0)
        cursor.close()
        assertEquals("Should have 100 transactions", 100, countBefore)

        db.close()

        // Migrate to version 2
        db = migrationTestHelper.runMigrationsAndValidate(
            TEST_DB_NAME,
            2,
            true,
            DatabaseMigrations.MIGRATION_1_2
        )

        // Verify all data preserved
        val afterCursor = db.query("SELECT COUNT(*) FROM transactions")
        afterCursor.moveToFirst()
        val countAfter = afterCursor.getInt(0)
        afterCursor.close()
        assertEquals("All transactions should be preserved", countBefore, countAfter)

        db.close()
    }
}
