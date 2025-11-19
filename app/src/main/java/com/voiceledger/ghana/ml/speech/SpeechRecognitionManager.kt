package com.voiceledger.ghana.ml.speech

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for speech recognition with automatic online/offline switching
 * Handles Ghana-specific language requirements and code-switching
 */
@Singleton
class SpeechRecognitionManager @Inject constructor(
    private val context: Context,
    private val googleSpeechRecognizer: GoogleCloudSpeechRecognizer?,
    private val offlineSpeechRecognizer: OfflineSpeechRecognizer
) {
    
    companion object {
        private const val TAG = "SpeechRecognitionManager"
        
        // Ghana language priorities
        private val DEFAULT_LANGUAGES = listOf("en-GH", "tw", "ga", "en-US")
        private val GHANA_LANGUAGES = listOf("en-GH", "tw", "ga")
    }
    
    private var currentLanguages = DEFAULT_LANGUAGES
    private var preferOnlineRecognition = true
    private var codeSwitchingEnabled = true
    private var networkAvailable = false
    
    private val _recognitionState = MutableStateFlow(RecognitionState.IDLE)
    val recognitionState: StateFlow<RecognitionState> = _recognitionState.asStateFlow()
    
    private val _languageDetection = MutableStateFlow<LanguageDetectionResult?>(null)
    val languageDetection: StateFlow<LanguageDetectionResult?> = _languageDetection.asStateFlow()
    
    init {
        monitorNetworkConnectivity()
        setupLanguageModels()
    }
    
    /**
     * Transcribe audio with automatic online/offline switching
     */
    suspend fun transcribe(audioData: ByteArray, languageHints: List<String>? = null): TranscriptionResult {
        val languages = languageHints ?: currentLanguages
        _recognitionState.value = RecognitionState.PROCESSING
        
        return try {
            val result = if (shouldUseOnlineRecognition() && googleSpeechRecognizer != null) {
                Log.d(TAG, "Using online speech recognition")
                googleSpeechRecognizer.transcribe(audioData, languages)
            } else {
                Log.d(TAG, "Using offline speech recognition")
                offlineSpeechRecognizer.transcribe(audioData, languages)
            }
            
            // Update language detection if available
            result.detectedLanguage?.let { detectedLang ->
                _languageDetection.value = LanguageDetectionResult(
                    detectedLanguage = detectedLang,
                    confidence = result.confidence
                )
            }
            
            _recognitionState.value = if (result.isSuccess) {
                RecognitionState.SUCCESS
            } else {
                RecognitionState.ERROR
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Speech recognition failed", e)
            _recognitionState.value = RecognitionState.ERROR
            
            TranscriptionResult(
                transcript = "",
                confidence = 0f,
                detectedLanguage = null,
                processingTimeMs = 0,
                error = e.message ?: "Recognition failed"
            )
        }
    }
    
    /**
     * Start streaming recognition with automatic fallback
     */
    suspend fun startStreamingRecognition(
        languageHints: List<String>? = null,
        callback: (StreamingResult) -> Unit
    ): StreamingSession {
        val languages = languageHints ?: currentLanguages
        _recognitionState.value = RecognitionState.STREAMING
        
        return if (shouldUseOnlineRecognition() && googleSpeechRecognizer != null) {
            Log.d(TAG, "Starting online streaming recognition")
            googleSpeechRecognizer.startStreaming(languages) { result ->
                updateLanguageDetection(result)
                callback(result)
            }
        } else {
            Log.d(TAG, "Starting offline streaming recognition")
            offlineSpeechRecognizer.startStreaming(languages) { result ->
                updateLanguageDetection(result)
                callback(result)
            }
        }
    }
    
    /**
     * Stop streaming recognition
     */
    suspend fun stopStreamingRecognition(session: StreamingSession) {
        if (shouldUseOnlineRecognition() && googleSpeechRecognizer != null) {
            googleSpeechRecognizer.stopStreaming(session)
        } else {
            offlineSpeechRecognizer.stopStreaming(session)
        }
        _recognitionState.value = RecognitionState.IDLE
    }
    
    /**
     * Set language preferences for recognition
     */
    fun setLanguagePreferences(languages: List<String>) {
        currentLanguages = languages.ifEmpty { DEFAULT_LANGUAGES }
        
        // Update recognizers
        googleSpeechRecognizer?.setLanguageModel(currentLanguages)
        offlineSpeechRecognizer.setLanguageModel(currentLanguages)
        
        Log.d(TAG, "Language preferences updated: $currentLanguages")
    }
    
    /**
     * Enable or disable code-switching detection
     */
    fun setCodeSwitchingEnabled(enabled: Boolean) {
        codeSwitchingEnabled = enabled
        googleSpeechRecognizer?.setCodeSwitchingEnabled(enabled)
        offlineSpeechRecognizer.setCodeSwitchingEnabled(enabled)
        Log.d(TAG, "Code-switching ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Set preference for online vs offline recognition
     */
    fun setPreferOnlineRecognition(prefer: Boolean) {
        preferOnlineRecognition = prefer
        Log.d(TAG, "Prefer online recognition: $prefer")
    }
    
    /**
     * Get current recognition capabilities
     */
    fun getRecognitionCapabilities(): RecognitionCapabilities {
        return RecognitionCapabilities(
            onlineAvailable = networkAvailable,
            offlineAvailable = offlineSpeechRecognizer.isOfflineCapable(),
            supportedLanguages = getSupportedLanguages(),
            codeSwitchingSupported = codeSwitchingEnabled,
            streamingSupported = true
        )
    }
    
    /**
     * Get all supported languages
     */
    fun getSupportedLanguages(): List<String> {
        val onlineLanguages = googleSpeechRecognizer.getSupportedLanguages()
        val offlineLanguages = offlineSpeechRecognizer.getSupportedLanguages()
        return (onlineLanguages + offlineLanguages).distinct()
    }
    
    /**
     * Check if a specific language is supported
     */
    fun isLanguageSupported(languageCode: String): Boolean {
        return googleSpeechRecognizer.isLanguageSupported(languageCode) ||
                offlineSpeechRecognizer.isLanguageSupported(languageCode)
    }
    
    /**
     * Get Ghana-specific language suggestions based on detected patterns
     */
    fun getGhanaLanguageSuggestions(transcript: String): List<String> {
        val suggestions = mutableListOf<String>()
        
        // Analyze transcript for language patterns
        val lowerTranscript = transcript.lowercase()
        
        // Check for Twi patterns
        val twiPatterns = listOf("sɛn na", "ɛyɛ", "dɛn", "wo ho te sɛn")
        if (twiPatterns.any { lowerTranscript.contains(it) }) {
            suggestions.add("tw")
        }
        
        // Check for Ga patterns
        val gaPatterns = listOf("bawo", "naa", "kɛ")
        if (gaPatterns.any { lowerTranscript.contains(it) }) {
            suggestions.add("ga")
        }
        
        // Check for Ghana English patterns
        val ghanaEnglishPatterns = listOf("how much", "what price", "reduce small", "customer price")
        if (ghanaEnglishPatterns.any { lowerTranscript.contains(it) }) {
            suggestions.add("en-GH")
        }
        
        return suggestions.ifEmpty { listOf("en-GH") }
    }
    
    /**
     * Optimize recognition for market environment
     */
    fun optimizeForMarketEnvironment() {
        // Set Ghana-specific languages as priority
        setLanguagePreferences(GHANA_LANGUAGES)
        
        // Enable code-switching for mixed language conversations
        setCodeSwitchingEnabled(true)
        
        // Prefer online recognition for better accuracy
        setPreferOnlineRecognition(true)
        
        Log.d(TAG, "Optimized for Ghana market environment")
    }
    
    private fun shouldUseOnlineRecognition(): Boolean {
        return preferOnlineRecognition && networkAvailable
    }
    
    private fun monitorNetworkConnectivity() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        try {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            
            networkAvailable = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            Log.d(TAG, "Network available: $networkAvailable")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check network connectivity", e)
            networkAvailable = false
        }
    }
    
    private fun setupLanguageModels() {
        googleSpeechRecognizer.setLanguageModel(currentLanguages)
        offlineSpeechRecognizer.setLanguageModel(currentLanguages)
        
        googleSpeechRecognizer.setCodeSwitchingEnabled(codeSwitchingEnabled)
        offlineSpeechRecognizer.setCodeSwitchingEnabled(codeSwitchingEnabled)
    }
    
    private fun updateLanguageDetection(result: StreamingResult) {
        result.detectedLanguage?.let { detectedLang ->
            _languageDetection.value = LanguageDetectionResult(
                detectedLanguage = detectedLang,
                confidence = result.confidence
            )
        }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        googleSpeechRecognizer.cleanup()
        offlineSpeechRecognizer.cleanup()
        Log.d(TAG, "Speech recognition manager cleaned up")
    }
}

/**
 * Recognition state enumeration
 */
enum class RecognitionState {
    IDLE,
    PROCESSING,
    STREAMING,
    SUCCESS,
    ERROR
}

/**
 * Recognition capabilities data class
 */
data class RecognitionCapabilities(
    val onlineAvailable: Boolean,
    val offlineAvailable: Boolean,
    val supportedLanguages: List<String>,
    val codeSwitchingSupported: Boolean,
    val streamingSupported: Boolean
)