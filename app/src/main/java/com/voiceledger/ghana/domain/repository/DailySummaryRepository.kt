package com.voiceledger.ghana.domain.repository

import com.voiceledger.ghana.data.local.entity.DailySummary
import com.voiceledger.ghana.data.local.dao.WeeklySummary
import com.voiceledger.ghana.data.local.dao.MonthlySummary
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for daily summary operations
 * Defines the contract for summary data access and analytics
 */
interface DailySummaryRepository {
    
    // Query operations
    fun getAllSummaries(): Flow<List<DailySummary>>
    suspend fun getSummaryByDate(date: String): DailySummary?
    fun getSummaryByDateFlow(date: String): Flow<DailySummary?>
    suspend fun getTodaysSummary(): DailySummary?
    fun getTodaysSummaryFlow(): Flow<DailySummary?>
    fun getRecentSummaries(limit: Int): Flow<List<DailySummary>>
    fun getSummariesByDateRange(startDate: String, endDate: String): Flow<List<DailySummary>>
    
    // Analytics operations
    suspend fun getTotalSalesForPeriod(startDate: String, endDate: String): Double
    suspend fun getTotalTransactionsForPeriod(startDate: String, endDate: String): Int
    suspend fun getAverageDailySalesForPeriod(startDate: String, endDate: String): Double
    suspend fun getAverageTransactionValueForPeriod(startDate: String, endDate: String): Double
    suspend fun getBestSalesDay(): DailySummary?
    suspend fun getBusiestDay(): DailySummary?
    suspend fun getMostFrequentTopProduct(): String?
    suspend fun getWeeklySummaries(startDate: String, endDate: String): List<WeeklySummary>
    suspend fun getMonthlySummaries(startDate: String, endDate: String): List<MonthlySummary>
    suspend fun getTotalDaysTracked(): Int
    suspend fun getHighestDailySales(): Double?
    suspend fun getLowestDailySales(): Double?
    
    // CRUD operations
    suspend fun insertSummary(summary: DailySummary)
    suspend fun insertSummaries(summaries: List<DailySummary>)
    suspend fun updateSummary(summary: DailySummary)
    suspend fun deleteSummary(summary: DailySummary)
    suspend fun deleteSummaryByDate(date: String)
    
    // Generation operations
    suspend fun generateDailySummary(date: String): DailySummary
    suspend fun generateTodaysSummary(): DailySummary
    suspend fun regenerateSummary(date: String): DailySummary
    
    // Sync operations
    suspend fun getUnsyncedSummaries(): List<DailySummary>
    suspend fun markSummariesAsSynced(dates: List<String>)
    
    // Maintenance operations
    suspend fun deleteOldSummaries(daysToKeep: Int)
}