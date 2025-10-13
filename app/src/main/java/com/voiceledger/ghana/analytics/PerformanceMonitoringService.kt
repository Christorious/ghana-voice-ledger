package com.voiceledger.ghana.analytics

import android.content.Context
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.ktx.performance
import com.google.firebase.perf.metrics.Trace
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Performance monitoring service for critical operations
 * Uses Firebase Performance Monitoring to track app performance
 */
@Singleton
class PerformanceMonitoringService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyticsService: AnalyticsService,
    private val crashlyticsService: CrashlyticsService
) {
    
    private val firebasePerformance: FirebasePerformance by lazy {
        Firebase.performance
    }
    
    private val performanceScope = CoroutineScope(Dispatchers.IO)
    private val activeTraces = ConcurrentHashMap<String, Trace>()
    
    companion object {
        // Trace names for critical operations
        const val TRACE_VOICE_RECOGNITION = "voice_recognition"
        const val TRACE_SPEAKER_IDENTIFICATION = "speaker_identification"
        const val TRACE_TRANSACTION_PROCESSING = "transaction_processing"
        const val TRACE_DATABASE_OPERATION = "database_operation"
        const val TRACE_OFFLINE_SYNC = "offline_sync"
        const val TRACE_AUDIO_PROCESSING = "audio_processing"
        const val TRACE_ML_INFERENCE = "ml_inference"
        const val TRACE_ENTITY_EXTRACTION = "entity_extraction"
        const val TRACE_LANGUAGE_DETECTION = "language_detection"
        const val TRACE_APP_STARTUP = "app_startup"
        const val TRACE_SCREEN_LOAD = "screen_load"
        const val TRACE_DATA_EXPORT = "data_export"
        const val TRACE_BACKUP_RESTORE = "backup_restore"
        
        // Performance thresholds (in milliseconds)
        const val THRESHOLD_VOICE_RECOGNITION = 3000L
        const val THRESHOLD_SPEAKER_IDENTIFICATION = 1000L
        const val THRESHOLD_TRANSACTION_PROCESSING = 500L
        const val THRESHOLD_DATABASE_OPERATION = 1000L
        const val THRESHOLD_OFFLINE_SYNC = 5000L
        const val THRESHOLD_AUDIO_PROCESSING = 2000L
        const val THRESHOLD_ML_INFERENCE = 2000L
        const val THRESHOLD_SCREEN_LOAD = 2000L
    }
    
    /**
     * Initialize performance monitoring
     */
    fun initialize() {
        performanceScope.launch {
            // Enable performance monitoring
            firebasePerformance.isPerformanceCollectionEnabled = true
            
            // Start app startup trace
            startTrace(TRACE_APP_STARTUP)
        }
    }
    
    /**
     * Start a performance trace
     */
    fun startTrace(traceName: String): String {
        val traceId = "${traceName}_${System.currentTimeMillis()}"
        val trace = firebasePerformance.newTrace(traceName)
        trace.start()
        activeTraces[traceId] = trace
        
        crashlyticsService.log("Started performance trace: $traceName")
        return traceId
    }
    
    /**
     * Stop a performance trace
     */
    fun stopTrace(traceId: String, success: Boolean = true) {
        activeTraces.remove(traceId)?.let { trace ->
            if (success) {
                trace.putAttribute("success", "true")
            } else {
                trace.putAttribute("success", "false")
            }
            trace.stop()
            
            crashlyticsService.log("Stopped performance trace: $traceId")
        }
    }
    
    /**
     * Add custom attributes to a trace
     */
    fun addTraceAttribute(traceId: String, key: String, value: String) {
        activeTraces[traceId]?.putAttribute(key, value)
    }
    
    fun addTraceMetric(traceId: String, metricName: String, value: Long) {
        activeTraces[traceId]?.putMetric(metricName, value)
    }
    
    /**
     * Monitor voice recognition performance
     */
    fun monitorVoiceRecognition(
        language: String?,
        audioLengthMs: Long,
        operation: suspend () -> VoiceRecognitionResult
    ): suspend () -> VoiceRecognitionResult = {
        val traceId = startTrace(TRACE_VOICE_RECOGNITION)
        val startTime = System.currentTimeMillis()
        
        try {
            language?.let { addTraceAttribute(traceId, "language", it) }
            addTraceMetric(traceId, "audio_length_ms", audioLengthMs)
            
            val result = operation()
            val duration = System.currentTimeMillis() - startTime
            
            addTraceMetric(traceId, "processing_time_ms", duration)
            addTraceAttribute(traceId, "confidence", result.confidence.toString())
            addTraceAttribute(traceId, "success", result.success.toString())
            
            // Check performance threshold
            if (duration > THRESHOLD_VOICE_RECOGNITION) {
                reportPerformanceIssue(
                    "voice_recognition_slow",
                    "Voice recognition took ${duration}ms (threshold: ${THRESHOLD_VOICE_RECOGNITION}ms)",
                    mapOf(
                        "duration" to duration.toString(),
                        "language" to (language ?: "unknown"),
                        "audio_length" to audioLengthMs.toString()
                    )
                )
            }
            
            stopTrace(traceId, result.success)
            result
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            addTraceMetric(traceId, "processing_time_ms", duration)
            stopTrace(traceId, false)
            
            crashlyticsService.recordVoiceRecognitionError(e, language, null)
            throw e
        }
    }
    
    /**
     * Monitor speaker identification performance
     */
    fun monitorSpeakerIdentification(
        operation: suspend () -> SpeakerIdentificationResult
    ): suspend () -> SpeakerIdentificationResult = {
        val traceId = startTrace(TRACE_SPEAKER_IDENTIFICATION)
        val startTime = System.currentTimeMillis()
        
        try {
            val result = operation()
            val duration = System.currentTimeMillis() - startTime
            
            addTraceMetric(traceId, "processing_time_ms", duration)
            addTraceAttribute(traceId, "speaker_id", result.speakerId ?: "unknown")
            addTraceAttribute(traceId, "confidence", result.confidence.toString())
            
            if (duration > THRESHOLD_SPEAKER_IDENTIFICATION) {
                reportPerformanceIssue(
                    "speaker_identification_slow",
                    "Speaker identification took ${duration}ms",
                    mapOf("duration" to duration.toString())
                )
            }
            
            stopTrace(traceId, true)
            result
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            addTraceMetric(traceId, "processing_time_ms", duration)
            stopTrace(traceId, false)
            
            crashlyticsService.recordSpeakerIdentificationError(e, null)
            throw e
        }
    }
    
    /**
     * Monitor transaction processing performance
     */
    fun monitorTransactionProcessing(
        productName: String,
        operation: suspend () -> TransactionResult
    ): suspend () -> TransactionResult = {
        val traceId = startTrace(TRACE_TRANSACTION_PROCESSING)
        val startTime = System.currentTimeMillis()
        
        try {
            addTraceAttribute(traceId, "product_name", productName)
            
            val result = operation()
            val duration = System.currentTimeMillis() - startTime
            
            addTraceMetric(traceId, "processing_time_ms", duration)
            addTraceAttribute(traceId, "success", result.success.toString())
            
            if (duration > THRESHOLD_TRANSACTION_PROCESSING) {
                reportPerformanceIssue(
                    "transaction_processing_slow",
                    "Transaction processing took ${duration}ms",
                    mapOf(
                        "duration" to duration.toString(),
                        "product" to productName
                    )
                )
            }
            
            stopTrace(traceId, result.success)
            result
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            addTraceMetric(traceId, "processing_time_ms", duration)
            stopTrace(traceId, false)
            
            crashlyticsService.recordTransactionError(e, productName)
            throw e
        }
    }
    
    /**
     * Monitor database operations
     */
    fun monitorDatabaseOperation(
        operation: String,
        tableName: String,
        dbOperation: suspend () -> DatabaseResult
    ): suspend () -> DatabaseResult = {
        val traceId = startTrace(TRACE_DATABASE_OPERATION)
        val startTime = System.currentTimeMillis()
        
        try {
            addTraceAttribute(traceId, "operation", operation)
            addTraceAttribute(traceId, "table_name", tableName)
            
            val result = dbOperation()
            val duration = System.currentTimeMillis() - startTime
            
            addTraceMetric(traceId, "processing_time_ms", duration)
            addTraceMetric(traceId, "rows_affected", result.rowsAffected.toLong())
            
            if (duration > THRESHOLD_DATABASE_OPERATION) {
                reportPerformanceIssue(
                    "database_operation_slow",
                    "Database operation took ${duration}ms",
                    mapOf(
                        "duration" to duration.toString(),
                        "operation" to operation,
                        "table" to tableName
                    )
                )
            }
            
            stopTrace(traceId, result.success)
            result
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            addTraceMetric(traceId, "processing_time_ms", duration)
            stopTrace(traceId, false)
            
            crashlyticsService.recordDatabaseError(e, operation, tableName)
            throw e
        }
    }
    
    /**
     * Monitor offline sync performance
     */
    fun monitorOfflineSync(
        itemCount: Int,
        operation: suspend () -> SyncResult
    ): suspend () -> SyncResult = {
        val traceId = startTrace(TRACE_OFFLINE_SYNC)
        val startTime = System.currentTimeMillis()
        
        try {
            addTraceMetric(traceId, "items_to_sync", itemCount.toLong())
            
            val result = operation()
            val duration = System.currentTimeMillis() - startTime
            
            addTraceMetric(traceId, "sync_duration_ms", duration)
            addTraceMetric(traceId, "items_synced", result.itemsSynced.toLong())
            addTraceAttribute(traceId, "success", result.success.toString())
            
            if (duration > THRESHOLD_OFFLINE_SYNC) {
                reportPerformanceIssue(
                    "offline_sync_slow",
                    "Offline sync took ${duration}ms for $itemCount items",
                    mapOf(
                        "duration" to duration.toString(),
                        "item_count" to itemCount.toString()
                    )
                )
            }
            
            // Log sync analytics
            analyticsService.logSyncCompleted(result.itemsSynced, duration, result.success)
            
            stopTrace(traceId, result.success)
            result
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            addTraceMetric(traceId, "sync_duration_ms", duration)
            stopTrace(traceId, false)
            
            crashlyticsService.recordSyncError(e, itemCount, duration)
            throw e
        }
    }
    
    /**
     * Monitor screen loading performance
     */
    fun monitorScreenLoad(screenName: String): ScreenLoadMonitor {
        val traceId = startTrace(TRACE_SCREEN_LOAD)
        addTraceAttribute(traceId, "screen_name", screenName)
        
        return ScreenLoadMonitor(traceId, screenName, this)
    }
    
    /**
     * Report performance issues
     */
    private fun reportPerformanceIssue(
        issueType: String,
        description: String,
        metadata: Map<String, String>
    ) {
        performanceScope.launch {
            // Log to analytics
            analyticsService.logPerformanceIssue(
                issueType,
                0L, // Memory usage would be provided by caller
                0f,  // CPU usage would be provided by caller
                0    // Battery level would be provided by caller
            )
            
            // Log to Crashlytics
            crashlyticsService.log("Performance issue: $issueType - $description")
            metadata.forEach { (key, value) ->
                crashlyticsService.setCustomKey("perf_$key", value)
            }
        }
    }
    
    /**
     * Enable/disable performance monitoring
     */
    fun setPerformanceCollectionEnabled(enabled: Boolean) {
        firebasePerformance.isPerformanceCollectionEnabled = enabled
    }
}

