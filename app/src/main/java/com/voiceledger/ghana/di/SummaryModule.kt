package com.voiceledger.ghana.di

import android.content.Context
import com.voiceledger.ghana.domain.service.DailySummaryGenerator
import com.voiceledger.ghana.domain.service.SummaryPresentationService
import com.voiceledger.ghana.domain.repository.TransactionRepository
import com.voiceledger.ghana.domain.repository.SpeakerProfileRepository
import com.voiceledger.ghana.domain.repository.DailySummaryRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for summary generation services
 */
@Module
@InstallIn(SingletonComponent::class)
object SummaryModule {
    
    @Provides
    @Singleton
    fun provideDailySummaryGenerator(
        transactionRepository: TransactionRepository,
        speakerProfileRepository: SpeakerProfileRepository,
        dailySummaryRepository: DailySummaryRepository
    ): DailySummaryGenerator {
        return DailySummaryGenerator(
            transactionRepository,
            speakerProfileRepository,
            dailySummaryRepository
        )
    }

    @Provides
    @Singleton
    fun provideSummaryPresentationService(
        @ApplicationContext context: Context,
        dailySummaryRepository: DailySummaryRepository
    ): SummaryPresentationService {
        return SummaryPresentationService(context, dailySummaryRepository)
    }
}