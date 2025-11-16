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
 * # DashboardViewModel
 * 
 * **Clean Architecture - Presentation Layer**
 * 
 * ViewModel for the dashboard screen, managing UI state and business logic. This class acts
 * as the middleman between the UI (Compose) and the data/domain layers, following the MVVM
 * (Model-View-ViewModel) architecture pattern.
 * 
 * ## What is a ViewModel?
 * 
 * ViewModel is an Android Architecture Component that:
 * 1. Survives configuration changes (e.g., screen rotation)
 * 2. Has a lifecycle tied to the screen, not individual Composables
 * 3. Holds UI state and exposes it to the UI layer
 * 4. Provides a scope for coroutines (viewModelScope)
 * 
 * ## The @HiltViewModel Annotation:
 * 
 * Tells Hilt to automatically inject this ViewModel's dependencies. Combined with @Inject on
 * the constructor, Hilt knows how to create this ViewModel and all its dependencies.
 * 
 * In a Composable, you can get this ViewModel with:
 * ```kotlin
 * val viewModel: DashboardViewModel = hiltViewModel()
 * ```
 * 
 * ## Constructor Injection with @Inject:
 * 
 * All dependencies (repositories, service manager) are injected via the constructor. This
 * makes testing easy - we can inject mock implementations without changing any code.
 * 
 * ## Clean Architecture Layers:
 * 
 * This ViewModel demonstrates proper layering:
 * - **Presentation Layer (this class)**: Handles UI logic, state management
 * - **Domain Layer (repositories)**: Business logic, use cases
 * - **Data Layer**: Database, network, local storage
 * 
 * The ViewModel never directly touches the database or makes network calls - it goes through
 * repositories, maintaining separation of concerns.
 * 
 * ## State Management Pattern:
 * 
 * Uses the _uiState / uiState pair:
 * - **_uiState (private MutableStateFlow)**: Internal, mutable state that only this ViewModel modifies
 * - **uiState (public StateFlow)**: External, read-only state that the UI observes
 * 
 * This prevents the UI from accidentally modifying state and ensures all state changes
 * go through the ViewModel's methods.
 * 
 * @property transactionRepository Provides access to transaction data
 * @property dailySummaryRepository Provides access to daily summaries
 * @property speakerProfileRepository Provides access to speaker/customer profiles
 * @property voiceAgentServiceManager Manages the voice processing service
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val dailySummaryRepository: DailySummaryRepository,
    private val speakerProfileRepository: SpeakerProfileRepository,
    private val voiceAgentServiceManager: VoiceAgentServiceManager
) : ViewModel() {
    
    // Private mutable state - only this ViewModel can modify it
    private val _uiState = MutableStateFlow(DashboardUiState())
    
    /**
     * Public immutable state exposed to the UI.
     * 
     * ## StateFlow vs LiveData:
     * 
     * StateFlow is Kotlin's modern alternative to LiveData:
     * - **StateFlow**: Kotlin-first, works anywhere (not Android-specific)
     * - **LiveData**: Android-specific, lifecycle-aware by default
     * 
     * ## asStateFlow():
     * 
     * Converts MutableStateFlow to read-only StateFlow. The UI can collect/observe this
     * flow but cannot modify it. All modifications must go through ViewModel methods.
     * 
     * ## How the UI Uses This:
     * 
     * ```kotlin
     * val uiState by viewModel.uiState.collectAsState()
     * // UI automatically recomposes when uiState changes
     * ```
     */
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "GH")).apply {
        currency = Currency.getInstance("GHS")
    }
    
    /**
     * Initialization block - runs when the ViewModel is created.
     * 
     * ## The init Block:
     * 
     * Kotlin's `init` block runs after the primary constructor but before the class is used.
     * It's perfect for initialization logic like loading initial data.
     * 
     * Here we:
     * 1. Load the dashboard data (transactions, analytics, summaries)
     * 2. Start observing the voice service state for real-time updates
     */
    init {
        loadDashboardData()
        observeServiceState()
    }
    
    /**
     * Loads all dashboard data from multiple repositories.
     * 
     * ## viewModelScope:
     * 
     * A coroutine scope provided by the ViewModel. It's automatically cancelled when the
     * ViewModel is cleared (user navigates away), preventing memory leaks. All database
     * operations run in this scope.
     * 
     * ## The combine Operator:
     * 
     * Combines multiple Flow sources into a single Flow. Whenever ANY of the source Flows
     * emit a new value, combine runs with the latest value from ALL sources.
     * 
     * This is powerful for dashboard UIs that need data from multiple sources:
     * - Transactions Flow
     * - Analytics Flow
     * - Summary Flow
     * - Customers Flow
     * - Service State Flow
     * 
     * When any changes, the dashboard automatically updates with fresh data from all sources.
     * 
     * ## Error Handling:
     * 
     * Uses both try-catch and Flow's .catch operator for comprehensive error handling:
     * - .catch: Handles errors in the Flow pipeline
     * - try-catch: Handles errors in setup or unexpected exceptions
     * 
     * ## State Updates:
     * 
     * Updates state using `copy()` - a data class feature that creates a new instance with
     * only specified fields changed. This immutability is crucial for reactive UIs.
     */
    private fun loadDashboardData() {
        // Launch a coroutine in the ViewModel's scope
        viewModelScope.launch {
            try {
                // Set loading state before fetching data
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Combine multiple data sources into a single reactive stream
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