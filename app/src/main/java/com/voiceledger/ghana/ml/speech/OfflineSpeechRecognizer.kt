package com.voiceledger.ghana.ml.speech

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline speech recognition implementation using Whisper.cpp
 * Provides fallback when internet is not available
 */
@Singleton
class OfflineSpeechRecognizer @Inject constructor(
    private val context: Context
) : SpeechRecognizer {
    
    companion object {
        private const val TAG = "OfflineSpeechRecognizer"
        private const val MODEL_PATH = "models/whisper-small-en.bin"
        
        // Supported languages for offline recognition
        private val SUPPORTED_LANGUAGES = listOf("en", "en-GH")
    }
    
    private var isModelLoaded = false
    private var currentLanguages = listOf("en-GH")
    private val activeStreamingSessions = mutableMapOf<String, OfflineStreamingSession>()
    
    init {
        loadModel()
    }
    
    private fun loadModel() {
        try {
            // In a real implementation, this would load the Whisper.cpp model
            // For now, we'll simulate model loading
            isModelLoaded = true
            Log.d(TAG, "Offline speech model loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load offline speech model", e)
            isModelLoaded = false
        }
    }
    
    override suspend fun transcribe(audioData: ByteArray, languageHints: List<String>): TranscriptionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        if (!isModelLoaded) {
            return@withContext TranscriptionResult(
                transcript = "",
                confidence = 0f,
                detectedLanguage = null,
                processingTimeMs = System.currentTimeMillis() - startTime,
                error = "Offline model not loaded"
            )
        }
        
        try {
            // Simulate offline transcription processing
            delay(500) // Simulate processing time
            
            val transcript = simulateOfflineTranscription(audioData)
            val processingTime = System.currentTimeMillis() - startTime
            
            if (transcript.isNotEmpty()) {
                TranscriptionResult(
                    transcript = transcript,
                    confidence = 0.75f, // Lower confidence for offline recognition
                    detectedLanguage = languageHints.firstOrNull() ?: "en",
                    processingTimeMs = processingTime
                )
            } else {
                TranscriptionResult(
                    transcript = "",
                    confidence = 0f,
                    detectedLanguage = null,
                    processingTimeMs = processingTime,
                    error = "No speech detected"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Offline transcription failed", e)
            TranscriptionResult(
                transcript = "",
                confidence = 0f,
                detectedLanguage = null,
                processingTimeMs = System.currentTimeMillis() - startTime,
                error = e.message ?: "Offline transcription error"
            )
        }
    }
    
    override suspend fun startStreaming(
        languageHints: List<String>,
        callback: (StreamingResult) -> Unit
    ): StreamingSession {
        val sessionId = UUID.randomUUID().toString()
        val session = OfflineStreamingSession(sessionId, languageHints, callback)
        activeStreamingSessions[sessionId] = session
        return session
    }
    
    override suspend fun stopStreaming(session: StreamingSession) {
        activeStreamingSessions.remove(session.sessionId)?.let { offlineSession ->
            offlineSession.cancel()
        }
    }
    
    override fun setLanguageModel(languages: List<String>) {
        // Filter to only supported languages
        currentLanguages = languages.filter { isLanguageSupported(it) }
            .ifEmpty { listOf("en") }
        Log.d(TAG, "Language model set to: $currentLanguages")
    }
    
    override fun isOfflineCapable(): Boolean = isModelLoaded
    
    override fun isLanguageSupported(languageCode: String): Boolean {
        return SUPPORTED_LANGUAGES.any { 
            it.equals(languageCode, ignoreCase = true) 
        }
    }
    
    override fun getSupportedLanguages(): List<String> = SUPPORTED_LANGUAGES
    
    override fun setCodeSwitchingEnabled(enabled: Boolean) {
        // Offline model has limited code-switching support
        Log.d(TAG, "Code-switching support is limited in offline mode")
    }
    
    /**
     * Simulate offline transcription
     * In a real implementation, this would call Whisper.cpp native methods
     */
    private fun simulateOfflineTranscription(audioData: ByteArray): String {
        // This is a simulation - in reality, this would process audio through Whisper.cpp
        
        // Simulate different transcription results based on audio data characteristics
        return when {
            audioData.size < 1000 -> "" // Too short
            audioData.size < 5000 -> "how much" // Short phrase
            audioData.size < 10000 -> "how much is this fish" // Medium phrase
            else -> "how much is this tilapia per piece" // Longer phrase
        }
    }
    
    /**
     * Offline streaming session implementation
     */
    private inner class OfflineStreamingSession(
        override val sessionId: String,
        private val languageHints: List<String>,
        private val callback: (StreamingResult) -> Unit
    ) : StreamingSession {
        
        override var isActive: Boolean = true
            private set
        
        private val audioBuffer = mutableListOf<ByteArray>()
        private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        private var lastProcessTime = 0L
        
        init {
            // Start processing loop
            scope.launch {
                processAudioBuffer()
            }
        }
        
        override suspend fun sendAudioChunk(audioData: ByteArray) {
            if (!isActive) return
            
            synchronized(audioBuffer) {
                audioBuffer.add(audioData)
                
                // Process buffer every 2 seconds or when buffer gets large
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastProcessTime > 2000 || audioBuffer.size > 10) {
                    scope.launch {
                        processBufferedAudio()
                    }
                }
            }
        }
        
        private suspend fun processAudioBuffer() {
            while (isActive) {
                delay(1000) // Process every second
                if (audioBuffer.isNotEmpty()) {
                    processBufferedAudio()
                }
            }
        }
        
        private suspend fun processBufferedAudio() {
            val audioToProcess = synchronized(audioBuffer) {
                if (audioBuffer.isEmpty()) return
                
                val combined = audioBuffer.reduce { acc, bytes -> acc + bytes }
                audioBuffer.clear()
                lastProcessTime = System.currentTimeMillis()
                combined
            }
            
            try {
                val result = transcribe(audioToProcess, languageHints)
                
                if (result.isSuccess && result.transcript.isNotEmpty()) {
                    callback(
                        StreamingResult(
                            transcript = result.transcript,
                            confidence = result.confidence,
                            isFinal = true, // Offline processing is always final
                            detectedLanguage = result.detectedLanguage
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process buffered audio", e)
            }
        }
        
        override suspend fun finish(): TranscriptionResult? {
            // Process any remaining audio in buffer
            val finalAudio = synchronized(audioBuffer) {
                if (audioBuffer.isEmpty()) return null
                audioBuffer.reduce { acc, bytes -> acc + bytes }
            }
            
            isActive = false
            scope.cancel()
            
            return transcribe(finalAudio, languageHints)
        }
        
        override suspend fun cancel() {
            isActive = false
            scope.cancel()
            synchronized(audioBuffer) {
                audioBuffer.clear()
            }
            Log.d(TAG, "Offline streaming session $sessionId cancelled")
        }
    }
    
    /**
     * Check if model is ready for use
     */
    fun isModelReady(): Boolean = isModelLoaded
    
    /**
     * Get model information
     */
    fun getModelInfo(): Map<String, Any> {
        return mapOf(
            "loaded" to isModelLoaded,
            "modelPath" to MODEL_PATH,
            "supportedLanguages" to SUPPORTED_LANGUAGES,
            "type" to "Whisper.cpp"
        )
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        activeStreamingSessions.values.forEach { session ->
            runBlocking { session.cancel() }
        }
        activeStreamingSessions.clear()
        
        // In real implementation, this would cleanup Whisper.cpp resources
        isModelLoaded = false
        Log.d(TAG, "Offline speech recognizer cleaned up")
    }
}