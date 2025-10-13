package com.voiceledger.ghana.presentation.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voiceledger.ghana.domain.repository.DailySummaryRepository
import com.voiceledger.ghana.domain.service.PresentationState
import com.voiceledger.ghana.domain.service.SummaryPresentation
import com.voiceledger.ghana.domain.service.SummaryPresentationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the summary presentation screen
 * Manages summary loading, presentation, and voice output
 */
@HiltViewModel
class SummaryPresentationViewModel @Inject constructor(
    private val dailySummaryRepository: DailySummaryRepository,
    private val summaryPresentationService: SummaryPresentationService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SummaryPresentationUiState())
    val uiState: StateFlow<SummaryPresentationUiState> = _uiState.asStateFlow()
    
    val presentationState: StateFlow<PresentationState> = summaryPresentationService.presentationState
    
    private var currentLanguage = "en"
    
    /**
     * Load and present summary for the given date
     */
    fun loadSummary(date: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null
                )
                
                // First try to get existing summary
                var summary = dailySummaryRepository.getSummaryByDate(date)
                
                // If no summary exists, generate one
                if (summary == null) {
                    summary = dailySummaryRepository.generateDailySummary(date)
                }
                
                // Present the summary
                val result = summaryPresentationService.presentDailySummary(
                    summary = summary,
                    includeVoice = false, // Don't auto-play
                    language = currentLanguage
                )
                
                when (result) {
                    is com.voiceledger.ghana.domain.service.PresentationResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            presentation = result.presentation,
                            summary = summary,
                            error = null
                        )
                    }
                    is com.voiceledger.ghana.domain.service.PresentationResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load summary"
                )
            }
        }
    }
    
    /**
     * Set presentation language and regenerate presentation
     */
    fun setLanguage(language: String) {
        if (currentLanguage == language) return
        
        currentLanguage = language
        val summary = _uiState.value.summary
        
        if (summary != null) {
            viewModelScope.launch {
                try {
                    val result = summaryPresentationService.presentDailySummary(
                        summary = summary,
                        includeVoice = false,
                        language = language
                    )
                    
                    when (result) {
                        is com.voiceledger.ghana.domain.service.PresentationResult.Success -> {
                            _uiState.value = _uiState.value.copy(
                                presentation = result.presentation,
                                message = "Language changed to ${getLanguageName(language)}"
                            )
                        }
                        is com.voiceledger.ghana.domain.service.PresentationResult.Error -> {
                            _uiState.value = _uiState.value.copy(
                                error = "Failed to change language: ${result.message}"
                            )
                        }
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to change language: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Start speaking the summary
     */
    fun speakSummary() {
        val summary = _uiState.value.summary ?: return
        
        viewModelScope.launch {
            try {
                summaryPresentationService.presentDailySummary(
                    summary = summary,
                    includeVoice = true,
                    language = currentLanguage
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to speak summary: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Stop speaking
     */
    fun stopSpeaking() {
        summaryPresentationService.stopSpeaking()
    }
    
    /**
     * Set speech rate
     */
    fun setSpeechRate(rate: Float) {
        summaryPresentationService.setSpeechRate(rate)
    }
    
    /**
     * Set speech pitch
     */
    fun setSpeechPitch(pitch: Float) {
        summaryPresentationService.setSpeechPitch(pitch)
    }
    
    /**
     * Generate and speak quick summary
     */
    fun speakQuickSummary() {
        val summary = _uiState.value.summary ?: return
        
        viewModelScope.launch {
            try {
                val quickText = summaryPresentationService.generateQuickSummary(summary)
                // This would speak just the quick summary
                _uiState.value = _uiState.value.copy(
                    message = "Quick summary: $quickText"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to generate quick summary: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Refresh the summary by regenerating it
     */
    fun refreshSummary() {
        val summary = _uiState.value.summary ?: return
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Force regenerate the summary
                val newSummary = dailySummaryRepository.generateDailySummary(summary.date)
                
                // Present the new summary
                val result = summaryPresentationService.presentDailySummary(
                    summary = newSummary,
                    includeVoice = false,
                    language = currentLanguage
                )
                
                when (result) {
                    is com.voiceledger.ghana.domain.service.PresentationResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            presentation = result.presentation,
                            summary = newSummary,
                            message = "Summary refreshed",
                            error = null
                        )
                    }
                    is com.voiceledger.ghana.domain.service.PresentationResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to refresh summary: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Share summary text
     */
    fun shareSummary(): String? {
        val presentation = _uiState.value.presentation ?: return null
        
        return buildString {
            appendLine(presentation.title)
            appendLine("=".repeat(presentation.title.length))
            appendLine()
            
            presentation.sections.forEach { section ->
                appendLine(section.title)
                appendLine("-".repeat(section.title.length))
                appendLine(section.content)
                appendLine()
            }
            
            appendLine("Generated by Ghana Voice Ledger")
        }
    }
    
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    private fun getLanguageName(code: String): String {
        return when (code) {
            "en" -> "English"
            "tw" -> "Twi"
            "ga" -> "Ga"
            "ee" -> "Ewe"
            "dag" -> "Dagbani"
            "ha" -> "Hausa"
            else -> "Unknown"
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        summaryPresentationService.cleanup()
    }
}

/**
 * UI state for the summary presentation screen
 */
data class SummaryPresentationUiState(
    val isLoading: Boolean = false,
    val presentation: SummaryPresentation? = null,
    val summary: com.voiceledger.ghana.data.local.entity.DailySummary? = null,
    val error: String? = null,
    val message: String? = null
)