package com.voiceledger.ghana.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.voiceledger.ghana.R
import com.voiceledger.ghana.presentation.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service that applies power optimizations based on PowerManager state
 * Coordinates with VoiceAgentService to optimize battery usage
 */
@Singleton
class PowerOptimizationService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val powerManager: PowerManager,
    private val voiceAgentServiceManager: VoiceAgentServiceManager
) {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    private var currentOptimizationLevel = 0
    private var isOptimizationActive = false
    
    companion object {
        private const val NOTIFICATION_CHANNEL_ID = \"power_optimization\"
        private const val NOTIFICATION_ID = 2001
        private const val BATTERY_CHECK_WORK_TAG = \"battery_check\"
    }
    
    init {
        createNotificationChannel()
        startPowerOptimizationMonitoring()
        scheduleBatteryCheckWork()
    }
    
    /**
     * Start monitoring power state and apply optimizations
     */
    private fun startPowerOptimizationMonitoring() {
        scope.launch {
            combine(
                powerManager.powerState,
                voiceAgentServiceManager.serviceState
            ) { powerState, serviceState ->
                Pair(powerState, serviceState)
            }.collect { (powerState, serviceState) ->
                applyOptimizations(powerState, serviceState)
            }
        }
    }
    
    /**
     * Apply power optimizations based on current state
     */
    private suspend fun applyOptimizations(
        powerState: PowerState,
        serviceState: ServiceState
    ) {
        val newOptimizationLevel = when (powerState.powerMode) {
            PowerMode.NORMAL -> 0
            PowerMode.POWER_SAVE -> 1
            PowerMode.CRITICAL_SAVE -> 2
            PowerMode.SLEEP -> 3
        }
        
        if (newOptimizationLevel != currentOptimizationLevel || !isOptimizationActive) {
            currentOptimizationLevel = newOptimizationLevel
            isOptimizationActive = true
            
            when (powerState.powerMode) {
                PowerMode.NORMAL -> applyNormalOptimizations(serviceState)
                PowerMode.POWER_SAVE -> applyPowerSaveOptimizations(serviceState)
                PowerMode.CRITICAL_SAVE -> applyCriticalSaveOptimizations(serviceState)
                PowerMode.SLEEP -> applySleepOptimizations(serviceState)
            }
            
            updatePowerNotification(powerState)
        }
    }
    
    /**
     * Apply normal operation optimizations
     */
    private suspend fun applyNormalOptimizations(serviceState: ServiceState) {
        // Full functionality enabled
        voiceAgentServiceManager.setAudioProcessingEnabled(true)
        voiceAgentServiceManager.setBackgroundSyncEnabled(true)
        voiceAgentServiceManager.setProcessingInterval(1000) // 1 second
        voiceAgentServiceManager.setVADSensitivity(0.5f) // Normal sensitivity
        
        // Cancel any power-saving work
        WorkManager.getInstance(context).cancelAllWorkByTag(\"power_save\")
    }
    
    /**
     * Apply power save optimizations
     */
    private suspend fun applyPowerSaveOptimizations(serviceState: ServiceState) {
        // Reduced functionality
        voiceAgentServiceManager.setAudioProcessingEnabled(true)
        voiceAgentServiceManager.setBackgroundSyncEnabled(false)
        voiceAgentServiceManager.setProcessingInterval(2000) // 2 seconds
        voiceAgentServiceManager.setVADSensitivity(0.6f) // Slightly less sensitive
        
        // Schedule periodic sync instead of continuous
        schedulePeriodicSync(30, TimeUnit.MINUTES)
    }
    
    /**
     * Apply critical save optimizations
     */
    private suspend fun applyCriticalSaveOptimizations(serviceState: ServiceState) {
        // Minimal functionality
        voiceAgentServiceManager.setAudioProcessingEnabled(true) // Keep core feature
        voiceAgentServiceManager.setBackgroundSyncEnabled(false)
        voiceAgentServiceManager.setProcessingInterval(5000) // 5 seconds
        voiceAgentServiceManager.setVADSensitivity(0.7f) // Less sensitive to save power
        
        // Reduce audio buffer size to save memory
        voiceAgentServiceManager.setAudioBufferSize(1024) // Smaller buffer
        
        // Schedule very infrequent sync
        schedulePeriodicSync(2, TimeUnit.HOURS)
    }
    
    /**
     * Apply sleep mode optimizations
     */
    private suspend fun applySleepOptimizations(serviceState: ServiceState) {
        // Market closed - minimal power usage
        voiceAgentServiceManager.setAudioProcessingEnabled(false)
        voiceAgentServiceManager.setBackgroundSyncEnabled(false)
        
        // Stop all non-essential services
        voiceAgentServiceManager.pauseService()
        
        // Schedule wake-up for market opening
        scheduleMarketOpeningWakeUp()
        
        // Cancel all background work
        WorkManager.getInstance(context).cancelAllWorkByTag(\"power_save\")
    }
    
    /**
     * Schedule periodic sync work during power save modes
     */
    private fun schedulePeriodicSync(interval: Long, timeUnit: TimeUnit) {
        val syncWork = PeriodicWorkRequestBuilder<PowerSaveSyncWorker>(interval, timeUnit)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .addTag(\"power_save\")
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            \"power_save_sync\",
            ExistingPeriodicWorkPolicy.REPLACE,
            syncWork
        )
    }
    
    /**
     * Schedule wake-up work for market opening
     */
    private fun scheduleMarketOpeningWakeUp() {
        val wakeUpWork = OneTimeWorkRequestBuilder<MarketWakeUpWorker>()
            .setInitialDelay(calculateTimeUntilMarketOpen(), TimeUnit.MILLISECONDS)
            .addTag(\"market_wakeup\")
            .build()
        
        WorkManager.getInstance(context).enqueueUniqueWork(
            \"market_wakeup\",
            ExistingWorkPolicy.REPLACE,
            wakeUpWork
        )
    }
    
    /**
     * Calculate time until market opens
     */
    private fun calculateTimeUntilMarketOpen(): Long {
        // This would calculate the actual time until 6 AM next day
        // For now, return 8 hours as example
        return 8 * 60 * 60 * 1000L // 8 hours in milliseconds
    }
    
    /**
     * Schedule periodic battery check work
     */
    private fun scheduleBatteryCheckWork() {
        val batteryCheckWork = PeriodicWorkRequestBuilder<BatteryCheckWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(false) // Always run
                    .build()
            )
            .addTag(BATTERY_CHECK_WORK_TAG)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            \"battery_check\",
            ExistingPeriodicWorkPolicy.KEEP,
            batteryCheckWork
        )
    }
    
    /**
     * Update power optimization notification
     */
    private fun updatePowerNotification(powerState: PowerState) {
        val (title, content, icon) = when (powerState.powerMode) {
            PowerMode.NORMAL -> Triple(
                \"Voice Ledger - Normal Mode\",
                \"Battery: ${powerState.batteryLevel}% | All features active\",
                R.drawable.ic_battery_full
            )
            PowerMode.POWER_SAVE -> Triple(
                \"Voice Ledger - Power Save\",
                \"Battery: ${powerState.batteryLevel}% | Reduced background activity\",
                R.drawable.ic_battery_alert
            )
            PowerMode.CRITICAL_SAVE -> Triple(
                \"Voice Ledger - Critical Save\",
                \"Battery: ${powerState.batteryLevel}% | Minimal functionality\",
                R.drawable.ic_battery_alert
            )
            PowerMode.SLEEP -> Triple(
                \"Voice Ledger - Sleep Mode\",
                \"Market closed | Minimal power usage\",
                R.drawable.ic_sleep
            )
        }
        
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(icon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * Create notification channel for power optimization
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                \"Power Optimization\",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = \"Shows current power optimization status\"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Get current optimization status
     */
    fun getOptimizationStatus(): OptimizationStatus {
        val powerState = powerManager.powerState.value
        return OptimizationStatus(
            level = currentOptimizationLevel,
            mode = powerState.powerMode,
            batteryLevel = powerState.batteryLevel,
            isCharging = powerState.isCharging,
            marketStatus = powerState.marketStatus,
            estimatedBatteryLife = powerManager.getEstimatedBatteryLife()
        )
    }
    
    /**
     * Manually trigger optimization level
     */
    fun setOptimizationLevel(level: Int) {
        val mode = when (level.coerceIn(0, 3)) {
            0 -> PowerMode.NORMAL
            1 -> PowerMode.POWER_SAVE
            2 -> PowerMode.CRITICAL_SAVE
            3 -> PowerMode.SLEEP
            else -> PowerMode.NORMAL
        }
        powerManager.forcePowerMode(mode)
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        notificationManager.cancel(NOTIFICATION_ID)
        WorkManager.getInstance(context).cancelAllWorkByTag(\"power_save\")
        WorkManager.getInstance(context).cancelAllWorkByTag(\"market_wakeup\")
        WorkManager.getInstance(context).cancelAllWorkByTag(BATTERY_CHECK_WORK_TAG)
    }
}

/**
 * Current optimization status
 */
data class OptimizationStatus(
    val level: Int,
    val mode: PowerMode,
    val batteryLevel: Int,
    val isCharging: Boolean,
    val marketStatus: MarketStatus,
    val estimatedBatteryLife: Float
)

/**
 * Worker for periodic sync during power save modes
 */
class PowerSaveSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            // Perform minimal sync operations
            // This would sync only critical data
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

/**
 * Worker for waking up when market opens
 */
class MarketWakeUpWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            // Resume normal operations when market opens
            // This would restart the VoiceAgentService
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}

/**
 * Worker for periodic battery checks
 */
class BatteryCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            // Trigger battery status update
            // This ensures power optimizations are applied even when app is backgrounded
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}