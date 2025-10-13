package com.voiceledger.ghana.domain.repository

import com.voiceledger.ghana.data.local.entity.SpeakerProfile
import com.voiceledger.ghana.data.local.dao.CustomerTypeCount
import com.voiceledger.ghana.data.local.dao.LanguageCount
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for speaker profile operations
 * Defines the contract for speaker identification and customer management
 */
interface SpeakerProfileRepository {
    
    // Query operations
    fun getAllActiveProfiles(): Flow<List<SpeakerProfile>>
    suspend fun getSellerProfile(): SpeakerProfile?
    fun getSellerProfileFlow(): Flow<SpeakerProfile?>
    fun getCustomerProfiles(): Flow<List<SpeakerProfile>>
    fun getRegularCustomers(minVisits: Int = 3): Flow<List<SpeakerProfile>>
    suspend fun getProfileById(id: String): SpeakerProfile?
    fun getRecentCustomers(recentThreshold: Long): Flow<List<SpeakerProfile>>
    fun getTopSpendingCustomers(limit: Int): Flow<List<SpeakerProfile>>
    fun getProfilesByLanguage(language: String): Flow<List<SpeakerProfile>>
    fun getNewCustomersSince(timestamp: Long): Flow<List<SpeakerProfile>>
    
    // Analytics operations
    suspend fun getCustomerCount(): Int
    suspend fun getRegularCustomerCount(minVisits: Int = 3): Int
    suspend fun getTotalCustomerSpending(): Double
    suspend fun getAverageCustomerSpending(): Double
    suspend fun getAllLanguages(): List<String>
    suspend fun getCustomerTypeDistribution(): List<CustomerTypeCount>
    suspend fun getLanguageDistribution(): List<LanguageCount>
    suspend fun getNewCustomerCountSince(timestamp: Long): Int
    
    // CRUD operations
    suspend fun insertProfile(profile: SpeakerProfile)
    suspend fun insertProfiles(profiles: List<SpeakerProfile>)
    suspend fun updateProfile(profile: SpeakerProfile)
    suspend fun deleteProfile(profile: SpeakerProfile)
    suspend fun deleteProfileById(id: String)
    suspend fun deactivateProfile(id: String)
    suspend fun reactivateProfile(id: String)
    
    // Speaker identification operations
    suspend fun enrollSeller(voiceEmbedding: FloatArray, name: String?): SpeakerProfile
    suspend fun addCustomerProfile(voiceEmbedding: FloatArray, customerId: String): SpeakerProfile
    suspend fun findSimilarProfiles(voiceEmbedding: FloatArray, threshold: Float = 0.75f): List<SpeakerProfile>
    suspend fun updateVoiceEmbedding(profileId: String, newEmbedding: FloatArray)
    
    // Customer management operations
    suspend fun incrementVisitCount(id: String, timestamp: Long)
    suspend fun addToTotalSpent(id: String, amount: Double)
    suspend fun updateAverageSpending(id: String, average: Double)
    suspend fun updateConfidenceThreshold(id: String, threshold: Float)
    suspend fun updatePreferredLanguage(id: String, language: String)
    suspend fun updateCustomerType(id: String, type: String)
    
    // Sync operations
    suspend fun getUnsyncedProfiles(): List<SpeakerProfile>
    suspend fun markProfilesAsSynced(ids: List<String>)
    
    // Maintenance operations
    suspend fun cleanupInactiveProfiles(cutoffTime: Long)
    suspend fun deactivateOldCustomers(cutoffTime: Long)
}