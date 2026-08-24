package com.voiceledger.ghana.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<Transaction>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE timestamp >= :startOfDay")
    fun observeTotalSince(startOfDay: Long): Flow<Double>

    // --- insights ---

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE timestamp >= :start AND timestamp < :end")
    fun observeTotalBetween(start: Long, end: Long): Flow<Double>

    @Query("SELECT COUNT(*) FROM transactions WHERE timestamp >= :start AND timestamp < :end")
    fun observeCountBetween(start: Long, end: Long): Flow<Int>

    @Query(
        """
        SELECT description AS name, SUM(amount) AS total, COUNT(*) AS count
        FROM transactions
        WHERE timestamp >= :start AND timestamp < :end
        GROUP BY description
        ORDER BY total DESC
        LIMIT :limit
        """
    )
    fun observeTopProducts(start: Long, end: Long, limit: Int): Flow<List<ProductTotal>>

    @Query(
        """
        SELECT strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch', 'localtime') AS day,
               SUM(amount) AS total
        FROM transactions
        WHERE timestamp >= :start
        GROUP BY day
        ORDER BY day ASC
        """
    )
    fun observeDailyTotals(start: Long): Flow<List<DayTotal>>

    @Insert
    suspend fun insert(transaction: Transaction)

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)
}
