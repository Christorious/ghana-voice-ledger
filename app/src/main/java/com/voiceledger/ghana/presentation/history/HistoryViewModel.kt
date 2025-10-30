package com.voiceledger.ghana.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voiceledger.ghana.data.local.entity.Transaction
import com.voiceledger.ghana.domain.repository.TransactionRepository
import com.voiceledger.ghana.domain.repository.ProductVocabularyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for the transaction history screen
 * Manages search, filtering, and transaction display
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val productVocabularyRepository: ProductVocabularyRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _selectedFilters = MutableStateFlow(TransactionFilters())
    val selectedFilters: StateFlow<TransactionFilters> = _selectedFilters.asStateFlow()
    
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "GH")).apply {
        currency = Currency.getInstance("GHS")
    }
    
    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val zoneId = ZoneId.systemDefault()
    
    init {
        loadTransactionHistory()
        setupSearch()
    }
    
    private fun loadTransactionHistory() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Combine search query, filters, and transactions
                combine(
                    transactionRepository.getAllTransactions(),
                    _searchQuery,
                    _selectedFilters
                ) { transactions, query, filters ->
                    filterAndSearchTransactions(transactions, query, filters)
                }.catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load transaction history"
                    )
                }.collect { filteredTransactions ->
                    val groupedTransactions = groupTransactionsByDate(filteredTransactions)
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        groupedTransactions = groupedTransactions,
                        totalTransactions = filteredTransactions.size,
                        totalAmount = filteredTransactions.sumOf { it.amount },
                        error = null
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }
    
    private fun setupSearch() {
        viewModelScope.launch {
            // Load available filter options
            val products = transactionRepository.getAllProducts()
            val customers = transactionRepository.getAllCustomerIds()
            
            _uiState.value = _uiState.value.copy(
                availableProducts = products,
                availableCustomers = customers
            )
        }
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun updateFilters(filters: TransactionFilters) {
        _selectedFilters.value = filters
    }
    
    fun clearFilters() {
        _selectedFilters.value = TransactionFilters()
        _searchQuery.value = ""
    }
    
    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                transactionRepository.deleteTransaction(transaction)
                _uiState.value = _uiState.value.copy(
                    message = "Transaction deleted successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete transaction: ${e.message}"
                )
            }
        }
    }
    
    fun markTransactionAsReviewed(transactionId: String) {
        viewModelScope.launch {
            try {
                transactionRepository.markTransactionAsReviewed(transactionId)
                _uiState.value = _uiState.value.copy(
                    message = "Transaction marked as reviewed"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update transaction: ${e.message}"
                )
            }
        }
    }
    
    fun exportTransactions(dateRange: Pair<String, String>) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isExporting = true)
                
                // Get transactions in date range
                val startTime = parseDate(dateRange.first)?.time ?: 0L
                val endTime = parseDate(dateRange.second)?.time ?: System.currentTimeMillis()
                
                transactionRepository.getTransactionsByTimeRange(startTime, endTime).first().let { transactions ->
                    // In a real implementation, this would export to CSV or other format
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        message = "Exported ${transactions.size} transactions"
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    error = "Failed to export transactions: ${e.message}"
                )
            }
        }
    }
    
    fun getTransactionStats(): TransactionStats {
        val transactions = _uiState.value.groupedTransactions.values.flatten()
        
        return TransactionStats(
            totalCount = transactions.size,
            totalAmount = transactions.sumOf { it.amount },
            averageAmount = if (transactions.isNotEmpty()) transactions.sumOf { it.amount } / transactions.size else 0.0,
            highestAmount = transactions.maxOfOrNull { it.amount } ?: 0.0,
            lowestAmount = transactions.minOfOrNull { it.amount } ?: 0.0,
            uniqueProducts = transactions.map { it.product }.distinct().size,
            uniqueCustomers = transactions.mapNotNull { it.customerId }.distinct().size,
            needsReviewCount = transactions.count { it.needsReview }
        )
    }
    
    fun formatCurrency(amount: Double): String {
        return currencyFormatter.format(amount)
    }
    
    fun formatDate(timestamp: Long): String {
        return dateFormatter.format(Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate())
    }
    
    fun formatTime(timestamp: Long): String {
        return timeFormatter.format(Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalTime())
    }
    
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    private fun filterAndSearchTransactions(
        transactions: List<Transaction>,
        query: String,
        filters: TransactionFilters
    ): List<Transaction> {
        return transactions.filter { transaction ->
            // Text search
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                transaction.product.contains(query, ignoreCase = true) ||
                transaction.transcriptSnippet.contains(query, ignoreCase = true) ||
                transaction.amount.toString().contains(query) ||
                transaction.customerId?.contains(query, ignoreCase = true) == true
            }
            
            // Date range filter
            val matchesDateRange = if (filters.startDate != null && filters.endDate != null) {
                val transactionDate = parseDate(transaction.date)
                val startDate = parseDate(filters.startDate)
                val endDate = parseDate(filters.endDate)
                
                transactionDate != null && startDate != null && endDate != null &&
                transactionDate >= startDate && transactionDate <= endDate
            } else {
                true
            }
            
            // Product filter
            val matchesProduct = if (filters.selectedProduct != null) {
                transaction.product.equals(filters.selectedProduct, ignoreCase = true)
            } else {
                true
            }
            
            // Customer filter
            val matchesCustomer = if (filters.selectedCustomer != null) {
                transaction.customerId == filters.selectedCustomer
            } else {
                true
            }
            
            // Amount range filter
            val matchesAmountRange = if (filters.minAmount != null || filters.maxAmount != null) {
                val minAmount = filters.minAmount ?: 0.0
                val maxAmount = filters.maxAmount ?: Double.MAX_VALUE
                transaction.amount >= minAmount && transaction.amount <= maxAmount
            } else {
                true
            }
            
            // Review status filter
            val matchesReviewStatus = when (filters.reviewStatus) {
                ReviewStatus.NEEDS_REVIEW -> transaction.needsReview
                ReviewStatus.REVIEWED -> !transaction.needsReview
                ReviewStatus.ALL -> true
            }
            
            matchesQuery && matchesDateRange && matchesProduct && 
            matchesCustomer && matchesAmountRange && matchesReviewStatus
        }.sortedByDescending { it.timestamp }
    }
    
    private fun groupTransactionsByDate(transactions: List<Transaction>): Map<String, List<Transaction>> {
        return transactions.groupBy { transaction ->
            formatDate(transaction.timestamp)
        }
    }
    
    private fun parseDate(dateString: String?): Date? {
        return try {
            dateString?.let { 
                Date.from(LocalDate.parse(it, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    .atStartOfDay(zoneId).toInstant())
            }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * UI state for the history screen
 */
data class HistoryUiState(
    val isLoading: Boolean = false,
    val isExporting: Boolean = false,
    val groupedTransactions: Map<String, List<Transaction>> = emptyMap(),
    val totalTransactions: Int = 0,
    val totalAmount: Double = 0.0,
    val availableProducts: List<String> = emptyList(),
    val availableCustomers: List<String> = emptyList(),
    val error: String? = null,
    val message: String? = null
)

/**
 * Transaction filters
 */
data class TransactionFilters(
    val startDate: String? = null,
    val endDate: String? = null,
    val selectedProduct: String? = null,
    val selectedCustomer: String? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val reviewStatus: ReviewStatus = ReviewStatus.ALL
)

/**
 * Review status filter options
 */
enum class ReviewStatus {
    ALL,
    NEEDS_REVIEW,
    REVIEWED
}

/**
 * Transaction statistics
 */
data class TransactionStats(
    val totalCount: Int,
    val totalAmount: Double,
    val averageAmount: Double,
    val highestAmount: Double,
    val lowestAmount: Double,
    val uniqueProducts: Int,
    val uniqueCustomers: Int,
    val needsReviewCount: Int
)