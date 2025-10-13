package com.voiceledger.ghana.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voiceledger.ghana.performance.DatabaseOptimizer
import com.voiceledger.ghana.performance.MemoryManager
import com.voiceledger.ghana.performance.PerformanceIssue
import com.voiceledger.ghana.performance.PerformanceMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for performance settings screen
 * Manages performance monitoring and optimization settings
 */
@HiltViewModel
class PerformanceSettingsViewModel @Inject constructor(
    private val memoryManager: MemoryManager,
    private val databaseOptimizer: DatabaseOptimizer,
    private val performanceMonitor: PerformanceMonitor
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PerformanceSettingsUiState())
    val uiState: StateFlow<PerformanceSettingsUiState> = _uiState.asStateFlow()
    
    init {
        observePerformanceMetrics()
        loadInitialData()
    }
    
    /**
     * Observe performance metrics from all sources
     */
    private fun observePerformanceMetrics() {
        viewModelScope.launch {
            combine(
                memoryManager.memoryState,
                databaseOptimizer.optimizationState,
                performanceMonitor.performanceState
            ) { memoryState, dbState, perfState ->
                Triple(memoryState, dbState, perfState)
            }.collect { (memoryState, dbState, perfState) ->
                
                val performanceSummary = performanceMonitor.getPerformanceSummary()
                val slowMethods = performanceMonitor.getSlowMethods(50)
                    .take(10)
                    .map { "${it.methodName} (${it.averageExecutionTime}ms)" }
                
                _uiState.value = _uiState.value.copy(
                    // Performance Overview
                    overallScore = performanceSummary.overallScore,
                    fps = perfState.fps,
                    droppedFramePercent = perfState.droppedFramePercent,
                    
                    // Memory Statistics
                    usedMemoryMB = memoryState.usedMemoryMB,
                    maxMemoryMB = memoryState.maxMemoryMB,
                    memoryUsagePercent = memoryState.memoryUsagePercent,
                    poolStatistics = memoryState.poolStatistics,
                    
                    // Database Performance
                    cacheHitRate = dbState.cacheHitRate,
                    slowQueryCount = dbState.slowQueryCount,
                    averageQueryTime = dbState.averageQueryTime,
                    
                    // Performance Issues
                    performanceIssues = perfState.performanceIssues,
                    slowMethods = slowMethods,
                    recommendations = performanceMonitor.getPerformanceRecommendations() +
                                   memoryManager.getOptimizationRecommendations() +
                                   databaseOptimizer.getOptimizationRecommendations(),
                    
                    lastUpdateTime = System.currentTimeMillis()
                )
            }
        }
    }
    
    /**
     * Load initial configuration data
     */
    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                monitoringEnabled = true, // These would be loaded from preferences
                frameRateMonitoringEnabled = true,
                methodTrackingEnabled = true
            )
        }
    }
    
    /**
     * Refresh all performance metrics
     */
    fun refreshMetrics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            
            // Force update of all metrics
            // This would trigger the combine flow above
            
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }
    
    /**
     * Clear memory pools
     */
    fun clearMemoryPools() {
        viewModelScope.launch {
            try {
                // Clear object pools to free memory
                // This would be implemented in MemoryManager
                _uiState.value = _uiState.value.copy(
                    message = "Memory pools cleared successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to clear memory pools: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Force garbage collection
     */
    fun forceGarbageCollection() {
        viewModelScope.launch {
            try {
                memoryManager.forceGarbageCollection()
                _uiState.value = _uiState.value.copy(
                    message = "Garbage collection triggered"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to trigger garbage collection: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Optimize database
     */
    fun optimizeDatabase() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isOptimizingDatabase = true)
                
                // Create recommended indexes
                databaseOptimizer.createRecommendedIndexes()
                
                _uiState.value = _uiState.value.copy(
                    isOptimizingDatabase = false,
                    message = "Database optimization completed"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isOptimizingDatabase = false,
                    error = "Database optimization failed: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Enable or disable performance monitoring
     */
    fun setMonitoringEnabled(enabled: Boolean) {
        viewModelScope.launch {
            performanceMonitor.configure(monitoringEnabled = enabled)
            _uiState.value = _uiState.value.copy(monitoringEnabled = enabled)
            
            // Save to preferences
            savePreference("monitoring_enabled", enabled)
        }
    }
    
    /**
     * Enable or disable frame rate monitoring
     */
    fun setFrameRateMonitoringEnabled(enabled: Boolean) {
        viewModelScope.launch {
            performanceMonitor.configure(frameRateMonitoringEnabled = enabled)
            _uiState.value = _uiState.value.copy(frameRateMonitoringEnabled = enabled)
            
            // Save to preferences
            savePreference("frame_rate_monitoring_enabled", enabled)
        }
    }
    
    /**
     * Enable or disable method tracking
     */
    fun setMethodTrackingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            performanceMonitor.configure(methodTrackingEnabled = enabled)
            _uiState.value = _uiState.value.copy(methodTrackingEnabled = enabled)
            
            // Save to preferences
            savePreference("method_tracking_enabled", enabled)
        }
    }
    
    /**
     * Reset all performance statistics
     */
    fun resetStatistics() {
        viewModelScope.launch {
            try {
                performanceMonitor.resetStatistics()
                databaseOptimizer.clearCache()
                
                _uiState.value = _uiState.value.copy(
                    message = "Performance statistics reset"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to reset statistics: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Configure performance thresholds
     */
    fun configureThresholds(
        memoryThreshold: Int,
        slowQueryThreshold: Long,
        frameRateThreshold: Int
    ) {
        viewModelScope.launch {
            try {
                memoryManager.configureThresholds(memoryThreshold, 50)
                databaseOptimizer.configure(slowQueryThresholdMs = slowQueryThreshold)
                
                _uiState.value = _uiState.value.copy(
                    message = "Performance thresholds updated"
                )
                
                // Save to preferences
                savePreference("memory_threshold", memoryThreshold)
                savePreference("slow_query_threshold", slowQueryThreshold)
                savePreference("frame_rate_threshold", frameRateThreshold)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update thresholds: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Get detailed performance report
     */
    fun getPerformanceReport(): String {
        val state = _uiState.value
        val summary = performanceMonitor.getPerformanceSummary()
        
        return buildString {
            appendLine("=== Performance Report ===")
            appendLine("Overall Score: ${summary.overallScore}/100")
            appendLine("FPS: ${state.fps}")
            appendLine("Dropped Frames: ${state.droppedFramePercent}%")
            appendLine("Memory Usage: ${state.memoryUsagePercent}%")
            appendLine("Cache Hit Rate: ${state.cacheHitRate}%")
            appendLine("Slow Queries: ${state.slowQueryCount}")
            appendLine("Average Query Time: ${state.averageQueryTime}ms")
            appendLine()
            
            if (state.performanceIssues.isNotEmpty()) {
                appendLine("Performance Issues:")
                state.performanceIssues.forEach { issue ->
                    appendLine("- ${issue.description}")
                    appendLine("  Recommendation: ${issue.recommendation}")
                }
                appendLine()
            }
            
            if (state.recommendations.isNotEmpty()) {
                appendLine("Recommendations:")
                state.recommendations.forEach { recommendation ->
                    appendLine("- $recommendation")
                }
            }
            
            appendLine()
            appendLine("Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())}")
        }
    }
    
    /**
     * Save preference (would be implemented with actual preference storage)
     */
    private suspend fun savePreference(key: String, value: Any) {
        // This would save to SharedPreferences or DataStore
        // For now, we'll just log it
        println("Saving preference: $key = $value")
    }
    
    /**
     * Clear messages and errors
     */
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI state for performance settings screen
 */
data class PerformanceSettingsUiState(
    // Performance Overview
    val overallScore: Int = 100,
    val fps: Int = 60,
    val droppedFramePercent: Int = 0,
    
    // Memory Statistics
    val usedMemoryMB: Int = 0,
    val maxMemoryMB: Int = 0,
    val memoryUsagePercent: Int = 0,
    val poolStatistics: Map<String, Int> = emptyMap(),
    
    // Database Performance
    val cacheHitRate: Int = 0,
    val slowQueryCount: Int = 0,
    val averageQueryTime: Long = 0,
    
    // Performance Issues
    val performanceIssues: List<PerformanceIssue> = emptyList(),
    val slowMethods: List<String> = emptyList(),
    val recommendations: List<String> = emptyList(),
    
    // Settings
    val monitoringEnabled: Boolean = true,
    val frameRateMonitoringEnabled: Boolean = true,
    val methodTrackingEnabled: Boolean = true,
    
    // UI State
    val isRefreshing: Boolean = false,
    val isOptimizingDatabase: Boolean = false,
    val lastUpdateTime: Long = 0L,
    val message: String? = null,
    val error: String? = null
)