package com.voiceledger.ghana.ml.speech

import android.content.Context
import android.util.Log
import com.google.cloud.speech.v1.*
import com.google.protobuf.ByteString
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Cloud Speech-to-Text implementation with Ghana language support
 * Supports en-GH (Ghana English), Twi, and Ga languages
 */
@Singleton
class GoogleCloudSpeechRecognizer @Inject constructor(
    private val context: Context
) : SpeechRecognizer {
    
    companion object {
        private const val TAG = "GoogleCloudSpeech"
        private const val SAMPLE_RATE = 16000
        private const val AUDIO_ENCODING = RecognitionConfig.AudioEncoding.LINEAR16
        
        // Ghana-specific language codes
        private const val GHANA_ENGLISH = "en-GH"
        private const val TWI_LANGUAGE = "tw"
        private const val GA_LANGUAGE = "ga"
        
        // Fallback languages
        private const val ENGLISH_US = "en-US"
        private const val ENGLISH_UK = "en-GB"
    }
    
    private var speechClient: SpeechClient? = null
    private var currentLanguages = listOf(GHANA_ENGLISH)
    private var codeSwitchingEnabled = true
    private val activeStreamingSessions = mutableMapOf<String, GoogleStreamingSession>()
    
    init {
        initializeSpeechClient()
    }
    
    private fun initializeSpeechClient() {
        try {
            // Initialize Google Cloud Speech client
            // In production, credentials would be loaded from secure storage
            speechClient = SpeechClient.create()
            Log.d(TAG, "Google Cloud Speech client initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Google Cloud Speech client", e)
        }
    }
    
    override suspend fun transcribe(audioData: ByteArray, languageHints: List<String>): TranscriptionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        try {
            val client = speechClient ?: return@withContext TranscriptionResult(
                transcript = "",
                confidence = 0f,
                detectedLanguage = null,
                processingTimeMs = System.currentTimeMillis() - startTime,
                error = "Speech client not initialized"
            )
            
            val config = buildRecognitionConfig(languageHints)
            val audio = RecognitionAudio.newBuilder()
                .setContent(ByteString.copyFrom(audioData))
                .build()
            
            val request = RecognizeRequest.newBuilder()
                .setConfig(config)
                .setAudio(audio)
                .build()
            
            val response = client.recognize(request)
            val processingTime = System.currentTimeMillis() - startTime
            
            if (response.resultsCount > 0) {
                val result = response.getResults(0)
                val alternative = result.getAlternatives(0)
                
                val alternatives = result.alternativesList.drop(1).map { alt ->
                    TranscriptionAlternative(
                        transcript = alt.transcript,
                        confidence = alt.confidence,
                        detectedLanguage = result.languageCode.takeIf { it.isNotEmpty() }
                    )
                }
                
                TranscriptionResult(
                    transcript = alternative.transcript,
                    confidence = alternative.confidence,
                    detectedLanguage = result.languageCode.takeIf { it.isNotEmpty() },
                    alternatives = alternatives,
                    processingTimeMs = processingTime
                )
            } else {
                TranscriptionResult(
                    transcript = "",
                    confidence = 0f,
                    detectedLanguage = null,
                    processingTimeMs = processingTime,
                    error = "No transcription results"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed", e)
            TranscriptionResult(
                transcript = "",
                confidence = 0f,
                detectedLanguage = null,
                processingTimeMs = System.currentTimeMillis() - startTime,
                error = e.message ?: "Unknown error"
            )
        }
    }
    
    override suspend fun startStreaming(
        languageHints: List<String>,
        callback: (StreamingResult) -> Unit
    ): StreamingSession = withContext(Dispatchers.IO) {
        val sessionId = UUID.randomUUID().toString()
        val session = GoogleStreamingSession(sessionId, languageHints, callback)
        activeStreamingSessions[sessionId] = session
        session.start()
        session
    }
    
    override suspend fun stopStreaming(session: StreamingSession) {
        activeStreamingSessions.remove(session.sessionId)?.let { googleSession ->
            googleSession.cancel()
        }
    }
    
    override fun setLanguageModel(languages: List<String>) {
        currentLanguages = languages.ifEmpty { listOf(GHANA_ENGLISH) }
        Log.d(TAG, "Language model set to: $currentLanguages")
    }
    
    override fun isOfflineCapable(): Boolean = false // Google Cloud Speech requires internet
    
    override fun isLanguageSupported(languageCode: String): Boolean {
        return when (languageCode.lowercase()) {
            GHANA_ENGLISH.lowercase(), 
            TWI_LANGUAGE.lowercase(), 
            GA_LANGUAGE.lowercase(),
            ENGLISH_US.lowercase(),
            ENGLISH_UK.lowercase() -> true
            else -> false
        }
    }
    
    override fun getSupportedLanguages(): List<String> {
        return listOf(GHANA_ENGLISH, TWI_LANGUAGE, GA_LANGUAGE, ENGLISH_US, ENGLISH_UK)
    }
    
    override fun setCodeSwitchingEnabled(enabled: Boolean) {
        codeSwitchingEnabled = enabled
        Log.d(TAG, "Code-switching ${if (enabled) "enabled" else "disabled"}")
    }
    
    private fun buildRecognitionConfig(languageHints: List<String>): RecognitionConfig {
        val configBuilder = RecognitionConfig.newBuilder()
            .setEncoding(AUDIO_ENCODING)
            .setSampleRateHertz(SAMPLE_RATE)
            .setLanguageCode(languageHints.firstOrNull() ?: GHANA_ENGLISH)
            .setMaxAlternatives(3)
            .setEnableAutomaticPunctuation(true)
            .setEnableWordTimeOffsets(false)
            .setEnableWordConfidence(true)
        
        // Add alternative language codes for code-switching
        if (codeSwitchingEnabled && languageHints.size > 1) {
            languageHints.drop(1).forEach { lang ->
                configBuilder.addAlternativeLanguageCodes(lang)
            }
        }
        
        // Add Ghana-specific speech contexts
        val speechContexts = buildGhanaSpeechContexts()
        speechContexts.forEach { context ->
            configBuilder.addSpeechContexts(context)
        }
        
        return configBuilder.build()
    }
    
    private fun buildGhanaSpeechContexts(): List<SpeechContext> {
        val contexts = mutableListOf<SpeechContext>()
        
        // Ghana currency context
        contexts.add(
            SpeechContext.newBuilder()
                .addAllPhrases(listOf(
                    "cedis", "pesewas", "Ghana cedis", "GH₵",
                    "cedi", "pesewa", "twenty cedis", "fifty cedis"
                ))
                .setBoost(10.0f)
                .build()
        )
        
        // Fish market context
        contexts.add(
            SpeechContext.newBuilder()
                .addAllPhrases(listOf(
                    "tilapia", "apateshi", "tuo",
                    "mackerel", "kpanla", "titus",
                    "sardines", "herring", "sardin",
                    "red fish", "adwene",
                    "catfish", "sumbre",
                    "croaker", "komi"
                ))
                .setBoost(15.0f)
                .build()
        )
        
        // Common market phrases
        contexts.add(
            SpeechContext.newBuilder()
                .addAllPhrases(listOf(
                    "how much", "sɛn na ɛyɛ", "what price",
                    "reduce small", "too much", "my last price",
                    "customer price", "okay", "fine", "deal",
                    "here money", "take it", "thank you",
                    "I dey sell", "how much be this"
                ))
                .setBoost(12.0f)
                .build()
        )
        
        // Measurement units
        contexts.add(
            SpeechContext.newBuilder()
                .addAllPhrases(listOf(
                    "piece", "bowl", "kokoo", "bucket", "rubber",
                    "tin", "size", "small", "medium", "large"
                ))
                .setBoost(8.0f)
                .build()
        )
        
        return contexts
    }
    
    /**
     * Google Cloud Speech streaming session implementation
     */
    private inner class GoogleStreamingSession(
        override val sessionId: String,
        private val languageHints: List<String>,
        private val callback: (StreamingResult) -> Unit
    ) : StreamingSession {
        
        override var isActive: Boolean = false
            private set
        
        private var streamingRecognizeCall: BidiStreamingCallable<StreamingRecognizeRequest, StreamingRecognizeResponse>? = null
        private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        
        suspend fun start() {
            try {
                val client = speechClient ?: throw IllegalStateException("Speech client not initialized")
                
                val config = buildRecognitionConfig(languageHints)
                val streamingConfig = StreamingRecognitionConfig.newBuilder()
                    .setConfig(config)
                    .setInterimResults(true)
                    .setSingleUtterance(false)
                    .build()
                
                val initialRequest = StreamingRecognizeRequest.newBuilder()
                    .setStreamingConfig(streamingConfig)
                    .build()
                
                streamingRecognizeCall = client.streamingRecognizeCallable()
                
                // Start streaming
                scope.launch {
                    try {
                        streamingRecognizeCall?.let { call ->
                            val requestObserver = call.splitCall()
                            requestObserver.onNext(initialRequest)
                            
                            isActive = true
                            Log.d(TAG, "Streaming session $sessionId started")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start streaming session", e)
                        isActive = false
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize streaming session", e)
                throw e
            }
        }
        
        override suspend fun sendAudioChunk(audioData: ByteArray) {
            if (!isActive) return
            
            try {
                val audioRequest = StreamingRecognizeRequest.newBuilder()
                    .setAudioContent(ByteString.copyFrom(audioData))
                    .build()
                
                // Send audio chunk (simplified implementation)
                // In real implementation, this would use the streaming call
                Log.d(TAG, "Sent audio chunk of ${audioData.size} bytes")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send audio chunk", e)
            }
        }
        
        override suspend fun finish(): TranscriptionResult? {
            isActive = false
            scope.cancel()
            return null // Simplified implementation
        }
        
        override suspend fun cancel() {
            isActive = false
            scope.cancel()
            streamingRecognizeCall = null
            Log.d(TAG, "Streaming session $sessionId cancelled")
        }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        activeStreamingSessions.values.forEach { session ->
            runBlocking { session.cancel() }
        }
        activeStreamingSessions.clear()
        
        speechClient?.close()
        speechClient = null
        Log.d(TAG, "Google Cloud Speech client cleaned up")
    }
}