package com.voiceledger.ghana.di

import android.content.Context
import com.voiceledger.ghana.ml.vad.VADManager
import com.voiceledger.ghana.ml.vad.VADProcessor
import com.voiceledger.ghana.ml.vad.WebRTCVADProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Voice Activity Detection dependencies
 * Provides VAD processors and manager for dependency injection
 */
@Module
@InstallIn(SingletonComponent::class)
object VADModule {
    
    /**
     * Provides custom VAD processor optimized for Ghana markets
     */
    @Provides
    @Singleton
    fun provideVADProcessor(@ApplicationContext context: Context): VADProcessor {
        return VADProcessor(context)
    }
    
    /**
     * Provides WebRTC-based VAD processor
     */
    @Provides
    @Singleton
    fun provideWebRTCVADProcessor(@ApplicationContext context: Context): WebRTCVADProcessor {
        return WebRTCVADProcessor(context)
    }
    
    /**
     * Provides VAD manager that coordinates different VAD implementations
     */
    @Provides
    @Singleton
    fun provideVADManager(
        @ApplicationContext context: Context,
        vadProcessor: VADProcessor,
        webRTCVADProcessor: WebRTCVADProcessor
    ): VADManager {
        return VADManager(context, vadProcessor, webRTCVADProcessor)
    }
}