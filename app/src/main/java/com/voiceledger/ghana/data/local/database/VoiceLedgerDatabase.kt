package com.voiceledger.ghana.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.voiceledger.ghana.BuildConfig
import com.voiceledger.ghana.data.local.dao.*
import com.voiceledger.ghana.data.local.entity.*
import com.voiceledger.ghana.data.local.database.seed.ProductVocabularySeeder
import com.voiceledger.ghana.data.local.database.seed.SeedDataLoader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Main Room database for Ghana Voice Ledger
 * Includes all entities and provides access to DAOs
 */
@Database(
    entities = [
        Transaction::class,
        DailySummary::class,
        SpeakerProfile::class,
        ProductVocabulary::class,
        AudioMetadata::class
    ],
    version = 1,
    exportSchema = true
)
abstract class VoiceLedgerDatabase : RoomDatabase() {
    
    abstract fun transactionDao(): TransactionDao
    abstract fun dailySummaryDao(): DailySummaryDao
    abstract fun speakerProfileDao(): SpeakerProfileDao
    abstract fun productVocabularyDao(): ProductVocabularyDao
    abstract fun audioMetadataDao(): AudioMetadataDao
    
    companion object {
        const val DATABASE_NAME = "voice_ledger_database"
        
        @Volatile
        private var INSTANCE: VoiceLedgerDatabase? = null
        
        fun getDatabase(context: Context): VoiceLedgerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VoiceLedgerDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2) // Future migrations
                    .addCallback(createSeedDataCallback(context.applicationContext))
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * Get encrypted database instance using SQLCipher
         * This will be used in production for data security
         */
        fun getEncryptedDatabase(context: Context, passphrase: String): VoiceLedgerDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                VoiceLedgerDatabase::class.java,
                DATABASE_NAME
            )
                .openHelperFactory(net.sqlcipher.room.SupportFactory(passphrase.toByteArray()))
                .addMigrations(MIGRATION_1_2)
                .addCallback(createSeedDataCallback(context.applicationContext))
                .build()
        }
        
        /**
         * Future migration from version 1 to 2
         * Placeholder for when we need to update the schema
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Future schema changes will go here
                // Example:
                // database.execSQL("ALTER TABLE transactions ADD COLUMN new_column TEXT")
            }
        }
        
        internal fun createSeedDataCallback(
            context: Context,
            dispatcher: CoroutineDispatcher = Dispatchers.IO
        ): RoomDatabase.Callback {
            return DatabaseCallback(context.applicationContext, dispatcher)
        }
    }
    
    /**
     * Database callback for initialization and pre-population
     * Loads seed data from structured JSON assets instead of hardcoded SQL
     */
    private class DatabaseCallback(
        context: Context,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ) : RoomDatabase.Callback() {
        
        private val applicationContext = context.applicationContext
        private val databaseScope = CoroutineScope(SupervisorJob() + dispatcher)
        private val seedDataLoader = SeedDataLoader(applicationContext)
        private val seeder = ProductVocabularySeeder(seedDataLoader)
        
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            databaseScope.launch {
                populateInitialData()
            }
        }
        
        private suspend fun populateInitialData() {
            val database = INSTANCE ?: run {
                Timber.w("Database instance not available for seed data loading")
                return
            }
            
            try {
                val result = seeder.seed(database)
                result.onSuccess { insertedCount ->
                    if (insertedCount > 0) {
                        Timber.i("Seeded $insertedCount product vocabulary entries from assets")
                    } else {
                        Timber.i("Product vocabulary already populated; no seed entries inserted")
                    }
                }.onFailure { error ->
                    Timber.e(error, "Failed to seed product vocabulary from assets")
                    if (BuildConfig.DEBUG_MODE) {
                        throw IllegalStateException("Failed to seed product vocabulary", error)
                    }
                }
            } catch (error: Exception) {
                Timber.e(error, "Unexpected error during product vocabulary seeding")
                if (BuildConfig.DEBUG_MODE) {
                    throw IllegalStateException("Unexpected error during product vocabulary seeding", error)
                }
            }
        }
    }
}