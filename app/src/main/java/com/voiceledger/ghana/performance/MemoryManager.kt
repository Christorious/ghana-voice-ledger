package com.voiceledger.ghana.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Memory management service for monitoring and optimizing memory usage
 * Provides memory leak detection, garbage collection optimization, and memory statistics
 */
@Singleton
class MemoryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    private val _memoryState = MutableStateFlow(MemoryState())
    val memoryState: StateFlow<MemoryState> = _memoryState.asStateFlow()
    
    // Object pools
    private val audioBufferPool = AudioBufferPool(bufferSize = 4096, maxPoolSize = 20)
    private val floatArrayPool = FloatArrayPool(arraySize = 4096, maxPoolSize = 15)
    private val stringBuilderPool = StringBuilderPool(initialCapacity = 256, maxPoolSize = 10)
    private val poolManager = PoolManager()
    
    // Memory leak detection
    private val trackedObjects = ConcurrentHashMap<String, WeakReference<Any>>()
    private val allocationTracker = ConcurrentHashMap<String, AllocationInfo>()
    
    // Configuration
    private var memoryThresholdPercent = 80 // Trigger cleanup at 80% memory usage
    private var gcThresholdMB = 50 // Trigger GC when 50MB of garbage is detected
    private var monitoringEnabled = true
    
    init {
        initializePools()
        startMemoryMonitoring()
    }
    
    /**
     * Initialize object pools
     */
    private fun initializePools() {
        scope.launch {
            poolManager.registerPool("audioBuffer", audioBufferPool.pool)
            poolManager.registerPool("floatArray", floatArrayPool.pool)
            poolManager.registerPool("stringBuilder", stringBuilderPool.pool)
        }
    }
    
    /**
     * Start memory monitoring
     */
    private fun startMemoryMonitoring() {
        scope.launch {
            while (isActive && monitoringEnabled) {
                updateMemoryState()
                checkMemoryPressure()
                cleanupWeakReferences()
                delay(10_000) // Check every 10 seconds
            }
        }
    }
    
    /**
     * Update current memory state
     */
    private fun updateMemoryState() {
        val runtime = Runtime.getRuntime()
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory()
        
        val memoryUsagePercent = (usedMemory.toFloat() / maxMemory * 100).toInt()
        
        val nativeHeapSize = Debug.getNativeHeapSize()
        val nativeHeapAllocated = Debug.getNativeHeapAllocatedSize()
        val nativeHeapFree = Debug.getNativeHeapFreeSize()
        
        _memoryState.value = MemoryState(
            totalMemoryMB = (totalMemory / 1024 / 1024).toInt(),
            usedMemoryMB = (usedMemory / 1024 / 1024).toInt(),
            freeMemoryMB = (freeMemory / 1024 / 1024).toInt(),
            maxMemoryMB = (maxMemory / 1024 / 1024).toInt(),
            memoryUsagePercent = memoryUsagePercent,
            availableSystemMemoryMB = (memInfo.availMem / 1024 / 1024).toInt(),
            totalSystemMemoryMB = (memInfo.totalMem / 1024 / 1024).toInt(),
            isLowMemory = memInfo.lowMemory,
            nativeHeapSizeMB = (nativeHeapSize / 1024 / 1024).toInt(),
            nativeHeapAllocatedMB = (nativeHeapAllocated / 1024 / 1024).toInt(),
            nativeHeapFreeMB = (nativeHeapFree / 1024 / 1024).toInt(),
            poolStatistics = getPoolStatistics(),
            trackedObjectCount = trackedObjects.size,
            lastUpdateTime = System.currentTimeMillis()
        )
    }
    
    /**
     * Check for memory pressure and trigger cleanup if needed
     */
    private suspend fun checkMemoryPressure() {
        val state = _memoryState.value
        
        when {
            state.memoryUsagePercent >= 90 -> {
                // Critical memory pressure
                performAggressiveCleanup()
            }
            state.memoryUsagePercent >= memoryThresholdPercent -> {
                // High memory pressure
                performMemoryCleanup()
            }
            state.isLowMemory -> {
                // System-wide low memory
                performSystemMemoryCleanup()
            }
        }
    }
    
    /**
     * Perform memory cleanup
     */
    private suspend fun performMemoryCleanup() {
        // Clear object pools partially
        clearPoolsPartially()
        
        // Suggest garbage collection
        System.gc()
        
        // Clear weak references
        cleanupWeakReferences()
        
        // Update state
        updateMemoryState()
    }
    
    /**
     * Perform aggressive memory cleanup
     */
    private suspend fun performAggressiveCleanup() {
        // Clear all object pools
        poolManager.clearAll()
        
        // Clear tracked objects
        trackedObjects.clear()
        allocationTracker.clear()
        
        // Force garbage collection
        System.gc()
        System.runFinalization()
        System.gc()
        
        // Update state
        updateMemoryState()
    }
    
    /**
     * Perform system memory cleanup
     */
    private suspend fun performSystemMemoryCleanup() {
        // Minimal cleanup to preserve functionality
        clearPoolsPartially()
        System.gc()
        updateMemoryState()
    }
    
    /**
     * Clear object pools partially
     */
    private suspend fun clearPoolsPartially() {
        // Keep some objects in pools but reduce size
        val stats = poolManager.getStatistics()
        stats.forEach { (name, size) ->
            if (size > 5) {
                // Clear half of the pool
                repeat(size / 2) {
                    when (name) {
                        "audioBuffer" -> audioBufferPool.acquire().let { audioBufferPool.release(it) }
                        "floatArray" -> floatArrayPool.acquire().let { floatArrayPool.release(it) }
                        "stringBuilder" -> stringBuilderPool.acquire().let { stringBuilderPool.release(it) }
                    }
                }
            }
        }
    }
    
    /**
     * Clean up weak references
     */
    private fun cleanupWeakReferences() {
        val iterator = trackedObjects.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.get() == null) {
                iterator.remove()
            }
        }
    }
    
    /**
     * Get object pool statistics
     */
    private suspend fun getPoolStatistics(): Map<String, Int> {
        return poolManager.getStatistics()
    }
    
    // Public API
    
    /**
     * Get audio buffer from pool
     */
    suspend fun getAudioBuffer(): ByteArray = audioBufferPool.acquire()
    
    /**
     * Return audio buffer to pool
     */
    suspend fun releaseAudioBuffer(buffer: ByteArray) = audioBufferPool.release(buffer)
    
    /**
     * Get float array from pool
     */
    suspend fun getFloatArray(): FloatArray = floatArrayPool.acquire()
    
    /**
     * Return float array to pool
     */
    suspend fun releaseFloatArray(array: FloatArray) = floatArrayPool.release(array)
    
    /**
     * Get StringBuilder from pool
     */
    suspend fun getStringBuilder(): StringBuilder = stringBuilderPool.acquire()
    
    /**
     * Return StringBuilder to pool
     */
    suspend fun releaseStringBuilder(sb: StringBuilder) = stringBuilderPool.release(sb)
    
    /**
     * Track an object for memory leak detection
     */
    fun trackObject(key: String, obj: Any) {
        trackedObjects[key] = WeakReference(obj)
        allocationTracker[key] = AllocationInfo(
            className = obj.javaClass.simpleName,
            timestamp = System.currentTimeMillis(),
            threadName = Thread.currentThread().name
        )
    }
    
    /**
     * Untrack an object
     */
    fun untrackObject(key: String) {
        trackedObjects.remove(key)
        allocationTracker.remove(key)
    }
    
    /**
     * Get memory leak suspects (objects that should have been garbage collected)
     */
    fun getMemoryLeakSuspects(): List<MemoryLeakSuspect> {
        val currentTime = System.currentTimeMillis()
        val suspects = mutableListOf<MemoryLeakSuspect>()
        
        allocationTracker.forEach { (key, info) ->
            val ref = trackedObjects[key]
            if (ref != null && ref.get() != null && (currentTime - info.timestamp) > 300_000) { // 5 minutes
                suspects.add(
                    MemoryLeakSuspect(
                        key = key,
                        className = info.className,
                        ageMs = currentTime - info.timestamp,
                        threadName = info.threadName
                    )
                )
            }
        }
        
        return suspects
    }
    
    /**
     * Force garbage collection
     */
    fun forceGarbageCollection() {
        scope.launch {
            System.gc()
            System.runFinalization()
            System.gc()
            delay(1000)
            updateMemoryState()
        }
    }
    
    /**
     * Configure memory thresholds
     */
    fun configureThresholds(memoryThresholdPercent: Int, gcThresholdMB: Int) {
        this.memoryThresholdPercent = memoryThresholdPercent.coerceIn(50, 95)
        this.gcThresholdMB = gcThresholdMB.coerceIn(10, 200)
    }
    
    /**
     * Enable or disable memory monitoring
     */
    fun setMonitoringEnabled(enabled: Boolean) {
        monitoringEnabled = enabled
        if (enabled && !scope.isActive) {
            startMemoryMonitoring()
        }
    }
    
    /**
     * Get memory optimization recommendations
     */
    fun getOptimizationRecommendations(): List<String> {
        val state = _memoryState.value
        val recommendations = mutableListOf<String>()
        
        if (state.memoryUsagePercent > 80) {
            recommendations.add("High memory usage detected. Consider reducing audio buffer sizes.")
        }
        
        if (state.nativeHeapAllocatedMB > state.nativeHeapSizeMB * 0.8) {
            recommendations.add("Native heap usage is high. Check for native memory leaks.")
        }
        
        val leakSuspects = getMemoryLeakSuspects()
        if (leakSuspects.isNotEmpty()) {
            recommendations.add("${leakSuspects.size} potential memory leaks detected.")
        }
        
        if (state.poolStatistics.values.sum() < 5) {
            recommendations.add("Object pools are underutilized. Consider adjusting pool sizes.")
        }
        
        return recommendations
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        scope.cancel()
        scope.launch {
            poolManager.clearAll()
            trackedObjects.clear()
            allocationTracker.clear()
        }
    }
}

/**
 * Memory state data class
 */
data class MemoryState(
    val totalMemoryMB: Int = 0,
    val usedMemoryMB: Int = 0,
    val freeMemoryMB: Int = 0,
    val maxMemoryMB: Int = 0,
    val memoryUsagePercent: Int = 0,
    val availableSystemMemoryMB: Int = 0,
    val totalSystemMemoryMB: Int = 0,
    val isLowMemory: Boolean = false,
    val nativeHeapSizeMB: Int = 0,
    val nativeHeapAllocatedMB: Int = 0,
    val nativeHeapFreeMB: Int = 0,
    val poolStatistics: Map<String, Int> = emptyMap(),
    val trackedObjectCount: Int = 0,
    val lastUpdateTime: Long = 0L
)

/**
 * Allocation tracking information
 */
data class AllocationInfo(
    val className: String,
    val timestamp: Long,
    val threadName: String
)

/**
 * Memory leak suspect information
 */
data class MemoryLeakSuspect(
    val key: String,
    val className: String,
    val ageMs: Long,
    val threadName: String
)