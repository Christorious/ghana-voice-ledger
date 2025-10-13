package com.voiceledger.ghana.data.local.dao

import androidx.room.*
import com.voiceledger.ghana.data.local.entity.SpeakerProfile
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for SpeakerProfile entity
 * Provides queries for speaker identification and customer management
 */
@Dao
interface SpeakerProfileDao {
    
    @Query("SELECT * FROM speaker_profiles WHERE isActive = 1 ORDER BY lastVisit DESC")
    fun getAllActiveProfiles(): Flow<List<SpeakerProfile>>
    
    @Query("SELECT * FROM speaker_profiles WHERE isSeller = 1 AND isActive = 1 LIMIT 1")
    suspend fun getSellerProfile(): SpeakerProfile?
    
    @Query("SELECT * FROM speaker_profiles WHERE isSeller = 1 AND isActive = 1 LIMIT 1")
    fun getSellerProfileFlow(): Flow<SpeakerProfile?>
    
    @Query("SELECT * FROM speaker_profiles WHERE isSeller = 0 AND isActive = 1 ORDER BY visitCount DESC")
    fun getCustomerProfiles(): Flow<List<SpeakerProfile>>
    
    @Query("SELECT * FROM speaker_profiles WHERE isSeller = 0 AND visitCount >= :minVisits AND isActive = 1 ORDER BY visitCount DESC")
    fun getRegularCustomers(minVisits: Int = 3): Flow<List<SpeakerProfile>>
    
    @Query("SELECT * FROM speaker_profiles WHERE id = :id AND isActive = 1")
    suspend fun getProfileById(id: String): SpeakerProfile?
    
    @Query("SELECT * FROM speaker_profiles WHERE synced = 0 AND isActive = 1 ORDER BY createdAt ASC")
    suspend fun getUnsyncedProfiles(): List<SpeakerProfile>
    
    @Query("SELECT * FROM speaker_profiles WHERE isSeller = 0 AND lastVisit >= :recentThreshold AND isActive = 1 ORDER BY lastVisit DESC")
    fun getRecentCustomers(recentThreshold: Long): Flow<List<SpeakerProfile>>
    
    @Query("SELECT COUNT(*) FROM speaker_profiles WHERE isSeller = 0 AND isActive = 1")
    suspend fun getCustomerCount(): Int
    
    @Query("SELECT COUNT(*) FROM speaker_profiles WHERE isSeller = 0 AND visitCount >= :minVisits AND isActive = 1")
    suspend fun getRegularCustomerCount(minVisits: Int = 3): Int
    
    @Query("SELECT SUM(totalSpent) FROM speaker_profiles WHERE isSeller = 0 AND isActive = 1")
    suspend fun getTotalCustomerSpending(): Double?
    
    @Query("SELECT AVG(averageSpending) FROM speaker_profiles WHERE isSeller = 0 AND averageSpending IS NOT NULL AND isActive = 1")
    suspend fun getAverageCustomerSpending(): Double?
    
    @Query("SELECT * FROM speaker_profiles WHERE isSeller = 0 AND totalSpent IS NOT NULL AND isActive = 1 ORDER BY totalSpent DESC LIMIT :limit")
    fun getTopSpendingCustomers(limit: Int): Flow<List<SpeakerProfile>>
    
    @Query("SELECT * FROM speaker_profiles WHERE preferredLanguage = :language AND isActive = 1")
    fun getProfilesByLanguage(language: String): Flow<List<SpeakerProfile>>
    
    @Query("SELECT DISTINCT preferredLanguage FROM speaker_profiles WHERE preferredLanguage IS NOT NULL AND isActive = 1")
    suspend fun getAllLanguages(): List<String>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: SpeakerProfile)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<SpeakerProfile>)
    
    @Update
    suspend fun updateProfile(profile: SpeakerProfile)
    
    @Delete
    suspend fun deleteProfile(profile: SpeakerProfile)
    
    @Query("UPDATE speaker_profiles SET isActive = 0 WHERE id = :id")
    suspend fun deactivateProfile(id: String)
    
    @Query("UPDATE speaker_profiles SET isActive = 1 WHERE id = :id")
    suspend fun reactivateProfile(id: String)
    
    @Query("DELETE FROM speaker_profiles WHERE id = :id")
    suspend fun deleteProfileById(id: String)
    
    @Query("UPDATE speaker_profiles SET synced = 1 WHERE id IN (:ids)")
    suspend fun markProfilesAsSynced(ids: List<String>)
    
    @Query("UPDATE speaker_profiles SET visitCount = visitCount + 1, lastVisit = :timestamp WHERE id = :id")
    suspend fun incrementVisitCount(id: String, timestamp: Long)
    
    @Query("UPDATE speaker_profiles SET totalSpent = COALESCE(totalSpent, 0) + :amount WHERE id = :id")
    suspend fun addToTotalSpent(id: String, amount: Double)
    
    @Query("UPDATE speaker_profiles SET averageSpending = :average WHERE id = :id")
    suspend fun updateAverageSpending(id: String, average: Double)
    
    @Query("UPDATE speaker_profiles SET confidenceThreshold = :threshold WHERE id = :id")
    suspend fun updateConfidenceThreshold(id: String, threshold: Float)
    
    @Query("UPDATE speaker_profiles SET preferredLanguage = :language WHERE id = :id")
    suspend fun updatePreferredLanguage(id: String, language: String)
    
    @Query("UPDATE speaker_profiles SET customerType = :type WHERE id = :id")
    suspend fun updateCustomerType(id: String, type: String)
    
    // Analytics queries
    @Query("""
        SELECT customerType, COUNT(*) as count 
        FROM speaker_profiles 
        WHERE isSeller = 0 AND customerType IS NOT NULL AND isActive = 1 
        GROUP BY customerType 
        ORDER BY count DESC
    """)
    suspend fun getCustomerTypeDistribution(): List<CustomerTypeCount>
    
    @Query("""
        SELECT preferredLanguage, COUNT(*) as count 
        FROM speaker_profiles 
        WHERE preferredLanguage IS NOT NULL AND isActive = 1 
        GROUP BY preferredLanguage 
        ORDER BY count DESC
    """)
    suspend fun getLanguageDistribution(): List<LanguageCount>
    
    @Query("SELECT * FROM speaker_profiles WHERE createdAt >= :timestamp AND isActive = 1 ORDER BY createdAt DESC")
    fun getNewCustomersSince(timestamp: Long): Flow<List<SpeakerProfile>>
    
    @Query("SELECT COUNT(*) FROM speaker_profiles WHERE createdAt >= :timestamp AND isSeller = 0 AND isActive = 1")
    suspend fun getNewCustomerCountSince(timestamp: Long): Int
    
    // Cleanup queries
    @Query("DELETE FROM speaker_profiles WHERE isActive = 0 AND updatedAt < :cutoffTime")
    suspend fun cleanupInactiveProfiles(cutoffTime: Long)
    
    @Query("UPDATE speaker_profiles SET isActive = 0 WHERE lastVisit < :cutoffTime AND isSeller = 0")
    suspend fun deactivateOldCustomers(cutoffTime: Long)
}

/**
 * Data class for customer type distribution results
 */
data class CustomerTypeCount(
    val customerType: String,
    val count: Int
)

/**
 * Data class for language distribution results
 */
data class LanguageCount(
    val preferredLanguage: String,
    val count: Int
)