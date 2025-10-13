package com.voiceledger.ghana.presentation.tutorial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TutorialViewModel @Inject constructor(
    private val tutorialRepository: TutorialRepository,
    private val analyticsService: AnalyticsService
) : ViewModel() {

    private val _uiState = MutableStateFlow(TutorialUiState())
    val uiState: StateFlow<TutorialUiState> = _uiState.asStateFlow()

    fun startTutorial(type: TutorialType) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                currentTutorialType = type,
                isStarted = true
            )
            
            analyticsService.trackEvent("tutorial_started", mapOf(
                "tutorial_type" to type.name
            ))
        }
    }

    fun onTutorialActionComplete(action: TutorialAction) {
        viewModelScope.launch {
            val currentState = _uiState.value
            
            when (action) {
                TutorialAction.PLAY_DEMO -> {
                    analyticsService.trackEvent("tutorial_demo_played", mapOf(
                        "tutorial_type" to currentState.currentTutorialType?.name.orEmpty()
                    ))
                }
                TutorialAction.COMPLETE_STEP -> {
                    val completedSteps = currentState.completedSteps + 1
                    _uiState.value = currentState.copy(
                        completedSteps = completedSteps
                    )
                    
                    analyticsService.trackEvent("tutorial_step_completed", mapOf(
                        "tutorial_type" to currentState.currentTutorialType?.name.orEmpty(),
                        "step_number" to completedSteps.toString()
                    ))
                }
                TutorialAction.START_PRACTICE -> {
                    _uiState.value = currentState.copy(
                        isPracticing = true
                    )
                    
                    analyticsService.trackEvent("tutorial_practice_started", mapOf(
                        "tutorial_type" to currentState.currentTutorialType?.name.orEmpty()
                    ))
                }
                TutorialAction.COMPLETE_PRACTICE -> {
                    _uiState.value = currentState.copy(
                        isPracticing = false,
                        practiceCompleted = true
                    )
                    
                    analyticsService.trackEvent("tutorial_practice_completed", mapOf(
                        "tutorial_type" to currentState.currentTutorialType?.name.orEmpty()
                    ))
                }
            }
        }
    }

    fun completeTutorial() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val tutorialType = currentState.currentTutorialType
            
            if (tutorialType != null) {
                try {
                    tutorialRepository.markTutorialCompleted(tutorialType)
                    
                    _uiState.value = currentState.copy(
                        isCompleted = true
                    )
                    
                    analyticsService.trackEvent("tutorial_completed", mapOf(
                        "tutorial_type" to tutorialType.name,
                        "completed_steps" to currentState.completedSteps.toString(),
                        "practice_completed" to currentState.practiceCompleted.toString()
                    ))
                } catch (e: Exception) {
                    _uiState.value = currentState.copy(
                        error = "Failed to complete tutorial: ${e.message}"
                    )
                }
            }
        }
    }

    fun skipTutorial() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val tutorialType = currentState.currentTutorialType
            
            if (tutorialType != null) {
                tutorialRepository.markTutorialSkipped(tutorialType)
                
                analyticsService.trackEvent("tutorial_skipped", mapOf(
                    "tutorial_type" to tutorialType.name,
                    "completed_steps" to currentState.completedSteps.toString()
                ))
            }
            
            _uiState.value = currentState.copy(
                isCompleted = true
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class TutorialUiState(
    val currentTutorialType: TutorialType? = null,
    val isStarted: Boolean = false,
    val isCompleted: Boolean = false,
    val completedSteps: Int = 0,
    val isPracticing: Boolean = false,
    val practiceCompleted: Boolean = false,
    val error: String? = null
)

interface TutorialRepository {
    suspend fun markTutorialCompleted(type: TutorialType)
    suspend fun markTutorialSkipped(type: TutorialType)
    suspend fun isTutorialCompleted(type: TutorialType): Boolean
    suspend fun getTutorialProgress(type: TutorialType): TutorialProgress
}

data class TutorialProgress(
    val type: TutorialType,
    val isCompleted: Boolean,
    val isSkipped: Boolean,
    val completedSteps: Int,
    val totalSteps: Int,
    val lastAccessedDate: Long
)