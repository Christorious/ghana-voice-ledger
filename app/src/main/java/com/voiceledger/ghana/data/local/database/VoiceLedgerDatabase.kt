package com.voiceledger.ghana.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.voiceledger.ghana.data.local.dao.*
import com.voiceledger.ghana.data.local.entity.*

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
        AudioMetadata::class,
        OfflineOperationEntity::class
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
                .addMigrations(*DatabaseMigrations.getAllMigrations())
                .addCallback(DatabaseCallback())
                .build()
        }
    }
    
    /**
     * Database callback for initialization and pre-population
     */
    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Pre-populate with Ghana fish vocabulary
            populateInitialData(db)
        }
        
        private fun populateInitialData(db: SupportSQLiteDatabase) {
            // Insert common Ghana fish products
            val fishProducts = listOf(
                // Tilapia variants
                "('tilapia-001', 'Tilapia', 'fish', 'tilapia,apateshi,tuo', 12.0, 25.0, 'piece,bowl', 0, 1, NULL, 'apateshi,tuo', NULL, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}, 0, 1.0)",
                
                // Mackerel variants  
                "('mackerel-001', 'Mackerel', 'fish', 'mackerel,kpanla,titus', 8.0, 18.0, 'piece,tin', 0, 1, NULL, 'kpanla', NULL, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}, 0, 1.0)",
                
                // Sardines variants
                "('sardines-001', 'Sardines', 'fish', 'sardines,herring,sardin', 5.0, 12.0, 'tin,piece', 0, 1, NULL, 'herring', NULL, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}, 0, 1.0)",
                
                // Tuna variants
                "('tuna-001', 'Tuna', 'fish', 'tuna,light meat,chunk light', 15.0, 30.0, 'tin,piece', 0, 1, NULL, NULL, NULL, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}, 0, 1.0)",
                
                // Red fish (Red Snapper)
                "('redfish-001', 'Red Fish', 'fish', 'red fish,red snapper,adwene', 20.0, 45.0, 'piece,size', 0, 1, NULL, 'adwene', NULL, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}, 0, 1.0)",
                
                // Salmon variants
                "('salmon-001', 'Salmon', 'fish', 'salmon,pink salmon', 25.0, 50.0, 'piece,tin', 0, 1, NULL, NULL, NULL, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}, 0, 1.0)",
                
                // Catfish variants
                "('catfish-001', 'Catfish', 'fish', 'catfish,mudfish,sumbre', 18.0, 35.0, 'piece,size', 0, 1, NULL, 'sumbre', NULL, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}, 0, 1.0)",
                
                // Croaker variants
                "('croaker-001', 'Croaker', 'fish', 'croaker,komi,yellow croaker', 10.0, 22.0, 'piece,size', 0, 1, NULL, 'komi', NULL, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}, 0, 1.0)"
            )
            
            fishProducts.forEach { product ->
                db.execSQL("""
                    INSERT INTO product_vocabulary 
                    (id, canonicalName, category, variants, minPrice, maxPrice, measurementUnits, frequency, isActive, seasonality, twiNames, gaNames, createdAt, updatedAt, isLearned, learningConfidence)
                    VALUES $product
                """)
            }
            
            // Insert common measurement units and currency terms
            val measurementProducts = listOf(
                "('bowl-001', 'Bowl', 'measurement', 'bowl,kokoo,rubber', 0.0, 0.0, 'bowl', 0, 1, NULL, 'kokoo', NULL, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}, 0, 1.0)",
                "('bucket-001', 'Bucket', 'measurement', 'bucket,rubber,container', 0.0, 0.0, 'bucket', 0, 1, NULL, 'rubber', NULL, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}, 0, 1.0)",
                "('piece-001', 'Piece', 'measurement', 'piece,one,single', 0.0, 0.0, 'piece', 0, 1, NULL, NULL, NULL, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}, 0, 1.0)"
            )
            
            measurementProducts.forEach { product ->
                db.execSQL("""
                    INSERT INTO product_vocabulary 
                    (id, canonicalName, category, variants, minPrice, maxPrice, measurementUnits, frequency, isActive, seasonality, twiNames, gaNames, createdAt, updatedAt, isLearned, learningConfidence)
                    VALUES $product
                """)
            }
        }
    }
}