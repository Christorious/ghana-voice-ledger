package com.voiceledger.ghana.data.local.dao

import androidx.room.*
import com.voiceledger.ghana.data.local.entity.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Transaction entity
 * Provides efficient queries for transaction data
 */
@Dao
interface TransactionDao {
    
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE date = :date ORDER BY timestamp DESC")
    fun getTransactionsByDate(date: String): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE date = :date ORDER BY timestamp DESC")
    suspend fun getTransactionsByDateSync(date: String): List<Transaction>
    
    @Query("SELECT * FROM transactions WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getTransactionsByCustomer(customerId: String): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE product LIKE '%' || :product || '%' ORDER BY timestamp DESC")
    fun getTransactionsByProduct(product: String): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE needsReview = 1 ORDER BY timestamp DESC")
    fun getTransactionsNeedingReview(): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE synced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedTransactions(): List<Transaction>
    
    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getTransactionsByTimeRange(startTime: Long, endTime: Long): Flow<List<Transaction>>
    
    @Query("SELECT SUM(amount) FROM transactions WHERE date = :date")
    suspend fun getTotalSalesForDate(date: String): Double?
    
    @Query("SELECT COUNT(*) FROM transactions WHERE date = :date")
    suspend fun getTransactionCountForDate(date: String): Int
    
    @Query("SELECT product, COUNT(*) as count FROM transactions WHERE date = :date GROUP BY product ORDER BY count DESC LIMIT 1")
    suspend fun getTopProductForDate(date: String): String?
    
    @Query("SELECT COUNT(DISTINCT customerId) FROM transactions WHERE date = :date AND customerId IS NOT NULL")
    suspend fun getUniqueCustomerCountForDate(date: String): Int
    
    @Query("SELECT COUNT(*) FROM transactions WHERE date = :date AND needsReview = 1")
    suspend fun getReviewedTransactionCountForDate(date: String): Int
    
    @Query("SELECT AVG(amount) FROM transactions WHERE date = :date")
    suspend fun getAverageTransactionValueForDate(date: String): Double?
    
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: String): Transaction?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>)
    
    @Update
    suspend fun updateTransaction(transaction: Transaction)
    
    @Delete
    suspend fun deleteTransaction(transaction: Transaction)
    
    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: String)
    
    @Query("DELETE FROM transactions WHERE date < :cutoffDate")
    suspend fun deleteOldTransactions(cutoffDate: String)
    
    @Query("SELECT DISTINCT product FROM transactions ORDER BY product ASC")
    suspend fun getAllProducts(): List<String>
    
    @Query("SELECT DISTINCT customerId FROM transactions WHERE customerId IS NOT NULL ORDER BY customerId ASC")
    suspend fun getAllCustomerIds(): List<String>
    
    @Query("UPDATE transactions SET synced = 1 WHERE id IN (:ids)")
    suspend fun markTransactionsAsSynced(ids: List<String>)
    
    @Query("UPDATE transactions SET needsReview = 0 WHERE id = :id")
    suspend fun markTransactionAsReviewed(id: String)
    
    // Advanced analytics queries
    @Query("""
        SELECT strftime('%H', datetime(timestamp/1000, 'unixepoch')) as hour, 
               SUM(amount) as total 
        FROM transactions 
        WHERE date = :date 
        GROUP BY hour 
        ORDER BY total DESC 
        LIMIT 1
    """)
    suspend fun getMostProfitableHourForDate(date: String): Int?
    
    @Query("""
        SELECT strftime('%H', datetime(timestamp/1000, 'unixepoch')) as hour, 
               COUNT(*) as count 
        FROM transactions 
        WHERE date = :date 
        GROUP BY hour 
        ORDER BY count DESC 
        LIMIT 1
    """)
    suspend fun getPeakHourForDate(date: String): String?
    
    @Query("SELECT * FROM transactions WHERE amount BETWEEN :minAmount AND :maxAmount ORDER BY timestamp DESC")
    fun getTransactionsByAmountRange(minAmount: Double, maxAmount: Double): Flow<List<Transaction>>
    
    @Query("SELECT DISTINCT product FROM transactions ORDER BY product ASC")
    suspend fun getAllProducts(): List<String>
    
    @Query("SELECT DISTINCT customerId FROM transactions WHERE customerId IS NOT NULL ORDER BY customerId ASC")
    suspend fun getAllCustomerIds(): List<String>
    
    // Additional analytics queries
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE date = :date")
    suspend fun getTotalSalesForDate(date: String): Double
    
    @Query("SELECT COUNT(*) FROM transactions WHERE date = :date")
    suspend fun getTransactionCountForDate(date: String): Int
    
    @Query("SELECT product FROM transactions WHERE date = :date GROUP BY product ORDER BY COUNT(*) DESC LIMIT 1")
    suspend fun getTopProductForDate(date: String): String?
    
    @Query("SELECT COUNT(DISTINCT customerId) FROM transactions WHERE date = :date AND customerId IS NOT NULL")
    suspend fun getUniqueCustomerCountForDate(date: String): Int
    
    @Query("SELECT COALESCE(AVG(amount), 0.0) FROM transactions WHERE date = :date")
    suspend fun getAverageTransactionValueForDate(date: String): Double
    
    @Query("SELECT COUNT(*) FROM transactions WHERE date = :date AND needsReview = 0")
    suspend fun getReviewedTransactionCountForDate(date: String): Int
    
    // Additional methods for offline-first architecture
    
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactions(): List<Transaction>
    
    @Query("SELECT * FROM transactions")
    fun getAllTransactionsFlow(): Flow<List<Transaction>>
    
    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int
    
    @Query("SELECT COUNT(*) FROM transactions WHERE synced = 0")
    suspend fun getUnsyncedTransactionCount(): Int
    
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    suspend fun getTransactionsByDateRange(startDate: String, endDate: String): List<Transaction>
    
    @Query("SELECT * FROM transactions WHERE date = date('now', 'localtime') ORDER BY timestamp DESC")
    fun getTodaysTransactionsFlow(): Flow<List<Transaction>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTransaction(transaction: Transaction)
    
    @Query("UPDATE transactions SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
    
    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: String): Int
}