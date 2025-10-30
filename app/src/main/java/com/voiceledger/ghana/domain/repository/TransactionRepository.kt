package com.voiceledger.ghana.domain.repository

import com.voiceledger.ghana.data.local.entity.Transaction
import com.voiceledger.ghana.data.repository.TransactionStats
import com.voiceledger.ghana.domain.model.TransactionAnalytics
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for transaction operations
 * Defines the contract for transaction data access
 */
interface TransactionRepository {
    
    // Query operations
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getTransactionsByDate(date: String): Flow<List<Transaction>>
    fun getTodaysTransactions(): Flow<List<Transaction>>
    fun getTransactionsByCustomer(customerId: String): Flow<List<Transaction>>
    fun getTransactionsByProduct(product: String): Flow<List<Transaction>>
    fun getTransactionsNeedingReview(): Flow<List<Transaction>>
    fun getTransactionsByTimeRange(startTime: Long, endTime: Long): Flow<List<Transaction>>
    fun getTransactionsByAmountRange(minAmount: Double, maxAmount: Double): Flow<List<Transaction>>
    fun searchTransactions(query: String): Flow<List<Transaction>>
    
    // Analytics flow
    fun getTodaysAnalytics(): Flow<TransactionAnalytics>
    
    // Analytics operations
    suspend fun getTotalSalesForDate(date: String): Double
    suspend fun getTodaysTotalSales(): Double
    suspend fun getTransactionCountForDate(date: String): Int
    suspend fun getTodaysTransactionCount(): Int
    suspend fun getTopProductForDate(date: String): String?
    suspend fun getTodaysTopProduct(): String?
    suspend fun getUniqueCustomerCountForDate(date: String): Int
    suspend fun getTodaysUniqueCustomerCount(): Int
    suspend fun getAverageTransactionValueForDate(date: String): Double
    suspend fun getTodaysAverageTransactionValue(): Double
    suspend fun getMostProfitableHourForDate(date: String): Int?
    suspend fun getTodaysMostProfitableHour(): Int?
    suspend fun getPeakHourForDate(date: String): String?
    suspend fun getTodaysPeakHour(): String?
    suspend fun getTransactionStats(startDate: String, endDate: String): TransactionStats
    
    // CRUD operations
    suspend fun getTransactionById(id: String): Transaction?
    suspend fun insertTransaction(transaction: Transaction)
    suspend fun insertTransactions(transactions: List<Transaction>)
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
    suspend fun deleteTransactionById(id: String)
    suspend fun markTransactionAsReviewed(id: String)
    
    // Sync operations
    suspend fun getUnsyncedTransactions(): List<Transaction>
    suspend fun markTransactionsAsSynced(ids: List<String>)
    
    // Maintenance operations
    suspend fun deleteOldTransactions(daysToKeep: Int)
    suspend fun getAllProducts(): List<String>
    suspend fun getAllCustomerIds(): List<String>
}