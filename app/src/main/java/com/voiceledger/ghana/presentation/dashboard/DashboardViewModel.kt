package com.voiceledger.ghana.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voiceledger.ghana.data.local.entity.Transaction
import com.voiceledger.ghana.domain.repository.TransactionRepository
import com.voiceledger.ghana.domain.repository.DailySummaryRepository
import com.voiceledger.ghana.domain.repository.SpeakerProfileRepository
import com.voiceledger.ghana.service.VoiceAgentServiceManager
import com.voiceledger.ghana.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for the dashboard screen
 * Manages dashboard state and business logic
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val dailySummaryRepository: DailySummaryRepository,
    private val speakerProfileRepository: SpeakerProfileRepository,
    private val voiceAgentServiceManager: VoiceAgentServiceManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "GH")).apply {
        currency = Currency.getInstance("GHS")
    }
    
    init {
        loadDashboardData()
        observeServiceState()
    }
    
    private fun loadDashboardData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Combine multiple data sources
                combine(
                    transactionRepository.getTodaysTransactions(),
                    transactionRepository.getTodaysAnalytics(),
                    dailySummaryRepository.getTodaysSummaryFlow(),
                    speakerProfileRepository.getRegularCustomers(),
                    voiceAgentServiceManager.serviceState
                ) { transactions, analytics, summary, customers, serviceState ->
                    
                    Pair(
                        DashboardData(
                            totalSales = analytics.totalSales,
                            transactionCount = analytics.transactionCount,
                            topProduct = analytics.topProduct ?: "No sales yet",
                            peakHour = analytics.peakHour ?: "N/A",
                            uniqueCustomers = analytics.uniqueCustomers,
                            regularCustomers = customers.size,
                            recentTransactions = transactions.take(10),
                            isListening = serviceState.isListening,
                            serviceStatus = serviceState.status,
                            batteryLevel = serviceState.batteryLevel,
                            lastTransactionTime = transactions.firstOrNull()?.timestamp
                        ),
                        summary
                    )
                }.catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load dashboard data"
                    )
                }.collect { (data, summary) ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        data = data,
                        todaysSummary = summary,
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
    
    private fun observeServiceState() {
        viewModelScope.launch {
            voiceAgentServiceManager.serviceState.collect { state ->
                _uiState.value = _uiState.value.copy(
                    data = _uiState.value.data?.copy(
                        isListening = state.isListening,
                        serviceStatus = state.status,
                        batteryLevel = state.batteryLevel
                    )
                )
            }
        }
    }
    
    fun pauseListening() {
        viewModelScope.launch {
            try {
                voiceAgentServiceManager.pauseListening()
                _uiState.value = _uiState.value.copy(
                    message = "Listening paused"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to pause listening: ${e.message}"
                )
            }
        }
    }
    
    fun resumeListening() {
        viewModelScope.launch {
            try {
                voiceAgentServiceManager.startListening()
                _uiState.value = _uiState.value.copy(
                    message = "Listening resumed"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to resume listening: ${e.message}"
                )
            }
        }
    }
    
    fun generateDailySummary() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isGeneratingSummary = true)
                
                val summary = dailySummaryRepository.generateTodaysSummary()
                
                _uiState.value = _uiState.value.copy(
                    isGeneratingSummary = false,
                    todaysSummary = summary,
                    message = "Daily summary generated successfully"
                )
                
                // Refresh dashboard data to show updated summary
                loadDashboardData()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGeneratingSummary = false,
                    error = "Failed to generate summary: ${e.message}"
                )
            }
        }
    }
    
    fun refreshData() {
        loadDashboardData()
    }
    
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun formatCurrency(amount: Double): String {
        return currencyFormatter.format(amount)
    }
    
    fun getTimeSinceLastTransaction(): String? {
        val lastTransactionTime = _uiState.value.data?.lastTransactionTime ?: return null
        val now = System.currentTimeMillis()
        val diffMinutes = (now - lastTransactionTime) / (1000 * 60)
        
        return when {
            diffMinutes < 1 -> "Just now"
            diffMinutes < 60 -> "${diffMinutes}m ago"
            diffMinutes < 1440 -> "${diffMinutes / 60}h ago"
            else -> "${diffMinutes / 1440}d ago"
        }
    }
    
    fun getMarketStatus(): MarketStatus {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            currentHour < 6 -> MarketStatus.BEFORE_HOURS
            currentHour >= 18 -> MarketStatus.AFTER_HOURS
            else -> MarketStatus.OPEN
        }
    }
    
    fun getCurrentDate(): String {
        return DateUtils.getTodayDateString()
    }
    
    fun getBatteryStatusColor(): androidx.compose.ui.graphics.Color {
        val batteryLevel = _uiState.value.data?.batteryLevel ?: 100
        return when {
            batteryLevel > 50 -> androidx.compose.ui.graphics.Color.Green
            batteryLevel > 20 -> androidx.compose.ui.graphics.Color(0xFFFF9800) // Orange
            else -> androidx.compose.ui.graphics.Color.Red
        }
    }
}

/**
 * UI state for the dashboard screen
 */
data class DashboardUiState(
    val isLoading: Boolean = false,
    val isGeneratingSummary: Boolean = false,
    val data: DashboardData? = null,
    val todaysSummary: com.voiceledger.ghana.data.local.entity.DailySummary? = null,
    val error: String? = null,
    val message: String? = null
)

/**
 * Dashboard data model
 */
data class DashboardData(
    val totalSales: Double,
    val transactionCount: Int,
    val topProduct: String,
    val peakHour: String,
    val uniqueCustomers: Int,
    val regularCustomers: Int,
    val recentTransactions: List<Transaction>,
    val isListening: Boolean,
    val serviceStatus: String,
    val batteryLevel: Int,
    val lastTransactionTime: Long?
)

/**
 * Market status enumeration
 */
enum class MarketStatus {
    BEFORE_HOURS,
    OPEN,
    AFTER_HOURS
}

/**
 * Service state data
 */
data class ServiceState(
    val isListening: Boolean = false,
    val status: String = "Stopped",
    val batteryLevel: Int = 100
)