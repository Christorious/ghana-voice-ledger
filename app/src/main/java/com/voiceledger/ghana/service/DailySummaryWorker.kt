package com.voiceledger.ghana.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.voiceledger.ghana.domain.service.DailySummaryGenerator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background worker for generating daily summaries
 * Runs once per day to generate and sync daily transaction summaries
 */
@HiltWorker
class DailySummaryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val dailySummaryGenerator: DailySummaryGenerator
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Generate daily summary for yesterday
            val yesterday = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            
            dailySummaryGenerator.generateDailySummary(yesterday)
            
            Result.success()
            
        } catch (e: Exception) {
            // Log error and retry
            e.printStackTrace()
            Result.retry()
        }
    }
}