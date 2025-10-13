package com.voiceledger.ghana.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voiceledger.ghana.service.PowerManager
import com.voiceledger.ghana.service.PowerMode
import com.voiceledger.ghana.service.PowerOptimizationService
import com.voiceledger.ghana.service.MarketStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for power settings screen
 * Manages power management configuration and status
 */
@HiltViewModel
class PowerSettingsViewModel @Inject constructor(
    private val powerManager: PowerManager,
    private val powerOptimizationService: PowerOptimizationService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PowerSettingsUiState())
    val uiState: StateFlow<PowerSettingsUiState> = _uiState.asStateFlow()
    
    init {
        observePowerState()
        loadCurrentSettings()
    }
    
    /**
     * Observe power state changes
     */
    private fun observePowerState() {
        viewModelScope.launch {
            powerManager.powerState.collect { powerState ->
                val optimizationStatus = powerOptimizationService.getOptimizationStatus()
                
                _uiState.value = _uiState.value.copy(
                    currentPowerMode = powerState.powerMode,
                    batteryLevel = powerState.batteryLevel,
                    isCharging = powerState.isCharging,
                    marketStatus = powerState.marketStatus,
                    estimatedBatteryLife = optimizationStatus.estimatedBatteryLife
                )
            }
        }
    }
    
    /**
     * Load current power management settings
     */
    private fun loadCurrentSettings() {
        viewModelScope.launch {
            // These would typically be loaded from SharedPreferences or a settings repository
            _uiState.value = _uiState.value.copy(
                marketStartHour = 6,
                marketEndHour = 18,
                lowBatteryThreshold = 20,
                criticalBatteryThreshold = 10,
                powerSavingEnabled = true
            )
        }
    }
    
    /**
     * Set market start hour
     */
    fun setMarketStartHour(hour: Int) {
        val currentState = _uiState.value
        val newStartHour = hour.coerceIn(0, 23)
        
        if (newStartHour != currentState.marketStartHour) {
            _uiState.value = currentState.copy(marketStartHour = newStartHour)
            
            viewModelScope.launch {
                powerManager.setMarketHours(newStartHour, currentState.marketEndHour)
                saveSettings()
            }
        }
    }
    
    /**
     * Set market end hour
     */
    fun setMarketEndHour(hour: Int) {
        val currentState = _uiState.value
        val newEndHour = hour.coerceIn(0, 23)
        
        if (newEndHour != currentState.marketEndHour) {
            _uiState.value = currentState.copy(marketEndHour = newEndHour)
            
            viewModelScope.launch {
                powerManager.setMarketHours(currentState.marketStartHour, newEndHour)
                saveSettings()
            }
        }
    }
    
    /**
     * Set low battery threshold
     */
    fun setLowBatteryThreshold(threshold: Int) {
        val currentState = _uiState.value
        val newThreshold = threshold.coerceIn(10, 50)
        
        if (newThreshold != currentState.lowBatteryThreshold) {
            // Ensure critical threshold is always lower than low threshold
            val newCriticalThreshold = if (currentState.criticalBatteryThreshold >= newThreshold) {
                (newThreshold - 1).coerceAtLeast(1)
            } else {
                currentState.criticalBatteryThreshold
            }
            
            _uiState.value = currentState.copy(
                lowBatteryThreshold = newThreshold,
                criticalBatteryThreshold = newCriticalThreshold
            )
            
            viewModelScope.launch {
                powerManager.setBatteryThresholds(newThreshold, newCriticalThreshold)
                saveSettings()
            }
        }
    }
    
    /**
     * Set critical battery threshold
     */
    fun setCriticalBatteryThreshold(threshold: Int) {
        val currentState = _uiState.value
        val maxThreshold = (currentState.lowBatteryThreshold - 1).coerceAtLeast(1)
        val newThreshold = threshold.coerceIn(1, maxThreshold)
        
        if (newThreshold != currentState.criticalBatteryThreshold) {
            _uiState.value = currentState.copy(criticalBatteryThreshold = newThreshold)
            
            viewModelScope.launch {
                powerManager.setBatteryThresholds(currentState.lowBatteryThreshold, newThreshold)
                saveSettings()
            }
        }
    }
    
    /**
     * Enable or disable power saving
     */
    fun setPowerSavingEnabled(enabled: Boolean) {
        val currentState = _uiState.value
        
        if (enabled != currentState.powerSavingEnabled) {
            _uiState.value = currentState.copy(powerSavingEnabled = enabled)
            
            viewModelScope.launch {
                powerManager.setPowerSavingEnabled(enabled)
                saveSettings()
            }
        }
    }
    
    /**
     * Manually set power mode (temporary override)
     */
    fun setManualPowerMode(mode: PowerMode) {
        viewModelScope.launch {
            powerOptimizationService.setOptimizationLevel(
                when (mode) {
                    PowerMode.NORMAL -> 0
                    PowerMode.POWER_SAVE -> 1
                    PowerMode.CRITICAL_SAVE -> 2
                    PowerMode.SLEEP -> 3
                }
            )
        }
    }
    
    /**
     * Reset to automatic power management
     */
    fun resetToAutomatic() {
        viewModelScope.launch {
            // This would reset any manual overrides and return to automatic mode
            powerManager.setPowerSavingEnabled(true)
        }
    }
    
    /**
     * Get market hours as formatted string
     */
    fun getMarketHoursString(): String {
        return powerManager.getMarketHoursString()
    }
    
    /**
     * Save settings to persistent storage
     */
    private suspend fun saveSettings() {
        // This would save settings to SharedPreferences or a settings repository
        // For now, we'll just update the power manager
    }
    
    /**
     * Get power optimization tips based on current state
     */
    fun getPowerOptimizationTips(): List<String> {
        val currentState = _uiState.value
        val tips = mutableListOf<String>()
        
        when (currentState.currentPowerMode) {
            PowerMode.NORMAL -> {
                if (currentState.batteryLevel < 50 && !currentState.isCharging) {
                    tips.add("Consider enabling power save mode to extend battery life")
                }
            }
            PowerMode.POWER_SAVE -> {
                tips.add("Background sync is disabled to save battery")
                tips.add("Audio processing interval is increased")
            }
            PowerMode.CRITICAL_SAVE -> {
                tips.add("Only essential features are active")
                tips.add("Consider charging your device soon")
            }
            PowerMode.SLEEP -> {
                tips.add("Market is closed - minimal power usage active")
                tips.add("Service will resume automatically when market opens")
            }
        }
        
        if (currentState.marketStatus != MarketStatus.OPEN) {
            tips.add("Adjust market hours if your trading schedule has changed")
        }
        
        return tips
    }
}

/**
 * UI state for power settings screen
 */
data class PowerSettingsUiState(
    val currentPowerMode: PowerMode = PowerMode.NORMAL,
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false,
    val marketStatus: MarketStatus = MarketStatus.OPEN,
    val estimatedBatteryLife: Float = 24f,
    val marketStartHour: Int = 6,
    val marketEndHour: Int = 18,
    val lowBatteryThreshold: Int = 20,
    val criticalBatteryThreshold: Int = 10,
    val powerSavingEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null
)