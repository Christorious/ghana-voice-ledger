package com.voiceledger.ghana.domain.service

import com.voiceledger.ghana.data.local.entity.DailySummary
import com.voiceledger.ghana.data.local.entity.Transaction
import com.voiceledger.ghana.domain.repository.TransactionRepository
import com.voiceledger.ghana.domain.repository.SpeakerProfileRepository
import com.voiceledger.ghana.domain.repository.DailySummaryRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for generating comprehensive daily summaries
 * Aggregates transaction data and provides business insights
 */
@Singleton
class DailySummaryGenerator @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val speakerProfileRepository: SpeakerProfileRepository,
    private val dailySummaryRepository: DailySummaryRepository
) {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    /**
     * Generate daily summary for today
     */
    suspend fun generateTodaysSummary(): DailySummary {
        val today = dateFormat.format(Date())
        return generateDailySummary(today)
    }
    
    /**
     * Generate daily summary for a specific date
     */
    suspend fun generateDailySummary(date: String): DailySummary {
        val transactions = transactionRepository.getTransactionsByDate(date).first()
        
        if (transactions.isEmpty()) {
            return createEmptySummary(date)
        }
        
        val summary = DailySummary(
            date = date,
            totalSales = calculateTotalSales(transactions),
            transactionCount = transactions.size,
            uniqueCustomers = calculateUniqueCustomers(transactions),
            topProduct = findTopProduct(transactions),
            topProductSales = calculateTopProductSales(transactions),
            peakHour = findPeakHour(transactions),
            peakHourSales = calculatePeakHourSales(transactions),
            averageTransactionValue = calculateAverageTransactionValue(transactions),
            repeatCustomers = calculateRepeatCustomers(transactions),
            newCustomers = calculateNewCustomers(transactions, date),
            totalQuantitySold = calculateTotalQuantitySold(transactions),
            mostProfitableHour = findMostProfitableHour(transactions),
            leastActiveHour = findLeastActiveHour(transactions),
            confidenceScore = calculateAverageConfidence(transactions),
            reviewedTransactions = countReviewedTransactions(transactions),
            comparisonWithYesterday = generateYesterdayComparison(date, transactions),
            comparisonWithLastWeek = generateLastWeekComparison(date, transactions),
            productBreakdown = generateProductBreakdown(transactions),
            hourlyBreakdown = generateHourlyBreakdown(transactions),
            customerInsights = generateCustomerInsights(transactions),
            recommendations = generateRecommendations(transactions, date),
            timestamp = System.currentTimeMillis()
        )
        
        // Save the summary
        dailySummaryRepository.insertSummary(summary)
        
        return summary
    }
    
    /**
     * Generate summary for a date range (weekly/monthly)
     */
    suspend fun generatePeriodSummary(startDate: String, endDate: String): PeriodSummary {
        val transactions = transactionRepository.getSummariesByDateRange(startDate, endDate).first()
            .flatMap { summary -> 
                transactionRepository.getTransactionsByDate(summary.date).first()
            }
        
        return PeriodSummary(
            startDate = startDate,
            endDate = endDate,
            totalSales = calculateTotalSales(transactions),
            totalTransactions = transactions.size,
            averageDailySales = calculateAverageDailySales(transactions, startDate, endDate),
            bestDay = findBestSalesDay(startDate, endDate),
            worstDay = findWorstSalesDay(startDate, endDate),
            topProducts = findTopProductsPeriod(transactions),
            customerGrowth = calculateCustomerGrowth(startDate, endDate),
            salesTrend = calculateSalesTrend(startDate, endDate),
            recommendations = generatePeriodRecommendations(transactions, startDate, endDate)
        )
    }
    
    private fun createEmptySummary(date: String): DailySummary {
        return DailySummary(
            date = date,
            totalSales = 0.0,
            transactionCount = 0,
            uniqueCustomers = 0,
            topProduct = null,
            topProductSales = 0.0,
            peakHour = null,
            peakHourSales = 0.0,
            averageTransactionValue = 0.0,
            repeatCustomers = 0,
            newCustomers = 0,
            totalQuantitySold = 0.0,
            mostProfitableHour = null,
            leastActiveHour = null,
            confidenceScore = 0.0f,
            reviewedTransactions = 0,
            comparisonWithYesterday = null,
            comparisonWithLastWeek = null,
            productBreakdown = emptyMap(),
            hourlyBreakdown = emptyMap(),
            customerInsights = emptyMap(),
            recommendations = emptyList(),
            timestamp = System.currentTimeMillis()
        )
    }
    
    private fun calculateTotalSales(transactions: List<Transaction>): Double {
        return transactions.sumOf { it.amount }
    }
    
    private fun calculateUniqueCustomers(transactions: List<Transaction>): Int {
        return transactions.mapNotNull { it.customerId }.distinct().size
    }
    
    private fun findTopProduct(transactions: List<Transaction>): String? {
        return transactions.groupBy { it.product }
            .maxByOrNull { it.value.size }?.key
    }
    
    private fun calculateTopProductSales(transactions: List<Transaction>): Double {
        val topProduct = findTopProduct(transactions) ?: return 0.0
        return transactions.filter { it.product == topProduct }.sumOf { it.amount }
    }
    
    private fun findPeakHour(transactions: List<Transaction>): String? {
        return transactions.groupBy { getHourFromTimestamp(it.timestamp) }
            .maxByOrNull { it.value.size }?.key
    }
    
    private fun calculatePeakHourSales(transactions: List<Transaction>): Double {
        val peakHour = findPeakHour(transactions) ?: return 0.0
        return transactions.filter { getHourFromTimestamp(it.timestamp) == peakHour }
            .sumOf { it.amount }
    }
    
    private fun calculateAverageTransactionValue(transactions: List<Transaction>): Double {
        return if (transactions.isNotEmpty()) {
            transactions.sumOf { it.amount } / transactions.size
        } else 0.0
    }
    
    private fun calculateRepeatCustomers(transactions: List<Transaction>): Int {
        return transactions.mapNotNull { it.customerId }
            .groupBy { it }
            .count { it.value.size > 1 }
    }
    
    private suspend fun calculateNewCustomers(transactions: List<Transaction>, date: String): Int {
        val todayCustomers = transactions.mapNotNull { it.customerId }.distinct()
        var newCustomers = 0
        
        for (customerId in todayCustomers) {
            val customerTransactions = transactionRepository.getTransactionsByCustomer(customerId).first()
            val firstTransactionDate = customerTransactions.minByOrNull { it.timestamp }?.let {
                dateFormat.format(Date(it.timestamp))
            }
            if (firstTransactionDate == date) {
                newCustomers++
            }
        }
        
        return newCustomers
    }
    
    private fun calculateTotalQuantitySold(transactions: List<Transaction>): Double {
        return transactions.sumOf { it.quantity ?: 0.0 }
    }
    
    private fun findMostProfitableHour(transactions: List<Transaction>): String? {
        return transactions.groupBy { getHourFromTimestamp(it.timestamp) }
            .maxByOrNull { it.value.sumOf { transaction -> transaction.amount } }?.key
    }
    
    private fun findLeastActiveHour(transactions: List<Transaction>): String? {
        val hourlyTransactions = transactions.groupBy { getHourFromTimestamp(it.timestamp) }
        return if (hourlyTransactions.isNotEmpty()) {
            hourlyTransactions.minByOrNull { it.value.size }?.key
        } else null
    }
    
    private fun calculateAverageConfidence(transactions: List<Transaction>): Float {
        return if (transactions.isNotEmpty()) {
            transactions.map { it.confidence }.average().toFloat()
        } else 0.0f
    }
    
    private fun countReviewedTransactions(transactions: List<Transaction>): Int {
        return transactions.count { !it.needsReview }
    }
    
    private suspend fun generateYesterdayComparison(date: String, todayTransactions: List<Transaction>): ComparisonData? {
        val yesterday = getYesterdayDate(date)
        val yesterdayTransactions = transactionRepository.getTransactionsByDate(yesterday).first()
        
        if (yesterdayTransactions.isEmpty()) return null
        
        val todaySales = calculateTotalSales(todayTransactions)
        val yesterdaySales = calculateTotalSales(yesterdayTransactions)
        val salesChange = ((todaySales - yesterdaySales) / yesterdaySales * 100).takeIf { !it.isNaN() } ?: 0.0
        
        val todayCount = todayTransactions.size
        val yesterdayCount = yesterdayTransactions.size
        val countChange = if (yesterdayCount > 0) {
            ((todayCount - yesterdayCount).toDouble() / yesterdayCount * 100)
        } else 0.0
        
        return ComparisonData(
            salesChange = salesChange,
            transactionCountChange = countChange,
            averageValueChange = calculateAverageTransactionValue(todayTransactions) - 
                                calculateAverageTransactionValue(yesterdayTransactions)
        )
    }
    
    private suspend fun generateLastWeekComparison(date: String, todayTransactions: List<Transaction>): ComparisonData? {
        val lastWeekDate = getLastWeekDate(date)
        val lastWeekTransactions = transactionRepository.getTransactionsByDate(lastWeekDate).first()
        
        if (lastWeekTransactions.isEmpty()) return null
        
        val todaySales = calculateTotalSales(todayTransactions)
        val lastWeekSales = calculateTotalSales(lastWeekTransactions)
        val salesChange = ((todaySales - lastWeekSales) / lastWeekSales * 100).takeIf { !it.isNaN() } ?: 0.0
        
        val todayCount = todayTransactions.size
        val lastWeekCount = lastWeekTransactions.size
        val countChange = if (lastWeekCount > 0) {
            ((todayCount - lastWeekCount).toDouble() / lastWeekCount * 100)
        } else 0.0
        
        return ComparisonData(
            salesChange = salesChange,
            transactionCountChange = countChange,
            averageValueChange = calculateAverageTransactionValue(todayTransactions) - 
                                calculateAverageTransactionValue(lastWeekTransactions)
        )
    }
    
    private fun generateProductBreakdown(transactions: List<Transaction>): Map<String, ProductSummary> {
        return transactions.groupBy { it.product }
            .mapValues { (_, productTransactions) ->
                ProductSummary(
                    totalSales = productTransactions.sumOf { it.amount },
                    transactionCount = productTransactions.size,
                    totalQuantity = productTransactions.sumOf { it.quantity ?: 0.0 },
                    averagePrice = productTransactions.sumOf { it.amount } / productTransactions.size,
                    peakHour = productTransactions.groupBy { getHourFromTimestamp(it.timestamp) }
                        .maxByOrNull { it.value.size }?.key
                )
            }
    }
    
    private fun generateHourlyBreakdown(transactions: List<Transaction>): Map<String, HourlySummary> {
        return transactions.groupBy { getHourFromTimestamp(it.timestamp) }
            .mapValues { (_, hourTransactions) ->
                HourlySummary(
                    totalSales = hourTransactions.sumOf { it.amount },
                    transactionCount = hourTransactions.size,
                    uniqueCustomers = hourTransactions.mapNotNull { it.customerId }.distinct().size,
                    topProduct = hourTransactions.groupBy { it.product }
                        .maxByOrNull { it.value.size }?.key
                )
            }
    }
    
    private fun generateCustomerInsights(transactions: List<Transaction>): Map<String, CustomerInsight> {
        return transactions.mapNotNull { it.customerId }
            .distinct()
            .associateWith { customerId ->
                val customerTransactions = transactions.filter { it.customerId == customerId }
                CustomerInsight(
                    totalSpent = customerTransactions.sumOf { it.amount },
                    transactionCount = customerTransactions.size,
                    favoriteProduct = customerTransactions.groupBy { it.product }
                        .maxByOrNull { it.value.size }?.key,
                    averageTransactionValue = customerTransactions.sumOf { it.amount } / customerTransactions.size,
                    preferredTime = customerTransactions.groupBy { getHourFromTimestamp(it.timestamp) }
                        .maxByOrNull { it.value.size }?.key
                )
            }
    }
    
    private fun generateRecommendations(transactions: List<Transaction>, date: String): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Sales performance recommendations
        val totalSales = calculateTotalSales(transactions)
        if (totalSales == 0.0) {
            recommendations.add("No sales recorded today. Consider checking your voice detection settings.")
        } else if (totalSales < 50.0) {
            recommendations.add("Sales are lower than usual. Consider promoting popular products.")
        }
        
        // Product diversity recommendations
        val uniqueProducts = transactions.map { it.product }.distinct().size
        if (uniqueProducts < 3 && transactions.isNotEmpty()) {
            recommendations.add("Consider diversifying your product offerings to attract more customers.")
        }
        
        // Peak hour recommendations
        val peakHour = findPeakHour(transactions)
        if (peakHour != null) {
            recommendations.add("Your peak sales hour is $peakHour:00. Consider stocking more inventory during this time.")
        }
        
        // Customer retention recommendations
        val repeatCustomers = calculateRepeatCustomers(transactions)
        val totalCustomers = calculateUniqueCustomers(transactions)
        if (totalCustomers > 0 && repeatCustomers.toDouble() / totalCustomers < 0.3) {
            recommendations.add("Focus on customer retention strategies to increase repeat business.")
        }
        
        // Confidence score recommendations
        val avgConfidence = calculateAverageConfidence(transactions)
        if (avgConfidence < 0.8f && transactions.isNotEmpty()) {
            recommendations.add("Consider re-enrolling your voice profile to improve transaction accuracy.")
        }
        
        return recommendations
    }
    
    // Helper methods for period summaries
    private suspend fun calculateAverageDailySales(transactions: List<Transaction>, startDate: String, endDate: String): Double {
        val days = calculateDaysBetween(startDate, endDate)
        return if (days > 0) calculateTotalSales(transactions) / days else 0.0
    }
    
    private suspend fun findBestSalesDay(startDate: String, endDate: String): String? {
        return dailySummaryRepository.getSummariesByDateRange(startDate, endDate).first()
            .maxByOrNull { it.totalSales }?.date
    }
    
    private suspend fun findWorstSalesDay(startDate: String, endDate: String): String? {
        return dailySummaryRepository.getSummariesByDateRange(startDate, endDate).first()
            .minByOrNull { it.totalSales }?.date
    }
    
    private fun findTopProductsPeriod(transactions: List<Transaction>): List<String> {
        return transactions.groupBy { it.product }
            .toList()
            .sortedByDescending { it.second.sumOf { transaction -> transaction.amount } }
            .take(5)
            .map { it.first }
    }
    
    private suspend fun calculateCustomerGrowth(startDate: String, endDate: String): Double {
        val startCustomers = speakerProfileRepository.getNewCustomerCountSince(
            dateFormat.parse(startDate)?.time ?: 0L
        )
        val endCustomers = speakerProfileRepository.getNewCustomerCountSince(
            dateFormat.parse(endDate)?.time ?: 0L
        )
        
        return if (startCustomers > 0) {
            ((endCustomers - startCustomers).toDouble() / startCustomers * 100)
        } else 0.0
    }
    
    private suspend fun calculateSalesTrend(startDate: String, endDate: String): Double {
        val summaries = dailySummaryRepository.getSummariesByDateRange(startDate, endDate).first()
        if (summaries.size < 2) return 0.0
        
        val firstHalf = summaries.take(summaries.size / 2).sumOf { it.totalSales }
        val secondHalf = summaries.drop(summaries.size / 2).sumOf { it.totalSales }
        
        return if (firstHalf > 0) ((secondHalf - firstHalf) / firstHalf * 100) else 0.0
    }
    
    private fun generatePeriodRecommendations(transactions: List<Transaction>, startDate: String, endDate: String): List<String> {
        val recommendations = mutableListOf<String>()
        
        val totalSales = calculateTotalSales(transactions)
        val days = calculateDaysBetween(startDate, endDate)
        val averageDailySales = if (days > 0) totalSales / days else 0.0
        
        if (averageDailySales < 100.0) {
            recommendations.add("Average daily sales are below target. Consider marketing strategies to boost sales.")
        }
        
        val topProducts = findTopProductsPeriod(transactions)
        if (topProducts.isNotEmpty()) {
            recommendations.add("Focus on promoting ${topProducts.first()} as it's your best-selling product.")
        }
        
        return recommendations
    }
    
    // Utility methods
    private fun getHourFromTimestamp(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return calendar.get(Calendar.HOUR_OF_DAY).toString()
    }
    
    private fun getYesterdayDate(date: String): String {
        val calendar = Calendar.getInstance()
        calendar.time = dateFormat.parse(date) ?: Date()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        return dateFormat.format(calendar.time)
    }
    
    private fun getLastWeekDate(date: String): String {
        val calendar = Calendar.getInstance()
        calendar.time = dateFormat.parse(date) ?: Date()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        return dateFormat.format(calendar.time)
    }
    
    private fun calculateDaysBetween(startDate: String, endDate: String): Int {
        val start = dateFormat.parse(startDate)?.time ?: 0L
        val end = dateFormat.parse(endDate)?.time ?: 0L
        return ((end - start) / (24 * 60 * 60 * 1000)).toInt() + 1
    }
}

/**
 * Data classes for summary components
 */
data class ComparisonData(
    val salesChange: Double,
    val transactionCountChange: Double,
    val averageValueChange: Double
)

data class ProductSummary(
    val totalSales: Double,
    val transactionCount: Int,
    val totalQuantity: Double,
    val averagePrice: Double,
    val peakHour: String?
)

data class HourlySummary(
    val totalSales: Double,
    val transactionCount: Int,
    val uniqueCustomers: Int,
    val topProduct: String?
)

data class CustomerInsight(
    val totalSpent: Double,
    val transactionCount: Int,
    val favoriteProduct: String?,
    val averageTransactionValue: Double,
    val preferredTime: String?
)

data class PeriodSummary(
    val startDate: String,
    val endDate: String,
    val totalSales: Double,
    val totalTransactions: Int,
    val averageDailySales: Double,
    val bestDay: String?,
    val worstDay: String?,
    val topProducts: List<String>,
    val customerGrowth: Double,
    val salesTrend: Double,
    val recommendations: List<String>
)