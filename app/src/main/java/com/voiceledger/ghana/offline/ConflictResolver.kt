package com.voiceledger.ghana.offline

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles conflict resolution when local and remote data differ
 * Provides strategies for resolving data conflicts during sync operations
 */
@Singleton
class ConflictResolver @Inject constructor() {
    
    private val _conflictState = MutableStateFlow(ConflictState())
    val conflictState: StateFlow<ConflictState> = _conflictState.asStateFlow()
    
    private val pendingConflicts = mutableMapOf<String, DataConflict>()
    
    /**
     * Resolve conflict between local and remote data
     */
    suspend fun <T> resolveConflict(
        conflictId: String,
        localData: T,
        remoteData: T,
        strategy: ConflictResolutionStrategy,
        metadata: ConflictMetadata
    ): ConflictResolution<T> {
        
        val conflict = DataConflict(
            id = conflictId,
            entityType = metadata.entityType,
            entityId = metadata.entityId,
            localTimestamp = metadata.localTimestamp,
            remoteTimestamp = metadata.remoteTimestamp,
            conflictType = determineConflictType(localData, remoteData, metadata),
            strategy = strategy
        )
        
        return when (strategy) {
            ConflictResolutionStrategy.LOCAL_WINS -> {
                ConflictResolution.Resolved(localData, "Local version preserved")
            }
            
            ConflictResolutionStrategy.REMOTE_WINS -> {
                ConflictResolution.Resolved(remoteData, "Remote version accepted")
            }
            
            ConflictResolutionStrategy.TIMESTAMP_WINS -> {
                if (metadata.localTimestamp > metadata.remoteTimestamp) {
                    ConflictResolution.Resolved(localData, "Local version is newer")
                } else {
                    ConflictResolution.Resolved(remoteData, "Remote version is newer")
                }
            }
            
            ConflictResolutionStrategy.MERGE -> {
                val mergedData = attemptMerge(localData, remoteData, metadata)
                if (mergedData != null) {
                    ConflictResolution.Resolved(mergedData, "Data merged successfully")
                } else {
                    // Merge failed, require manual resolution
                    pendingConflicts[conflictId] = conflict
                    updateConflictState()
                    ConflictResolution.RequiresManualResolution(conflict)
                }
            }
            
            ConflictResolutionStrategy.MANUAL -> {
                pendingConflicts[conflictId] = conflict
                updateConflictState()
                ConflictResolution.RequiresManualResolution(conflict)
            }
        }
    }
    
    /**
     * Attempt automatic merge of conflicting data
     */
    private fun <T> attemptMerge(
        localData: T,
        remoteData: T,
        metadata: ConflictMetadata
    ): T? {
        return when (metadata.entityType) {
            "Transaction" -> mergeTransactions(localData, remoteData)
            "DailySummary" -> mergeDailySummaries(localData, remoteData)
            "SpeakerProfile" -> mergeSpeakerProfiles(localData, remoteData)
            else -> null // Unknown type, cannot merge
        }
    }
    
    /**
     * Merge transaction data
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T> mergeTransactions(localData: T, remoteData: T): T? {
        // Implementation would merge transaction fields intelligently
        // For now, return null to indicate merge not possible
        return null
    }
    
    /**
     * Merge daily summary data
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T> mergeDailySummaries(localData: T, remoteData: T): T? {
        // Implementation would merge summary fields
        // Prefer local calculations but accept remote metadata
        return null
    }
    
    /**
     * Merge speaker profile data
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T> mergeSpeakerProfiles(localData: T, remoteData: T): T? {
        // Implementation would merge profile data
        // Prefer local voice data but accept remote settings
        return null
    }
    
    /**
     * Determine the type of conflict
     */
    private fun <T> determineConflictType(
        localData: T,
        remoteData: T,
        metadata: ConflictMetadata
    ): ConflictType {
        return when {
            localData == null && remoteData != null -> ConflictType.REMOTE_CREATED
            localData != null && remoteData == null -> ConflictType.LOCAL_CREATED
            localData != remoteData -> {
                if (metadata.localTimestamp > metadata.remoteTimestamp) {
                    ConflictType.LOCAL_MODIFIED
                } else {
                    ConflictType.REMOTE_MODIFIED
                }
            }
            else -> ConflictType.NO_CONFLICT
        }
    }
    
    /**
     * Manually resolve a pending conflict
     */
    suspend fun <T> manuallyResolveConflict(
        conflictId: String,
        resolution: ManualResolution<T>
    ): Boolean {
        val conflict = pendingConflicts[conflictId] ?: return false
        
        // Apply the manual resolution
        when (resolution) {
            is ManualResolution.ChooseLocal -> {
                // Keep local version
            }
            is ManualResolution.ChooseRemote -> {
                // Accept remote version
            }
            is ManualResolution.CustomMerge -> {
                // Use custom merged data
            }
        }
        
        // Remove from pending conflicts
        pendingConflicts.remove(conflictId)
        updateConflictState()
        
        return true
    }
    
