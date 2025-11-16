package com.voiceledger.ghana.domain.model

/**
 * Core analytics models used to present transaction insights to the UI layer.
 */

data class TransactionAnalytics(
    val totalSales: Double,
    val transactionCount: Int,
    val topProduct: String?,
    val peakHour: String?,
    val uniqueCustomers: Int,
    val averageTransactionValue: Double,
    val successRate: Double = 0.0
)

/** Category level analytics describing distribution of sales. */
data class TransactionCategoryMetric(
    val category: String,
    val transactionCount: Int,
    val totalAmount: Double,
    val percentageOfTransactions: Double
)

/** Daily trend information used for charts/time series visualisations. */
data class TransactionTrendPoint(
    val date: String,
    val transactionCount: Int,
    val totalAmount: Double
)

/** Hour-of-day distribution for understanding peak trading windows. */
data class TransactionHourlyMetric(
    val hour: Int,
    val label: String,
    val transactionCount: Int,
    val totalAmount: Double
)

/** Speaker/customer level analytics for personalised insights. */
data class TransactionSpeakerMetric(
    val speakerId: String,
    val transactionCount: Int,
    val totalAmount: Double,
    val successRate: Double
)

/** Success metrics that track auto-saves versus manual interventions. */
data class TransactionSuccessMetrics(
    val autoSaved: Int = 0,
    val flaggedForReview: Int = 0,
    val manual: Int = 0,
    val successRate: Double = 0.0
)

/**
 * Supported analytics ranges. The label is user facing and reused by the UI.
 */
sealed class AnalyticsRange(open val label: String) {
    object Last7Days : AnalyticsRange("Last 7 days")
    object Last30Days : AnalyticsRange("Last 30 days")
    data class Custom(
        val startTimeMillis: Long,
        val endTimeMillis: Long,
        override val label: String
    ) : AnalyticsRange(label)
}

/**
 * Aggregated analytics packet delivered to the presentation layer.
 */
data class TransactionAnalyticsOverview(
    val summary: TransactionAnalytics,
    val categories: List<TransactionCategoryMetric>,
    val dailyTrend: List<TransactionTrendPoint>,
    val hourlyTrend: List<TransactionHourlyMetric>,
    val speakerStats: List<TransactionSpeakerMetric>,
    val successMetrics: TransactionSuccessMetrics,
    val analyticsRange: AnalyticsRange,
    val generatedAt: Long = System.currentTimeMillis(),
    val analyticsEnabled: Boolean = true
) {
    companion object {
        fun empty(range: AnalyticsRange, analyticsEnabled: Boolean): TransactionAnalyticsOverview {
            return TransactionAnalyticsOverview(
                summary = TransactionAnalytics(
                    totalSales = 0.0,
                    transactionCount = 0,
                    topProduct = null,
                    peakHour = null,
                    uniqueCustomers = 0,
                    averageTransactionValue = 0.0,
                    successRate = 0.0
                ),
                categories = emptyList(),
                dailyTrend = emptyList(),
                hourlyTrend = emptyList(),
                speakerStats = emptyList(),
                successMetrics = TransactionSuccessMetrics(),
                analyticsRange = range,
                analyticsEnabled = analyticsEnabled
            )
        }
    }
}
