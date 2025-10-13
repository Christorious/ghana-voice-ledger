package com.voiceledger.ghana.security

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import com.voiceledger.ghana.data.local.database.VoiceLedgerDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.io.File
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure data storage service that handles encrypted database operations
 * Uses SQLCipher for database encryption and secure key management
 */
@Singleton
class SecureDataStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptionService: EncryptionService,
    private val privacyManager: PrivacyManager
) {
    
    companion object {
        private const val DATABASE_NAME = "voice_ledger_encrypted.db"
        private const val DATABASE_KEY_ALIAS = "database_encryption_key"
        private const val KEY_LENGTH = 32 // 256 bits
    }
    
    private var database: VoiceLedgerDatabase? = null
    private var databaseKey: ByteArray? = null
    
    /**
     * Initialize secure database with encryption
     */
    suspend fun initializeSecureDatabase(): VoiceLedgerDatabase = withContext(Dispatchers.IO) {
        if (database == null) {
            val key = getDatabaseEncryptionKey()
            val passphrase = SQLiteDatabase.getBytes(key.toString().toCharArray())
            val factory = SupportFactory(passphrase)
            
            database = Room.databaseBuilder(
                context,
                VoiceLedgerDatabase::class.java,
                DATABASE_NAME
            )
                .openHelperFactory(factory)
                .addCallback(object : androidx.room.RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Enable secure delete and other security features
                        db.execSQL("PRAGMA secure_delete = ON")
                        db.execSQL("PRAGMA auto_vacuum = FULL")
                    }
                    
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        // Set additional security pragmas
                        db.execSQL("PRAGMA cipher_memory_security = ON")
                        db.execSQL("PRAGMA cipher_hmac_algorithm = HMAC_SHA512")
                        db.execSQL("PRAGMA cipher_kdf_algorithm = PBKDF2_HMAC_SHA512")
                    }
                })
                .build()
        }
        
        database!!
    }
    
    /**
     * Get or generate database encryption key
     */
    private suspend fun getDatabaseEncryptionKey(): ByteArray {
        if (databaseKey == null) {
            val preferences = encryptionService.getEncryptedSharedPreferences("secure_storage")
            val keyString = preferences.getString(DATABASE_KEY_ALIAS, null)
            
            databaseKey = if (keyString != null) {
                try {
                    // Decrypt stored key
                    val encryptedData = deserializeEncryptedData(keyString)
                    encryptionService.decrypt(encryptedData).toByteArray()
                } catch (e: Exception) {
                    // Generate new key if decryption fails
                    generateAndStoreNewKey(preferences)
                }
            } else {
                // Generate new key for first time
                generateAndStoreNewKey(preferences)
            }
        }
        
        return databaseKey!!
    }
    
    /**
     * Generate and store new database encryption key
     */
    private suspend fun generateAndStoreNewKey(preferences: android.content.SharedPreferences): ByteArray {
        val newKey = encryptionService.generateSecureKey(KEY_LENGTH)
        val encryptedKey = encryptionService.encrypt(String(newKey))
        val serializedKey = serializeEncryptedData(encryptedKey)
        
        preferences.edit()
            .putString(DATABASE_KEY_ALIAS, serializedKey)
            .putLong("key_created", System.currentTimeMillis())
            .apply()
        
        return newKey
    }
    
    /**
     * Encrypt sensitive data before storage
     */
    suspend fun encryptSensitiveField(data: String): String {
        return if (privacyManager.privacyState.value.settings.encryptSensitiveData) {
            val encryptedData = encryptionService.encrypt(data)
            serializeEncryptedData(encryptedData)
        } else {
            data
        }
    }
    
    /**
     * Decrypt sensitive data after retrieval
     */
    suspend fun decryptSensitiveField(encryptedData: String): String {
        return try {
            if (privacyManager.privacyState.value.settings.encryptSensitiveData) {
                val data = deserializeEncryptedData(encryptedData)
                encryptionService.decrypt(data)
            } else {
                encryptedData
            }
        } catch (e: Exception) {
            // Return empty string if decryption fails
            ""
        }
    }
    
    /**
     * Secure backup of database
     */
    suspend fun createSecureBackup(backupPath: String): BackupResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val db = initializeSecureDatabase()
            db.close()
            
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            val backupFile = File(backupPath)
            
            // Create encrypted backup
            val backupKey = encryptionService.generateSecureKey()
            val encryptedBackup = encryptFile(dbFile, backupKey)
            
            backupFile.writeBytes(encryptedBackup.encryptedData)
            
            // Store backup metadata securely
            val backupMetadata = BackupMetadata(
                backupPath = backupPath,
                creationTime = System.currentTimeMillis(),
                encryptionKey = backupKey,
                iv = encryptedBackup.iv,
                checksum = calculateChecksum(dbFile)
            )
            
            storeBackupMetadata(backupMetadata)
            
            BackupResult(
                success = true,
                backupPath = backupPath,
                backupSize = backupFile.length(),
                creationTime = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            BackupResult(
                success = false,
                error = e.message,
                creationTime = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Restore database from secure backup
     */
    suspend fun restoreFromSecureBackup(backupPath: String): RestoreResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val backupMetadata = getBackupMetadata(backupPath)
                ?: return@withContext RestoreResult(false, error = "Backup metadata not found")
            
            val backupFile = File(backupPath)
            if (!backupFile.exists()) {
                return@withContext RestoreResult(false, error = "Backup file not found")
            }
            
            // Decrypt backup
            val encryptedData = EncryptedData(
                encryptedData = backupFile.readBytes(),
                iv = backupMetadata.iv
            )
            
            val decryptedData = encryptionService.decryptWithKey(encryptedData, backupMetadata.encryptionKey)
            
            // Close current database
            database?.close()
            database = null
            
            // Replace database file
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            dbFile.writeBytes(decryptedData.toByteArray())
            
            // Verify integrity
            val restoredChecksum = calculateChecksum(dbFile)
            if (!restoredChecksum.contentEquals(backupMetadata.checksum)) {
                return@withContext RestoreResult(false, error = "Backup integrity check failed")
            }
            
            // Reinitialize database
            initializeSecureDatabase()
            
            RestoreResult(
                success = true,
                restoredSize = dbFile.length(),
                restoreTime = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            RestoreResult(
                success = false,
                error = e.message,
                restoreTime = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Securely delete sensitive data
     */
    suspend fun secureDeleteData(dataType: DataType, olderThan: Long): DeletionResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val db = initializeSecureDatabase()
            var deletedCount = 0
            
            when (dataType) {
                DataType.VOICE_RECORDINGS -> {
                    // Delete old voice recordings
                    deletedCount = db.audioMetadataDao().deleteOlderThan(olderThan)
                    // Also delete associated audio files
                    deleteOldAudioFiles(olderThan)
                }
                DataType.TRANSACTION_DATA -> {
                    deletedCount = db.transactionDao().deleteOlderThan(olderThan)
                }
                DataType.ANALYTICS_DATA -> {
                    // Delete analytics data (would be implemented based on analytics schema)
                    deletedCount = 0
                }
                DataType.SPEAKER_PROFILES -> {
                    deletedCount = db.speakerProfileDao().deleteOlderThan(olderThan)
                }
            }
            
            // Vacuum database to reclaim space
            db.openHelper.writableDatabase.execSQL("VACUUM")
            
            DeletionResult(
                success = true,
                deletedCount = deletedCount,
                dataType = dataType,
                deletionTime = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            DeletionResult(
                success = false,
                error = e.message,
                dataType = dataType,
                deletionTime = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Get storage statistics
     */
    suspend fun getStorageStatistics(): StorageStatistics = withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        val dbSize = if (dbFile.exists()) dbFile.length() else 0L
        
        val audioDir = File(context.filesDir, "audio")
        val audioSize = if (audioDir.exists()) {
            audioDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        } else 0L
        
        val cacheSize = context.cacheDir.walkTopDown()
            .filter { it.isFile }
            .map { it.length() }
            .sum()
        
        StorageStatistics(
            databaseSize = dbSize,
            audioFilesSize = audioSize,
            cacheSize = cacheSize,
            totalSize = dbSize + audioSize + cacheSize,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    /**
     * Encrypt file with custom key
     */
    private suspend fun encryptFile(file: File, key: ByteArray): EncryptedData {
        val fileContent = file.readText()
        return encryptionService.encryptWithKey(fileContent, key)
    }
    
    /**
     * Calculate file checksum for integrity verification
     */
    private fun calculateChecksum(file: File): ByteArray {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(file.readBytes())
    }
    
    /**
     * Delete old audio files from filesystem
     */
    private fun deleteOldAudioFiles(olderThan: Long) {
        val audioDir = File(context.filesDir, "audio")
        if (audioDir.exists()) {
            audioDir.listFiles()?.forEach { file ->
                if (file.lastModified() < olderThan) {
                    file.delete()
                }
            }
        }
    }
    
    /**
     * Serialize encrypted data for storage
     */
    private fun serializeEncryptedData(encryptedData: EncryptedData): String {
        val combined = encryptedData.iv + encryptedData.encryptedData
        return android.util.Base64.encodeToString(combined, android.util.Base64.DEFAULT)
    }
    
    /**
     * Deserialize encrypted data from storage
     */
    private fun deserializeEncryptedData(serializedData: String): EncryptedData {
        val combined = android.util.Base64.decode(serializedData, android.util.Base64.DEFAULT)
        val iv = combined.sliceArray(0..11) // First 12 bytes are IV
        val encryptedData = combined.sliceArray(12 until combined.size)
        return EncryptedData(encryptedData, iv)
    }
    
    /**
     * Store backup metadata securely
     */
    private suspend fun storeBackupMetadata(metadata: BackupMetadata) {
        val preferences = encryptionService.getEncryptedSharedPreferences("backup_metadata")
        val serializedMetadata = kotlinx.serialization.json.Json.encodeToString(metadata)
        val encryptedMetadata = encryptionService.encrypt(serializedMetadata)
        
        preferences.edit()
            .putString("backup_${metadata.backupPath.hashCode()}", serializeEncryptedData(encryptedMetadata))
            .apply()
    }
    
    /**
     * Get backup metadata
     */
    private suspend fun getBackupMetadata(backupPath: String): BackupMetadata? {
        return try {
            val preferences = encryptionService.getEncryptedSharedPreferences("backup_metadata")
            val encryptedData = preferences.getString("backup_${backupPath.hashCode()}", null)
                ?: return null
            
            val metadata = deserializeEncryptedData(encryptedData)
            val decryptedMetadata = encryptionService.decrypt(metadata)
            kotlinx.serialization.json.Json.decodeFromString<BackupMetadata>(decryptedMetadata)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        database?.close()
        database = null
        databaseKey?.fill(0) // Securely wipe key from memory
        databaseKey = null
    }
}

/**
 * Backup metadata
 */
@kotlinx.serialization.Serializable
data class BackupMetadata(
    val backupPath: String,
    val creationTime: Long,
    @kotlinx.serialization.Serializable(with = ByteArraySerializer::class)
    val encryptionKey: ByteArray,
    @kotlinx.serialization.Serializable(with = ByteArraySerializer::class)
    val iv: ByteArray,
    @kotlinx.serialization.Serializable(with = ByteArraySerializer::class)
    val checksum: ByteArray
)

/**
 * Backup result
 */
data class BackupResult(
    val success: Boolean,
    val backupPath: String? = null,
    val backupSize: Long = 0L,
    val error: String? = null,
    val creationTime: Long
)

/**
 * Restore result
 */
data class RestoreResult(
    val success: Boolean,
    val restoredSize: Long = 0L,
    val error: String? = null,
    val restoreTime: Long
)

/**
 * Deletion result
 */
data class DeletionResult(
    val success: Boolean,
    val deletedCount: Int = 0,
    val dataType: DataType,
    val error: String? = null,
    val deletionTime: Long
)

/**
 * Storage statistics
 */
data class StorageStatistics(
    val databaseSize: Long,
    val audioFilesSize: Long,
    val cacheSize: Long,
    val totalSize: Long,
    val lastUpdated: Long
)

/**
 * ByteArray serializer for Kotlinx Serialization
 */
object ByteArraySerializer : kotlinx.serialization.KSerializer<ByteArray> {
    override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("ByteArray", kotlinx.serialization.descriptors.PrimitiveKind.STRING)
    
    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: ByteArray) {
        encoder.encodeString(android.util.Base64.encodeToString(value, android.util.Base64.DEFAULT))
    }
    
    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): ByteArray {
        return android.util.Base64.decode(decoder.decodeString(), android.util.Base64.DEFAULT)
    }
}