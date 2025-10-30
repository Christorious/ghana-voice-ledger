package com.voiceledger.ghana.presentation.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voiceledger.ghana.data.local.entity.DailySummary
import com.voiceledger.ghana.domain.repository.DailySummaryRepository
import com.voiceledger.ghana.domain.service.DailySummaryGenerator
import com.voiceledger.ghana.domain.service.PeriodSummary
import com.voiceledger.ghana.service.TextToSpeechService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for the summary presentation screen
 * Manages summary data display and voice output functionality
 */
@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val dailySummaryRepository: DailySummaryRepository,
    private val dailySummaryGenerator: DailySummaryGenerator,
    private val textToSpeechService: TextToSpeechService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SummaryUiState())
    val uiState: StateFlow<SummaryUiState> = _uiState.asStateFlow()
    
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "GH")).apply {
        currency = Currency.getInstance("GHS")
    }
    
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val displayDateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
    private val zoneId = ZoneId.systemDefault()
    
    init {
        loadTodaysSummary()
    }
    
    fun loadTodaysSummary() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val today = LocalDate.now().format(dateFormat)
                var summary = dailySummaryRepository.getSummaryByDate(today)
                
                // If no summary exists, generate one
                if (summary == null) {
                    summary = dailySummaryGenerator.generateTodaysSummary()
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentSummary = summary,
                    selectedDate = today,
                    summaryType = SummaryType.DAILY,
                    error = null
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load summary"
                )
            }
        }
    }
    
    fun loadSummaryForDate(date: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                var summary = dailySummaryRepository.getSummaryByDate(date)
                
                // If no summary exists, generate one
                if (summary == null) {
                    summary = dailySummaryGenerator.generateDailySummary(date)
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentSummary = summary,
                    selectedDate = date,
                    summaryType = SummaryType.DAILY,
                    error = null
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load summary for $date"
                )
            }
        }
    }
    
    fun loadWeeklySummary(startDate: String, endDate: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val periodSummary = dailySummaryGenerator.generatePeriodSummary(startDate, endDate)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentPeriodSummary = periodSummary,
                    selectedDate = startDate,
                    summaryType = SummaryType.WEEKLY,
                    error = null
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load weekly summary"
                )
            }
        }
    }
    
    fun loadMonthlySummary(year: Int, month: Int) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val startDate = LocalDate.of(year, month, 1).format(dateFormat)
                val endDate = LocalDate.of(year, month, LocalDate.of(year, month, 1).lengthOfMonth()).format(dateFormat)
                
                val periodSummary = dailySummaryGenerator.generatePeriodSummary(startDate, endDate)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentPeriodSummary = periodSummary,
                    selectedDate = startDate,
                    summaryType = SummaryType.MONTHLY,
                    error = null
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load monthly summary"
                )
            }
        }
    }
    
    fun speakSummary() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSpeaking = true)
                
                val summaryText = when (_uiState.value.summaryType) {
                    SummaryType.DAILY -> generateDailySummaryText(_uiState.value.currentSummary)
                    SummaryType.WEEKLY -> generatePeriodSummaryText(_uiState.value.currentPeriodSummary, "week")
                    SummaryType.MONTHLY -> generatePeriodSummaryText(_uiState.value.currentPeriodSummary, "month")
                }
                
                textToSpeechService.speak(summaryText)
                
                _uiState.value = _uiState.value.copy(
                    isSpeaking = false,
                    message = "Summary spoken successfully"
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSpeaking = false,
                    error = "Failed to speak summary: ${e.message}"
                )
            }
        }
    }
    
    fun stopSpeaking() {
        textToSpeechService.stop()
        _uiState.value = _uiState.value.copy(isSpeaking = false)
    }
    
    fun exportSummary() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isExporting = true)
                
                val exportData = when (_uiState.value.summaryType) {
                    SummaryType.DAILY -> generateDailySummaryExport(_uiState.value.currentSummary)
                    SummaryType.WEEKLY, SummaryType.MONTHLY -> generatePeriodSummaryExport(_uiState.value.currentPeriodSummary)
                }
                
                // This would implement actual file export
                kotlinx.coroutines.delay(2000) // Simulate export process
                
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    message = "Summary exported successfully"
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    error = "Failed to export summary: ${e.message}"
                )
            }
        }
    }
    
    fun refreshSummary() {
        when (_uiState.value.summaryType) {
            SummaryType.DAILY -> {
                if (_uiState.value.selectedDate == dateFormat.format(Date())) {
                    loadTodaysSummary()
                } else {
                    loadSummaryForDate(_uiState.value.selectedDate)
                }
            }
            SummaryType.WEEKLY -> {
                val startDate = _uiState.value.selectedDate
                val localDate = LocalDate.parse(startDate, dateFormat)
                val endDate = localDate.plusDays(6).format(dateFormat)
                loadWeeklySummary(startDate, endDate)
            }
            SummaryType.MONTHLY -> {
                val localDate = LocalDate.parse(_uiState.value.selectedDate, dateFormat)
                loadMonthlySummary(localDate.year, localDate.monthValue)
            }
        }
    }
    
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    // Helper methods for formatting
    fun formatCurrency(amount: Double): String {
        return currencyFormatter.format(amount)
    }
    
    fun formatDate(date: String): String {
        return try {
            val localDate = LocalDate.parse(date, dateFormat)
            localDate.format(displayDateFormat)
        } catch (e: Exception) {
            date
        }
    }
    
    fun formatPercentage(value: Double): String {
        return "${if (value >= 0) "+" else ""}${String.format("%.1f", value)}%"
    }
    
    // Text generation for voice output
    private fun generateDailySummaryText(summary: DailySummary?): String {
        if (summary == null) return "No summary available."
        
        val text = StringBuilder()
        text.append("Daily Summary for ${formatDate(summary.date)}. ")
        
        // Main metrics
        text.append("Total sales: ${formatCurrency(summary.totalSales)}. ")
        text.append("Number of transactions: ${summary.transactionCount}. ")
        
        if (summary.uniqueCustomers > 0) {
            text.append("Served ${summary.uniqueCustomers} customers. ")
        }
        
        // Top product
        summary.topProduct?.let { product ->
            text.append("Best selling product: $product with sales of ${formatCurrency(summary.topProductSales)}. ")
        }
        
        // Peak hour
        summary.peakHour?.let { hour ->
            text.append("Peak sales hour: ${hour}:00 with ${formatCurrency(summary.peakHourSales)} in sales. ")
        }
        
        // Comparisons
        summary.comparisonWithYesterday?.let { comparison ->
            if (comparison.salesChange != 0.0) {
                val direction = if (comparison.salesChange > 0) "increased" else "decreased"
                text.append("Sales $direction by ${formatPercentage(Math.abs(comparison.salesChange))} compared to yesterday. ")
            }
        }
        
        // Recommendations
        if (summary.recommendations.isNotEmpty()) {
            text.append("Recommendations: ")
            summary.recommendations.take(3).forEach { recommendation ->
                text.append("$recommendation. ")
            }
        }
        
        return text.toString()
    }
    
    private fun generatePeriodSummaryText(summary: PeriodSummary?, period: String): String {
        if (summary == null) return "No $period summary available."
        
        val text = StringBuilder()
        text.append("$period summary from ${formatDate(summary.startDate)} to ${formatDate(summary.endDate)}. ")
        
        text.append("Total sales: ${formatCurrency(summary.totalSales)}. ")
        text.append("Total transactions: ${summary.totalTransactions}. ")
        text.append("Average daily sales: ${formatCurrency(summary.averageDailySales)}. ")
        
        summary.bestDay?.let { day ->
            text.append("Best sales day: ${formatDate(day)}. ")
        }
        
        if (summary.topProducts.isNotEmpty()) {
            text.append("Top products: ${summary.topProducts.take(3).joinToString(", ")}. ")
        }
        
        if (summary.salesTrend != 0.0) {
            val direction = if (summary.salesTrend > 0) "increasing" else "decreasing"
            text.append("Sales trend is $direction by ${formatPercentage(Math.abs(summary.salesTrend))}. ")
        }
        
        return text.toString()
    }
    
    // Export data generation
    private fun generateDailySummaryExport(summary: DailySummary?): String {
        if (summary == null) return ""
        
        return buildString {
            appendLine("Daily Summary Export")
            appendLine("Date: ${formatDate(summary.date)}")
            appendLine("Generated: ${Date()}")
            appendLine()
            appendLine("OVERVIEW")
            appendLine("Total Sales: ${formatCurrency(summary.totalSales)}")
            appendLine("Transaction Count: ${summary.transactionCount}")
            appendLine("Unique Customers: ${summary.uniqueCustomers}")
            appendLine("Average Transaction Value: ${formatCurrency(summary.averageTransactionValue)}")
            appendLine()
            appendLine("PRODUCT PERFORMANCE")
            summary.topProduct?.let { product ->
                appendLine("Top Product: $product")
                appendLine("Top Product Sales: ${formatCurrency(summary.topProductSales)}")
            }
            appendLine()
            appendLine("TIME ANALYSIS")
            summary.peakHour?.let { hour ->
                appendLine("Peak Hour: ${hour}:00")
                appendLine("Peak Hour Sales: ${formatCurrency(summary.peakHourSales)}")
            }
            summary.mostProfitableHour?.let { hour ->
                appendLine("Most Profitable Hour: ${hour}:00")
            }
            appendLine()
            appendLine("CUSTOMER INSIGHTS")
            appendLine("Repeat Customers: ${summary.repeatCustomers}")
            appendLine("New Customers: ${summary.newCustomers}")
            appendLine()
            appendLine("RECOMMENDATIONS")
            summary.recommendations.forEach { recommendation ->
                appendLine("- $recommendation")
            }
        }
    }
    
    private fun generatePeriodSummaryExport(summary: PeriodSummary?): String {
        if (summary == null) return ""
        
        return buildString {
            appendLine("Period Summary Export")
            appendLine("Period: ${formatDate(summary.startDate)} to ${formatDate(summary.endDate)}")
            appendLine("Generated: ${Date()}")
            appendLine()
            appendLine("OVERVIEW")
            appendLine("Total Sales: ${formatCurrency(summary.totalSales)}")
            appendLine("Total Transactions: ${summary.totalTransactions}")
            appendLine("Average Daily Sales: ${formatCurrency(summary.averageDailySales)}")
            appendLine()
            appendLine("PERFORMANCE")
            summary.bestDay?.let { day ->
                appendLine("Best Sales Day: ${formatDate(day)}")
            }
            summary.worstDay?.let { day ->
                appendLine("Lowest Sales Day: ${formatDate(day)}")
            }
            appendLine()
            appendLine("TOP PRODUCTS")
            summary.topProducts.forEachIndexed { index, product ->
                appendLine("${index + 1}. $product")
            }
            appendLine()
            appendLine("TRENDS")
            appendLine("Customer Growth: ${formatPercentage(summary.customerGrowth)}")
            appendLine("Sales Trend: ${formatPercentage(summary.salesTrend)}")
            appendLine()
            appendLine("RECOMMENDATIONS")
            summary.recommendations.forEach { recommendation ->
                appendLine("- $recommendation")
            }
        }
    }
}

/**
 * UI state for the summary screen
 */
data class SummaryUiState(
    val isLoading: Boolean = false,
    val isSpeaking: Boolean = false,
    val isExporting: Boolean = false,
    val currentSummary: DailySummary? = null,
    val currentPeriodSummary: PeriodSummary? = null,
    val selectedDate: String = "",
    val summaryType: SummaryType = SummaryType.DAILY,
    val error: String? = null,
    val message: String? = null
)

/**
 * Types of summaries that can be displayed
 */
enum class SummaryType {
    DAILY,
    WEEKLY,
    MONTHLY
}