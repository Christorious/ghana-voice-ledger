package com.voiceledger.ghana.presentation.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val feedbackRepository: FeedbackRepository,
    private val deviceInfoProvider: DeviceInfoProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedbackUiState())
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()

    fun onFeedbackTypeSelected(type: FeedbackType) {
        _uiState.value = _uiState.value.copy(feedbackType = type)
    }

    fun onRatingChanged(rating: Int) {
        _uiState.value = _uiState.value.copy(rating = rating)
    }

    fun onCategorySelected(category: FeedbackCategory) {
        _uiState.value = _uiState.value.copy(category = category)
    }

    fun onDescriptionChanged(description: String) {
        if (description.length <= 500) {
            _uiState.value = _uiState.value.copy(description = description)
        }
    }

    fun onEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    fun onIncludeDeviceInfoChanged(include: Boolean) {
        _uiState.value = _uiState.value.copy(includeDeviceInfo = include)
    }

    fun submitFeedback() {
        val currentState = _uiState.value
        
        if (!currentState.canSubmit) {
            _uiState.value = currentState.copy(
                error = "Please fill in all required fields"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(
                isLoading = true,
                error = null
            )

            try {
                val deviceInfo = if (currentState.includeDeviceInfo) {
                    deviceInfoProvider.getDeviceInfo()
                } else {
                    null
                }

                val feedback = Feedback(
                    type = currentState.feedbackType,
                    rating = currentState.rating,
                    category = currentState.category,
                    description = currentState.description,
                    email = currentState.email.takeIf { it.isNotBlank() },
                    deviceInfo = deviceInfo,
                    timestamp = System.currentTimeMillis()
                )

                feedbackRepository.submitFeedback(feedback)

                _uiState.value = currentState.copy(
                    isLoading = false,
                    isSubmitted = true
                )
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    isLoading = false,
                    error = "Failed to submit feedback: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class FeedbackUiState(
    val feedbackType: FeedbackType = FeedbackType.GENERAL_FEEDBACK,
    val rating: Int = 0,
    val category: FeedbackCategory = FeedbackCategory.OTHER,
    val description: String = "",
    val email: String = "",
    val includeDeviceInfo: Boolean = true,
    val isLoading: Boolean = false,
    val isSubmitted: Boolean = false,
    val error: String? = null
) {
    val canSubmit: Boolean
        get() = description.isNotBlank() && rating > 0
}

data class Feedback(
    val type: FeedbackType,
    val rating: Int,
    val category: FeedbackCategory,
    val description: String,
    val email: String?,
    val deviceInfo: DeviceInfo?,
    val timestamp: Long
)

data class DeviceInfo(
    val deviceModel: String,
    val androidVersion: String,
    val appVersion: String,
    val buildType: String,
    val locale: String,
    val screenDensity: String,
    val availableMemory: Long,
    val totalStorage: Long,
    val batteryLevel: Int?
)

interface FeedbackRepository {
    suspend fun submitFeedback(feedback: Feedback)
    suspend fun getFeedbackHistory(): List<Feedback>
}

interface DeviceInfoProvider {
    suspend fun getDeviceInfo(): DeviceInfo
}