package com.voiceledger.ghana.ml.speaker

import android.content.Context
import android.util.Log
import com.voiceledger.ghana.data.local.entity.SpeakerProfile
import com.voiceledger.ghana.domain.repository.SpeakerProfileRepository
import com.voiceledger.ghana.ml.audio.AudioUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * TensorFlow Lite implementation of speaker identification
 * Uses ResNet-based speaker embedding model optimized for mobile devices
 */
@Singleton
class TensorFlowLiteSpeakerIdentifier @Inject constructor(
    private val context: Context,
    private val speakerRepository: SpeakerProfileRepository,
    private val audioUtils: AudioUtils
) : SpeakerIdentifier {
    
    companion object {
        private const val TAG = "SpeakerIdentifier"
        private const val MODEL_FILE = "speaker_embedding_model.tflite"
        private const val EMBEDDING_SIZE = 128
        private const val AUDIO_SAMPLE_RATE = 16000
        private const val AUDIO_DURATION_MS = 3000 // 3 seconds
        private const val AUDIO_SAMPLES = AUDIO_SAMPLE_RATE * AUDIO_DURATION_MS / 1000
        private const val SELLER_CONFIDENCE_THRESHOLD = 0.85f
        private const val CUSTOMER_CONFIDENCE_THRESHOLD = 0.75f
        private const val MIN_ENROLLMENT_SAMPLES = 3
        private const val MAX_ENROLLMENT_SAMPLES = 10
    }
    
    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private var isInitialized = false
    
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
    
    override suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()
            
            // Load TensorFlow Lite model
            val modelBuffer = loadModelFile()
            
            // Configure interpreter options
            val options = Interpreter.Options().apply {
                setNumThreads(2) // Optimize for budget devices
                
                // Use GPU delegate if available and compatible
                val compatibilityList = CompatibilityList()
                if (compatibilityList.isDelegateSupportedOnThisDevice) {
                    gpuDelegate = GpuDelegate()
                    addDelegate(gpuDelegate)
                    Log.d(TAG, "GPU delegate enabled for speaker identification")
                } else {
                    Log.d(TAG, "GPU delegate not supported, using CPU")
                }
            }
            
            interpreter = Interpreter(modelBuffer, options)
            
            val loadTime = System.currentTimeMillis() - startTime
            isInitialized = true
            
            // Update status
            updateStatus(modelLoadTime = loadTime)
            
            Log.d(TAG, "Speaker identification initialized in ${loadTime}ms")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize speaker identification", e)
            cleanup()
            Result.failure(e)
        }
    }
    
    override suspend fun enrollSeller(audioSamples: List<ByteArray>, sellerName: String?): EnrollmentResult = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            return@withContext EnrollmentResult(
                success = false,
                sellerId = null,
                confidence = 0f,
                message = "Speaker identification not initialized",
                samplesProcessed = 0
            )
        }
        
        if (audioSamples.size < MIN_ENROLLMENT_SAMPLES) {
            return@withContext EnrollmentResult(
                success = false,
                sellerId = null,
                confidence = 0f,
                message = "Need at least $MIN_ENROLLMENT_SAMPLES audio samples for enrollment",
                samplesProcessed = 0
            )
        }
        
        try {
            val embeddings = mutableListOf<FloatArray>()
            var processedSamples = 0
            
            // Process each audio sample
            for (audioData in audioSamples.take(MAX_ENROLLMENT_SAMPLES)) {
                val embedding = extractEmbedding(audioData)
                if (embedding != null) {
                    embeddings.add(embedding)
                    processedSamples++
                }
            }
            
            if (embeddings.isEmpty()) {
                return@withContext EnrollmentResult(
                    success = false,
                    sellerId = null,
                    confidence = 0f,
                    message = "Failed to extract embeddings from audio samples",
                    samplesProcessed = processedSamples
                )
            }
            
            // Average the embeddings to create seller profile
            val averageEmbedding = averageEmbeddings(embeddings)
            
            // Calculate enrollment confidence based on consistency
            val confidence = calculateEnrollmentConfidence(embeddings)
            
            if (confidence < 0.7f) {
                return@withContext EnrollmentResult(
                    success = false,
                    sellerId = null,
                    confidence = confidence,
                    message = "Enrollment confidence too low. Please record in a quieter environment.",
                    samplesProcessed = processedSamples
                )
            }
            
            // Save seller profile
            val sellerProfile = speakerRepository.enrollSeller(averageEmbedding, sellerName)
            
            updateStatus(hasSellerProfile = true)
            
            EnrollmentResult(
                success = true,
                sellerId = sellerProfile.id,
                confidence = confidence,
                message = "Seller enrolled successfully",
                samplesProcessed = processedSamples
            )
            
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
    
    override suspend fun identifySpeaker(audioData: ByteArray): SpeakerResult = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            return@withContext SpeakerResult(
                speakerId = null,
                speakerType = SpeakerType.UNKNOWN,
                confidence = 0f,
                embedding = null
            )
        }
        
        val startTime = System.currentTimeMillis()
        
        try {
            // Extract embedding from audio
            val embedding = extractEmbedding(audioData)
            if (embedding == null) {
                return@withContext SpeakerResult(
                    speakerId = null,
                    speakerType = SpeakerType.UNKNOWN,
                    confidence = 0f,
                    embedding = null
                )
            }
            
            // Check against seller profile first
            val sellerProfile = speakerRepository.getSellerProfile()
            if (sellerProfile != null) {
                val sellerSimilarity = calculateCosineSimilarity(embedding, sellerProfile.getEmbeddingArray())
                if (sellerSimilarity >= SELLER_CONFIDENCE_THRESHOLD) {
                    val processingTime = System.currentTimeMillis() - startTime
                    updateStatus(lastProcessingTime = processingTime, averageConfidence = sellerSimilarity)
                    
                    return@withContext SpeakerResult(
                        speakerId = sellerProfile.id,
                        speakerType = SpeakerType.SELLER,
                        confidence = sellerSimilarity,
                        embedding = embedding
                    )
                }
            }
            
            // Check against known customers
            val customerProfiles = speakerRepository.getCustomerProfiles()
            customerProfiles.collect { customers ->
                for (customer in customers) {
                    val similarity = calculateCosineSimilarity(embedding, customer.getEmbeddingArray())
                    if (similarity >= customer.confidenceThreshold) {
                        // Update customer visit
                        speakerRepository.incrementVisitCount(customer.id, System.currentTimeMillis())
                        
                        val processingTime = System.currentTimeMillis() - startTime
                        updateStatus(lastProcessingTime = processingTime, averageConfidence = similarity)
                        
                        return@withContext SpeakerResult(
                            speakerId = customer.id,
                            speakerType = SpeakerType.KNOWN_CUSTOMER,
                            confidence = similarity,
                            embedding = embedding,
                            customerVisitCount = customer.visitCount + 1
                        )
                    }
                }
            }
            
            // New customer - create profile if confidence is reasonable
            if (embedding.isNotEmpty()) {
                val customerId = "customer_${System.currentTimeMillis()}"
                speakerRepository.addCustomerProfile(embedding, customerId)
                
                val processingTime = System.currentTimeMillis() - startTime
                updateStatus(lastProcessingTime = processingTime, averageConfidence = 0.5f)
                
                return@withContext SpeakerResult(
                    speakerId = customerId,
                    speakerType = SpeakerType.NEW_CUSTOMER,
                    confidence = 0.5f, // Default confidence for new customers
                    embedding = embedding,
                    isNewCustomer = true,
                    customerVisitCount = 1
                )
            }
            
            // Unknown speaker
            SpeakerResult(
                speakerId = null,
                speakerType = SpeakerType.UNKNOWN,
                confidence = 0f,
                embedding = embedding
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during speaker identification", e)
            SpeakerResult(
                speakerId = null,
                speakerType = SpeakerType.UNKNOWN,
                confidence = 0f,
                embedding = null
            )
        }
    }
    
    override suspend fun addCustomerProfile(embedding: FloatArray, customerId: String): Result<Unit> {
        return try {
            speakerRepository.addCustomerProfile(embedding, customerId)
            updateStatus(knownCustomerCount = speakerRepository.getCustomerCount())
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding customer profile", e)
            Result.failure(e)
        }
    }
    
    override suspend fun updateSellerProfile(audioData: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val embedding = extractEmbedding(audioData)
            if (embedding == null) {
                return@withContext Result.failure(Exception("Failed to extract embedding from audio"))
            }
            
            val sellerProfile = speakerRepository.getSellerProfile()
            if (sellerProfile == null) {
                return@withContext Result.failure(Exception("No seller profile found"))
            }
            
            // Blend new embedding with existing profile (weighted average)
            val existingEmbedding = sellerProfile.getEmbeddingArray()
            val blendedEmbedding = blendEmbeddings(existingEmbedding, embedding, 0.8f)
            
            speakerRepository.updateVoiceEmbedding(sellerProfile.id, blendedEmbedding)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating seller profile", e)
            Result.failure(e)
        }
    }
    
    override fun getStatus(): Flow<SpeakerIdentificationStatus> = _status.asStateFlow()
    
    override fun cleanup() {
        interpreter?.close()
        interpreter = null
        gpuDelegate?.close()
        gpuDelegate = null
        isInitialized = false
        Log.d(TAG, "Speaker identification resources cleaned up")
    }
    
    /**
     * Load TensorFlow Lite model from assets
     */
    private fun loadModelFile(): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(MODEL_FILE)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
    
    /**
     * Extract speaker embedding from audio data
     */
    private suspend fun extractEmbedding(audioData: ByteArray): FloatArray? = withContext(Dispatchers.Default) {
        try {
            val interpreter = this@TensorFlowLiteSpeakerIdentifier.interpreter ?: return@withContext null
            
            // Preprocess audio data
            val processedAudio = audioUtils.preprocessAudioForML(audioData, AUDIO_SAMPLES)
            
            // Prepare input buffer
            val inputBuffer = ByteBuffer.allocateDirect(processedAudio.size * 4).apply {
                order(ByteOrder.nativeOrder())
                processedAudio.forEach { putFloat(it) }
                rewind()
            }
            
            // Prepare output buffer
            val outputBuffer = ByteBuffer.allocateDirect(EMBEDDING_SIZE * 4).apply {
                order(ByteOrder.nativeOrder())
            }
            
            // Run inference
            interpreter.run(inputBuffer, outputBuffer)
            
            // Extract embedding
            outputBuffer.rewind()
            val embedding = FloatArray(EMBEDDING_SIZE)
            outputBuffer.asFloatBuffer().get(embedding)
            
            // Normalize embedding
            normalizeEmbedding(embedding)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting embedding", e)
            null
        }
    }
    
    /**
     * Calculate cosine similarity between two embeddings
     */
    private fun calculateCosineSimilarity(embedding1: FloatArray, embedding2: FloatArray): Float {
        if (embedding1.size != embedding2.size) return 0f
        
        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f
        
        for (i in embedding1.indices) {
            dotProduct += embedding1[i] * embedding2[i]
            norm1 += embedding1[i] * embedding1[i]
            norm2 += embedding2[i] * embedding2[i]
        }
        
        val denominator = sqrt(norm1) * sqrt(norm2)
        return if (denominator != 0f) dotProduct / denominator else 0f
    }
    
    /**
     * Average multiple embeddings
     */
    private fun averageEmbeddings(embeddings: List<FloatArray>): FloatArray {
        val result = FloatArray(EMBEDDING_SIZE)
        
        for (embedding in embeddings) {
            for (i in embedding.indices) {
                result[i] += embedding[i]
            }
        }
        
        for (i in result.indices) {
            result[i] /= embeddings.size
        }
        
        return normalizeEmbedding(result)
    }
    
    /**
     * Calculate enrollment confidence based on embedding consistency
     */
    private fun calculateEnrollmentConfidence(embeddings: List<FloatArray>): Float {
        if (embeddings.size < 2) return 0.5f
        
        val average = averageEmbeddings(embeddings)
        var totalSimilarity = 0f
        
        for (embedding in embeddings) {
            totalSimilarity += calculateCosineSimilarity(embedding, average)
        }
        
        return totalSimilarity / embeddings.size
    }
    
    /**
     * Blend two embeddings with a weight factor
     */
    private fun blendEmbeddings(existing: FloatArray, new: FloatArray, existingWeight: Float): FloatArray {
        val result = FloatArray(existing.size)
        val newWeight = 1f - existingWeight
        
        for (i in existing.indices) {
            result[i] = existing[i] * existingWeight + new[i] * newWeight
        }
        
        return normalizeEmbedding(result)
    }
    
    /**
     * Normalize embedding to unit length
     */
    private fun normalizeEmbedding(embedding: FloatArray): FloatArray {
        var norm = 0f
        for (value in embedding) {
            norm += value * value
        }
        norm = sqrt(norm)
        
        if (norm > 0f) {
            for (i in embedding.indices) {
                embedding[i] /= norm
            }
        }
        
        return embedding
    }
    
    /**
     * Update system status
     */
    private suspend fun updateStatus(
        modelLoadTime: Long? = null,
        lastProcessingTime: Long? = null,
        averageConfidence: Float? = null,
        hasSellerProfile: Boolean? = null,
        knownCustomerCount: Int? = null
    ) {
        val currentStatus = _status.value
        val sellerExists = hasSellerProfile ?: (speakerRepository.getSellerProfile() != null)
        val customerCount = knownCustomerCount ?: speakerRepository.getCustomerCount()
        
        _status.value = currentStatus.copy(
            isInitialized = isInitialized,
            hasSellerProfile = sellerExists,
            knownCustomerCount = customerCount,
            modelLoadTime = modelLoadTime ?: currentStatus.modelLoadTime,
            lastProcessingTime = lastProcessingTime ?: currentStatus.lastProcessingTime,
            averageConfidence = averageConfidence ?: currentStatus.averageConfidence
        )
    }
}