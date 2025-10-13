package com.voiceledger.ghana.ml.speaker

import android.content.Context
import android.util.Log
import com.voiceledger.ghana.domain.repository.SpeakerProfileRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import kotlin.random.Random

/**
 * Mock implementation of SpeakerIdentifier for testing and development
 * Simulates speaker identification behavior without requiring actual ML models
 */
class MockSpeakerIdentifier @Inject constructor(
    private val context: Context,
    private val speakerRepository: SpeakerProfileRepository
) : SpeakerIdentifier {
    
    companion object {
        private const val TAG = "MockSpeakerIdentifier"
        private const val EMBEDDING_SIZE = 128
    }
    
    private var isInitialized = false
    private val random = Random.Default
    
    private val _status = MutableStateFlow(
        SpeakerIdentificationStatus(
            isInitialized = false,
            hasSellerProfile = false,
            knownCustomerCount = 0,
            modelLoadTime = 0L,
            lastProcessingTime = 0L,
            averageConfidence = 0f
        )
    )
    
    override suspend fun initialize(): Result<Unit> {
        Log.d(TAG, "Initializing mock speaker identifier")
        
        // Simulate model loading time
        delay(500)
        
        isInitialized = true
        
        _status.value = _status.value.copy(
            isInitialized = true,
            modelLoadTime = 500L
        )
        
        Log.d(TAG, "Mock speaker identifier initialized")
        return Result.success(Unit)
    }
    
    override suspend fun enrollSeller(audioSamples: List<ByteArray>, sellerName: String?): EnrollmentResult {
        if (!isInitialized) {
            return EnrollmentResult(
                success = false,
                sellerId = null,
                confidence = 0f,
                message = "Speaker identification not initialized",
                samplesProcessed = 0
            )
        }
        
        Log.d(TAG, "Mock enrolling seller with ${audioSamples.size} samples")
        
        // Simulate processing time
        delay(1000)
        
        if (audioSamples.size < 3) {
            return EnrollmentResult(
                success = false,
                sellerId = null,
                confidence = 0f,
                message = "Need at least 3 audio samples",
                samplesProcessed = audioSamples.size
            )
        }
        
        // Generate mock embedding
        val mockEmbedding = generateMockEmbedding()
        
        // Create seller profile
        val sellerProfile = speakerRepository.enrollSeller(mockEmbedding, sellerName)
        
        // Simulate varying confidence based on sample count
        val confidence = when {
            audioSamples.size >= 5 -> 0.95f
            audioSamples.size >= 4 -> 0.90f
            else -> 0.85f
        }
        
        _status.value = _status.value.copy(
            hasSellerProfile = true,
            averageConfidence = confidence
        )
        
        return EnrollmentResult(
            success = true,
            sellerId = sellerProfile.id,
            confidence = confidence,
            message = "Seller enrolled successfully",
            samplesProcessed = audioSamples.size
        )
    }
    
    override suspend fun identifySpeaker(audioData: ByteArray): SpeakerResult {
        if (!isInitialized) {
            return SpeakerResult(
                speakerId = null,
                speakerType = SpeakerType.UNKNOWN,
                confidence = 0f,
                embedding = null
            )
        }
        
        // Simulate processing time
        delay(100)
        
        val startTime = System.currentTimeMillis()
        
        // Generate mock embedding
        val mockEmbedding = generateMockEmbedding()
        
        // Check if seller profile exists
        val sellerProfile = speakerRepository.getSellerProfile()
        
        // Simulate speaker identification logic
        val identificationResult = when {
            sellerProfile != null && random.nextFloat() < 0.7f -> {
                // 70% chance to identify as seller if profile exists
                SpeakerResult(
                    speakerId = sellerProfile.id,
                    speakerType = SpeakerType.SELLER,
                    confidence = 0.85f + random.nextFloat() * 0.1f, // 0.85-0.95
                    embedding = mockEmbedding
                )
            }
            
            random.nextFloat() < 0.3f -> {
                // 30% chance for known customer
                val customerId = "mock_customer_${random.nextInt(1000)}"
                SpeakerResult(
                    speakerId = customerId,
                    speakerType = SpeakerType.KNOWN_CUSTOMER,
                    confidence = 0.75f + random.nextFloat() * 0.15f, // 0.75-0.90
                    embedding = mockEmbedding,
                    customerVisitCount = random.nextInt(1, 10)
                )
            }
            
            random.nextFloat() < 0.5f -> {
                // 50% chance for new customer
                val customerId = "new_customer_${System.currentTimeMillis()}"
                SpeakerResult(
                    speakerId = customerId,
                    speakerType = SpeakerType.NEW_CUSTOMER,
                    confidence = 0.6f + random.nextFloat() * 0.2f, // 0.6-0.8
                    embedding = mockEmbedding,
                    isNewCustomer = true,
                    customerVisitCount = 1
                )
            }
            
            else -> {
                // Unknown speaker
                SpeakerResult(
                    speakerId = null,
                    speakerType = SpeakerType.UNKNOWN,
                    confidence = 0f,
                    embedding = mockEmbedding
                )
            }
        }
        
        val processingTime = System.currentTimeMillis() - startTime
        _status.value = _status.value.copy(
            lastProcessingTime = processingTime,
            averageConfidence = identificationResult.confidence
        )
        
        Log.d(TAG, "Mock identified speaker: ${identificationResult.speakerType} with confidence ${identificationResult.confidence}")
        
        return identificationResult
    }
    
    override suspend fun addCustomerProfile(embedding: FloatArray, customerId: String): Result<Unit> {
        return try {
            speakerRepository.addCustomerProfile(embedding, customerId)
            
            val customerCount = speakerRepository.getCustomerCount()
            _status.value = _status.value.copy(knownCustomerCount = customerCount)
            
            Log.d(TAG, "Mock added customer profile: $customerId")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateSellerProfile(audioData: ByteArray): Result<Unit> {
        if (!isInitialized) {
            return Result.failure(Exception("Not initialized"))
        }
        
        // Simulate processing
        delay(200)
        
        val sellerProfile = speakerRepository.getSellerProfile()
        if (sellerProfile == null) {
            return Result.failure(Exception("No seller profile found"))
        }
        
        // Generate new mock embedding and update
        val newEmbedding = generateMockEmbedding()
        speakerRepository.updateVoiceEmbedding(sellerProfile.id, newEmbedding)
        
        Log.d(TAG, "Mock updated seller profile")
        return Result.success(Unit)
    }
    
    override fun getStatus(): Flow<SpeakerIdentificationStatus> = _status.asStateFlow()
    
    override fun cleanup() {
        isInitialized = false
        _status.value = _status.value.copy(isInitialized = false)
        Log.d(TAG, "Mock speaker identifier cleaned up")
    }
    
    /**
     * Generate a mock speaker embedding for testing
     */
    private fun generateMockEmbedding(): FloatArray {
        val embedding = FloatArray(EMBEDDING_SIZE)
        
        // Generate random normalized embedding
        for (i in embedding.indices) {
            embedding[i] = random.nextFloat() * 2f - 1f // Range: -1 to 1
        }
        
        // Normalize to unit length
        var norm = 0f
        for (value in embedding) {
            norm += value * value
        }
        norm = kotlin.math.sqrt(norm)
        
        if (norm > 0f) {
            for (i in embedding.indices) {
                embedding[i] /= norm
            }
        }
        
        return embedding
    }
}