package com.voiceledger.ghana.data.repository

import com.voiceledger.ghana.data.local.dao.TransactionDao
import com.voiceledger.ghana.data.local.entity.Transaction
import com.voiceledger.ghana.domain.model.TransactionAnalytics
import com.voiceledger.ghana.domain.repository.TransactionRepository
import com.voiceledger.ghana.security.SecurityManager
import com.voiceledger.ghana.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TransactionRepository
 * Handles all transaction-related data operations
 */
@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val securityManager: SecurityManager
) : TransactionRepository {
    
    override fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions()
    }
    
    override fun getTransactionsByDate(date: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByDate(date)
    }
    
    override fun getTodaysTransactions(): Flow<List<Transaction>> {
        val today = DateUtils.getTodayDateString()
        return transactionDao.getTransactionsByDate(today)
    }
    
    override fun getTodaysAnalytics(): Flow<TransactionAnalytics> {
        return getTodaysTransactions().map { transactions ->
            computeAnalytics(transactions)
        }.distinctUntilChanged()
    }
    
    private fun computeAnalytics(transactions: List<Transaction>): TransactionAnalytics {
        val startTime = System.currentTimeMillis()
        
        if (transactions.isEmpty()) {
            Timber.d("Computing analytics for empty transaction list")
            return TransactionAnalytics(
                totalSales = 0.0,
                transactionCount = 0,
                topProduct = null,
                peakHour = null,
                uniqueCustomers = 0,
                averageTransactionValue = 0.0
            )
        }
        
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
            ?.let { hour -> formatHourRange(hour) }
        
        val uniqueCustomers = transactions
            .mapNotNull { it.customerId }
            .toSet()
            .size
        
        val averageTransactionValue = totalSales / transactionCount
        
        val endTime = System.currentTimeMillis()
        val computationTime = endTime - startTime
        
        Timber.d(
            "Analytics computed in ${computationTime}ms: " +
            "totalSales=$totalSales, count=$transactionCount, " +
            "topProduct=$topProduct, peakHour=$peakHour, " +
            "uniqueCustomers=$uniqueCustomers"
        )
        
        return TransactionAnalytics(
            totalSales = totalSales,
            transactionCount = transactionCount,
            topProduct = topProduct,
            peakHour = peakHour,
            uniqueCustomers = uniqueCustomers,
            averageTransactionValue = averageTransactionValue
        )
    }
    
    private fun extractHourOfDay(timestamp: Long): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return calendar.get(Calendar.HOUR_OF_DAY)
    }
    
    private fun formatHourRange(hour: Int): String {
        val nextHour = (hour + 1) % 24
        return String.format(Locale.getDefault(), "%02d:00-%02d:00", hour, nextHour)
    }
    
    override fun getTransactionsByCustomer(customerId: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByCustomer(customerId)
    }
    
    override fun getTransactionsByProduct(product: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByProduct(product)
    }
    
    override fun getTransactionsNeedingReview(): Flow<List<Transaction>> {
        return transactionDao.getTransactionsNeedingReview()
    }
    
    override fun getTransactionsByTimeRange(startTime: Long, endTime: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByTimeRange(startTime, endTime)
    }
    
    override fun getTransactionsByAmountRange(minAmount: Double, maxAmount: Double): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByAmountRange(minAmount, maxAmount)
    }
    
    override fun getTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>> {
        return getTransactionsByTimeRange(startTime, endTime)
    }
    
    override suspend fun getTotalSalesForDate(date: String): Double {
        return transactionDao.getTotalSalesForDate(date) ?: 0.0
    }
    
    override suspend fun getTodaysTotalSales(): Double {
        val today = DateUtils.getTodayDateString()
        return getTotalSalesForDate(today)
    }
    
    override suspend fun getTransactionCountForDate(date: String): Int {
        return transactionDao.getTransactionCountForDate(date)
    }
    
    override suspend fun getTodaysTransactionCount(): Int {
        val today = DateUtils.getTodayDateString()
        return getTransactionCountForDate(today)
    }
    
    override suspend fun getTopProductForDate(date: String): String? {
        return transactionDao.getTopProductForDate(date)
    }
    
    override suspend fun getTodaysTopProduct(): String? {
        val today = DateUtils.getTodayDateString()
        return getTopProductForDate(today)
    }
    
    override suspend fun getUniqueCustomerCountForDate(date: String): Int {
        return transactionDao.getUniqueCustomerCountForDate(date)
    }
    
    override suspend fun getTodaysUniqueCustomerCount(): Int {
        val today = DateUtils.getTodayDateString()
        return getUniqueCustomerCountForDate(today)
    }
    
    override suspend fun getAverageTransactionValueForDate(date: String): Double {
        return transactionDao.getAverageTransactionValueForDate(date) ?: 0.0
    }
    
    override suspend fun getTodaysAverageTransactionValue(): Double {
        val today = DateUtils.getTodayDateString()
        return getAverageTransactionValueForDate(today)
    }
    
    override suspend fun getMostProfitableHourForDate(date: String): Int? {
        return transactionDao.getMostProfitableHourForDate(date)
    }
    
    override suspend fun getTodaysMostProfitableHour(): Int? {
        val today = DateUtils.getTodayDateString()
        return getMostProfitableHourForDate(today)
    }
    
    override suspend fun getPeakHourForDate(date: String): String? {
        return transactionDao.getPeakHourForDate(date)
    }
    
    override suspend fun getTodaysPeakHour(): String? {
        val today = DateUtils.getTodayDateString()
        return getPeakHourForDate(today)
    }
    
    override suspend fun getTransactionById(id: String): Transaction? {
        return transactionDao.getTransactionById(id)
    }
    
    override suspend fun insertTransaction(transaction: Transaction) {
        val transactionWithDate = transaction.copy(
            date = DateUtils.formatDate(transaction.timestamp)
        )
        transactionDao.insertTransaction(transactionWithDate)
    }
    
    override suspend fun insertTransactions(transactions: List<Transaction>) {
        val transactionsWithDate = transactions.map { transaction ->
            transaction.copy(
                date = DateUtils.formatDate(transaction.timestamp)
            )
        }
        transactionDao.insertTransactions(transactionsWithDate)
    }
    
    override suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }
    
    override suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }
    
    override suspend fun deleteTransactionById(id: String) {
        transactionDao.deleteTransactionById(id)
    }
    
    override suspend fun markTransactionAsReviewed(id: String) {
        transactionDao.markTransactionAsReviewed(id)
    }
    
    override suspend fun getUnsyncedTransactions(): List<Transaction> {
        return transactionDao.getUnsyncedTransactions()
    }
    
    override suspend fun markTransactionsAsSynced(ids: List<String>) {
        transactionDao.markTransactionsAsSynced(ids)
    }
    
    override suspend fun deleteOldTransactions(daysToKeep: Int) {
        val cutoffDate = DateUtils.getDateDaysAgo(daysToKeep)
        transactionDao.deleteOldTransactions(cutoffDate)
    }
    
    override suspend fun getAllProducts(): List<String> {
        return transactionDao.getAllProducts()
    }
    
    override suspend fun getAllCustomerIds(): List<String> {
        return transactionDao.getAllCustomerIds()
    }
    
    override fun searchTransactions(query: String): Flow<List<Transaction>> {
        val sanitizedQuery = securityManager.sanitizeForQuery(query)
        if (sanitizedQuery.isBlank()) {
            return getAllTransactions()
        }
        return getAllTransactions().map { transactions ->
            transactions.filter { transaction ->
                transaction.product.contains(sanitizedQuery, ignoreCase = true) ||
                transaction.transcriptSnippet.contains(sanitizedQuery, ignoreCase = true) ||
                transaction.amount.toString().contains(sanitizedQuery) ||
                transaction.customerId?.contains(sanitizedQuery, ignoreCase = true) == true
            }
        }
    }
    
    override suspend fun getTransactionStats(startDate: String, endDate: String): TransactionStats {
        val transactions = transactionDao.getTransactionsByDateSync(startDate)
        
        return TransactionStats(
            totalTransactions = transactions.size,
            totalAmount = transactions.sumOf { it.amount },
            averageAmount = if (transactions.isNotEmpty()) transactions.sumOf { it.amount } / transactions.size else 0.0,
            highestAmount = transactions.maxOfOrNull { it.amount } ?: 0.0,
            lowestAmount = transactions.minOfOrNull { it.amount } ?: 0.0,
            uniqueCustomers = transactions.mapNotNull { it.customerId }.distinct().size,
            uniqueProducts = transactions.map { it.product }.distinct().size,
            reviewedTransactions = transactions.count { it.needsReview }
        )
    }
    
}

/**
 * Data class for transaction statistics
 */
data class TransactionStats(
    val totalTransactions: Int,
    val totalAmount: Double,
    val averageAmount: Double,
    val highestAmount: Double,
    val lowestAmount: Double,
    val uniqueCustomers: Int,
    val uniqueProducts: Int,
    val reviewedTransactions: Int
)