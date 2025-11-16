package com.voiceledger.ghana.offline

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Background worker for processing offline queue operations
 * Runs periodically to sync pending operations when network is available
 */
@HiltWorker
class OfflineSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val offlineQueueManager: OfflineQueueManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Process all pending operations
            offlineQueueManager.processAllPendingOperations()
            
            // Get queue statistics for logging
            val stats = offlineQueueManager.getQueueStatistics()
            
            // Clean up old operations
            offlineQueueManager.cleanupOldOperations()
            
            // Determine if work should be retried
            return if (stats.failedCount > 0) {
                Result.retry()
            } else {
                Result.success()
            }
            
        } catch (e: Exception) {
            // Log error and retry
            e.printStackTrace()
            Result.retry()
        }
    }
}