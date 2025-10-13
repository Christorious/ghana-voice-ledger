package com.voiceledger.ghana.performance

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.voiceledger.ghana.data.local.database.VoiceLedgerDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Database optimization service for improving query performance and managing database resources
 * Handles query caching, index optimization, and database maintenance
 */
@Singleton
class DatabaseOptimizer @Inject constructor(
    private val database: VoiceLedgerDatabase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _optimizationState = MutableStateFlow(DatabaseOptimizationState())
    val optimizationState: StateFlow<DatabaseOptimizationState> = _optimizationState.asStateFlow()
    
    // Query cache
    private val queryCache = ConcurrentHashMap<String, CachedQuery>()
    private val cacheHits = AtomicLong(0)
    private val cacheMisses = AtomicLong(0)
    
    // Query performance tracking
    private val queryPerformance = ConcurrentHashMap<String, QueryPerformanceStats>()
    
    // Configuration
    private var cacheMaxSize = 100
    private var cacheExpirationMs = 300_000L // 5 minutes
    private var slowQueryThresholdMs = 100L
    private var optimizationEnabled = true
    
    init {
        startOptimizationMonitoring()
        scheduleMaintenanceTasks()
    }
    
    /**
     * Start optimization monitoring
     */
    private fun startOptimizationMonitoring() {
        scope.launch {
            while (isActive && optimizationEnabled) {
                updateOptimizationState()
                cleanupExpiredCache()
                analyzeQueryPerformance()
                delay(30_000) // Check every 30 seconds
            }
        }
    }
    
    /**
     * Schedule database maintenance tasks
     */
    private fun scheduleMaintenanceTasks() {
        scope.launch {
            while (isActive) {
                delay(3_600_000) // Every hour
                performMaintenance()
            }
        }
    }
    
    /**
     * Update optimization state
     */
    private fun updateOptimizationState() {
        val totalQueries = cacheHits.get() + cacheMisses.get()
        val hitRate = if (totalQueries > 0) {
            (cacheHits.get().toFloat() / totalQueries * 100).toInt()
        } else 0
        
        val slowQueries = queryPerformance.values.count { it.averageExecutionTime > slowQueryThresholdMs }
        
        _optimizationState.value = DatabaseOptimizationState(
            cacheSize = queryCache.size,
            cacheHitRate = hitRate,
            totalQueries = totalQueries,
            slowQueryCount = slowQueries,
            averageQueryTime = calculateAverageQueryTime(),
            lastOptimizationTime = System.currentTimeMillis(),
            recommendedIndexes = analyzeIndexRecommendations()
        )
    }
    
    /**
     * Clean up expired cache entries
     */
    private fun cleanupExpiredCache() {
        val currentTime = System.currentTimeMillis()
        val iterator = queryCache.iterator()
        
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (currentTime - entry.value.timestamp > cacheExpirationMs) {
                iterator.remove()
            }
        }
        
        // If cache is still too large, remove oldest entries
        if (queryCache.size > cacheMaxSize) {
            val sortedEntries = queryCache.entries.sortedBy { it.value.timestamp }
            val entriesToRemove = sortedEntries.take(queryCache.size - cacheMaxSize)
            entriesToRemove.forEach { queryCache.remove(it.key) }
        }
    }
    
    /**
     * Analyze query performance and identify slow queries
     */
    private fun analyzeQueryPerformance() {
        queryPerformance.values.forEach { stats ->
            if (stats.averageExecutionTime > slowQueryThresholdMs) {
                // Log slow query for analysis
                println("Slow query detected: ${stats.queryHash} - ${stats.averageExecutionTime}ms")
            }
        }
    }
    
    /**
     * Calculate average query execution time
     */
    private fun calculateAverageQueryTime(): Long {
        return if (queryPerformance.isNotEmpty()) {
            queryPerformance.values.map { it.averageExecutionTime }.average().toLong()
        } else 0L
    }
    
    /**
     * Analyze and recommend database indexes
     */
    private fun analyzeIndexRecommendations(): List<IndexRecommendation> {
        val recommendations = mutableListOf<IndexRecommendation>()
        
        // Analyze slow queries and suggest indexes
        queryPerformance.values.filter { it.averageExecutionTime > slowQueryThresholdMs }
            .forEach { stats ->
                // This would analyze the query pattern and suggest appropriate indexes
                // For now, we'll provide some common recommendations
                recommendations.add(
                    IndexRecommendation(
                        tableName = "transactions",
                        columnNames = listOf("timestamp", "speakerId"),
                        reason = "Improve query performance for transaction filtering",
                        estimatedImprovement = "30-50% faster queries"
                    )
                )
            }
        
        return recommendations
    }
    
    /**
     * Execute query with caching and performance tracking
     */
    suspend fun <T> executeWithOptimization(
        queryKey: String,
        query: suspend () -> T
    ): T {
        val startTime = System.currentTimeMillis()
        
        // Check cache first
        val cachedResult = queryCache[queryKey]
        if (cachedResult != null && !isCacheExpired(cachedResult)) {
            cacheHits.incrementAndGet()
            @Suppress("UNCHECKED_CAST")
            return cachedResult.result as T
        }
        
        // Execute query
        cacheMisses.incrementAndGet()
        val result = query()
        val executionTime = System.currentTimeMillis() - startTime
        
        // Cache result if appropriate
        if (shouldCacheResult(queryKey, executionTime)) {
            queryCache[queryKey] = CachedQuery(
                result = result,
                timestamp = System.currentTimeMillis()
            )
        }
        
        // Track performance
        updateQueryPerformance(queryKey, executionTime)
        
        return result
    }
    
    /**
     * Check if cache entry is expired
     */
    private fun isCacheExpired(cachedQuery: CachedQuery): Boolean {
        return System.currentTimeMillis() - cachedQuery.timestamp > cacheExpirationMs
    }
    
    /**
     * Determine if result should be cached
     */
    private fun shouldCacheResult(queryKey: String, executionTime: Long): Boolean {
        // Cache queries that take longer than 50ms or are frequently accessed
        return executionTime > 50L || queryPerformance[queryKey]?.executionCount ?: 0 > 5
    }
    
    /**
     * Update query performance statistics
     */
    private fun updateQueryPerformance(queryKey: String, executionTime: Long) {
        val stats = queryPerformance.getOrPut(queryKey) {
            QueryPerformanceStats(
                queryHash = queryKey,
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
        
        queryPerformance[queryKey] = stats.copy(
            executionCount = newCount,
            totalExecutionTime = newTotal,
            averageExecutionTime = newAverage,
            minExecutionTime = newMin,
            maxExecutionTime = newMax
        )
    }
    
    /**
     * Perform database maintenance
     */
    private suspend fun performMaintenance() {
        withContext(Dispatchers.IO) {
            try {
                // Vacuum database to reclaim space
                database.openHelper.writableDatabase.execSQL("VACUUM")
                
                // Analyze tables to update statistics
                database.openHelper.writableDatabase.execSQL("ANALYZE")
                
                // Reindex if needed
                reindexIfNeeded()
                
                println("Database maintenance completed")
            } catch (e: Exception) {
                println("Database maintenance failed: ${e.message}")
            }
        }
    }
    
    /**
     * Reindex database if needed
     */
    private fun reindexIfNeeded() {
        val db = database.openHelper.writableDatabase
        
        // Check if reindexing is needed based on query performance
        val needsReindex = queryPerformance.values.any { it.averageExecutionTime > slowQueryThresholdMs * 2 }
        
        if (needsReindex) {
            db.execSQL("REINDEX")
            println("Database reindexed")
        }
    }
    
    /**
     * Create recommended indexes
     */
    suspend fun createRecommendedIndexes() {
        withContext(Dispatchers.IO) {
            val recommendations = _optimizationState.value.recommendedIndexes
            val db = database.openHelper.writableDatabase
            
            recommendations.forEach { recommendation ->
                try {
                    val indexName = "idx_${recommendation.tableName}_${recommendation.columnNames.joinToString("_")}"
                    val columns = recommendation.columnNames.joinToString(", ")
                    val sql = "CREATE INDEX IF NOT EXISTS $indexName ON ${recommendation.tableName} ($columns)"
                    
                    db.execSQL(sql)
                    println("Created index: $indexName")
                } catch (e: Exception) {
                    println("Failed to create index for ${recommendation.tableName}: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Clear query cache
     */
    fun clearCache() {
        queryCache.clear()
        cacheHits.set(0)
        cacheMisses.set(0)
    }
    
    /**
     * Configure optimization parameters
     */
    fun configure(
        cacheMaxSize: Int = this.cacheMaxSize,
        cacheExpirationMs: Long = this.cacheExpirationMs,
        slowQueryThresholdMs: Long = this.slowQueryThresholdMs
    ) {
        this.cacheMaxSize = cacheMaxSize.coerceIn(10, 1000)
        this.cacheExpirationMs = cacheExpirationMs.coerceIn(60_000L, 3_600_000L)
        this.slowQueryThresholdMs = slowQueryThresholdMs.coerceIn(10L, 1000L)
    }
    
    /**
     * Get database size information
     */
    suspend fun getDatabaseSizeInfo(): DatabaseSizeInfo {
        return withContext(Dispatchers.IO) {
            val db = database.openHelper.readableDatabase
            
            // Get database file size
            val dbPath = db.path
            val dbFile = java.io.File(dbPath)
            val dbSizeBytes = if (dbFile.exists()) dbFile.length() else 0L
            
            // Get table sizes (approximate)
            val tableSizes = mutableMapOf<String, Long>()
            val cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)
            
            cursor.use {
                while (it.moveToNext()) {
                    val tableName = it.getString(0)
                    if (!tableName.startsWith("sqlite_") && !tableName.startsWith("android_")) {
                        val countCursor = db.rawQuery("SELECT COUNT(*) FROM $tableName", null)
                        countCursor.use { countCur ->
                            if (countCur.moveToFirst()) {
                                tableSizes[tableName] = countCur.getLong(0)
                            }
                        }
                    }
                }
            }
            
            DatabaseSizeInfo(
                totalSizeBytes = dbSizeBytes,
                tableSizes = tableSizes
            )
        }
    }
    
    /**
     * Get optimization recommendations
     */
    fun getOptimizationRecommendations(): List<String> {
        val state = _optimizationState.value
        val recommendations = mutableListOf<String>()
        
        if (state.cacheHitRate < 50) {
            recommendations.add("Low cache hit rate (${state.cacheHitRate}%). Consider increasing cache size or expiration time.")
        }
        
        if (state.slowQueryCount > 5) {
            recommendations.add("${state.slowQueryCount} slow queries detected. Consider adding indexes or optimizing queries.")
        }
        
        if (state.averageQueryTime > 100) {
            recommendations.add("Average query time is ${state.averageQueryTime}ms. Database optimization recommended.")
        }
        
        if (state.recommendedIndexes.isNotEmpty()) {
            recommendations.add("${state.recommendedIndexes.size} index recommendations available.")
        }
        
        return recommendations
    }
    
    /**
     * Enable or disable optimization
     */
    fun setOptimizationEnabled(enabled: Boolean) {
        optimizationEnabled = enabled
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        scope.cancel()
        queryCache.clear()
        queryPerformance.clear()
    }
}

/**
 * Database optimization state
 */
data class DatabaseOptimizationState(
    val cacheSize: Int = 0,
    val cacheHitRate: Int = 0,
    val totalQueries: Long = 0,
    val slowQueryCount: Int = 0,
    val averageQueryTime: Long = 0,
    val lastOptimizationTime: Long = 0,
    val recommendedIndexes: List<IndexRecommendation> = emptyList()
)

/**
 * Cached query result
 */
data class CachedQuery(
    val result: Any?,
    val timestamp: Long
)

/**
 * Query performance statistics
 */
data class QueryPerformanceStats(
    val queryHash: String,
    val executionCount: Int,
    val totalExecutionTime: Long,
    val averageExecutionTime: Long,
    val minExecutionTime: Long,
    val maxExecutionTime: Long
)

/**
 * Index recommendation
 */
data class IndexRecommendation(
    val tableName: String,
    val columnNames: List<String>,
    val reason: String,
    val estimatedImprovement: String
)

/**
 * Database size information
 */
data class DatabaseSizeInfo(
    val totalSizeBytes: Long,
    val tableSizes: Map<String, Long>
)