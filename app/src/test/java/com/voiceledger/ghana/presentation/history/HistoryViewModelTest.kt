package com.voiceledger.ghana.presentation.history

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.voiceledger.ghana.data.local.entity.Transaction
import com.voiceledger.ghana.domain.repository.ProductVocabularyRepository
import com.voiceledger.ghana.domain.repository.TransactionRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var productVocabularyRepository: ProductVocabularyRepository
    private lateinit var viewModel: HistoryViewModel

    private val sampleTransactions = listOf(
        Transaction(
            id = "1",
            product = "Rice",
            amount = 50.0,
            timestamp = System.currentTimeMillis(),
            transcriptSnippet = "I want to buy rice",
            confidence = 0.95f,
            needsReview = false,
            quantity = 2.0,
            unit = "kg",
            customerId = "customer1"
        ),
        Transaction(
            id = "2",
            product = "Beans",
            amount = 30.0,
            timestamp = System.currentTimeMillis() - 3600000,
            transcriptSnippet = "Give me beans",
            confidence = 0.85f,
            needsReview = true,
            quantity = 1.5,
            unit = "kg",
            customerId = "customer2"
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        transactionRepository = mockk()
        productVocabularyRepository = mockk()

        every { transactionRepository.getAllTransactions() } returns flowOf(sampleTransactions)
        coEvery { transactionRepository.getAllProducts() } returns listOf("Rice", "Beans", "Tomatoes")
        coEvery { transactionRepository.getAllCustomerIds() } returns listOf("customer1", "customer2")
        coEvery { transactionRepository.deleteTransaction(any()) } just Runs
        coEvery { transactionRepository.markTransactionAsReviewed(any()) } just Runs

        viewModel = HistoryViewModel(transactionRepository, productVocabularyRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should load transactions`() = runTest {
        // Given - setup is done in @Before

        // When - viewModel is initialized

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertEquals(2, uiState.transactions.size)
        assertEquals(80.0, uiState.totalAmount)
        assertEquals(2, uiState.totalTransactions)
    }

    @Test
    fun `search query should filter transactions`() = runTest {
        // Given
        val searchQuery = "rice"

        // When
        viewModel.updateSearchQuery(searchQuery)

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(1, uiState.transactions.size)
        assertEquals("Rice", uiState.transactions.first().product)
    }

    @Test
    fun `amount filter should filter transactions`() = runTest {
        // Given
        val minAmount = 40.0
        val maxAmount = 60.0

        // When
        viewModel.updateAmountFilter(minAmount, maxAmount)

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(1, uiState.transactions.size)
        assertEquals("Rice", uiState.transactions.first().product)
    }

    @Test
    fun `review filter should show only transactions needing review`() = runTest {
        // Given
        val showOnlyNeedsReview = true

        // When
        viewModel.updateReviewFilter(showOnlyNeedsReview)

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(1, uiState.transactions.size)
        assertTrue(uiState.transactions.first().needsReview)
    }

    @Test
    fun `clear filters should reset all filters`() = runTest {
        // Given - set some filters first
        viewModel.updateSearchQuery("rice")
        viewModel.updateAmountFilter(40.0, 60.0)
        viewModel.updateReviewFilter(true)

        // When
        viewModel.clearFilters()

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(2, uiState.transactions.size)
        assertEquals("", viewModel.searchQuery.value)
        assertFalse(viewModel.hasActiveFilters())
    }

    @Test
    fun `delete transaction should call repository`() = runTest {
        // Given
        val transaction = sampleTransactions.first()

        // When
        viewModel.deleteTransaction(transaction)

        // Then
        coVerify { transactionRepository.deleteTransaction(transaction) }
    }

    @Test
    fun `mark as reviewed should call repository`() = runTest {
        // Given
        val transaction = sampleTransactions.first()

        // When
        viewModel.markAsReviewed(transaction)

        // Then
        coVerify { transactionRepository.markTransactionAsReviewed(transaction.id) }
    }

    @Test
    fun `format currency should return formatted string`() {
        // Given
        val amount = 123.45

        // When
        val formatted = viewModel.formatCurrency(amount)

        // Then
        assertTrue(formatted.contains("123"))
        assertTrue(formatted.contains("45"))
    }

    @Test
    fun `has active filters should return true when filters are applied`() = runTest {
        // Given
        viewModel.updateSearchQuery("rice")

        // When
        val hasFilters = viewModel.hasActiveFilters()

        // Then
        assertTrue(hasFilters)
    }

    @Test
    fun `has active filters should return false when no filters are applied`() = runTest {
        // Given - no filters applied

        // When
        val hasFilters = viewModel.hasActiveFilters()

        // Then
        assertFalse(hasFilters)
    }

    @Test
    fun `filter summary should describe active filters`() = runTest {
        // Given
        viewModel.updateReviewFilter(true)

        // When
        val summary = viewModel.getFilterSummary()

        // Then
        assertTrue(summary.contains("needs review"))
    }

    @Test
    fun `export transactions should update loading state`() = runTest {
        // When
        viewModel.exportTransactions()

        // Then - should eventually complete and show success message
        advanceUntilIdle()
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isExporting)
        assertEquals("Transactions exported successfully", uiState.message)
    }
}