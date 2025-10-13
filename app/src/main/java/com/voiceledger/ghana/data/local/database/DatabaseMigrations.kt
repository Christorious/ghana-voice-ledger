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
     * Example: Adding new columns or tables
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Example migrations (to be implemented when needed):
            
            // Add new column to transactions table
            // database.execSQL("ALTER TABLE transactions ADD COLUMN payment_method TEXT")
            
            // Create new table for payment methods
            // database.execSQL("""
            //     CREATE TABLE payment_methods (
            //         id TEXT PRIMARY KEY NOT NULL,
            //         name TEXT NOT NULL,
            //         is_active INTEGER NOT NULL DEFAULT 1
            //     )
            // """)
            
            // Add indexes for better performance
            // database.execSQL("CREATE INDEX index_transactions_payment_method ON transactions(payment_method)")
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