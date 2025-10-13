package com.voiceledger.ghana.performance

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Performance monitoring service for tracking app performance metrics
 * Monitors frame rates, method execution times, and system performance
 */
@Singleton
class PerformanceMonitor @Inject constructor() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val _performanceState = MutableStateFlow(PerformanceState())
    val performanceState: StateFlow<PerformanceState> = _performanceState.asStateFlow()
    
    // Frame rate monitoring
    private val mainHandler = Handler(Looper.getMainLooper())
    private var frameStartTime = 0L
    private var frameCount = 0
    private var droppedFrames = 0
    private val targetFrameTime = 16_666_667L // 60 FPS in nanoseconds
    
    // Method execution tracking
    private val methodExecutionTimes = ConcurrentHashMap<String, MethodExecutionStats>()
    private val activeOperations = ConcurrentHashMap<String, Long>()
    
    // System performance metrics
    private var cpuUsage = 0.0
    private var memoryPressure = 0
    private val performanceIssues = mutableListOf<PerformanceIssue>()
    
    // Configuration
    private var monitoringEnabled = true
    private var frameRateMonitoringEnabled = true
    private var methodTrackingEnabled = true
    
    init {
        startPerformanceMonitoring()
        if (frameRateMonitoringEnabled) {
            startFrameRateMonitoring()
        }
    }
    
    /**
     * Start performance monitoring
     */
    private fun startPerformanceMonitoring() {
        scope.launch {
            while (isActive && monitoringEnabled) {
                updatePerformanceState()
                analyzePerformanceIssues()
                delay(5_000) // Update every 5 seconds
            }
        }
    }
    
    /**
     * Start frame rate monitoring
     */
    private fun startFrameRateMonitoring() {
        mainHandler.post(object : Runnable {
            override fun run() {
                if (!frameRateMonitoringEnabled) return
                
                val currentTime = SystemClock.elapsedRealtimeNanos()
                
                if (frameStartTime != 0L) {
                    val frameTime = currentTime - frameStartTime
                    frameCount++
                    
                    if (frameTime > targetFrameTime * 1.5) { // Frame took 50% longer than target
                        droppedFrames++
                    }
                }
                
                frameStartTime = currentTime
                mainHandler.post(this)
            }
        })
    }
    
    /**
     * Update performance state
     */
    private fun updatePerformanceState() {
        val currentTime = System.currentTimeMillis()
        
        // Calculate frame rate
        val fps = if (frameCount > 0) {
            val elapsedSeconds = (currentTime - (currentTime - 5000)) / 1000.0
            (frameCount / elapsedSeconds).toInt().coerceAtMost(60)
        } else 60
        
        // Calculate dropped frame percentage
        val droppedFramePercent = if (frameCount > 0) {
            (droppedFrames.toFloat() / frameCount * 100).toInt()
        } else 0
        
        // Get method execution statistics
        val slowMethods = methodExecutionTimes.values.filter { it.averageExecutionTime > 100 }
        
        _performanceState.value = PerformanceState(
            fps = fps,
            droppedFramePercent = droppedFramePercent,
            averageFrameTime = if (frameCount > 0) targetFrameTime / 1_000_000.0 else 16.67,
            slowMethodCount = slowMethods.size,
            activeOperationCount = activeOperations.size,
            cpuUsage = cpuUsage,
            memoryPressure = memoryPressure,
            performanceIssues = performanceIssues.toList(),
            lastUpdateTime = currentTime
        )
        
        // Reset frame counters
        frameCount = 0
        droppedFrames = 0
    }
    
    /**
     * Analyze performance issues
     */
    private fun analyzePerformanceIssues() {
        performanceIssues.clear()
        
        val state = _performanceState.value
        
        // Check frame rate issues
        if (state.fps < 45) {
            performanceIssues.add(
                PerformanceIssue(
                    type = PerformanceIssueType.LOW_FRAME_RATE,
                    severity = if (state.fps < 30) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                    description = "Frame rate is ${state.fps} FPS (target: 60 FPS)",
                    recommendation = "Check for heavy operations on the main thread"
                )
            )
        }
        
        // Check dropped frames
        if (state.droppedFramePercent > 10) {
            performanceIssues.add(
                PerformanceIssue(
                    type = PerformanceIssueType.DROPPED_FRAMES,
                    severity = if (state.droppedFramePercent > 25) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                    description = "${state.droppedFramePercent}% of frames are dropped",
                    recommendation = "Optimize UI rendering and reduce main thread work"
                )
            )
        }
        
        // Check slow methods
        if (state.slowMethodCount > 5) {
            performanceIssues.add(
                PerformanceIssue(
                    type = PerformanceIssueType.SLOW_METHODS,
                    severity = IssueSeverity.MEDIUM,
                    description = "${state.slowMethodCount} methods are executing slowly",
                    recommendation = "Profile and optimize slow methods"
                )
            )
        }
        
        // Check active operations
        if (state.activeOperationCount > 20) {
            performanceIssues.add(
                PerformanceIssue(
                    type = PerformanceIssueType.TOO_MANY_OPERATIONS,
                    severity = IssueSeverity.MEDIUM,
                    description = "${state.activeOperationCount} operations are currently active",
                    recommendation = "Consider throttling or queuing operations"
                )
            )
        }
    }
    
    /**
     * Start tracking method execution
     */
    fun startMethodTracking(methodName: String): String {
        if (!methodTrackingEnabled) return ""
        
        val trackingId = "${methodName}_${System.nanoTime()}"
        activeOperations[trackingId] = System.nanoTime()
        return trackingId
    }
    
    /**
     * End tracking method execution
     */
    fun endMethodTracking(trackingId: String, methodName: String) {
        if (!methodTrackingEnabled || trackingId.isEmpty()) return
        
        val startTime = activeOperations.remove(trackingId) ?: return
        val executionTime = (System.nanoTime() - startTime) / 1_000_000 // Convert to milliseconds
        
        updateMethodExecutionStats(methodName, executionTime)
    }
    
    /**
     * Update method execution statistics
     */
    private fun updateMethodExecutionStats(methodName: String, executionTime: Long) {
        val stats = methodExecutionTimes.getOrPut(methodName) {
            MethodExecutionStats(
                methodName = methodName,
                executionCount = 0,
                totalExecutionTime = 0L,
                averageExecutionTime = 0L,
                minExecutionTime = Long.MAX_VALUE,
                maxExecutionTime = 0L
            )
        }
        
        val newCount = stats.executionCount + 1
        val newTotal = stats.totalExecutionTime + executionTime
        val newAverage = newTotal / newCount
        val newMin = minOf(stats.minExecutionTime, executionTime)
        val newMax = maxOf(stats.maxExecutionTime, executionTime)
        
        methodExecutionTimes[methodName] = stats.copy(
            executionCount = newCount,
            totalExecutionTime = newTotal,
            averageExecutionTime = newAverage,
            minExecutionTime = newMin,
            maxExecutionTime = newMax
        )
    }
    
    /**
     * Track operation with automatic timing
     */
    suspend inline fun <T> trackOperation(operationName: String, operation: suspend () -> T): T {
        val trackingId = startMethodTracking(operationName)
        return try {
            operation()
        } finally {
            endMethodTracking(trackingId, operationName)
        }
    }
    
    /**
     * Get method execution statistics
     */
    fun getMethodExecutionStats(): List<MethodExecutionStats> {
        return methodExecutionTimes.values.sortedByDescending { it.averageExecutionTime }
    }
    
    /**
     * Get slow methods (execution time > threshold)
     */
    fun getSlowMethods(thresholdMs: Long = 100): List<MethodExecutionStats> {
        return methodExecutionTimes.values.filter { it.averageExecutionTime > thresholdMs }
            .sortedByDescending { it.averageExecutionTime }
    }
    
    /**
     * Get performance recommendations
     */
    fun getPerformanceRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val state = _performanceState.value
        
        if (state.fps < 50) {
            recommendations.add("Frame rate is below optimal. Consider reducing main thread work.")
        }
        
        if (state.droppedFramePercent > 5) {
            recommendations.add("High frame drop rate detected. Optimize UI rendering.")
        }
        
        val slowMethods = getSlowMethods(50)
        if (slowMethods.isNotEmpty()) {
            recommendations.add("${slowMethods.size} methods are executing slowly. Consider optimization.")
        }
        
        if (state.activeOperationCount > 15) {
            recommendations.add("Many concurrent operations detected. Consider operation throttling.")
        }
        
        return recommendations
    }
    
    /**
     * Reset performance statistics
     */
    fun resetStatistics() {
        methodExecutionTimes.clear()
        activeOperations.clear()
        frameCount = 0
        droppedFrames = 0
        performanceIssues.clear()
    }
    
    /**
     * Configure monitoring settings
     */
    fun configure(
        monitoringEnabled: Boolean = this.monitoringEnabled,
        frameRateMonitoringEnabled: Boolean = this.frameRateMonitoringEnabled,
        methodTrackingEnabled: Boolean = this.methodTrackingEnabled
    ) {
        this.monitoringEnabled = monitoringEnabled
        this.frameRateMonitoringEnabled = frameRateMonitoringEnabled
        this.methodTrackingEnabled = methodTrackingEnabled
        
        if (!frameRateMonitoringEnabled) {
            mainHandler.removeCallbacksAndMessages(null)
        } else if (frameRateMonitoringEnabled && frameStartTime == 0L) {
            startFrameRateMonitoring()
        }
    }
    
    /**
     * Get performance summary
     */
    fun getPerformanceSummary(): PerformanceSummary {
        val state = _performanceState.value
        val methodStats = getMethodExecutionStats()
        
        return PerformanceSummary(
            overallScore = calculateOverallScore(state),
            fps = state.fps,
            droppedFramePercent = state.droppedFramePercent,
            slowMethodCount = state.slowMethodCount,
            totalMethodCalls = methodStats.sumOf { it.executionCount },
            averageMethodTime = if (methodStats.isNotEmpty()) {
                methodStats.map { it.averageExecutionTime }.average().toLong()
            } else 0L,
            performanceIssueCount = state.performanceIssues.size,
            recommendations = getPerformanceRecommendations()
        )
    }
    
    /**
     * Calculate overall performance score (0-100)
     */
    private fun calculateOverallScore(state: PerformanceState): Int {
        var score = 100
        
        // Frame rate impact (30% of score)
        val fpsScore = (state.fps.toFloat() / 60 * 30).toInt()
        score = score - 30 + fpsScore
        
        // Dropped frames impact (25% of score)
        val droppedFrameScore = ((100 - state.droppedFramePercent) / 100.0 * 25).toInt()
        score = score - 25 + droppedFrameScore
        
        // Method performance impact (25% of score)
        val methodScore = if (state.slowMethodCount == 0) 25 else {
            (25 - (state.slowMethodCount * 2)).coerceAtLeast(0)
        }
        score = score - 25 + methodScore
        
        // Active operations impact (20% of score)
        val operationScore = if (state.activeOperationCount <= 10) 20 else {
            (20 - ((state.activeOperationCount - 10) * 1)).coerceAtLeast(0)
        }
        score = score - 20 + operationScore
        
        return score.coerceIn(0, 100)
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        scope.cancel()
        mainHandler.removeCallbacksAndMessages(null)
        methodExecutionTimes.clear()
        activeOperations.clear()
    }
}