/**
 * Screen load monitor helper class
 */
class ScreenLoadMonitor(
    private val traceId: String,
    private val screenName: String,
    private val performanceService: PerformanceMonitoringService
) {
    private val startTime = System.currentTimeMillis()
    
    fun onContentLoaded() {
        val duration = System.currentTimeMillis() - startTime
        performanceService.addTraceMetric(traceId, "content_load_time_ms", duration)
    }
    
    fun onScreenReady() {
        val duration = System.currentTimeMillis() - startTime
        performanceService.addTraceMetric(traceId, "screen_ready_time_ms", duration)
        
        if (duration > PerformanceMonitoringService.THRESHOLD_SCREEN_LOAD) {
            // Report slow screen load
        }
        
        performanceService.stopTrace(traceId, true)
    }
    
    fun onError(error: Throwable) {
        val duration = System.currentTimeMillis() - startTime
        performanceService.addTraceMetric(traceId, "error_time_ms", duration)
        performanceService.stopTrace(traceId, false)
    }
}

// Result data classes
data class VoiceRecognitionResult(
    val success: Boolean,
    val confidence: Float,
    val text: String? = null
)

data class SpeakerIdentificationResult(
    val speakerId: String?,
    val confidence: Float
)

data class TransactionResult(
    val success: Boolean,
    val transactionId: Long? = null
)

data class DatabaseResult(
    val success: Boolean,
    val rowsAffected: Int
)

data class SyncResult(
    val success: Boolean,
    val itemsSynced: Int
)