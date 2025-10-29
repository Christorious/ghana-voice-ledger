package com.voiceledger.ghana.di

import android.content.Context
import com.voiceledger.ghana.data.local.dao.*
import com.voiceledger.ghana.data.local.database.DatabaseFactory
import com.voiceledger.ghana.data.local.database.VoiceLedgerDatabase
import com.voiceledger.ghana.data.repository.*
import com.voiceledger.ghana.domain.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
        return DatabaseFactory.createDatabase(
            context = context,
            encrypted = USE_ENCRYPTION,
            passphrase = if (USE_ENCRYPTION) DATABASE_PASSPHRASE else null
        )
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