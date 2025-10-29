package com.voiceledger.ghana.di

import android.content.Context
import androidx.room.Room
import com.voiceledger.ghana.data.local.dao.*
import com.voiceledger.ghana.data.local.database.VoiceLedgerDatabase
import com.voiceledger.ghana.data.repository.*
import com.voiceledger.ghana.domain.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Hilt module for database and repository dependencies
 * Provides all database-related dependencies for dependency injection
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    /**
     * Provides the main Room database instance
     * Uses encrypted database in production for security
     */
    @Provides
    @Singleton
    fun provideVoiceLedgerDatabase(@ApplicationContext context: Context): VoiceLedgerDatabase {
        return VoiceLedgerDatabase.getDatabase(context)
    }
    
    /**
     * Provides encrypted database instance for production use
     * This would be used when security is required
     */
    @Provides
    @Singleton
    @EncryptedDatabase
    fun provideEncryptedVoiceLedgerDatabase(@ApplicationContext context: Context): VoiceLedgerDatabase {
        // In production, the passphrase would come from secure storage or user input
        val passphrase = "ghana_voice_ledger_secure_key_2024"
        return VoiceLedgerDatabase.getEncryptedDatabase(context, passphrase)
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

/**
 * Qualifier annotation for encrypted database
 * Used when we need the encrypted version of the database
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class EncryptedDatabase