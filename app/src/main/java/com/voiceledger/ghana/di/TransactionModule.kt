package com.voiceledger.ghana.di

import com.voiceledger.ghana.ml.transaction.TransactionPatternMatcher
import com.voiceledger.ghana.ml.transaction.TransactionStateMachine
import com.voiceledger.ghana.ml.transaction.TransactionProcessor
import com.voiceledger.ghana.domain.repository.TransactionRepository
import com.voiceledger.ghana.domain.repository.ProductVocabularyRepository
import com.voiceledger.ghana.domain.repository.AudioMetadataRepository
import com.voiceledger.ghana.security.SecurityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for transaction processing dependencies
 * Provides all transaction-related components for dependency injection
 */
@Module
@InstallIn(SingletonComponent::class)
object TransactionModule {
    
    /**
     * Provides transaction pattern matcher
     */
    @Provides
    @Singleton
    fun provideTransactionPatternMatcher(): TransactionPatternMatcher {
        return TransactionPatternMatcher()
    }
    
    /**
     * Provides transaction state machine
     */
    @Provides
    @Singleton
    fun provideTransactionStateMachine(
        patternMatcher: TransactionPatternMatcher,
        securityManager: SecurityManager
    ): TransactionStateMachine {
        return TransactionStateMachine(patternMatcher, securityManager)
    }
    
    /**
     * Provides transaction processor
     */
    @Provides
    @Singleton
    fun provideTransactionProcessor(
        stateMachine: TransactionStateMachine,
        transactionRepository: TransactionRepository,
        productVocabularyRepository: ProductVocabularyRepository,
        audioMetadataRepository: AudioMetadataRepository
    ): TransactionProcessor {
        return TransactionProcessor(
            stateMachine = stateMachine,
            transactionRepository = transactionRepository,
            productVocabularyRepository = productVocabularyRepository,
            audioMetadataRepository = audioMetadataRepository
        )
    }
}