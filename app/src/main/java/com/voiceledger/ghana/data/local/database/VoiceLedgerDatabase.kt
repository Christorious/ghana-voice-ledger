package com.voiceledger.ghana.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.voiceledger.ghana.data.local.dao.AudioMetadataDao
import com.voiceledger.ghana.data.local.dao.DailySummaryDao
import com.voiceledger.ghana.data.local.dao.ProductVocabularyDao
import com.voiceledger.ghana.data.local.dao.SpeakerProfileDao
import com.voiceledger.ghana.data.local.dao.TransactionDao
import com.voiceledger.ghana.data.local.entity.AudioMetadata
import com.voiceledger.ghana.data.local.entity.DailySummary
import com.voiceledger.ghana.data.local.entity.ProductVocabulary
import com.voiceledger.ghana.data.local.entity.SpeakerProfile
import com.voiceledger.ghana.data.local.entity.Transaction
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
 * Main Room database for Ghana Voice Ledger.
 * Includes all entities and provides access to DAOs.
 */
@Database(
    entities = [
        Transaction::class,
        DailySummary::class,
        SpeakerProfile::class,
        ProductVocabulary::class,
        AudioMetadata::class,
        OfflineOperationEntity::class
        OfflineOperation::class
    ],
    version = 2,
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
    abstract fun offlineOperationDao(): OfflineOperationDao
    
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
                    .addMigrations(*DatabaseMigrations.getAllMigrations())
                    .addCallback(DatabaseCallback())
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
                .addMigrations(*DatabaseMigrations.getAllMigrations())
                .addCallback(DatabaseCallback())
                .build()
        }
        
        /**
         * Migration from version 1 to 2
         * Adds offline operations table for durable queue
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create offline operations table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS offline_operations (
                        id TEXT NOT NULL PRIMARY KEY,
                        operationType TEXT NOT NULL,
                        entityType TEXT NOT NULL,
                        entityId TEXT NOT NULL,
                        data TEXT NOT NULL,
                        INTEGER NOT NULL,
                        synced INTEGER NOT NULL DEFAULT 0,
                        retryCount INTEGER NOT NULL DEFAULT 0,
                        maxRetries INTEGER NOT NULL DEFAULT 3,
                        lastError TEXT,
                        priority INTEGER NOT NULL DEFAULT 3,
                        processing INTEGER NOT NULL DEFAULT 0
                    )
                """)
                
                // Create indices for offline operations
                database.execSQL("CREATE INDEX IF NOT EXISTS index_offline_operations_operationType ON offline_operations(operationType)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_offline_operations_timestamp ON offline_operations(timestamp)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_offline_operations_synced ON offline_operations(synced)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_offline_operations_retryCount ON offline_operations(retryCount)")
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
