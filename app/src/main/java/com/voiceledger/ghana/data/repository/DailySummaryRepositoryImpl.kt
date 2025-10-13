package com.voiceledger.ghana.data.repository

import com.voiceledger.ghana.data.local.dao.DailySummaryDao
import com.voiceledger.ghana.data.local.dao.TransactionDao
import com.voiceledger.ghana.data.local.dao.WeeklySummary
import com.voiceledger.ghana.data.local.dao.MonthlySummary
import com.voiceledger.ghana.data.local.entity.DailySummary
import com.voiceledger.ghana.domain.repository.DailySummaryRepository
import com.voiceledger.ghana.domain.service.DailySummaryGenerator
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of DailySummaryRepository
 * Handles daily summary generation and analytics
 */
@Singleton
class DailySummaryRepositoryImpl @Inject constructor(
    private val dailySummaryDao: DailySummaryDao,
    private val transactionDao: TransactionDao,
    private val dailySummaryGenerator: DailySummaryGenerator
) : DailySummaryRepository {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    override fun getAllSummaries(): Flow<List<DailySummary>> {
        return dailySummaryDao.getAllSummaries()
    }
    
    override suspend fun getSummaryByDate(date: String): DailySummary? {
        return dailySummaryDao.getSummaryByDate(date)
    }
    
    override fun getSummaryByDateFlow(date: String): Flow<DailySummary?> {
        return dailySummaryDao.getSummaryByDateFlow(date)
    }
    
    override suspend fun getTodaysSummary(): DailySummary? {
        val today = dateFormat.format(Date())
        return getSummaryByDate(today)
    }
    
    override fun getTodaysSummaryFlow(): Flow<DailySummary?> {
        val today = dateFormat.format(Date())
        return getSummaryByDateFlow(today)
    }
    
    override fun getRecentSummaries(limit: Int): Flow<List<DailySummary>> {
        return dailySummaryDao.getRecentSummaries(limit)
    }
    
    override fun getSummariesByDateRange(startDate: String, endDate: String): Flow<List<DailySummary>> {
        return dailySummaryDao.getSummariesByDateRange(startDate, endDate)
    }
    
    override suspend fun getTotalSalesForPeriod(startDate: String, endDate: String): Double {
        return dailySummaryDao.getTotalSalesForPeriod(startDate, endDate) ?: 0.0
    }
    
    override suspend fun getTotalTransactionsForPeriod(startDate: String, endDate: String): Int {
        return dailySummaryDao.getTotalTransactionsForPeriod(startDate, endDate) ?: 0
    }
    
    override suspend fun getAverageDailySalesForPeriod(startDate: String, endDate: String): Double {
        return dailySummaryDao.getAverageDailySalesForPeriod(startDate, endDate) ?: 0.0
    }
    
    override suspend fun getAverageTransactionValueForPeriod(startDate: String, endDate: String): Double {
        return dailySummaryDao.getAverageTransactionValueForPeriod(startDate, endDate) ?: 0.0
    }
    
    override suspend fun getBestSalesDay(): DailySummary? {
        return dailySummaryDao.getBestSalesDay()
    }
    
    override suspend fun getBusiestDay(): DailySummary? {
        return dailySummaryDao.getBusiestDay()
    }
    
    override suspend fun getMostFrequentTopProduct(): String? {
        return dailySummaryDao.getMostFrequentTopProduct()
    }
    
    override suspend fun getWeeklySummaries(startDate: String, endDate: String): List<WeeklySummary> {
        return dailySummaryDao.getWeeklySummaries(startDate, endDate)
    }
    
    override suspend fun getMonthlySummaries(startDate: String, endDate: String): List<MonthlySummary> {
        return dailySummaryDao.getMonthlySummaries(startDate, endDate)
    }
    
    override suspend fun getTotalDaysTracked(): Int {
        return dailySummaryDao.getTotalDaysTracked()
    }
    
    override suspend fun getHighestDailySales(): Double? {
        return dailySummaryDao.getHighestDailySales()
    }
    
    override suspend fun getLowestDailySales(): Double? {
        return dailySummaryDao.getLowestDailySales()
    }
    
    override suspend fun insertSummary(summary: DailySummary) {
        dailySummaryDao.insertSummary(summary)
    }
    
    override suspend fun insertSummaries(summaries: List<DailySummary>) {
        dailySummaryDao.insertSummaries(summaries)
    }
    
    override suspend fun updateSummary(summary: DailySummary) {
        dailySummaryDao.updateSummary(summary)
    }
    
    override suspend fun deleteSummary(summary: DailySummary) {
        dailySummaryDao.deleteSummary(summary)
    }
    
    override suspend fun deleteSummaryByDate(date: String) {
        dailySummaryDao.deleteSummaryByDate(date)
    }
    
    override suspend fun generateDailySummary(date: String): DailySummary {
        // Delegate to the DailySummaryGenerator service for comprehensive analysis
        return dailySummaryGenerator.generateDailySummary(date)
    }
    
    override suspend fun generateTodaysSummary(): DailySummary {
        // Delegate to the DailySummaryGenerator service
        return dailySummaryGenerator.generateTodaysSummary()
    }
    
    override suspend fun regenerateSummary(date: String): DailySummary {
        // Delete existing summary if it exists
        deleteSummaryByDate(date)
        // Generate new summary
        return generateDailySummary(date)
    }
    
    override suspend fun getUnsyncedSummaries(): List<DailySummary> {
        return dailySummaryDao.getUnsyncedSummaries()
    }
    
    override suspend fun markSummariesAsSynced(dates: List<String>) {
        dailySummaryDao.markSummariesAsSynced(dates)
    }
    
    override suspend fun deleteOldSummaries(daysToKeep: Int) {
        val cutoffDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -daysToKeep)
        }.time
        val cutoffDateString = dateFormat.format(cutoffDate)
        dailySummaryDao.deleteOldSummaries(cutoffDateString)
    }
    
    private fun getPreviousDate(date: String): String {
        val calendar = Calendar.getInstance()
        val currentDate = dateFormat.parse(date)
        calendar.time = currentDate ?: Date()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        return dateFormat.format(calendar.time)
    }
}