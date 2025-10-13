package com.voiceledger.ghana.ml.vad

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * Voice Activity Detection (VAD) processor for Ghana Voice Ledger
 * Filters silence and background noise to process only speech segments
 * Optimized for noisy market environments
 */
@Singleton
class VADProcessor @Inject constructor(
    private val context: Context
) {
    
    companion object {
        // VAD configuration constants
        private const val SAMPLE_RATE = 16000
        private const val FRAME_SIZE_MS = 30 // 30ms frames
        private const val FRAME_SIZE_SAMPLES = (SAMPLE_RATE * FRAME_SIZE_MS) / 1000
        
        // Energy thresholds (tuned for market environments)
        private const val DEFAULT_ENERGY_THRESHOLD = 0.01f
        private const val MIN_ENERGY_THRESHOLD = 0.005f
        private const val MAX_ENERGY_THRESHOLD = 0.1f
        
        // Spectral thresholds
        private const val DEFAULT_SPECTRAL_THRESHOLD = 0.5f
        private const val ZERO_CROSSING_THRESHOLD = 0.3f
        
        // Adaptive parameters
        private const val NOISE_ADAPTATION_RATE = 0.95f
        private const val SPEECH_ADAPTATION_RATE = 0.1f
        
        // Smoothing parameters
        private const val SMOOTHING_WINDOW_SIZE = 5
        private const val MIN_SPEECH_DURATION_MS = 100
        private const val MIN_SILENCE_DURATION_MS = 200
    }
    
    // VAD state
    private var energyThreshold = DEFAULT_ENERGY_THRESHOLD
    private var spectralThreshold = DEFAULT_SPECTRAL_THRESHOLD
    private var noiseFloor = 0.001f
    private var isAdaptive = true
    
    // Smoothing buffers
    private val energyBuffer = mutableListOf<Float>()
    private val spectralBuffer = mutableListOf<Float>()
    private val vadDecisionBuffer = mutableListOf<Boolean>()
    
    // State tracking
    private var consecutiveSpeechFrames = 0
    private var consecutiveSilenceFrames = 0
    private var lastVADDecision = false
    
    /**
     * Process audio sample and return VAD result
     */
    suspend fun processSample(audioData: ByteArray): VADResult = withContext(Dispatchers.Default) {
        if (audioData.size < FRAME_SIZE_SAMPLES * 2) { // 16-bit samples
            return@withContext VADResult(
                isSpeech = false,
                confidence = 0.0f,
                energyLevel = 0.0f,
                spectralCentroid = 0.0f,
                zeroCrossingRate = 0.0f
            )
        }
        
        // Convert byte array to float array
        val samples = convertBytesToFloats(audioData)
        
        // Extract features
        val energy = calculateEnergy(samples)
        val spectralCentroid = calculateSpectralCentroid(samples)
        val zeroCrossingRate = calculateZeroCrossingRate(samples)
        
        // Update noise floor adaptively
        if (isAdaptive) {
            updateNoiseFloor(energy)
        }
        
        // Make VAD decision based on multiple features
        val energyDecision = energy > (noiseFloor * energyThreshold)
        val spectralDecision = spectralCentroid > spectralThreshold
        val zcDecision = zeroCrossingRate < ZERO_CROSSING_THRESHOLD
        
        // Combine decisions with weights
        val rawDecision = energyDecision && (spectralDecision || zcDecision)
        
        // Apply temporal smoothing
        val smoothedDecision = applySmoothingFilter(rawDecision)
        
        // Calculate confidence score
        val confidence = calculateConfidence(energy, spectralCentroid, zeroCrossingRate)
        
        // Update state
        updateVADState(smoothedDecision)
        
        VADResult(
            isSpeech = smoothedDecision,
            confidence = confidence,
            energyLevel = energy,
            spectralCentroid = spectralCentroid,
            zeroCrossingRate = zeroCrossingRate
        )
    }
    
    /**
     * Set noise threshold for VAD
     */
    fun setNoiseThreshold(threshold: Float) {
        energyThreshold = threshold.coerceIn(MIN_ENERGY_THRESHOLD, MAX_ENERGY_THRESHOLD)
    }
    
    /**
     * Enable or disable adaptive thresholding
     */
    fun setAdaptive(adaptive: Boolean) {
        isAdaptive = adaptive
    }
    
    /**
     * Reset VAD state (useful when starting new session)
     */
    fun reset() {
        energyBuffer.clear()
        spectralBuffer.clear()
        vadDecisionBuffer.clear()
        consecutiveSpeechFrames = 0
        consecutiveSilenceFrames = 0
        lastVADDecision = false
        noiseFloor = 0.001f
        energyThreshold = DEFAULT_ENERGY_THRESHOLD
        spectralThreshold = DEFAULT_SPECTRAL_THRESHOLD
    }
    
    /**
     * Get current VAD statistics
     */
    fun getStatistics(): VADStatistics {
        return VADStatistics(
            currentEnergyThreshold = energyThreshold,
            currentSpectralThreshold = spectralThreshold,
            currentNoiseFloor = noiseFloor,
            averageEnergy = energyBuffer.average().toFloat(),
            averageSpectralCentroid = spectralBuffer.average().toFloat(),
            speechPercentage = calculateSpeechPercentage()
        )
    }
    
    /**
     * Convert byte array to float array (16-bit PCM)
     */
    private fun convertBytesToFloats(audioData: ByteArray): FloatArray {
        val samples = FloatArray(audioData.size / 2)
        for (i in samples.indices) {
            val sample = ((audioData[i * 2 + 1].toInt() shl 8) or 
                         (audioData[i * 2].toInt() and 0xFF)).toShort()
            samples[i] = sample / 32768.0f // Normalize to [-1, 1]
        }
        return samples
    }
    
    /**
     * Calculate energy (RMS) of audio frame
     */
    private fun calculateEnergy(samples: FloatArray): Float {
        var sum = 0.0
        for (sample in samples) {
            sum += sample * sample
        }
        val rms = sqrt(sum / samples.size).toFloat()
        
        // Update energy buffer for smoothing
        energyBuffer.add(rms)
        if (energyBuffer.size > SMOOTHING_WINDOW_SIZE) {
            energyBuffer.removeAt(0)
        }
        
        return rms
    }
    
    /**
     * Calculate spectral centroid (brightness measure)
     */
    private fun calculateSpectralCentroid(samples: FloatArray): Float {
        // Simple approximation using high-frequency energy
        var highFreqEnergy = 0.0
        var totalEnergy = 0.0
        
        // Apply simple high-pass filter approximation
        for (i in 1 until samples.size) {
            val highFreqSample = samples[i] - samples[i - 1]
            highFreqEnergy += highFreqSample * highFreqSample
            totalEnergy += samples[i] * samples[i]
        }
        
        val centroid = if (totalEnergy > 0) {
            (highFreqEnergy / totalEnergy).toFloat()
        } else {
            0.0f
        }
        
        // Update spectral buffer
        spectralBuffer.add(centroid)
        if (spectralBuffer.size > SMOOTHING_WINDOW_SIZE) {
            spectralBuffer.removeAt(0)
        }
        
        return centroid
    }
    
    /**
     * Calculate zero crossing rate
     */
    private fun calculateZeroCrossingRate(samples: FloatArray): Float {
        var crossings = 0
        for (i in 1 until samples.size) {
            if ((samples[i] >= 0) != (samples[i - 1] >= 0)) {
                crossings++
            }
        }
        return crossings.toFloat() / (samples.size - 1)
    }
    
    /**
     * Update noise floor adaptively
     */
    private fun updateNoiseFloor(energy: Float) {
        if (energy < noiseFloor * 2) { // Likely noise
            noiseFloor = noiseFloor * NOISE_ADAPTATION_RATE + energy * (1 - NOISE_ADAPTATION_RATE)
        }
        
        // Prevent noise floor from becoming too small or too large
        noiseFloor = noiseFloor.coerceIn(0.0001f, 0.01f)
    }
    
    /**
     * Apply temporal smoothing to reduce false positives/negatives
     */
    private fun applySmoothingFilter(rawDecision: Boolean): Boolean {
        vadDecisionBuffer.add(rawDecision)
        if (vadDecisionBuffer.size > SMOOTHING_WINDOW_SIZE) {
            vadDecisionBuffer.removeAt(0)
        }
        
        // Majority voting
        val speechCount = vadDecisionBuffer.count { it }
        val smoothedDecision = speechCount > vadDecisionBuffer.size / 2
        
        // Apply minimum duration constraints
        return applyDurationConstraints(smoothedDecision)
    }
    
    /**
     * Apply minimum duration constraints for speech and silence
     */
    private fun applyDurationConstraints(decision: Boolean): Boolean {
        val minSpeechFrames = (MIN_SPEECH_DURATION_MS * SAMPLE_RATE) / (FRAME_SIZE_MS * 1000)
        val minSilenceFrames = (MIN_SILENCE_DURATION_MS * SAMPLE_RATE) / (FRAME_SIZE_MS * 1000)
        
        if (decision) { // Speech detected
            consecutiveSpeechFrames++
            consecutiveSilenceFrames = 0
            
            // Only return true if we have enough consecutive speech frames
            return consecutiveSpeechFrames >= minSpeechFrames || lastVADDecision
        } else { // Silence detected
            consecutiveSilenceFrames++
            consecutiveSpeechFrames = 0
            
            // Only return false if we have enough consecutive silence frames
            return consecutiveSilenceFrames < minSilenceFrames && lastVADDecision
        }
    }
    
    /**
     * Calculate confidence score based on multiple features
     */
    private fun calculateConfidence(energy: Float, spectralCentroid: Float, zcRate: Float): Float {
        // Energy confidence
        val energyConf = min(1.0f, energy / (noiseFloor * energyThreshold * 2))
        
        // Spectral confidence
        val spectralConf = min(1.0f, spectralCentroid / spectralThreshold)
        
        // Zero crossing confidence (lower is better for speech)
        val zcConf = 1.0f - min(1.0f, zcRate / ZERO_CROSSING_THRESHOLD)
        
        // Weighted combination
        return (energyConf * 0.5f + spectralConf * 0.3f + zcConf * 0.2f).coerceIn(0.0f, 1.0f)
    }
    
    /**
     * Update VAD state tracking
     */
    private fun updateVADState(decision: Boolean) {
        lastVADDecision = decision
    }
    
    /**
     * Calculate percentage of frames classified as speech
     */
    private fun calculateSpeechPercentage(): Float {
        return if (vadDecisionBuffer.isNotEmpty()) {
            vadDecisionBuffer.count { it }.toFloat() / vadDecisionBuffer.size
        } else {
            0.0f
        }
    }
}

/**
 * Result of VAD processing
 */
data class VADResult(
    val isSpeech: Boolean,
    val confidence: Float,
    val energyLevel: Float,
    val spectralCentroid: Float,
    val zeroCrossingRate: Float
)

/**
 * VAD statistics for monitoring and debugging
 */
data class VADStatistics(
    val currentEnergyThreshold: Float,
    val currentSpectralThreshold: Float,
    val currentNoiseFloor: Float,
    val averageEnergy: Float,
    val averageSpectralCentroid: Float,
    val speechPercentage: Float
)