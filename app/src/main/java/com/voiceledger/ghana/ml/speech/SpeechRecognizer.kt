package com.voiceledger.ghana.ml.speech

/**
 * Interface for speech recognition with multilingual support
 * Supports Ghana English, Twi, and Ga languages with code-switching
 */
interface SpeechRecognizer {
    
    /**
     * Transcribe audio data to text
     * @param audioData Raw audio data (PCM 16-bit, 16kHz)
     * @param languageHints Preferred languages for recognition
     * @return Transcription result with confidence and language detection
     */
    suspend fun transcribe(audioData: ByteArray, languageHints: List<String> = listOf("en-GH")): TranscriptionResult
    
    /**
     * Start streaming recognition session
     * @param languageHints Preferred languages for recognition
     * @param callback Callback for receiving streaming results
     */
    suspend fun startStreaming(
        languageHints: List<String> = listOf("en-GH"),
        callback: (StreamingResult) -> Unit
    ): StreamingSession
    
    /**
     * Stop streaming recognition session
     */
    suspend fun stopStreaming(session: StreamingSession)
    
    /**
     * Set language model preferences
     * @param languages List of language codes (en-GH, tw, ga)
     */
    fun setLanguageModel(languages: List<String>)
    
    /**
     * Check if offline recognition is available
     */
    fun isOfflineCapable(): Boolean
    
    /**
     * Check if a specific language is supported
     */
    fun isLanguageSupported(languageCode: String): Boolean
    
    /**
     * Get supported languages
     */
    fun getSupportedLanguages(): List<String>
    
    /**
     * Enable or disable code-switching detection
     */
    fun setCodeSwitchingEnabled(enabled: Boolean)
}

/**
 * Result of speech transcription
 */
data class TranscriptionResult(
    val transcript: String,
    val confidence: Float,
    val detectedLanguage: String?,
    val alternatives: List<TranscriptionAlternative> = emptyList(),
    val processingTimeMs: Long,
    val isFromCache: Boolean = false,
    val error: String? = null
) {
    val isSuccess: Boolean get() = error == null
    val isEmpty: Boolean get() = transcript.isBlank()
}

/**
 * Alternative transcription result
 */
data class TranscriptionAlternative(
    val transcript: String,
    val confidence: Float,
    val detectedLanguage: String?
)

/**
 * Streaming recognition result
 */
data class StreamingResult(
    val transcript: String,
    val confidence: Float,
    val isFinal: Boolean,
    val detectedLanguage: String?,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Streaming recognition session
 */
interface StreamingSession {
    val sessionId: String
    val isActive: Boolean
    
    /**
     * Send audio chunk to streaming session
     */
    suspend fun sendAudioChunk(audioData: ByteArray)
    
    /**
     * Finish streaming session
     */
    suspend fun finish(): TranscriptionResult?
    
    /**
     * Cancel streaming session
     */
    suspend fun cancel()
}

/**
 * Language detection result
 */
data class LanguageDetectionResult(
    val detectedLanguage: String,
    val confidence: Float,
    val alternatives: List<LanguageAlternative> = emptyList()
)

/**
 * Alternative language detection
 */
data class LanguageAlternative(
    val language: String,
    val confidence: Float
)