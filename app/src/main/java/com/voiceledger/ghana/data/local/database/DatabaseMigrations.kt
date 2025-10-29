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
     * Adds offline_operations table for persistent offline queue
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS offline_operations (
                    id TEXT PRIMARY KEY NOT NULL,
                    type TEXT NOT NULL,
                    data TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    priority TEXT NOT NULL,
                    status TEXT NOT NULL,
                    errorMessage TEXT,
                    lastAttempt INTEGER,
                    retryCount INTEGER NOT NULL DEFAULT 0
                )
            """)
            
            database.execSQL("CREATE INDEX IF NOT EXISTS index_offline_operations_status ON offline_operations(status)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_offline_operations_priority ON offline_operations(priority)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_offline_operations_timestamp ON offline_operations(timestamp)")
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