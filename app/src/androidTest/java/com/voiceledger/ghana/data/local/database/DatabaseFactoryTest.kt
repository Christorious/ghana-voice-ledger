package com.voiceledger.ghana.data.local.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for DatabaseFactory to validate both standard and encrypted
 * database creation paths produce usable database instances with proper initialization.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseFactoryTest {

    private lateinit var context: Context
    private val databases = mutableListOf<VoiceLedgerDatabase>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        clearTestDatabases()
    }

    @After
    fun tearDown() {
        databases.forEach { db ->
            try {
                db.close()
            } catch (e: Exception) {
            }
        }
        clearTestDatabases()
    }

    @Test
    fun testCreateNonEncryptedDatabase_shouldSucceed() = runTest {
        val database = DatabaseFactory.createDatabase(
            context = context,
            encrypted = false
        )
        databases.add(database)

        assertNotNull("Non-encrypted database should be created", database)
        assertTrue("Database should be open", database.isOpen)

        val transactionDao = database.transactionDao()
        assertNotNull("TransactionDao should be accessible", transactionDao)

        val productDao = database.productVocabularyDao()
        assertNotNull("ProductVocabularyDao should be accessible", productDao)

        val count = productDao.getActiveProductCount()
        assertTrue("Should have seeded product vocabulary", count > 0)
    }

    @Test
    fun testCreateEncryptedDatabase_shouldSucceed() = runTest {
        val passphrase = "test_secure_passphrase_12345"
        val database = DatabaseFactory.createDatabase(
            context = context,
            encrypted = true,
            passphrase = passphrase
        )
        databases.add(database)

        assertNotNull("Encrypted database should be created", database)
        assertTrue("Database should be open", database.isOpen)

        val transactionDao = database.transactionDao()
        assertNotNull("TransactionDao should be accessible", transactionDao)

        val productDao = database.productVocabularyDao()
        assertNotNull("ProductVocabularyDao should be accessible", productDao)

        val count = productDao.getActiveProductCount()
        assertTrue("Should have seeded product vocabulary", count > 0)
    }

    @Test
    fun testEncryptedAndNonEncryptedDatabases_shouldBothBeFunctional() = runTest {
        val nonEncryptedDb = DatabaseFactory.createDatabase(
            context = context,
            encrypted = false
        )
        databases.add(nonEncryptedDb)

        val nonEncryptedProductCount = nonEncryptedDb.productVocabularyDao().getActiveProductCount()
        assertTrue("Non-encrypted DB should have products", nonEncryptedProductCount > 0)

        val encryptedDb = DatabaseFactory.createDatabase(
            context = context,
            encrypted = true,
            passphrase = "secure_test_key"
        )
        databases.add(encryptedDb)

        val encryptedProductCount = encryptedDb.productVocabularyDao().getActiveProductCount()
        assertTrue("Encrypted DB should have products", encryptedProductCount > 0)

        assertEquals(
            "Both databases should seed same initial data",
            nonEncryptedProductCount,
            encryptedProductCount
        )
    }

    @Test
    fun testDatabaseSeeding_shouldPopulateInitialData() = runTest {
        val database = DatabaseFactory.createDatabase(
            context = context,
            encrypted = false
        )
        databases.add(database)

        val productDao = database.productVocabularyDao()
        val activeProductCount = productDao.getActiveProductCount()

        assertTrue("Should have seeded products", activeProductCount > 0)

        val tilapiaProduct = productDao.searchProducts("Tilapia")
        assertTrue("Should have Tilapia in seed data", tilapiaProduct.isNotEmpty())

        val mackerelProduct = productDao.searchProducts("Mackerel")
        assertTrue("Should have Mackerel in seed data", mackerelProduct.isNotEmpty())

        val categories = productDao.getAllCategories()
        assertTrue("Should have fish category", categories.contains("fish"))
        assertTrue("Should have measurement category", categories.contains("measurement"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testCreateEncryptedDatabase_withoutPassphrase_shouldThrow() = runTest {
        DatabaseFactory.createDatabase(
            context = context,
            encrypted = true,
            passphrase = null
        )
    }

    private fun clearTestDatabases() {
        try {
            context.deleteDatabase(VoiceLedgerDatabase.DATABASE_NAME)
        } catch (e: Exception) {
        }
    }
}
