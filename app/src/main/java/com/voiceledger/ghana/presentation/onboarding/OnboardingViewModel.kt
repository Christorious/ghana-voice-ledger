package com.voiceledger.ghana.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
    private val permissionManager: PermissionManager,
    private val voiceEnrollmentManager: VoiceEnrollmentManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        checkOnboardingStatus()
    }

    private fun checkOnboardingStatus() {
        viewModelScope.launch {
            val isComplete = onboardingRepository.isOnboardingComplete()
            _uiState.value = _uiState.value.copy(
                isOnboardingComplete = isComplete
            )
        }
    }

    fun onPermissionGranted(permission: String) {
        viewModelScope.launch {
            val granted = permissionManager.requestPermission(permission)
            if (granted) {
                updatePermissionStatus()
            }
        }
    }

    private fun updatePermissionStatus() {
        viewModelScope.launch {
            val hasAudio = permissionManager.hasPermission("android.permission.RECORD_AUDIO")
            val hasStorage = permissionManager.hasPermission("android.permission.WRITE_EXTERNAL_STORAGE")
            
            _uiState.value = _uiState.value.copy(
                hasRequiredPermissions = hasAudio && hasStorage
            )
        }
    }

    fun onVoiceEnrollmentComplete() {
        viewModelScope.launch {
            try {
                voiceEnrollmentManager.completeEnrollment()
                _uiState.value = _uiState.value.copy(
                    isVoiceEnrolled = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Voice enrollment failed: ${e.message}"
                )
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            try {
                onboardingRepository.setOnboardingComplete(true)
                _uiState.value = _uiState.value.copy(
                    isOnboardingComplete = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to complete onboarding: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class OnboardingUiState(
    val isOnboardingComplete: Boolean = false,
    val hasRequiredPermissions: Boolean = false,
    val isVoiceEnrolled: Boolean = false,
    val error: String? = null,
    val isLoading: Boolean = false
)

// Repository interface for onboarding data
interface OnboardingRepository {
    suspend fun isOnboardingComplete(): Boolean
    suspend fun setOnboardingComplete(complete: Boolean)
}

// Permission manager interface
interface PermissionManager {
    suspend fun hasPermission(permission: String): Boolean
    suspend fun requestPermission(permission: String): Boolean
}

// Voice enrollment manager interface
interface VoiceEnrollmentManager {
    suspend fun startEnrollment()
    suspend fun completeEnrollment()
    suspend fun isEnrollmentComplete(): Boolean
}