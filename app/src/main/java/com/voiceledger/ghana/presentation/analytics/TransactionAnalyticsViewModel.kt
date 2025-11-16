package com.voiceledger.ghana.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voiceledger.ghana.domain.model.AnalyticsRange
import com.voiceledger.ghana.domain.model.TransactionAnalyticsOverview
import com.voiceledger.ghana.domain.repository.TransactionAnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TransactionAnalyticsViewModel @Inject constructor(
    private val analyticsRepository: TransactionAnalyticsRepository
) : ViewModel() {

    private val selectedRange = MutableStateFlow<AnalyticsRange>(AnalyticsRange.Last30Days)
    private val _uiState = MutableStateFlow(
        TransactionAnalyticsUiState(selectedRange = AnalyticsRange.Last30Days)
    )
    val uiState: StateFlow<TransactionAnalyticsUiState> = _uiState.asStateFlow()

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "GH")).apply {
        currency = Currency.getInstance("GHS")
    }

    init {
        observeAnalytics()
    }

    private fun observeAnalytics() {
        viewModelScope.launch {
            selectedRange
                .flatMapLatest { range ->
                    analyticsRepository.observeAnalytics(range)
                        .map<Result<Pair<AnalyticsRange, TransactionAnalyticsOverview>>> { overview ->
                            Result.success(range to overview)
                        }
                        .onStart {
                            _uiState.value = _uiState.value.copy(
                                isLoading = true,
                                selectedRange = range,
                                errorMessage = null
                            )
                        }
                        .catch { error ->
                            emit(Result.failure(error))
                        }
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { (range, overview) ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                selectedRange = range,
                                overview = overview
                            )
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Unable to load analytics"
                            )
                        }
                    )
                }
        }
    }

    fun selectRange(range: AnalyticsRange) {
        if (selectedRange.value != range) {
            selectedRange.value = range
        }
    }

    fun formatCurrency(amount: Double): String = currencyFormatter.format(amount)

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

data class TransactionAnalyticsUiState(
    val isLoading: Boolean = true,
    val overview: TransactionAnalyticsOverview? = null,
    val errorMessage: String? = null,
    val selectedRange: AnalyticsRange = AnalyticsRange.Last30Days
)
