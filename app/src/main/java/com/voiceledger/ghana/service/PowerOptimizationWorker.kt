package com.voiceledger.ghana.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.voiceledger.ghana.offline.OfflineQueueManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background worker for power optimization and maintenance tasks
 * Runs periodically to perform cleanup and optimization tasks
 */
@HiltWorker
class PowerOptimizationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val powerManager: PowerManager,
    private val offlineQueueManager: OfflineQueueManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Clean up old offline operations
            offlineQueueManager.cleanupOldOperations()
            
            // Update power management settings based on current state
            val powerState = powerManager.powerState.value
            
            // Apply additional optimizations if needed
            when (powerState.powerMode) {
                PowerMode.CRITICAL_SAVE -> {
                    // In critical power save, clean up more aggressively
                    offlineQueueManager.cleanupOldOperations()
                }
                PowerMode.SLEEP -> {
                    // In sleep mode, pause all non-essential operations
                    // This would be handled by the power manager
                }
                else -> {
                    // Normal operation, no additional cleanup needed
                }
            }
            
            Result.success()
            
        } catch (e: Exception) {
            // Log error but don't retry - this is maintenance work
            e.printStackTrace()
            Result.failure()
        }
    }
}