/**
 * Performance state data class
 */
data class PerformanceState(
    val fps: Int = 60,
    val droppedFramePercent: Int = 0,
    val averageFrameTime: Double = 16.67,
    val slowMethodCount: Int = 0,
    val activeOperationCount: Int = 0,
    val cpuUsage: Double = 0.0,
    val memoryPressure: Int = 0,
    val performanceIssues: List<PerformanceIssue> = emptyList(),
    val lastUpdateTime: Long = 0L
)

/**
 * Method execution statistics
 */
data class MethodExecutionStats(
    val methodName: String,
    val executionCount: Int,
    val totalExecutionTime: Long,
    val averageExecutionTime: Long,
    val minExecutionTime: Long,
    val maxExecutionTime: Long
)

/**
 * Performance issue data class
 */
data class PerformanceIssue(
    val type: PerformanceIssueType,
    val severity: IssueSeverity,
    val description: String,
    val recommendation: String
)

/**
 * Performance issue types
 */
enum class PerformanceIssueType {
    LOW_FRAME_RATE,
    DROPPED_FRAMES,
    SLOW_METHODS,
    TOO_MANY_OPERATIONS,
    HIGH_CPU_USAGE,
    MEMORY_PRESSURE
}

/**
 * Issue severity levels
 */
enum class IssueSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Performance summary
 */
data class PerformanceSummary(
    val overallScore: Int,
    val fps: Int,
    val droppedFramePercent: Int,
    val slowMethodCount: Int,
    val totalMethodCalls: Int,
    val averageMethodTime: Long,
    val performanceIssueCount: Int,
    val recommendations: List<String>
)