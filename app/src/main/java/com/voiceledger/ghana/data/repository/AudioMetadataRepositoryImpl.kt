package com.voiceledger.ghana.data.repository

import com.voiceledger.ghana.data.local.dao.AudioMetadataDao
import com.voiceledger.ghana.data.local.dao.ErrorFrequency
import com.voiceledger.ghana.data.local.dao.HourlyPerformanceStats
import com.voiceledger.ghana.data.local.dao.SpeakerDetectionStats
import com.voiceledger.ghana.data.local.entity.AudioMetadata
import com.voiceledger.ghana.domain.repository.AudioMetadataRepository
import com.voiceledger.ghana.domain.repository.PerformanceStats
import com.voiceledger.ghana.domain.repository.SystemHealthMetrics
import kotlinx.coroutines.flow.Flow
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AudioMetadataRepository
 * Handles audio processing analytics and system monitoring
 */
@Singleton
class AudioMetadataRepositoryImpl @Inject constructor(
    private val audioMetadataDao: AudioMetadataDao
) : AudioMetadataRepository {
    
    override fun getRecentAudioMetadata(limit: Int): Flow<List<AudioMetadata>> {
        return audioMetadataDao.getRecentAudioMetadata(limit)
    }
    
    override fun getAudioMetadataByTimeRange(startTime: Long, endTime: Long): Flow<List<AudioMetadata>> {
        return audioMetadataDao.getAudioMetadataByTimeRange(startTime, endTime)
    }
    
    override fun getMetadataWithSpeakerDetection(): Flow<List<AudioMetadata>> {
        return audioMetadataDao.getMetadataWithSpeakerDetection()
    }
    
    override fun getMetadataWithSpeechDetection(): Flow<List<AudioMetadata>> {
        return audioMetadataDao.getMetadataWithSpeechDetection()
    }
    
    override fun getMetadataContributingToTransactions(): Flow<List<AudioMetadata>> {
        return audioMetadataDao.getMetadataContributingToTransactions()
    }
    
    override suspend fun getMetadataForTransaction(transactionId: String): List<AudioMetadata> {
        return audioMetadataDao.getMetadataForTransaction(transactionId)
    }
    
    override fun getMetadataWithErrors(): Flow<List<AudioMetadata>> {
        return audioMetadataDao.getMetadataWithErrors()
    }
    
    override fun getMetadataInPowerSavingMode(): Flow<List<AudioMetadata>> {
        return audioMetadataDao.getMetadataInPowerSavingMode()
    }
    
    override suspend fun getSpeechDetectionCount(startTime: Long, endTime: Long): Int {
        return audioMetadataDao.getSpeechDetectionCount(startTime, endTime)
    }
    
    override suspend fun getSpeakerDetectionCount(startTime: Long, endTime: Long): Int {
        return audioMetadataDao.getSpeakerDetectionCount(startTime, endTime)
    }
    
    override suspend fun getAverageVADScore(startTime: Long, endTime: Long): Double {
        return audioMetadataDao.getAverageVADScore(startTime, endTime) ?: 0.0
    }
    
    override suspend fun getAverageSpeakerConfidence(startTime: Long, endTime: Long): Double {
        return audioMetadataDao.getAverageSpeakerConfidence(startTime, endTime) ?: 0.0
    }
    
    override suspend fun getAverageAudioQuality(startTime: Long, endTime: Long): Double {
        return audioMetadataDao.getAverageAudioQuality(startTime, endTime) ?: 0.0
    }
    
    override suspend fun getAverageProcessingTime(startTime: Long, endTime: Long): Double {
        return audioMetadataDao.getAverageProcessingTime(startTime, endTime) ?: 0.0
    }
    
    override suspend fun getMaxProcessingTime(startTime: Long, endTime: Long): Long {
        return audioMetadataDao.getMaxProcessingTime(startTime, endTime) ?: 0L
    }
    
    override suspend fun getErrorCount(startTime: Long, endTime: Long): Int {
        return audioMetadataDao.getErrorCount(startTime, endTime)
    }
    
    override suspend fun getErrorFrequency(): List<ErrorFrequency> {
        return audioMetadataDao.getErrorFrequency()
    }
    
    override suspend fun getAverageBatteryLevel(startTime: Long, endTime: Long): Double {
        return audioMetadataDao.getAverageBatteryLevel(startTime, endTime) ?: 0.0
    }
    
    override suspend fun getPowerSavingModeCount(startTime: Long, endTime: Long): Int {
        return audioMetadataDao.getPowerSavingModeCount(startTime, endTime)
    }
    
    override suspend fun getHourlyPerformanceStats(startTime: Long, endTime: Long): List<HourlyPerformanceStats> {
        return audioMetadataDao.getHourlyPerformanceStats(startTime, endTime)
    }
    
    override suspend fun getSpeakerDetectionStats(startTime: Long, endTime: Long): List<SpeakerDetectionStats> {
        return audioMetadataDao.getSpeakerDetectionStats(startTime, endTime)
    }
    
    override suspend fun insertMetadata(metadata: AudioMetadata) {
        audioMetadataDao.insertMetadata(metadata)
    }
    
    override suspend fun insertMetadataList(metadataList: List<AudioMetadata>) {
        audioMetadataDao.insertMetadataList(metadataList)
    }
    
    override suspend fun updateMetadata(metadata: AudioMetadata) {
        audioMetadataDao.updateMetadata(metadata)
    }
    
    override suspend fun deleteMetadata(metadata: AudioMetadata) {
        audioMetadataDao.deleteMetadata(metadata)
    }
    
    override suspend fun deleteMetadataById(chunkId: String) {
        audioMetadataDao.deleteMetadataById(chunkId)
    }
    
    override suspend fun markAsContributingToTransaction(chunkIds: List<String>, transactionId: String) {
        audioMetadataDao.markAsContributingToTransaction(chunkIds, transactionId)
    }
    
    override suspend fun getTodaysPerformanceStats(): PerformanceStats {
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        val startTime = today.timeInMillis
        val endTime = startTime + TimeUnit.DAYS.toMillis(1)
        
        return getPerformanceStatsForPeriod(startTime, endTime)
    }
    
    override suspend fun getPerformanceStatsForPeriod(startTime: Long, endTime: Long): PerformanceStats {
        val totalChunks = audioMetadataDao.getRecentAudioMetadata(Int.MAX_VALUE).toString().length // Simplified
        val speechCount = getSpeechDetectionCount(startTime, endTime)
        val speakerCount = getSpeakerDetectionCount(startTime, endTime)
        val avgVADScore = getAverageVADScore(startTime, endTime)
        val avgProcessingTime = getAverageProcessingTime(startTime, endTime)
        val maxProcessingTime = getMaxProcessingTime(startTime, endTime)
        val errorCount = getErrorCount(startTime, endTime)
        val avgBatteryLevel = getAverageBatteryLevel(startTime, endTime)
        val powerSavingCount = getPowerSavingModeCount(startTime, endTime)
        
        val speechDetectionRate = if (totalChunks > 0) speechCount.toDouble() / totalChunks else 0.0
        val speakerDetectionRate = if (speechCount > 0) speakerCount.toDouble() / speechCount else 0.0
        val errorRate = if (totalChunks > 0) errorCount.toDouble() / totalChunks else 0.0
        val powerSavingUsage = if (totalChunks > 0) powerSavingCount.toDouble() / totalChunks else 0.0
        
        return PerformanceStats(
            totalChunks = totalChunks,
            speechDetectionRate = speechDetectionRate,
            speakerDetectionRate = speakerDetectionRate,
            averageVADScore = avgVADScore,
            averageProcessingTime = avgProcessingTime,
            maxProcessingTime = maxProcessingTime,
            errorRate = errorRate,
            averageBatteryLevel = avgBatteryLevel,
            powerSavingModeUsage = powerSavingUsage
        )
    }
    
    override suspend fun getSystemHealthMetrics(): SystemHealthMetrics {
        val todayStats = getTodaysPerformanceStats()
        val recommendations = mutableListOf<String>()
        
        // Analyze performance and generate recommendations
        val isHealthy = todayStats.errorRate < 0.05 && // Less than 5% error rate
                todayStats.averageProcessingTime < 1000 && // Less than 1 second processing time
                todayStats.averageBatteryLevel > 20 // Battery above 20%
        
        if (todayStats.errorRate > 0.1) {
            recommendations.add("High error rate detected. Check audio input quality.")
        }
        
        if (todayStats.averageProcessingTime > 2000) {
            recommendations.add("Processing time is high. Consider optimizing ML models.")
        }
        
        if (todayStats.averageBatteryLevel < 30) {
            recommendations.add("Battery level is low. Enable power saving mode.")
        }
        
        if (todayStats.speechDetectionRate < 0.1) {
            recommendations.add("Low speech detection rate. Check microphone permissions.")
        }
        
        if (todayStats.speakerDetectionRate < 0.5) {
            recommendations.add("Low speaker identification rate. Consider retraining speaker models.")
        }
        
        return SystemHealthMetrics(
            isHealthy = isHealthy,
            processingLatency = todayStats.averageProcessingTime,
            errorRate = todayStats.errorRate,
            batteryImpact = 100 - todayStats.averageBatteryLevel,
            memoryUsage = 0.0, // Would be calculated from actual memory metrics
            recommendations = recommendations
        )
    }
    
    override suspend fun deleteOldMetadata(cutoffTime: Long) {
        audioMetadataDao.deleteOldMetadata(cutoffTime)
    }
    
    override suspend fun getTotalMetadataCount(): Int {
        return audioMetadataDao.getTotalMetadataCount()
    }
    
    override suspend fun getOldestMetadataTimestamp(): Long? {
        return audioMetadataDao.getOldestMetadataTimestamp()
    }
    
    override suspend fun getNewestMetadataTimestamp(): Long? {
        return audioMetadataDao.getNewestMetadataTimestamp()
    }
    
    override suspend fun deleteOldestMetadata(count: Int) {
        audioMetadataDao.deleteOldestMetadata(count)
    }
    
    override suspend fun cleanupMetadata(maxCount: Int, maxAge: Long) {
        val totalCount = getTotalMetadataCount()
        
        // Delete old metadata if we have too many records
        if (totalCount > maxCount) {
            val excessCount = totalCount - maxCount
            deleteOldestMetadata(excessCount)
        }
        
        // Delete metadata older than maxAge
        val cutoffTime = System.currentTimeMillis() - maxAge
        deleteOldMetadata(cutoffTime)
    }
}