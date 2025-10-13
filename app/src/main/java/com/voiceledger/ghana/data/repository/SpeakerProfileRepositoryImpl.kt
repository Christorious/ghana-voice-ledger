package com.voiceledger.ghana.data.repository

import com.voiceledger.ghana.data.local.dao.SpeakerProfileDao
import com.voiceledger.ghana.data.local.dao.CustomerTypeCount
import com.voiceledger.ghana.data.local.dao.LanguageCount
import com.voiceledger.ghana.data.local.entity.SpeakerProfile
import com.voiceledger.ghana.domain.repository.SpeakerProfileRepository
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Implementation of SpeakerProfileRepository
 * Handles speaker identification and customer management
 */
@Singleton
class SpeakerProfileRepositoryImpl @Inject constructor(
    private val speakerProfileDao: SpeakerProfileDao
) : SpeakerProfileRepository {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    override fun getAllActiveProfiles(): Flow<List<SpeakerProfile>> {
        return speakerProfileDao.getAllActiveProfiles()
    }
    
    override suspend fun getSellerProfile(): SpeakerProfile? {
        return speakerProfileDao.getSellerProfile()
    }
    
    override fun getSellerProfileFlow(): Flow<SpeakerProfile?> {
        return speakerProfileDao.getSellerProfileFlow()
    }
    
    override fun getCustomerProfiles(): Flow<List<SpeakerProfile>> {
        return speakerProfileDao.getCustomerProfiles()
    }
    
    override fun getRegularCustomers(minVisits: Int): Flow<List<SpeakerProfile>> {
        return speakerProfileDao.getRegularCustomers(minVisits)
    }
    
    override suspend fun getProfileById(id: String): SpeakerProfile? {
        return speakerProfileDao.getProfileById(id)
    }
    
    override fun getRecentCustomers(recentThreshold: Long): Flow<List<SpeakerProfile>> {
        return speakerProfileDao.getRecentCustomers(recentThreshold)
    }
    
    override fun getTopSpendingCustomers(limit: Int): Flow<List<SpeakerProfile>> {
        return speakerProfileDao.getTopSpendingCustomers(limit)
    }
    
    override fun getProfilesByLanguage(language: String): Flow<List<SpeakerProfile>> {
        return speakerProfileDao.getProfilesByLanguage(language)
    }
    
    override fun getNewCustomersSince(timestamp: Long): Flow<List<SpeakerProfile>> {
        return speakerProfileDao.getNewCustomersSince(timestamp)
    }
    
    override suspend fun getCustomerCount(): Int {
        return speakerProfileDao.getCustomerCount()
    }
    
    override suspend fun getRegularCustomerCount(minVisits: Int): Int {
        return speakerProfileDao.getRegularCustomerCount(minVisits)
    }
    
    override suspend fun getTotalCustomerSpending(): Double {
        return speakerProfileDao.getTotalCustomerSpending() ?: 0.0
    }
    
    override suspend fun getAverageCustomerSpending(): Double {
        return speakerProfileDao.getAverageCustomerSpending() ?: 0.0
    }
    
    override suspend fun getAllLanguages(): List<String> {
        return speakerProfileDao.getAllLanguages()
    }
    
    override suspend fun getCustomerTypeDistribution(): List<CustomerTypeCount> {
        return speakerProfileDao.getCustomerTypeDistribution()
    }
    
    override suspend fun getLanguageDistribution(): List<LanguageCount> {
        return speakerProfileDao.getLanguageDistribution()
    }
    
    override suspend fun getNewCustomerCountSince(timestamp: Long): Int {
        return speakerProfileDao.getNewCustomerCountSince(timestamp)
    }
    
    override suspend fun insertProfile(profile: SpeakerProfile) {
        speakerProfileDao.insertProfile(profile)
    }
    
    override suspend fun insertProfiles(profiles: List<SpeakerProfile>) {
        speakerProfileDao.insertProfiles(profiles)
    }
    
    override suspend fun updateProfile(profile: SpeakerProfile) {
        val updatedProfile = profile.copy(updatedAt = System.currentTimeMillis())
        speakerProfileDao.updateProfile(updatedProfile)
    }
    
    override suspend fun deleteProfile(profile: SpeakerProfile) {
        speakerProfileDao.deleteProfile(profile)
    }
    
    override suspend fun deleteProfileById(id: String) {
        speakerProfileDao.deleteProfileById(id)
    }
    
    override suspend fun deactivateProfile(id: String) {
        speakerProfileDao.deactivateProfile(id)
    }
    
    override suspend fun reactivateProfile(id: String) {
        speakerProfileDao.reactivateProfile(id)
    }
    
    override suspend fun enrollSeller(voiceEmbedding: FloatArray, name: String?): SpeakerProfile {
        val sellerId = "seller_${System.currentTimeMillis()}"
        val currentTime = System.currentTimeMillis()
        
        val sellerProfile = SpeakerProfile(
            id = sellerId,
            voiceEmbedding = SpeakerProfile.embeddingToString(voiceEmbedding),
            isSeller = true,
            name = name ?: "Seller",
            visitCount = 0,
            lastVisit = currentTime,
            averageSpending = null,
            totalSpent = null,
            preferredLanguage = "en", // Default to English
            customerType = null,
            confidenceThreshold = 0.85f, // Higher threshold for seller
            isActive = true,
            createdAt = currentTime,
            updatedAt = currentTime,
            synced = false
        )
        
        insertProfile(sellerProfile)
        return sellerProfile
    }
    
    override suspend fun addCustomerProfile(voiceEmbedding: FloatArray, customerId: String): SpeakerProfile {
        val currentTime = System.currentTimeMillis()
        
        val customerProfile = SpeakerProfile(
            id = customerId,
            voiceEmbedding = SpeakerProfile.embeddingToString(voiceEmbedding),
            isSeller = false,
            name = null,
            visitCount = 1,
            lastVisit = currentTime,
            averageSpending = null,
            totalSpent = 0.0,
            preferredLanguage = null,
            customerType = "new",
            confidenceThreshold = 0.75f,
            isActive = true,
            createdAt = currentTime,
            updatedAt = currentTime,
            synced = false
        )
        
        insertProfile(customerProfile)
        return customerProfile
    }
    
    override suspend fun findSimilarProfiles(voiceEmbedding: FloatArray, threshold: Float): List<SpeakerProfile> {
        val allProfiles = speakerProfileDao.getAllActiveProfiles()
        val similarProfiles = mutableListOf<SpeakerProfile>()
        
        // This is a simplified implementation
        // In a real app, you'd use more sophisticated similarity calculations
        allProfiles.collect { profiles ->
            profiles.forEach { profile ->
                val profileEmbedding = profile.getEmbeddingArray()
                val similarity = calculateCosineSimilarity(voiceEmbedding, profileEmbedding)
                if (similarity >= threshold) {
                    similarProfiles.add(profile)
                }
            }
        }
        
        return similarProfiles.sortedByDescending { profile ->
            calculateCosineSimilarity(voiceEmbedding, profile.getEmbeddingArray())
        }
    }
    
    override suspend fun updateVoiceEmbedding(profileId: String, newEmbedding: FloatArray) {
        val profile = getProfileById(profileId)
        profile?.let {
            val updatedProfile = it.copy(
                voiceEmbedding = SpeakerProfile.embeddingToString(newEmbedding),
                updatedAt = System.currentTimeMillis()
            )
            updateProfile(updatedProfile)
        }
    }
    
    override suspend fun incrementVisitCount(id: String, timestamp: Long) {
        speakerProfileDao.incrementVisitCount(id, timestamp)
        
        // Update customer type based on visit count
        val profile = getProfileById(id)
        profile?.let {
            val newType = when {
                it.visitCount >= 10 -> "regular"
                it.visitCount >= 3 -> "frequent"
                else -> "occasional"
            }
            updateCustomerType(id, newType)
        }
    }
    
    override suspend fun addToTotalSpent(id: String, amount: Double) {
        speakerProfileDao.addToTotalSpent(id, amount)
        
        // Update average spending
        val profile = getProfileById(id)
        profile?.let {
            val newAverage = (it.totalSpent ?: 0.0) / it.visitCount.coerceAtLeast(1)
            updateAverageSpending(id, newAverage)
        }
    }
    
    override suspend fun updateAverageSpending(id: String, average: Double) {
        speakerProfileDao.updateAverageSpending(id, average)
    }
    
    override suspend fun updateConfidenceThreshold(id: String, threshold: Float) {
        speakerProfileDao.updateConfidenceThreshold(id, threshold)
    }
    
    override suspend fun updatePreferredLanguage(id: String, language: String) {
        speakerProfileDao.updatePreferredLanguage(id, language)
    }
    
    override suspend fun updateCustomerType(id: String, type: String) {
        speakerProfileDao.updateCustomerType(id, type)
    }
    
    override suspend fun getUnsyncedProfiles(): List<SpeakerProfile> {
        return speakerProfileDao.getUnsyncedProfiles()
    }
    
    override suspend fun markProfilesAsSynced(ids: List<String>) {
        speakerProfileDao.markProfilesAsSynced(ids)
    }
    
    override suspend fun cleanupInactiveProfiles(cutoffTime: Long) {
        speakerProfileDao.cleanupInactiveProfiles(cutoffTime)
    }
    
    override suspend fun deactivateOldCustomers(cutoffTime: Long) {
        speakerProfileDao.deactivateOldCustomers(cutoffTime)
    }
    
    /**
     * Calculate cosine similarity between two voice embeddings
     */
    private fun calculateCosineSimilarity(embedding1: FloatArray, embedding2: FloatArray): Float {
        if (embedding1.size != embedding2.size) return 0f
        
        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f
        
        for (i in embedding1.indices) {
            dotProduct += embedding1[i] * embedding2[i]
            norm1 += embedding1[i] * embedding1[i]
            norm2 += embedding2[i] * embedding2[i]
        }
        
        val denominator = sqrt(norm1) * sqrt(norm2)
        return if (denominator != 0f) dotProduct / denominator else 0f
    }
}