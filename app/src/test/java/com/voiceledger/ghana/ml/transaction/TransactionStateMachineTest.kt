package com.voiceledger.ghana.ml.transaction

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

/**
 * Unit tests for TransactionStateMachine
 * Tests all state transitions and edge cases
 */
class TransactionStateMachineTest {
    
    @Mock
    private lateinit var mockPatternMatcher: TransactionPatternMatcher
    
    private lateinit var stateMachine: TransactionStateMachine
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        stateMachine = TransactionStateMachine(mockPatternMatcher)
    }
    
    @Test
    fun testInitialState_shouldBeIdle() {
        // When
        val currentState = stateMachine.getCurrentState()
        
        // Then
        assertEquals("Initial state should be IDLE", TransactionState.IDLE, currentState)
    }
    
    @Test
    fun testProcessInput_fromIdle_withProductMention_shouldTransitionToProductDetected() = runTest {
        // Given
        val input = "I want to buy tilapia"
        whenever(mockPatternMatcher.detectTransactionIntent(input))
            .thenReturn(TransactionIntent.PRODUCT_MENTION)
        whenever(mockPatternMatcher.extractProduct(input))
            .thenReturn("tilapia")
        
        // When
        val result = stateMachine.processInput(input)
        
        // Then
        assertEquals("Should transition to PRODUCT_DETECTED", 
            TransactionState.PRODUCT_DETECTED, stateMachine.getCurrentState())
        assertTrue("Should return success", result.success)
        assertEquals("Should extract product", "tilapia", result.extractedProduct)
    }
    
    @Test
    fun testProcessInput_fromIdle_withPriceNegotiation_shouldTransitionToPriceNegotiation() = runTest {
        // Given
        val input = "How much for the fish?"
        whenever(mockPatternMatcher.detectTransactionIntent(input))
            .thenReturn(TransactionIntent.PRICE_INQUIRY)
        
        // When
        val result = stateMachine.processInput(input)
        
        // Then
        assertEquals("Should transition to PRICE_NEGOTIATION", 
            TransactionState.PRICE_NEGOTIATION, stateMachine.getCurrentState())
        assertTrue("Should return success", result.success)
    }
    
    @Test
    fun testProcessInput_fromProductDetected_withQuantityMention_shouldTransitionToQuantityDetected() = runTest {
        // Given - start in PRODUCT_DETECTED state
        stateMachine.processInput("I want tilapia")
        whenever(mockPatternMatcher.detectTransactionIntent("I want tilapia"))
            .thenReturn(TransactionIntent.PRODUCT_MENTION)
        whenever(mockPatternMatcher.extractProduct("I want tilapia"))
            .thenReturn("tilapia")
        
        val quantityInput = "Give me 3 pieces"
        whenever(mockPatternMatcher.detectTransactionIntent(quantityInput))
            .thenReturn(TransactionIntent.QUANTITY_MENTION)
        whenever(mockPatternMatcher.extractQuantity(quantityInput))
            .thenReturn(Pair(3, "pieces"))
        
        // When
        val result = stateMachine.processInput(quantityInput)
        
        // Then
        assertEquals("Should transition to QUANTITY_DETECTED", 
            TransactionState.QUANTITY_DETECTED, stateMachine.getCurrentState())
        assertTrue("Should return success", result.success)
        assertEquals("Should extract quantity", 3, result.extractedQuantity)
        assertEquals("Should extract unit", "pieces", result.extractedUnit)
    }
    
    @Test
    fun testProcessInput_fromQuantityDetected_withPriceAgreement_shouldTransitionToTransactionComplete() = runTest {
        // Given - navigate to QUANTITY_DETECTED state
        setupQuantityDetectedState()
        
        val priceInput = "That's 15 cedis"
        whenever(mockPatternMatcher.detectTransactionIntent(priceInput))
            .thenReturn(TransactionIntent.PRICE_AGREEMENT)
        whenever(mockPatternMatcher.extractPrice(priceInput))
            .thenReturn(15.0)
        
        // When
        val result = stateMachine.processInput(priceInput)
        
        // Then
        assertEquals("Should transition to TRANSACTION_COMPLETE", 
            TransactionState.TRANSACTION_COMPLETE, stateMachine.getCurrentState())
        assertTrue("Should return success", result.success)
        assertEquals("Should extract price", 15.0, result.extractedPrice, 0.01)
        assertTrue("Should be complete transaction", result.isCompleteTransaction)
    }
    
    @Test
    fun testProcessInput_fromPriceNegotiation_withPriceCounteroffer_shouldStayInPriceNegotiation() = runTest {
        // Given - start in PRICE_NEGOTIATION state
        stateMachine.processInput("How much?")
        whenever(mockPatternMatcher.detectTransactionIntent("How much?"))
            .thenReturn(TransactionIntent.PRICE_INQUIRY)
        
        val counterofferInput = "Can you do 10 cedis?"
        whenever(mockPatternMatcher.detectTransactionIntent(counterofferInput))
            .thenReturn(TransactionIntent.PRICE_COUNTEROFFER)
        whenever(mockPatternMatcher.extractPrice(counterofferInput))
            .thenReturn(10.0)
        
        // When
        val result = stateMachine.processInput(counterofferInput)
        
        // Then
        assertEquals("Should stay in PRICE_NEGOTIATION", 
            TransactionState.PRICE_NEGOTIATION, stateMachine.getCurrentState())
        assertTrue("Should return success", result.success)
        assertEquals("Should extract counteroffer price", 10.0, result.extractedPrice, 0.01)
    }
    
    @Test
    fun testProcessInput_withTransactionCancellation_shouldTransitionToIdle() = runTest {
        // Given - start in any state
        setupQuantityDetectedState()
        
        val cancellationInput = "Never mind, I don't want it"
        whenever(mockPatternMatcher.detectTransactionIntent(cancellationInput))
            .thenReturn(TransactionIntent.TRANSACTION_CANCELLATION)
        
        // When
        val result = stateMachine.processInput(cancellationInput)
        
        // Then
        assertEquals("Should transition to IDLE", 
            TransactionState.IDLE, stateMachine.getCurrentState())
        assertTrue("Should return success", result.success)
        assertTrue("Should be cancelled", result.isCancelled)
    }
    
    @Test
    fun testProcessInput_withUnrecognizedInput_shouldStayInCurrentState() = runTest {
        // Given
        val unrecognizedInput = "Random unrelated text"
        whenever(mockPatternMatcher.detectTransactionIntent(unrecognizedInput))
            .thenReturn(TransactionIntent.UNKNOWN)
        
        // When
        val result = stateMachine.processInput(unrecognizedInput)
        
        // Then
        assertEquals("Should stay in IDLE", 
            TransactionState.IDLE, stateMachine.getCurrentState())
        assertFalse("Should return failure", result.success)
    }
    
    @Test
    fun testProcessInput_withInvalidTransition_shouldReturnError() = runTest {
        // Given - try to go directly from IDLE to TRANSACTION_COMPLETE (invalid)
        val input = "Transaction complete"
        whenever(mockPatternMatcher.detectTransactionIntent(input))
            .thenReturn(TransactionIntent.PRICE_AGREEMENT)
        
        // When
        val result = stateMachine.processInput(input)
        
        // Then
        assertEquals("Should stay in IDLE", 
            TransactionState.IDLE, stateMachine.getCurrentState())
        assertFalse("Should return failure", result.success)
        assertNotNull("Should have error message", result.errorMessage)
    }
    
    @Test
    fun testReset_shouldReturnToIdleState() = runTest {
        // Given - navigate to some state
        setupQuantityDetectedState()
        
        // When
        stateMachine.reset()
        
        // Then
        assertEquals("Should return to IDLE", 
            TransactionState.IDLE, stateMachine.getCurrentState())
    }
    
    @Test
    fun testGetTransactionData_afterCompleteTransaction_shouldReturnAllData() = runTest {
        // Given - complete a full transaction
        setupCompleteTransaction()
        
        // When
        val transactionData = stateMachine.getTransactionData()
        
        // Then
        assertNotNull("Transaction data should not be null", transactionData)
        assertEquals("Should have product", "tilapia", transactionData?.product)
        assertEquals("Should have quantity", 3, transactionData?.quantity)
        assertEquals("Should have unit", "pieces", transactionData?.unit)
        assertEquals("Should have price", 15.0, transactionData?.totalPrice, 0.01)
    }
    
    @Test
    fun testGetTransactionData_beforeCompleteTransaction_shouldReturnNull() = runTest {
        // Given - incomplete transaction
        setupQuantityDetectedState()
        
        // When
        val transactionData = stateMachine.getTransactionData()
        
        // Then
        assertNull("Transaction data should be null for incomplete transaction", transactionData)
    }
    
    @Test
    fun testTimeout_shouldTransitionToIdle() = runTest {
        // Given - in some state
        setupQuantityDetectedState()
        
        // When
        stateMachine.handleTimeout()
        
        // Then
        assertEquals("Should transition to IDLE on timeout", 
            TransactionState.IDLE, stateMachine.getCurrentState())
    }
    
    @Test
    fun testMultipleProductMentions_shouldUpdateProduct() = runTest {
        // Given - first product mention
        stateMachine.processInput("I want tilapia")
        whenever(mockPatternMatcher.detectTransactionIntent("I want tilapia"))
            .thenReturn(TransactionIntent.PRODUCT_MENTION)
        whenever(mockPatternMatcher.extractProduct("I want tilapia"))
            .thenReturn("tilapia")
        
        // When - second product mention
        val secondInput = "Actually, I want mackerel"
        whenever(mockPatternMatcher.detectTransactionIntent(secondInput))
            .thenReturn(TransactionIntent.PRODUCT_MENTION)
        whenever(mockPatternMatcher.extractProduct(secondInput))
            .thenReturn("mackerel")
        
        val result = stateMachine.processInput(secondInput)
        
        // Then
        assertEquals("Should stay in PRODUCT_DETECTED", 
            TransactionState.PRODUCT_DETECTED, stateMachine.getCurrentState())
        assertTrue("Should return success", result.success)
        assertEquals("Should update to new product", "mackerel", result.extractedProduct)
    }
    
    @Test
    fun testComplexNegotiation_shouldHandleMultipleCounterOffers() = runTest {
        // Given - start negotiation
        stateMachine.processInput("How much for tilapia?")
        whenever(mockPatternMatcher.detectTransactionIntent("How much for tilapia?"))
            .thenReturn(TransactionIntent.PRICE_INQUIRY)
        
        // When - multiple counteroffers
        val offer1 = "20 cedis"
        whenever(mockPatternMatcher.detectTransactionIntent(offer1))
            .thenReturn(TransactionIntent.PRICE_OFFER)
        whenever(mockPatternMatcher.extractPrice(offer1))
            .thenReturn(20.0)
        
        val counter1 = "Too expensive, 15 cedis"
        whenever(mockPatternMatcher.detectTransactionIntent(counter1))
            .thenReturn(TransactionIntent.PRICE_COUNTEROFFER)
        whenever(mockPatternMatcher.extractPrice(counter1))
            .thenReturn(15.0)
        
        val agreement = "OK, 15 cedis"
        whenever(mockPatternMatcher.detectTransactionIntent(agreement))
            .thenReturn(TransactionIntent.PRICE_AGREEMENT)
        whenever(mockPatternMatcher.extractPrice(agreement))
            .thenReturn(15.0)
        
        // Process the negotiation
        stateMachine.processInput(offer1)
        stateMachine.processInput(counter1)
        val finalResult = stateMachine.processInput(agreement)
        
        // Then
        assertEquals("Should reach agreement", 
            TransactionState.TRANSACTION_COMPLETE, stateMachine.getCurrentState())
        assertTrue("Final result should be success", finalResult.success)
        assertEquals("Should have agreed price", 15.0, finalResult.extractedPrice, 0.01)
    }
    
    // Helper methods
    private suspend fun setupQuantityDetectedState() {
        // Navigate to QUANTITY_DETECTED state
        stateMachine.processInput("I want tilapia")
        whenever(mockPatternMatcher.detectTransactionIntent("I want tilapia"))
            .thenReturn(TransactionIntent.PRODUCT_MENTION)
        whenever(mockPatternMatcher.extractProduct("I want tilapia"))
            .thenReturn("tilapia")
        
        stateMachine.processInput("3 pieces")
        whenever(mockPatternMatcher.detectTransactionIntent("3 pieces"))
            .thenReturn(TransactionIntent.QUANTITY_MENTION)
        whenever(mockPatternMatcher.extractQuantity("3 pieces"))
            .thenReturn(Pair(3, "pieces"))
    }
    
    private suspend fun setupCompleteTransaction() {
        setupQuantityDetectedState()
        
        stateMachine.processInput("15 cedis")
        whenever(mockPatternMatcher.detectTransactionIntent("15 cedis"))
            .thenReturn(TransactionIntent.PRICE_AGREEMENT)
        whenever(mockPatternMatcher.extractPrice("15 cedis"))
            .thenReturn(15.0)
    }
}