package com.voiceledger.ghana.service

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager as AndroidPowerManager
import android.os.PowerManager.WakeLock
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Power management service for optimizing battery usage
 * Handles smart sleep, CPU throttling, and market hours enforcement
 */
@Singleton
class PowerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val androidPowerManager = context.getSystemService(Context.POWER_SERVICE) as AndroidPowerManager
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    
    private var wakeLock: WakeLock? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val _powerState = MutableStateFlow(PowerState())
    val powerState: StateFlow<PowerState> = _powerState.asStateFlow()
    
    // Configuration
    private var marketStartHour = 6 // 6 AM
    private var marketEndHour = 18 // 6 PM
    private var lowBatteryThreshold = 20 // 20%
    private var criticalBatteryThreshold = 10 // 10%
    private var powerSavingEnabled = true
    
    init {
        startBatteryMonitoring()
        startMarketHoursMonitoring()
    }
    
    /**
     * Start battery level monitoring
     */
    private fun startBatteryMonitoring() {
        scope.launch {
            while (isActive) {
                updateBatteryStatus()
                delay(30_000) // Check every 30 seconds
            }
        }
    }
    
    /**
     * Start market hours monitoring
     */
    private fun startMarketHoursMonitoring() {
        scope.launch {
            while (isActive) {
                updateMarketStatus()
                delay(60_000) // Check every minute
            }
        }
    }
    
    /**
     * Update battery status and apply power optimizations
     */
    private fun updateBatteryStatus() {
        val batteryLevel = getBatteryLevel()
        val isCharging = isCharging()
        val batteryStatus = getBatteryStatus()
        
        val currentState = _powerState.value
        val newPowerMode = determinePowerMode(batteryLevel, isCharging, currentState.marketStatus)
        
        _powerState.value = currentState.copy(
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            batteryStatus = batteryStatus,
            powerMode = newPowerMode,
            lastBatteryCheck = System.currentTimeMillis()
        )
        
        // Apply power optimizations based on new mode
        applyPowerOptimizations(newPowerMode)
    }
    
    /**
     * Update market status based on current time
     */
    private fun updateMarketStatus() {
        val currentTime = LocalTime.now()
        val marketStart = LocalTime.of(marketStartHour, 0)
        val marketEnd = LocalTime.of(marketEndHour, 0)
        
        val marketStatus = when {
            currentTime.isBefore(marketStart) -> MarketStatus.BEFORE_HOURS
            currentTime.isAfter(marketEnd) -> MarketStatus.AFTER_HOURS
            else -> MarketStatus.OPEN
        }
        
        val currentState = _powerState.value
        if (currentState.marketStatus != marketStatus) {
            _powerState.value = currentState.copy(
                marketStatus = marketStatus,
                lastMarketCheck = System.currentTimeMillis()
            )
            
            // Reapply power optimizations when market status changes
            applyPowerOptimizations(currentState.powerMode)
        }
    }
    
    /**
     * Determine appropriate power mode based on battery and market conditions
     */
    private fun determinePowerMode(
        batteryLevel: Int,
        isCharging: Boolean,
        marketStatus: MarketStatus
    ): PowerMode {
        return when {
            // Critical battery - maximum power saving
            batteryLevel <= criticalBatteryThreshold && !isCharging -> PowerMode.CRITICAL_SAVE
            
            // Low battery - aggressive power saving
            batteryLevel <= lowBatteryThreshold && !isCharging -> PowerMode.POWER_SAVE
            
            // Market closed - sleep mode
            marketStatus != MarketStatus.OPEN && powerSavingEnabled -> PowerMode.SLEEP
            
            // Charging - normal operation
            isCharging -> PowerMode.NORMAL
            
            // Normal battery during market hours
            batteryLevel > lowBatteryThreshold && marketStatus == MarketStatus.OPEN -> PowerMode.NORMAL
            
            // Default to power save
            else -> PowerMode.POWER_SAVE
        }
    }
    
    /**
     * Apply power optimizations based on power mode
     */
    private fun applyPowerOptimizations(powerMode: PowerMode) {
        when (powerMode) {
            PowerMode.NORMAL -> {
                enableNormalOperation()
            }
            PowerMode.POWER_SAVE -> {
                enablePowerSaveMode()
            }
            PowerMode.CRITICAL_SAVE -> {
                enableCriticalSaveMode()
            }
            PowerMode.SLEEP -> {
                enableSleepMode()
            }
        }
    }
    
    /**
     * Enable normal operation mode
     */
    private fun enableNormalOperation() {
        acquireWakeLock(WakeLockType.PARTIAL)
        // Normal CPU usage, all features enabled
        _powerState.value = _powerState.value.copy(
            cpuThrottleLevel = 0,
            audioProcessingEnabled = true,
            backgroundSyncEnabled = true,
            wakeLockActive = true
        )
    }
    
    /**
     * Enable power save mode
     */
    private fun enablePowerSaveMode() {
        acquireWakeLock(WakeLockType.PARTIAL)
        // Reduced CPU usage, limited background activity
        _powerState.value = _powerState.value.copy(
            cpuThrottleLevel = 1,
            audioProcessingEnabled = true,
            backgroundSyncEnabled = false,
            wakeLockActive = true
        )
    }
    
    /**
     * Enable critical save mode
     */
    private fun enableCriticalSaveMode() {
        releaseWakeLock()
        // Minimal CPU usage, essential features only
        _powerState.value = _powerState.value.copy(
            cpuThrottleLevel = 2,
            audioProcessingEnabled = true, // Keep core functionality
            backgroundSyncEnabled = false,
            wakeLockActive = false
        )
    }
    
    /**
     * Enable sleep mode (market closed)
     */
    private fun enableSleepMode() {
        releaseWakeLock()
        // Minimal power usage, periodic wake-ups only
        _powerState.value = _powerState.value.copy(
            cpuThrottleLevel = 3,
            audioProcessingEnabled = false,
            backgroundSyncEnabled = false,
            wakeLockActive = false
        )
    }
    
    /**
     * Acquire wake lock to prevent device sleep
     */
    private fun acquireWakeLock(type: WakeLockType) {
        releaseWakeLock() // Release any existing wake lock
        
        val wakeLockFlag = when (type) {
            WakeLockType.PARTIAL -> AndroidPowerManager.PARTIAL_WAKE_LOCK
            WakeLockType.SCREEN_DIM -> AndroidPowerManager.SCREEN_DIM_WAKE_LOCK
            WakeLockType.SCREEN_BRIGHT -> AndroidPowerManager.SCREEN_BRIGHT_WAKE_LOCK
        }
        
        wakeLock = androidPowerManager.newWakeLock(
            wakeLockFlag,
            \"VoiceLedger::PowerManager\"
        ).apply {
            acquire(10 * 60 * 1000L) // 10 minutes timeout
        }
    }
    
    /**
     * Release wake lock
     */
    private fun releaseWakeLock() {
        wakeLock?.let { lock ->
            if (lock.isHeld) {
                lock.release()
            }
        }
        wakeLock = null
    }
    
    /**
     * Get current battery level percentage
     */
    private fun getBatteryLevel(): Int {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        
        return if (level != -1 && scale != -1) {
            (level * 100 / scale.toFloat()).toInt()
        } else {
            100 // Default to full battery if unable to read
        }
    }
    
    /**
     * Check if device is currently charging
     */
    private fun isCharging(): Boolean {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
               status == BatteryManager.BATTERY_STATUS_FULL
    }
    
    /**
     * Get detailed battery status
     */
    private fun getBatteryStatus(): BatteryStatus {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val health = batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val temperature = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        
        return BatteryStatus(
            status = when (status) {
                BatteryManager.BATTERY_STATUS_CHARGING -> \"Charging\"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> \"Discharging\"
                BatteryManager.BATTERY_STATUS_FULL -> \"Full\"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> \"Not Charging\"
                else -> \"Unknown\"
            },
            health = when (health) {
                BatteryManager.BATTERY_HEALTH_GOOD -> \"Good\"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> \"Overheating\"
                BatteryManager.BATTERY_HEALTH_DEAD -> \"Dead\"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> \"Over Voltage\"
                BatteryManager.BATTERY_HEALTH_COLD -> \"Cold\"
                else -> \"Unknown\"
            },
            temperature = temperature / 10.0f // Convert from tenths of degree Celsius
        )
    }
    
    /**
     * Configure market hours
     */
    fun setMarketHours(startHour: Int, endHour: Int) {
        marketStartHour = startHour.coerceIn(0, 23)
        marketEndHour = endHour.coerceIn(0, 23)
        updateMarketStatus()
    }
    
    /**
     * Configure battery thresholds
     */
    fun setBatteryThresholds(lowThreshold: Int, criticalThreshold: Int) {
        lowBatteryThreshold = lowThreshold.coerceIn(5, 50)
        criticalBatteryThreshold = criticalThreshold.coerceIn(1, lowBatteryThreshold - 1)
        updateBatteryStatus()
    }
    
    /**
     * Enable or disable power saving features
     */
    fun setPowerSavingEnabled(enabled: Boolean) {
        powerSavingEnabled = enabled
        updateBatteryStatus()
    }
    
    /**
     * Force a specific power mode (for testing or manual override)
     */
    fun forcePowerMode(mode: PowerMode) {
        applyPowerOptimizations(mode)
    }
    
    /**
     * Get current market hours as formatted string
     */
    fun getMarketHoursString(): String {
        val startTime = LocalTime.of(marketStartHour, 0)
        val endTime = LocalTime.of(marketEndHour, 0)
        val formatter = DateTimeFormatter.ofPattern(\"h:mm a\")
        return \"${startTime.format(formatter)} - ${endTime.format(formatter)}\"
    }
    
    /**
     * Check if current time is within market hours
     */
    fun isMarketOpen(): Boolean {
        return _powerState.value.marketStatus == MarketStatus.OPEN
    }
    
    /**
     * Get estimated battery life in hours based on current usage
     */
    fun getEstimatedBatteryLife(): Float {
        val currentLevel = _powerState.value.batteryLevel
        val powerMode = _powerState.value.powerMode
        
        // Rough estimates based on power mode (in hours)
        val baseConsumptionRate = when (powerMode) {
            PowerMode.NORMAL -> 0.8f // 80% per hour
            PowerMode.POWER_SAVE -> 0.5f // 50% per hour
            PowerMode.CRITICAL_SAVE -> 0.3f // 30% per hour
            PowerMode.SLEEP -> 0.1f // 10% per hour
        }
        
        return if (baseConsumptionRate > 0) {
            currentLevel / baseConsumptionRate
        } else {
            Float.MAX_VALUE
        }
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        releaseWakeLock()
        // Cancel any pending work
        WorkManager.getInstance(context).cancelAllWorkByTag(\"PowerManager\")
    }
}

/**
 * Power management state
 */
data class PowerState(
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false,
    val batteryStatus: BatteryStatus = BatteryStatus(),
    val powerMode: PowerMode = PowerMode.NORMAL,
    val marketStatus: MarketStatus = MarketStatus.OPEN,
    val cpuThrottleLevel: Int = 0,
    val audioProcessingEnabled: Boolean = true,
    val backgroundSyncEnabled: Boolean = true,
    val wakeLockActive: Boolean = false,
    val lastBatteryCheck: Long = 0L,
    val lastMarketCheck: Long = 0L
)

/**
 * Battery status details
 */
data class BatteryStatus(
    val status: String = \"Unknown\",
    val health: String = \"Unknown\",
    val temperature: Float = 0f
)

/**
 * Power management modes
 */
enum class PowerMode {
    NORMAL,      // Full functionality
    POWER_SAVE,  // Reduced background activity
    CRITICAL_SAVE, // Minimal functionality
    SLEEP        // Market closed, minimal power usage
}

/**
 * Market operating status
 */
enum class MarketStatus {
    BEFORE_HOURS, // Before market opens
    OPEN,         // Market is open
    AFTER_HOURS   // After market closes
}

/**
 * Wake lock types
 */
enum class WakeLockType {
    PARTIAL,       // CPU stays on, screen can turn off
    SCREEN_DIM,    // Screen stays on but dimmed
    SCREEN_BRIGHT  // Screen stays on at full brightness
}