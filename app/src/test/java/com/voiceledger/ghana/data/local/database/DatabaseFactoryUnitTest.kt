package com.voiceledger.ghana.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for DatabaseFactory configuration and behavior
 */
@RunWith(AndroidJUnit4::class)
class DatabaseFactoryUnitTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testCreateDatabase_withoutEncryption_shouldNotThrow() {
        val database = DatabaseFactory.createDatabase(
            context = context,
            encrypted = false
        )

        assertNotNull("Database should be created", database)
        database.close()
    }

    @Test
    fun testCreateDatabase_withEncryption_andPassphrase_shouldNotThrow() {
        val database = DatabaseFactory.createDatabase(
            context = context,
            encrypted = true,
            passphrase = "test_passphrase"
        )

        assertNotNull("Encrypted database should be created", database)
        database.close()
    }

    @Test
    fun testCreateDatabase_withEncryption_withoutPassphrase_shouldThrow() {
        assertThrows(IllegalArgumentException::class.java) {
            DatabaseFactory.createDatabase(
                context = context,
                encrypted = true,
                passphrase = null
            )
        }
    }

    @Test
    fun testCreateDatabase_withEncryption_emptyPassphrase_shouldNotThrow() {
        val database = DatabaseFactory.createDatabase(
            context = context,
            encrypted = true,
            passphrase = ""
        )

        assertNotNull("Database with empty passphrase should be created", database)
        database.close()
    }
}
