package com.voiceledger.ghana.ml.transaction

/**
 * Transaction state enumeration for Ghana market conversations
 * Represents the flow of a typical market transaction
 */
sealed class TransactionState {
    /**
     * No active transaction - waiting for customer interaction
     */
    object IDLE : TransactionState()
    
    /**
     * Customer has made a price inquiry
     * Triggers: "how much", "sɛn na ɛyɛ", "what price"
     */
    object INQUIRY : TransactionState()
    
    /**
     * Seller has provided a price quote
     * Triggers: Numbers + currency, "GH₵", "cedis"
     */
    object PRICE_QUOTE : TransactionState()
    
    /**
     * Negotiation is happening between seller and customer
     * Triggers: "reduce small", "too much", "my last price"
     */
    object NEGOTIATION : TransactionState()
    
    /**
     * Agreement has been reached on price
     * Triggers: "okay", "fine", "deal", "I'll take it"
     */
    object AGREEMENT : TransactionState()
    
    /**
     * Payment is being made or confirmed
     * Triggers: "here money", "take it", "thank you"
     */
    object PAYMENT : TransactionState()
    
    /**
     * Transaction is complete and ready to be logged
     */
    object COMPLETE : TransactionState()
    
    /**
     * Transaction was cancelled or abandoned
     */
    object CANCELLED : TransactionState()
    
    /**
     * Get state name for logging and debugging
     */
    fun getStateName(): String {
        return when (this) {
            is IDLE -> "IDLE"
            is INQUIRY -> "INQUIRY"
            is PRICE_QUOTE -> "PRICE_QUOTE"
            is NEGOTIATION -> "NEGOTIATION"
            is AGREEMENT -> "AGREEMENT"
            is PAYMENT -> "PAYMENT"
            is COMPLETE -> "COMPLETE"
            is CANCELLED -> "CANCELLED"
        }
    }
    
    /**
     * Check if this is a terminal state
     */
    fun isTerminal(): Boolean {
        return this is COMPLETE || this is CANCELLED
    }
    
    /**
     * Check if this is an active transaction state
     */
    fun isActive(): Boolean {
        return this !is IDLE && !isTerminal()
    }
}

/**
 * State transition result
 */
data class StateTransition(
    val fromState: TransactionState,
    val toState: TransactionState,
    val trigger: String,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val extractedData: Map<String, Any> = emptyMap()
) {
    val isStateChange: Boolean get() = fromState != toState
    val isValidTransition: Boolean get() = confidence > 0.5f
}

/**
 * Transaction context data
 */
data class TransactionContext(
    val sessionId: String,
    val startTime: Long,
    val lastActivity: Long,
    val currentState: TransactionState,
    val stateHistory: List<StateTransition>,
    val extractedAmount: Double? = null,
    val extractedProduct: String? = null,
    val extractedQuantity: Int? = null,
    val extractedUnit: String? = null,
    val originalPrice: Double? = null,
    val finalPrice: Double? = null,
    val customerId: String? = null,
    val sellerId: String? = null,
    val conversationSnippets: List<String> = emptyList(),
    val confidence: Float = 0f
) {
    /**
     * Check if transaction has timed out (2 minutes of inactivity)
     */
    fun hasTimedOut(currentTime: Long = System.currentTimeMillis()): Boolean {
        return currentTime - lastActivity > 120_000 // 2 minutes
    }
    
    /**
     * Check if transaction is complete and ready to log
     */
    fun isReadyToLog(): Boolean {
        return currentState is TransactionState.COMPLETE &&
                extractedAmount != null &&
                extractedProduct != null &&
                confidence > 0.7f
    }
    
    /**
     * Get transaction duration in milliseconds
     */
    fun getDuration(currentTime: Long = System.currentTimeMillis()): Long {
        return currentTime - startTime
    }
    
    /**
     * Get completion percentage based on states visited
     */
    fun getCompletionPercentage(): Float {
        val visitedStates = stateHistory.map { it.toState }.distinct()
        val totalStates = 6 // INQUIRY -> PRICE_QUOTE -> NEGOTIATION -> AGREEMENT -> PAYMENT -> COMPLETE
        return visitedStates.size.toFloat() / totalStates
    }
}