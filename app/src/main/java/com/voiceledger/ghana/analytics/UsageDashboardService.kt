package com.voiceledger.ghana.analytics

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Usage dashboard service for creating usage dashboards and alerting
 * Aggregates analytics data and provides insights for monitoring
 */
@Singleton
class UsageDashboardService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyticsService: AnalyticsService,
    private val crashlyticsService: CrashlyticsService
) {
    
    private val firestore: FirebaseFirestore by lazy {
        Firebase.firestore
    }
    
    private val dashboardScope = CoroutineScope(Dispatchers.IO)
    
    private val _dashboardData = MutableStateFlow(DashboardData())
    val dashboardData: StateFlow<DashboardData> = _dashboardData.asStateFlow()
    
    private val _alerts = MutableStateFlow<List<Alert>>(emptyList())
    val alerts: StateFlow<List<Alert>> = _alerts.asStateFlow()
    
    companion object {
        private const val COLLECTION_USAGE_METRICS = "usage_metrics"
        private const val COLLECTION_PERFORMANCE_METRICS = "performance_metrics"
        private const val COLLECTION_ERROR_METRICS = "error_metrics"
        private const val COLLECTION_ALERTS = "alerts"
        
        // Alert thresholds
        private const val ERROR_RATE_THRESHOLD = 0.05 // 5%
        private const val CRASH_RATE_THRESHOLD = 0.01 // 1%
        private const val SLOW_OPERATION_THRESHOLD = 0.10 // 10%
        private const val LOW_CONFIDENCE_THRESHOLD = 0.20 // 20%
    }
    
    /**
     * Initialize dashboard service
     */
    fun initialize() {
        dashboardScope.launch {
            startPeriodicDataCollection()
            loadExistingAlerts()
        }
    }
    
    /**
     * Record usage metrics
     */
    fun recordUsageMetrics(metrics: UsageMetrics) {
        dashboardScope.launch {
            try {
                val document = mapOf(
                    "timestamp" to System.currentTimeMillis(),
                    "date" to getCurrentDateString(),
                    "user_id" to metrics.userId,
                    "session_duration" to metrics.sessionDurationMs,
                    "transactions_created" to metrics.transactionsCreated,
                    "voice_recognitions" to metrics.voiceRecognitions,
                    "speaker_identifications" to metrics.speakerIdentifications,
                    "offline_transactions" to metrics.offlineTransactions,
                    "sync_operations" to metrics.syncOperations,
                    "errors_encountered" to metrics.errorsEncountered,
                    "app_version" to getAppVersion(),
                    "device_language" to getDeviceLanguage()
                )
                
                firestore.collection(COLLECTION_USAGE_METRICS)
                    .add(document)
                    .await()
                
                updateDashboardData(metrics)
                
            } catch (e: Exception) {
                crashlyticsService.recordException(e, "usage_metrics_recording")
            }
        }
    }
    
    /**
     * Record performance metrics
     */
    fun recordPerformanceMetrics(metrics: PerformanceMetrics) {
        dashboardScope.launch {
            try {
                val document = mapOf(
                    "timestamp" to System.currentTimeMillis(),
                    "date" to getCurrentDateString(),
                    "operation_type" to metrics.operationType,
                    "duration_ms" to metrics.durationMs,
                    "success" to metrics.success,
                    "memory_usage_mb" to metrics.memoryUsageMb,
                    "cpu_usage_percent" to metrics.cpuUsagePercent,
                    "battery_level" to metrics.batteryLevel,
                    "network_type" to metrics.networkType
                )
                
                firestore.collection(COLLECTION_PERFORMANCE_METRICS)
                    .add(document)
                    .await()
                
                checkPerformanceAlerts(metrics)
                
            } catch (e: Exception) {
                crashlyticsService.recordException(e, "performance_metrics_recording")
            }
        }
    }
    
    /**
     * Record error metrics
     */
    fun recordErrorMetrics(metrics: ErrorMetrics) {
        dashboardScope.launch {
            try {
                val document = mapOf(
                    "timestamp" to System.currentTimeMillis(),
                    "date" to getCurrentDateString(),
                    "error_type" to metrics.errorType,
                    "error_message" to metrics.errorMessage,
                    "context" to metrics.context,
                    "user_id" to metrics.userId,
                    "app_version" to getAppVersion(),
                    "fatal" to metrics.isFatal
                )
                
                firestore.collection(COLLECTION_ERROR_METRICS)
                    .add(document)
                    .await()
                
                checkErrorAlerts(metrics)
                
            } catch (e: Exception) {
                crashlyticsService.recordException(e, "error_metrics_recording")
            }
        }
    }
    
    /**
     * Get dashboard summary for a date range
     */
    suspend fun getDashboardSummary(
        startDate: String,
        endDate: String
    ): DashboardSummary {
        return try {
            val usageMetrics = getUsageMetricsForDateRange(startDate, endDate)
            val performanceMetrics = getPerformanceMetricsForDateRange(startDate, endDate)
            val errorMetrics = getErrorMetricsForDateRange(startDate, endDate)
            
            DashboardSummary(
                dateRange = "$startDate to $endDate",
                totalUsers = usageMetrics.map { it["user_id"] }.distinct().size,
                totalSessions = usageMetrics.size,
                totalTransactions = usageMetrics.sumOf { (it["transactions_created"] as? Long) ?: 0L },
                averageSessionDuration = usageMetrics.mapNotNull { 
                    it["session_duration"] as? Long 
                }.average().toLong(),
                errorRate = calculateErrorRate(usageMetrics.size, errorMetrics.size),
                crashRate = calculateCrashRate(errorMetrics),
                averagePerformance = calculateAveragePerformance(performanceMetrics),
                topErrors = getTopErrors(errorMetrics),
                performanceIssues = getPerformanceIssues(performanceMetrics)
            )
        } catch (e: Exception) {
            crashlyticsService.recordException(e, "dashboard_summary_generation")
            DashboardSummary()
        }
    }
    
    /**
     * Create alert
     */
    fun createAlert(alert: Alert) {
        dashboardScope.launch {
            try {
                val document = mapOf(
                    "id" to alert.id,
                    "type" to alert.type.name,
                    "severity" to alert.severity.name,
                    "title" to alert.title,
                    "message" to alert.message,
                    "timestamp" to alert.timestamp,
                    "resolved" to alert.resolved,
                    "metadata" to alert.metadata
                )
                
                firestore.collection(COLLECTION_ALERTS)
                    .document(alert.id)
                    .set(document)
                    .await()
                
                updateAlertsState()
                
                // Log alert to analytics
                analyticsService.logEvent("alert_created", android.os.Bundle().apply {
                    putString("alert_type", alert.type.name)
                    putString("alert_severity", alert.severity.name)
                    putString("alert_title", alert.title)
                })
                
            } catch (e: Exception) {
                crashlyticsService.recordException(e, "alert_creation")
            }
        }
    }
    
    /**
     * Resolve alert
     */
    fun resolveAlert(alertId: String) {
        dashboardScope.launch {
            try {
                firestore.collection(COLLECTION_ALERTS)
                    .document(alertId)
                    .update("resolved", true)
                    .await()
                
                updateAlertsState()
                
            } catch (e: Exception) {
                crashlyticsService.recordException(e, "alert_resolution")
            }
        }
    }
    
    /**
     * Get real-time metrics
     */
    suspend fun getRealTimeMetrics(): RealTimeMetrics {
        return try {
            val currentData = _dashboardData.value
            val activeAlerts = _alerts.value.filter { !it.resolved }
            
            RealTimeMetrics(
                activeUsers = currentData.activeUsers,
                transactionsPerMinute = currentData.transactionsPerMinute,
                errorRate = currentData.errorRate,
                averageResponseTime = currentData.averageResponseTime,
                activeAlerts = activeAlerts.size,
                systemHealth = calculateSystemHealth(currentData, activeAlerts)
            )
        } catch (e: Exception) {
            crashlyticsService.recordException(e, "real_time_metrics")
            RealTimeMetrics()
        }
    }
    
    // Private helper methods
    private fun startPeriodicDataCollection() {
        // This would typically use a WorkManager or similar for periodic collection
        // For now, we'll just initialize the dashboard data
        _dashboardData.value = DashboardData(
            activeUsers = 0,
            transactionsPerMinute = 0.0,
            errorRate = 0.0,
            averageResponseTime = 0L,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    private suspend fun loadExistingAlerts() {
        try {
            val alertsSnapshot = firestore.collection(COLLECTION_ALERTS)
                .whereEqualTo("resolved", false)
                .get()
                .await()
            
            val alerts = alertsSnapshot.documents.mapNotNull { doc ->
                try {
                    Alert(
                        id = doc.getString("id") ?: "",
                        type = AlertType.valueOf(doc.getString("type") ?: "INFO"),
                        severity = AlertSeverity.valueOf(doc.getString("severity") ?: "LOW"),
                        title = doc.getString("title") ?: "",
                        message = doc.getString("message") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        resolved = doc.getBoolean("resolved") ?: false,
                        metadata = doc.get("metadata") as? Map<String, String> ?: emptyMap()
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            _alerts.value = alerts
            
        } catch (e: Exception) {
            crashlyticsService.recordException(e, "loading_existing_alerts")
        }
    }
    
    private suspend fun getUsageMetricsForDateRange(startDate: String, endDate: String): List<Map<String, Any>> {
        return try {
            val snapshot = firestore.collection(COLLECTION_USAGE_METRICS)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .get()
                .await()
            
            snapshot.documents.map { it.data ?: emptyMap() }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private suspend fun getPerformanceMetricsForDateRange(startDate: String, endDate: String): List<Map<String, Any>> {
        return try {
            val snapshot = firestore.collection(COLLECTION_PERFORMANCE_METRICS)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .get()
                .await()
            
            snapshot.documents.map { it.data ?: emptyMap() }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private suspend fun getErrorMetricsForDateRange(startDate: String, endDate: String): List<Map<String, Any>> {
        return try {
            val snapshot = firestore.collection(COLLECTION_ERROR_METRICS)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .get()
                .await()
            
            snapshot.documents.map { it.data ?: emptyMap() }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun updateDashboardData(metrics: UsageMetrics) {
        val currentData = _dashboardData.value
        _dashboardData.value = currentData.copy(
            activeUsers = currentData.activeUsers + 1,
            transactionsPerMinute = calculateTransactionsPerMinute(metrics),
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    private fun checkPerformanceAlerts(metrics: PerformanceMetrics) {
        if (metrics.durationMs > 5000) { // 5 seconds threshold
            createAlert(
                Alert(
                    id = UUID.randomUUID().toString(),
                    type = AlertType.PERFORMANCE,
                    severity = AlertSeverity.HIGH,
                    title = "Slow Operation Detected",
                    message = "${metrics.operationType} took ${metrics.durationMs}ms",
                    timestamp = System.currentTimeMillis(),
                    metadata = mapOf(
                        "operation" to metrics.operationType,
                        "duration" to metrics.durationMs.toString()
                    )
                )
            )
        }
    }
    
    private fun checkErrorAlerts(metrics: ErrorMetrics) {
        if (metrics.isFatal) {
            createAlert(
                Alert(
                    id = UUID.randomUUID().toString(),
                    type = AlertType.ERROR,
                    severity = AlertSeverity.CRITICAL,
                    title = "Fatal Error Detected",
                    message = "Fatal error: ${metrics.errorType}",
                    timestamp = System.currentTimeMillis(),
                    metadata = mapOf(
                        "error_type" to metrics.errorType,
                        "context" to metrics.context
                    )
                )
            )
        }
    }
    
    private fun updateAlertsState() {
        dashboardScope.launch {
            loadExistingAlerts()
        }
    }
    
    private fun calculateErrorRate(totalSessions: Int, totalErrors: Int): Double {
        return if (totalSessions > 0) totalErrors.toDouble() / totalSessions else 0.0
    }
    
    private fun calculateCrashRate(errorMetrics: List<Map<String, Any>>): Double {
        val totalErrors = errorMetrics.size
        val crashes = errorMetrics.count { (it["fatal"] as? Boolean) == true }
        return if (totalErrors > 0) crashes.toDouble() / totalErrors else 0.0
    }
    
    private fun calculateAveragePerformance(performanceMetrics: List<Map<String, Any>>): Long {
        return performanceMetrics.mapNotNull { it["duration_ms"] as? Long }.average().toLong()
    }
    
    private fun getTopErrors(errorMetrics: List<Map<String, Any>>): List<String> {
        return errorMetrics
            .groupBy { it["error_type"] as? String ?: "unknown" }
            .toList()
            .sortedByDescending { it.second.size }
            .take(5)
            .map { "${it.first} (${it.second.size})" }
    }
    
    private fun getPerformanceIssues(performanceMetrics: List<Map<String, Any>>): List<String> {
        return performanceMetrics
            .filter { (it["duration_ms"] as? Long ?: 0L) > 3000 }
            .groupBy { it["operation_type"] as? String ?: "unknown" }
            .map { "${it.key}: ${it.value.size} slow operations" }
    }
    
    private fun calculateTransactionsPerMinute(metrics: UsageMetrics): Double {
        // This would be calculated based on time windows
        return metrics.transactionsCreated.toDouble()
    }
    
    private fun calculateSystemHealth(data: DashboardData, alerts: List<Alert>): SystemHealth {
        val criticalAlerts = alerts.count { it.severity == AlertSeverity.CRITICAL }
        val highAlerts = alerts.count { it.severity == AlertSeverity.HIGH }
        
        return when {
            criticalAlerts > 0 -> SystemHealth.CRITICAL
            highAlerts > 2 -> SystemHealth.DEGRADED
            data.errorRate > 0.05 -> SystemHealth.DEGRADED
            else -> SystemHealth.HEALTHY
        }
    }
    
    private fun getCurrentDateString(): String {
        return java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    }
    
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    private fun getDeviceLanguage(): String {
        return Locale.getDefault().language
    }
}

// Data classes
@Serializable
data class DashboardData(
    val activeUsers: Int = 0,
    val transactionsPerMinute: Double = 0.0,
    val errorRate: Double = 0.0,
    val averageResponseTime: Long = 0L,
    val lastUpdated: Long = 0L
)

@Serializable
data class UsageMetrics(
    val userId: String,
    val sessionDurationMs: Long,
    val transactionsCreated: Int,
    val voiceRecognitions: Int,
    val speakerIdentifications: Int,
    val offlineTransactions: Int,
    val syncOperations: Int,
    val errorsEncountered: Int
)

@Serializable
data class PerformanceMetrics(
    val operationType: String,
    val durationMs: Long,
    val success: Boolean,
    val memoryUsageMb: Long,
    val cpuUsagePercent: Float,
    val batteryLevel: Int,
    val networkType: String
)

@Serializable
data class ErrorMetrics(
    val errorType: String,
    val errorMessage: String,
    val context: String,
    val userId: String,
    val isFatal: Boolean
)

@Serializable
data class Alert(
    val id: String,
    val type: AlertType,
    val severity: AlertSeverity,
    val title: String,
    val message: String,
    val timestamp: Long,
    val resolved: Boolean = false,
    val metadata: Map<String, String> = emptyMap()
)

enum class AlertType {
    ERROR, PERFORMANCE, SECURITY, USAGE, INFO
}

enum class AlertSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class SystemHealth {
    HEALTHY, DEGRADED, CRITICAL
}

@Serializable
data class DashboardSummary(
    val dateRange: String = "",
    val totalUsers: Int = 0,
    val totalSessions: Int = 0,
    val totalTransactions: Long = 0L,
    val averageSessionDuration: Long = 0L,
    val errorRate: Double = 0.0,
    val crashRate: Double = 0.0,
    val averagePerformance: Long = 0L,
    val topErrors: List<String> = emptyList(),
    val performanceIssues: List<String> = emptyList()
)

@Serializable
data class RealTimeMetrics(
    val activeUsers: Int = 0,
    val transactionsPerMinute: Double = 0.0,
    val errorRate: Double = 0.0,
    val averageResponseTime: Long = 0L,
    val activeAlerts: Int = 0,
    val systemHealth: SystemHealth = SystemHealth.HEALTHY
)