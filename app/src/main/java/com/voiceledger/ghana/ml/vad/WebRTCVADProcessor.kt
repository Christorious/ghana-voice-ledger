package com.voiceledger.ghana.ml.vad

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebRTC-based Voice Activity Detection processor
 * Uses Google's WebRTC VAD algorithm for more accurate speech detection
 * Optimized for real-time processing in noisy environments
 */
@Singleton
class WebRTCVADProcessor @Inject constructor(
    private val context: Context
) {
    
    companion object {
        // WebRTC VAD modes
        const val VAD_MODE_QUALITY = 0      // High quality, less aggressive
        const val VAD_MODE_LOW_BITRATE = 1  // Optimized for low bitrate
        const val VAD_MODE_AGGRESSIVE = 2   // Most aggressive, good for noisy environments
        const val VAD_MODE_VERY_AGGRESSIVE = 3 // Very aggressive, best for very noisy environments
        
        // Supported sample rates
        const val SAMPLE_RATE_8KHZ = 8000
        const val SAMPLE_RATE_16KHZ = 16000
        const val SAMPLE_RATE_32KHZ = 32000
        const val SAMPLE_RATE_48KHZ = 48000
        
        // Frame sizes (in samples)
        const val FRAME_SIZE_10MS = 160   // 10ms at 16kHz
        const val FRAME_SIZE_20MS = 320   // 20ms at 16kHz
        const val FRAME_SIZE_30MS = 480   // 30ms at 16kHz
        
        // Default configuration for Ghana market environment
        private const val DEFAULT_MODE = VAD_MODE_AGGRESSIVE
        private const val DEFAULT_SAMPLE_RATE = SAMPLE_RATE_16KHZ
        private const val DEFAULT_FRAME_SIZE = FRAME_SIZE_30MS
    }
    
    // VAD configuration
    private var vadMode = DEFAULT_MODE
    private var sampleRate = DEFAULT_SAMPLE_RATE
    private var frameSize = DEFAULT_FRAME_SIZE
    
    // Native WebRTC VAD instance (would be initialized via JNI)
    private var vadInstance: Long = 0
    
    // Statistics tracking
    private var totalFrames = 0
    private var speechFrames = 0
    private var silenceFrames = 0
    
    // Smoothing and post-processing
    private val recentDecisions = mutableListOf<Boolean>()
    private val maxRecentDecisions = 5
    
    /**
     * Initialize WebRTC VAD
     */
    suspend fun initialize(
        mode: Int = DEFAULT_MODE,
        sampleRate: Int = DEFAULT_SAMPLE_RATE
    ): Boolean = withContext(Dispatchers.Default) {
        this@WebRTCVADProcessor.vadMode = mode
        this@WebRTCVADProcessor.sampleRate = sampleRate
        
        // Calculate frame size based on sample rate
        frameSize = when (sampleRate) {
            SAMPLE_RATE_8KHZ -> 240   // 30ms at 8kHz
            SAMPLE_RATE_16KHZ -> 480  // 30ms at 16kHz
            SAMPLE_RATE_32KHZ -> 960  // 30ms at 32kHz
            SAMPLE_RATE_48KHZ -> 1440 // 30ms at 48kHz
            else -> throw IllegalArgumentException("Unsupported sample rate: $sampleRate")
        }
        
        // Initialize native WebRTC VAD (simulated)
        vadInstance = initializeNativeVAD(mode, sampleRate)
        
        vadInstance != 0L
    }
    
    /**
     * Process audio frame and return VAD result
     */
    suspend fun processFrame(audioData: ShortArray): WebRTCVADResult = withContext(Dispatchers.Default) {
        if (vadInstance == 0L) {
            throw IllegalStateException("VAD not initialized. Call initialize() first.")
        }
        
        if (audioData.size != frameSize) {
            throw IllegalArgumentException("Audio frame size must be $frameSize samples")
        }
        
        // Process frame with native WebRTC VAD
        val vadDecision = processNativeVAD(vadInstance, audioData)
        
        // Update statistics
        totalFrames++
        if (vadDecision) {
            speechFrames++
        } else {
            silenceFrames++
        }
        
        // Apply post-processing
        val smoothedDecision = applyPostProcessing(vadDecision)
        
        // Calculate additional metrics
        val confidence = calculateConfidence(audioData, vadDecision)
        val energy = calculateFrameEnergy(audioData)
        
        WebRTCVADResult(
            isSpeech = smoothedDecision,
            rawDecision = vadDecision,
            confidence = confidence,
            energy = energy,
            frameNumber = totalFrames
        )
    }
    
    /**
     * Process audio data in byte format (16-bit PCM)
     */
    suspend fun processSample(audioData: ByteArray): VADResult = withContext(Dispatchers.Default) {
        // Convert bytes to shorts
        val samples = convertBytesToShorts(audioData)
        
        // Process in frames
        val results = mutableListOf<WebRTCVADResult>()
        var offset = 0
        
        while (offset + frameSize <= samples.size) {
            val frame = samples.sliceArray(offset until offset + frameSize)
            val result = processFrame(frame)
            results.add(result)
            offset += frameSize
        }
        
        // Aggregate results
        val overallDecision = results.any { it.isSpeech }
        val averageConfidence = results.map { it.confidence }.average().toFloat()
        val averageEnergy = results.map { it.energy }.average().toFloat()
        
        VADResult(
            isSpeech = overallDecision,
            confidence = averageConfidence,
            energyLevel = averageEnergy,
            spectralCentroid = 0.0f, // Not calculated in WebRTC VAD
            zeroCrossingRate = 0.0f  // Not calculated in WebRTC VAD
        )
    }
    
    /**
     * Set VAD aggressiveness mode
     */
    fun setMode(mode: Int) {
        if (mode !in 0..3) {
            throw IllegalArgumentException("VAD mode must be between 0 and 3")
        }
        vadMode = mode
        if (vadInstance != 0L) {
            setNativeVADMode(vadInstance, mode)
        }
    }
    
    /**
     * Get current VAD statistics
     */
    fun getStatistics(): WebRTCVADStatistics {
        val speechPercentage = if (totalFrames > 0) {
            speechFrames.toFloat() / totalFrames
        } else {
            0.0f
        }
        
        return WebRTCVADStatistics(
            totalFrames = totalFrames,
            speechFrames = speechFrames,
            silenceFrames = silenceFrames,
            speechPercentage = speechPercentage,
            currentMode = vadMode,
            sampleRate = sampleRate,
            frameSize = frameSize
        )
    }
    
    /**
     * Reset VAD state and statistics
     */
    fun reset() {
        totalFrames = 0
        speechFrames = 0
        silenceFrames = 0
        recentDecisions.clear()
        
        if (vadInstance != 0L) {
            resetNativeVAD(vadInstance)
        }
    }
    
    /**
     * Clean up resources
     */
    fun destroy() {
        if (vadInstance != 0L) {
            destroyNativeVAD(vadInstance)
            vadInstance = 0L
        }
    }
    
    /**
     * Convert byte array to short array (16-bit PCM)
     */
    private fun convertBytesToShorts(audioData: ByteArray): ShortArray {
        val samples = ShortArray(audioData.size / 2)
        for (i in samples.indices) {
            samples[i] = ((audioData[i * 2 + 1].toInt() shl 8) or 
                         (audioData[i * 2].toInt() and 0xFF)).toShort()
        }
        return samples
    }
    
    /**
     * Apply post-processing to smooth VAD decisions
     */
    private fun applyPostProcessing(rawDecision: Boolean): Boolean {
        recentDecisions.add(rawDecision)
        if (recentDecisions.size > maxRecentDecisions) {
            recentDecisions.removeAt(0)
        }
        
        // Simple majority voting for smoothing
        val speechCount = recentDecisions.count { it }
        return speechCount > recentDecisions.size / 2
    }
    
    /**
     * Calculate confidence score based on frame characteristics
     */
    private fun calculateConfidence(audioData: ShortArray, vadDecision: Boolean): Float {
        val energy = calculateFrameEnergy(audioData)
        val maxEnergy = 32767.0f * 32767.0f // Max possible energy for 16-bit audio
        
        // Base confidence on energy level and VAD decision consistency
        val energyConfidence = (energy / maxEnergy).coerceIn(0.0f, 1.0f)
        val decisionConfidence = if (vadDecision) 0.8f else 0.6f
        
        return (energyConfidence * 0.7f + decisionConfidence * 0.3f).coerceIn(0.0f, 1.0f)
    }
    
    /**
     * Calculate frame energy (RMS)
     */
    private fun calculateFrameEnergy(audioData: ShortArray): Float {
        var sum = 0.0
        for (sample in audioData) {
            sum += sample * sample
        }
        return kotlin.math.sqrt(sum / audioData.size).toFloat()
    }
    
    // Native method stubs (would be implemented in C++ with WebRTC)
    private fun initializeNativeVAD(mode: Int, sampleRate: Int): Long {
        // Simulated native VAD initialization
        // In real implementation, this would call WebRTC's VAD via JNI
        return System.currentTimeMillis() // Return dummy handle
    }
    
    private fun processNativeVAD(vadInstance: Long, audioData: ShortArray): Boolean {
        // Simulated VAD processing
        // In real implementation, this would call WebRTC's VAD processing
        val energy = calculateFrameEnergy(audioData)
        val threshold = when (vadMode) {
            VAD_MODE_QUALITY -> 1000.0f
            VAD_MODE_LOW_BITRATE -> 800.0f
            VAD_MODE_AGGRESSIVE -> 600.0f
            VAD_MODE_VERY_AGGRESSIVE -> 400.0f
            else -> 600.0f
        }
        return energy > threshold
    }
    
    private fun setNativeVADMode(vadInstance: Long, mode: Int) {
        // Simulated mode setting
        // In real implementation, this would update WebRTC VAD mode
    }
    
    private fun resetNativeVAD(vadInstance: Long) {
        // Simulated VAD reset
        // In real implementation, this would reset WebRTC VAD state
    }
    
    private fun destroyNativeVAD(vadInstance: Long) {
        // Simulated VAD cleanup
        // In real implementation, this would free WebRTC VAD resources
    }
}

/**
 * Result of WebRTC VAD processing
 */
data class WebRTCVADResult(
    val isSpeech: Boolean,
    val rawDecision: Boolean,
    val confidence: Float,
    val energy: Float,
    val frameNumber: Int
)

/**
 * WebRTC VAD statistics
 */
data class WebRTCVADStatistics(
    val totalFrames: Int,
    val speechFrames: Int,
    val silenceFrames: Int,
    val speechPercentage: Float,
    val currentMode: Int,
    val sampleRate: Int,
    val frameSize: Int
)