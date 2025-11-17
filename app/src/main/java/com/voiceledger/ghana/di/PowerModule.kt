package com.voiceledger.ghana.di

import android.content.Context
import com.voiceledger.ghana.service.PowerManager
import com.voiceledger.ghana.service.PowerOptimizationService
import com.voiceledger.ghana.service.VoiceAgentServiceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for power management dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object PowerModule {

    @Provides
    @Singleton
    fun providePowerManager(
        @ApplicationContext context: Context
    ): PowerManager {
        return PowerManager(context)
    }

    @Provides
    @Singleton
    fun provideVoiceAgentServiceManager(
        @ApplicationContext context: Context
    ): VoiceAgentServiceManager {
        return VoiceAgentServiceManager(context)
    }

    @Provides
    @Singleton
    fun providePowerOptimizationService(
        @ApplicationContext context: Context,
        powerManager: PowerManager,
        voiceAgentServiceManager: VoiceAgentServiceManager
    ): PowerOptimizationService {
        return PowerOptimizationService(context, powerManager, voiceAgentServiceManager)
    }
}