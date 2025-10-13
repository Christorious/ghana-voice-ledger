package com.voiceledger.ghana.analytics

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Analytics integration helper to easily integrate analytics throughout the app
 * Provides convenient methods for common analytics operations
 */
@Singleton
class AnalyticsIntegration @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyticsService: AnalyticsService,
    private val crashlyticsService: CrashlyticsService,
    private val performanceMonitoringService: PerformanceMonitoringService,
    private val usageDashboardService: UsageDashboardService
) : DefaultLifecycleObserver {
    
    private val integrationScope = CoroutineScope(Dispatchers.IO)
    private var sessionStartTime: Long = 0
    private var sessionMetrics = SessionMetrics()
    
    /**
     * Initialize analytics integration
     */
    fun initialize() {
        integrationScope.launch {
            // Initialize all analytics services
            analyticsService.initialize()
            crashlyticsService.initialize()
            performanceMonitoringService.initialize()
            usageDashboardService.initialize()
            
            // Register lifecycle observer
            ProcessLifecycleOwner.get().lifecycle.addObserver(this@AnalyticsIntegration)
            
            crashlyticsService.log("Analytics integration initialized")
        }
    }
    
    /**
     * Set user information for analytics
     */
    fun setUser(userId: String, properties: Map<String, String> = emptyMap()) {
        analyticsService.setUserId(userId)
        crashlyticsService.setUserId(userId)
        
        properties.forEach { (key, value) ->
            analyticsService.setUserProperty(key, value)
            crashlyticsService.setCustomKey(key, value)
        }
    }
    
    /**
     * Track transaction creation with full analytics
     */
    fun trackTransactionCreated(
        productName: String,
        amount: Double,
        quantity: Int,
        confidenceScore: Float,
        speakerId: String?,
        language: String?,
        processingTimeMs: Long
    ) {
        integrationScope.launch {
            // Analytics
            analyticsService.logTransactionCreated(
                productName, amount, quantity, confidenceScore, speakerId, language
            )
            
            // Performance monitoring
            val performanceMetrics = PerformanceMetrics(
                operationType = "transaction_creation",
                durationMs = processingTimeMs,
                success = true,
                memoryUsageMb = getMemoryUsage(),
                cpuUsagePercent = getCpuUsage(),
                batteryLevel = getBatteryLevel(),
                networkType = getNetworkType()
            )
            usageDashboardService.recordPerformanceMetrics(performanceMetrics)
            
            // Update session metrics
            sessionMetrics.transactionsCreated++
        }
    }
    
    /**
     * Track voice recognition with performance monitoring
     */
    suspend fun trackVoiceRecognition(
        language: String?,
        audioLengthMs: Long,
        operation: suspend () -> VoiceRecognitionResult
    ): VoiceRecognitionResult {
        val monitoredOperation = performanceMonitoringService.monitorVoiceRecognition(
            language, audioLengthMs, operation
        )
        
        val result = monitoredOperation()
        
        integrationScope.launch {
            analyticsService.logVoiceRecognitionCompleted(
                language, result.confidence, audioLengthMs, result.success
            )
            sessionMetrics.voiceRecognitions++
        }
        
        return result
    }
    
    /**
     * Track speaker identification
     */
    suspend fun trackSpeakerIdentification(
        operation: suspend () -> SpeakerIdentificationResult
    ): SpeakerIdentificationResult {
        val monitoredOperation = performanceMonitoringService.monitorSpeakerIdentification(operation)
        val result = monitoredOperation()
        
        integrationScope.launch {
            analyticsService.logSpeakerIdentified(
                result.speakerId ?: "unknown", result.confidence
            )
            sessionMetrics.speakerIdentifications++
        }
        
        return result
    }
    
    /**
     * Track offline transactions
     */
    fun trackOfflineTransaction(productName: String, amount: Double) {
        integrationScope.launch {
            analyticsService.logOfflineTransaction(productName, amount)
            sessionMetrics.offlineTransactions++
        }
    }
    
    /**
     * Track sync operations
     */
    suspend fun trackSyncOperation(
        itemCount: Int,
        operation: suspend () -> SyncResult
    ): SyncResult {
        val monitoredOperation = performanceMonitoringService.monitorOfflineSync(itemCount, operation)
        val result = monitoredOperation()
        
        integrationScope.launch {
            sessionMetrics.syncOperations++
        }
        
        return result
    }
    
    /**
     * Track errors with context
     */
    fun trackError(
        error: Throwable,
        errorType: String,
        context: String,
        isFatal: Boolean = false
    ) {
        integrationScope.launch {
            // Analytics
            analyticsService.logError(errorType, error.message ?: "Unknown error", context)
            
            // Crashlytics
            crashlyticsService.recordException(error, context)
            
            // Dashboard metrics
            val errorMetrics = ErrorMetrics(
                errorType = errorType,
                errorMessage = error.message ?: "Unknown error",
                context = context,
                userId = getCurrentUserId(),
                isFatal = isFatal
            )
            usageDashboardService.recordErrorMetrics(errorMetrics)
            
            sessionMetrics.errorsEncountered++
        }
    }
    
    /**
     * Track screen views
     */
    fun trackScreenView(screenName: String, screenClass: String) {
        analyticsService.logScreenView(screenName, screenClass)
        
        // Monitor screen load performance
        val screenMonitor = performanceMonitoringService.monitorScreenLoad(screenName)
        // The screen should call screenMonitor.onScreenReady() when fully loaded
    }
    
    /**
     * Track settings changes
     */
    fun trackSettingsChange(settingName: String, settingValue: String) {
        analyticsService.logSettingsChanged(settingName, settingValue)
    }
    
    /**
     * Track privacy consent
     */
    fun trackPrivacyConsent(consentType: String, granted: Boolean) {
        analyticsService.logPrivacyConsentGiven(consentType, granted)
    }
    
    /**
     * Track performance issues
     */
    fun trackPerformanceIssue(
        issueType: String,
        memoryUsage: Long,
        cpuUsage: Float,
        batteryLevel: Int
    ) {
        integrationScope.launch {
            analyticsService.logPerformanceIssue(issueType, memoryUsage, cpuUsage, batteryLevel)
            crashlyticsService.recordPerformanceIssue(issueType, memoryUsage, cpuUsage, batteryLevel)
        }
    }
    
    /**
     * Enable/disable analytics collection based on user consent
     */
    fun setAnalyticsEnabled(enabled: Boolean) {
        analyticsService.setAnalyticsCollectionEnabled(enabled)
        crashlyticsService.setCrashlyticsCollectionEnabled(enabled)
        performanceMonitoringService.setPerformanceCollectionEnabled(enabled)
    }
    
    // Lifecycle callbacks
    override fun onStart(owner: LifecycleOwner) {
        sessionStartTime = System.currentTimeMillis()
        sessionMetrics = SessionMetrics()
        
        analyticsService.logEvent("session_started")
        crashlyticsService.log("Session started")
    }
    
    override fun onStop(owner: LifecycleOwner) {
        val sessionDuration = System.currentTimeMillis() - sessionStartTime
        
        integrationScope.launch {
            // Record session metrics
            val usageMetrics = UsageMetrics(
                userId = getCurrentUserId(),
                sessionDurationMs = sessionDuration,
                transactionsCreated = sessionMetrics.transactionsCreated,
                voiceRecognitions = sessionMetrics.voiceRecognitions,
                speakerIdentifications = sessionMetrics.speakerIdentifications,
                offlineTransactions = sessionMetrics.offlineTransactions,
                syncOperations = sessionMetrics.syncOperations,
                errorsEncountered = sessionMetrics.errorsEncountered
            )
            
            usageDashboardService.recordUsageMetrics(usageMetrics)
            
            analyticsService.logEvent("session_ended", android.os.Bundle().apply {
                putLong("session_duration", sessionDuration)
                putInt("transactions_created", sessionMetrics.transactionsCreated)
                putInt("errors_encountered", sessionMetrics.errorsEncountered)
            })
            
            crashlyticsService.log("Session ended after ${sessionDuration}ms")
        }
    }
    
    // Helper methods
    private fun getCurrentUserId(): String {
        // This would typically come from user session or preferences
        return "user_${System.currentTimeMillis()}"
    }
    
    private fun getMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024) // MB
    }
    
    private fun getCpuUsage(): Float {
        // This would require more complex implementation to get actual CPU usage
        return 0.0f
    }
    
    private fun getBatteryLevel(): Int {
        // This would require battery manager to get actual battery level
        return 100
    }
    
    private fun getNetworkType(): String {
        // This would require connectivity manager to get network type
        return "wifi"
    }
}

/**
 * Session metrics tracking
 */
private data class SessionMetrics(
    var transactionsCreated: Int = 0,
    var voiceRecognitions: Int = 0,
    var speakerIdentifications: Int = 0,
    var offlineTransactions: Int = 0,
    var syncOperations: Int = 0,
    var errorsEncountered: Int = 0
)