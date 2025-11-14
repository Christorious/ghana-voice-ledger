package com.voiceledger.ghana.di

import android.content.Context
import androidx.room.Room
import com.voiceledger.ghana.data.local.dao.*
import com.voiceledger.ghana.data.local.database.DatabaseFactory
import com.voiceledger.ghana.data.local.database.DatabaseMigrations
import com.voiceledger.ghana.data.local.database.VoiceLedgerDatabase
import com.voiceledger.ghana.data.repository.*
import com.voiceledger.ghana.domain.repository.*
import com.voiceledger.ghana.security.SecurityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.room.SupportFactory
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Hilt module for database and repository dependencies
 * Provides all database-related dependencies for dependency injection
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    private const val USE_ENCRYPTION = false
    private const val DATABASE_PASSPHRASE = "ghana_voice_ledger_secure_key_2024"
    
    /**
     * Provides the main Room database instance.
     * Centralizes creation logic through DatabaseFactory, which handles both
     * encrypted and non-encrypted configurations with shared builder setup.
     */
    @Provides
    @Singleton
    fun provideVoiceLedgerDatabase(@ApplicationContext context: Context): VoiceLedgerDatabase {
        return if (USE_ENCRYPTION) {
            provideEncryptedVoiceLedgerDatabase(context, buildSecurityManager())
        } else {
            DatabaseFactory.createDatabase(
                context = context,
                encrypted = false
            )
        }
    }
    
    private fun buildSecurityManager(): SecurityManager {
        return SecurityManager.getInstance()
    }
    
    /**
     * Provides encrypted database instance for production use
     * This would be used when security is required
     */
    @Provides
    @Singleton
    @EncryptedDatabase
    fun provideEncryptedVoiceLedgerDatabase(
        @ApplicationContext context: Context,
        securityManager: SecurityManager
    ): VoiceLedgerDatabase {
        return buildEncryptedDatabase(context, securityManager)
    }
    
    private fun buildEncryptedDatabase(
        context: Context,
        securityManager: SecurityManager
    ): VoiceLedgerDatabase {
        val passphraseChars = securityManager.getDatabasePassphrase().toCharArray()
        return try {
            val passphraseBytes = SQLiteDatabase.getBytes(passphraseChars)
            Room.databaseBuilder(
                context.applicationContext,
                VoiceLedgerDatabase::class.java,
                VoiceLedgerDatabase.DATABASE_NAME
            )
                .openHelperFactory(SupportFactory(passphraseBytes))
                .addMigrations(*DatabaseMigrations.getAllMigrations())
                .addCallback(VoiceLedgerDatabase.createSeedDataCallback(context))
                .build()
        } finally {
            passphraseChars.fill('\u0000')
        }
    }
    
    // DAO Providers
    
    @Provides
    fun provideTransactionDao(database: VoiceLedgerDatabase): TransactionDao {
        return database.transactionDao()
    }
    
    @Provides
    fun provideDailySummaryDao(database: VoiceLedgerDatabase): DailySummaryDao {
        return database.dailySummaryDao()
    }
    
    @Provides
    fun provideSpeakerProfileDao(database: VoiceLedgerDatabase): SpeakerProfileDao {
        return database.speakerProfileDao()
    }
    
    @Provides
    fun provideProductVocabularyDao(database: VoiceLedgerDatabase): ProductVocabularyDao {
        return database.productVocabularyDao()
    }
    
    @Provides
    fun provideAudioMetadataDao(database: VoiceLedgerDatabase): AudioMetadataDao {
        return database.audioMetadataDao()
    }
    
    @Provides
    fun provideOfflineOperationDao(database: VoiceLedgerDatabase): OfflineOperationDao {
        return database.offlineOperationDao()
    }
    
    // Repository Providers
    
    @Provides
    @Singleton
    fun provideTransactionRepository(
        transactionDao: TransactionDao
    ): TransactionRepository {
        return TransactionRepositoryImpl(transactionDao)
    }
    
    @Provides
    @Singleton
    fun provideDailySummaryRepository(
        dailySummaryDao: DailySummaryDao,
        transactionDao: TransactionDao
    ): DailySummaryRepository {
        return DailySummaryRepositoryImpl(dailySummaryDao, transactionDao)
    }
    
    @Provides
    @Singleton
    fun provideSpeakerProfileRepository(
        speakerProfileDao: SpeakerProfileDao
    ): SpeakerProfileRepository {
        return SpeakerProfileRepositoryImpl(speakerProfileDao)
    }
    
    @Provides
    @Singleton
    fun provideProductVocabularyRepository(
        productVocabularyDao: ProductVocabularyDao
    ): ProductVocabularyRepository {
        return ProductVocabularyRepositoryImpl(productVocabularyDao)
    }
    
    @Provides
    @Singleton
    fun provideAudioMetadataRepository(
        audioMetadataDao: AudioMetadataDao
    ): AudioMetadataRepository {
        return AudioMetadataRepositoryImpl(audioMetadataDao)
    }
}