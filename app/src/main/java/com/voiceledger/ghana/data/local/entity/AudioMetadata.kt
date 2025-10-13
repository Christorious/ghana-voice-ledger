package com.voiceledger.ghana.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Audio metadata entity for tracking processed audio chunks
 * Used for debugging and performance monitoring
 */
@Entity(
    tableName = "audio_metadata",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["speakerDetected"]),
        Index(value = ["vadScore"])
    ]
)
data class AudioMetadata(
    @PrimaryKey
    val chunkId: String,
    
    /** Timestamp when audio chunk was processed */
    val timestamp: Long,
    
    /** Voice Activity Detection score (0.0 to 1.0) */
    val vadScore: Float,
    
    /** Whether speech was detected in this chunk */
    val speechDetected: Boolean,
    
    /** Whether a speaker was identified */
    val speakerDetected: Boolean,
    
    /** Speaker ID if identified */
    val speakerId: String?,
    
    /** Speaker confidence score */
    val speakerConfidence: Float?,
    
    /** Audio quality score (signal-to-noise ratio) */
    val audioQuality: Float?,
    
    /** Duration of audio chunk in milliseconds */
    val durationMs: Long,
    
    /** Processing time in milliseconds */
    val processingTimeMs: Long,
    
    /** Whether this chunk contributed to a transaction */
    val contributedToTransaction: Boolean = false,
    
    /** Transaction ID if this chunk was part of a transaction */
    val transactionId: String?,
    
    /** Error message if processing failed */
    val errorMessage: String?,
    
    /** Battery level when this chunk was processed */
    val batteryLevel: Int?,
    
    /** Whether device was in power saving mode */
    val powerSavingMode: Boolean = false
)