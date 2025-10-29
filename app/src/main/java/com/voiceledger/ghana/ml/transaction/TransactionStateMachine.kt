package com.voiceledger.ghana.ml.transaction

import android.util.Log
import com.voiceledger.ghana.data.local.entity.Transaction
import com.voiceledger.ghana.security.SecurityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Transaction state machine for Ghana Voice Ledger
 * Tracks conversation flow and detects completed transactions
 */
@Singleton
class TransactionStateMachine @Inject constructor(
    private val patternMatcher: TransactionPatternMatcher,
    private val securityManager: SecurityManager
) {
    
    companion object {
        private const val TAG = "TransactionStateMachine"
        private const val TIMEOUT_DURATION = 120_000L // 2 minutes
        private const val MIN_CONFIDENCE_THRESHOLD = 0.7f
    }
    
    private var currentContext: TransactionContext? = null
    private val _currentState = MutableStateFlow<TransactionState>(TransactionState.IDLE)
    val currentState: StateFlow<TransactionState> = _currentState.asStateFlow()
    
    private val _transactionCompleted = MutableStateFlow<Transaction?>(null)
    val transactionCompleted: StateFlow<Transaction?> = _transactionCompleted.asStateFlow()
    
    /**
     * Process an utterance and potentially transition states
     */
    fun processUtterance(
        text: String,
        speakerId: String,
        isSeller: Boolean,
        timestamp: Long = System.currentTimeMillis()
    ): StateTransition {
        Log.d(TAG, "Processing utterance: '$text' from ${if (isSeller) "seller" else "customer"}")
        
        // Check for timeout first
        checkTimeout(timestamp)
        
        val currentState = _currentState.value
        val context = currentContext
        
        // Determine next state based on current state and utterance
        val nextState = determineNextState(currentState, text, isSeller, context)
        val confidence = calculateTransitionConfidence(currentState, nextState, text)
        
        val transition = StateTransition(
            fromState = currentState,
            toState = nextState,
            trigger = text,
            confidence = confidence,
            timestamp = timestamp,
            extractedData = extractDataFromUtterance(text)
        )
        
        // Update state if transition is valid
        if (transition.isValidTransition) {
            updateState(nextState, transition, speakerId, isSeller, timestamp)
        }
        
        Log.d(TAG, "State transition: ${currentState.getStateName()} -> ${nextState.getStateName()} (confidence: $confidence)")
        
        return transition
    }
    
    /**
     * Get current transaction context
     */
    fun getCurrentContext(): TransactionContext? = currentContext
    
    /**
     * Reset state machine to IDLE
     */
    fun reset() {
        Log.d(TAG, "Resetting state machine")
        currentContext = null
        _currentState.value = TransactionState.IDLE
        _transactionCompleted.value = null
    }
    
    /**
     * Force complete current transaction
     */
    fun forceComplete(): Transaction? {
        val context = currentContext ?: return null
        
        if (context.extractedAmount != null && context.extractedProduct != null) {
            val transaction = createTransactionFromContext(context)
            completeTransaction(transaction)
            return transaction
        }
        
        return null
    }
    
    /**
     * Check if current transaction has timed out
     */
    fun checkTimeout(currentTime: Long = System.currentTimeMillis()) {
        val context = currentContext
        if (context != null && context.hasTimedOut(currentTime)) {
            Log.d(TAG, "Transaction timed out, resetting to IDLE")
            reset()
        }
    }
    
    private fun determineNextState(
        currentState: TransactionState,
        text: String,
        isSeller: Boolean,
        context: TransactionContext?
    ): TransactionState {
        
        // Check for cancellation first
        val cancellationMatch = patternMatcher.matchCancellation(text)
        if (cancellationMatch.matched && cancellationMatch.confidence > 0.6f) {
            return TransactionState.CANCELLED
        }
        
        return when (currentState) {
            is TransactionState.IDLE -> {
                // Look for price inquiry from customer
                val inquiryMatch = patternMatcher.matchPriceInquiry(text)
                if (inquiryMatch.matched && !isSeller) {
                    TransactionState.INQUIRY
                } else {
                    TransactionState.IDLE
                }
            }
            
            is TransactionState.INQUIRY -> {
                // Look for price quote from seller
                val quoteMatch = patternMatcher.matchPriceQuote(text)
                if (quoteMatch.matched && isSeller) {
                    TransactionState.PRICE_QUOTE
                } else {
                    // Stay in inquiry if customer asks more questions
                    val inquiryMatch = patternMatcher.matchPriceInquiry(text)
                    if (inquiryMatch.matched && !isSeller) {
                        TransactionState.INQUIRY
                    } else {
                        currentState
                    }
                }
            }
            
            is TransactionState.PRICE_QUOTE -> {
                // Look for negotiation or agreement
                val negotiationMatch = patternMatcher.matchNegotiation(text)
                val agreementMatch = patternMatcher.matchAgreement(text)
                
                when {
                    negotiationMatch.matched && !isSeller -> TransactionState.NEGOTIATION
                    agreementMatch.matched && !isSeller -> TransactionState.AGREEMENT
                    else -> {
                        // Seller might give another quote
                        val quoteMatch = patternMatcher.matchPriceQuote(text)
                        if (quoteMatch.matched && isSeller) {
                            TransactionState.PRICE_QUOTE
                        } else {
                            currentState
                        }
                    }
                }
            }
            
            is TransactionState.NEGOTIATION -> {
                // Look for new price quote, agreement, or continued negotiation
                val quoteMatch = patternMatcher.matchPriceQuote(text)
                val agreementMatch = patternMatcher.matchAgreement(text)
                val negotiationMatch = patternMatcher.matchNegotiation(text)
                
                when {
                    quoteMatch.matched && isSeller -> TransactionState.PRICE_QUOTE
                    agreementMatch.matched && !isSeller -> TransactionState.AGREEMENT
                    negotiationMatch.matched -> TransactionState.NEGOTIATION
                    else -> currentState
                }
            }
            
            is TransactionState.AGREEMENT -> {
                // Look for payment confirmation
                val paymentMatch = patternMatcher.matchPayment(text)
                if (paymentMatch.matched) {
                    TransactionState.PAYMENT
                } else {
                    // Might go back to negotiation
                    val negotiationMatch = patternMatcher.matchNegotiation(text)
                    if (negotiationMatch.matched) {
                        TransactionState.NEGOTIATION
                    } else {
                        currentState
                    }
                }
            }
            
            is TransactionState.PAYMENT -> {
                // Transaction should complete after payment
                TransactionState.COMPLETE
            }
            
            is TransactionState.COMPLETE, is TransactionState.CANCELLED -> {
                // Terminal states - start new transaction if inquiry detected
                val inquiryMatch = patternMatcher.matchPriceInquiry(text)
                if (inquiryMatch.matched && !isSeller) {
                    TransactionState.INQUIRY
                } else {
                    TransactionState.IDLE
                }
            }
        }
    }
    
    private fun calculateTransitionConfidence(
        fromState: TransactionState,
        toState: TransactionState,
        text: String
    ): Float {
        if (fromState == toState) return 0.5f // No state change
        
        // Base confidence on pattern matching results
        val baseConfidence = when (toState) {
            is TransactionState.INQUIRY -> patternMatcher.matchPriceInquiry(text).confidence
            is TransactionState.PRICE_QUOTE -> patternMatcher.matchPriceQuote(text).confidence
            is TransactionState.NEGOTIATION -> patternMatcher.matchNegotiation(text).confidence
            is TransactionState.AGREEMENT -> patternMatcher.matchAgreement(text).confidence
            is TransactionState.PAYMENT -> patternMatcher.matchPayment(text).confidence
            is TransactionState.COMPLETE -> 0.9f // High confidence for completion
            is TransactionState.CANCELLED -> patternMatcher.matchCancellation(text).confidence
            else -> 0.5f
        }
        
        // Adjust confidence based on state transition logic
        val transitionBonus = when {
            fromState is TransactionState.IDLE && toState is TransactionState.INQUIRY -> 0.1f
            fromState is TransactionState.INQUIRY && toState is TransactionState.PRICE_QUOTE -> 0.1f
            fromState is TransactionState.PRICE_QUOTE && toState is TransactionState.AGREEMENT -> 0.1f
            fromState is TransactionState.AGREEMENT && toState is TransactionState.PAYMENT -> 0.1f
            fromState is TransactionState.PAYMENT && toState is TransactionState.COMPLETE -> 0.2f
            else -> 0f
        }
        
        return (baseConfidence + transitionBonus).coerceAtMost(1f)
    }
    
    private fun extractDataFromUtterance(text: String): Map<String, Any> {
        val extractedData = mutableMapOf<String, Any>()
        
        // Extract amount
        val priceMatch = patternMatcher.matchPriceQuote(text)
        if (priceMatch.matched) {
            priceMatch.extractedData["amount"]?.let { amount ->
                extractedData["amount"] = amount
            }
        }
        
        // Extract product
        val productMatch = patternMatcher.extractProduct(text)
        if (productMatch.found) {
            extractedData["product"] = productMatch.productName
            extractedData["productVariant"] = productMatch.matchedVariant
        }
        
        // Extract quantity
        val quantityMatch = patternMatcher.extractQuantity(text)
        if (quantityMatch.found) {
            extractedData["quantity"] = quantityMatch.quantity
            extractedData["unit"] = quantityMatch.unit
        }
        
        return extractedData
    }
    
    private fun updateState(
        newState: TransactionState,
        transition: StateTransition,
        speakerId: String,
        isSeller: Boolean,
        timestamp: Long
    ) {
        // Create or update context
        val context = currentContext ?: createNewContext(speakerId, isSeller, timestamp)
        
        // Update context with new state and extracted data
        val updatedContext = context.copy(
            currentState = newState,
            lastActivity = timestamp,
            stateHistory = context.stateHistory + transition,
            extractedAmount = transition.extractedData["amount"] as? Double ?: context.extractedAmount,
            extractedProduct = transition.extractedData["product"] as? String ?: context.extractedProduct,
            extractedQuantity = transition.extractedData["quantity"] as? Int ?: context.extractedQuantity,
            extractedUnit = transition.extractedData["unit"] as? String ?: context.extractedUnit,
            conversationSnippets = context.conversationSnippets + transition.trigger,
            confidence = calculateOverallConfidence(context.stateHistory + transition)
        ).let { ctx ->
            // Update seller/customer IDs
            if (isSeller) {
                ctx.copy(sellerId = speakerId)
            } else {
                ctx.copy(customerId = speakerId)
            }
        }
        
        currentContext = updatedContext
        _currentState.value = newState
        
        // Check if transaction is complete
        if (newState is TransactionState.COMPLETE && updatedContext.isReadyToLog()) {
            val transaction = createTransactionFromContext(updatedContext)
            completeTransaction(transaction)
        } else if (newState is TransactionState.CANCELLED) {
            reset()
        }
    }
    
    private fun createNewContext(speakerId: String, isSeller: Boolean, timestamp: Long): TransactionContext {
        return TransactionContext(
            sessionId = UUID.randomUUID().toString(),
            startTime = timestamp,
            lastActivity = timestamp,
            currentState = TransactionState.IDLE,
            stateHistory = emptyList(),
            sellerId = if (isSeller) speakerId else null,
            customerId = if (!isSeller) speakerId else null
        )
    }
    
    private fun calculateOverallConfidence(stateHistory: List<StateTransition>): Float {
        if (stateHistory.isEmpty()) return 0f
        
        val avgConfidence = stateHistory.map { it.confidence }.average().toFloat()
        val completionBonus = stateHistory.size * 0.1f // Bonus for more complete conversations
        
        return (avgConfidence + completionBonus).coerceAtMost(1f)
    }
    
    private fun createTransactionFromContext(context: TransactionContext): Transaction {
        val transactionId = UUID.randomUUID().toString()
        val currentTime = System.currentTimeMillis()
        
        val rawTranscriptSnippet = context.conversationSnippets.takeLast(3).joinToString(" | ")
        val sanitizedTranscript = securityManager.sanitizeForDisplay(rawTranscriptSnippet, maxLength = 500)
        
        val sanitizedProduct = securityManager.sanitizeForQuery(
            context.extractedProduct ?: "Unknown",
            maxLength = 100
        )
        
        val sanitizedCustomerId = context.customerId?.let { 
            securityManager.sanitizeForQuery(it, maxLength = 50)
        }
        
        return Transaction(
            id = transactionId,
            timestamp = context.startTime,
            date = "", // Will be set by repository
            amount = context.extractedAmount ?: 0.0,
            currency = "GHS",
            product = sanitizedProduct,
            quantity = context.extractedQuantity,
            unit = context.extractedUnit,
            customerId = sanitizedCustomerId,
            confidence = context.confidence,
            transcriptSnippet = sanitizedTranscript,
            sellerConfidence = 0.85f, // Would come from speaker identification
            customerConfidence = 0.75f, // Would come from speaker identification
            needsReview = context.confidence < MIN_CONFIDENCE_THRESHOLD,
            synced = false,
            originalPrice = context.originalPrice,
            finalPrice = context.extractedAmount ?: 0.0
        )
    }
    
    private fun completeTransaction(transaction: Transaction) {
        Log.d(TAG, "Transaction completed: ${transaction.product} for GH₵${transaction.amount}")
        _transactionCompleted.value = transaction
        reset()
    }
}