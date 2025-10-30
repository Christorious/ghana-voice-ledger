package com.voiceledger.ghana.domain.model

/**
 * Precomputed analytics data for transactions
 * Used to avoid expensive suspend calls in flow combine blocks
 */
data class TransactionAnalytics(
    val totalSales: Double,
    val transactionCount: Int,
    val topProduct: String?,
    val peakHour: String?,
    val uniqueCustomers: Int,
    val averageTransactionValue: Double
)