    /**
     * Get all pending conflicts
     */
    fun getPendingConflicts(): List<DataConflict> {
        return pendingConflicts.values.toList()
    }
    
    /**
     * Get conflicts by entity type
     */
    fun getConflictsByType(entityType: String): List<DataConflict> {
        return pendingConflicts.values.filter { it.entityType == entityType }
    }
    
    /**
     * Clear all resolved conflicts
     */
    fun clearResolvedConflicts() {
        pendingConflicts.clear()
        updateConflictState()
    }
    
    /**
     * Get recommended resolution strategy based on conflict type and context
     */
    fun getRecommendedStrategy(
        conflictType: ConflictType,
        entityType: String,
        userPreferences: ConflictPreferences
    ): ConflictResolutionStrategy {
        return when (conflictType) {
            ConflictType.NO_CONFLICT -> ConflictResolutionStrategy.LOCAL_WINS
            
            ConflictType.LOCAL_CREATED -> ConflictResolutionStrategy.LOCAL_WINS
            
            ConflictType.REMOTE_CREATED -> ConflictResolutionStrategy.REMOTE_WINS
            
            ConflictType.LOCAL_MODIFIED, ConflictType.REMOTE_MODIFIED -> {
                when (userPreferences.defaultStrategy) {
                    ConflictResolutionStrategy.TIMESTAMP_WINS -> ConflictResolutionStrategy.TIMESTAMP_WINS
                    ConflictResolutionStrategy.MERGE -> {
                        if (canAutoMerge(entityType)) {
                            ConflictResolutionStrategy.MERGE
                        } else {
                            ConflictResolutionStrategy.MANUAL
                        }
                    }
                    else -> userPreferences.defaultStrategy
                }
            }
        }
    }
    
    /**
     * Check if entity type supports automatic merging
     */
    private fun canAutoMerge(entityType: String): Boolean {
        return when (entityType) {
            "Transaction" -> false // Transactions should not be auto-merged
            "DailySummary" -> true // Summaries can be recalculated
            "SpeakerProfile" -> true // Profiles can be merged
            else -> false
        }
    }
    
    /**
     * Update conflict state
     */
    private fun updateConflictState() {
        val conflicts = pendingConflicts.values.toList()
        val byType = conflicts.groupBy { it.entityType }
        
        _conflictState.value = ConflictState(
            totalConflicts = conflicts.size,
            conflictsByType = byType.mapValues { it.value.size },
            oldestConflictTime = conflicts.minOfOrNull { it.localTimestamp } ?: 0L,
            lastUpdateTime = System.currentTimeMillis()
        )
    }
}

/**
 * Conflict resolution result
 */
sealed class ConflictResolution<T> {
    data class Resolved<T>(val data: T, val reason: String) : ConflictResolution<T>()
    data class RequiresManualResolution<T>(val conflict: DataConflict) : ConflictResolution<T>()
}

/**
 * Manual resolution options
 */
sealed class ManualResolution<T> {
    data class ChooseLocal<T>(val data: T) : ManualResolution<T>()
    data class ChooseRemote<T>(val data: T) : ManualResolution<T>()
    data class CustomMerge<T>(val mergedData: T) : ManualResolution<T>()
}

/**
 * Data conflict information
 */
@Serializable
data class DataConflict(
    val id: String,
    val entityType: String,
    val entityId: String,
    val localTimestamp: Long,
    val remoteTimestamp: Long,
    val conflictType: ConflictType,
    val strategy: ConflictResolutionStrategy
)

/**
 * Conflict metadata
 */
data class ConflictMetadata(
    val entityType: String,
    val entityId: String,
    val localTimestamp: Long,
    val remoteTimestamp: Long,
    val localVersion: String? = null,
    val remoteVersion: String? = null
)

/**
 * Conflict resolution strategies
 */
@Serializable
enum class ConflictResolutionStrategy {
    LOCAL_WINS,      // Always prefer local data
    REMOTE_WINS,     // Always prefer remote data
    TIMESTAMP_WINS,  // Prefer newer data based on timestamp
    MERGE,           // Attempt to merge data automatically
    MANUAL           // Require manual resolution
}

/**
 * Types of conflicts
 */
@Serializable
enum class ConflictType {
    NO_CONFLICT,
    LOCAL_CREATED,
    REMOTE_CREATED,
    LOCAL_MODIFIED,
    REMOTE_MODIFIED
}

/**
 * User preferences for conflict resolution
 */
data class ConflictPreferences(
    val defaultStrategy: ConflictResolutionStrategy = ConflictResolutionStrategy.TIMESTAMP_WINS,
    val autoResolveTransactions: Boolean = false,
    val autoResolveSummaries: Boolean = true,
    val autoResolveProfiles: Boolean = true
)

/**
 * Current conflict state
 */
data class ConflictState(
    val totalConflicts: Int = 0,
    val conflictsByType: Map<String, Int> = emptyMap(),
    val oldestConflictTime: Long = 0L,
    val lastUpdateTime: Long = 0L
)