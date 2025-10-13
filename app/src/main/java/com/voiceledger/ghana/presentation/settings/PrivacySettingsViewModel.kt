package com.voiceledger.ghana.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voiceledger.ghana.security.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for privacy settings screen
 */
@HiltViewModel
class PrivacySettingsViewModel @Inject constructor(
    private val privacyManager: PrivacyManager,
    private val securityManager: SecurityManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PrivacySettingsUiState())
    val uiState: StateFlow<PrivacySettingsUiState> = _uiState.asStateFlow()
    
    init {
        observePrivacyState()
        loadComplianceReport()
    }
    
    /**
     * Observe privacy state changes
     */
    private fun observePrivacyState() {
        viewModelScope.launch {
            combine(
                privacyManager.privacyState,
                privacyManager.consentState
            ) { privacyState, consentState ->
                _uiState.value = _uiState.value.copy(
                    privacySettings = privacyState.settings,
                    consentState = consentState,
                    isLoading = false
                )
            }.collect()
        }
    }
    
    /**
     * Update privacy settings
     */
    fun updatePrivacySettings(settings: PrivacySettings) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                privacyManager.updatePrivacySettings(settings)
                loadComplianceReport() // Refresh compliance after settings change
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to update privacy settings: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Update user consent
     */
    fun updateConsent() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val consent = UserConsent(
                    voiceProcessingConsent = true,
                    dataStorageConsent = true,
                    analyticsConsent = _uiState.value.privacySettings.allowAnalytics,
                    cloudSyncConsent = _uiState.value.privacySettings.allowCloudSync,
                    speakerIdentificationConsent = _uiState.value.privacySettings.allowSpeakerIdentification,
                    consentDate = System.currentTimeMillis(),
                    expiryDate = System.currentTimeMillis() + (365 * 24 * 60 * 60 * 1000L) // 1 year
                )
                
                privacyManager.recordConsent(consent)
                loadComplianceReport()
                
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to update consent: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Revoke user consent
     */
    fun revokeConsent() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                privacyManager.revokeConsent()
                loadComplianceReport()
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to revoke consent: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Load privacy compliance report
     */
    fun refreshComplianceReport() {
        loadComplianceReport()
    }
    
    private fun loadComplianceReport() {
        viewModelScope.launch {
            try {
                val report = privacyManager.getPrivacyComplianceReport()
                _uiState.value = _uiState.value.copy(complianceReport = report)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to load compliance report: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Export user data
     */
    fun exportUserData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val exportData = privacyManager.exportUserData()
                
                // In a real implementation, this would save to file or share
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Data export completed successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to export data: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Show delete confirmation dialog
     */
    fun showDeleteConfirmation() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmation = true)
    }
    
    /**
     * Hide delete confirmation dialog
     */
    fun hideDeleteConfirmation() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmation = false)
    }
    
    /**
     * Delete all user data
     */
    fun deleteAllUserData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    showDeleteConfirmation = false
                )
                
                val result = privacyManager.deleteAllUserData()
                
                if (result.success) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "All data deleted successfully"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to delete data: ${result.error}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to delete data: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    /**
     * Clear success message
     */
    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
    
    /**
     * Check if specific data processing is allowed
     */
    fun isDataProcessingAllowed(purpose: DataProcessingPurpose): Boolean {
        return privacyManager.isDataProcessingAllowed(purpose)
    }
    
    /**
     * Get data retention period for specific data type
     */
    fun getDataRetentionPeriod(dataType: DataType): Long {
        return privacyManager.getDataRetentionPeriod(dataType)
    }
    
    /**
     * Check if data should be deleted based on retention policy
     */
    fun shouldDeleteData(dataType: DataType, creationTime: Long): Boolean {
        return privacyManager.shouldDeleteData(dataType, creationTime)
    }
}

/**
 * UI state for privacy settings screen
 */
data class PrivacySettingsUiState(
    val privacySettings: PrivacySettings = PrivacySettings(),
    val consentState: ConsentState = ConsentState(),
    val complianceReport: PrivacyComplianceReport? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showDeleteConfirmation: Boolean = false
)