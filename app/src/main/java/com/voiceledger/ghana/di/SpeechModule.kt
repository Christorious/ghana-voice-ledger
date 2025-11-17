package com.voiceledger.ghana.di

import android.content.Context
import com.voiceledger.ghana.ml.speech.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Hilt module for speech recognition dependencies
 * Provides all speech-related components for dependency injection
 */
@Module
@InstallIn(SingletonComponent::class)
object SpeechModule {
    
    /**
     * Provides Google Cloud Speech recognizer
     */
    @Provides
    @Singleton
    fun provideGoogleCloudSpeechRecognizer(
        @ApplicationContext context: Context
    ): GoogleCloudSpeechRecognizer {
        return GoogleCloudSpeechRecognizer(context)
    }
    
    /**
     * Provides offline speech recognizer
     */
    @Provides
    @Singleton
    fun provideOfflineSpeechRecognizer(
        @ApplicationContext context: Context
    ): OfflineSpeechRecognizer {
        return OfflineSpeechRecognizer(context)
    }
    
    /**
     * Provides speech recognition manager
     */
    @Provides
    @Singleton
    fun provideSpeechRecognitionManager(
        @ApplicationContext context: Context,
        googleSpeechRecognizer: GoogleCloudSpeechRecognizer,
        offlineSpeechRecognizer: OfflineSpeechRecognizer
    ): SpeechRecognitionManager {
        return SpeechRecognitionManager(
            context = context,
            googleSpeechRecognizer = googleSpeechRecognizer,
            offlineSpeechRecognizer = offlineSpeechRecognizer
        )
    }
    
    /**
     * Provides language detector
     */
    @Provides
    @Singleton
    fun provideLanguageDetector(): LanguageDetector {
        return LanguageDetector()
    }
    
    /**
     * Provides primary speech recognizer interface
     * Uses the manager as the main implementation
     */
    @Provides
    @Singleton
    @PrimarySpeechRecognizer
    fun providePrimarySpeechRecognizer(
        speechRecognitionManager: SpeechRecognitionManager
    ): SpeechRecognizer {
        return object : SpeechRecognizer {
            override suspend fun transcribe(audioData: ByteArray, languageHints: List<String>): TranscriptionResult {
                return speechRecognitionManager.transcribe(audioData, languageHints)
            }
            
            override suspend fun startStreaming(
                languageHints: List<String>,
                callback: (StreamingResult) -> Unit
            ): StreamingSession {
                return speechRecognitionManager.startStreamingRecognition(languageHints, callback)
            }
            
            override suspend fun stopStreaming(session: StreamingSession) {
                speechRecognitionManager.stopStreamingRecognition(session)
            }
            
            override fun setLanguageModel(languages: List<String>) {
                speechRecognitionManager.setLanguagePreferences(languages)
            }
            
            override fun isOfflineCapable(): Boolean {
                return speechRecognitionManager.getRecognitionCapabilities().offlineAvailable
            }
            
            override fun isLanguageSupported(languageCode: String): Boolean {
                return speechRecognitionManager.isLanguageSupported(languageCode)
            }
            
            override fun getSupportedLanguages(): List<String> {
                return speechRecognitionManager.getSupportedLanguages()
            }
            
            override fun setCodeSwitchingEnabled(enabled: Boolean) {
                speechRecognitionManager.setCodeSwitchingEnabled(enabled)
            }
        }
    }
    
    /**
     * Provides online-only speech recognizer
     */
    @Provides
    @Singleton
    @OnlineSpeechRecognizer
    fun provideOnlineSpeechRecognizer(
        googleSpeechRecognizer: GoogleCloudSpeechRecognizer
    ): SpeechRecognizer {
        return googleSpeechRecognizer
    }
    
    /**
     * Provides offline-only speech recognizer interface
     */
    @Provides
    @Singleton
    @OfflineSpeechRecognizer
    fun provideOfflineSpeechRecognizerInterface(
        offlineSpeechRecognizer: OfflineSpeechRecognizer
    ): SpeechRecognizer {
        return offlineSpeechRecognizer
    }
}

/**
 * Qualifier for primary speech recognizer (manager)
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PrimarySpeechRecognizer

/**
 * Qualifier for online speech recognizer
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OnlineSpeechRecognizer

/**
 * Qualifier for offline speech recognizer
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OfflineSpeechRecognizer