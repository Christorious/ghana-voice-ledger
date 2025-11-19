package com.voiceledger.ghana.di

import android.content.Context
import com.voiceledger.ghana.domain.repository.SpeakerProfileRepository
import com.voiceledger.ghana.ml.audio.AudioUtils
import com.voiceledger.ghana.ml.speaker.SpeakerIdentifier
import com.voiceledger.ghana.ml.speaker.TensorFlowLiteSpeakerIdentifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for speaker identification dependencies
 * Provides speaker identification and enrollment components
 */
@Module
@InstallIn(SingletonComponent::class)
object SpeakerModule {
    
    /**
     * Provides the main speaker identifier implementation
     */
    @Provides
    @Singleton
    fun provideSpeakerIdentifier(
        @ApplicationContext context: Context,
        speakerRepository: SpeakerProfileRepository
    ): SpeakerIdentifier {
        return TensorFlowLiteSpeakerIdentifier(context, speakerRepository, AudioUtils)
    }
}