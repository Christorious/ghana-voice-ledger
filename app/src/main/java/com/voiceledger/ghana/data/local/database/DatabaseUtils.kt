package com.voiceledger.ghana.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.voiceledger.ghana.security.SecurityManager
import com.voiceledger.ghana.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Utility functions for database operations
 * Includes backup, restore, and maintenance functions
 */
object DatabaseUtils {
    
    private const val BACKUP_DIRECTORY = "database_backups"
    private val backupDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
    
    /**
     * Create a backup of the current database
     */
    suspend fun backupDatabase(
        context: Context, 
        database: VoiceLedgerDatabase,
        securityManager: SecurityManager
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Close database connections
            database.close()
            
            val backupDir = File(context.filesDir, BACKUP_DIRECTORY)
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            
            val timestamp = dateFormat.format(Date())
            val sanitizedTimestamp = securityManager.sanitizeForFileName("voice_ledger_backup_$timestamp")
            val backupFileName = "$sanitizedTimestamp.db"
            val timestamp = LocalDateTime.now().format(backupDateFormat)
            val backupFileName = "voice_ledger_backup_$timestamp.db"
            val backupFile = File(backupDir, backupFileName)
            
            val currentDbFile = context.getDatabasePath(VoiceLedgerDatabase.DATABASE_NAME)
            
            if (currentDbFile.exists()) {
                currentDbFile.copyTo(backupFile, overwrite = true)
                Result.success(backupFile.absolutePath)
            } else {
                Result.failure(Exception("Database file not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Restore database from backup file
     */
    suspend fun restoreDatabase(context: Context, backupFilePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val backupFile = File(backupFilePath)
            if (!backupFile.exists()) {
                return@withContext Result.failure(Exception("Backup file not found"))
            }
            
            val currentDbFile = context.getDatabasePath(VoiceLedgerDatabase.DATABASE_NAME)
            
            // Close any existing database connections
            VoiceLedgerDatabase.getDatabase(context).close()
            
            // Copy backup file to current database location
            backupFile.copyTo(currentDbFile, overwrite = true)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get list of available backup files
     */
    fun getBackupFiles(context: Context): List<File> {
        val backupDir = File(context.filesDir, BACKUP_DIRECTORY)
        return if (backupDir.exists()) {
            backupDir.listFiles { file -> file.name.endsWith(".db") }?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    /**
     * Delete old backup files (keep only the most recent N backups)
     */
    suspend fun cleanupOldBackups(context: Context, keepCount: Int = 5): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val backupFiles = getBackupFiles(context)
            val sortedFiles = backupFiles.sortedByDescending { it.lastModified() }
            
            var deletedCount = 0
            if (sortedFiles.size > keepCount) {
                val filesToDelete = sortedFiles.drop(keepCount)
                filesToDelete.forEach { file ->
                    if (file.delete()) {
                        deletedCount++
                    }
                }
            }
            
            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get database file size in bytes
     */
    fun getDatabaseSize(context: Context): Long {
        val dbFile = context.getDatabasePath(VoiceLedgerDatabase.DATABASE_NAME)
        return if (dbFile.exists()) dbFile.length() else 0L
    }
    
    /**
     * Format database size for display
     */
    fun formatDatabaseSize(sizeInBytes: Long): String {
        return when {
            sizeInBytes < 1024 -> "$sizeInBytes B"
            sizeInBytes < 1024 * 1024 -> "${sizeInBytes / 1024} KB"
            sizeInBytes < 1024 * 1024 * 1024 -> "${sizeInBytes / (1024 * 1024)} MB"
            else -> "${sizeInBytes / (1024 * 1024 * 1024)} GB"
        }
    }
    
    /**
     * Vacuum the database to reclaim space
     */
    suspend fun vacuumDatabase(database: VoiceLedgerDatabase): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.openHelper.writableDatabase.execSQL("VACUUM")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check database integrity
     */
    suspend fun checkDatabaseIntegrity(database: VoiceLedgerDatabase): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val cursor = database.openHelper.readableDatabase.rawQuery("PRAGMA integrity_check", null)
            cursor.use {
                if (it.moveToFirst()) {
                    val result = it.getString(0)
                    Result.success(result == "ok")
                } else {
                    Result.failure(Exception("Unable to check database integrity"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get database statistics
     */
    suspend fun getDatabaseStats(database: VoiceLedgerDatabase): DatabaseStats = withContext(Dispatchers.IO) {
        try {
            val transactionCount = database.transactionDao().getAllTransactions().toString().length // Simplified
            val summaryCount = database.dailySummaryDao().getTotalDaysTracked()
            val speakerCount = database.speakerProfileDao().getCustomerCount()
            val productCount = database.productVocabularyDao().getActiveProductCount()
            val metadataCount = database.audioMetadataDao().getTotalMetadataCount()
            
            DatabaseStats(
                transactionCount = transactionCount,
                summaryCount = summaryCount,
                speakerCount = speakerCount,
                productCount = productCount,
                metadataCount = metadataCount
            )
        } catch (e: Exception) {
            DatabaseStats()
        }
    }
    
    /**
     * Clear all data from database (for testing or reset)
     */
    suspend fun clearAllData(database: VoiceLedgerDatabase): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.runInTransaction {
                database.clearAllTables()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Export database to JSON format
     */
    suspend fun exportToJson(
        context: Context, 
        database: VoiceLedgerDatabase,
        securityManager: SecurityManager
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // This would be implemented to export all data to JSON format
            // For now, return a placeholder
            val timestamp = dateFormat.format(Date())
            val sanitizedFilename = securityManager.sanitizeForFileName("voice_ledger_export_$timestamp")
            val exportFileName = "$sanitizedFilename.json"
            val exportFile = File(context.getExternalFilesDir(null), exportFileName)
            
            // TODO: Implement actual JSON export logic
            exportFile.writeText("{\"exported_at\": \"$timestamp\", \"data\": \"placeholder\"}")
            
            Result.success(exportFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Data class for database statistics
 */
data class DatabaseStats(
    val transactionCount: Int = 0,
    val summaryCount: Int = 0,
    val speakerCount: Int = 0,
    val productCount: Int = 0,
    val metadataCount: Int = 0,
    val totalSize: Long = 0L
)