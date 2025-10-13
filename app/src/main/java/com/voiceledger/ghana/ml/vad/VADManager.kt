package com.voiceledger.ghana.ml.vad

import android.content.Context
import android.os.PowerManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * VAD Manager for Ghana Voice Ledger
 * Manages voice activity detection with smart sleep functionality
 * Optimized for battery efficiency in market environments
 */
@Singleton
class VADManager @Inject constructor(
    private val context: Context,
    private val vadProcessor: VADProcessor,
    private val webRTCVADProcessor: WebRTCVADProcessor
) {
    
    companion object {
        // Sleep configuration
        private const val SILENCE_THRESHOLD_MS = 30000L // 30 seconds of silence triggers sleep
        private const val DEEP_SLEEP_THRESHOLD_MS = 300000L // 5 minutes triggers deep sleep
        private const val WAKE_UP_ENERGY_MULTIPLIER = 1.5f
        
        // Processing intervals
        private const val NORMAL_PROCESSING_INTERVAL_MS = 100L
        private const val LIGHT_SLEEP_INTERVAL_MS = 500L
        private const val DEEP_SLEEP_INTERVAL_MS = 2000L
        
        // Confidence thresholds
        private const val HIGH_CONFIDENCE_THRESHOLD = 0.8f
        private const val LOW_CONFIDENCE_THRESHOLD = 0.3f
    }
    
    // VAD configuration
    private var currentVADType = VADType.CUSTOM
    private var isAdaptiveMode = true
    private var batteryOptimizationEnabled = true
    
    // State management
    private var isInitialized = false
    private var isProcessing = false
    private var currentSleepMode = SleepMode.AWAKE
    
    // Timing tracking
    private var lastSpeechDetectedTime = System.currentTimeMillis()
    private var lastProcessingTime = 0L
    private var silenceDuration = 0L
    
    // Statistics
    private var totalProcessedFrames = 0L
    private var speechFrames = 0L
    private var processingTimeMs = 0L
    
    // Coroutine management
    private val vadScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var processingJob: Job? = null
    
    // Flow for VAD results
    private val _vadResults = MutableSharedFlow<VADResult>(replay = 1)
    val vadResults: SharedFlow<VADResult> = _vadResults.asSharedFlow()
    
    // Flow for sleep mode changes
    private val _sleepModeChanges = MutableSharedFlow<SleepMode>(replay = 1)
    val sleepModeChanges: SharedFlow<SleepMode> = _sleepModeChanges.asSharedFlow()
    
    /**
     * Initialize VAD manager
     */
    suspend fun initialize(vadType: VADType = VADType.CUSTOM): Boolean {
        return withContext(Dispatchers.Default) {
            try {
                currentVADType = vadType
                
                when (vadType) {
                    VADType.CUSTOM -> {
                        vadProcessor.reset()
                        vadProcessor.setAdaptive(isAdaptiveMode)
                    }
                    VADType.WEBRTC -> {
                        webRTCVADProcessor.initialize(
                            mode = WebRTCVADProcessor.VAD_MODE_AGGRESSIVE,
                            sampleRate = WebRTCVADProcessor.SAMPLE_RATE_16KHZ
                        )
                    }
                }
                
                isInitialized = true
                currentSleepMode = SleepMode.AWAKE
                lastSpeechDetectedTime = System.currentTimeMillis()
                
                // Emit initial sleep mode
                _sleepModeChanges.emit(currentSleepMode)
                
                true
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Start VAD processing
     */
    fun startProcessing() {
        if (!isInitialized) {
            throw IllegalStateException("VAD Manager not initialized")
        }
        
        if (isProcessing) {
            return
        }
        
        isProcessing = true
        processingJob = vadScope.launch {
            processVADLoop()
        }
    }
    
    /**
     * Stop VAD processing
     */
    fun stopProcessing() {
        isProcessing = false
        processingJob?.cancel()
        processingJob = null
        currentSleepMode = SleepMode.AWAKE
        vadScope.launch {
            _sleepModeChanges.emit(currentSleepMode)
        }
    }
    
    /**
     * Process audio sample
     */
    suspend fun processAudioSample(audioData: ByteArray): VADResult {
        if (!isInitialized) {
            throw IllegalStateException("VAD Manager not initialized")
        }
        
        val startTime = System.currentTimeMillis()
        
        val result = when (currentVADType) {
            VADType.CUSTOM -> vadProcessor.processSample(audioData)
            VADType.WEBRTC -> webRTCVADProcessor.processSample(audioData)
        }
        
        val processingTime = System.currentTimeMillis() - startTime
        updateStatistics(result, processingTime)
        updateSleepMode(result)
        
        // Emit result
        _vadResults.emit(result)
        
        return result
    }
    
    /**
     * Set VAD configuration
     */
    fun setConfiguration(
        vadType: VADType? = null,
        adaptiveMode: Boolean? = null,
        batteryOptimization: Boolean? = null
    ) {
        vadType?.let { currentVADType = it }
        adaptiveMode?.let { 
            isAdaptiveMode = it
            if (currentVADType == VADType.CUSTOM) {
                vadProcessor.setAdaptive(it)
            }
        }
        batteryOptimization?.let { batteryOptimizationEnabled = it }
    }
    
    /**
     * Get current VAD statistics
     */
    fun getStatistics(): VADManagerStatistics {
        val baseStats = when (currentVADType) {
            VADType.CUSTOM -> vadProcessor.getStatistics()
            VADType.WEBRTC -> webRTCVADProcessor.getStatistics()
        }
        
        val speechPercentage = if (totalProcessedFrames > 0) {
            speechFrames.toFloat() / totalProcessedFrames
        } else {
            0.0f
        }
        
        val averageProcessingTime = if (totalProcessedFrames > 0) {
            processingTimeMs.toFloat() / totalProcessedFrames
        } else {
            0.0f
        }
        
        return VADManagerStatistics(
            vadType = currentVADType,
            sleepMode = currentSleepMode,
            totalFrames = totalProcessedFrames,
            speechFrames = speechFrames,
            speechPercentage = speechPercentage,
            averageProcessingTimeMs = averageProcessingTime,
            silenceDurationMs = silenceDuration,
            lastSpeechTime = lastSpeechDetectedTime,
            batteryOptimizationEnabled = batteryOptimizationEnabled
        )
    }
    
    /**
     * Force wake up from sleep mode
     */
    suspend fun forceWakeUp() {
        if (currentSleepMode != SleepMode.AWAKE) {
            currentSleepMode = SleepMode.AWAKE
            lastSpeechDetectedTime = System.currentTimeMillis()
            silenceDuration = 0L
            _sleepModeChanges.emit(currentSleepMode)
        }
    }
    
    /**
     * Reset VAD state and statistics
     */
    fun reset() {
        when (currentVADType) {
            VADType.CUSTOM -> vadProcessor.reset()
            VADType.WEBRTC -> webRTCVADProcessor.reset()
        }
        
        totalProcessedFrames = 0L
        speechFrames = 0L
        processingTimeMs = 0L
        lastSpeechDetectedTime = System.currentTimeMillis()
        silenceDuration = 0L
        currentSleepMode = SleepMode.AWAKE
    }
    
    /**
     * Clean up resources
     */
    fun destroy() {
        stopProcessing()
        vadScope.cancel()
        
        when (currentVADType) {
            VADType.WEBRTC -> webRTCVADProcessor.destroy()
            else -> { /* Custom VAD doesn't need cleanup */ }
        }
        
        isInitialized = false
    }
    
    /**
     * Main VAD processing loop (for continuous processing)
     */
    private suspend fun processVADLoop() {
        while (isProcessing) {
            val interval = when (currentSleepMode) {
                SleepMode.AWAKE -> NORMAL_PROCESSING_INTERVAL_MS
                SleepMode.LIGHT_SLEEP -> LIGHT_SLEEP_INTERVAL_MS
                SleepMode.DEEP_SLEEP -> DEEP_SLEEP_INTERVAL_MS
            }
            
            delay(interval)
            
            // Update silence duration
            val currentTime = System.currentTimeMillis()
            silenceDuration = currentTime - lastSpeechDetectedTime
            
            // Check if we should change sleep mode
            updateSleepModeBasedOnTime()
        }
    }
    
    /**
     * Update statistics after processing
     */
    private fun updateStatistics(result: VADResult, processingTime: Long) {
        totalProcessedFrames++
        processingTimeMs += processingTime
        
        if (result.isSpeech) {
            speechFrames++
        }
        
        lastProcessingTime = processingTime
    }
    
    /**
     * Update sleep mode based on VAD result
     */
    private suspend fun updateSleepMode(result: VADResult) {
        val currentTime = System.currentTimeMillis()
        
        if (result.isSpeech && result.confidence > LOW_CONFIDENCE_THRESHOLD) {
            // Speech detected, wake up
            if (currentSleepMode != SleepMode.AWAKE) {
                currentSleepMode = SleepMode.AWAKE
                _sleepModeChanges.emit(currentSleepMode)
            }
            lastSpeechDetectedTime = currentTime
            silenceDuration = 0L
        } else {
            // Update silence duration
            silenceDuration = currentTime - lastSpeechDetectedTime
            updateSleepModeBasedOnTime()
        }
    }
    
    /**
     * Update sleep mode based on silence duration
     */
    private suspend fun updateSleepModeBasedOnTime() {
        if (!batteryOptimizationEnabled) {
            return
        }
        
        val newSleepMode = when {
            silenceDuration > DEEP_SLEEP_THRESHOLD_MS -> SleepMode.DEEP_SLEEP
            silenceDuration > SILENCE_THRESHOLD_MS -> SleepMode.LIGHT_SLEEP
            else -> SleepMode.AWAKE
        }
        
        if (newSleepMode != currentSleepMode) {
            currentSleepMode = newSleepMode
            _sleepModeChanges.emit(currentSleepMode)
        }
    }
    
    /**
     * Get recommended processing interval based on current mode
     */
    fun getRecommendedProcessingInterval(): Long {
        return when (currentSleepMode) {
            SleepMode.AWAKE -> NORMAL_PROCESSING_INTERVAL_MS
            SleepMode.LIGHT_SLEEP -> LIGHT_SLEEP_INTERVAL_MS
            SleepMode.DEEP_SLEEP -> DEEP_SLEEP_INTERVAL_MS
        }
    }
    
    /**
     * Check if system should be in power saving mode
     */
    fun shouldUsePowerSavingMode(): Boolean {
        return batteryOptimizationEnabled && currentSleepMode != SleepMode.AWAKE
    }
}

/**
 * VAD implementation types
 */
enum class VADType {
    CUSTOM,   // Custom implementation optimized for Ghana markets
    WEBRTC    // Google WebRTC VAD
}

/**
 * Sleep modes for battery optimization
 */
enum class SleepMode {
    AWAKE,       // Normal processing
    LIGHT_SLEEP, // Reduced processing frequency
    DEEP_SLEEP   // Minimal processing
}

/**
 * VAD Manager statistics
 */
data class VADManagerStatistics(
    val vadType: VADType,
    val sleepMode: SleepMode,
    val totalFrames: Long,
    val speechFrames: Long,
    val speechPercentage: Float,
    val averageProcessingTimeMs: Float,
    val silenceDurationMs: Long,
    val lastSpeechTime: Long,
    val batteryOptimizationEnabled: Boolean
)