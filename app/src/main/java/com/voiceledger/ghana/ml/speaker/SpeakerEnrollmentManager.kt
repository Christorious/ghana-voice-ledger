package com.voiceledger.ghana.ml.speaker

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the speaker enrollment process
 * Guides users through voice enrollment with feedback and validation
 */
@Singleton
class SpeakerEnrollmentManager @Inject constructor(
    private val speakerIdentifier: SpeakerIdentifier
) {
    
    companion object {
        private const val TAG = "SpeakerEnrollment"
        private const val MIN_SAMPLES = 3
        private const val MAX_SAMPLES = 5
        private const val MIN_AUDIO_DURATION_MS = 2000 // 2 seconds minimum
        private const val MAX_AUDIO_DURATION_MS = 5000 // 5 seconds maximum
    }
    
    private val _enrollmentState = MutableStateFlow(EnrollmentState.IDLE)
    val enrollmentState: StateFlow<EnrollmentState> = _enrollmentState.asStateFlow()
    
    private val _enrollmentProgress = MutableStateFlow(EnrollmentProgress())
    val enrollmentProgress: StateFlow<EnrollmentProgress> = _enrollmentProgress.asStateFlow()
    
    private val collectedSamples = mutableListOf<ByteArray>()
    private var currentSellerName: String? = null
    
    /**
     * Start the enrollment process
     */
    fun startEnrollment(sellerName: String? = null) {
        Log.d(TAG, "Starting speaker enrollment for: ${sellerName ?: "unnamed seller"}")
        
        currentSellerName = sellerName
        collectedSamples.clear()
        
        _enrollmentState.value = EnrollmentState.RECORDING
        _enrollmentProgress.value = EnrollmentProgress(
            currentSample = 0,
            totalSamples = MIN_SAMPLES,
            message = "Please say your name and a few words about your business",
            canProceed = false
        )
    }
    
    /**
     * Add an audio sample to the enrollment process
     */
    suspend fun addAudioSample(audioData: ByteArray, durationMs: Long): SampleResult {
        if (_enrollmentState.value != EnrollmentState.RECORDING) {
            return SampleResult(
                accepted = false,
                message = "Enrollment not in progress",
                needsMoreSamples = true
            )
        }
        
        // Validate audio duration
        if (durationMs < MIN_AUDIO_DURATION_MS) {
            return SampleResult(
                accepted = false,
                message = "Please speak for at least 2 seconds",
                needsMoreSamples = true
            )
        }
        
        if (durationMs > MAX_AUDIO_DURATION_MS) {
            return SampleResult(
                accepted = false,
                message = "Please keep your recording under 5 seconds",
                needsMoreSamples = true
            )
        }
        
        // Validate audio quality (basic check)
        if (!isAudioQualityAcceptable(audioData)) {
            return SampleResult(
                accepted = false,
                message = "Audio quality too low. Please record in a quieter environment",
                needsMoreSamples = true
            )
        }
        
        // Add sample
        collectedSamples.add(audioData)
        val currentSampleCount = collectedSamples.size
        
        Log.d(TAG, "Added audio sample $currentSampleCount/$MIN_SAMPLES")
        
        // Update progress
        val message = when (currentSampleCount) {
            1 -> "Great! Now say something different about your products"
            2 -> "Perfect! One more sample to complete enrollment"
            3 -> "Excellent! You can add more samples or finish enrollment"
            else -> "Additional sample recorded. You can finish enrollment now"
        }
        
        _enrollmentProgress.value = EnrollmentProgress(
            currentSample = currentSampleCount,
            totalSamples = MIN_SAMPLES,
            message = message,
            canProceed = currentSampleCount >= MIN_SAMPLES
        )
        
        val needsMore = currentSampleCount < MIN_SAMPLES
        val canAddMore = currentSampleCount < MAX_SAMPLES
        
        return SampleResult(
            accepted = true,
            message = message,
            needsMoreSamples = needsMore,
            canAddMoreSamples = canAddMore
        )
    }
    
    /**
     * Complete the enrollment process
     */
    suspend fun completeEnrollment(): EnrollmentResult {
        if (_enrollmentState.value != EnrollmentState.RECORDING) {
            return EnrollmentResult(
                success = false,
                sellerId = null,
                confidence = 0f,
                message = "No enrollment in progress",
                samplesProcessed = 0
            )
        }
        
        if (collectedSamples.size < MIN_SAMPLES) {
            return EnrollmentResult(
                success = false,
                sellerId = null,
                confidence = 0f,
                message = "Need at least $MIN_SAMPLES audio samples",
                samplesProcessed = collectedSamples.size
            )
        }
        
        _enrollmentState.value = EnrollmentState.PROCESSING
        _enrollmentProgress.value = _enrollmentProgress.value.copy(
            message = "Processing your voice samples...",
            canProceed = false
        )
        
        try {
            // Perform enrollment
            val result = speakerIdentifier.enrollSeller(collectedSamples, currentSellerName)
            
            if (result.success) {
                _enrollmentState.value = EnrollmentState.COMPLETED
                _enrollmentProgress.value = _enrollmentProgress.value.copy(
                    message = "Enrollment completed successfully!",
                    canProceed = true
                )
                Log.d(TAG, "Speaker enrollment completed successfully")
            } else {
                _enrollmentState.value = EnrollmentState.FAILED
                _enrollmentProgress.value = _enrollmentProgress.value.copy(
                    message = result.message,
                    canProceed = false
                )
                Log.w(TAG, "Speaker enrollment failed: ${result.message}")
            }
            
            return result
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during enrollment completion", e)
            
            _enrollmentState.value = EnrollmentState.FAILED
            _enrollmentProgress.value = _enrollmentProgress.value.copy(
                message = "Enrollment failed: ${e.message}",
                canProceed = false
            )
            
            return EnrollmentResult(
                success = false,
                sellerId = null,
                confidence = 0f,
                message = "Enrollment failed: ${e.message}",
                samplesProcessed = collectedSamples.size
            )
        }
    }
    
    /**
     * Cancel the enrollment process
     */
    fun cancelEnrollment() {
        Log.d(TAG, "Enrollment cancelled")
        
        collectedSamples.clear()
        currentSellerName = null
        
        _enrollmentState.value = EnrollmentState.CANCELLED
        _enrollmentProgress.value = EnrollmentProgress(
            currentSample = 0,
            totalSamples = MIN_SAMPLES,
            message = "Enrollment cancelled",
            canProceed = false
        )
    }
    
    /**
     * Reset enrollment state
     */
    fun reset() {
        collectedSamples.clear()
        currentSellerName = null
        _enrollmentState.value = EnrollmentState.IDLE
        _enrollmentProgress.value = EnrollmentProgress()
    }
    
    /**
     * Get enrollment instructions for the current step
     */
    fun getInstructions(): List<String> {
        return when (_enrollmentProgress.value.currentSample) {
            0 -> listOf(
                "We need to learn your voice for accurate transaction tracking",
                "Please record 3-5 short samples of your voice",
                "Speak clearly and naturally",
                "Try to record in your usual selling environment"
            )
            1 -> listOf(
                "Great first sample!",
                "Now say something different",
                "You could mention your products or prices",
                "Keep speaking naturally"
            )
            2 -> listOf(
                "Excellent! One more sample needed",
                "Try a different phrase or sentence",
                "This helps us recognize your voice better",
                "Almost done!"
            )
            else -> listOf(
                "Perfect! You have enough samples",
                "You can add more samples for better accuracy",
                "Or tap 'Complete Enrollment' to finish",
                "More samples = better voice recognition"
            )
        }
    }
    
    /**
     * Check if audio quality is acceptable for enrollment
     */
    private fun isAudioQualityAcceptable(audioData: ByteArray): Boolean {
        // Convert bytes to shorts for analysis
        val samples = ShortArray(audioData.size / 2)
        for (i in samples.indices) {
            val byte1 = audioData[i * 2].toInt() and 0xFF
            val byte2 = audioData[i * 2 + 1].toInt() and 0xFF
            samples[i] = (byte2 shl 8 or byte1).toShort()
        }
        
        // Calculate RMS (Root Mean Square) for volume check
        var sum = 0.0
        for (sample in samples) {
            sum += sample * sample
        }
        val rms = kotlin.math.sqrt(sum / samples.size)
        
        // Check if audio is too quiet (likely silence or very low volume)
        val minRMS = 500.0 // Minimum acceptable RMS value
        if (rms < minRMS) {
            Log.d(TAG, "Audio too quiet: RMS = $rms")
            return false
        }
        
        // Check for clipping (audio too loud)
        val maxAmplitude = samples.maxOrNull()?.toInt() ?: 0
        val clippingThreshold = 30000 // Near max value for 16-bit audio
        if (maxAmplitude > clippingThreshold) {
            Log.d(TAG, "Audio clipping detected: max amplitude = $maxAmplitude")
            return false
        }
        
        return true
    }
}

/**
 * States of the enrollment process
 */
enum class EnrollmentState {
    IDLE,
    RECORDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Progress information for enrollment
 */
data class EnrollmentProgress(
    val currentSample: Int = 0,
    val totalSamples: Int = 3,
    val message: String = "Ready to start enrollment",
    val canProceed: Boolean = false
)

/**
 * Result of adding an audio sample
 */
data class SampleResult(
    val accepted: Boolean,
    val message: String,
    val needsMoreSamples: Boolean,
    val canAddMoreSamples: Boolean = true
)