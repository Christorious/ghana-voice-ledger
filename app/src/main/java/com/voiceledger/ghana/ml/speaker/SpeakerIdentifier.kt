package com.voiceledger.ghana.ml.speaker

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Interface for speaker identification functionality
 * Handles voice enrollment, speaker classification, and customer recognition
 */
interface SpeakerIdentifier {
    
    /**
     * Initialize the speaker identification system
     */
    suspend fun initialize(): Result<Unit>
    
    /**
     * Enroll seller voice with multiple samples
     * @param audioSamples List of audio samples for training
     * @param sellerName Optional name for the seller
     * @return Result containing enrollment success/failure
     */
    suspend fun enrollSeller(audioSamples: List<ByteArray>, sellerName: String? = null): EnrollmentResult
    
    /**
     * Identify speaker from audio data
     * @param audioData Raw audio data (PCM 16-bit, 16kHz)
     * @return Speaker identification result with confidence
     */
    suspend fun identifySpeaker(audioData: ByteArray): SpeakerResult
    
    /**
     * Add customer profile for repeat recognition
     * @param embedding Voice embedding
     * @param customerId Unique customer identifier
     * @return Success/failure result
     */
    suspend fun addCustomerProfile(embedding: FloatArray, customerId: String): Result<Unit>
    
    /**
     * Update seller voice profile with new sample
     * @param audioData New audio sample
     * @return Update result
     */
    suspend fun updateSellerProfile(audioData: ByteArray): Result<Unit>
    
    /**
     * Get current speaker identification status
     */
    fun getStatus(): Flow<SpeakerIdentificationStatus>
    
    /**
     * Clean up resources
     */
    fun cleanup()
}

/**
 * Result of speaker enrollment process
 */
data class EnrollmentResult(
    val success: Boolean,
    val sellerId: String?,
    val confidence: Float,
    val message: String,
    val samplesProcessed: Int
)

/**
 * Result of speaker identification
 */
data class SpeakerResult(
    val speakerId: String?,
    val speakerType: SpeakerType,
    val confidence: Float,
    val embedding: FloatArray?,
    val isNewCustomer: Boolean = false,
    val customerVisitCount: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as SpeakerResult
        
        if (speakerId != other.speakerId) return false
        if (speakerType != other.speakerType) return false
        if (confidence != other.confidence) return false
        if (embedding != null) {
            if (other.embedding == null) return false
            if (!embedding.contentEquals(other.embedding)) return false
        } else if (other.embedding != null) return false
        if (isNewCustomer != other.isNewCustomer) return false
        if (customerVisitCount != other.customerVisitCount) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = speakerId?.hashCode() ?: 0
        result = 31 * result + speakerType.hashCode()
        result = 31 * result + confidence.hashCode()
        result = 31 * result + (embedding?.contentHashCode() ?: 0)
        result = 31 * result + isNewCustomer.hashCode()
        result = 31 * result + customerVisitCount
        return result
    }
}

/**
 * Types of speakers the system can identify
 */
enum class SpeakerType {
    SELLER,
    KNOWN_CUSTOMER,
    NEW_CUSTOMER,
    UNKNOWN
}

/**
 * Status of the speaker identification system
 */
data class SpeakerIdentificationStatus(
    val isInitialized: Boolean,
    val hasSellerProfile: Boolean,
    val knownCustomerCount: Int,
    val modelLoadTime: Long,
    val lastProcessingTime: Long,
    val averageConfidence: Float
)