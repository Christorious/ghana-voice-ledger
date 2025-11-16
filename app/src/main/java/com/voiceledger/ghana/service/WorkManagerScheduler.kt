package com.voiceledger.ghana.service

import android.content.Context
import androidx.work.*
import com.voiceledger.ghana.offline.OfflineSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for managing WorkManager background jobs
 * Handles scheduling and cancellation of various periodic and one-time tasks
 */
@Singleton
class WorkManagerScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    companion object {
        private const val OFFLINE_SYNC_WORK = "OfflineSyncWork"
        private const val POWER_OPTIMIZATION_WORK = "PowerOptimizationWork"
        private const val CLEANUP_WORK = "CleanupWork"
        private const val DAILY_SUMMARY_WORK = "DailySummaryWork"
    }
    
    init {
        scheduleAllPeriodicWork()
    }
    
    /**
     * Schedule all periodic work tasks
     */
    private fun scheduleAllPeriodicWork() {
        scheduleOfflineSync()
        schedulePowerOptimization()
        scheduleDailyCleanup()
        scheduleDailySummaryGeneration()
    }
    
    /**
     * Schedule periodic offline sync
     */
    fun scheduleOfflineSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()
        
        val syncRequest = PeriodicWorkRequestBuilder<OfflineSyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag(OFFLINE_SYNC_WORK)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            OFFLINE_SYNC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            syncRequest
        )
    }
    
    /**
     * Schedule periodic power optimization
     */
    fun schedulePowerOptimization() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false) // Can run even on low battery
            .setRequiresCharging(false)
            .build()
        
        val optimizationRequest = PeriodicWorkRequestBuilder<PowerOptimizationWorker>(
            2, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                30, TimeUnit.MINUTES
            )
            .addTag(POWER_OPTIMIZATION_WORK)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            POWER_OPTIMIZATION_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            optimizationRequest
        )
    }
    
    /**
     * Schedule daily cleanup tasks
     */
    fun scheduleDailyCleanup() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresCharging(true)
            .setRequiresDeviceIdle(true)
            .build()
        
        val cleanupRequest = PeriodicWorkRequestBuilder<PowerOptimizationWorker>(
            1, TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .addTag(CLEANUP_WORK)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            CLEANUP_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            cleanupRequest
        )
    }
    
    /**
     * Schedule daily summary generation
     */
    fun scheduleDailySummaryGeneration() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .setRequiresCharging(true)
            .build()
        
        val summaryRequest = PeriodicWorkRequestBuilder<DailySummaryWorker>(
            1, TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setInitialDelay(
                calculateDelayUntilTargetHour(22), // Run at 10 PM
                TimeUnit.MILLISECONDS
            )
            .addTag(DAILY_SUMMARY_WORK)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DAILY_SUMMARY_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            summaryRequest
        )
    }
    
    /**
     * Schedule immediate offline sync
     */
    fun scheduleImmediateOfflineSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val immediateSyncRequest = OneTimeWorkRequestBuilder<OfflineSyncWorker>()
            .setConstraints(constraints)
            .addTag(OFFLINE_SYNC_WORK)
            .build()
        
        WorkManager.getInstance(context).enqueue(immediateSyncRequest)
    }
    
    /**
     * Schedule immediate power optimization
     */
    fun scheduleImmediatePowerOptimization() {
        val optimizationRequest = OneTimeWorkRequestBuilder<PowerOptimizationWorker>()
            .addTag(POWER_OPTIMIZATION_WORK)
            .build()
        
        WorkManager.getInstance(context).enqueue(optimizationRequest)
    }
    
    /**
     * Cancel all work by tag
     */
    fun cancelWorkByTag(tag: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag(tag)
    }
    
    /**
     * Cancel all scheduled work
     */
    fun cancelAllWork() {
        WorkManager.getInstance(context).cancelAllWork()
    }
    
    /**
     * Get work info for a specific tag
     */
    fun getWorkInfoByTag(tag: String) = WorkManager.getInstance(context)
        .getWorkInfosByTagLiveData(tag)
    
    /**
     * Check if work is scheduled
     */
    fun isWorkScheduled(tag: String): Boolean {
        return try {
            val workInfos = WorkManager.getInstance(context)
                .getWorkInfosByTag(tag).get()
            workInfos.any { !it.state.isFinished }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Calculate delay until target hour (for daily tasks)
     */
    private fun calculateDelayUntilTargetHour(targetHour: Int): Long {
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = now
        
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val targetTime = calendar.apply {
            set(java.util.Calendar.HOUR_OF_DAY, targetHour)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        // If target time has passed today, schedule for tomorrow
        return if (targetTime <= now) {
            targetTime + TimeUnit.DAYS.toMillis(1) - now
        } else {
            targetTime - now
        }
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        scope.cancel()
    }
}