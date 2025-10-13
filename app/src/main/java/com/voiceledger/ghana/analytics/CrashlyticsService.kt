package com.voiceledger.ghana.analytics

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Crashlytics service for error reporting and monitoring
 * Handles crash reporting, non-fatal exceptions, and custom logging
 */
@Singleton
class CrashlyticsService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val crashlytics: FirebaseCrashlytics by lazy {
        Firebase.crashlytics
    }
    
    private val crashlyticsScope = CoroutineScope(Dispatchers.IO)
    
    /**
     * Initialize Crashlytics service
     */
    fun initialize() {
        crashlyticsScope.launch {
            // Set custom keys for better crash analysis
            crashlytics.setCustomKey("app_version", getAppVersion())
            crashlytics.setCustomKey("device_language", getDeviceLanguage())
            crashlytics.setCustomKey("country", "Ghana")
            crashlytics.setCustomKey("build_type", getBuildType())
            
            // Log initialization
            crashlytics.log("CrashlyticsService initialized")
        }
    }
    
    /**
     * Set user identifier for crash reports
     */
    fun setUserId(userId: String) {
        crashlytics.setUserId(userId)
    }
    
    /**
     * Set custom key-value pairs for crash context
     */
    fun setCustomKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }
    
    fun setCustomKey(key: String, value: Boolean) {
        crashlytics.setCustomKey(key, value)
    }
    
    fun setCustomKey(key: String, value: Int) {
        crashlytics.setCustomKey(key, value)
    }
    
    fun setCustomKey(key: String, value: Long) {
        crashlytics.setCustomKey(key, value)
    }
    
    fun setCustomKey(key: String, value: Float) {
        crashlytics.setCustomKey(key, value)
    }
    
    /**
     * Log custom messages for crash context
     */
    fun log(message: String) {
        crashlytics.log(message)
    }
    
    fun log(priority: Int, tag: String, message: String) {
        crashlytics.log("[$tag] $message")
    }
    
    /**
     * Record non-fatal exceptions
     */
    fun recordException(throwable: Throwable) {
        crashlytics.recordException(throwable)
    }
    
    fun recordException(throwable: Throwable, context: String) {
        crashlytics.setCustomKey("error_context", context)
        crashlytics.recordException(throwable)
    }
    
    /**
     * Record voice recognition errors
     */
    fun recordVoiceRecognitionError(error: Throwable, language: String?, confidence: Float?) {
        crashlytics.setCustomKey("error_type", "voice_recognition")
        language?.let { crashlytics.setCustomKey("language", it) }
        confidence?.let { crashlytics.setCustomKey("confidence", it) }
        crashlytics.recordException(error)
    }
    
    /**
     * Record speaker identification errors
     */
    fun recordSpeakerIdentificationError(error: Throwable, speakerId: String?) {
        crashlytics.setCustomKey("error_type", "speaker_identification")
        speakerId?.let { crashlytics.setCustomKey("speaker_id", it) }
        crashlytics.recordException(error)
    }
    
    /**
     * Record transaction processing errors
     */
    fun recordTransactionError(error: Throwable, transactionData: String?) {
        crashlytics.setCustomKey("error_type", "transaction_processing")
        transactionData?.let { crashlytics.setCustomKey("transaction_data", it.take(100)) }
        crashlytics.recordException(error)
    }
    
    /**
     * Record database errors
     */
    fun recordDatabaseError(error: Throwable, operation: String, tableName: String?) {
        crashlytics.setCustomKey("error_type", "database")
        crashlytics.setCustomKey("database_operation", operation)
        tableName?.let { crashlytics.setCustomKey("table_name", it) }
        crashlytics.recordException(error)
    }
    
    /**
     * Record network errors
     */
    fun recordNetworkError(error: Throwable, endpoint: String?, statusCode: Int?) {
        crashlytics.setCustomKey("error_type", "network")
        endpoint?.let { crashlytics.setCustomKey("endpoint", it) }
        statusCode?.let { crashlytics.setCustomKey("status_code", it) }
        crashlytics.recordException(error)
    }
    
    /**
     * Record security errors
     */
    fun recordSecurityError(error: Throwable, securityContext: String) {
        crashlytics.setCustomKey("error_type", "security")
        crashlytics.setCustomKey("security_context", securityContext)
        crashlytics.recordException(error)
    }
    
    /**
     * Record performance issues
     */
    fun recordPerformanceIssue(
        issueType: String,
        memoryUsage: Long,
        cpuUsage: Float,
        batteryLevel: Int
    ) {
        crashlytics.setCustomKey("issue_type", "performance")
        crashlytics.setCustomKey("performance_issue", issueType)
        crashlytics.setCustomKey("memory_usage", memoryUsage)
        crashlytics.setCustomKey("cpu_usage", cpuUsage)
        crashlytics.setCustomKey("battery_level", batteryLevel)
        
        val performanceException = PerformanceException(
            "Performance issue detected: $issueType. " +
            "Memory: ${memoryUsage}MB, CPU: ${cpuUsage}%, Battery: ${batteryLevel}%"
        )
        crashlytics.recordException(performanceException)
    }
    
    /**
     * Record offline sync issues
     */
    fun recordSyncError(error: Throwable, itemsToSync: Int, syncDuration: Long) {
        crashlytics.setCustomKey("error_type", "sync")
        crashlytics.setCustomKey("items_to_sync", itemsToSync)
        crashlytics.setCustomKey("sync_duration", syncDuration)
        crashlytics.recordException(error)
    }
    
    /**
     * Record ML model errors
     */
    fun recordMLModelError(error: Throwable, modelType: String, inputSize: Int?) {
        crashlytics.setCustomKey("error_type", "ml_model")
        crashlytics.setCustomKey("model_type", modelType)
        inputSize?.let { crashlytics.setCustomKey("input_size", it) }
        crashlytics.recordException(error)
    }
    
    /**
     * Enable/disable crash reporting
     */
    fun setCrashlyticsCollectionEnabled(enabled: Boolean) {
        crashlytics.setCrashlyticsCollectionEnabled(enabled)
    }
    
    /**
     * Check if crash reporting is enabled
     */
    fun isCrashlyticsCollectionEnabled(): Boolean {
        return crashlytics.isCrashlyticsCollectionEnabled
    }
    
    /**
     * Send any unsent crash reports
     */
    fun sendUnsentReports() {
        crashlytics.sendUnsentReports()
    }
    
    /**
     * Delete any unsent crash reports
     */
    fun deleteUnsentReports() {
        crashlytics.deleteUnsentReports()
    }
    
    /**
     * Check if there are unsent crash reports
     */
    fun checkForUnsentReports(callback: (Boolean) -> Unit) {
        crashlytics.checkForUnsentReports().addOnCompleteListener { task ->
            callback(task.result ?: false)
        }
    }
    
    // Helper methods
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    private fun getDeviceLanguage(): String {
        return java.util.Locale.getDefault().language
    }
    
    private fun getBuildType(): String {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            try {
                val applicationInfo = context.applicationInfo
                if ((applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                    "debug"
                } else {
                    "release"
                }
            } catch (e: Exception) {
                "unknown"
            }
        } else {
            "unknown"
        }
    }
}

/**
 * Custom exception for performance issues
 */
class PerformanceException(message: String) : Exception(message)