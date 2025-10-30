package com.voiceledger.ghana.ml.transaction

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.voiceledger.ghana.data.local.entity.ProductVocabulary
import com.voiceledger.ghana.data.local.entity.Transaction
import com.voiceledger.ghana.domain.repository.AudioMetadataRepository
import com.voiceledger.ghana.domain.repository.ProductVocabularyRepository
import com.voiceledger.ghana.domain.repository.TransactionRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionProcessorTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var transactionProcessor: TransactionProcessor
    private lateinit var stateMachine: TransactionStateMachine
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var productVocabularyRepository: ProductVocabularyRepository
    private lateinit var audioMetadataRepository: AudioMetadataRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        stateMachine = mockk(relaxed = true)
        transactionRepository = mockk(relaxed = true)
        productVocabularyRepository = mockk(relaxed = true)
        audioMetadataRepository = mockk(relaxed = true)

        every { stateMachine.currentState } returns MutableStateFlow(TransactionState.IDLE)
        every { stateMachine.transactionCompleted } returns MutableStateFlow(null)

        transactionProcessor = TransactionProcessor(
            stateMachine = stateMachine,
            transactionRepository = transactionRepository,
            productVocabularyRepository = productVocabularyRepository,
            audioMetadataRepository = audioMetadataRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        transactionProcessor.cleanup()
        unmockkAll()
    }

    @Test
    fun `processUtterance with blank transcript returns error result`() = runTest {
        val result = transactionProcessor.processUtterance(
            transcript = "",
            speakerId = "seller1",
            isSeller = true,
            confidence = 0.9f
        )

        assertFalse(result.processed)
        assertEquals("Empty transcript", result.error)
        assertEquals(0f, result.confidence)
    }

    @Test
    fun `processUtterance with valid transcript processes through state machine`() = runTest {
        val transcript = "how much tilapia"
        val speakerId = "customer1"
        val isSeller = false

        coEvery { productVocabularyRepository.findBestMatch(any()) } returns null

        every { stateMachine.processUtterance(any(), any(), any(), any()) } returns StateTransition(
            fromState = TransactionState.IDLE,
            toState = TransactionState.INQUIRY,
            trigger = transcript,
            confidence = 0.85f,
            extractedData = mapOf("product" to "tilapia")
        )

        every { stateMachine.getCurrentContext() } returns TransactionContext(
            sessionId = "session1",
            startTime = System.currentTimeMillis(),
            lastActivity = System.currentTimeMillis(),
            currentState = TransactionState.INQUIRY,
            stateHistory = emptyList(),
            extractedProduct = "tilapia"
        )

        val result = transactionProcessor.processUtterance(
            transcript = transcript,
            speakerId = speakerId,
            isSeller = isSeller,
            confidence = 0.9f
        )

        assertTrue(result.processed)
        assertTrue(result.stateChanged)
        assertEquals(TransactionState.INQUIRY, result.currentState)
        assertEquals(0.85f, result.confidence)
        assertNotNull(result.transactionContext)
    }

    @Test
    fun `processUtterance enhances transcript with product vocabulary`() = runTest {
        val transcript = "how much apateshi"

        coEvery { productVocabularyRepository.findBestMatch("how") } returns null
        coEvery { productVocabularyRepository.findBestMatch("much") } returns null
        coEvery { productVocabularyRepository.findBestMatch("apateshi") } returns ProductVocabulary(
            id = "tilapia-001",
            canonicalName = "Tilapia",
            category = "fish",
            variants = "tilapia,apateshi",
            minPrice = 12.0,
            maxPrice = 25.0,
            measurementUnits = "piece",
            frequency = 10,
            isActive = true,
            seasonality = null,
            twiNames = "apateshi",
            gaNames = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isLearned = false,
            learningConfidence = 1.0f
        )

        every { stateMachine.processUtterance(any(), any(), any(), any()) } returns StateTransition(
            fromState = TransactionState.IDLE,
            toState = TransactionState.INQUIRY,
            trigger = "how much Tilapia",
            confidence = 0.85f
        )

        val result = transactionProcessor.processUtterance(
            transcript = transcript,
            speakerId = "customer1",
            isSeller = false,
            confidence = 0.9f
        )

        verify { stateMachine.processUtterance(match { it.contains("Tilapia") }, any(), any(), any()) }
        assertTrue(result.processed)
    }

    @Test
    fun `completed transaction with high confidence is auto-saved`() = runTest {
        val transaction = Transaction(
            id = "tx1",
            timestamp = System.currentTimeMillis(),
            amount = 50.0,
            product = "Tilapia",
            quantity = 2,
            unit = "piece",
            customerId = "customer1",
            sellerId = "seller1",
            confidence = 0.85f,
            needsReview = false,
            isSynced = false
        )

        coEvery { productVocabularyRepository.isValidProduct("Tilapia") } returns true
        coEvery { productVocabularyRepository.validatePrice("Tilapia", 50.0) } returns true
        coEvery { productVocabularyRepository.findBestMatch("Tilapia") } returns createProduct("tilapia-001", "Tilapia")
        coEvery { productVocabularyRepository.incrementFrequency(any()) } just Runs
        coEvery { transactionRepository.insertTransaction(any()) } just Runs

        val transactionFlow = MutableStateFlow<Transaction?>(null)
        every { stateMachine.transactionCompleted } returns transactionFlow

        transactionFlow.value = transaction
        testScheduler.advanceUntilIdle()

        coVerify { transactionRepository.insertTransaction(match { !it.needsReview }) }
    }

    @Test
    fun `completed transaction with low confidence needs review`() = runTest {
        val transaction = Transaction(
            id = "tx2",
            timestamp = System.currentTimeMillis(),
            amount = 50.0,
            product = "Tilapia",
            quantity = 2,
            unit = "piece",
            customerId = "customer1",
            sellerId = "seller1",
            confidence = 0.65f,
            needsReview = false,
            isSynced = false
        )

        coEvery { productVocabularyRepository.isValidProduct("Tilapia") } returns true
        coEvery { productVocabularyRepository.validatePrice("Tilapia", 50.0) } returns true
        coEvery { productVocabularyRepository.findBestMatch("Tilapia") } returns createProduct("tilapia-001", "Tilapia")
        coEvery { productVocabularyRepository.incrementFrequency(any()) } just Runs
        coEvery { transactionRepository.insertTransaction(any()) } just Runs

        val transactionFlow = MutableStateFlow<Transaction?>(null)
        every { stateMachine.transactionCompleted } returns transactionFlow

        transactionFlow.value = transaction
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 0) { transactionRepository.insertTransaction(any()) }
    }

    @Test
    fun `getCurrentState returns current state from state machine`() {
        every { stateMachine.currentState } returns MutableStateFlow(TransactionState.INQUIRY)

        val state = transactionProcessor.getCurrentState()

        assertEquals(TransactionState.INQUIRY, state)
    }

    @Test
    fun `getCurrentContext returns context from state machine`() {
        val context = TransactionContext(
            sessionId = "session1",
            startTime = System.currentTimeMillis(),
            lastActivity = System.currentTimeMillis(),
            currentState = TransactionState.AGREEMENT,
            stateHistory = emptyList(),
            extractedAmount = 25.0,
            extractedProduct = "Mackerel"
        )

        every { stateMachine.getCurrentContext() } returns context

        val result = transactionProcessor.getCurrentContext()

        assertEquals(context, result)
    }

    @Test
    fun `forceCompleteTransaction completes transaction if data is valid`() = runTest {
        val context = TransactionContext(
            sessionId = "session1",
            startTime = System.currentTimeMillis(),
            lastActivity = System.currentTimeMillis(),
            currentState = TransactionState.AGREEMENT,
            stateHistory = emptyList(),
            extractedAmount = 30.0,
            extractedProduct = "Sardines",
            sellerId = "seller1",
            customerId = "customer1"
        )

        val transaction = Transaction(
            id = "forced_tx",
            timestamp = System.currentTimeMillis(),
            amount = 30.0,
            product = "Sardines",
            quantity = 1,
            unit = "tin",
            customerId = "customer1",
            sellerId = "seller1",
            confidence = 0.8f,
            needsReview = false,
            isSynced = false
        )

        every { stateMachine.getCurrentContext() } returns context
        every { stateMachine.forceComplete() } returns transaction

        coEvery { productVocabularyRepository.isValidProduct("Sardines") } returns true
        coEvery { productVocabularyRepository.validatePrice("Sardines", 30.0) } returns true
        coEvery { productVocabularyRepository.findBestMatch("Sardines") } returns createProduct("sardines-001", "Sardines")
        coEvery { productVocabularyRepository.incrementFrequency(any()) } just Runs
        coEvery { transactionRepository.insertTransaction(any()) } just Runs

        val result = transactionProcessor.forceCompleteTransaction()

        assertNotNull(result)
        assertEquals("forced_tx", result.id)
    }

    @Test
    fun `resetStateMachine resets state machine and processing state`() {
        every { stateMachine.reset() } just Runs

        transactionProcessor.resetStateMachine()

        verify { stateMachine.reset() }
        assertEquals(ProcessingState.IDLE, transactionProcessor.processingState.value)
    }

    @Test
    fun `getTransactionStats returns today's statistics`() = runTest {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())

        coEvery { transactionRepository.getTotalSalesForDate(today) } returns 500.0
        coEvery { transactionRepository.getTransactionCountForDate(today) } returns 15

        every { stateMachine.currentState } returns MutableStateFlow(TransactionState.IDLE)
        every { stateMachine.getCurrentContext() } returns null

        val stats = transactionProcessor.getTransactionStats()

        assertEquals(500.0, stats.todayTotal)
        assertEquals(15, stats.todayCount)
        assertEquals(TransactionState.IDLE, stats.currentState)
    }

    @Test
    fun `transaction validation fails for invalid amount`() = runTest {
        val transaction = Transaction(
            id = "invalid_tx",
            timestamp = System.currentTimeMillis(),
            amount = 0.0,
            product = "Fish",
            quantity = 1,
            unit = "piece",
            customerId = "customer1",
            sellerId = "seller1",
            confidence = 0.9f,
            needsReview = false,
            isSynced = false
        )

        coEvery { productVocabularyRepository.isValidProduct("Fish") } returns true
        coEvery { productVocabularyRepository.validatePrice("Fish", 0.0) } returns false
        coEvery { transactionRepository.insertTransaction(any()) } just Runs

        val transactionFlow = MutableStateFlow<Transaction?>(null)
        every { stateMachine.transactionCompleted } returns transactionFlow

        transactionFlow.value = transaction
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 0) { transactionRepository.insertTransaction(any()) }
    }

    @Test
    fun `transaction validation fails for unknown product`() = runTest {
        val transaction = Transaction(
            id = "unknown_tx",
            timestamp = System.currentTimeMillis(),
            amount = 50.0,
            product = "UnknownProduct",
            quantity = 1,
            unit = "piece",
            customerId = "customer1",
            sellerId = "seller1",
            confidence = 0.9f,
            needsReview = false,
            isSynced = false
        )

        coEvery { productVocabularyRepository.isValidProduct("UnknownProduct") } returns false
        coEvery { productVocabularyRepository.validatePrice("UnknownProduct", 50.0) } returns false
        coEvery { transactionRepository.insertTransaction(any()) } just Runs

        val transactionFlow = MutableStateFlow<Transaction?>(null)
        every { stateMachine.transactionCompleted } returns transactionFlow

        transactionFlow.value = transaction
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 0) { transactionRepository.insertTransaction(any()) }
    }

    @Test
    fun `detectedTransactions emits events for completed transactions`() = runTest(testDispatcher) {
        val transaction = Transaction(
            id = "emit_tx",
            timestamp = System.currentTimeMillis(),
            amount = 40.0,
            product = "Mackerel",
            quantity = 3,
            unit = "piece",
            customerId = "customer1",
            sellerId = "seller1",
            confidence = 0.88f,
            needsReview = false,
            isSynced = false
        )

        coEvery { productVocabularyRepository.isValidProduct("Mackerel") } returns true
        coEvery { productVocabularyRepository.validatePrice("Mackerel", 40.0) } returns true
        coEvery { productVocabularyRepository.findBestMatch("Mackerel") } returns createProduct("mackerel-001", "Mackerel")
        coEvery { productVocabularyRepository.incrementFrequency(any()) } just Runs
        coEvery { transactionRepository.insertTransaction(any()) } just Runs

        val transactionFlow = MutableStateFlow<Transaction?>(null)
        every { stateMachine.transactionCompleted } returns transactionFlow

        val emissions = mutableListOf<DetectedTransaction>()
        backgroundScope.launch {
            transactionProcessor.detectedTransactions.collect { emissions.add(it) }
        }

        transactionFlow.value = transaction
        testScheduler.advanceUntilIdle()

        assertTrue(emissions.size > 0)
        assertEquals("emit_tx", emissions.first().transaction.id)
        assertTrue(emissions.first().autoSaved)
    }

    @Test
    fun `processing state transitions through states correctly`() = runTest {
        val transcript = "five cedis tilapia"

        coEvery { productVocabularyRepository.findBestMatch(any()) } returns null

        every { stateMachine.processUtterance(any(), any(), any(), any()) } returns StateTransition(
            fromState = TransactionState.IDLE,
            toState = TransactionState.PRICE_QUOTE,
            trigger = transcript,
            confidence = 0.88f
        )

        assertEquals(ProcessingState.IDLE, transactionProcessor.processingState.value)

        val result = transactionProcessor.processUtterance(
            transcript = transcript,
            speakerId = "seller1",
            isSeller = true,
            confidence = 0.9f
        )

        assertTrue(result.processed)
        assertEquals(ProcessingState.SUCCESS, transactionProcessor.processingState.value)
    }

    private fun createProduct(id: String, canonicalName: String) = ProductVocabulary(
        id = id,
        canonicalName = canonicalName,
        category = "fish",
        variants = canonicalName.lowercase(),
        minPrice = 10.0,
        maxPrice = 50.0,
        measurementUnits = "piece",
        frequency = 10,
        isActive = true,
        seasonality = null,
        twiNames = null,
        gaNames = null,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        isLearned = false,
        learningConfidence = 1.0f
    )
}
