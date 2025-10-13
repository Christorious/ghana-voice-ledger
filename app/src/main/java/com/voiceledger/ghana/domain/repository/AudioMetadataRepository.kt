package com.voiceledger.ghana.domain.repository

import com.voiceledger.ghana.data.local.entity.AudioMetadata
import com.voiceledger.ghana.data.local.dao.ErrorFrequency
import com.voiceledger.ghana.data.local.dao.HourlyPerformanceStats
import com.voiceledger.ghana.data.local.dao.SpeakerDetectionStats
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for audio metadata operations
 * Defines the contract for audio processing analytics and debugging
 */
interface AudioMetadataRepository {
    
    // Query operations
    fun getRecentAudioMetadata(limit: Int = 100): Flow<List<AudioMetadata>>
    fun getAudioMetadataByTimeRange(startTime: Long, endTime: Long): Flow<List<AudioMetadata>>
    fun getMetadataWithSpeakerDetection(): Flow<List<AudioMetadata>>
    fun getMetadataWithSpeechDetection(): Flow<List<AudioMetadata>>
    fun getMetadataContributingToTransactions(): Flow<List<AudioMetadata>>
    suspend fun getMetadataForTransaction(transactionId: String): List<AudioMetadata>
    fun getMetadataWithErrors(): Flow<List<AudioMetadata>>
    fun getMetadataInPowerSavingMode(): Flow<List<AudioMetadata>>
    
    // Analytics operations
    suspend fun getSpeechDetectionCount(startTime: Long, endTime: Long): Int
    suspend fun getSpeakerDetectionCount(startTime: Long, endTime: Long): Int
    suspend fun getAverageVADScore(startTime: Long, endTime: Long): Double
    suspend fun getAverageSpeakerConfidence(startTime: Long, endTime: Long): Double
    suspend fun getAverageAudioQuality(startTime: Long, endTime: Long): Double
    suspend fun getAverageProcessingTime(startTime: Long, endTime: Long): Double
    suspend fun getMaxProcessingTime(startTime: Long, endTime: Long): Long
    suspend fun getErrorCount(startTime: Long, endTime: Long): Int
    suspend fun getErrorFrequency(): List<ErrorFrequency>
    suspend fun getAverageBatteryLevel(startTime: Long, endTime: Long): Double
    suspend fun getPowerSavingModeCount(startTime: Long, endTime: Long): Int
    suspend fun getHourlyPerformanceStats(startTime: Long, endTime: Long): List<HourlyPerformanceStats>
    suspend fun getSpeakerDetectionStats(startTime: Long, endTime: Long): List<SpeakerDetectionStats>
    
    // CRUD operations
    suspend fun insertMetadata(metadata: AudioMetadata)
    suspend fun insertMetadataList(metadataList: List<AudioMetadata>)
    suspend fun updateMetadata(metadata: AudioMetadata)
    suspend fun deleteMetadata(metadata: AudioMetadata)
    suspend fun deleteMetadataById(chunkId: String)
    suspend fun markAsContributingToTransaction(chunkIds: List<String>, transactionId: String)
    
    // Performance monitoring operations
    suspend fun getTodaysPerformanceStats(): PerformanceStats
    suspend fun getPerformanceStatsForPeriod(startTime: Long, endTime: Long): PerformanceStats
    suspend fun getSystemHealthMetrics(): SystemHealthMetrics
    
    // Maintenance operations
    suspend fun deleteOldMetadata(cutoffTime: Long)
    suspend fun getTotalMetadataCount(): Int
    suspend fun getOldestMetadataTimestamp(): Long?
    suspend fun getNewestMetadataTimestamp(): Long?
    suspend fun deleteOldestMetadata(count: Int)
    suspend fun cleanupMetadata(maxCount: Int, maxAge: Long)
}

/**
 * Data class for performance statistics
 */
data class PerformanceStats(
    val totalChunks: Int,
    val speechDetectionRate: Double,
    val speakerDetectionRate: Double,
    val averageVADScore: Double,
    val averageProcessingTime: Double,
    val maxProcessingTime: Long,
    val errorRate: Double,
    val averageBatteryLevel: Double,
    val powerSavingModeUsage: Double
)

/**
 * Data class for system health metrics
 */
data class SystemHealthMetrics(
    val isHealthy: Boolean,
    val processingLatency: Double,
    val errorRate: Double,
    val batteryImpact: Double,
    val memoryUsage: Double,
    val recommendations: List<String>
)