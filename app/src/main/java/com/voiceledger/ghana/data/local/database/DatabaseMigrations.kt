package com.voiceledger.ghana.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database migration definitions for schema changes
 * Each migration handles upgrading from one version to the next
 */
object DatabaseMigrations {
    
    /**
     * Migration from version 1 to 2
     * Adds offline_operations table for offline-first architecture
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create offline_operations table for queuing operations when offline
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS offline_operations (
                    id TEXT PRIMARY KEY NOT NULL,
                    type TEXT NOT NULL,
                    data TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    priority TEXT NOT NULL DEFAULT 'NORMAL',
                    status TEXT NOT NULL DEFAULT 'PENDING',
                    errorMessage TEXT,
                    lastAttempt INTEGER,
                    retryCount INTEGER NOT NULL DEFAULT 0
                )
            """)
            
            // Create indexes for efficient querying
            database.execSQL("CREATE INDEX IF NOT EXISTS index_offline_operations_timestamp ON offline_operations(timestamp)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_offline_operations_type ON offline_operations(type)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_offline_operations_status ON offline_operations(status)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_offline_operations_priority ON offline_operations(priority)")
     * Adds performance indices for high-frequency query columns
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add indices to transactions table
            database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_needsReview ON transactions(needsReview)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_synced ON transactions(synced)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_date_customerId ON transactions(date, customerId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_date_needsReview ON transactions(date, needsReview)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_synced_timestamp ON transactions(synced, timestamp)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_customerId_timestamp ON transactions(customerId, timestamp)")
            
            // Add indices to daily_summaries table
            database.execSQL("CREATE INDEX IF NOT EXISTS index_daily_summaries_synced ON daily_summaries(synced)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_daily_summaries_timestamp ON daily_summaries(timestamp)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_daily_summaries_date_synced ON daily_summaries(date, synced)")
            
            // Add indices to audio_metadata table
            database.execSQL("CREATE INDEX IF NOT EXISTS index_audio_metadata_speechDetected ON audio_metadata(speechDetected)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_audio_metadata_contributedToTransaction ON audio_metadata(contributedToTransaction)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_audio_metadata_transactionId ON audio_metadata(transactionId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_audio_metadata_powerSavingMode ON audio_metadata(powerSavingMode)")
            
            // Add indices to speaker_profiles table
            database.execSQL("CREATE INDEX IF NOT EXISTS index_speaker_profiles_isActive ON speaker_profiles(isActive)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_speaker_profiles_synced ON speaker_profiles(synced)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_speaker_profiles_createdAt ON speaker_profiles(createdAt)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_speaker_profiles_isSeller_isActive ON speaker_profiles(isSeller, isActive)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_speaker_profiles_lastVisit_isActive ON speaker_profiles(lastVisit, isActive)")
            
            // Add indices to product_vocabulary table
            database.execSQL("CREATE INDEX IF NOT EXISTS index_product_vocabulary_frequency ON product_vocabulary(frequency)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_product_vocabulary_isLearned ON product_vocabulary(isLearned)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_product_vocabulary_updatedAt ON product_vocabulary(updatedAt)")
        }
    }
    
    /**
     * Migration from version 2 to 3
     * Future migration placeholder
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Future schema changes
        }
    }
    
    /**
     * Migration from version 3 to 4
     * Future migration placeholder
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Future schema changes
        }
    }
    
    /**
     * Get all available migrations
     */
    fun getAllMigrations(): Array<Migration> {
        return arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4
        )
    }
    
    /**
     * Destructive migration fallback
     * Only use in development or when data loss is acceptable
     */
    fun createDestructiveMigration(startVersion: Int, endVersion: Int) = object : Migration(startVersion, endVersion) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Drop all tables and recreate
            database.execSQL("DROP TABLE IF EXISTS transactions")
            database.execSQL("DROP TABLE IF EXISTS daily_summaries")
            database.execSQL("DROP TABLE IF EXISTS speaker_profiles")
            database.execSQL("DROP TABLE IF EXISTS product_vocabulary")
            database.execSQL("DROP TABLE IF EXISTS audio_metadata")
            
            // Tables will be recreated by Room automatically
        }
    }
}