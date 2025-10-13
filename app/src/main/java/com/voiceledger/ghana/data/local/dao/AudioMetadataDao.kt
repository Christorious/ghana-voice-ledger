package com.voiceledger.ghana.data.local.dao

import androidx.room.*
import com.voiceledger.ghana.data.local.entity.AudioMetadata
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for AudioMetadata entity
 * Provides queries for audio processing analytics and debugging
 */
@Dao
interface AudioMetadataDao {
    
    @Query("SELECT * FROM audio_metadata ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentAudioMetadata(limit: Int = 100): Flow<List<AudioMetadata>>
    
    @Query("SELECT * FROM audio_metadata WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getAudioMetadataByTimeRange(startTime: Long, endTime: Long): Flow<List<AudioMetadata>>
    
    @Query("SELECT * FROM audio_metadata WHERE speakerDetected = 1 ORDER BY timestamp DESC")
    fun getMetadataWithSpeakerDetection(): Flow<List<AudioMetadata>>
    
    @Query("SELECT * FROM audio_metadata WHERE speechDetected = 1 ORDER BY timestamp DESC")
    fun getMetadataWithSpeechDetection(): Flow<List<AudioMetadata>>
    
    @Query("SELECT * FROM audio_metadata WHERE contributedToTransaction = 1 ORDER BY timestamp DESC")
    fun getMetadataContributingToTransactions(): Flow<List<AudioMetadata>>
    
    @Query("SELECT * FROM audio_metadata WHERE transactionId = :transactionId ORDER BY timestamp ASC")
    suspend fun getMetadataForTransaction(transactionId: String): List<AudioMetadata>
    
    @Query("SELECT * FROM audio_metadata WHERE errorMessage IS NOT NULL ORDER BY timestamp DESC")
    fun getMetadataWithErrors(): Flow<List<AudioMetadata>>
    
    @Query("SELECT * FROM audio_metadata WHERE powerSavingMode = 1 ORDER BY timestamp DESC")
    fun getMetadataInPowerSavingMode(): Flow<List<AudioMetadata>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: AudioMetadata)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadataList(metadataList: List<AudioMetadata>)
    
    @Update
    suspend fun updateMetadata(metadata: AudioMetadata)
    
    @Delete
    suspend fun deleteMetadata(metadata: AudioMetadata)
    
    @Query("DELETE FROM audio_metadata WHERE chunkId = :chunkId")
    suspend fun deleteMetadataById(chunkId: String)
    
    @Query("DELETE FROM audio_metadata WHERE timestamp < :cutoffTime")
    suspend fun deleteOldMetadata(cutoffTime: Long)
    
    @Query("UPDATE audio_metadata SET contributedToTransaction = 1, transactionId = :transactionId WHERE chunkId IN (:chunkIds)")
    suspend fun markAsContributingToTransaction(chunkIds: List<String>, transactionId: String)
    
    // Analytics queries
    @Query("SELECT COUNT(*) FROM audio_metadata WHERE speechDetected = 1 AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getSpeechDetectionCount(startTime: Long, endTime: Long): Int
    
    @Query("SELECT COUNT(*) FROM audio_metadata WHERE speakerDetected = 1 AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getSpeakerDetectionCount(startTime: Long, endTime: Long): Int
    
    @Query("SELECT AVG(vadScore) FROM audio_metadata WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getAverageVADScore(startTime: Long, endTime: Long): Double?
    
    @Query("SELECT AVG(speakerConfidence) FROM audio_metadata WHERE speakerConfidence IS NOT NULL AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getAverageSpeakerConfidence(startTime: Long, endTime: Long): Double?
    
    @Query("SELECT AVG(audioQuality) FROM audio_metadata WHERE audioQuality IS NOT NULL AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getAverageAudioQuality(startTime: Long, endTime: Long): Double?
    
    @Query("SELECT AVG(processingTimeMs) FROM audio_metadata WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getAverageProcessingTime(startTime: Long, endTime: Long): Double?
    
    @Query("SELECT MAX(processingTimeMs) FROM audio_metadata WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getMaxProcessingTime(startTime: Long, endTime: Long): Long?
    
    @Query("SELECT COUNT(*) FROM audio_metadata WHERE errorMessage IS NOT NULL AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getErrorCount(startTime: Long, endTime: Long): Int
    
    @Query("SELECT errorMessage, COUNT(*) as count FROM audio_metadata WHERE errorMessage IS NOT NULL GROUP BY errorMessage ORDER BY count DESC")
    suspend fun getErrorFrequency(): List<ErrorFrequency>
    
    @Query("SELECT AVG(batteryLevel) FROM audio_metadata WHERE batteryLevel IS NOT NULL AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getAverageBatteryLevel(startTime: Long, endTime: Long): Double?
    
    @Query("SELECT COUNT(*) FROM audio_metadata WHERE powerSavingMode = 1 AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getPowerSavingModeCount(startTime: Long, endTime: Long): Int
    
    // Performance monitoring queries
    @Query("""
        SELECT 
            strftime('%H', datetime(timestamp/1000, 'unixepoch')) as hour,
            COUNT(*) as chunkCount,
            AVG(processingTimeMs) as avgProcessingTime,
            AVG(vadScore) as avgVadScore
        FROM audio_metadata 
        WHERE timestamp BETWEEN :startTime AND :endTime
        GROUP BY hour
        ORDER BY hour
    """)
    suspend fun getHourlyPerformanceStats(startTime: Long, endTime: Long): List<HourlyPerformanceStats>
    
    @Query("""
        SELECT 
            speakerId,
            COUNT(*) as detectionCount,
            AVG(speakerConfidence) as avgConfidence
        FROM audio_metadata 
        WHERE speakerId IS NOT NULL AND timestamp BETWEEN :startTime AND :endTime
        GROUP BY speakerId
        ORDER BY detectionCount DESC
    """)
    suspend fun getSpeakerDetectionStats(startTime: Long, endTime: Long): List<SpeakerDetectionStats>
    
    // Cleanup and maintenance
    @Query("SELECT COUNT(*) FROM audio_metadata")
    suspend fun getTotalMetadataCount(): Int
    
    @Query("SELECT MIN(timestamp) FROM audio_metadata")
    suspend fun getOldestMetadataTimestamp(): Long?
    
    @Query("SELECT MAX(timestamp) FROM audio_metadata")
    suspend fun getNewestMetadataTimestamp(): Long?
    
    @Query("DELETE FROM audio_metadata WHERE chunkId IN (SELECT chunkId FROM audio_metadata ORDER BY timestamp ASC LIMIT :count)")
    suspend fun deleteOldestMetadata(count: Int)
}

/**
 * Data class for error frequency results
 */
data class ErrorFrequency(
    val errorMessage: String,
    val count: Int
)

/**
 * Data class for hourly performance statistics
 */
data class HourlyPerformanceStats(
    val hour: String,
    val chunkCount: Int,
    val avgProcessingTime: Double,
    val avgVadScore: Double
)

/**
 * Data class for speaker detection statistics
 */
data class SpeakerDetectionStats(
    val speakerId: String,
    val detectionCount: Int,
    val avgConfidence: Double
)