package com.voiceledger.ghana.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for text-to-speech functionality
 * Supports multiple languages for Ghana market vendors
 */
@Singleton
class TextToSpeechService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()
    
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()
    
    private val _currentLanguage = MutableStateFlow("en")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()
    
    // Supported languages for Ghana
    private val supportedLanguages = mapOf(
        "en" to Locale.ENGLISH,
        "tw" to Locale("tw", "GH"), // Twi
        "ga" to Locale("ga", "GH"), // Ga
        "ee" to Locale("ee", "GH"), // Ewe
        "dag" to Locale("dag", "GH"), // Dagbani
        "ha" to Locale("ha", "GH")  // Hausa
    )
    
    init {
        initializeTextToSpeech()
    }
    
    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                _isReady.value = true
                
                // Set default language to English
                setLanguage("en")
                
                // Set up utterance progress listener
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }
                    
                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                    
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                })
            } else {
                _isReady.value = false
            }
        }
    }
    
    /**
     * Set the language for text-to-speech
     */
    fun setLanguage(languageCode: String): Boolean {
        if (!isInitialized) return false
        
        val locale = supportedLanguages[languageCode] ?: Locale.ENGLISH
        val result = textToSpeech?.setLanguage(locale)
        
        return when (result) {
            TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED -> {
                // Fallback to English if language not supported
                textToSpeech?.setLanguage(Locale.ENGLISH)
                _currentLanguage.value = "en"
                false
            }
            else -> {
                _currentLanguage.value = languageCode
                true
            }
        }
    }
    
    /**
     * Speak the given text
     */
    fun speak(text: String, languageCode: String? = null) {
        if (!isInitialized || text.isBlank()) return
        
        // Set language if specified
        languageCode?.let { setLanguage(it) }
        
        // Configure speech parameters
        textToSpeech?.setSpeechRate(0.9f) // Slightly slower for clarity
        textToSpeech?.setPitch(1.0f)
        
        // Speak the text
        val utteranceId = "summary_${System.currentTimeMillis()}"
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }
    
    /**
     * Stop current speech
     */
    fun stop() {
        textToSpeech?.stop()
        _isSpeaking.value = false
    }
    
    /**
     * Pause current speech (if supported)
     */
    fun pause() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            textToSpeech?.stop()
        }
        _isSpeaking.value = false
    }
    
    /**
     * Check if a language is available
     */
    fun isLanguageAvailable(languageCode: String): Boolean {
        if (!isInitialized) return false
        
        val locale = supportedLanguages[languageCode] ?: return false
        val result = textToSpeech?.isLanguageAvailable(locale)
        
        return result == TextToSpeech.LANG_AVAILABLE || result == TextToSpeech.LANG_COUNTRY_AVAILABLE
    }
    
    /**
     * Get list of available languages
     */
    fun getAvailableLanguages(): List<String> {
        return supportedLanguages.keys.filter { isLanguageAvailable(it) }
    }
    
    /**
     * Set speech rate (0.5 to 2.0)
     */
    fun setSpeechRate(rate: Float) {
        textToSpeech?.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
    }
    
    /**
     * Set speech pitch (0.5 to 2.0)
     */
    fun setPitch(pitch: Float) {
        textToSpeech?.setPitch(pitch.coerceIn(0.5f, 2.0f))
    }
    
    /**
     * Speak text with custom parameters
     */
    fun speakWithParameters(
        text: String,
        languageCode: String? = null,
        speechRate: Float = 0.9f,
        pitch: Float = 1.0f
    ) {
        if (!isInitialized || text.isBlank()) return
        
        // Set language if specified
        languageCode?.let { setLanguage(it) }
        
        // Set speech parameters
        setSpeechRate(speechRate)
        setPitch(pitch)
        
        // Speak the text
        speak(text)
    }
    
    /**
     * Speak summary with appropriate formatting for Ghana context
     */
    fun speakSummary(
        summaryText: String,
        languageCode: String = "en",
        isDetailed: Boolean = false
    ) {
        val formattedText = formatSummaryForSpeech(summaryText, isDetailed)
        
        // Use slightly slower speech for summaries
        val speechRate = if (isDetailed) 0.8f else 0.9f
        
        speakWithParameters(
            text = formattedText,
            languageCode = languageCode,
            speechRate = speechRate,
            pitch = 1.0f
        )
    }
    
    /**
     * Format summary text for better speech output
     */
    private fun formatSummaryForSpeech(text: String, isDetailed: Boolean): String {
        var formattedText = text
        
        // Add pauses for better comprehension
        formattedText = formattedText.replace(". ", ". ... ")
        formattedText = formattedText.replace(": ", ": ... ")
        
        // Format currency for speech
        formattedText = formattedText.replace("GH₵", "Ghana cedis ")
        formattedText = formattedText.replace("GHS", "Ghana cedis ")
        
        // Format percentages
        formattedText = formattedText.replace("%", " percent")
        
        // Add introduction and conclusion for detailed summaries
        if (isDetailed) {
            formattedText = "Here is your detailed business summary. ... $formattedText ... End of summary."
        } else {
            formattedText = "Business summary: ... $formattedText"
        }
        
        return formattedText
    }
    
    /**
     * Cleanup resources
     */
    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isInitialized = false
        _isReady.value = false
        _isSpeaking.value = false
    }
}