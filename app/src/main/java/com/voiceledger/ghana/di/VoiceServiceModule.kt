package com.voiceledger.ghana.di

import android.content.Context
import com.voiceledger.ghana.service.AudioCaptureController
import com.voiceledger.ghana.service.SpeechProcessingPipeline
import com.voiceledger.ghana.service.VoiceNotificationHelper
import com.voiceledger.ghana.service.VoiceSessionCoordinator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VoiceServiceModule {
    
    @Provides
    @Singleton
    fun provideAudioCaptureController(
        @ApplicationContext context: Context
    ): AudioCaptureController {
        return AudioCaptureController(context)
    }
    
    @Provides
    @Singleton
    fun provideVoiceNotificationHelper(
        @ApplicationContext context: Context
    ): VoiceNotificationHelper {
        return VoiceNotificationHelper(context)
    }
}
