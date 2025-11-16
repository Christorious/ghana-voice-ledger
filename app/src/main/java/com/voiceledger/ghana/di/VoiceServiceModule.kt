package com.voiceledger.ghana.di

import android.content.Context
import com.voiceledger.ghana.service.AudioCaptureController
import com.voiceledger.ghana.service.SpeechProcessingPipeline
import com.voiceledger.ghana.service.VoiceNotificationHelper
import com.voiceledger.ghana.service.VoiceSessionCoordinator
import com.voiceledger.ghana.ml.vad.VADManager
import com.voiceledger.ghana.ml.speaker.SpeakerIdentifier
import com.voiceledger.ghana.ml.speech.SpeechRecognitionManager
import com.voiceledger.ghana.ml.transaction.TransactionProcessor
import com.voiceledger.ghana.domain.repository.AudioMetadataRepository
import com.voiceledger.ghana.offline.OfflineQueueManager
import com.voiceledger.ghana.service.PowerManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineDispatcher
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
    fun provideSpeechProcessingPipeline(
        vadManager: VADManager,
        speakerIdentifier: SpeakerIdentifier,
        speechRecognitionManager: SpeechRecognitionManager,
        transactionProcessor: TransactionProcessor,
        audioMetadataRepository: AudioMetadataRepository,
        defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    ): SpeechProcessingPipeline {
        return SpeechProcessingPipeline(
            vadManager,
            speakerIdentifier,
            speechRecognitionManager,
            transactionProcessor,
            audioMetadataRepository,
            defaultDispatcher,
            ioDispatcher
        )
    }
    
    @Provides
    @Singleton
    fun provideVoiceSessionCoordinator(
        @ApplicationContext context: Context,
        audioCaptureController: AudioCaptureController,
        speechProcessingPipeline: SpeechProcessingPipeline,
        notificationHelper: VoiceNotificationHelper,
        vadManager: VADManager,
        speechRecognitionManager: SpeechRecognitionManager,
        offlineQueueManager: OfflineQueueManager,
        powerManager: PowerManager,
        defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
    ): VoiceSessionCoordinator {
        return VoiceSessionCoordinator(
            context,
            audioCaptureController,
            speechProcessingPipeline,
            notificationHelper,
            vadManager,
            speechRecognitionManager,
            offlineQueueManager,
            powerManager,
            defaultDispatcher
        )
    }
    
    @Provides
    @Singleton
    fun provideVoiceNotificationHelper(
        @ApplicationContext context: Context
    ): VoiceNotificationHelper {
        return VoiceNotificationHelper(context)
    }
}
