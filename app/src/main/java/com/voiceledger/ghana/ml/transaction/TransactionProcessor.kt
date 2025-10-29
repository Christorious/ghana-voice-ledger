package com.voiceledger.ghana.ml.transaction

import android.util.Log
import com.voiceledger.ghana.data.local.entity.Transaction
import com.voiceledger.ghana.domain.repository.TransactionRepository
import com.voiceledger.ghana.domain.repository.ProductVocabularyRepository
import com.voiceledger.ghana.domain.repository.AudioMetadataRepository
import com.voiceledger.ghana.util.DateUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level transaction processor that coordinates all transaction detection components
 * Integrates state machine, pattern matching, and data persistence
 */
@Singleton
class TransactionProcessor @Inject constructor(
    private val stateMachine: TransactionStateMachine,
    private val transactionRepository: TransactionRepository,
    private val productVocabularyRepository: ProductVocabularyRepository,
    private val audioMetadataRepository: AudioMetadataRepository
) {
    
    companion object {
        private const val TAG = "TransactionProcessor"
        private const val AUTO_SAVE_THRESHOLD = 0.8f
        private const val REVIEW_THRESHOLD = 0.7f
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _processingState = MutableStateFlow(ProcessingState.IDLE)
    val processingState: StateFlow<ProcessingState> = _processingState.asStateFlow()
    
    private val _detectedTransactions = MutableSharedFlow<DetectedTransaction>()
    val detectedTransactions: SharedFlow<DetectedTransaction> = _detectedTransactions.asSharedFlow()
    
    init {
        // Monitor completed transactions from state machine
        scope.launch {
            stateMachine.transactionCompleted.collect { transaction ->
                transaction?.let { handleCompletedTransaction(it) }
            }
        }
    }
    
    /**
     * Process a transcribed utterance for transaction detection
     */
    suspend fun processUtterance(
        transcript: String,
        speakerId: String,
        isSeller: Boolean,
        confidence: Float,
        timestamp: Long = System.currentTimeMillis(),
        audioChunkId: String? = null
    ): TransactionProcessingResult {
        
        if (transcript.isBlank()) {
            return TransactionProcessingResult(
                processed = false,
                stateChanged = false,
                currentState = stateMachine.currentState.value,
                confidence = 0f,
                extractedData = emptyMap(),
                error = "Empty transcript"
            )
        }
        
        _processingState.value = ProcessingState.PROCESSING
        
        return try {
            Log.d(TAG, "Processing utterance: '$transcript' from ${if (isSeller) "seller" else "customer"}")
            
            // Enhance transcript with product vocabulary
            val enhancedTranscript = enhanceTranscriptWithVocabulary(transcript)
            
            // Process through state machine
            val transition = stateMachine.processUtterance(
                text = enhancedTranscript,
                speakerId = speakerId,
                isSeller = isSeller,
                timestamp = timestamp
            )
            
            // Log audio metadata if chunk ID provided
            audioChunkId?.let { chunkId ->
                logAudioMetadata(chunkId, transcript, transition, timestamp)
            }
            
            val result = TransactionProcessingResult(
                processed = true,
                stateChanged = transition.isStateChange,
                currentState = transition.toState,
                confidence = transition.confidence,
                extractedData = transition.extractedData,
                transactionContext = stateMachine.getCurrentContext()
            )
            
            _processingState.value = ProcessingState.SUCCESS
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing utterance", e)
            _processingState.value = ProcessingState.ERROR
            
            TransactionProcessingResult(
                processed = false,
                stateChanged = false,
                currentState = stateMachine.currentState.value,
                confidence = 0f,
                extractedData = emptyMap(),
                error = e.message
            )
        }
    }
    
    /**
     * Get current transaction state
     */
    fun getCurrentState(): TransactionState = stateMachine.currentState.value
    
    /**
     * Get current transaction context
     */
    fun getCurrentContext(): TransactionContext? = stateMachine.getCurrentContext()
    
    /**
     * Force complete current transaction
     */
    suspend fun forceCompleteTransaction(): Transaction? {
        return stateMachine.forceComplete()?.also { transaction ->
            handleCompletedTransaction(transaction)
        }
    }
    
    /**
     * Reset state machine
     */
    fun resetStateMachine() {
        stateMachine.reset()
        _processingState.value = ProcessingState.IDLE
    }
    
    /**
     * Get transaction statistics
     */
    suspend fun getTransactionStats(): TransactionStats {
        val today = DateUtils.getTodayDateString()
        
        return TransactionStats(
            todayTotal = transactionRepository.getTotalSalesForDate(today),
            todayCount = transactionRepository.getTransactionCountForDate(today),
            currentState = getCurrentState(),
            processingState = _processingState.value,
            contextDuration = getCurrentContext()?.getDuration() ?: 0L
        )
    }
    
    private suspend fun enhanceTranscriptWithVocabulary(transcript: String): String {
        // Find best matching products and replace with canonical names
        val words = transcript.split("\\s+".toRegex())
        val enhancedWords = mutableListOf<String>()
        
        for (word in words) {
            val bestMatch = productVocabularyRepository.findBestMatch(word)
            if (bestMatch != null && bestMatch.canonicalName != word) {
                enhancedWords.add(bestMatch.canonicalName)
                Log.d(TAG, "Enhanced '$word' -> '${bestMatch.canonicalName}'")
            } else {
                enhancedWords.add(word)
            }
        }
        
        return enhancedWords.joinToString(" ")
    }
    
    private suspend fun handleCompletedTransaction(transaction: Transaction) {
        Log.d(TAG, "Handling completed transaction: ${transaction.id}")
        
        try {
            // Validate transaction data
            val isValid = validateTransaction(transaction)
            val shouldAutoSave = transaction.confidence >= AUTO_SAVE_THRESHOLD
            val needsReview = transaction.confidence < REVIEW_THRESHOLD || !isValid
            
            val finalTransaction = transaction.copy(needsReview = needsReview)
            
            // Save to database if auto-save threshold met
            if (shouldAutoSave) {
                transactionRepository.insertTransaction(finalTransaction)
                Log.d(TAG, "Auto-saved transaction: ${finalTransaction.id}")
            }
            
            // Update product frequency
            updateProductFrequency(finalTransaction.product)
            
            // Emit detected transaction event
            val detectedTransaction = DetectedTransaction(
                transaction = finalTransaction,
                autoSaved = shouldAutoSave,
                needsReview = needsReview,
                detectedAt = System.currentTimeMillis()
            )
            
            _detectedTransactions.emit(detectedTransaction)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling completed transaction", e)
        }
    }
    
    private suspend fun validateTransaction(transaction: Transaction): Boolean {
        // Basic validation
        if (transaction.amount <= 0 || transaction.product.isBlank()) {
            return false
        }
        
        // Validate against product vocabulary
        val productExists = productVocabularyRepository.isValidProduct(transaction.product)
        if (!productExists) {
            Log.w(TAG, "Unknown product: ${transaction.product}")
        }
        
        // Validate price range
        val priceValid = productVocabularyRepository.validatePrice(transaction.product, transaction.amount)
        if (!priceValid) {
            Log.w(TAG, "Price ${transaction.amount} seems unusual for ${transaction.product}")
        }
        
        return productExists && priceValid
    }
    
    private suspend fun updateProductFrequency(productName: String) {
        try {
            val product = productVocabularyRepository.findBestMatch(productName)
            product?.let {
                productVocabularyRepository.incrementFrequency(it.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating product frequency", e)
        }
    }
    
    private suspend fun logAudioMetadata(
        chunkId: String,
        transcript: String,
        transition: StateTransition,
        timestamp: Long
    ) {
        try {
            // This would be called to mark audio chunks that contributed to transactions
            // Implementation depends on how audio metadata is structured
            Log.d(TAG, "Logging audio metadata for chunk: $chunkId")
        } catch (e: Exception) {
            Log.e(TAG, "Error logging audio metadata", e)
        }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        scope.cancel()
        Log.d(TAG, "Transaction processor cleaned up")
    }
}

/**
 * Processing state enumeration
 */
enum class ProcessingState {
    IDLE,
    PROCESSING,
    SUCCESS,
    ERROR
}

/**
 * Transaction processing result
 */
data class TransactionProcessingResult(
    val processed: Boolean,
    val stateChanged: Boolean,
    val currentState: TransactionState,
    val confidence: Float,
    val extractedData: Map<String, Any>,
    val transactionContext: TransactionContext? = null,
    val error: String? = null
) {
    val isSuccess: Boolean get() = processed && error == null
}

/**
 * Detected transaction event
 */
data class DetectedTransaction(
    val transaction: Transaction,
    val autoSaved: Boolean,
    val needsReview: Boolean,
    val detectedAt: Long
)

/**
 * Transaction statistics
 */
data class TransactionStats(
    val todayTotal: Double,
    val todayCount: Int,
    val currentState: TransactionState,
    val processingState: ProcessingState,
    val contextDuration: Long
)