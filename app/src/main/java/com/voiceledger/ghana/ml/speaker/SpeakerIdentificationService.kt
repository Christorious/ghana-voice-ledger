package com.voiceledger.ghana.ml.speaker

import android.util.Log
import com.voiceledger.ghana.data.local.entity.AudioMetadata
import com.voiceledger.ghana.domain.repository.AudioMetadataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service that manages speaker identification in the audio processing pipeline
 * Integrates with the voice agent service to provide real-time speaker identification
 */
@Singleton
class SpeakerIdentificationService @Inject constructor(
    private val speakerIdentifier: SpeakerIdentifier,
    private val audioMetadataRepository: AudioMetadataRepository
) {
    
    companion object {
        private const val TAG = "SpeakerIdentificationService"
    }
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val _speakerEvents = MutableSharedFlow<SpeakerEvent>()
    val speakerEvents: SharedFlow<SpeakerEvent> = _speakerEvents.asSharedFlow()
    
    private var isRunning = false
    
    /**
     * Initialize the speaker identification service
     */
    suspend fun initialize(): Result<Unit> {
        Log.d(TAG, "Initializing speaker identification service")
        
        return try {
            val result = speakerIdentifier.initialize()
            if (result.isSuccess) {
                isRunning = true
                Log.d(TAG, "Speaker identification service initialized successfully")
            } else {
                Log.e(TAG, "Failed to initialize speaker identifier")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing speaker identification service", e)
            Result.failure(e)
        }
    }
    
    /**
     * Process audio chunk for speaker identification
     */
    suspend fun processAudioChunk(
        chunkId: String,
        audioData: ByteArray,
        timestamp: Long,
        batteryLevel: Int? = null
    ): SpeakerResult? {
        if (!isRunning) {
            Log.w(TAG, "Speaker identification service not running")
            return null
        }
        
        val startTime = System.currentTimeMillis()
        
        return try {
            // Perform speaker identification
            val result = speakerIdentifier.identifySpeaker(audioData)
            
            val processingTime = System.currentTimeMillis() - startTime
            
            // Log audio metadata for analytics
            val metadata = AudioMetadata(
                chunkId = chunkId,
                timestamp = timestamp,
                vadScore = 1.0f, // Assume speech was detected if we're processing
                speechDetected = true,
                speakerDetected = result.speakerId != null,
                speakerId = result.speakerId,
                speakerConfidence = result.confidence,
                audioQuality = calculateAudioQuality(audioData),
                durationMs = audioData.size / 32L, // Approximate duration for 16kHz, 16-bit
                processingTimeMs = processingTime,
                batteryLevel = batteryLevel,
                powerSavingMode = batteryLevel != null && batteryLevel < 20
            )
            
            serviceScope.launch {
                audioMetadataRepository.insertMetadata(metadata)
            }
            
            // Emit speaker event
            val event = when (result.speakerType) {
                SpeakerType.SELLER -> SpeakerEvent.SellerDetected(result.speakerId!!, result.confidence)
                SpeakerType.KNOWN_CUSTOMER -> SpeakerEvent.KnownCustomerDetected(
                    result.speakerId!!, 
                    result.confidence, 
                    result.customerVisitCount
                )
                SpeakerType.NEW_CUSTOMER -> SpeakerEvent.NewCustomerDetected(result.speakerId!!, result.confidence)
                SpeakerType.UNKNOWN -> SpeakerEvent.UnknownSpeaker(result.confidence)
            }
            
            serviceScope.launch {
                _speakerEvents.emit(event)
            }
            
            Log.d(TAG, "Speaker identified: ${result.speakerType} (confidence: ${result.confidence})")
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing audio chunk for speaker identification", e)
            
            // Log error metadata
            val errorMetadata = AudioMetadata(
                chunkId = chunkId,
                timestamp = timestamp,
                vadScore = 0f,
                speechDetected = false,
                speakerDetected = false,
                speakerId = null,
                speakerConfidence = null,
                audioQuality = null,
                durationMs = audioData.size / 32L,
                processingTimeMs = System.currentTimeMillis() - startTime,
                errorMessage = e.message,
                batteryLevel = batteryLevel,
                powerSavingMode = batteryLevel != null && batteryLevel < 20
            )
            
            serviceScope.launch {
                audioMetadataRepository.insertMetadata(errorMetadata)
            }
            
            null
        }
    }
    
    /**
     * Handle speaker enrollment process
     */
    suspend fun enrollSeller(audioSamples: List<ByteArray>, sellerName: String? = null): EnrollmentResult {
        Log.d(TAG, "Starting seller enrollment with ${audioSamples.size} samples")
        
        return try {
            val result = speakerIdentifier.enrollSeller(audioSamples, sellerName)
            
            if (result.success) {
                serviceScope.launch {
                    _speakerEvents.emit(SpeakerEvent.SellerEnrolled(result.sellerId!!, result.confidence))
                }
                Log.d(TAG, "Seller enrollment completed successfully")
            } else {
                Log.w(TAG, "Seller enrollment failed: ${result.message}")
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error during seller enrollment", e)
            EnrollmentResult(
                success = false,
                sellerId = null,
                confidence = 0f,
                message = "Enrollment failed: ${e.message}",
                samplesProcessed = 0
            )
        }
    }
    
    /**
     * Update seller voice profile with new audio sample
     */
    suspend fun updateSellerProfile(audioData: ByteArray): Result<Unit> {
        return try {
            val result = speakerIdentifier.updateSellerProfile(audioData)
            if (result.isSuccess) {
                serviceScope.launch {
                    _speakerEvents.emit(SpeakerEvent.SellerProfileUpdated)
                }
                Log.d(TAG, "Seller profile updated successfully")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error updating seller profile", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get current speaker identification status
     */
    fun getStatus() = speakerIdentifier.getStatus()
    
    /**
     * Stop the speaker identification service
     */
    fun stop() {
        Log.d(TAG, "Stopping speaker identification service")
        isRunning = false
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up speaker identification service")
        stop()
        speakerIdentifier.cleanup()
    }
    
    /**
     * Calculate basic audio quality metric
     */
    private fun calculateAudioQuality(audioData: ByteArray): Float {
        if (audioData.size < 2) return 0f
        
        // Convert bytes to shorts
        val samples = ShortArray(audioData.size / 2)
        for (i in samples.indices) {
            val byte1 = audioData[i * 2].toInt() and 0xFF
            val byte2 = audioData[i * 2 + 1].toInt() and 0xFF
            samples[i] = (byte2 shl 8 or byte1).toShort()
        }
        
        // Calculate RMS as a quality indicator
        var sum = 0.0
        for (sample in samples) {
            sum += sample * sample
        }
        val rms = kotlin.math.sqrt(sum / samples.size)
        
        // Normalize to 0-1 range (assuming max RMS around 10000 for good quality)
        return (rms / 10000.0).coerceIn(0.0, 1.0).toFloat()
    }
}

/**
 * Events emitted by the speaker identification service
 */
sealed class SpeakerEvent {
    data class SellerDetected(val sellerId: String, val confidence: Float) : SpeakerEvent()
    data class KnownCustomerDetected(val customerId: String, val confidence: Float, val visitCount: Int) : SpeakerEvent()
    data class NewCustomerDetected(val customerId: String, val confidence: Float) : SpeakerEvent()
    data class UnknownSpeaker(val confidence: Float) : SpeakerEvent()
    data class SellerEnrolled(val sellerId: String, val confidence: Float) : SpeakerEvent()
    object SellerProfileUpdated : SpeakerEvent()
}