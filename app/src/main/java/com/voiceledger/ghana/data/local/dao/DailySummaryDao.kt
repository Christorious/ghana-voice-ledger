package com.voiceledger.ghana.data.local.dao

import androidx.room.*
import com.voiceledger.ghana.data.local.entity.DailySummary
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for DailySummary entity
 * Provides queries for daily summary data and analytics
 */
@Dao
interface DailySummaryDao {
    
    @Query("SELECT * FROM daily_summaries ORDER BY date DESC")
    fun getAllSummaries(): Flow<List<DailySummary>>
    
    @Query("SELECT * FROM daily_summaries WHERE date = :date")
    suspend fun getSummaryByDate(date: String): DailySummary?
    
    @Query("SELECT * FROM daily_summaries WHERE date = :date")
    fun getSummaryByDateFlow(date: String): Flow<DailySummary?>
    
    @Query("SELECT * FROM daily_summaries ORDER BY date DESC LIMIT :limit")
    fun getRecentSummaries(limit: Int): Flow<List<DailySummary>>
    
    @Query("SELECT * FROM daily_summaries WHERE synced = 0 ORDER BY date ASC")
    suspend fun getUnsyncedSummaries(): List<DailySummary>
    
    @Query("SELECT * FROM daily_summaries WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getSummariesByDateRange(startDate: String, endDate: String): Flow<List<DailySummary>>
    
    @Query("SELECT SUM(totalSales) FROM daily_summaries WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalSalesForPeriod(startDate: String, endDate: String): Double?
    
    @Query("SELECT SUM(transactionCount) FROM daily_summaries WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalTransactionsForPeriod(startDate: String, endDate: String): Int?
    
    @Query("SELECT AVG(totalSales) FROM daily_summaries WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getAverageDailySalesForPeriod(startDate: String, endDate: String): Double?
    
    @Query("SELECT * FROM daily_summaries ORDER BY totalSales DESC LIMIT 1")
    suspend fun getBestSalesDay(): DailySummary?
    
    @Query("SELECT * FROM daily_summaries ORDER BY transactionCount DESC LIMIT 1")
    suspend fun getBusiestDay(): DailySummary?
    
    @Query("SELECT topProduct, COUNT(*) as frequency FROM daily_summaries GROUP BY topProduct ORDER BY frequency DESC LIMIT 1")
    suspend fun getMostFrequentTopProduct(): String?
    
    @Query("SELECT AVG(averageTransactionValue) FROM daily_summaries WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getAverageTransactionValueForPeriod(startDate: String, endDate: String): Double?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: DailySummary)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummaries(summaries: List<DailySummary>)
    
    @Update
    suspend fun updateSummary(summary: DailySummary)
    
    @Delete
    suspend fun deleteSummary(summary: DailySummary)
    
    @Query("DELETE FROM daily_summaries WHERE date = :date")
    suspend fun deleteSummaryByDate(date: String)
    
    @Query("DELETE FROM daily_summaries WHERE date < :cutoffDate")
    suspend fun deleteOldSummaries(cutoffDate: String)
    
    @Query("UPDATE daily_summaries SET synced = 1 WHERE date IN (:dates)")
    suspend fun markSummariesAsSynced(dates: List<String>)
    
    // Weekly and monthly analytics
    @Query("""
        SELECT 
            strftime('%Y-%W', date) as week,
            SUM(totalSales) as weeklyTotal,
            AVG(totalSales) as dailyAverage,
            SUM(transactionCount) as weeklyTransactions
        FROM daily_summaries 
        WHERE date BETWEEN :startDate AND :endDate
        GROUP BY week
        ORDER BY week DESC
    """)
    suspend fun getWeeklySummaries(startDate: String, endDate: String): List<WeeklySummary>
    
    @Query("""
        SELECT 
            strftime('%Y-%m', date) as month,
            SUM(totalSales) as monthlyTotal,
            AVG(totalSales) as dailyAverage,
            SUM(transactionCount) as monthlyTransactions,
            COUNT(*) as activeDays
        FROM daily_summaries 
        WHERE date BETWEEN :startDate AND :endDate
        GROUP BY month
        ORDER BY month DESC
    """)
    suspend fun getMonthlySummaries(startDate: String, endDate: String): List<MonthlySummary>
    
    // Performance tracking
    @Query("SELECT COUNT(*) FROM daily_summaries")
    suspend fun getTotalDaysTracked(): Int
    
    @Query("SELECT MAX(totalSales) FROM daily_summaries")
    suspend fun getHighestDailySales(): Double?
    
    @Query("SELECT MIN(totalSales) FROM daily_summaries WHERE totalSales > 0")
    suspend fun getLowestDailySales(): Double?
}

/**
 * Data class for weekly summary results
 */
data class WeeklySummary(
    val week: String,
    val weeklyTotal: Double,
    val dailyAverage: Double,
    val weeklyTransactions: Int
)

/**
 * Data class for monthly summary results
 */
data class MonthlySummary(
    val month: String,
    val monthlyTotal: Double,
    val dailyAverage: Double,
    val monthlyTransactions: Int,
    val activeDays: Int
)