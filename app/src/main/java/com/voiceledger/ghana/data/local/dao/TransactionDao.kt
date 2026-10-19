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
    
    // Only the product is selected: Room maps a single-column result to String?.
    // Selecting the count as well would give a two-column cursor it cannot convert.
    @Query("SELECT product FROM transactions WHERE date = :date GROUP BY product ORDER BY COUNT(*) DESC LIMIT 1")
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

    /**
     * Retention enforcement: returns the number of rows removed, for the deletion report.
     * Takes an epoch timestamp rather than a date string, because retention is computed
     * from PrivacyManager.getDataRetentionPeriod, which works in milliseconds.
     */
    @Query("DELETE FROM transactions WHERE timestamp < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long): Int
    
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
        SELECT CAST(strftime('%H', datetime(timestamp/1000, 'unixepoch')) AS INTEGER) AS hour
        FROM transactions
        WHERE date = :date
        GROUP BY hour
        ORDER BY SUM(amount) DESC
        LIMIT 1
    """)
    suspend fun getMostProfitableHourForDate(date: String): Int?
    
    @Query("""
        SELECT strftime('%H', datetime(timestamp/1000, 'unixepoch')) AS hour
        FROM transactions
        WHERE date = :date
        GROUP BY hour
        ORDER BY COUNT(*) DESC
        LIMIT 1
    """)
    suspend fun getPeakHourForDate(date: String): String?
    
    @Query("SELECT * FROM transactions WHERE amount BETWEEN :minAmount AND :maxAmount ORDER BY timestamp DESC")
    fun getTransactionsByAmountRange(minAmount: Double, maxAmount: Double): Flow<List<Transaction>>
    
    // Additional methods for offline-first architecture

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