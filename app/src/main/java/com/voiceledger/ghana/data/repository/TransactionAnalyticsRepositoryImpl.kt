package com.voiceledger.ghana.data.repository

import com.voiceledger.ghana.data.local.dao.TransactionDao
import com.voiceledger.ghana.data.local.entity.Transaction
import com.voiceledger.ghana.domain.model.AnalyticsRange
import com.voiceledger.ghana.domain.model.TransactionAnalytics
import com.voiceledger.ghana.domain.model.TransactionAnalyticsOverview
import com.voiceledger.ghana.domain.model.TransactionCategoryMetric
import com.voiceledger.ghana.domain.model.TransactionHourlyMetric
import com.voiceledger.ghana.domain.model.TransactionSpeakerMetric
import com.voiceledger.ghana.domain.model.TransactionSuccessMetrics
import com.voiceledger.ghana.domain.model.TransactionTrendPoint
import com.voiceledger.ghana.domain.repository.TransactionAnalyticsRepository
import com.voiceledger.ghana.domain.service.AnalyticsConsentProvider
import com.voiceledger.ghana.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionAnalyticsRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val consentProvider: AnalyticsConsentProvider
) : TransactionAnalyticsRepository {

    override fun observeAnalytics(range: AnalyticsRange): Flow<TransactionAnalyticsOverview> {
        return transactionDao.getAllTransactionsFlow()
            .map { transactions ->
                val analyticsEnabled = consentProvider.isAnalyticsEnabled()
                if (!analyticsEnabled) {
                    Timber.d("Analytics disabled by privacy settings")
                    TransactionAnalyticsOverview.empty(range, analyticsEnabled = false)
                } else {
                    val filtered = filterByRange(transactions, range)
                    if (filtered.isEmpty()) {
                        TransactionAnalyticsOverview.empty(range, analyticsEnabled = true)
                    } else {
                        computeOverview(filtered, range)
                    }
                }
            }
            .distinctUntilChanged()
    }

    private fun filterByRange(transactions: List<Transaction>, range: AnalyticsRange): List<Transaction> {
        val (start, end) = when (range) {
            AnalyticsRange.Last7Days -> boundsForDays(7)
            AnalyticsRange.Last30Days -> boundsForDays(30)
            is AnalyticsRange.Custom -> range.startTimeMillis to range.endTimeMillis
        }
        return transactions.filter { it.timestamp in start..end }
    }

    private fun boundsForDays(days: Int): Pair<Long, Long> {
        val endDate = DateUtils.getTodayDateString()
        val end = DateUtils.getEndOfDayTimestamp(endDate)
        val startDate = DateUtils.getDateDaysAgo(days - 1)
        val start = DateUtils.getStartOfDayTimestamp(startDate)
        return start to end
    }

    private fun computeOverview(transactions: List<Transaction>, range: AnalyticsRange): TransactionAnalyticsOverview {
        val summary = computeSummary(transactions)
        val categories = computeCategoryMetrics(transactions, summary.transactionCount)
        val dailyTrend = computeDailyTrend(transactions)
        val hourlyTrend = computeHourlyMetrics(transactions)
        val speakerStats = computeSpeakerMetrics(transactions)
        val successMetrics = computeSuccessMetrics(transactions, summary.transactionCount)

        return TransactionAnalyticsOverview(
            summary = summary,
            categories = categories,
            dailyTrend = dailyTrend,
            hourlyTrend = hourlyTrend,
            speakerStats = speakerStats,
            successMetrics = successMetrics,
            analyticsRange = range,
            analyticsEnabled = true
        )
    }

    private fun computeSummary(transactions: List<Transaction>): TransactionAnalytics {
        val totalSales = transactions.sumOf { it.amount }
        val transactionCount = transactions.size

        val topProduct = transactions
            .groupingBy { it.product }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

        val peakHour = transactions
            .groupingBy { extractHourOfDay(it.timestamp) }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            ?.let { formatHourRange(it) }

        val uniqueCustomers = transactions
            .mapNotNull { it.customerId }
            .toSet()
            .size

        val averageTransactionValue = if (transactionCount > 0) {
            totalSales / transactionCount
        } else {
            0.0
        }

        val successRate = if (transactionCount > 0) {
            transactions.count { !it.needsReview }.toDouble() / transactionCount
        } else {
            0.0
        }

        return TransactionAnalytics(
            totalSales = totalSales,
            transactionCount = transactionCount,
            topProduct = topProduct,
            peakHour = peakHour,
            uniqueCustomers = uniqueCustomers,
            averageTransactionValue = averageTransactionValue,
            successRate = successRate
        )
    }

    private fun computeCategoryMetrics(transactions: List<Transaction>, totalCount: Int): List<TransactionCategoryMetric> {
        if (transactions.isEmpty() || totalCount == 0) return emptyList()

        return transactions
            .groupBy { it.product }
            .map { (product, items) ->
                val count = items.size
                val total = items.sumOf { it.amount }
                TransactionCategoryMetric(
                    category = product,
                    transactionCount = count,
                    totalAmount = total,
                    percentageOfTransactions = count.toDouble() / totalCount
                )
            }
            .sortedByDescending { it.totalAmount }
    }

    private fun computeDailyTrend(transactions: List<Transaction>): List<TransactionTrendPoint> {
        return transactions
            .groupBy { it.date }
            .map { (date, items) ->
                TransactionTrendPoint(
                    date = date,
                    transactionCount = items.size,
                    totalAmount = items.sumOf { it.amount }
                )
            }
            .sortedBy { it.date }
    }

    private fun computeHourlyMetrics(transactions: List<Transaction>): List<TransactionHourlyMetric> {
        return transactions
            .groupBy { extractHourOfDay(it.timestamp) }
            .map { (hour, items) ->
                TransactionHourlyMetric(
                    hour = hour,
                    label = formatHourRange(hour),
                    transactionCount = items.size,
                    totalAmount = items.sumOf { it.amount }
                )
            }
            .sortedBy { it.hour }
    }

    private fun computeSpeakerMetrics(transactions: List<Transaction>): List<TransactionSpeakerMetric> {
        return transactions
            .mapNotNull { transaction ->
                val speaker = transaction.customerId ?: return@mapNotNull null
                speaker to transaction
            }
            .groupBy({ it.first }, { it.second })
            .map { (speakerId, items) ->
                val count = items.size
                val totalAmount = items.sumOf { it.amount }
                val successful = items.count { !it.needsReview }
                val successRate = if (count > 0) successful.toDouble() / count else 0.0

                TransactionSpeakerMetric(
                    speakerId = speakerId,
                    transactionCount = count,
                    totalAmount = totalAmount,
                    successRate = successRate
                )
            }
            .sortedByDescending { it.transactionCount }
    }

    private fun computeSuccessMetrics(transactions: List<Transaction>, totalCount: Int): TransactionSuccessMetrics {
        if (transactions.isEmpty()) return TransactionSuccessMetrics()

        val autoSaved = transactions.count { !it.needsReview && it.confidence >= AUTO_SAVE_THRESHOLD }
        val flagged = transactions.count { it.needsReview }
        val manual = transactions.count { it.confidence < AUTO_SAVE_THRESHOLD }
        val successRate = if (totalCount > 0) transactions.count { !it.needsReview }.toDouble() / totalCount else 0.0

        return TransactionSuccessMetrics(
            autoSaved = autoSaved,
            flaggedForReview = flagged,
            manual = manual,
            successRate = successRate
        )
    }

    private fun extractHourOfDay(timestamp: Long): Int {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return calendar.get(Calendar.HOUR_OF_DAY)
    }

    private fun formatHourRange(hour: Int): String {
        val nextHour = (hour + 1) % 24
        return String.format(Locale.getDefault(), "%02d:00-%02d:00", hour, nextHour)
    }

    private companion object {
        const val AUTO_SAVE_THRESHOLD = 0.8f
    }
}
