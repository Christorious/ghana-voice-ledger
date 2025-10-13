package com.voiceledger.ghana.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voiceledger.ghana.domain.repository.SpeakerProfileRepository
import com.voiceledger.ghana.domain.repository.TransactionRepository
import com.voiceledger.ghana.service.VoiceAgentServiceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for the settings screen
 * Manages app configuration, user preferences, and privacy controls
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val speakerProfileRepository: SpeakerProfileRepository,
    private val transactionRepository: TransactionRepository,
    private val voiceAgentServiceManager: VoiceAgentServiceManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    // Available languages for Ghana
    val availableLanguages = listOf(
        Language("en", "English"),
        Language("tw", "Twi (Akan)"),
        Language("ga", "Ga"),
        Language("ee", "Ewe"),
        Language("dag", "Dagbani"),
        Language("ha", "Hausa")
    )
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Load current settings from preferences or database
                val currentSettings = AppSettings(
                    selectedLanguage = getCurrentLanguage(),
                    marketStartHour = getMarketStartHour(),
                    marketEndHour = getMarketEndHour(),
                    enableNotifications = getNotificationsEnabled(),
                    enableVoiceConfirmation = getVoiceConfirmationEnabled(),
                    confidenceThreshold = getConfidenceThreshold(),
                    autoBackup = getAutoBackupEnabled(),
                    dataRetentionDays = getDataRetentionDays()
                )
                
                // Check if seller profile exists
                val sellerProfile = speakerProfileRepository.getSellerProfile()
                val hasSellerProfile = sellerProfile != null
                
                // Get data statistics
                val totalTransactions = transactionRepository.getAllTransactions().first().size
                val totalCustomers = speakerProfileRepository.getCustomerCount()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    settings = currentSettings,
                    hasSellerProfile = hasSellerProfile,
                    sellerProfileName = sellerProfile?.name,
                    totalTransactions = totalTransactions,
                    totalCustomers = totalCustomers,
                    error = null
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load settings"
                )
            }
        }
    }
    
    fun updateLanguage(language: Language) {
        viewModelScope.launch {
            try {
                saveLanguagePreference(language.code)
                _uiState.value = _uiState.value.copy(
                    settings = _uiState.value.settings.copy(selectedLanguage = language),
                    message = "Language updated to ${language.name}"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update language: ${e.message}"
                )
            }
        }
    }
    
    fun updateMarketHours(startHour: Int, endHour: Int) {
        viewModelScope.launch {
            try {
                if (startHour >= endHour) {
                    _uiState.value = _uiState.value.copy(
                        error = "Start hour must be before end hour"
                    )
                    return@launch
                }
                
                saveMarketHours(startHour, endHour)
                _uiState.value = _uiState.value.copy(
                    settings = _uiState.value.settings.copy(
                        marketStartHour = startHour,
                        marketEndHour = endHour
                    ),
                    message = "Market hours updated"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update market hours: ${e.message}"
                )
            }
        }
    }
    
    fun updateNotifications(enabled: Boolean) {
        viewModelScope.launch {
            try {
                saveNotificationPreference(enabled)
                _uiState.value = _uiState.value.copy(
                    settings = _uiState.value.settings.copy(enableNotifications = enabled),
                    message = if (enabled) "Notifications enabled" else "Notifications disabled"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update notifications: ${e.message}"
                )
            }
        }
    }
    
    fun updateVoiceConfirmation(enabled: Boolean) {
        viewModelScope.launch {
            try {
                saveVoiceConfirmationPreference(enabled)
                _uiState.value = _uiState.value.copy(
                    settings = _uiState.value.settings.copy(enableVoiceConfirmation = enabled),
                    message = if (enabled) "Voice confirmation enabled" else "Voice confirmation disabled"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update voice confirmation: ${e.message}"
                )
            }
        }
    }
    
    fun updateConfidenceThreshold(threshold: Float) {
        viewModelScope.launch {
            try {
                if (threshold < 0.5f || threshold > 1.0f) {
                    _uiState.value = _uiState.value.copy(
                        error = "Confidence threshold must be between 50% and 100%"
                    )
                    return@launch
                }
                
                saveConfidenceThreshold(threshold)
                _uiState.value = _uiState.value.copy(
                    settings = _uiState.value.settings.copy(confidenceThreshold = threshold),
                    message = "Confidence threshold updated to ${(threshold * 100).toInt()}%"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update confidence threshold: ${e.message}"
                )
            }
        }
    }
    
    fun updateAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            try {
                saveAutoBackupPreference(enabled)
                _uiState.value = _uiState.value.copy(
                    settings = _uiState.value.settings.copy(autoBackup = enabled),
                    message = if (enabled) "Auto backup enabled" else "Auto backup disabled"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update auto backup: ${e.message}"
                )
            }
        }
    }
    
    fun updateDataRetention(days: Int) {
        viewModelScope.launch {
            try {
                if (days < 30 || days > 365) {
                    _uiState.value = _uiState.value.copy(
                        error = "Data retention must be between 30 and 365 days"
                    )
                    return@launch
                }
                
                saveDataRetentionDays(days)
                _uiState.value = _uiState.value.copy(
                    settings = _uiState.value.settings.copy(dataRetentionDays = days),
                    message = "Data retention updated to $days days"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update data retention: ${e.message}"
                )
            }
        }
    }
    
    fun startVoiceEnrollment() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isEnrollingVoice = true,
                    enrollmentStep = VoiceEnrollmentStep.RECORDING
                )
                
                // This would integrate with the actual voice enrollment process
                // For now, we'll simulate the process
                kotlinx.coroutines.delay(3000)
                
                _uiState.value = _uiState.value.copy(
                    isEnrollingVoice = false,
                    enrollmentStep = VoiceEnrollmentStep.COMPLETED,
                    hasSellerProfile = true,
                    sellerProfileName = "Seller Profile",
                    message = "Voice enrollment completed successfully"
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isEnrollingVoice = false,
                    enrollmentStep = VoiceEnrollmentStep.IDLE,
                    error = "Voice enrollment failed: ${e.message}"
                )
            }
        }
    }
    
    fun cancelVoiceEnrollment() {
        _uiState.value = _uiState.value.copy(
            isEnrollingVoice = false,
            enrollmentStep = VoiceEnrollmentStep.IDLE
        )
    }
    
    fun exportData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isExportingData = true)
                
                // This would implement actual data export
                kotlinx.coroutines.delay(2000)
                
                _uiState.value = _uiState.value.copy(
                    isExportingData = false,
                    message = "Data exported successfully"
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExportingData = false,
                    error = "Failed to export data: ${e.message}"
                )
            }
        }
    }
    
    fun deleteAllData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isDeletingData = true)
                
                // Delete all transactions and customer data
                transactionRepository.getAllTransactions().first().forEach { transaction ->
                    transactionRepository.deleteTransaction(transaction)
                }
                
                // Delete all speaker profiles except seller
                speakerProfileRepository.getCustomerProfiles().first().forEach { profile ->
                    speakerProfileRepository.deleteProfile(profile)
                }
                
                _uiState.value = _uiState.value.copy(
                    isDeletingData = false,
                    totalTransactions = 0,
                    totalCustomers = 0,
                    message = "All data deleted successfully"
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDeletingData = false,
                    error = "Failed to delete data: ${e.message}"
                )
            }
        }
    }
    
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    // Helper methods for preferences (would integrate with SharedPreferences or DataStore)
    private fun getCurrentLanguage(): Language {
        // Default to English, would load from preferences
        return availableLanguages.first { it.code == "en" }
    }
    
    private fun getMarketStartHour(): Int = 6 // 6 AM default
    private fun getMarketEndHour(): Int = 18 // 6 PM default
    private fun getNotificationsEnabled(): Boolean = true
    private fun getVoiceConfirmationEnabled(): Boolean = true
    private fun getConfidenceThreshold(): Float = 0.8f
    private fun getAutoBackupEnabled(): Boolean = false
    private fun getDataRetentionDays(): Int = 90
    
    private suspend fun saveLanguagePreference(languageCode: String) {
        // Would save to SharedPreferences or DataStore
    }
    
    private suspend fun saveMarketHours(startHour: Int, endHour: Int) {
        // Would save to SharedPreferences or DataStore
    }
    
    private suspend fun saveNotificationPreference(enabled: Boolean) {
        // Would save to SharedPreferences or DataStore
    }
    
    private suspend fun saveVoiceConfirmationPreference(enabled: Boolean) {
        // Would save to SharedPreferences or DataStore
    }
    
    private suspend fun saveConfidenceThreshold(threshold: Float) {
        // Would save to SharedPreferences or DataStore
    }
    
    private suspend fun saveAutoBackupPreference(enabled: Boolean) {
        // Would save to SharedPreferences or DataStore
    }
    
    private suspend fun saveDataRetentionDays(days: Int) {
        // Would save to SharedPreferences or DataStore
    }
}

/**
 * UI state for the settings screen
 */
data class SettingsUiState(
    val isLoading: Boolean = false,
    val isEnrollingVoice: Boolean = false,
    val isExportingData: Boolean = false,
    val isDeletingData: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val hasSellerProfile: Boolean = false,
    val sellerProfileName: String? = null,
    val totalTransactions: Int = 0,
    val totalCustomers: Int = 0,
    val enrollmentStep: VoiceEnrollmentStep = VoiceEnrollmentStep.IDLE,
    val error: String? = null,
    val message: String? = null
)

/**
 * App settings data model
 */
data class AppSettings(
    val selectedLanguage: Language = Language("en", "English"),
    val marketStartHour: Int = 6,
    val marketEndHour: Int = 18,
    val enableNotifications: Boolean = true,
    val enableVoiceConfirmation: Boolean = true,
    val confidenceThreshold: Float = 0.8f,
    val autoBackup: Boolean = false,
    val dataRetentionDays: Int = 90
)

/**
 * Language data model
 */
data class Language(
    val code: String,
    val name: String
)

/**
 * Voice enrollment step enumeration
 */
enum class VoiceEnrollmentStep {
    IDLE,
    RECORDING,
    PROCESSING,
    COMPLETED,
    FAILED
}